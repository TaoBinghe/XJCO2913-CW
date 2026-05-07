package com.greengo.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordHashUtil {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    private static final String BCRYPT_PREFIX = "$2";
    private static final int MD5_HEX_LENGTH = 32;

    private PasswordHashUtil() {
    }

    public static String hash(String rawPassword) {
        return BCRYPT.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        if (isBcrypt(storedHash)) {
            try {
                return BCRYPT.matches(rawPassword, storedHash);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        if (isLegacyMd5(storedHash)) {
            return Md5Util.checkPassword(rawPassword, storedHash);
        }
        return false;
    }

    public static boolean needsUpgrade(String storedHash) {
        return !isBcrypt(storedHash);
    }

    public static boolean isBcrypt(String storedHash) {
        return storedHash != null && storedHash.startsWith(BCRYPT_PREFIX);
    }

    private static boolean isLegacyMd5(String storedHash) {
        return storedHash != null
                && storedHash.length() == MD5_HEX_LENGTH
                && storedHash.matches("[0-9a-fA-F]{32}");
    }
}
