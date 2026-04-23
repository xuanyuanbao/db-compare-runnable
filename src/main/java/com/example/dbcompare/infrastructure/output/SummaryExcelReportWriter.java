package com.example.dbcompare.infrastructure.output;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.enums.DiffType;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.domain.model.CompareOptions;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class SummaryExcelReportWriter {
    private static final String SUMMARY_SHEET_NAME = "汇总";
    private static final String TABLE_STATUS_SHEET_NAME = "表级状态";
    private static final String TYPE_LENGTH_SUMMARY_SHEET_NAME = "类型长度联合汇总";
    private static final String FIELD_EXISTENCE_DETAIL_SHEET_NAME = "字段存在明细";
    private static final String TYPE_DETAIL_SHEET_NAME = "类型明细";
    private static final String LENGTH_DETAIL_SHEET_NAME = "长度明细";
    private static final String DEFAULT_DETAIL_SHEET_NAME = "默认值明细";
    private static final String NULLABLE_DETAIL_SHEET_NAME = "可空明细";
    private static final String[] DETAIL_HEADERS = {"源数据库", "源Schema", "源表", "目标Schema", "目标对象", "字段名", "差异类型", "说明"};
    private static final List<String> TYPE_LENGTH_CODES = List.of(
            "MATCH",
            "TYPE_MISMATCH",
            "TARGET_LENGTH_LONGER",
            "TARGET_LENGTH_SHORTER",
            "MISSING_SOURCE",
            "MISSING_TARGET",
            "LENGTH_MISMATCH",
            "NOT_APPLICABLE"
    );

    public SummaryExcelReportSession open(Path path, CompareOptions options) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            SXSSFWorkbook workbook = new SXSSFWorkbook(200);
            workbook.setCompressTempFiles(true);
            return new SummaryExcelReportSession(path, workbook, options == null ? new CompareOptions() : options);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open summary Excel report: " + path, e);
        }
    }

    public SummaryExcelReportSession open(Path path) {
        return open(path, new CompareOptions());
    }

    public final class SummaryExcelReportSession implements Closeable {
        private final Path path;
        private final SXSSFWorkbook workbook;
        private final CompareOptions options;
        private final List<ColumnComparisonRecord> detailRecords = new ArrayList<>();
        private final Map<String, TableStats> tableStats = new LinkedHashMap<>();
        private final CellStyle pageTitleStyle;
        private final CellStyle titleStyle;
        private final CellStyle headerStyle;
        private final CellStyle neutralStyle;
        private final CellStyle positiveStyle;
        private final CellStyle mediumStyle;
        private final CellStyle highStyle;

        private SummaryExcelReportSession(Path path, SXSSFWorkbook workbook, CompareOptions options) {
            this.path = path;
            this.workbook = workbook;
            this.options = options;
            this.pageTitleStyle = createPageTitleStyle(workbook);
            this.titleStyle = createTitleStyle(workbook);
            this.headerStyle = createHeaderStyle(workbook);
            this.neutralStyle = createBodyStyle(workbook, IndexedColors.WHITE);
            this.positiveStyle = createBodyStyle(workbook, IndexedColors.LIGHT_GREEN);
            this.mediumStyle = createBodyStyle(workbook, IndexedColors.LIGHT_ORANGE);
            this.highStyle = createBodyStyle(workbook, IndexedColors.CORAL);
        }

        public void append(List<ColumnComparisonRecord> records) {
            for (ColumnComparisonRecord record : records) {
                detailRecords.add(record);
                tableStats.computeIfAbsent(tableKey(record), ignored -> new TableStats(record)).accept(record);
            }
        }

        @Override
        public void close() throws IOException {
            renderSummarySheet();
            renderTableStatusSheet();
            renderTypeLengthSummarySheet();
            renderDetailSheet(FIELD_EXISTENCE_DETAIL_SHEET_NAME, this::isFieldExistenceRecord);
            renderDetailSheet(TYPE_DETAIL_SHEET_NAME, record -> containsDiff(record, DiffType.COLUMN_TYPE_MISMATCH));
            renderDetailSheet(LENGTH_DETAIL_SHEET_NAME, record -> containsDiff(record, DiffType.COLUMN_LENGTH_MISMATCH));
            if (options.isCompareDefaultValue()) {
                renderDetailSheet(DEFAULT_DETAIL_SHEET_NAME, record -> containsDiff(record, DiffType.COLUMN_DEFAULT_MISMATCH));
            }
            if (options.isCompareNullable()) {
                renderDetailSheet(NULLABLE_DETAIL_SHEET_NAME, record -> containsDiff(record, DiffType.COLUMN_NULLABLE_MISMATCH));
            }
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                workbook.write(outputStream);
            } finally {
                workbook.dispose();
                workbook.close();
            }
        }

        private void renderSummarySheet() {
            SXSSFSheet sheet = workbook.createSheet(SUMMARY_SHEET_NAME);
            configureSummarySheetLayout(sheet);

            writePageTitle(sheet, 0, "数据库结构对比汇总");
            writeInfoLine(sheet, 1, "统计对象", tableStats.size() + " 张目标对象");

            writeBlockTitle(sheet, 3, 0, "视图Schema");
            writeHeaders(sheet, 4, 0, List.of("视图Schema", "视图数量"));
            Map<String, Integer> schemaCount = new LinkedHashMap<>();
            for (TableStats stats : tableStats.values()) {
                schemaCount.merge(stats.targetSchemaName, 1, Integer::sum);
            }
            int schemaRow = 5;
            for (Map.Entry<String, Integer> entry : schemaCount.entrySet()) {
                Row row = row(sheet, schemaRow++);
                writeCell(row, 0, entry.getKey(), neutralStyle);
                writeCell(row, 1, Integer.toString(entry.getValue()), neutralStyle);
            }
            Row totalRow = row(sheet, schemaRow);
            writeCell(totalRow, 0, "合计", titleStyle);
            writeCell(totalRow, 1, Integer.toString(tableStats.size()), titleStyle);

            writeBlockTitle(sheet, 3, 4, "指标概况");
            writeHeaders(sheet, 4, 4, List.of("指标", "值"));
            List<String[]> metrics = new ArrayList<>();
            metrics.add(new String[]{"总表数", Integer.toString(tableStats.size())});
            metrics.add(new String[]{"完全存在表数", Integer.toString(countStatus(TableStats::fieldExistenceStatus, "FULL_EXISTS"))});
            metrics.add(new String[]{"不完全存在表数", Integer.toString(countStatus(TableStats::fieldExistenceStatus, "NOT_FULL_EXISTS"))});
            metrics.add(new String[]{"类型不一致表数", Integer.toString(countStatus(TableStats::typeStatus, "TYPE_MISMATCH"))});
            metrics.add(new String[]{"长度不一致表数", Integer.toString(countStatus(TableStats::lengthStatus, "LENGTH_MISMATCH"))});
            metrics.add(new String[]{"联合判断异常表数", Integer.toString(countCombinedIssueTables())});
            if (options.isCompareDefaultValue()) {
                metrics.add(new String[]{"默认值不一致表数", Integer.toString(countStatus(TableStats::defaultStatus, "DEFAULT_MISMATCH"))});
            }
            if (options.isCompareNullable()) {
                metrics.add(new String[]{"可空性不一致表数", Integer.toString(countStatus(TableStats::nullableStatus, "NULLABLE_MISMATCH"))});
            }
            int metricRow = 5;
            for (String[] metric : metrics) {
                Row row = row(sheet, metricRow++);
                writeCell(row, 4, metric[0], neutralStyle);
                writeCell(row, 5, metric[1], neutralStyle);
            }

            int upperBlockStartRow = 3;
            int riskEndRow = renderStatusBlock(sheet, upperBlockStartRow, 8, "风险等级", List.of("LOW", "MEDIUM", "HIGH"), TableStats::riskLevel);
            int diffEndRow = renderStatusBlock(sheet, upperBlockStartRow, 12, "差异分类", List.of("FULL_MATCH", "MISSING_COLUMN", "TYPE_MISMATCH", "LENGTH_MISMATCH", "OTHER"), TableStats::diffCategory);

            int lowerBlockStartRow = Math.max(Math.max(schemaRow, metricRow - 1), Math.max(riskEndRow, diffEndRow)) + 3;
            int nextRow = renderStatusBlock(sheet, lowerBlockStartRow, 0, "字段存在状态", List.of("FULL_EXISTS", "NOT_FULL_EXISTS"), TableStats::fieldExistenceStatus);
            nextRow = Math.max(nextRow, renderStatusBlock(sheet, lowerBlockStartRow, 4, "类型状态", List.of("TYPE_MATCH", "TYPE_MISMATCH"), TableStats::typeStatus));
            nextRow = Math.max(nextRow, renderStatusBlock(sheet, lowerBlockStartRow, 8, "长度状态", List.of("LENGTH_MATCH", "LENGTH_MISMATCH"), TableStats::lengthStatus));

            if (options.isCompareDefaultValue() || options.isCompareNullable()) {
                int thirdRowStart = nextRow + 2;
                if (options.isCompareDefaultValue()) {
                    nextRow = renderStatusBlock(sheet, thirdRowStart, 0, "默认值状态", List.of("DEFAULT_MATCH", "DEFAULT_MISMATCH"), TableStats::defaultStatus);
                }
                if (options.isCompareNullable()) {
                    int nullableStartCol = options.isCompareDefaultValue() ? 4 : 0;
                    renderStatusBlock(sheet, thirdRowStart, nullableStartCol, "可空状态", List.of("NULLABLE_MATCH", "NULLABLE_MISMATCH"), TableStats::nullableStatus);
                }
            }
        }

        private void renderTableStatusSheet() {
            SXSSFSheet sheet = workbook.createSheet(TABLE_STATUS_SHEET_NAME);
            List<String> headers = new ArrayList<>(List.of("源数据库", "源Schema", "源表", "目标Schema", "目标对象", "字段存在状态", "类型状态", "长度状态", "类型长度联合结论"));
            if (options.isCompareDefaultValue()) {
                headers.add("默认值状态");
            }
            if (options.isCompareNullable()) {
                headers.add("可空状态");
            }
            headers.add("风险等级");
            headers.add("差异分类");
            writeHeaders(sheet, 0, 0, headers);

            int rowIndex = 1;
            for (TableStats stats : sortedTableStats()) {
                Row row = row(sheet, rowIndex++);
                int col = 0;
                writeCell(row, col++, stats.sourceDatabaseName, null);
                writeCell(row, col++, stats.sourceSchemaName, null);
                writeCell(row, col++, stats.sourceTableName, null);
                writeCell(row, col++, stats.targetSchemaName, null);
                writeCell(row, col++, stats.targetTableName, null);
                writeCell(row, col++, OutputTextFormatter.summaryStatusText(stats.fieldExistenceStatus()), styleForStatus(stats.fieldExistenceStatus()));
                writeCell(row, col++, OutputTextFormatter.summaryStatusText(stats.typeStatus()), styleForStatus(stats.typeStatus()));
                writeCell(row, col++, OutputTextFormatter.summaryStatusText(stats.lengthStatus()), styleForStatus(stats.lengthStatus()));
                writeCell(row, col++, OutputTextFormatter.summaryStatusText(stats.typeLengthConclusion()), styleForStatus(stats.typeLengthConclusion()));
                if (options.isCompareDefaultValue()) {
                    writeCell(row, col++, OutputTextFormatter.summaryStatusText(stats.defaultStatus()), styleForStatus(stats.defaultStatus()));
                }
                if (options.isCompareNullable()) {
                    writeCell(row, col++, OutputTextFormatter.summaryStatusText(stats.nullableStatus()), styleForStatus(stats.nullableStatus()));
                }
                writeCell(row, col++, OutputTextFormatter.summaryStatusText(stats.riskLevel()), styleForStatus(stats.riskLevel()));
                writeCell(row, col, OutputTextFormatter.summaryStatusText(stats.diffCategory()), styleForStatus(stats.diffCategory()));
            }
        }

        private void renderTypeLengthSummarySheet() {
            SXSSFSheet sheet = workbook.createSheet(TYPE_LENGTH_SUMMARY_SHEET_NAME);
            List<String> headers = List.of(
                    "源数据库", "源Schema", "源表", "目标Schema", "目标对象", "字段总数",
                    "联合判断一致数", "联合判断类型不一致数", "联合判断目标长度大于源长度数",
                    "联合判断目标长度小于源长度数", "联合判断缺少源字段数", "联合判断缺少目标字段数",
                    "联合判断其他长度差异数", "表级联合判断结论"
            );
            writeHeaders(sheet, 0, 0, headers);

            int rowIndex = 1;
            for (TableStats stats : sortedTableStats()) {
                Row row = row(sheet, rowIndex++);
                int col = 0;
                writeCell(row, col++, stats.sourceDatabaseName, null);
                writeCell(row, col++, stats.sourceSchemaName, null);
                writeCell(row, col++, stats.sourceTableName, null);
                writeCell(row, col++, stats.targetSchemaName, null);
                writeCell(row, col++, stats.targetTableName, null);
                writeCell(row, col++, Integer.toString(stats.totalColumns()), neutralStyle);
                writeCell(row, col++, Integer.toString(stats.typeLengthCount("MATCH")), styleForStatus("MATCH"));
                writeCell(row, col++, Integer.toString(stats.typeLengthCount("TYPE_MISMATCH")), styleForStatus("TYPE_MISMATCH"));
                writeCell(row, col++, Integer.toString(stats.typeLengthCount("TARGET_LENGTH_LONGER")), styleForStatus("TARGET_LENGTH_LONGER"));
                writeCell(row, col++, Integer.toString(stats.typeLengthCount("TARGET_LENGTH_SHORTER")), styleForStatus("TARGET_LENGTH_SHORTER"));
                writeCell(row, col++, Integer.toString(stats.typeLengthCount("MISSING_SOURCE")), styleForStatus("MISSING_SOURCE"));
                writeCell(row, col++, Integer.toString(stats.typeLengthCount("MISSING_TARGET")), styleForStatus("MISSING_TARGET"));
                writeCell(row, col++, Integer.toString(stats.typeLengthCount("LENGTH_MISMATCH")), styleForStatus("LENGTH_MISMATCH"));
                writeCell(row, col, OutputTextFormatter.summaryStatusText(stats.typeLengthConclusion()), styleForStatus(stats.typeLengthConclusion()));
            }
            for (int index = 0; index < headers.size(); index++) {
                sheet.setColumnWidth(index, (index < 5 ? 18 : 16) * 256);
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(1, rowIndex - 1), 0, headers.size() - 1));
        }

        private void renderDetailSheet(String sheetName, Predicate<ColumnComparisonRecord> predicate) {
            SXSSFSheet sheet = workbook.createSheet(sheetName);
            writeHeaders(sheet, 0, 0, List.of(DETAIL_HEADERS));
            int rowIndex = 1;
            List<ColumnComparisonRecord> filtered = new ArrayList<>();
            for (ColumnComparisonRecord record : detailRecords) {
                if (predicate.test(record)) {
                    filtered.add(record);
                }
            }
            if (filtered.isEmpty()) {
                Row row = row(sheet, rowIndex);
                writeCell(row, 0, OutputTextFormatter.summaryStatusText("NO_DATA"), neutralStyle);
                return;
            }
            for (ColumnComparisonRecord record : filtered) {
                Row row = row(sheet, rowIndex++);
                writeCell(row, 0, record.getSourceDatabaseName(), null);
                writeCell(row, 1, record.getSourceSchemaName(), null);
                writeCell(row, 2, record.getSourceTableName(), null);
                writeCell(row, 3, record.getTargetSchemaName(), null);
                writeCell(row, 4, record.getTargetTableName(), null);
                writeCell(row, 5, record.getColumnName(), null);
                writeCell(row, 6, OutputTextFormatter.diffTypesText(detailDiffType(record)), styleForStatus(detailDiffType(record)));
                writeCell(row, 7, detailMessage(record), styleForStatus(detailDiffType(record)));
            }
        }

        private boolean isFieldExistenceRecord(ColumnComparisonRecord record) {
            return !record.isSourceColumnExists() || !record.isTargetColumnExists() || containsDiff(record, DiffType.VIEW_MISSING_COLUMN_INFO);
        }

        private boolean containsDiff(ColumnComparisonRecord record, DiffType diffType) {
            if (record.getDiffTypes() == null || record.getDiffTypes().isBlank()) {
                return false;
            }
            for (String token : record.getDiffTypes().split("\\|")) {
                if (diffType.name().equals(token.trim())) {
                    return true;
                }
            }
            return false;
        }

        private int countStatus(Function<TableStats, String> resolver, String expected) {
            int count = 0;
            for (TableStats stats : tableStats.values()) {
                if (expected.equals(resolver.apply(stats))) {
                    count++;
                }
            }
            return count;
        }

        private int countCombinedIssueTables() {
            int count = 0;
            for (TableStats stats : tableStats.values()) {
                if (!"MATCH".equals(stats.typeLengthConclusion())) {
                    count++;
                }
            }
            return count;
        }

        private List<TableStats> sortedTableStats() {
            List<TableStats> values = new ArrayList<>(tableStats.values());
            values.sort(Comparator.comparing((TableStats stats) -> stats.sourceDatabaseName)
                    .thenComparing(stats -> stats.sourceSchemaName)
                    .thenComparing(stats -> stats.sourceTableName)
                    .thenComparing(stats -> stats.targetSchemaName)
                    .thenComparing(stats -> stats.targetTableName));
            return values;
        }

        private int renderStatusBlock(SXSSFSheet sheet, int startRow, int startColumn, String title, List<String> statuses, Function<TableStats, String> resolver) {
            writeBlockTitle(sheet, startRow, startColumn, title);
            writeHeaders(sheet, startRow + 1, startColumn, List.of("状态", "表数量", "占比"));
            int total = tableStats.size();
            int rowIndex = startRow + 2;
            for (String status : statuses) {
                int count = countStatus(resolver, status);
                Row row = row(sheet, rowIndex++);
                writeCell(row, startColumn, OutputTextFormatter.summaryStatusText(status), styleForStatus(status));
                writeCell(row, startColumn + 1, Integer.toString(count), styleForStatus(status));
                writeCell(row, startColumn + 2, ratio(count, total), styleForStatus(status));
            }
            return rowIndex - 1;
        }

        private String detailDiffType(ColumnComparisonRecord record) {
            if (record.getDiffTypes() != null && !record.getDiffTypes().isBlank()) {
                return record.getDiffTypes();
            }
            return record.getOverallStatus() == ComparisonStatus.MATCH ? "MATCH" : "MISMATCH";
        }

        private String detailMessage(ColumnComparisonRecord record) {
            if (record.getMessage() != null && !record.getMessage().isBlank()) {
                return OutputTextFormatter.messageText(record.getMessage());
            }
            return record.getOverallStatus() == ComparisonStatus.MATCH ? OutputTextFormatter.comparisonStatusText(ComparisonStatus.MATCH) : "";
        }

        private void writeBlockTitle(SXSSFSheet sheet, int rowIndex, int columnIndex, String title) {
            Row row = row(sheet, rowIndex);
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(title);
            cell.setCellStyle(titleStyle);
        }

        private void writePageTitle(SXSSFSheet sheet, int rowIndex, String title) {
            Row row = row(sheet, rowIndex);
            Cell cell = row.createCell(0);
            cell.setCellValue(title);
            cell.setCellStyle(pageTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 14));
        }

        private void writeInfoLine(SXSSFSheet sheet, int rowIndex, String label, String value) {
            Row row = row(sheet, rowIndex);
            writeCell(row, 0, label, titleStyle);
            writeCell(row, 1, value, neutralStyle);
        }

        private void writeHeaders(SXSSFSheet sheet, int rowIndex, int startColumn, List<String> headers) {
            Row row = row(sheet, rowIndex);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = row.createCell(startColumn + i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }
        }

        private Row row(SXSSFSheet sheet, int rowIndex) {
            Row row = sheet.getRow(rowIndex);
            return row == null ? sheet.createRow(rowIndex) : row;
        }

        private void writeCell(Row row, int columnIndex, String value, CellStyle style) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(value == null ? "" : value);
            if (style != null) {
                cell.setCellStyle(style);
            }
        }

        private String tableKey(ColumnComparisonRecord record) {
            return String.join("|",
                    safe(record.getSourceDatabaseName()),
                    safe(record.getSourceSchemaName()),
                    safe(record.getSourceTableName()),
                    safe(record.getTargetSchemaName()),
                    safe(record.getTargetTableName()));
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }

        private void setWidth(SXSSFSheet sheet, int columnIndex, int widthChars) {
            sheet.setColumnWidth(columnIndex, widthChars * 256);
        }

        private void configureSummarySheetLayout(SXSSFSheet sheet) {
            setWidth(sheet, 0, 20);
            setWidth(sheet, 1, 12);
            setWidth(sheet, 2, 3);
            setWidth(sheet, 3, 3);
            setWidth(sheet, 4, 20);
            setWidth(sheet, 5, 12);
            setWidth(sheet, 6, 3);
            setWidth(sheet, 7, 3);
            setWidth(sheet, 8, 16);
            setWidth(sheet, 9, 12);
            setWidth(sheet, 10, 12);
            setWidth(sheet, 11, 3);
            setWidth(sheet, 12, 16);
            setWidth(sheet, 13, 12);
            setWidth(sheet, 14, 12);
            sheet.createFreezePane(0, 3);
        }

        private String ratio(int count, int total) {
            if (total <= 0) {
                return "0.00%";
            }
            return String.format(java.util.Locale.ROOT, "%.2f%%", count * 100.0 / total);
        }

        private CellStyle styleForStatus(String status) {
            if (status == null || status.isBlank()) {
                return neutralStyle;
            }
            String normalized = status.toUpperCase();
            if (normalized.endsWith("_MATCH") || normalized.equals("FULL_EXISTS") || normalized.equals("FULL_MATCH")
                    || normalized.equals("LOW") || normalized.equals("MATCH")) {
                return positiveStyle;
            }
            if (normalized.contains("MISSING") || normalized.contains("NOT_FULL_EXISTS") || normalized.contains("TYPE_MISMATCH")
                    || normalized.equals("HIGH") || normalized.equals("PRESENCE_ISSUE") || normalized.equals("TARGET_LENGTH_TIGHTER")) {
                return highStyle;
            }
            return mediumStyle;
        }

        private CellStyle createTitleStyle(SXSSFWorkbook workbook) {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setFontHeightInPoints((short) 12);
            style.setFont(font);
            style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }

        private CellStyle createPageTitleStyle(SXSSFWorkbook workbook) {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setFontHeightInPoints((short) 14);
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }

        private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }

        private CellStyle createBodyStyle(SXSSFWorkbook workbook, IndexedColors fillColor) {
            CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(fillColor.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }
    }

    private static final class TableStats {
        private final String sourceDatabaseName;
        private final String sourceSchemaName;
        private final String sourceTableName;
        private final String targetSchemaName;
        private final String targetTableName;
        private final java.util.EnumSet<DiffType> diffTypes = java.util.EnumSet.noneOf(DiffType.class);
        private final Map<String, Integer> typeLengthCounts = new LinkedHashMap<>();
        private boolean mismatch;
        private boolean fullExists = true;
        private boolean typeMismatch;
        private boolean lengthMismatch;
        private boolean defaultMismatch;
        private boolean nullableMismatch;
        private int totalColumns;

        private TableStats(ColumnComparisonRecord record) {
            this.sourceDatabaseName = safe(record.getSourceDatabaseName());
            this.sourceSchemaName = safe(record.getSourceSchemaName());
            this.sourceTableName = safe(record.getSourceTableName());
            this.targetSchemaName = safe(record.getTargetSchemaName());
            this.targetTableName = safe(record.getTargetTableName());
            for (String code : TYPE_LENGTH_CODES) {
                typeLengthCounts.put(code, 0);
            }
        }

        private void accept(ColumnComparisonRecord record) {
            totalColumns++;
            String combinedStatus = record.getTypeLengthCombinedStatus();
            if (combinedStatus != null && !combinedStatus.isBlank()) {
                typeLengthCounts.merge(combinedStatus, 1, Integer::sum);
            }
            if (record.isAffectsResult() && record.getOverallStatus() == ComparisonStatus.MISMATCH) {
                mismatch = true;
            }
            if (record.isAffectsResult() && (!record.isSourceColumnExists() || !record.isTargetColumnExists())) {
                fullExists = false;
            }
            if (!record.isAffectsResult() || record.getDiffTypes() == null || record.getDiffTypes().isBlank()) {
                return;
            }
            for (String token : record.getDiffTypes().split("\\|")) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    DiffType diffType = DiffType.valueOf(trimmed);
                    diffTypes.add(diffType);
                    if (diffType == DiffType.COLUMN_TYPE_MISMATCH) {
                        typeMismatch = true;
                    }
                    if (diffType == DiffType.COLUMN_LENGTH_MISMATCH) {
                        lengthMismatch = true;
                    }
                    if (diffType == DiffType.COLUMN_DEFAULT_MISMATCH) {
                        defaultMismatch = true;
                    }
                    if (diffType == DiffType.COLUMN_NULLABLE_MISMATCH) {
                        nullableMismatch = true;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        private int totalColumns() {
            return totalColumns;
        }

        private int typeLengthCount(String code) {
            return typeLengthCounts.getOrDefault(code, 0);
        }

        private String typeLengthConclusion() {
            if (typeLengthCount("MISSING_SOURCE") > 0 || typeLengthCount("MISSING_TARGET") > 0) {
                return "PRESENCE_ISSUE";
            }
            if (typeLengthCount("TYPE_MISMATCH") > 0) {
                return "TYPE_MISMATCH";
            }
            if (typeLengthCount("TARGET_LENGTH_SHORTER") > 0 || typeLengthCount("LENGTH_MISMATCH") > 0) {
                return "TARGET_LENGTH_TIGHTER";
            }
            if (typeLengthCount("TARGET_LENGTH_LONGER") > 0) {
                return "TARGET_LENGTH_RELAXED";
            }
            if (typeLengthCount("MATCH") > 0) {
                return "MATCH";
            }
            return "NO_DATA";
        }

        private String fieldExistenceStatus() {
            return fullExists ? "FULL_EXISTS" : "NOT_FULL_EXISTS";
        }

        private String typeStatus() {
            return typeMismatch ? "TYPE_MISMATCH" : "TYPE_MATCH";
        }

        private String lengthStatus() {
            return lengthMismatch ? "LENGTH_MISMATCH" : "LENGTH_MATCH";
        }

        private String defaultStatus() {
            return defaultMismatch ? "DEFAULT_MISMATCH" : "DEFAULT_MATCH";
        }

        private String nullableStatus() {
            return nullableMismatch ? "NULLABLE_MISMATCH" : "NULLABLE_MATCH";
        }

        private String diffCategory() {
            if (!mismatch) {
                return "FULL_MATCH";
            }
            if (containsAny(DiffType.SOURCE_TABLE_NOT_FOUND, DiffType.SOURCE_TABLE_AMBIGUOUS, DiffType.TARGET_TABLE_NOT_FOUND,
                    DiffType.COLUMN_MISSING_IN_SOURCE, DiffType.COLUMN_MISSING_IN_TARGET)) {
                return "MISSING_COLUMN";
            }
            if (typeMismatch) {
                return "TYPE_MISMATCH";
            }
            if (lengthMismatch) {
                return "LENGTH_MISMATCH";
            }
            return "OTHER";
        }

        private String riskLevel() {
            if (!mismatch) {
                return "LOW";
            }
            if (containsAny(DiffType.SOURCE_TABLE_NOT_FOUND, DiffType.SOURCE_TABLE_AMBIGUOUS, DiffType.TARGET_TABLE_NOT_FOUND,
                    DiffType.COLUMN_MISSING_IN_SOURCE, DiffType.COLUMN_MISSING_IN_TARGET, DiffType.COLUMN_TYPE_MISMATCH)) {
                return "HIGH";
            }
            return "MEDIUM";
        }

        private boolean containsAny(DiffType... candidates) {
            for (DiffType candidate : candidates) {
                if (diffTypes.contains(candidate)) {
                    return true;
                }
            }
            return false;
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
