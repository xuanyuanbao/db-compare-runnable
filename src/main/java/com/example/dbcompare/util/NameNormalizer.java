package com.example.dbcompare.util;

public final class NameNormalizer {
    private NameNormalizer() {}

    public static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
