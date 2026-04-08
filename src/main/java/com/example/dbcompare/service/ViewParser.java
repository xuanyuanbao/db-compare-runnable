package com.example.dbcompare.service;

import com.example.dbcompare.util.NameNormalizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViewParser {
    private static final Pattern FROM_PATTERN = Pattern.compile("(?i)\\bfrom\\s+((?:\"?[A-Za-z0-9_]+\"?\\.)?(\"?[A-Za-z0-9_]+\"?))");

    public String resolveBaseTableName(String viewName, String viewDefinition) {
        if (viewDefinition != null && !viewDefinition.isBlank()) {
            Matcher matcher = FROM_PATTERN.matcher(viewDefinition);
            if (matcher.find()) {
                String candidate = matcher.group(2);
                if (candidate != null) {
                    return NameNormalizer.normalize(stripQuotes(candidate));
                }
            }
        }
        return NameNormalizer.normalize(viewName);
    }

    private String stripQuotes(String value) {
        String trimmed = value == null ? null : value.trim();
        if (trimmed == null) {
            return null;
        }
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}