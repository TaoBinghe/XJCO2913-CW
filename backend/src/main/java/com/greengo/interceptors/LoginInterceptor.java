package com.greengo.interceptors;


import com.greengo.domain.AuthSession;
import com.greengo.service.AuthSessionService;
import com.greengo.utils.JwtUtil;
import com.greengo.utils.ThreadLocalUtil;
import io.micrometer.common.lang.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthSessionService authSessionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (CorsUtils.isPreFlightRequest(request) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            response.setStatus(401);
            return false;
        }
        String token = auth.startsWith("Bearer ") ? auth.substring(7) : auth;
        try {
            Map<String, Object> tokenClaims = JwtUtil.parseToken(token);
            String sid = resolveSessionId(tokenClaims);
            AuthSession session = authSessionService.getSession(sid);
            if (!isSessionValid(tokenClaims, session)) {
                response.setStatus(401);
                return false;
            }

            ThreadLocalUtil.set(buildRequestClaims(session));
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        ThreadLocalUtil.remove();
    }

    private String resolveSessionId(Map<String, Object> claims) {
        if (claims == null || claims.get("sid") == null) {
            return null;
        }
        return claims.get("sid").toString();
    }

    private boolean isSessionValid(Map<String, Object> tokenClaims, AuthSession session) {
        if (session == null || session.getStatus() == null || session.getStatus() != 1) {
            return false;
        }
        if (tokenClaims == null || !(tokenClaims.get("id") instanceof Number)) {
            return false;
        }

        long tokenUserId = ((Number) tokenClaims.get("id")).longValue();
        if (session.getUserId() == null || session.getUserId() != tokenUserId) {
            return false;
        }

        Object tokenRole = tokenClaims.get("role");
        return tokenRole != null && session.getRole() != null && session.getRole().equalsIgnoreCase(tokenRole.toString());
    }

    private Map<String, Object> buildRequestClaims(AuthSession session) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", session.getUserId());
        claims.put("username", session.getUsername());
        claims.put("role", session.getRole());
        claims.put("status", session.getStatus());
        claims.put("sid", session.getSid());
        return claims;
    }
}

