package com.greengo.utils;

import java.util.Map;

/**
 * Auth helpers based on current request context (ThreadLocal claims).
 */
public final class AuthUtil {

    private AuthUtil() {}

    /**
     * Whether the current request user has MANAGER role (admin).
     */
    public static boolean isAdmin() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) {
            return false;
        }
        Object role = claims.get("role");
        return role != null && "MANAGER".equalsIgnoreCase(role.toString());
    }
}

