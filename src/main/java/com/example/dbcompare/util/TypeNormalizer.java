package com.example.dbcompare.util;

import com.example.dbcompare.domain.enums.DatabaseType;

import java.util.EnumMap;
import java.util.Map;

public class TypeNormalizer {
    private final Map<DatabaseType, TypeNormalizationRule> rules = new EnumMap<>(DatabaseType.class);

    public TypeNormalizer() {
        TypeNormalizationRule identity = TypeNormalizationRule.identity();
        rules.put(DatabaseType.AS400, new As400TypeNormalizationRule());
        rules.put(DatabaseType.DB2, identity);
        rules.put(DatabaseType.GAUSS, new GaussTypeNormalizationRule());
        rules.put(DatabaseType.SNAPSHOT, identity);
    }

    public String normalize(DatabaseType databaseType, String rawType) {
        String normalizedType = normalizeCommon(rawType);
        if (normalizedType == null) {
            return null;
        }
        TypeNormalizationRule rule = rules.getOrDefault(databaseType, TypeNormalizationRule.identity());
        return rule.normalize(normalizedType);
    }

    private String normalizeCommon(String rawType) {
        if (rawType == null) {
            return null;
        }
        String type = rawType.trim().toUpperCase();
        if ("CHARACTER VARYING".equals(type)) return "VARCHAR";
        if ("CHARACTER".equals(type)) return "CHAR";
        if ("DECIMAL".equals(type)) return "NUMERIC";
        return type;
    }
}
