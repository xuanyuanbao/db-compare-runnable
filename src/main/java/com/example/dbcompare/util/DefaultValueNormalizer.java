package com.example.dbcompare.util;

public final class DefaultValueNormalizer {
    private DefaultValueNormalizer() {}

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = normalized.replace("::character varying", "")
                .replace("::bpchar", "")
                .replace("'", "")
                .trim();
        if ("NULL".equalsIgnoreCase(normalized)) {
            return null;
        }
        return normalized.toUpperCase();
    }
}
