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

public class SummaryExcelReportWriter {
    private static final String OVERVIEW_SHEET_NAME = "Overview";
    private static final String FIELD_EXISTENCE_SUMMARY_SHEET_NAME = "Field Existence Summary";
    private static final String TYPE_SUMMARY_SHEET_NAME = "Type Summary";
    private static final String LENGTH_SUMMARY_SHEET_NAME = "Length Summary";
    private static final String DEFAULT_SUMMARY_SHEET_NAME = "Default Summary";
    private static final String TABLE_STATUS_SHEET_NAME = "Table Status";
    private static final String DIFF_SUMMARY_SHEET_NAME = "Diff Summary";
    private static final String RISK_SUMMARY_SHEET_NAME = "Risk Summary";
    private static final String SCHEMA_DISTRIBUTION_SHEET_NAME = "Schema Distribution";
    private static final String TOP_ISSUES_SHEET_NAME = "Top Issue Tables";
    private static final String DETAIL_SHEET_NAME = "Detail";

    private static final String FULL_EXISTS = "FULL_EXISTS";
    private static final String NOT_FULL_EXISTS = "NOT_FULL_EXISTS";
    private static final String TYPE_MATCH = "TYPE_MATCH";
    private static final String TYPE_MISMATCH = "TYPE_MISMATCH";
    private static final String LENGTH_MATCH = "LENGTH_MATCH";
    private static final String LENGTH_MISMATCH = "LENGTH_MISMATCH";
    private static final String DEFAULT_MATCH = "DEFAULT_MATCH";
    private static final String DEFAULT_MISMATCH = "DEFAULT_MISMATCH";
    private static final String FULL_MATCH = "FULL_MATCH";
    private static final String MISSING_COLUMN = "MISSING_COLUMN";
    private static final String OTHER = "OTHER";
    private static final String LOW = "LOW";
    private static final String MEDIUM = "MEDIUM";
    private static final String HIGH = "HIGH";

    private static final String[] OVERVIEW_HEADERS = {"metric", "value"};
    private static final String[] FIELD_EXISTENCE_SUMMARY_HEADERS = {"fieldExistenceStatus", "tableCount", "ratio"};
    private static final String[] TYPE_SUMMARY_HEADERS = {"typeStatus", "tableCount", "ratio"};
    private static final String[] LENGTH_SUMMARY_HEADERS = {"lengthStatus", "tableCount", "ratio"};
    private static final String[] DEFAULT_SUMMARY_HEADERS = {"defaultStatus", "tableCount", "ratio"};
    private static final String[] TABLE_STATUS_HEADERS = {"sourceDatabase", "sourceSchema", "sourceTable", "targetSchema", "targetTable", "tableStatus"};
    private static final String[] DIFF_SUMMARY_HEADERS = {"diffCategory", "tableCount", "ratio"};
    private static final String[] RISK_SUMMARY_HEADERS = {"riskLevel", "tableCount", "ratio"};
    private static final String[] SCHEMA_DISTRIBUTION_HEADERS = {"schema", "tableCount"};
    private static final String[] TOP_ISSUES_HEADERS = {"table", "diffCount"};
    private static final String[] DETAIL_HEADERS = {"source_db", "schema", "table", "column", "diff_type", "detail"};

    private static final List<String> FIELD_EXISTENCE_ORDER = List.of(FULL_EXISTS, NOT_FULL_EXISTS);
    private static final List<String> TYPE_SUMMARY_ORDER = List.of(TYPE_MATCH, TYPE_MISMATCH);
    private static final List<String> LENGTH_SUMMARY_ORDER = List.of(LENGTH_MATCH, LENGTH_MISMATCH);
    private static final List<String> DEFAULT_SUMMARY_ORDER = List.of(DEFAULT_MATCH, DEFAULT_MISMATCH);
    private static final List<String> DIFF_CATEGORY_ORDER = List.of(FULL_MATCH, MISSING_COLUMN, TYPE_MISMATCH, LENGTH_MISMATCH, OTHER);
    private static final List<String> RISK_LEVEL_ORDER = List.of(LOW, MEDIUM, HIGH);

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
            CellStyle headerStyle = createHeaderStyle(workbook);
            return new SummaryExcelReportSession(path, workbook, headerStyle, maxRowsPerSheet);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open summary Excel report: " + path, e);
        }
    }

    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private int defaultColumnWidth(int columnIndex, int headerCount) {
        if (headerCount == DETAIL_HEADERS.length && columnIndex == DETAIL_HEADERS.length - 1) {
            return 60 * 256;
        }
        if (headerCount == TABLE_STATUS_HEADERS.length && columnIndex < 5) {
            return 20 * 256;
        }
        if (headerCount == TOP_ISSUES_HEADERS.length && columnIndex == 0) {
            return 40 * 256;
        }
        return 20 * 256;
    }

    public final class SummaryExcelReportSession implements Closeable {
        private final Path path;
        private final SXSSFWorkbook workbook;
        private final CellStyle headerStyle;
        private final int maxRowsPerSheet;
        private final SXSSFSheet overviewSheet;
        private final SXSSFSheet fieldExistenceSummarySheet;
        private final SXSSFSheet typeSummarySheet;
        private final SXSSFSheet lengthSummarySheet;
        private final SXSSFSheet defaultSummarySheet;
        private final SXSSFSheet diffSummarySheet;
        private final SXSSFSheet riskSummarySheet;
        private final SXSSFSheet schemaDistributionSheet;
        private final SXSSFSheet topIssuesSheet;
        private final Map<String, TableStats> tableStats = new LinkedHashMap<>();

        private SXSSFSheet tableStatusSheet;
        private int tableStatusSheetSequence = 0;
        private int tableStatusRowIndex = 1;

        private SXSSFSheet detailSheet;
        private int detailSheetSequence = 0;
        private int detailRowIndex = 1;

        private SummaryExcelReportSession(Path path,
                                          SXSSFWorkbook workbook,
                                          CellStyle headerStyle,
                                          int maxRowsPerSheet) {
            this.path = path;
            this.workbook = workbook;
            this.headerStyle = headerStyle;
            this.maxRowsPerSheet = maxRowsPerSheet;
            this.overviewSheet = createSheet(OVERVIEW_SHEET_NAME, OVERVIEW_HEADERS);
            this.fieldExistenceSummarySheet = createSheet(FIELD_EXISTENCE_SUMMARY_SHEET_NAME, FIELD_EXISTENCE_SUMMARY_HEADERS);
            this.typeSummarySheet = createSheet(TYPE_SUMMARY_SHEET_NAME, TYPE_SUMMARY_HEADERS);
            this.lengthSummarySheet = createSheet(LENGTH_SUMMARY_SHEET_NAME, LENGTH_SUMMARY_HEADERS);
            this.defaultSummarySheet = createSheet(DEFAULT_SUMMARY_SHEET_NAME, DEFAULT_SUMMARY_HEADERS);
            this.tableStatusSheet = createTableStatusSheet();
            this.diffSummarySheet = createSheet(DIFF_SUMMARY_SHEET_NAME, DIFF_SUMMARY_HEADERS);
            this.riskSummarySheet = createSheet(RISK_SUMMARY_SHEET_NAME, RISK_SUMMARY_HEADERS);
            this.schemaDistributionSheet = createSheet(SCHEMA_DISTRIBUTION_SHEET_NAME, SCHEMA_DISTRIBUTION_HEADERS);
            this.topIssuesSheet = createSheet(TOP_ISSUES_SHEET_NAME, TOP_ISSUES_HEADERS);
            this.detailSheet = createDetailSheet();
        }

        public void append(List<ColumnComparisonRecord> records) {
            for (ColumnComparisonRecord record : records) {
                collect(record);
                writeDetail(record);
            }
        }

        @Override
        public void close() throws IOException {
            renderOverview();
            renderStatusSummary(fieldExistenceSummarySheet, FIELD_EXISTENCE_ORDER, TableStats::fieldExistenceStatus);
            renderStatusSummary(typeSummarySheet, TYPE_SUMMARY_ORDER, TableStats::typeSummaryStatus);
            renderStatusSummary(lengthSummarySheet, LENGTH_SUMMARY_ORDER, TableStats::lengthSummaryStatus);
            renderStatusSummary(defaultSummarySheet, DEFAULT_SUMMARY_ORDER, TableStats::defaultSummaryStatus);
            renderTableStatus();
            renderDiffSummary();
            renderRiskSummary();
            renderSchemaDistribution();
            renderTopIssues();
            applyAutoFilter(tableStatusSheet, tableStatusRowIndex, TABLE_STATUS_HEADERS.length);
            applyAutoFilter(detailSheet, detailRowIndex, DETAIL_HEADERS.length);
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                workbook.write(outputStream);
            } finally {
                workbook.dispose();
                workbook.close();
            }
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

        private void writeDetail(ColumnComparisonRecord record) {
            ensureDetailCapacityForNextRow();
            Row row = detailSheet.createRow(detailRowIndex++);
            writeCell(row, 0, safe(record.getSourceDatabaseName()));
            writeCell(row, 1, firstNonBlank(record.getSourceSchemaName(), record.getTargetSchemaName(), ""));
            writeCell(row, 2, firstNonBlank(record.getSourceTableName(), record.getTargetTableName(), ""));
            writeCell(row, 3, safe(record.getColumnName()));
            writeCell(row, 4, detailDiffType(record));
            writeCell(row, 5, detailMessage(record));
        }

        private void renderOverview() {
            int totalTables = tableStats.size();
            int fullExistsTables = countByStatus(TableStats::fieldExistenceStatus, FULL_EXISTS);
            int notFullExistsTables = countByStatus(TableStats::fieldExistenceStatus, NOT_FULL_EXISTS);
            int typeMismatchTables = countByStatus(TableStats::typeSummaryStatus, TYPE_MISMATCH);
            int lengthMismatchTables = countByStatus(TableStats::lengthSummaryStatus, LENGTH_MISMATCH);
            int defaultMismatchTables = countByStatus(TableStats::defaultSummaryStatus, DEFAULT_MISMATCH);
            int rowIndex = 1;
            rowIndex = writeMetricRow(overviewSheet, rowIndex, "totalTables", Integer.toString(totalTables));
            rowIndex = writeMetricRow(overviewSheet, rowIndex, "fullExistsTables", Integer.toString(fullExistsTables));
            rowIndex = writeMetricRow(overviewSheet, rowIndex, "notFullExistsTables", Integer.toString(notFullExistsTables));
            rowIndex = writeMetricRow(overviewSheet, rowIndex, "typeMismatchTables", Integer.toString(typeMismatchTables));
            rowIndex = writeMetricRow(overviewSheet, rowIndex, "lengthMismatchTables", Integer.toString(lengthMismatchTables));
            rowIndex = writeMetricRow(overviewSheet, rowIndex, "defaultMismatchTables", Integer.toString(defaultMismatchTables));
            writeMetricRow(overviewSheet, rowIndex, "fullExistsRatio", ratio(fullExistsTables, totalTables));
            applyAutoFilter(overviewSheet, rowIndex + 1, OVERVIEW_HEADERS.length);
        }

        private void renderStatusSummary(SXSSFSheet sheet, List<String> orderedStatuses, StatusResolver resolver) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String status : orderedStatuses) {
                counts.put(status, 0);
            }
            for (TableStats stats : tableStats.values()) {
                counts.merge(resolver.resolve(stats), 1, Integer::sum);
            }
            int totalTables = tableStats.size();
            int rowIndex = 1;
            for (String status : orderedStatuses) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, status);
                writeCell(row, 1, Integer.toString(counts.get(status)));
                writeCell(row, 2, ratio(counts.get(status), totalTables));
            }
            applyAutoFilter(sheet, rowIndex, 3);
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
                writeCell(row, 5, stats.fieldExistenceStatus());
            }
        }

        private void renderDiffSummary() {
            Map<String, Integer> categoryCount = new LinkedHashMap<>();
            for (String category : DIFF_CATEGORY_ORDER) {
                categoryCount.put(category, 0);
            }
            for (TableStats stats : tableStats.values()) {
                categoryCount.merge(stats.primaryDiffCategory(), 1, Integer::sum);
            }

            int totalTables = tableStats.size();
            int rowIndex = 1;
            for (String category : DIFF_CATEGORY_ORDER) {
                Row row = diffSummarySheet.createRow(rowIndex++);
                writeCell(row, 0, category);
                writeCell(row, 1, Integer.toString(categoryCount.get(category)));
                writeCell(row, 2, ratio(categoryCount.get(category), totalTables));
            }
            applyAutoFilter(diffSummarySheet, rowIndex, DIFF_SUMMARY_HEADERS.length);
        }

        private void renderRiskSummary() {
            Map<String, Integer> riskCount = new LinkedHashMap<>();
            for (String risk : RISK_LEVEL_ORDER) {
                riskCount.put(risk, 0);
            }
            for (TableStats stats : tableStats.values()) {
                riskCount.merge(stats.riskLevel(), 1, Integer::sum);
            }

            int totalTables = tableStats.size();
            int rowIndex = 1;
            for (String risk : RISK_LEVEL_ORDER) {
                Row row = riskSummarySheet.createRow(rowIndex++);
                writeCell(row, 0, risk);
                writeCell(row, 1, Integer.toString(riskCount.get(risk)));
                writeCell(row, 2, ratio(riskCount.get(risk), totalTables));
            }
            applyAutoFilter(riskSummarySheet, rowIndex, RISK_SUMMARY_HEADERS.length);
        }

        private void renderSchemaDistribution() {
            Map<String, Integer> schemaCount = new LinkedHashMap<>();
            for (TableStats stats : tableStats.values()) {
                schemaCount.merge(stats.sourceSchemaName, 1, Integer::sum);
            }

            List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(schemaCount.entrySet());
            sortedEntries.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                    .thenComparing(Map.Entry.comparingByKey()));

            int rowIndex = 1;
            for (Map.Entry<String, Integer> entry : sortedEntries) {
                Row row = schemaDistributionSheet.createRow(rowIndex++);
                writeCell(row, 0, entry.getKey());
                writeCell(row, 1, Integer.toString(entry.getValue()));
            }
            applyAutoFilter(schemaDistributionSheet, Math.max(rowIndex, 1), SCHEMA_DISTRIBUTION_HEADERS.length);
        }

        private void renderTopIssues() {
            List<TableStats> sorted = new ArrayList<>(tableStats.values());
            sorted.removeIf(stats -> stats.diffCount == 0);
            sorted.sort(Comparator.comparingInt(TableStats::getDiffCount)
                    .reversed()
                    .thenComparing(TableStats::qualifiedName));

            int rowIndex = 1;
            for (TableStats stats : sorted) {
                Row row = topIssuesSheet.createRow(rowIndex++);
                writeCell(row, 0, stats.qualifiedName());
                writeCell(row, 1, Integer.toString(stats.diffCount));
            }
            applyAutoFilter(topIssuesSheet, Math.max(rowIndex, 1), TOP_ISSUES_HEADERS.length);
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

        private int writeMetricRow(SXSSFSheet sheet, int rowIndex, String metric, String value) {
            Row row = sheet.createRow(rowIndex);
            writeCell(row, 0, metric);
            writeCell(row, 1, value);
            return rowIndex + 1;
        }

        private void ensureTableStatusCapacityForNextRow() {
            if (tableStatusRowIndex < maxRowsPerSheet) {
                return;
            }
            applyAutoFilter(tableStatusSheet, tableStatusRowIndex, TABLE_STATUS_HEADERS.length);
            tableStatusSheet = createTableStatusSheet();
            tableStatusRowIndex = 1;
        }

        private void ensureDetailCapacityForNextRow() {
            if (detailRowIndex < maxRowsPerSheet) {
                return;
            }
            applyAutoFilter(detailSheet, detailRowIndex, DETAIL_HEADERS.length);
            detailSheet = createDetailSheet();
            detailRowIndex = 1;
        }

        private SXSSFSheet createTableStatusSheet() {
            String sheetName = tableStatusSheetSequence == 0 ? TABLE_STATUS_SHEET_NAME : TABLE_STATUS_SHEET_NAME + "_" + (tableStatusSheetSequence + 1);
            tableStatusSheetSequence++;
            return createSheet(sheetName, TABLE_STATUS_HEADERS);
        }

        private SXSSFSheet createDetailSheet() {
            String sheetName = detailSheetSequence == 0 ? DETAIL_SHEET_NAME : DETAIL_SHEET_NAME + "_" + (detailSheetSequence + 1);
            detailSheetSequence++;
            return createSheet(sheetName, DETAIL_HEADERS);
        }

        private SXSSFSheet createSheet(String sheetName, String[] headers) {
            SXSSFSheet sheet = workbook.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(index, defaultColumnWidth(index, headers.length));
            }
            sheet.createFreezePane(0, 1);
            return sheet;
        }

        private void applyAutoFilter(SXSSFSheet sheet, int nextRowIndex, int headerCount) {
            int lastRowIndex = Math.max(nextRowIndex - 1, 0);
            sheet.setAutoFilter(new CellRangeAddress(0, lastRowIndex, 0, headerCount - 1));
        }

        private void writeCell(Row row, int columnIndex, String value) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(value == null ? "" : value);
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
        }

        private String fieldExistenceStatus() {
            return fullExists ? FULL_EXISTS : NOT_FULL_EXISTS;
        }

        private String typeSummaryStatus() {
            return typeMismatch ? TYPE_MISMATCH : TYPE_MATCH;
        }

        private String lengthSummaryStatus() {
            return lengthMismatch ? LENGTH_MISMATCH : LENGTH_MATCH;
        }

        private String defaultSummaryStatus() {
            return defaultMismatch ? DEFAULT_MISMATCH : DEFAULT_MATCH;
        }

        private String primaryDiffCategory() {
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

        private String qualifiedName() {
            return sourceDatabaseName + "." + sourceSchemaName + "." + sourceTableName + " -> " + targetSchemaName + "." + targetTableName;
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