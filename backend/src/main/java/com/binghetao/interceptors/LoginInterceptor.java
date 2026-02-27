package com.binghetao.interceptors;


import com.binghetao.utils.JwtUtil;
import com.binghetao.utils.ThreadLocalUtil;
import io.micrometer.common.lang.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            response.setStatus(401);
            return false;
        }
        // 支持 "Bearer <token>" 或直接 "<token>"
        String token = auth.startsWith("Bearer ") ? auth.substring(7) : auth;
        try {
            Map<String, Object> claims = JwtUtil.parseToken(token);

            // store user information
            ThreadLocalUtil.set(claims);
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
}
