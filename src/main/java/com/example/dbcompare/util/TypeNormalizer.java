package com.example.dbcompare.util;

import com.example.dbcompare.domain.enums.DatabaseType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
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
        return normalize(databaseType, rawType, Map.of());
    }

    public String normalize(DatabaseType databaseType, String rawType, Map<String, List<String>> customTypeMappings) {
        String normalizedType = normalizeCommon(rawType);
        if (normalizedType == null) {
            return null;
        }
        TypeNormalizationRule rule = rules.getOrDefault(databaseType, TypeNormalizationRule.identity());
        String normalizedByRule = rule.normalize(normalizedType);
        return normalizeCustomMappings(normalizedByRule, customTypeMappings);
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

    private String normalizeCustomMappings(String normalizedType, Map<String, List<String>> customTypeMappings) {
        if (normalizedType == null || customTypeMappings == null || customTypeMappings.isEmpty()) {
            return normalizedType;
        }
        Map<String, String> aliasToCanonical = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : customTypeMappings.entrySet()) {
            String canonical = normalizeCommon(entry.getKey());
            if (canonical == null) {
                continue;
            }
            aliasToCanonical.put(canonical, canonical);
            for (String alias : entry.getValue()) {
                String normalizedAlias = normalizeCommon(alias);
                if (normalizedAlias != null) {
                    aliasToCanonical.put(normalizedAlias, canonical);
                }
            }
        }
        return aliasToCanonical.getOrDefault(normalizedType, normalizedType);
    }
}
