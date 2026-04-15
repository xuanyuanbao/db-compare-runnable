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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ManualConfirmationMergeService {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Z0-9_]+");

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
            provisionalMatches.put(index, findMatch(filteredAiRecords.get(index), manualRecords));
        }

        Map<Integer, List<Integer>> manualToAiMatches = new HashMap<>();
        for (Map.Entry<Integer, ProvisionalMatch> entry : provisionalMatches.entrySet()) {
            ProvisionalMatch provisionalMatch = entry.getValue();
            if (provisionalMatch.status == ManualConfirmationMatchStatus.MATCHED && provisionalMatch.matchedRecord != null) {
                manualToAiMatches.computeIfAbsent(System.identityHashCode(provisionalMatch.matchedRecord), ignored -> new ArrayList<>())
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
            row.setCandidateCount(provisionalMatch.candidates.size());

            if (provisionalMatch.status == ManualConfirmationMatchStatus.MATCHED && provisionalMatch.matchedRecord != null) {
                List<Integer> conflictingAiIndexes = manualToAiMatches.get(System.identityHashCode(provisionalMatch.matchedRecord));
                if (conflictingAiIndexes != null && conflictingAiIndexes.size() > 1) {
                    row.setMatchStatus(ManualConfirmationMatchStatus.AMBIGUOUS_AI);
                    row.setMatchBasis("同一条测试组记录命中了多条 AI 差异记录");
                    row.getCandidateRecords().clear();
                    row.getCandidateRecords().add(provisionalMatch.matchedRecord);
                    row.setCandidateCount(1);
                    provisionalMatch.matchedRecord.setAnalysisReason("同一条测试组记录同时命中多条 AI 差异记录");
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
                    for (ManualConfirmationRecord candidate : provisionalMatch.candidates) {
                        candidate.setAnalysisReason("同表同类型候选过多，暂时无法唯一归并到某一条 AI 记录");
                    }
                }
            }
            result.getMergedRows().add(row);
        }

        for (ManualConfirmationRecord manualRecord : manualRecords) {
            if (matchedManualRecords.contains(manualRecord)) {
                continue;
            }
            if (ambiguousManualRecords.contains(manualRecord)) {
                if (isBlank(manualRecord.getAnalysisReason())) {
                    manualRecord.setAnalysisReason(analyzeManualRecord(manualRecord, filteredAiRecords));
                }
                result.getAmbiguousTestRecords().add(manualRecord);
            } else {
                manualRecord.setAnalysisReason(analyzeManualRecord(manualRecord, filteredAiRecords));
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
        Set<String> aiBaseTableNames = baseTableNames(aiTableNames);
        Set<String> aiCategories = aiCategories(aiRecord);

        List<ManualConfirmationRecord> exactTableCandidates = new ArrayList<>();
        for (ManualConfirmationRecord manualRecord : manualRecords) {
            if (aiTableNames.contains(manualRecord.getNormalizedTableName())) {
                exactTableCandidates.add(manualRecord);
            }
        }
        List<ManualConfirmationRecord> tableCandidates = exactTableCandidates;
        String tableStage = "表名精确匹配";
        if (tableCandidates.isEmpty()) {
            List<ManualConfirmationRecord> relaxedTableCandidates = new ArrayList<>();
            for (ManualConfirmationRecord manualRecord : manualRecords) {
                if (aiBaseTableNames.contains(normalizeBaseName(manualRecord.getNormalizedTableName()))) {
                    relaxedTableCandidates.add(manualRecord);
                }
            }
            tableCandidates = relaxedTableCandidates;
            tableStage = "表名放宽匹配";
        }
        if (tableCandidates.isEmpty()) {
            return new ProvisionalMatch(
                    ManualConfirmationMatchStatus.UNMATCHED_AI,
                    null,
                    List.of(),
                    "未找到同表对象的测试组记录");
        }

        List<ManualConfirmationRecord> exactCategoryCandidates = new ArrayList<>();
        List<ManualConfirmationRecord> fallbackOtherCandidates = new ArrayList<>();
        for (ManualConfirmationRecord manualRecord : tableCandidates) {
            if (aiCategories.contains(manualRecord.getNormalizedDiffCategory())) {
                exactCategoryCandidates.add(manualRecord);
            } else if ("OTHER".equals(manualRecord.getNormalizedDiffCategory())) {
                fallbackOtherCandidates.add(manualRecord);
            }
        }
        List<ManualConfirmationRecord> baseCandidates = !exactCategoryCandidates.isEmpty()
                ? exactCategoryCandidates
                : fallbackOtherCandidates;
        if (baseCandidates.isEmpty()) {
            return new ProvisionalMatch(
                    ManualConfirmationMatchStatus.UNMATCHED_AI,
                    null,
                    tableCandidates,
                    "已找到同表对象，但差异类型未对齐");
        }
        if (baseCandidates.size() == 1) {
            String basis = tableStage + (!exactCategoryCandidates.isEmpty() ? " + 差异类型唯一匹配" : " + 差异类型模糊兜底");
            return new ProvisionalMatch(ManualConfirmationMatchStatus.MATCHED, baseCandidates.get(0), baseCandidates, basis);
        }

        List<ScoredCandidate> scoredCandidates = scoreCandidates(aiRecord, aiCategories, baseCandidates);
        if (!scoredCandidates.isEmpty() && scoredCandidates.get(0).score > 0) {
            if (scoredCandidates.size() == 1 || scoredCandidates.get(0).score > scoredCandidates.get(1).score) {
                return new ProvisionalMatch(
                        ManualConfirmationMatchStatus.MATCHED,
                        scoredCandidates.get(0).record,
                        baseCandidates,
                        tableStage + " + 字段/详情增强匹配");
            }
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
                return new ProvisionalMatch(ManualConfirmationMatchStatus.MATCHED, columnCandidates.get(0), columnCandidates, tableStage + " + 字段名唯一匹配");
            }
            if (columnCandidates.size() > 1) {
                return new ProvisionalMatch(ManualConfirmationMatchStatus.AMBIGUOUS_AI, null, columnCandidates, "同表同类型候选过多，字段名仍无法唯一定位");
            }
        }

        return new ProvisionalMatch(ManualConfirmationMatchStatus.AMBIGUOUS_AI, null, baseCandidates, "同表同类型候选过多，当前信息不足以唯一定位");
    }

    private List<ScoredCandidate> scoreCandidates(ColumnComparisonRecord aiRecord,
                                                  Set<String> aiCategories,
                                                  List<ManualConfirmationRecord> baseCandidates) {
        String normalizedColumnName = normalizeToken(aiRecord.getColumnName());
        Set<String> aiDetailTokens = aiDetailTokens(aiRecord);
        List<ScoredCandidate> scored = new ArrayList<>();
        for (ManualConfirmationRecord manualRecord : baseCandidates) {
            int score = 0;
            if (aiCategories.contains(manualRecord.getNormalizedDiffCategory())) {
                score += 100;
            } else if ("OTHER".equals(manualRecord.getNormalizedDiffCategory())) {
                score += 40;
            }
            if (!normalizedColumnName.isEmpty() && containsToken(manualRecord.getDiffDetailRaw(), normalizedColumnName)) {
                score += 60;
            }
            Set<String> manualTokens = extractTokens(manualRecord.getDiffDetailRaw());
            int overlap = 0;
            for (String token : aiDetailTokens) {
                if (manualTokens.contains(token)) {
                    overlap++;
                }
            }
            score += overlap * 15;
            scored.add(new ScoredCandidate(manualRecord, score));
        }
        scored.sort(Comparator.comparingInt(ScoredCandidate::score).reversed());
        return scored;
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

    private String analyzeManualRecord(ManualConfirmationRecord manualRecord, List<ColumnComparisonRecord> aiRecords) {
        List<ColumnComparisonRecord> tableCandidates = new ArrayList<>();
        String manualTableName = manualRecord.getNormalizedTableName();
        String manualBaseName = normalizeBaseName(manualTableName);
        for (ColumnComparisonRecord aiRecord : aiRecords) {
            Set<String> aiTableNames = aiTableNames(aiRecord);
            if (aiTableNames.contains(manualTableName) || baseTableNames(aiTableNames).contains(manualBaseName)) {
                tableCandidates.add(aiRecord);
            }
        }
        if (tableCandidates.isEmpty()) {
            return "未找到同表对象的 AI 差异记录";
        }

        List<ColumnComparisonRecord> categoryCandidates = new ArrayList<>();
        for (ColumnComparisonRecord aiRecord : tableCandidates) {
            if (aiCategories(aiRecord).contains(manualRecord.getNormalizedDiffCategory())
                    || "OTHER".equals(manualRecord.getNormalizedDiffCategory())) {
                categoryCandidates.add(aiRecord);
            }
        }
        if (categoryCandidates.isEmpty()) {
            return "已找到同表对象，但差异类型未对齐";
        }

        Set<String> manualFieldTokens = extractTokens(manualRecord.getDiffDetailRaw());
        if (manualFieldTokens.isEmpty()) {
            return categoryCandidates.size() == 1
                    ? "已找到唯一 AI 候选，但该记录被其他测试组记录占用或发生冲突"
                    : "同表同类型候选过多，字段信息不足以唯一定位";
        }

        List<ColumnComparisonRecord> fieldCandidates = new ArrayList<>();
        for (ColumnComparisonRecord aiRecord : categoryCandidates) {
            String aiColumn = normalizeToken(aiRecord.getColumnName());
            if (!aiColumn.isEmpty() && manualFieldTokens.contains(aiColumn)) {
                fieldCandidates.add(aiRecord);
            }
        }
        if (fieldCandidates.isEmpty()) {
            return "同表同类型候选存在，但人工明细里的字段信息无法命中 AI 结果";
        }
        if (fieldCandidates.size() == 1) {
            return "已找到唯一 AI 候选，但该记录被其他测试组记录占用或发生冲突";
        }
        return "同表同类型候选过多，字段信息仍对应多条 AI 记录";
    }

    private Set<String> aiTableNames(ColumnComparisonRecord aiRecord) {
        Set<String> tableNames = new LinkedHashSet<>();
        addIfPresent(tableNames, aiRecord.getSourceTableName());
        addIfPresent(tableNames, aiRecord.getTargetViewName());
        addIfPresent(tableNames, aiRecord.getTargetTableName());
        splitAndAdd(tableNames, aiRecord.getTargetLineageTableName());
        return tableNames;
    }

    private Set<String> baseTableNames(Set<String> tableNames) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String tableName : tableNames) {
            normalized.add(normalizeBaseName(tableName));
        }
        return normalized;
    }

    private String normalizeBaseName(String value) {
        String normalized = normalizeToken(value);
        if (normalized.endsWith("_VIEW")) {
            return normalized.substring(0, normalized.length() - 5);
        }
        if (normalized.endsWith("VIEW")) {
            return normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
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

    private Set<String> aiDetailTokens(ColumnComparisonRecord aiRecord) {
        Set<String> tokens = new LinkedHashSet<>();
        addIfPresent(tokens, aiRecord.getColumnName());
        addIfPresent(tokens, aiRecord.getSourceType());
        addIfPresent(tokens, aiRecord.getTargetType());
        addIfPresent(tokens, aiRecord.getSourceLength());
        addIfPresent(tokens, aiRecord.getTargetLength());
        addIfPresent(tokens, aiRecord.getSourceDefaultValue());
        addIfPresent(tokens, aiRecord.getTargetDefaultValue());
        addIfPresent(tokens, aiRecord.getSourceNullable());
        addIfPresent(tokens, aiRecord.getTargetNullable());
        tokens.addAll(extractTokens(aiRecord.getMessage()));
        return tokens;
    }

    private Set<String> extractTokens(String raw) {
        Set<String> tokens = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) {
            return tokens;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(raw.toUpperCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
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
            if (Character.isLetterOrDigit(ch) || (ch >= 0x4E00 && ch <= 0x9FFF) || ch == '_') {
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record ScoredCandidate(ManualConfirmationRecord record, int score) {
    }

    private record ProvisionalMatch(ManualConfirmationMatchStatus status,
                                    ManualConfirmationRecord matchedRecord,
                                    List<ManualConfirmationRecord> candidates,
                                    String matchBasis) {
    }
}
