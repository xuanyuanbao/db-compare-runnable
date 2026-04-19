package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.enums.DiffGroup;
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
            assertEquals("明细", workbook.getSheetAt(0).getSheetName(), "first sheet should keep the base name");
            assertEquals("明细_2", workbook.getSheetAt(1).getSheetName(), "second sheet should continue with an index suffix");
            assertEquals("明细_3", workbook.getSheetAt(2).getSheetName(), "third sheet should continue with an index suffix");
            assertEquals("字段名", workbook.getSheetAt(0).getRow(0).getCell(5).getStringCellValue(), "detail workbook headers should remain unchanged");
            assertEquals("COL_1", workbook.getSheetAt(0).getRow(1).getCell(5).getStringCellValue(), "first sheet should contain the first record");
            assertEquals("COL_3", workbook.getSheetAt(1).getRow(1).getCell(5).getStringCellValue(), "second sheet should continue with later records");
            assertEquals("COL_5", workbook.getSheetAt(2).getRow(1).getCell(5).getStringCellValue(), "final sheet should contain the tail record");
            assertEquals("一致", workbook.getSheetAt(0).getRow(1).getCell(20).getStringCellValue(), "status values should be localized");
            assertEquals("主差异", workbook.getSheetAt(0).getRow(1).getCell(21).getStringCellValue(), "diff groups should be written to the detail workbook");
            assertEquals("是", workbook.getSheetAt(0).getRow(1).getCell(22).getStringCellValue(), "result impact should be written to the detail workbook");
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
        record.setDiffGroup(DiffGroup.MAIN);
        return record;
    }
}
