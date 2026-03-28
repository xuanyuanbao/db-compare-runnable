package com.example.dbcompare.infrastructure.output;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExcelReportWriter {
    private static final String[] HEADERS = {
            "sourceDatabase", "sourceSchema", "sourceTable", "targetSchema", "targetTable", "columnName",
            "sourceExists", "targetExists",
            "sourceType", "targetType", "typeStatus",
            "sourceLength", "targetLength", "lengthStatus",
            "sourceDefault", "targetDefault", "defaultStatus",
            "sourceNullable", "targetNullable", "nullableStatus",
            "overallStatus", "diffTypes", "message"
    };

    public void write(Path path, List<ColumnComparisonRecord> records) {
        try (Workbook workbook = new XSSFWorkbook()) {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle matchStyle = createStatusStyle(workbook, IndexedColors.LIGHT_GREEN);
            CellStyle mismatchStyle = createStatusStyle(workbook, IndexedColors.ROSE);
            CellStyle naStyle = createStatusStyle(workbook, IndexedColors.GREY_25_PERCENT);

            Sheet sheet = workbook.createSheet("Detail");
            writeHeader(sheet, headerStyle);
            writeRows(sheet, records, matchStyle, mismatchStyle, naStyle);
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(records.size(), 1), 0, HEADERS.length - 1));
            for (int columnIndex = 0; columnIndex < HEADERS.length; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
                sheet.setColumnWidth(columnIndex, Math.min(sheet.getColumnWidth(columnIndex) + 512, 40 * 256));
            }

            try (OutputStream outputStream = Files.newOutputStream(path)) {
                workbook.write(outputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write Excel report: " + path, e);
        }
    }

    private void writeHeader(Sheet sheet, CellStyle headerStyle) {
        Row header = sheet.createRow(0);
        for (int index = 0; index < HEADERS.length; index++) {
            Cell cell = header.createCell(index);
            cell.setCellValue(HEADERS[index]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeRows(Sheet sheet,
                           List<ColumnComparisonRecord> records,
                           CellStyle matchStyle,
                           CellStyle mismatchStyle,
                           CellStyle naStyle) {
        for (int rowIndex = 0; rowIndex < records.size(); rowIndex++) {
            ColumnComparisonRecord record = records.get(rowIndex);
            Row row = sheet.createRow(rowIndex + 1);
            int columnIndex = 0;
            writeCell(row, columnIndex++, record.getSourceDatabaseName(), null);
            writeCell(row, columnIndex++, record.getSourceSchemaName(), null);
            writeCell(row, columnIndex++, record.getSourceTableName(), null);
            writeCell(row, columnIndex++, record.getTargetSchemaName(), null);
            writeCell(row, columnIndex++, record.getTargetTableName(), null);
            writeCell(row, columnIndex++, record.getColumnName(), null);
            writeCell(row, columnIndex++, Boolean.toString(record.isSourceColumnExists()), null);
            writeCell(row, columnIndex++, Boolean.toString(record.isTargetColumnExists()), null);
            writeCell(row, columnIndex++, record.getSourceType(), null);
            writeCell(row, columnIndex++, record.getTargetType(), null);
            writeCell(row, columnIndex++, statusText(record.getTypeStatus()), statusStyle(record.getTypeStatus(), matchStyle, mismatchStyle, naStyle));
            writeCell(row, columnIndex++, record.getSourceLength(), null);
            writeCell(row, columnIndex++, record.getTargetLength(), null);
            writeCell(row, columnIndex++, statusText(record.getLengthStatus()), statusStyle(record.getLengthStatus(), matchStyle, mismatchStyle, naStyle));
            writeCell(row, columnIndex++, record.getSourceDefaultValue(), null);
            writeCell(row, columnIndex++, record.getTargetDefaultValue(), null);
            writeCell(row, columnIndex++, statusText(record.getDefaultStatus()), statusStyle(record.getDefaultStatus(), matchStyle, mismatchStyle, naStyle));
            writeCell(row, columnIndex++, record.getSourceNullable(), null);
            writeCell(row, columnIndex++, record.getTargetNullable(), null);
            writeCell(row, columnIndex++, statusText(record.getNullableStatus()), statusStyle(record.getNullableStatus(), matchStyle, mismatchStyle, naStyle));
            writeCell(row, columnIndex++, statusText(record.getOverallStatus()), statusStyle(record.getOverallStatus(), matchStyle, mismatchStyle, naStyle));
            writeCell(row, columnIndex++, record.getDiffTypes(), null);
            writeCell(row, columnIndex, record.getMessage(), null);
        }
    }

    private void writeCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private String statusText(ComparisonStatus status) {
        return status == null ? "" : status.name();
    }

    private CellStyle statusStyle(ComparisonStatus status, CellStyle matchStyle, CellStyle mismatchStyle, CellStyle naStyle) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case MATCH -> matchStyle;
            case MISMATCH -> mismatchStyle;
            case NOT_APPLICABLE -> naStyle;
        };
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createStatusStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
