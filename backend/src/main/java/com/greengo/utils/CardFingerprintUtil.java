package com.greengo.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class CardFingerprintUtil {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private CardFingerprintUtil() {
    }

    public static String fingerprint(String normalizedCardNumber) {
        if (normalizedCardNumber == null || normalizedCardNumber.isBlank()) {
            throw new IllegalArgumentException("Card number is required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedCardNumber.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            chars[i * 2] = HEX[value >>> 4];
            chars[i * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(chars);
    }
}
