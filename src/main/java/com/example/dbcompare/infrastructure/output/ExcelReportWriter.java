package com.example.dbcompare.infrastructure.output;

import com.example.dbcompare.domain.enums.ComparisonStatus;
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
import java.util.List;

public class ExcelReportWriter {
    private static final String DETAIL_SHEET_NAME = "明细";
    static final String[] DETAIL_HEADERS = {
            "源数据库", "源Schema", "源表", "目标Schema", "目标表", "字段名",
            "源端存在", "目标端存在",
            "源类型", "目标类型", "类型状态",
            "源长度", "目标长度", "长度状态",
            "源默认值", "目标默认值", "默认值状态",
            "源端可空", "目标端可空", "可空状态",
            "总体状态", "差异类型", "说明"
    };

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

    public ExcelReportSession open(Path path) {
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
            return new ExcelReportSession(path, workbook, headerStyle, matchStyle, mismatchStyle, naStyle, maxRowsPerSheet);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open Excel report: " + path, e);
        }
    }

    private int defaultColumnWidth(int columnIndex) {
        if (columnIndex == DETAIL_HEADERS.length - 1) {
            return 60 * 256;
        }
        if (columnIndex >= 8 && columnIndex <= 20) {
            return 18 * 256;
        }
        return 20 * 256;
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
        private final int maxRowsPerSheet;

        private SXSSFSheet sheet;
        private int sheetSequence = 0;
        private int rowIndex = 1;

        private ExcelReportSession(Path path,
                                   SXSSFWorkbook workbook,
                                   CellStyle headerStyle,
                                   CellStyle matchStyle,
                                   CellStyle mismatchStyle,
                                   CellStyle naStyle,
                                   int maxRowsPerSheet) {
            this.path = path;
            this.workbook = workbook;
            this.headerStyle = headerStyle;
            this.matchStyle = matchStyle;
            this.mismatchStyle = mismatchStyle;
            this.naStyle = naStyle;
            this.maxRowsPerSheet = maxRowsPerSheet;
            this.sheet = createSheet();
        }

        public void append(List<ColumnComparisonRecord> records) {
            for (ColumnComparisonRecord record : records) {
                ensureCapacityForNextRow();
                Row row = sheet.createRow(rowIndex++);
                int columnIndex = 0;
                writeCell(row, columnIndex++, record.getSourceDatabaseName(), null);
                writeCell(row, columnIndex++, record.getSourceSchemaName(), null);
                writeCell(row, columnIndex++, record.getSourceTableName(), null);
                writeCell(row, columnIndex++, record.getTargetSchemaName(), null);
                writeCell(row, columnIndex++, record.getTargetTableName(), null);
                writeCell(row, columnIndex++, record.getColumnName(), null);
                writeCell(row, columnIndex++, OutputTextFormatter.boolText(record.isSourceColumnExists()), null);
                writeCell(row, columnIndex++, OutputTextFormatter.boolText(record.isTargetColumnExists()), null);
                writeCell(row, columnIndex++, record.getSourceType(), null);
                writeCell(row, columnIndex++, record.getTargetType(), null);
                writeCell(row, columnIndex++, statusText(record.getTypeStatus()), statusStyle(record.getTypeStatus()));
                writeCell(row, columnIndex++, record.getSourceLength(), null);
                writeCell(row, columnIndex++, record.getTargetLength(), null);
                writeCell(row, columnIndex++, statusText(record.getLengthStatus()), statusStyle(record.getLengthStatus()));
                writeCell(row, columnIndex++, record.getSourceDefaultValue(), null);
                writeCell(row, columnIndex++, record.getTargetDefaultValue(), null);
                writeCell(row, columnIndex++, statusText(record.getDefaultStatus()), statusStyle(record.getDefaultStatus()));
                writeCell(row, columnIndex++, OutputTextFormatter.nullableText(record.getSourceNullable()), null);
                writeCell(row, columnIndex++, OutputTextFormatter.nullableText(record.getTargetNullable()), null);
                writeCell(row, columnIndex++, statusText(record.getNullableStatus()), statusStyle(record.getNullableStatus()));
                writeCell(row, columnIndex++, statusText(record.getOverallStatus()), statusStyle(record.getOverallStatus()));
                writeCell(row, columnIndex++, OutputTextFormatter.diffTypesText(record.getDiffTypes()), null);
                writeCell(row, columnIndex, OutputTextFormatter.messageText(record.getMessage()), null);
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
            for (int index = 0; index < DETAIL_HEADERS.length; index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(DETAIL_HEADERS[index]);
                cell.setCellStyle(headerStyle);
                createdSheet.setColumnWidth(index, defaultColumnWidth(index));
            }
            createdSheet.createFreezePane(0, 1);
            return createdSheet;
        }

        private void applyAutoFilter(SXSSFSheet targetSheet, int nextRowIndex) {
            int lastRowIndex = Math.max(nextRowIndex - 1, 1);
            targetSheet.setAutoFilter(new CellRangeAddress(0, lastRowIndex, 0, DETAIL_HEADERS.length - 1));
        }

        private void writeCell(Row row, int columnIndex, String value, CellStyle style) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(value == null ? "" : value);
            if (style != null) {
                cell.setCellStyle(style);
            }
        }

        private String statusText(ComparisonStatus status) {
            return OutputTextFormatter.comparisonStatusText(status);
        }

        private CellStyle statusStyle(ComparisonStatus status) {
            if (status == null) {
                return null;
            }
            return switch (status) {
                case MATCH -> matchStyle;
                case MISMATCH -> mismatchStyle;
                case NOT_APPLICABLE -> naStyle;
            };
        }
    }
}
