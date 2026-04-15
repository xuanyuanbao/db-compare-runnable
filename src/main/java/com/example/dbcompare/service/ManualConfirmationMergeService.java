package com.example.dbcompare.service;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.enums.ManualConfirmationMatchStatus;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.domain.model.CompareConfig;
import com.example.dbcompare.domain.model.ManualConfirmationMergeResult;
import com.example.dbcompare.domain.model.ManualConfirmationMergedRow;
import com.example.dbcompare.domain.model.ManualConfirmationRecord;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ManualConfirmationMergeService {
    private final ManualConfirmationExcelParser parser;

    public ManualConfirmationMergeService() {
        this(new ManualConfirmationExcelParser());
    }

    public ManualConfirmationMergeService(ManualConfirmationExcelParser parser) {
        this.parser = parser;
    }

    public ManualConfirmationMergeResult merge(CompareConfig compareConfig, List<ColumnComparisonRecord> aiRecords) {
        String manualExcelPath = compareConfig.getReport().getManualConfirmation().resolveExcelPath();
        List<ManualConfirmationRecord> manualRecords = parser.parse(Path.of(manualExcelPath));
        return merge(aiRecords, manualRecords);
    }

    public ManualConfirmationMergeResult merge(List<ColumnComparisonRecord> aiRecords, List<ManualConfirmationRecord> manualRecords) {
        ManualConfirmationMergeResult result = new ManualConfirmationMergeResult();
        List<ColumnComparisonRecord> filteredAiRecords = aiRecords.stream()
                .filter(record -> record.getOverallStatus() == ComparisonStatus.MISMATCH)
                .toList();

        Map<Integer, ProvisionalMatch> provisionalMatches = new LinkedHashMap<>();
        for (int index = 0; index < filteredAiRecords.size(); index++) {
            ColumnComparisonRecord aiRecord = filteredAiRecords.get(index);
            provisionalMatches.put(index, findMatch(aiRecord, manualRecords));
        }

        Map<Integer, List<Integer>> manualToAiMatches = new HashMap<>();
        for (Map.Entry<Integer, ProvisionalMatch> entry : provisionalMatches.entrySet()) {
            if (entry.getValue().status == ManualConfirmationMatchStatus.MATCHED && entry.getValue().matchedRecord != null) {
                manualToAiMatches.computeIfAbsent(System.identityHashCode(entry.getValue().matchedRecord), ignored -> new ArrayList<>())
                        .add(entry.getKey());
            }
        }

        Set<ManualConfirmationRecord> matchedManualRecords = new HashSet<>();
        Set<ManualConfirmationRecord> ambiguousManualRecords = new HashSet<>();
        for (Map.Entry<Integer, ProvisionalMatch> entry : provisionalMatches.entrySet()) {
            ColumnComparisonRecord aiRecord = filteredAiRecords.get(entry.getKey());
            ProvisionalMatch provisionalMatch = entry.getValue();
            ManualConfirmationMergedRow row = buildMergedRow(aiRecord);
            row.getCandidateRecords().addAll(provisionalMatch.candidates);

            if (provisionalMatch.status == ManualConfirmationMatchStatus.MATCHED && provisionalMatch.matchedRecord != null) {
                List<Integer> conflictingAiIndexes = manualToAiMatches.get(System.identityHashCode(provisionalMatch.matchedRecord));
                if (conflictingAiIndexes != null && conflictingAiIndexes.size() > 1) {
                    row.setMatchStatus(ManualConfirmationMatchStatus.AMBIGUOUS_AI);
                    row.setMatchBasis("同一条测试组记录命中了多条 AI 记录");
                    row.getCandidateRecords().clear();
                    row.getCandidateRecords().add(provisionalMatch.matchedRecord);
                    ambiguousManualRecords.add(provisionalMatch.matchedRecord);
                } else {
                    applyMatchedManualRecord(row, provisionalMatch.matchedRecord, provisionalMatch.matchBasis);
                    matchedManualRecords.add(provisionalMatch.matchedRecord);
                }
            } else {
                row.setMatchStatus(provisionalMatch.status);
                row.setMatchBasis(provisionalMatch.matchBasis);
                if (provisionalMatch.status == ManualConfirmationMatchStatus.AMBIGUOUS_AI) {
                    ambiguousManualRecords.addAll(provisionalMatch.candidates);
                }
            }
            result.getMergedRows().add(row);
        }

        for (ManualConfirmationRecord manualRecord : manualRecords) {
            if (ambiguousManualRecords.contains(manualRecord)) {
                result.getAmbiguousTestRecords().add(manualRecord);
            } else if (!matchedManualRecords.contains(manualRecord)) {
                result.getUnmatchedTestRecords().add(manualRecord);
            }
        }

        result.getMergedRows().sort(Comparator.comparing((ManualConfirmationMergedRow row) -> safe(row.getAiRecord().getSourceDatabaseName()))
                .thenComparing(row -> safe(row.getAiRecord().getSourceSchemaName()))
                .thenComparing(row -> safe(row.getAiRecord().getSourceTableName()))
                .thenComparing(row -> safe(row.getAiRecord().getTargetViewSchemaName()))
                .thenComparing(row -> safe(row.getAiRecord().getTargetViewName()))
                .thenComparing(row -> safe(row.getAiRecord().getColumnName())));
        result.getUnmatchedTestRecords().sort(Comparator.comparing(ManualConfirmationRecord::getSheetName, Comparator.nullsLast(String::compareTo))
                .thenComparingInt(ManualConfirmationRecord::getRowNumber));
        result.getAmbiguousTestRecords().sort(Comparator.comparing(ManualConfirmationRecord::getSheetName, Comparator.nullsLast(String::compareTo))
                .thenComparingInt(ManualConfirmationRecord::getRowNumber));
        return result;
    }

    private ProvisionalMatch findMatch(ColumnComparisonRecord aiRecord, List<ManualConfirmationRecord> manualRecords) {
        Set<String> aiTableNames = aiTableNames(aiRecord);
        Set<String> aiCategories = aiCategories(aiRecord);
        List<ManualConfirmationRecord> tableCandidates = new ArrayList<>();
        for (ManualConfirmationRecord manualRecord : manualRecords) {
            if (aiTableNames.contains(manualRecord.getNormalizedTableName())) {
                tableCandidates.add(manualRecord);
            }
        }
        if (tableCandidates.isEmpty()) {
            return new ProvisionalMatch(ManualConfirmationMatchStatus.UNMATCHED_AI, null, List.of(), "未找到同表名测试组记录");
        }

        List<ManualConfirmationRecord> categoryCandidates = new ArrayList<>();
        for (ManualConfirmationRecord manualRecord : tableCandidates) {
            if (aiCategories.contains(manualRecord.getNormalizedDiffCategory()) || "OTHER".equals(manualRecord.getNormalizedDiffCategory())) {
                categoryCandidates.add(manualRecord);
            }
        }
        List<ManualConfirmationRecord> baseCandidates = categoryCandidates.isEmpty() ? tableCandidates : categoryCandidates;
        if (baseCandidates.size() == 1) {
            return new ProvisionalMatch(ManualConfirmationMatchStatus.MATCHED, baseCandidates.get(0), baseCandidates, "表名+差异类型唯一匹配");
        }

        String normalizedColumnName = normalizeToken(aiRecord.getColumnName());
        if (!normalizedColumnName.isEmpty()) {
            List<ManualConfirmationRecord> columnCandidates = new ArrayList<>();
            for (ManualConfirmationRecord manualRecord : baseCandidates) {
                if (containsToken(manualRecord.getDiffDetailRaw(), normalizedColumnName)) {
                    columnCandidates.add(manualRecord);
                }
            }
            if (columnCandidates.size() == 1) {
                return new ProvisionalMatch(ManualConfirmationMatchStatus.MATCHED, columnCandidates.get(0), columnCandidates, "表名+差异类型+字段名匹配");
            }
            if (columnCandidates.size() > 1) {
                return new ProvisionalMatch(ManualConfirmationMatchStatus.AMBIGUOUS_AI, null, columnCandidates, "多个测试组候选同时命中字段名");
            }
        }

        String aiDetail = normalizeFreeText(aiRecord.getMessage());
        if (!aiDetail.isEmpty()) {
            List<ManualConfirmationRecord> detailCandidates = new ArrayList<>();
            for (ManualConfirmationRecord manualRecord : baseCandidates) {
                String manualDetail = normalizeFreeText(manualRecord.getDiffDetailRaw());
                if (!manualDetail.isEmpty() && (manualDetail.contains(aiDetail) || aiDetail.contains(manualDetail))) {
                    detailCandidates.add(manualRecord);
                }
            }
            if (detailCandidates.size() == 1) {
                return new ProvisionalMatch(ManualConfirmationMatchStatus.MATCHED, detailCandidates.get(0), detailCandidates, "表名+差异类型+差异详情匹配");
            }
            if (detailCandidates.size() > 1) {
                return new ProvisionalMatch(ManualConfirmationMatchStatus.AMBIGUOUS_AI, null, detailCandidates, "多个测试组候选同时命中差异详情");
            }
        }

        return new ProvisionalMatch(ManualConfirmationMatchStatus.AMBIGUOUS_AI, null, baseCandidates, "存在多个测试组候选，无法唯一确定");
    }

    private void applyMatchedManualRecord(ManualConfirmationMergedRow row, ManualConfirmationRecord manualRecord, String matchBasis) {
        row.setMatchStatus(ManualConfirmationMatchStatus.MATCHED);
        row.setMatchBasis(matchBasis);
        row.setOwner(manualRecord.getOwner());
        row.setConfirmResultRaw(manualRecord.getConfirmResultRaw());
        row.setConfirmResultNormalized(manualRecord.getConfirmResultNormalized());
        row.setComment(manualRecord.getComment());
        row.setMatchedSheetName(manualRecord.getSheetName());
        row.setMatchedTableName(manualRecord.getTableName());
        row.setMatchedDiffType(manualRecord.getDiffTypeRaw());
        row.setMatchedDiffDetail(manualRecord.getDiffDetailRaw());
    }

    private ManualConfirmationMergedRow buildMergedRow(ColumnComparisonRecord aiRecord) {
        ManualConfirmationMergedRow row = new ManualConfirmationMergedRow();
        row.setAiRecord(aiRecord);
        row.setAiDiffCategory(String.join(" | ", localizedDiffCategories(aiCategories(aiRecord))));
        row.setAiDiffDetail(aiRecord.getMessage());
        row.setRiskLevel(riskLevel(aiCategories(aiRecord)));
        row.setTargetViewSchema(aiRecord.getTargetViewSchemaName());
        row.setTargetView(aiRecord.getTargetViewName());
        row.setTargetTableSchema(aiRecord.getTargetLineageTableSchemaName());
        row.setTargetTable(aiRecord.getTargetLineageTableName());
        return row;
    }

    private Set<String> aiTableNames(ColumnComparisonRecord aiRecord) {
        Set<String> tableNames = new LinkedHashSet<>();
        addIfPresent(tableNames, aiRecord.getSourceTableName());
        addIfPresent(tableNames, aiRecord.getTargetViewName());
        addIfPresent(tableNames, aiRecord.getTargetTableName());
        splitAndAdd(tableNames, aiRecord.getTargetLineageTableName());
        return tableNames;
    }

    private Set<String> aiCategories(ColumnComparisonRecord aiRecord) {
        Set<String> categories = new LinkedHashSet<>();
        String diffTypes = aiRecord.getDiffTypes();
        if (diffTypes != null && !diffTypes.isBlank()) {
            for (String token : diffTypes.split("\\|")) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.contains("TYPE")) {
                    categories.add("TYPE");
                } else if (trimmed.contains("LENGTH")) {
                    categories.add("LENGTH");
                } else if (trimmed.contains("DEFAULT")) {
                    categories.add("DEFAULT");
                } else if (trimmed.contains("NULLABLE")) {
                    categories.add("NULLABLE");
                } else if (trimmed.contains("MISSING") || trimmed.contains("NOT_FOUND") || trimmed.contains("AMBIGUOUS")) {
                    categories.add("EXISTENCE");
                } else {
                    categories.add("OTHER");
                }
            }
        }
        if (categories.isEmpty()) {
            categories.add("OTHER");
        }
        return categories;
    }

    private List<String> localizedDiffCategories(Set<String> categories) {
        List<String> labels = new ArrayList<>();
        for (String category : categories) {
            switch (category) {
                case "EXISTENCE" -> labels.add("字段存在问题");
                case "TYPE" -> labels.add("字段类型问题");
                case "LENGTH" -> labels.add("字段长度问题");
                case "DEFAULT" -> labels.add("默认值问题");
                case "NULLABLE" -> labels.add("可空性问题");
                default -> labels.add("其他问题");
            }
        }
        return labels;
    }

    private String riskLevel(Set<String> categories) {
        if (categories.contains("EXISTENCE") || categories.contains("TYPE")) {
            return "高";
        }
        if (categories.contains("LENGTH") || categories.contains("DEFAULT") || categories.contains("NULLABLE")) {
            return "中";
        }
        return "低";
    }

    private boolean containsToken(String source, String normalizedToken) {
        return !normalizedToken.isEmpty() && normalizeFreeText(source).contains(normalizedToken);
    }

    private String normalizeFreeText(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (char ch : value.trim().toUpperCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(ch) || ch >= 0x4E00 && ch <= 0x9FFF || ch == '_') {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private String normalizeToken(String value) {
        return normalizeFreeText(value);
    }

    private void splitAndAdd(Set<String> target, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String token : raw.split("\\|")) {
            addIfPresent(target, token);
        }
    }

    private void addIfPresent(Set<String> target, String value) {
        String normalized = normalizeToken(value);
        if (!normalized.isEmpty()) {
            target.add(normalized);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record ProvisionalMatch(ManualConfirmationMatchStatus status,
                                    ManualConfirmationRecord matchedRecord,
                                    List<ManualConfirmationRecord> candidates,
                                    String matchBasis) {
    }
}
