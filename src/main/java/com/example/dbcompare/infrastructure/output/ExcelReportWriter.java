package com.example.dbcompare.infrastructure.output;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.domain.model.CompareOptions;
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
import java.util.List;

public class ExcelReportWriter {
    private static final String DETAIL_SHEET_NAME = "明细";

    private final int maxRowsPerSheet;

    public ExcelReportWriter() {
        this(SpreadsheetVersion.EXCEL2007.getMaxRows());
    }

    public ExcelReportWriter(int maxRowsPerSheet) {
        if (maxRowsPerSheet < 2) {
            throw new IllegalArgumentException("maxRowsPerSheet must allow at least one data row");
        }
        this.maxRowsPerSheet = maxRowsPerSheet;
    }

    public ExcelReportSession open(Path path, CompareOptions options) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            SXSSFWorkbook workbook = new SXSSFWorkbook(200);
            workbook.setCompressTempFiles(true);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle matchStyle = createStatusStyle(workbook, IndexedColors.LIGHT_GREEN);
            CellStyle mismatchStyle = createStatusStyle(workbook, IndexedColors.ROSE);
            CellStyle naStyle = createStatusStyle(workbook, IndexedColors.GREY_25_PERCENT);
            CellStyle infoStyle = createStatusStyle(workbook, IndexedColors.LIGHT_YELLOW);
            return new ExcelReportSession(path, workbook, headerStyle, matchStyle, mismatchStyle, naStyle, infoStyle, maxRowsPerSheet, buildColumns(options));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open Excel report: " + path, e);
        }
    }

    public ExcelReportSession open(Path path) {
        return open(path, new CompareOptions());
    }

    private List<DetailColumn> buildColumns(CompareOptions options) {
        List<DetailColumn> columns = new ArrayList<>();
        columns.add(new DetailColumn("源数据库", 20, (row, record) -> row.write(record.getSourceDatabaseName(), null)));
        columns.add(new DetailColumn("源Schema", 20, (row, record) -> row.write(record.getSourceSchemaName(), null)));
        columns.add(new DetailColumn("源表", 20, (row, record) -> row.write(record.getSourceTableName(), null)));
        columns.add(new DetailColumn("目标Schema", 20, (row, record) -> row.write(record.getTargetSchemaName(), null)));
        columns.add(new DetailColumn("目标对象", 20, (row, record) -> row.write(record.getTargetTableName(), null)));
        columns.add(new DetailColumn("字段名", 20, (row, record) -> row.write(record.getColumnName(), null)));
        columns.add(new DetailColumn("源端存在", 16, (row, record) -> row.write(OutputTextFormatter.boolText(record.isSourceColumnExists()), null)));
        columns.add(new DetailColumn("目标端存在", 16, (row, record) -> row.write(OutputTextFormatter.boolText(record.isTargetColumnExists()), null)));
        columns.add(new DetailColumn("源类型", 18, (row, record) -> row.write(record.getSourceType(), null)));
        columns.add(new DetailColumn("目标类型", 18, (row, record) -> row.write(record.getTargetType(), null)));
        columns.add(new DetailColumn("类型状态", 18, (row, record) -> row.write(statusText(record.getTypeStatus()), row.statusStyle(record))));
        columns.add(new DetailColumn("源长度", 18, (row, record) -> row.write(record.getSourceLength(), null)));
        columns.add(new DetailColumn("目标长度", 18, (row, record) -> row.write(record.getTargetLength(), null)));
        columns.add(new DetailColumn("长度状态", 18, (row, record) -> row.write(statusText(record.getLengthStatus()), row.statusStyle(record))));
        if (options.isCompareDefaultValue()) {
            columns.add(new DetailColumn("源默认值", 18, (row, record) -> row.write(record.getSourceDefaultValue(), null)));
            columns.add(new DetailColumn("目标默认值", 18, (row, record) -> row.write(record.getTargetDefaultValue(), null)));
            columns.add(new DetailColumn("默认值状态", 18, (row, record) -> row.write(statusText(record.getDefaultStatus()), row.statusStyle(record))));
        }
        if (options.isCompareNullable()) {
            columns.add(new DetailColumn("源端可空", 18, (row, record) -> row.write(OutputTextFormatter.nullableText(record.getSourceNullable()), null)));
            columns.add(new DetailColumn("目标端可空", 18, (row, record) -> row.write(OutputTextFormatter.nullableText(record.getTargetNullable()), null)));
            columns.add(new DetailColumn("可空状态", 18, (row, record) -> row.write(statusText(record.getNullableStatus()), row.statusStyle(record))));
        }
        columns.add(new DetailColumn("整体状态", 18, (row, record) -> row.write(statusText(record.getOverallStatus()), row.statusStyle(record))));
        columns.add(new DetailColumn("差异分组", 18, (row, record) -> row.write(OutputTextFormatter.diffGroupText(record.getDiffGroup()), row.statusStyle(record))));
        columns.add(new DetailColumn("是否影响结果", 18, (row, record) -> row.write(OutputTextFormatter.boolText(record.isAffectsResult()), row.statusStyle(record))));
        columns.add(new DetailColumn("差异类型", 22, (row, record) -> row.write(OutputTextFormatter.diffTypesText(record.getDiffTypes()), null)));
        columns.add(new DetailColumn("说明", 60, (row, record) -> row.write(OutputTextFormatter.messageText(record.getMessage()), null)));
        return columns;
    }

    private static String statusText(ComparisonStatus status) {
        return OutputTextFormatter.comparisonStatusText(status);
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

    private CellStyle createStatusStyle(SXSSFWorkbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    public class ExcelReportSession implements Closeable {
        private final Path path;
        private final SXSSFWorkbook workbook;
        private final CellStyle headerStyle;
        private final CellStyle matchStyle;
        private final CellStyle mismatchStyle;
        private final CellStyle naStyle;
        private final CellStyle infoStyle;
        private final int maxRowsPerSheet;
        private final List<DetailColumn> columns;

        private SXSSFSheet sheet;
        private int sheetSequence = 0;
        private int rowIndex = 1;

        private ExcelReportSession(Path path,
                                   SXSSFWorkbook workbook,
                                   CellStyle headerStyle,
                                   CellStyle matchStyle,
                                   CellStyle mismatchStyle,
                                   CellStyle naStyle,
                                   CellStyle infoStyle,
                                   int maxRowsPerSheet,
                                   List<DetailColumn> columns) {
            this.path = path;
            this.workbook = workbook;
            this.headerStyle = headerStyle;
            this.matchStyle = matchStyle;
            this.mismatchStyle = mismatchStyle;
            this.naStyle = naStyle;
            this.infoStyle = infoStyle;
            this.maxRowsPerSheet = maxRowsPerSheet;
            this.columns = columns;
            this.sheet = createSheet();
        }

        public void append(List<ColumnComparisonRecord> records) {
            for (ColumnComparisonRecord record : records) {
                ensureCapacityForNextRow();
                Row row = sheet.createRow(rowIndex++);
                RowWriter rowWriter = new RowWriter(row);
                for (DetailColumn column : columns) {
                    column.writer.write(rowWriter, record);
                }
            }
        }

        @Override
        public void close() throws IOException {
            applyAutoFilter(sheet, rowIndex);
            try (OutputStream outputStream = Files.newOutputStream(path)) {
                workbook.write(outputStream);
            } finally {
                workbook.dispose();
                workbook.close();
            }
        }

        private void ensureCapacityForNextRow() {
            if (rowIndex < maxRowsPerSheet) {
                return;
            }
            applyAutoFilter(sheet, rowIndex);
            sheet = createSheet();
            rowIndex = 1;
        }

        private SXSSFSheet createSheet() {
            String sheetName = sheetSequence == 0 ? DETAIL_SHEET_NAME : DETAIL_SHEET_NAME + "_" + (sheetSequence + 1);
            sheetSequence++;
            SXSSFSheet createdSheet = workbook.createSheet(sheetName);
            Row header = createdSheet.createRow(0);
            for (int index = 0; index < columns.size(); index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(columns.get(index).header);
                cell.setCellStyle(headerStyle);
                createdSheet.setColumnWidth(index, columns.get(index).widthChars * 256);
            }
            createdSheet.createFreezePane(0, 1);
            return createdSheet;
        }

        private void applyAutoFilter(SXSSFSheet targetSheet, int nextRowIndex) {
            int lastRowIndex = Math.max(nextRowIndex - 1, 1);
            targetSheet.setAutoFilter(new CellRangeAddress(0, lastRowIndex, 0, columns.size() - 1));
        }

        private CellStyle statusStyle(ColumnComparisonRecord record) {
            if (!record.isAffectsResult()) {
                return infoStyle;
            }
            ComparisonStatus status = record.getOverallStatus();
            if (status == null) {
                return null;
            }
            return switch (status) {
                case MATCH -> matchStyle;
                case MISMATCH -> mismatchStyle;
                case NOT_APPLICABLE -> naStyle;
            };
        }

        private final class RowWriter {
            private final Row row;
            private int columnIndex;

            private RowWriter(Row row) {
                this.row = row;
            }

            private void write(String value, CellStyle style) {
                Cell cell = row.createCell(columnIndex++);
                cell.setCellValue(value == null ? "" : value);
                if (style != null) {
                    cell.setCellStyle(style);
                }
            }

            private CellStyle statusStyle(ColumnComparisonRecord record) {
                return ExcelReportSession.this.statusStyle(record);
            }
        }
    }

    private record DetailColumn(String header, int widthChars, ColumnValueWriter writer) {
    }

    @FunctionalInterface
    private interface ColumnValueWriter {
        void write(ExcelReportSession.RowWriter row, ColumnComparisonRecord record);
    }
}
