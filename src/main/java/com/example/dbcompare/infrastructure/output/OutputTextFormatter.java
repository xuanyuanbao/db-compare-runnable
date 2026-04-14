package com.example.dbcompare.infrastructure.output;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.enums.DiffType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class OutputTextFormatter {
    private OutputTextFormatter() {
    }

    static String boolText(boolean value) {
        return value ? "是" : "否";
    }

    static String nullableText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "YES", "Y", "TRUE" -> "是";
            case "NO", "N", "FALSE" -> "否";
            default -> value;
        };
    }

    static String comparisonStatusText(ComparisonStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case MATCH -> "一致";
            case MISMATCH -> "不一致";
            case NOT_APPLICABLE -> "不适用";
        };
    }

    static String diffTypeText(DiffType diffType) {
        if (diffType == null) {
            return "";
        }
        return switch (diffType) {
            case SOURCE_TABLE_NOT_FOUND -> "源表不存在";
            case SOURCE_TABLE_AMBIGUOUS -> "源表匹配歧义";
            case TARGET_TABLE_NOT_FOUND -> "目标表不存在";
            case COLUMN_MISSING_IN_SOURCE -> "源端缺字段";
            case COLUMN_MISSING_IN_TARGET -> "目标端缺字段";
            case COLUMN_TYPE_MISMATCH -> "字段类型不一致";
            case COLUMN_LENGTH_MISMATCH -> "字段长度不一致";
            case COLUMN_DEFAULT_MISMATCH -> "默认值不一致";
            case COLUMN_NULLABLE_MISMATCH -> "可空性不一致";
        };
    }

    static String diffTypesText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        List<String> labels = new ArrayList<>();
        for (String token : raw.split("\\|")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                labels.add(diffTypeText(DiffType.valueOf(trimmed)));
            } catch (IllegalArgumentException ignored) {
                labels.add(trimmed);
            }
        }
        return String.join(" | ", labels);
    }

    static String summaryStatusText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return switch (value) {
            case "FULL_EXISTS" -> "完全存在";
            case "NOT_FULL_EXISTS" -> "不完全存在";
            case "TYPE_MATCH" -> "类型一致";
            case "TYPE_MISMATCH" -> "类型不一致";
            case "LENGTH_MATCH" -> "长度一致";
            case "LENGTH_MISMATCH" -> "长度不一致";
            case "DEFAULT_MATCH" -> "默认值一致";
            case "DEFAULT_MISMATCH" -> "默认值不一致";
            case "NULLABLE_MATCH" -> "可空性一致";
            case "NULLABLE_MISMATCH" -> "可空性不一致";
            case "LOW" -> "低";
            case "MEDIUM" -> "中";
            case "HIGH" -> "高";
            case "FULL_MATCH" -> "完全一致";
            case "MISSING_COLUMN" -> "字段缺失";
            case "OTHER" -> "其他差异";
            case "MATCH" -> "一致";
            case "MISMATCH" -> "不一致";
            case "NO_DATA" -> "无数据";
            default -> value;
        };
    }

    static String messageText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replace("Source table not found", "源表不存在")
                .replace("Target table not found", "目标表不存在")
                .replace("source table not found", "源表不存在")
                .replace("target table not found", "目标表不存在")
                .replace("Column exists only in target", "字段仅存在于目标端")
                .replace("Column exists only in source", "字段仅存在于源端")
                .replace("column exists only in target", "字段仅存在于目标端")
                .replace("column exists only in source", "字段仅存在于源端")
                .replace("target column missing", "字段仅存在于源端")
                .replace("source column missing", "字段仅存在于目标端")
                .replace("Type mismatch", "类型不一致")
                .replace("Length mismatch", "长度不一致")
                .replace("Default value mismatch", "默认值不一致")
                .replace("Nullable mismatch", "可空性不一致")
                .replace("type mismatch", "类型不一致")
                .replace("length mismatch", "长度不一致")
                .replace("default value mismatch", "默认值不一致")
                .replace("nullable mismatch", "可空性不一致")
                .replace("MATCH", "一致")
                .replace("MISMATCH", "不一致")
                .replace("Multiple source schemas matched the same table name: ", "多个源Schema匹配到了同一张表：");
    }
}
