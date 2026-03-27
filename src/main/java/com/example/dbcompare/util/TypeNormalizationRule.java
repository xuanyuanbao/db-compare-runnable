package com.example.dbcompare.util;

@FunctionalInterface
public interface TypeNormalizationRule {
    String normalize(String normalizedType);

    static TypeNormalizationRule identity() {
        return normalizedType -> normalizedType;
    }
}
