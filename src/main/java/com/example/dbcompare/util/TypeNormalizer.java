package com.example.dbcompare.util;

import com.example.dbcompare.domain.enums.DatabaseType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeNormalizer {
    private final Map<DatabaseType, TypeNormalizationRule> rules = new EnumMap<>(DatabaseType.class);

    public TypeNormalizer() {
        TypeNormalizationRule identity = TypeNormalizationRule.identity();
        rules.put(DatabaseType.AS400, identity);
        rules.put(DatabaseType.DB2, identity);
        rules.put(DatabaseType.GAUSS, identity);
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
        return type.isEmpty() ? null : type;
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
            if (entry.getValue() == null) {
                continue;
            }
            for (String alias : entry.getValue()) {
                String normalizedAlias = normalizeCommon(alias);
                if (normalizedAlias != null) {
                    aliasToCanonical.put(normalizedAlias, canonical);
                }
            }
        }
        return aliasToCanonical.getOrDefault(normalizedType, normalizedType);
    }

    public List<TypeEqualityRule> describeTypeEqualityRules(Map<String, List<String>> customTypeMappings) {
        List<TypeEqualityRule> rules = new java.util.ArrayList<>();
        if (customTypeMappings == null || customTypeMappings.isEmpty()) {
            return rules;
        }
        for (Map.Entry<String, List<String>> entry : customTypeMappings.entrySet()) {
            String canonicalType = normalizeCommon(entry.getKey());
            if (canonicalType == null) {
                continue;
            }
            LinkedHashSet<String> rawTypes = new LinkedHashSet<>();
            rawTypes.add(canonicalType);
            if (entry.getValue() != null) {
                for (String alias : entry.getValue()) {
                    String normalizedAlias = normalizeCommon(alias);
                    if (normalizedAlias != null) {
                        rawTypes.add(normalizedAlias);
                    }
                }
            }
            rules.add(new TypeEqualityRule(
                    String.join(", ", rawTypes),
                    canonicalType,
                    "以下原始类型在比较时会归一到 " + canonicalType + " 后判定为一致"
            ));
        }
        return rules;
    }

    public record TypeEqualityRule(String rawTypes, String comparableType, String description) {
    }
}
