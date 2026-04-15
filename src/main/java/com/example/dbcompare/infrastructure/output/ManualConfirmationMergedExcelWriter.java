package com.example.dbcompare.infrastructure.output;

import com.example.dbcompare.domain.enums.ManualConfirmationMatchStatus;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.domain.model.CompareConfig;
import com.example.dbcompare.domain.model.ManualConfirmationMergeResult;
import com.example.dbcompare.domain.model.ManualConfirmationMergedRow;
import com.example.dbcompare.domain.model.ManualConfirmationRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ManualConfirmationMergedExcelWriter {
    private static final String[] MERGED_HEADERS = {
            "源数据库", "源Schema", "源表", "目标ViewSchema", "目标View", "目标TableSchema", "目标Table",
            "字段名", "AI差异类型", "AI差异详情", "风险等级",
            "责任人", "确认结果_原始值", "确认结果_标准值", "附加说明",
            "匹配状态", "匹配依据", "候选数", "匹配来源sheet", "测试组原始表名", "测试组原始不一致类型", "测试组原始不一致详细"
    };

    private static final String[] TABLE_STATUS_HEADERS = {
            "源数据库", "源Schema", "源表", "目标ViewSchema", "目标View", "目标TableSchema", "目标Table",
            "差异总数", "已匹配数", "未匹配数", "歧义数", "匹配率", "责任人", "主要确认结果", "风险等级"
    };

    private static final String[] SUMMARY_HEADERS = {"项目", "数量", "占比"};
    private static final String[] OWNER_HEADERS = {"责任人", "问题总数", "已匹配数", "待确认数", "无影响数", "已修复数", "已废弃数", "匹配率", "已确认率"};
    private static final String[] UNMATCHED_AI_HEADERS = {"源数据库", "源Schema", "源表", "目标ViewSchema", "目标View", "目标TableSchema", "目标Table", "字段名", "AI差异类型", "AI差异详情", "风险等级", "候选数", "未匹配原因"};
    private static final String[] UNMATCHED_TEST_HEADERS = {"来源sheet", "行号", "表名", "责任人", "不一致类型", "不一致详细", "确认结果", "附加说明", "分析原因"};
    private static final String[] AMBIGUOUS_HEADERS = {"记录类型", "来源sheet", "行号/字段", "表名", "不一致类型", "不一致详细", "候选数", "说明"};

    public void write(Path path, CompareConfig compareConfig, ManualConfirmationMergeResult mergeResult) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (XSSFWorkbook workbook = new XSSFWorkbook();
                 OutputStream outputStream = Files.newOutputStream(path)) {
                Styles styles = createStyles(workbook);
                writeInstructionSheet(workbook, styles, compareConfig, mergeResult);
                writeMergedSheet(workbook, styles, mergeResult);
                writeTableStatusSheet(workbook, styles, mergeResult);
                writeConfirmResultSummarySheet(workbook, styles, mergeResult);
                writeOwnerSummarySheet(workbook, styles, mergeResult);
                writeUnmatchedAiSheet(workbook, styles, mergeResult);
                writeUnmatchedTestSheet(workbook, styles, mergeResult);
                writeAmbiguousSheet(workbook, styles, mergeResult);
                workbook.write(outputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write manual confirmation merged Excel: " + path, e);
        }
    }

    private void writeInstructionSheet(XSSFWorkbook workbook,
                                       Styles styles,
                                       CompareConfig compareConfig,
                                       ManualConfirmationMergeResult mergeResult) {
        var sheet = workbook.createSheet("说明");
        setColumnWidths(sheet, 0, 0, 28);
        setColumnWidths(sheet, 1, 1, 90);

        int rowIndex = 0;
        rowIndex = writePair(sheet, rowIndex, "数据来源", "AI 结构化差异结果 + 测试组人工确认 Excel", styles);
        rowIndex = writePair(sheet, rowIndex, "融合开关", compareConfig.getReport().getManualConfirmation().isEnabled() ? "开启" : "关闭", styles);
        rowIndex = writePair(sheet, rowIndex, "测试组Excel路径", compareConfig.getReport().getManualConfirmation().resolveExcelPath(), styles);
        rowIndex = writePair(sheet, rowIndex, "融合输出路径", compareConfig.getOutput().getManualConfirmationExcelPath(), styles);
        rowIndex = writePair(sheet, rowIndex, "AI差异记录数", Integer.toString(mergeResult.getMergedRows().size()), styles);
        rowIndex = writePair(sheet, rowIndex, "已匹配记录数", Integer.toString(countMergedRows(mergeResult, ManualConfirmationMatchStatus.MATCHED)), styles);
        rowIndex = writePair(sheet, rowIndex, "未匹配AI记录数", Integer.toString(countMergedRows(mergeResult, ManualConfirmationMatchStatus.UNMATCHED_AI)), styles);
        rowIndex = writePair(sheet, rowIndex, "歧义AI记录数", Integer.toString(countMergedRows(mergeResult, ManualConfirmationMatchStatus.AMBIGUOUS_AI)), styles);
        rowIndex = writePair(sheet, rowIndex, "未匹配测试组记录数", Integer.toString(mergeResult.getUnmatchedTestRecords().size()), styles);
        rowIndex = writePair(sheet, rowIndex, "歧义测试组记录数", Integer.toString(mergeResult.getAmbiguousTestRecords().size()), styles);
        writePair(sheet, rowIndex, "匹配策略", "优先按表名匹配，再按差异类型、字段名和差异详情增强匹配；未匹配与歧义记录会单独输出原因。", styles);
    }

    private void writeMergedSheet(XSSFWorkbook workbook, Styles styles, ManualConfirmationMergeResult mergeResult) {
        var sheet = workbook.createSheet("人工确认融合明细");
        writeHeaders(sheet, 0, MERGED_HEADERS, styles.header);
        int rowIndex = 1;
        for (ManualConfirmationMergedRow row : mergeResult.getMergedRows()) {
            Row excelRow = sheet.createRow(rowIndex++);
            ColumnComparisonRecord ai = row.getAiRecord();
            int columnIndex = 0;
            writeCell(excelRow, columnIndex++, ai.getSourceDatabaseName(), styles.body);
            writeCell(excelRow, columnIndex++, ai.getSourceSchemaName(), styles.body);
            writeCell(excelRow, columnIndex++, ai.getSourceTableName(), styles.body);
            writeCell(excelRow, columnIndex++, row.getTargetViewSchema(), styles.body);
            writeCell(excelRow, columnIndex++, row.getTargetView(), styles.body);
            writeCell(excelRow, columnIndex++, row.getTargetTableSchema(), styles.body);
            writeCell(excelRow, columnIndex++, row.getTargetTable(), styles.body);
            writeCell(excelRow, columnIndex++, ai.getColumnName(), styles.body);
            writeCell(excelRow, columnIndex++, row.getAiDiffCategory(), styles.body);
            writeCell(excelRow, columnIndex++, OutputTextFormatter.messageText(row.getAiDiffDetail()), styles.body);
            writeCell(excelRow, columnIndex++, row.getRiskLevel(), riskStyle(styles, row.getRiskLevel()));
            writeCell(excelRow, columnIndex++, row.getOwner(), styles.body);
            writeCell(excelRow, columnIndex++, row.getConfirmResultRaw(), styles.body);
            writeCell(excelRow, columnIndex++, row.getConfirmResultNormalized(), styles.body);
            writeCell(excelRow, columnIndex++, row.getComment(), styles.body);
            writeCell(excelRow, columnIndex++, matchStatusText(row.getMatchStatus()), matchStatusStyle(styles, row.getMatchStatus()));
            writeCell(excelRow, columnIndex++, row.getMatchBasis(), styles.body);
            writeCell(excelRow, columnIndex++, Integer.toString(row.getCandidateCount()), styles.body);
            writeCell(excelRow, columnIndex++, row.getMatchedSheetName(), styles.body);
            writeCell(excelRow, columnIndex++, row.getMatchedTableName(), styles.body);
            writeCell(excelRow, columnIndex++, row.getMatchedDiffType(), styles.body);
            writeCell(excelRow, columnIndex, row.getMatchedDiffDetail(), styles.body);
        }
        autosize(sheet, MERGED_HEADERS.length);
    }

    private void writeTableStatusSheet(XSSFWorkbook workbook, Styles styles, ManualConfirmationMergeResult mergeResult) {
        var sheet = workbook.createSheet("表级融合状态");
        writeHeaders(sheet, 0, TABLE_STATUS_HEADERS, styles.header);
        Map<String, TableSummary> summaries = new LinkedHashMap<>();
        for (ManualConfirmationMergedRow row : mergeResult.getMergedRows()) {
            ColumnComparisonRecord ai = row.getAiRecord();
            String key = String.join("|",
                    safe(ai.getSourceDatabaseName()),
                    safe(ai.getSourceSchemaName()),
                    safe(ai.getSourceTableName()),
                    safe(row.getTargetViewSchema()),
                    safe(row.getTargetView()),
                    safe(row.getTargetTableSchema()),
                    safe(row.getTargetTable()));
            summaries.computeIfAbsent(key, ignored -> new TableSummary(ai, row)).accept(row);
        }

        int rowIndex = 1;
        for (TableSummary summary : summaries.values()) {
            Row excelRow = sheet.createRow(rowIndex++);
            int columnIndex = 0;
            writeCell(excelRow, columnIndex++, summary.sourceDatabase, styles.body);
            writeCell(excelRow, columnIndex++, summary.sourceSchema, styles.body);
            writeCell(excelRow, columnIndex++, summary.sourceTable, styles.body);
            writeCell(excelRow, columnIndex++, summary.targetViewSchema, styles.body);
            writeCell(excelRow, columnIndex++, summary.targetView, styles.body);
            writeCell(excelRow, columnIndex++, summary.targetTableSchema, styles.body);
            writeCell(excelRow, columnIndex++, summary.targetTable, styles.body);
            writeCell(excelRow, columnIndex++, Integer.toString(summary.totalCount), styles.body);
            writeCell(excelRow, columnIndex++, Integer.toString(summary.matchedCount), styles.body);
            writeCell(excelRow, columnIndex++, Integer.toString(summary.unmatchedCount), styles.body);
            writeCell(excelRow, columnIndex++, Integer.toString(summary.ambiguousCount), styles.body);
            writeCell(excelRow, columnIndex++, percentage(summary.matchedCount, summary.totalCount), styles.body);
            writeCell(excelRow, columnIndex++, join(summary.owners), styles.body);
            writeCell(excelRow, columnIndex++, join(summary.confirmResults), styles.body);
            writeCell(excelRow, columnIndex, summary.riskLevel, riskStyle(styles, summary.riskLevel));
        }
        autosize(sheet, TABLE_STATUS_HEADERS.length);
    }

    private void writeConfirmResultSummarySheet(XSSFWorkbook workbook, Styles styles, ManualConfirmationMergeResult mergeResult) {
        var sheet = workbook.createSheet("确认结果汇总");
        writeHeaders(sheet, 0, SUMMARY_HEADERS, styles.header);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ManualConfirmationMergedRow row : mergeResult.getMergedRows()) {
            if (row.getMatchStatus() != ManualConfirmationMatchStatus.MATCHED) {
                continue;
            }
            String key = row.getConfirmResultNormalized();
            if (key == null || key.isBlank()) {
                key = "未提供";
            }
            counts.merge(key, 1, Integer::sum);
        }
        writeSummaryRows(sheet, counts, styles);
    }

    private void writeOwnerSummarySheet(XSSFWorkbook workbook, Styles styles, ManualConfirmationMergeResult mergeResult) {
        var sheet = workbook.createSheet("责任人汇总");
        writeHeaders(sheet, 0, OWNER_HEADERS, styles.header);
        Map<String, OwnerSummary> summaries = new LinkedHashMap<>();
        for (ManualConfirmationMergedRow row : mergeResult.getMergedRows()) {
            String owner = row.getOwner();
            if (owner == null || owner.isBlank()) {
                owner = "未分配";
            }
            summaries.computeIfAbsent(owner, OwnerSummary::new).accept(row);
        }

        int rowIndex = 1;
        for (OwnerSummary summary : summaries.values()) {
            Row excelRow = sheet.createRow(rowIndex++);
            writeCell(excelRow, 0, summary.owner, styles.body);
            writeCell(excelRow, 1, Integer.toString(summary.totalCount), styles.body);
            writeCell(excelRow, 2, Integer.toString(summary.matchedCount), styles.body);
            writeCell(excelRow, 3, Integer.toString(summary.pendingCount), styles.body);
            writeCell(excelRow, 4, Integer.toString(summary.noImpactCount), styles.body);
            writeCell(excelRow, 5, Integer.toString(summary.fixedCount), styles.body);
            writeCell(excelRow, 6, Integer.toString(summary.abandonedCount), styles.body);
            writeCell(excelRow, 7, percentage(summary.matchedCount, summary.totalCount), styles.body);
            writeCell(excelRow, 8, percentage(summary.confirmedCount(), summary.totalCount), styles.body);
        }
        autosize(sheet, OWNER_HEADERS.length);
    }

    private void writeUnmatchedAiSheet(XSSFWorkbook workbook, Styles styles, ManualConfirmationMergeResult mergeResult) {
        var sheet = workbook.createSheet("未匹配AI记录");
        writeHeaders(sheet, 0, UNMATCHED_AI_HEADERS, styles.header);
        int rowIndex = 1;
        for (ManualConfirmationMergedRow row : mergeResult.getMergedRows()) {
            if (row.getMatchStatus() != ManualConfirmationMatchStatus.UNMATCHED_AI) {
                continue;
            }
            Row excelRow = sheet.createRow(rowIndex++);
            ColumnComparisonRecord ai = row.getAiRecord();
            writeCell(excelRow, 0, ai.getSourceDatabaseName(), styles.body);
            writeCell(excelRow, 1, ai.getSourceSchemaName(), styles.body);
            writeCell(excelRow, 2, ai.getSourceTableName(), styles.body);
            writeCell(excelRow, 3, row.getTargetViewSchema(), styles.body);
            writeCell(excelRow, 4, row.getTargetView(), styles.body);
            writeCell(excelRow, 5, row.getTargetTableSchema(), styles.body);
            writeCell(excelRow, 6, row.getTargetTable(), styles.body);
            writeCell(excelRow, 7, ai.getColumnName(), styles.body);
            writeCell(excelRow, 8, row.getAiDiffCategory(), styles.body);
            writeCell(excelRow, 9, OutputTextFormatter.messageText(row.getAiDiffDetail()), styles.body);
            writeCell(excelRow, 10, row.getRiskLevel(), riskStyle(styles, row.getRiskLevel()));
            writeCell(excelRow, 11, Integer.toString(row.getCandidateCount()), styles.body);
            writeCell(excelRow, 12, row.getMatchBasis(), styles.body);
        }
        autosize(sheet, UNMATCHED_AI_HEADERS.length);
    }

    private void writeUnmatchedTestSheet(XSSFWorkbook workbook, Styles styles, ManualConfirmationMergeResult mergeResult) {
        var sheet = workbook.createSheet("未匹配测试组记录");
        writeHeaders(sheet, 0, UNMATCHED_TEST_HEADERS, styles.header);
        int rowIndex = 1;
        for (ManualConfirmationRecord record : mergeResult.getUnmatchedTestRecords()) {
            Row excelRow = sheet.createRow(rowIndex++);
            writeCell(excelRow, 0, record.getSheetName(), styles.body);
            writeCell(excelRow, 1, Integer.toString(record.getRowNumber()), styles.body);
            writeCell(excelRow, 2, record.getTableName(), styles.body);
            writeCell(excelRow, 3, record.getOwner(), styles.body);
            writeCell(excelRow, 4, record.getDiffTypeRaw(), styles.body);
            writeCell(excelRow, 5, record.getDiffDetailRaw(), styles.body);
            writeCell(excelRow, 6, record.getConfirmResultRaw(), styles.body);
            writeCell(excelRow, 7, record.getComment(), styles.body);
            writeCell(excelRow, 8, record.getAnalysisReason(), styles.body);
        }
        autosize(sheet, UNMATCHED_TEST_HEADERS.length);
    }

    private void writeAmbiguousSheet(XSSFWorkbook workbook, Styles styles, ManualConfirmationMergeResult mergeResult) {
        var sheet = workbook.createSheet("歧义匹配记录");
        writeHeaders(sheet, 0, AMBIGUOUS_HEADERS, styles.header);
        int rowIndex = 1;
        for (ManualConfirmationMergedRow row : mergeResult.getMergedRows()) {
            if (row.getMatchStatus() != ManualConfirmationMatchStatus.AMBIGUOUS_AI) {
                continue;
            }
            Row excelRow = sheet.createRow(rowIndex++);
            writeCell(excelRow, 0, "AI", styles.body);
            writeCell(excelRow, 1, row.getMatchedSheetName(), styles.body);
            writeCell(excelRow, 2, row.getAiRecord().getColumnName(), styles.body);
            writeCell(excelRow, 3, row.getAiRecord().getSourceTableName(), styles.body);
            writeCell(excelRow, 4, row.getAiDiffCategory(), styles.body);
            writeCell(excelRow, 5, row.getAiDiffDetail(), styles.body);
            writeCell(excelRow, 6, Integer.toString(row.getCandidateCount()), styles.body);
            writeCell(excelRow, 7, row.getMatchBasis(), styles.body);
        }
        for (ManualConfirmationRecord record : mergeResult.getAmbiguousTestRecords()) {
            Row excelRow = sheet.createRow(rowIndex++);
            writeCell(excelRow, 0, "TEST", styles.body);
            writeCell(excelRow, 1, record.getSheetName(), styles.body);
            writeCell(excelRow, 2, Integer.toString(record.getRowNumber()), styles.body);
            writeCell(excelRow, 3, record.getTableName(), styles.body);
            writeCell(excelRow, 4, record.getDiffTypeRaw(), styles.body);
            writeCell(excelRow, 5, record.getDiffDetailRaw(), styles.body);
            writeCell(excelRow, 6, "多候选", styles.body);
            writeCell(excelRow, 7, record.getAnalysisReason(), styles.body);
        }
        autosize(sheet, AMBIGUOUS_HEADERS.length);
    }

    private int writePair(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, String key, String value, Styles styles) {
        Row row = sheet.createRow(rowIndex);
        writeCell(row, 0, key, styles.header);
        writeCell(row, 1, value, styles.body);
        return rowIndex + 1;
    }

    private void writeSummaryRows(org.apache.poi.ss.usermodel.Sheet sheet, Map<String, Integer> counts, Styles styles) {
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        int rowIndex = 1;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            Row row = sheet.createRow(rowIndex++);
            writeCell(row, 0, entry.getKey(), styles.body);
            writeCell(row, 1, Integer.toString(entry.getValue()), styles.body);
            writeCell(row, 2, percentage(entry.getValue(), total), styles.body);
        }
        autosize(sheet, SUMMARY_HEADERS.length);
    }

    private int countMergedRows(ManualConfirmationMergeResult mergeResult, ManualConfirmationMatchStatus status) {
        int count = 0;
        for (ManualConfirmationMergedRow row : mergeResult.getMergedRows()) {
            if (row.getMatchStatus() == status) {
                count++;
            }
        }
        return count;
    }

    private String percentage(int numerator, int denominator) {
        if (denominator <= 0) {
            return "0.00%";
        }
        return String.format(Locale.ROOT, "%.2f%%", numerator * 100.0 / denominator);
    }

    private CellStyle riskStyle(Styles styles, String riskLevel) {
        if ("高".equals(riskLevel)) {
            return styles.highRisk;
        }
        if ("中".equals(riskLevel)) {
            return styles.mediumRisk;
        }
        return styles.lowRisk;
    }

    private CellStyle matchStatusStyle(Styles styles, ManualConfirmationMatchStatus status) {
        return switch (status) {
            case MATCHED -> styles.matched;
            case UNMATCHED_AI, UNMATCHED_TEST -> styles.unmatched;
            case AMBIGUOUS_AI, AMBIGUOUS_TEST -> styles.ambiguous;
        };
    }

    private String matchStatusText(ManualConfirmationMatchStatus status) {
        return switch (status) {
            case MATCHED -> "已匹配";
            case UNMATCHED_AI -> "AI未匹配";
            case AMBIGUOUS_AI -> "AI歧义匹配";
            case UNMATCHED_TEST -> "测试组未匹配";
            case AMBIGUOUS_TEST -> "测试组歧义匹配";
        };
    }

    private void writeHeaders(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, String[] headers, CellStyle headerStyle) {
        Row row = sheet.createRow(rowIndex);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void autosize(org.apache.poi.ss.usermodel.Sheet sheet, int columnCount) {
        for (int index = 0; index < columnCount; index++) {
            int headerLength = sheet.getRow(0).getCell(index).getStringCellValue().length();
            sheet.setColumnWidth(index, Math.min(90, Math.max(14, headerLength + 4)) * 256);
        }
    }

    private void setColumnWidths(org.apache.poi.ss.usermodel.Sheet sheet, int from, int to, int widthChars) {
        for (int index = from; index <= to; index++) {
            sheet.setColumnWidth(index, widthChars * 256);
        }
    }

    private Styles createStyles(XSSFWorkbook workbook) {
        CellStyle header = workbook.createCellStyle();
        Font bold = workbook.createFont();
        bold.setBold(true);
        header.setFont(bold);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle body = workbook.createCellStyle();
        CellStyle matched = fill(workbook, IndexedColors.LIGHT_GREEN);
        CellStyle unmatched = fill(workbook, IndexedColors.LIGHT_YELLOW);
        CellStyle ambiguous = fill(workbook, IndexedColors.ROSE);
        CellStyle lowRisk = fill(workbook, IndexedColors.LIGHT_GREEN);
        CellStyle mediumRisk = fill(workbook, IndexedColors.LIGHT_ORANGE);
        CellStyle highRisk = fill(workbook, IndexedColors.CORAL);
        return new Styles(header, body, matched, unmatched, ambiguous, lowRisk, mediumRisk, highRisk);
    }

    private CellStyle fill(XSSFWorkbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String join(Set<String> values) {
        if (values.isEmpty()) {
            return "";
        }
        return String.join(" | ", values);
    }

    private record Styles(CellStyle header,
                          CellStyle body,
                          CellStyle matched,
                          CellStyle unmatched,
                          CellStyle ambiguous,
                          CellStyle lowRisk,
                          CellStyle mediumRisk,
                          CellStyle highRisk) {
    }

    private static final class TableSummary {
        private final String sourceDatabase;
        private final String sourceSchema;
        private final String sourceTable;
        private final String targetViewSchema;
        private final String targetView;
        private final String targetTableSchema;
        private final String targetTable;
        private final Set<String> owners = new LinkedHashSet<>();
        private final Set<String> confirmResults = new LinkedHashSet<>();
        private int totalCount;
        private int matchedCount;
        private int unmatchedCount;
        private int ambiguousCount;
        private String riskLevel = "低";

        private TableSummary(ColumnComparisonRecord aiRecord, ManualConfirmationMergedRow mergedRow) {
            this.sourceDatabase = safe(aiRecord.getSourceDatabaseName());
            this.sourceSchema = safe(aiRecord.getSourceSchemaName());
            this.sourceTable = safe(aiRecord.getSourceTableName());
            this.targetViewSchema = safe(mergedRow.getTargetViewSchema());
            this.targetView = safe(mergedRow.getTargetView());
            this.targetTableSchema = safe(mergedRow.getTargetTableSchema());
            this.targetTable = safe(mergedRow.getTargetTable());
        }

        private void accept(ManualConfirmationMergedRow row) {
            totalCount++;
            if (row.getOwner() != null && !row.getOwner().isBlank()) {
                owners.add(row.getOwner());
            }
            if (row.getConfirmResultNormalized() != null && !row.getConfirmResultNormalized().isBlank()) {
                confirmResults.add(row.getConfirmResultNormalized());
            }
            if ("高".equals(row.getRiskLevel())) {
                riskLevel = "高";
            } else if ("中".equals(row.getRiskLevel()) && !"高".equals(riskLevel)) {
                riskLevel = "中";
            }
            if (row.getMatchStatus() == ManualConfirmationMatchStatus.MATCHED) {
                matchedCount++;
            } else if (row.getMatchStatus() == ManualConfirmationMatchStatus.UNMATCHED_AI) {
                unmatchedCount++;
            } else if (row.getMatchStatus() == ManualConfirmationMatchStatus.AMBIGUOUS_AI) {
                ambiguousCount++;
            }
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }

    private static final class OwnerSummary {
        private final String owner;
        private int totalCount;
        private int matchedCount;
        private int pendingCount;
        private int noImpactCount;
        private int fixedCount;
        private int abandonedCount;

        private OwnerSummary(String owner) {
            this.owner = owner;
        }

        private void accept(ManualConfirmationMergedRow row) {
            totalCount++;
            if (row.getMatchStatus() == ManualConfirmationMatchStatus.MATCHED) {
                matchedCount++;
            }
            String normalized = row.getConfirmResultNormalized();
            if (normalized == null || normalized.isBlank()) {
                pendingCount++;
            } else if ("待确认".equals(normalized)) {
                pendingCount++;
            } else if ("无影响".equals(normalized)) {
                noImpactCount++;
            } else if ("已修复".equals(normalized)) {
                fixedCount++;
            } else if ("已废弃".equals(normalized)) {
                abandonedCount++;
            }
        }

        private int confirmedCount() {
            return noImpactCount + fixedCount + abandonedCount;
        }
    }
}
