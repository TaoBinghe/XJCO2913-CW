package com.greengo.utils;

public final class RedisKeys {

    private static final String PREFIX = "greengo:";

    private RedisKeys() {}

    public static String authSession(String sid) {
        return PREFIX + "auth:session:" + sid;
    }

    public static String authUserSessions(Long userId) {
        return PREFIX + "auth:user-sessions:" + userId;
    }

    public static String userBookingLock(Long userId) {
        return PREFIX + "lock:user-booking:" + userId;
    }

    public static String scooterLock(Long scooterId) {
        return PREFIX + "lock:scooter:" + scooterId;
    }

    public static String storeLock(Long storeId) {
        return PREFIX + "lock:store:" + storeId;
    }

    public static String bookingLock(Long bookingId) {
        return PREFIX + "lock:booking:" + bookingId;
    }
}
