package com.greengo.interceptors;

import com.greengo.domain.AuthSession;
import com.greengo.service.AuthSessionService;
import com.greengo.utils.JwtUtil;
import com.greengo.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginInterceptorTest {

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private LoginInterceptor loginInterceptor;

    @BeforeEach
    void setUp() {
        loginInterceptor = new LoginInterceptor();
        ReflectionTestUtils.setField(loginInterceptor, "authSessionService", authSessionService);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalUtil.remove();
    }

    @Test
    void preHandleAcceptsTokenBackedByRedisSession() throws Exception {
        String token = JwtUtil.genToken(Map.of(
                "id", 1L,
                "username", "alice01",
                "role", "CUSTOMER",
                "sid", "sid-123"
        ));
        AuthSession session = AuthSession.builder()
                .sid("sid-123")
                .userId(1L)
                .username("alice01")
                .role("CUSTOMER")
                .status(1)
                .build();
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(token);
        when(authSessionService.getSession("sid-123")).thenReturn(session);

        boolean allowed = loginInterceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
        Map<String, Object> claims = ThreadLocalUtil.get();
        assertEquals(1L, ((Number) claims.get("id")).longValue());
        assertEquals("sid-123", claims.get("sid"));
        assertEquals("CUSTOMER", claims.get("role"));
    }

    @Test
    void preHandleRejectsMissingRedisSession() throws Exception {
        String token = JwtUtil.genToken(Map.of(
                "id", 1L,
                "username", "alice01",
                "role", "CUSTOMER",
                "sid", "sid-123"
        ));
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(token);
        when(authSessionService.getSession("sid-123")).thenReturn(null);

        boolean allowed = loginInterceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        verify(response).setStatus(401);
    }
}
