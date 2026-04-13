package com.example.dbcompare.infrastructure.output;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.enums.DiffType;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.BorderStyle;
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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class SummaryExcelReportWriter {
    private static final String SUMMARY_SHEET_NAME = "Summary";
    private static final String TABLE_STATUS_SHEET_NAME = "Table Status";
    private static final String FIELD_EXISTENCE_DETAIL_SHEET_NAME = "Field Existence Detail";
    private static final String TYPE_DETAIL_SHEET_NAME = "Type Detail";
    private static final String LENGTH_DETAIL_SHEET_NAME = "Length Detail";
    private static final String DEFAULT_DETAIL_SHEET_NAME = "Default Detail";
    private static final String NULLABLE_DETAIL_SHEET_NAME = "Nullable Detail";

    private static final String FULL_EXISTS = "FULL_EXISTS";
    private static final String NOT_FULL_EXISTS = "NOT_FULL_EXISTS";
    private static final String TYPE_MATCH = "TYPE_MATCH";
    private static final String TYPE_MISMATCH = "TYPE_MISMATCH";
    private static final String LENGTH_MATCH = "LENGTH_MATCH";
    private static final String LENGTH_MISMATCH = "LENGTH_MISMATCH";
    private static final String DEFAULT_MATCH = "DEFAULT_MATCH";
    private static final String DEFAULT_MISMATCH = "DEFAULT_MISMATCH";
    private static final String NULLABLE_MATCH = "NULLABLE_MATCH";
    private static final String NULLABLE_MISMATCH = "NULLABLE_MISMATCH";
    private static final String LOW = "LOW";
    private static final String MEDIUM = "MEDIUM";
    private static final String HIGH = "HIGH";
    private static final String FULL_MATCH = "FULL_MATCH";
    private static final String MISSING_COLUMN = "MISSING_COLUMN";
    private static final String OTHER = "OTHER";

    private static final String[] SUMMARY_PAIR_HEADERS = {"metric", "value"};
    private static final String[] SUMMARY_RATIO_HEADERS = {"status", "tableCount", "ratio"};
    private static final String[] TABLE_STATUS_HEADERS = {
            "sourceDatabase", "sourceSchema", "sourceTable", "targetSchema", "targetTable",
            "fieldExistenceStatus", "typeStatus", "lengthStatus", "defaultStatus", "nullableStatus",
            "riskLevel", "diffCategory"
    };
    private static final String[] DETAIL_HEADERS = {
            "sourceDatabase", "sourceSchema", "sourceTable", "targetSchema", "targetTable", "columnName", "diffType", "detail"
    };

    private static final List<String> FIELD_EXISTENCE_ORDER = List.of(FULL_EXISTS, NOT_FULL_EXISTS);
    private static final List<String> TYPE_STATUS_ORDER = List.of(TYPE_MATCH, TYPE_MISMATCH);
    private static final List<String> LENGTH_STATUS_ORDER = List.of(LENGTH_MATCH, LENGTH_MISMATCH);
    private static final List<String> DEFAULT_STATUS_ORDER = List.of(DEFAULT_MATCH, DEFAULT_MISMATCH);
    private static final List<String> NULLABLE_STATUS_ORDER = List.of(NULLABLE_MATCH, NULLABLE_MISMATCH);
    private static final List<String> RISK_LEVEL_ORDER = List.of(LOW, MEDIUM, HIGH);
    private static final List<String> DIFF_CATEGORY_ORDER = List.of(FULL_MATCH, MISSING_COLUMN, TYPE_MISMATCH, LENGTH_MISMATCH, OTHER);

    private final int maxRowsPerSheet;

    public SummaryExcelReportWriter() {
        this(SpreadsheetVersion.EXCEL2007.getMaxRows());
    }

    public SummaryExcelReportWriter(int maxRowsPerSheet) {
        if (maxRowsPerSheet < 2) {
            throw new IllegalArgumentException("maxRowsPerSheet must allow at least one data row");
        }
        this.maxRowsPerSheet = maxRowsPerSheet;
    }

    public SummaryExcelReportSession open(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            SXSSFWorkbook workbook = new SXSSFWorkbook(200);
            workbook.setCompressTempFiles(true);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle neutralStyle = createBodyStyle(workbook, IndexedColors.WHITE);
            CellStyle positiveStyle = createBodyStyle(workbook, IndexedColors.LIGHT_GREEN);
            CellStyle warningStyle = createBodyStyle(workbook, IndexedColors.LIGHT_YELLOW);
            CellStyle negativeStyle = createBodyStyle(workbook, IndexedColors.ROSE);
            CellStyle mediumRiskStyle = createBodyStyle(workbook, IndexedColors.LIGHT_ORANGE);
            CellStyle highRiskStyle = createBodyStyle(workbook, IndexedColors.CORAL);
            return new SummaryExcelReportSession(
                    path,
                    workbook,
                    titleStyle,
                    headerStyle,
                    neutralStyle,
                    positiveStyle,
                    warningStyle,
                    negativeStyle,
                    mediumRiskStyle,
                    highRiskStyle,
                    maxRowsPerSheet);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open summary Excel report: " + path, e);
        }
    }

    private CellStyle createTitleStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
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

    private int defaultColumnWidth(int columnIndex, int headerCount) {
        if (headerCount == DETAIL_HEADERS.length && columnIndex == DETAIL_HEADERS.length - 1) {
            return 56 * 256;
        }
        if (headerCount == TABLE_STATUS_HEADERS.length && columnIndex <= 4) {
            return 18 * 256;
        }
        if (headerCount == TABLE_STATUS_HEADERS.length) {
            return 20 * 256;
        }
        return 22 * 256;
    }

    public final class SummaryExcelReportSession implements Closeable {
        private final Path path;
        private final SXSSFWorkbook workbook;
        private final CellStyle titleStyle;
        private final CellStyle headerStyle;
        private final CellStyle neutralStyle;
        private final CellStyle positiveStyle;
        private final CellStyle warningStyle;
        private final CellStyle negativeStyle;
        private final CellStyle mediumRiskStyle;
        private final CellStyle highRiskStyle;
        private final int maxRowsPerSheet;
        private final SXSSFSheet summarySheet;
        private final Map<String, TableStats> tableStats = new LinkedHashMap<>();
        private final List<ColumnComparisonRecord> detailRecords = new ArrayList<>();

        private SXSSFSheet tableStatusSheet;
        private int tableStatusSheetSequence = 0;
        private int tableStatusRowIndex = 1;

        private SummaryExcelReportSession(Path path,
                                          SXSSFWorkbook workbook,
                                          CellStyle titleStyle,
                                          CellStyle headerStyle,
                                          CellStyle neutralStyle,
                                          CellStyle positiveStyle,
                                          CellStyle warningStyle,
                                          CellStyle negativeStyle,
                                          CellStyle mediumRiskStyle,
                                          CellStyle highRiskStyle,
                                          int maxRowsPerSheet) {
            this.path = path;
            this.workbook = workbook;
            this.titleStyle = titleStyle;
            this.headerStyle = headerStyle;
            this.neutralStyle = neutralStyle;
            this.positiveStyle = positiveStyle;
            this.warningStyle = warningStyle;
            this.negativeStyle = negativeStyle;
            this.mediumRiskStyle = mediumRiskStyle;
            this.highRiskStyle = highRiskStyle;
            this.maxRowsPerSheet = maxRowsPerSheet;
            this.summarySheet = workbook.createSheet(SUMMARY_SHEET_NAME);
            this.tableStatusSheet = createTableStatusSheet();
            initializeSummarySheet();
        }

        public void append(List<ColumnComparisonRecord> records) {
            for (ColumnComparisonRecord record : records) {
                collect(record);
                detailRecords.add(record);
            }
        }

        @Override
        public void close() throws IOException {
            renderSummary();
            renderTableStatus();
            renderDetail();
            applyAutoFilter(tableStatusSheet, tableStatusRowIndex, TABLE_STATUS_HEADERS.length);
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                workbook.write(outputStream);
            } finally {
                workbook.dispose();
                workbook.close();
            }
        }

        private void initializeSummarySheet() {
            setColumnWidths(summarySheet, 0, 0, 18);
            setColumnWidths(summarySheet, 1, 1, 14);
            setColumnWidths(summarySheet, 3, 3, 22);
            setColumnWidths(summarySheet, 4, 4, 14);
            setColumnWidths(summarySheet, 6, 6, 24);
            setColumnWidths(summarySheet, 7, 7, 14);
            setColumnWidths(summarySheet, 8, 8, 14);
            setColumnWidths(summarySheet, 10, 10, 22);
            setColumnWidths(summarySheet, 11, 11, 14);
            setColumnWidths(summarySheet, 12, 12, 14);
            setColumnWidths(summarySheet, 14, 14, 34);
            setColumnWidths(summarySheet, 15, 15, 16);
        }

        private void collect(ColumnComparisonRecord record) {
            String sourceDatabase = safe(record.getSourceDatabaseName());
            String sourceSchema = firstNonBlank(record.getSourceSchemaName(), "UNKNOWN");
            String sourceTable = firstNonBlank(record.getSourceTableName(), "UNKNOWN");
            String targetSchema = firstNonBlank(record.getTargetSchemaName(), "UNKNOWN");
            String targetTable = firstNonBlank(record.getTargetTableName(), sourceTable, "UNKNOWN");
            String key = String.join("|", sourceDatabase, sourceSchema, sourceTable, targetSchema, targetTable);
            tableStats.computeIfAbsent(key, ignored -> new TableStats(sourceDatabase, sourceSchema, sourceTable, targetSchema, targetTable))
                    .accept(record);
        }

        private void renderSummary() {
            writeSchemaDistributionBlock(0, 0);
            writeOverviewBlock(0, 3);
            writeStatusBlock("fieldExistenceStatus", FIELD_EXISTENCE_ORDER, TableStats::fieldExistenceStatus, 0, 6);
            writeStatusBlock("typeStatus", TYPE_STATUS_ORDER, TableStats::typeStatus, 4, 6);
            writeStatusBlock("lengthStatus", LENGTH_STATUS_ORDER, TableStats::lengthStatus, 8, 6);
            writeStatusBlock("defaultStatus", DEFAULT_STATUS_ORDER, TableStats::defaultStatus, 12, 6);
            writeStatusBlock("nullableStatus", NULLABLE_STATUS_ORDER, TableStats::nullableStatus, 16, 6);
            writeStatusBlock("riskLevel", RISK_LEVEL_ORDER, TableStats::riskLevel, 0, 10);
            writeStatusBlock("diffCategory", DIFF_CATEGORY_ORDER, TableStats::diffCategory, 5, 10);
            writeRiskRuleBlock(0, 14);
        }

        private void writeSchemaDistributionBlock(int startRow, int startColumn) {
            writeBlockTitle(summarySheet, startRow, startColumn, "view schema");
            writeHeaders(summarySheet, startRow + 1, startColumn, new String[]{"viewSchema", "viewCount"});
            Map<String, Integer> schemaCount = new LinkedHashMap<>();
            for (TableStats stats : tableStats.values()) {
                schemaCount.merge(stats.targetSchemaName, 1, Integer::sum);
            }
            List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(schemaCount.entrySet());
            sortedEntries.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                    .thenComparing(Map.Entry.comparingByKey()));
            int rowIndex = startRow + 2;
            for (Map.Entry<String, Integer> entry : sortedEntries) {
                Row row = row(summarySheet, rowIndex++);
                writeCell(row, startColumn, entry.getKey(), neutralStyle);
                writeCell(row, startColumn + 1, Integer.toString(entry.getValue()), neutralStyle);
            }
            Row totalRow = row(summarySheet, rowIndex);
            writeCell(totalRow, startColumn, "total", titleStyle);
            writeCell(totalRow, startColumn + 1, Integer.toString(tableStats.size()), titleStyle);
        }

        private void writeOverviewBlock(int startRow, int startColumn) {
            writeBlockTitle(summarySheet, startRow, startColumn, "metric");
            writeHeaders(summarySheet, startRow + 1, startColumn, SUMMARY_PAIR_HEADERS);
            int totalTables = tableStats.size();
            int fullExistsTables = countByStatus(TableStats::fieldExistenceStatus, FULL_EXISTS);
            int notFullExistsTables = countByStatus(TableStats::fieldExistenceStatus, NOT_FULL_EXISTS);
            int typeMismatchTables = countByStatus(TableStats::typeStatus, TYPE_MISMATCH);
            int lengthMismatchTables = countByStatus(TableStats::lengthStatus, LENGTH_MISMATCH);
            int defaultMismatchTables = countByStatus(TableStats::defaultStatus, DEFAULT_MISMATCH);
            int nullableMismatchTables = countByStatus(TableStats::nullableStatus, NULLABLE_MISMATCH);
            String[][] metrics = {
                    {"totalTables", Integer.toString(totalTables)},
                    {"fullExistsTables", Integer.toString(fullExistsTables)},
                    {"notFullExistsTables", Integer.toString(notFullExistsTables)},
                    {"typeMismatchTables", Integer.toString(typeMismatchTables)},
                    {"lengthMismatchTables", Integer.toString(lengthMismatchTables)},
                    {"defaultMismatchTables", Integer.toString(defaultMismatchTables)},
                    {"nullableMismatchTables", Integer.toString(nullableMismatchTables)},
                    {"fullExistsRatio", ratio(fullExistsTables, totalTables)}
            };
            int rowIndex = startRow + 2;
            for (String[] metric : metrics) {
                Row row = row(summarySheet, rowIndex++);
                writeCell(row, startColumn, metric[0], neutralStyle);
                writeCell(row, startColumn + 1, metric[1], neutralStyle);
            }
        }

        private void writeStatusBlock(String title,
                                      List<String> orderedStatuses,
                                      StatusResolver resolver,
                                      int startRow,
                                      int startColumn) {
            writeBlockTitle(summarySheet, startRow, startColumn, title);
            writeHeaders(summarySheet, startRow + 1, startColumn, SUMMARY_RATIO_HEADERS);
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String status : orderedStatuses) {
                counts.put(status, 0);
            }
            for (TableStats stats : tableStats.values()) {
                counts.merge(resolver.resolve(stats), 1, Integer::sum);
            }
            int totalTables = tableStats.size();
            int rowIndex = startRow + 2;
            for (String status : orderedStatuses) {
                Row row = row(summarySheet, rowIndex++);
                CellStyle statusStyle = styleForStatus(status);
                writeCell(row, startColumn, status, statusStyle);
                writeCell(row, startColumn + 1, Integer.toString(counts.get(status)), statusStyle);
                writeCell(row, startColumn + 2, ratio(counts.get(status), totalTables), statusStyle);
            }
        }

        private void writeRiskRuleBlock(int startRow, int startColumn) {
            writeBlockTitle(summarySheet, startRow, startColumn, "risk rules");
            writeHeaders(summarySheet, startRow + 1, startColumn, new String[]{"condition", "riskLevel"});
            String[][] rules = {
                    {"No mismatch", LOW},
                    {"Length/default/nullable mismatch", MEDIUM},
                    {"Missing field/table or type mismatch", HIGH}
            };
            int rowIndex = startRow + 2;
            for (String[] rule : rules) {
                Row row = row(summarySheet, rowIndex++);
                writeCell(row, startColumn, rule[0], neutralStyle);
                writeCell(row, startColumn + 1, rule[1], styleForStatus(rule[1]));
            }
        }

        private void renderTableStatus() {
            List<TableStats> sorted = sortedTableStats();
            for (TableStats stats : sorted) {
                ensureTableStatusCapacityForNextRow();
                Row row = tableStatusSheet.createRow(tableStatusRowIndex++);
                writeCell(row, 0, stats.sourceDatabaseName);
                writeCell(row, 1, stats.sourceSchemaName);
                writeCell(row, 2, stats.sourceTableName);
                writeCell(row, 3, stats.targetSchemaName);
                writeCell(row, 4, stats.targetTableName);
                writeCell(row, 5, stats.fieldExistenceStatus(), styleForStatus(stats.fieldExistenceStatus()));
                writeCell(row, 6, stats.typeStatus(), styleForStatus(stats.typeStatus()));
                writeCell(row, 7, stats.lengthStatus(), styleForStatus(stats.lengthStatus()));
                writeCell(row, 8, stats.defaultStatus(), styleForStatus(stats.defaultStatus()));
                writeCell(row, 9, stats.nullableStatus(), styleForStatus(stats.nullableStatus()));
                writeCell(row, 10, stats.riskLevel(), styleForStatus(stats.riskLevel()));
                writeCell(row, 11, stats.diffCategory(), styleForStatus(stats.diffCategory()));
            }
        }

        private void renderDetail() {
            writeDetailSection(FIELD_EXISTENCE_DETAIL_SHEET_NAME, this::isFieldExistenceRecord);
            writeDetailSection(TYPE_DETAIL_SHEET_NAME, record -> containsDiff(record, DiffType.COLUMN_TYPE_MISMATCH));
            writeDetailSection(LENGTH_DETAIL_SHEET_NAME, record -> containsDiff(record, DiffType.COLUMN_LENGTH_MISMATCH));
            writeDetailSection(DEFAULT_DETAIL_SHEET_NAME, record -> containsDiff(record, DiffType.COLUMN_DEFAULT_MISMATCH));
            writeDetailSection(NULLABLE_DETAIL_SHEET_NAME, record -> containsDiff(record, DiffType.COLUMN_NULLABLE_MISMATCH));
        }

        private void writeDetailSection(String baseSheetName, Predicate<ColumnComparisonRecord> predicate) {
            List<ColumnComparisonRecord> filtered = new ArrayList<>();
            for (ColumnComparisonRecord record : detailRecords) {
                if (predicate.test(record)) {
                    filtered.add(record);
                }
            }

            DetailSheetState state = createDetailSheetState(baseSheetName);
            if (filtered.isEmpty()) {
                Row emptyRow = row(state.currentSheet, state.nextRowIndex++);
                writeCell(emptyRow, 0, "NO_DATA", neutralStyle);
                return;
            }
            for (ColumnComparisonRecord record : filtered) {
                ensureDetailCapacityForNextRow(state);
                Row row = row(state.currentSheet, state.nextRowIndex++);
                writeCell(row, 0, safe(record.getSourceDatabaseName()));
                writeCell(row, 1, firstNonBlank(record.getSourceSchemaName(), ""));
                writeCell(row, 2, firstNonBlank(record.getSourceTableName(), ""));
                writeCell(row, 3, firstNonBlank(record.getTargetSchemaName(), ""));
                writeCell(row, 4, firstNonBlank(record.getTargetTableName(), ""));
                writeCell(row, 5, safe(record.getColumnName()));
                String diffType = detailDiffType(record);
                writeCell(row, 6, diffType, styleForStatus(diffType));
                writeCell(row, 7, detailMessage(record), styleForStatus(diffType));
            }
        }

        private boolean isFieldExistenceRecord(ColumnComparisonRecord record) {
            return !record.isSourceColumnExists() || !record.isTargetColumnExists();
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

        private int countByStatus(StatusResolver resolver, String expectedStatus) {
            int count = 0;
            for (TableStats stats : tableStats.values()) {
                if (expectedStatus.equals(resolver.resolve(stats))) {
                    count++;
                }
            }
            return count;
        }

        private List<TableStats> sortedTableStats() {
            List<TableStats> sorted = new ArrayList<>(tableStats.values());
            sorted.sort(Comparator.comparing((TableStats stats) -> stats.sourceDatabaseName)
                    .thenComparing(stats -> stats.sourceSchemaName)
                    .thenComparing(stats -> stats.sourceTableName)
                    .thenComparing(stats -> stats.targetSchemaName)
                    .thenComparing(stats -> stats.targetTableName));
            return sorted;
        }

        private void ensureTableStatusCapacityForNextRow() {
            if (tableStatusRowIndex < maxRowsPerSheet) {
                return;
            }
            applyAutoFilter(tableStatusSheet, tableStatusRowIndex, TABLE_STATUS_HEADERS.length);
            tableStatusSheet = createTableStatusSheet();
            tableStatusRowIndex = 1;
        }

        private SXSSFSheet createTableStatusSheet() {
            String sheetName = tableStatusSheetSequence == 0 ? TABLE_STATUS_SHEET_NAME : TABLE_STATUS_SHEET_NAME + "_" + (tableStatusSheetSequence + 1);
            tableStatusSheetSequence++;
            SXSSFSheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            writeHeaders(headerRow, TABLE_STATUS_HEADERS);
            sheet.createFreezePane(0, 1);
            for (int index = 0; index < TABLE_STATUS_HEADERS.length; index++) {
                sheet.setColumnWidth(index, defaultColumnWidth(index, TABLE_STATUS_HEADERS.length));
            }
            return sheet;
        }

        private DetailSheetState createDetailSheetState(String baseSheetName) {
            DetailSheetState state = new DetailSheetState(baseSheetName);
            state.currentSheet = createDetailSheet(baseSheetName, state.sequence++);
            state.nextRowIndex = 1;
            return state;
        }

        private SXSSFSheet createDetailSheet(String baseSheetName, int sequence) {
            String sheetName = sequence == 0 ? baseSheetName : baseSheetName + "_" + (sequence + 1);
            SXSSFSheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            writeHeaders(headerRow, DETAIL_HEADERS);
            sheet.createFreezePane(0, 1);
            for (int index = 0; index < DETAIL_HEADERS.length; index++) {
                sheet.setColumnWidth(index, defaultColumnWidth(index, DETAIL_HEADERS.length));
            }
            return sheet;
        }

        private void ensureDetailCapacityForNextRow(DetailSheetState state) {
            if (state.nextRowIndex < maxRowsPerSheet) {
                return;
            }
            applyAutoFilter(state.currentSheet, state.nextRowIndex, DETAIL_HEADERS.length);
            state.currentSheet = createDetailSheet(state.baseSheetName, state.sequence++);
            state.nextRowIndex = 1;
        }

        private void writeBlockTitle(SXSSFSheet sheet, int rowIndex, int columnIndex, String title) {
            Row row = row(sheet, rowIndex);
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(title);
            cell.setCellStyle(titleStyle);
        }

        private void writeHeaders(SXSSFSheet sheet, int rowIndex, int startColumn, String[] headers) {
            Row row = row(sheet, rowIndex);
            for (int index = 0; index < headers.length; index++) {
                Cell cell = row.createCell(startColumn + index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
            }
        }

        private void writeHeaders(Row row, String[] headers) {
            for (int index = 0; index < headers.length; index++) {
                Cell cell = row.createCell(index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
            }
        }

        private Row row(SXSSFSheet sheet, int rowIndex) {
            Row row = sheet.getRow(rowIndex);
            return row == null ? sheet.createRow(rowIndex) : row;
        }

        private void writeCell(Row row, int columnIndex, String value) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(value == null ? "" : value);
        }

        private void writeCell(Row row, int columnIndex, String value, CellStyle style) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(value == null ? "" : value);
            if (style != null) {
                cell.setCellStyle(style);
            }
        }

        private void setColumnWidths(SXSSFSheet sheet, int fromColumn, int toColumn, int widthChars) {
            for (int columnIndex = fromColumn; columnIndex <= toColumn; columnIndex++) {
                sheet.setColumnWidth(columnIndex, widthChars * 256);
            }
        }

        private void applyAutoFilter(SXSSFSheet sheet, int nextRowIndex, int headerCount) {
            int lastRowIndex = Math.max(nextRowIndex - 1, 0);
            sheet.setAutoFilter(new CellRangeAddress(0, lastRowIndex, 0, headerCount - 1));
        }

        private String detailDiffType(ColumnComparisonRecord record) {
            if (record.getDiffTypes() != null && !record.getDiffTypes().isBlank()) {
                return record.getDiffTypes();
            }
            return record.getOverallStatus() == ComparisonStatus.MATCH ? "MATCH" : "MISMATCH";
        }

        private String detailMessage(ColumnComparisonRecord record) {
            if (record.getMessage() != null && !record.getMessage().isBlank()) {
                return record.getMessage();
            }
            return record.getOverallStatus() == ComparisonStatus.MATCH ? "MATCH" : "";
        }

        private String ratio(int count, int total) {
            if (total <= 0) {
                return "0.00%";
            }
            return String.format(java.util.Locale.ROOT, "%.2f%%", count * 100.0 / total);
        }

        private String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
            return "";
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }

        private CellStyle styleForStatus(String status) {
            if (status == null || status.isBlank()) {
                return neutralStyle;
            }
            String normalized = status.trim().toUpperCase();
            if (normalized.endsWith("_MATCH") || FULL_EXISTS.equals(normalized) || FULL_MATCH.equals(normalized) || LOW.equals(normalized) || "MATCH".equals(normalized)) {
                return positiveStyle;
            }
            if (normalized.contains("MISSING") || normalized.contains("NOT_FULL_EXISTS") || HIGH.equals(normalized) || normalized.contains("TYPE_MISMATCH")) {
                return highRiskStyle;
            }
            if (normalized.contains("LENGTH_MISMATCH") || normalized.contains("DEFAULT_MISMATCH") || normalized.contains("NULLABLE_MISMATCH")
                    || MEDIUM.equals(normalized) || OTHER.equals(normalized) || "MISMATCH".equals(normalized)) {
                return mediumRiskStyle;
            }
            return warningStyle;
        }
    }

    private static final class DetailSheetState {
        private final String baseSheetName;
        private SXSSFSheet currentSheet;
        private int sequence;
        private int nextRowIndex;

        private DetailSheetState(String baseSheetName) {
            this.baseSheetName = baseSheetName;
        }
    }

    private static final class TableStats {
        private final String sourceDatabaseName;
        private final String sourceSchemaName;
        private final String sourceTableName;
        private final String targetSchemaName;
        private final String targetTableName;
        private final EnumSet<DiffType> diffTypes = EnumSet.noneOf(DiffType.class);
        private int diffCount;
        private boolean mismatch;
        private boolean fullExists = true;
        private boolean typeMismatch;
        private boolean lengthMismatch;
        private boolean defaultMismatch;
        private boolean nullableMismatch;

        private TableStats(String sourceDatabaseName,
                           String sourceSchemaName,
                           String sourceTableName,
                           String targetSchemaName,
                           String targetTableName) {
            this.sourceDatabaseName = sourceDatabaseName;
            this.sourceSchemaName = sourceSchemaName;
            this.sourceTableName = sourceTableName;
            this.targetSchemaName = targetSchemaName;
            this.targetTableName = targetTableName;
        }

        private void accept(ColumnComparisonRecord record) {
            if (record.getOverallStatus() == ComparisonStatus.MISMATCH) {
                mismatch = true;
            }
            if (!record.isSourceColumnExists() || !record.isTargetColumnExists()) {
                fullExists = false;
            }
            List<DiffType> parsedDiffTypes = parseDiffTypes(record.getDiffTypes());
            if (parsedDiffTypes.isEmpty()) {
                if (record.getOverallStatus() == ComparisonStatus.MISMATCH) {
                    diffCount++;
                }
                return;
            }
            diffCount += parsedDiffTypes.size();
            diffTypes.addAll(parsedDiffTypes);
            if (parsedDiffTypes.contains(DiffType.COLUMN_TYPE_MISMATCH)) {
                typeMismatch = true;
            }
            if (parsedDiffTypes.contains(DiffType.COLUMN_LENGTH_MISMATCH)) {
                lengthMismatch = true;
            }
            if (parsedDiffTypes.contains(DiffType.COLUMN_DEFAULT_MISMATCH)) {
                defaultMismatch = true;
            }
            if (parsedDiffTypes.contains(DiffType.COLUMN_NULLABLE_MISMATCH)) {
                nullableMismatch = true;
            }
        }

        private String fieldExistenceStatus() {
            return fullExists ? FULL_EXISTS : NOT_FULL_EXISTS;
        }

        private String typeStatus() {
            return typeMismatch ? TYPE_MISMATCH : TYPE_MATCH;
        }

        private String lengthStatus() {
            return lengthMismatch ? LENGTH_MISMATCH : LENGTH_MATCH;
        }

        private String defaultStatus() {
            return defaultMismatch ? DEFAULT_MISMATCH : DEFAULT_MATCH;
        }

        private String nullableStatus() {
            return nullableMismatch ? NULLABLE_MISMATCH : NULLABLE_MATCH;
        }

        private String diffCategory() {
            if (!mismatch) {
                return FULL_MATCH;
            }
            if (containsAny(DiffType.SOURCE_TABLE_NOT_FOUND,
                    DiffType.SOURCE_TABLE_AMBIGUOUS,
                    DiffType.TARGET_TABLE_NOT_FOUND,
                    DiffType.COLUMN_MISSING_IN_SOURCE,
                    DiffType.COLUMN_MISSING_IN_TARGET)) {
                return MISSING_COLUMN;
            }
            if (typeMismatch) {
                return TYPE_MISMATCH;
            }
            if (lengthMismatch) {
                return LENGTH_MISMATCH;
            }
            return OTHER;
        }

        private String riskLevel() {
            if (!mismatch) {
                return LOW;
            }
            if (containsAny(DiffType.SOURCE_TABLE_NOT_FOUND,
                    DiffType.SOURCE_TABLE_AMBIGUOUS,
                    DiffType.TARGET_TABLE_NOT_FOUND,
                    DiffType.COLUMN_MISSING_IN_SOURCE,
                    DiffType.COLUMN_MISSING_IN_TARGET,
                    DiffType.COLUMN_TYPE_MISMATCH)) {
                return HIGH;
            }
            return MEDIUM;
        }

        private boolean containsAny(DiffType... candidates) {
            for (DiffType candidate : candidates) {
                if (diffTypes.contains(candidate)) {
                    return true;
                }
            }
            return false;
        }

        private int getDiffCount() {
            return diffCount;
        }

        private List<DiffType> parseDiffTypes(String raw) {
            List<DiffType> values = new ArrayList<>();
            if (raw == null || raw.isBlank()) {
                return values;
            }
            for (String token : raw.split("\\|")) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    values.add(DiffType.valueOf(trimmed));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown diff labels so report generation stays resilient.
                }
            }
            return values;
        }
    }

    @FunctionalInterface
    private interface StatusResolver {
        String resolve(TableStats tableStats);
    }
}
