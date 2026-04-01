package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.infrastructure.output.ExcelReportWriter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelReportWriterTest {
    @Test
    void splitsDataAcrossSheetsWhenSheetRowLimitIsReached(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("detail.xlsx");
        ExcelReportWriter writer = new ExcelReportWriter(3);

        try (ExcelReportWriter.ExcelReportSession session = writer.open(output)) {
            session.append(List.of(
                    record("COL_1"),
                    record("COL_2"),
                    record("COL_3"),
                    record("COL_4"),
                    record("COL_5")
            ));
        }

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals(3, workbook.getNumberOfSheets(), "writer should create follow-up sheets when the first sheet is full");
            assertEquals("Detail", workbook.getSheetAt(0).getSheetName(), "first sheet should keep the base name");
            assertEquals("Detail_2", workbook.getSheetAt(1).getSheetName(), "second sheet should continue with an index suffix");
            assertEquals("Detail_3", workbook.getSheetAt(2).getSheetName(), "third sheet should continue with an index suffix");
            assertEquals("COL_1", workbook.getSheetAt(0).getRow(1).getCell(5).getStringCellValue(), "first sheet should contain the first record");
            assertEquals("COL_3", workbook.getSheetAt(1).getRow(1).getCell(5).getStringCellValue(), "second sheet should continue with later records");
            assertEquals("COL_5", workbook.getSheetAt(2).getRow(1).getCell(5).getStringCellValue(), "final sheet should contain the tail record");
        }
    }

    private ColumnComparisonRecord record(String columnName) {
        ColumnComparisonRecord record = new ColumnComparisonRecord();
        record.setSourceDatabaseName("SRC");
        record.setSourceSchemaName("SRC_SCHEMA");
        record.setSourceTableName("SRC_TABLE");
        record.setTargetSchemaName("TGT_SCHEMA");
        record.setTargetTableName("TGT_TABLE");
        record.setColumnName(columnName);
        record.setSourceColumnExists(true);
        record.setTargetColumnExists(true);
        record.setOverallStatus(ComparisonStatus.MATCH);
        return record;
    }
}