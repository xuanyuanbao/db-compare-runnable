package com.example.dbcompare.infrastructure.output;

import com.example.dbcompare.domain.model.TargetViewLineageReportRow;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TargetViewLineageExcelWriter {
    private static final String SHEET_NAME = "目标View血缘";
    static final String[] HEADERS = {
            "源数据库", "源Schema", "源表", "目标基表Schema", "目标基表", "目标View", "目标ViewSchema"
    };

    private final int maxRowsPerSheet;

    public TargetViewLineageExcelWriter() {
        this(SpreadsheetVersion.EXCEL2007.getMaxRows());
    }

    public TargetViewLineageExcelWriter(int maxRowsPerSheet) {
        if (maxRowsPerSheet < 2) {
            throw new IllegalArgumentException("maxRowsPerSheet must allow at least one data row");
        }
        this.maxRowsPerSheet = maxRowsPerSheet;
    }

    public TargetViewLineageExcelSession open(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            SXSSFWorkbook workbook = new SXSSFWorkbook(200);
            workbook.setCompressTempFiles(true);
            CellStyle headerStyle = createHeaderStyle(workbook);
            return new TargetViewLineageExcelSession(path, workbook, headerStyle, maxRowsPerSheet);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open target view lineage Excel report: " + path, e);
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

    private int defaultColumnWidth(int columnIndex) {
        if (columnIndex == 0 || columnIndex == 1 || columnIndex == 2) {
            return 18 * 256;
        }
        return 22 * 256;
    }

    public final class TargetViewLineageExcelSession implements Closeable {
        private final Path path;
        private final SXSSFWorkbook workbook;
        private final CellStyle headerStyle;
        private final int maxRowsPerSheet;
        private final Set<String> writtenKeys = new LinkedHashSet<>();

        private SXSSFSheet sheet;
        private int sheetSequence = 0;
        private int rowIndex = 1;

        private TargetViewLineageExcelSession(Path path,
                                              SXSSFWorkbook workbook,
                                              CellStyle headerStyle,
                                              int maxRowsPerSheet) {
            this.path = path;
            this.workbook = workbook;
            this.headerStyle = headerStyle;
            this.maxRowsPerSheet = maxRowsPerSheet;
            this.sheet = createSheet();
        }

        public void append(List<TargetViewLineageReportRow> rows) {
            for (TargetViewLineageReportRow rowData : rows) {
                String key = String.join("|",
                        safe(rowData.getSourceDatabase()),
                        safe(rowData.getSourceSchema()),
                        safe(rowData.getSourceTable()),
                        safe(rowData.getTargetTableSchema()),
                        safe(rowData.getTargetTable()),
                        safe(rowData.getTargetView()),
                        safe(rowData.getTargetViewSchema()));
                if (!writtenKeys.add(key)) {
                    continue;
                }
                ensureCapacityForNextRow();
                Row row = sheet.createRow(rowIndex++);
                int columnIndex = 0;
                writeCell(row, columnIndex++, rowData.getSourceDatabase());
                writeCell(row, columnIndex++, rowData.getSourceSchema());
                writeCell(row, columnIndex++, rowData.getSourceTable());
                writeCell(row, columnIndex++, rowData.getTargetTableSchema());
                writeCell(row, columnIndex++, rowData.getTargetTable());
                writeCell(row, columnIndex++, rowData.getTargetView());
                writeCell(row, columnIndex, rowData.getTargetViewSchema());
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
            String sheetName = sheetSequence == 0 ? SHEET_NAME : SHEET_NAME + "_" + (sheetSequence + 1);
            sheetSequence++;
            SXSSFSheet createdSheet = workbook.createSheet(sheetName);
            Row header = createdSheet.createRow(0);
            for (int index = 0; index < HEADERS.length; index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(HEADERS[index]);
                cell.setCellStyle(headerStyle);
                createdSheet.setColumnWidth(index, defaultColumnWidth(index));
            }
            createdSheet.createFreezePane(0, 1);
            return createdSheet;
        }

        private void applyAutoFilter(SXSSFSheet targetSheet, int nextRowIndex) {
            int lastRowIndex = Math.max(nextRowIndex - 1, 1);
            targetSheet.setAutoFilter(new CellRangeAddress(0, lastRowIndex, 0, HEADERS.length - 1));
        }

        private void writeCell(Row row, int columnIndex, String value) {
            Cell cell = row.createCell(columnIndex);
            cell.setCellValue(safe(value));
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
