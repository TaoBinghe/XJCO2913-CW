package com.greengo.utils;

public final class RentalConstants {

    public static final String RENTAL_TYPE_STORE_PICKUP = "STORE_PICKUP";
    public static final String RENTAL_TYPE_SCAN_RIDE = "SCAN_RIDE";

    public static final String BOOKING_STATUS_RESERVED = "RESERVED";
    public static final String BOOKING_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String BOOKING_STATUS_OVERDUE = "OVERDUE";
    public static final String BOOKING_STATUS_COMPLETED = "COMPLETED";
    public static final String BOOKING_STATUS_CANCELLED = "CANCELLED";
    public static final String BOOKING_STATUS_NO_SHOW_CANCELLED = "NO_SHOW_CANCELLED";

    public static final String SCOOTER_STATUS_AVAILABLE = "AVAILABLE";
    public static final String SCOOTER_STATUS_IN_USE = "IN_USE";
    public static final String SCOOTER_STATUS_MAINTENANCE = "MAINTENANCE";
    public static final String SCOOTER_STATUS_DISABLED = "DISABLED";

    public static final String SCOOTER_LOCK_STATUS_LOCKED = "LOCKED";
    public static final String SCOOTER_LOCK_STATUS_UNLOCKED = "UNLOCKED";

    public static final String STORE_STATUS_ENABLED = "ENABLED";
    public static final String STORE_STATUS_DISABLED = "DISABLED";

    public static final String LEGACY_BOOKING_DISABLED_MESSAGE = "Legacy direct scooter booking is no longer supported";

    private RentalConstants() {
    }
}
