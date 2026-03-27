package com.example.dbcompare.util;

public class GaussTypeNormalizationRule implements TypeNormalizationRule {
    @Override
    public String normalize(String normalizedType) {
        if ("INT4".equals(normalizedType)) return "INTEGER";
        return normalizedType;
    }
}
