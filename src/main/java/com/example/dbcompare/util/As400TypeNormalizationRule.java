package com.example.dbcompare.util;

public class As400TypeNormalizationRule implements TypeNormalizationRule {
    @Override
    public String normalize(String normalizedType) {
        if ("GRAPHIC".equals(normalizedType)) return "CHAR";
        if ("VARGRAPHIC".equals(normalizedType)) return "VARCHAR";
        return normalizedType;
    }
}
