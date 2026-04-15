package com.greengo.utils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PricingPlanPeriodUtil {

    private static final Pattern HIRE_PERIOD_PATTERN = Pattern.compile("^(MINUTE|HOUR|DAY|WEEK|MONTH)_(\\d+)$");

    private PricingPlanPeriodUtil() {
    }

    public static String normalizeHirePeriod(String hirePeriod) {
        if (hirePeriod == null) {
            return null;
        }

        String normalized = hirePeriod.trim().toUpperCase(Locale.ROOT);
        Matcher matcher = HIRE_PERIOD_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }

        int amount = parsePositiveAmount(matcher.group(2));
        if (amount <= 0) {
            return null;
        }

        return matcher.group(1) + "_" + amount;
    }

    public static boolean isSupportedHirePeriod(String hirePeriod) {
        return normalizeHirePeriod(hirePeriod) != null;
    }

    public static boolean isMinuteHirePeriod(String hirePeriod) {
        ParsedHirePeriod parsed = parse(hirePeriod);
        return parsed != null && "MINUTE".equals(parsed.unit());
    }

    public static boolean isReservationHirePeriod(String hirePeriod) {
        ParsedHirePeriod parsed = parse(hirePeriod);
        return parsed != null && !"MINUTE".equals(parsed.unit());
    }

    public static LocalDateTime addPeriod(LocalDateTime baseTime, String hirePeriod) {
        if (baseTime == null) {
            throw new IllegalArgumentException("Booking time is missing");
        }

        ParsedHirePeriod parsed = parseRequired(hirePeriod);
        return switch (parsed.unit()) {
            case "MINUTE" -> baseTime.plusMinutes(parsed.amount());
            case "HOUR" -> baseTime.plusHours(parsed.amount());
            case "DAY" -> baseTime.plusDays(parsed.amount());
            case "WEEK" -> baseTime.plusWeeks(parsed.amount());
            case "MONTH" -> baseTime.plusMonths(parsed.amount());
            default -> throw new IllegalArgumentException("Unsupported hire period unit");
        };
    }

    public static int compareHirePeriods(String left, String right) {
        ParsedHirePeriod leftParsed = parse(left);
        ParsedHirePeriod rightParsed = parse(right);

        if (leftParsed != null && rightParsed != null) {
            int durationCompare = Long.compare(leftParsed.sortWeight(), rightParsed.sortWeight());
            if (durationCompare != 0) {
                return durationCompare;
            }

            int unitCompare = Integer.compare(leftParsed.unitOrder(), rightParsed.unitOrder());
            if (unitCompare != 0) {
                return unitCompare;
            }

            return Integer.compare(leftParsed.amount(), rightParsed.amount());
        }

        if (leftParsed != null) {
            return -1;
        }
        if (rightParsed != null) {
            return 1;
        }

        return safeString(left).compareTo(safeString(right));
    }

    public static <T> Comparator<T> comparingByHirePeriod(Function<T, String> extractor) {
        Objects.requireNonNull(extractor, "extractor");
        return (left, right) -> compareHirePeriods(extractor.apply(left), extractor.apply(right));
    }

    public static String formatHint() {
        return "Hire period must use UNIT_NUMBER format like MINUTE_1, HOUR_2, DAY_3, WEEK_2, or MONTH_1";
    }

    public static String reservationFormatHint() {
        return "Hire period must use UNIT_NUMBER format like HOUR_2, DAY_3, WEEK_2, or MONTH_1";
    }

    private static ParsedHirePeriod parse(String hirePeriod) {
        String normalized = normalizeHirePeriod(hirePeriod);
        if (normalized == null) {
            return null;
        }

        Matcher matcher = HIRE_PERIOD_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }

        int amount = parsePositiveAmount(matcher.group(2));
        if (amount <= 0) {
            return null;
        }

        return new ParsedHirePeriod(matcher.group(1), amount);
    }

    private static ParsedHirePeriod parseRequired(String hirePeriod) {
        ParsedHirePeriod parsed = parse(hirePeriod);
        if (parsed == null) {
            throw new IllegalArgumentException(formatHint());
        }
        return parsed;
    }

    private static int parsePositiveAmount(String value) {
        try {
            int amount = Integer.parseInt(value);
            return amount > 0 ? amount : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private record ParsedHirePeriod(String unit, int amount) {

        private long sortWeight() {
            return switch (unit) {
                case "MINUTE" -> amount;
                case "HOUR" -> amount * 60L;
                case "DAY" -> amount * 1_440L;
                case "WEEK" -> amount * 10_080L;
                case "MONTH" -> amount * 43_200L;
                default -> Long.MAX_VALUE;
            };
        }

        private int unitOrder() {
            return switch (unit) {
                case "MINUTE" -> 0;
                case "HOUR" -> 1;
                case "DAY" -> 2;
                case "WEEK" -> 3;
                case "MONTH" -> 4;
                default -> Integer.MAX_VALUE;
            };
        }
    }
}
