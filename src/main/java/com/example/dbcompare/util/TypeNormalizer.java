package com.example.dbcompare.util;

import com.example.dbcompare.domain.enums.DatabaseType;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeNormalizer {
    private static final List<String> AS400_RULE_SEEDS = List.of(
            "CHAR", "CHARACTER", "GRAPHIC", "VARCHAR", "CHARACTER VARYING", "VARGRAPHIC",
            "SMALLINT", "INTEGER", "BIGINT", "DECIMAL", "NUMERIC", "DATE", "TIMESTAMP", "INT", "NUMBER"
    );
    private static final List<String> GAUSS_RULE_SEEDS = List.of(
            "CHAR", "CHARACTER", "VARCHAR", "CHARACTER VARYING", "SMALLINT", "INTEGER", "INT4", "BIGINT",
            "DECIMAL", "NUMERIC", "NUMBER", "DATE", "TIMESTAMP", "INT"
    );

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

    public List<TypeEqualityRule> describeAs400GaussRules(Map<String, List<String>> customTypeMappings) {
        Map<String, Set<String>> as400Groups = collectGroups(DatabaseType.AS400, AS400_RULE_SEEDS, customTypeMappings);
        Map<String, Set<String>> gaussGroups = collectGroups(DatabaseType.GAUSS, GAUSS_RULE_SEEDS, customTypeMappings);
        List<TypeEqualityRule> rules = new java.util.ArrayList<>();
        LinkedHashSet<String> canonicalTypes = new LinkedHashSet<>();
        canonicalTypes.addAll(as400Groups.keySet());
        canonicalTypes.addAll(gaussGroups.keySet());
        for (String canonicalType : canonicalTypes) {
            Set<String> as400Types = as400Groups.get(canonicalType);
            Set<String> gaussTypes = gaussGroups.get(canonicalType);
            if (as400Types == null || as400Types.isEmpty() || gaussTypes == null || gaussTypes.isEmpty()) {
                continue;
            }
            rules.add(new TypeEqualityRule(
                    join(as400Types),
                    join(gaussTypes),
                    canonicalType,
                    canonicalType + " 后判定为一致"
            ));
        }
        return rules;
    }

    private Map<String, Set<String>> collectGroups(DatabaseType databaseType,
                                                   List<String> rawTypes,
                                                   Map<String, List<String>> customTypeMappings) {
        Map<String, Set<String>> groups = new LinkedHashMap<>();
        for (String rawType : rawTypes) {
            String canonicalType = normalize(databaseType, rawType, customTypeMappings);
            if (canonicalType == null) {
                continue;
            }
            groups.computeIfAbsent(canonicalType, ignored -> new LinkedHashSet<>()).add(rawType);
        }
        return groups;
    }

    private String join(Set<String> values) {
        return String.join(", ", values);
    }

    public record TypeEqualityRule(String as400Types, String gaussTypes, String comparableType, String description) {
    }
}
