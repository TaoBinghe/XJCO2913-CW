package com.greengo.utils;

public final class LockKeys {

    private static final String PREFIX = "greengo:";

    private LockKeys() {
    }

    public static String userBookingLock(Long userId) {
        return PREFIX + "lock:user-booking:" + userId;
    }

    public static String userWalletLock(Long userId) {
        return PREFIX + "lock:user-wallet:" + userId;
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
