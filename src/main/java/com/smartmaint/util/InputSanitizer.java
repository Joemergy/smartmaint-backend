package com.smartmaint.util;

public final class InputSanitizer {

    private InputSanitizer() {
    }

    public static String normalizeEmail(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase();
    }

    public static String normalizeText(String value) {
        if (value == null) return "";
        // Remove control characters except line breaks and tabs.
        return value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
    }

    public static String normalizeText(String value, int maxLength) {
        String normalized = normalizeText(value);
        if (maxLength <= 0 || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
