package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.enums.DiffGroup;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.domain.model.CompareOptions;
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
    void omitsDefaultAndNullableColumnsWhenThoseComparisonsAreDisabled(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("detail.xlsx");
        CompareOptions options = new CompareOptions();
        options.setCompareDefaultValue(false);
        options.setCompareNullable(false);

        try (ExcelReportWriter.ExcelReportSession session = new ExcelReportWriter(10).open(output, options)) {
            session.append(List.of(record("COL_1")));
        }

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals("字段名", workbook.getSheetAt(0).getRow(0).getCell(5).getStringCellValue());
            assertEquals("整体状态", workbook.getSheetAt(0).getRow(0).getCell(14).getStringCellValue());
            assertEquals("差异分组", workbook.getSheetAt(0).getRow(0).getCell(15).getStringCellValue());
            assertEquals("是否影响结果", workbook.getSheetAt(0).getRow(0).getCell(16).getStringCellValue());
            assertEquals("说明", workbook.getSheetAt(0).getRow(0).getCell(18).getStringCellValue());
            assertEquals(19, workbook.getSheetAt(0).getRow(0).getLastCellNum(), "detail excel should remove default/null related columns");
            assertEquals("一致", workbook.getSheetAt(0).getRow(1).getCell(14).getStringCellValue());
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
