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
            assertEquals("类型判等规则", workbook.getSheetAt(1).getSheetName());
        }
    }

    @Test
    void writesRawDatabaseMetadataValuesAndAddsDynamicTypeRuleSheet(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("detail-raw.xlsx");
        CompareOptions options = new CompareOptions();
        options.putTypeMapping("DATE", List.of("TIMESTAMP"));

        ColumnComparisonRecord record = record("COL_1");
        record.setSourceType("CHARACTER");
        record.setTargetType("VARCHAR");
        record.setSourceLength("16 OCTETS");
        record.setTargetLength("60");

        try (ExcelReportWriter.ExcelReportSession session = new ExcelReportWriter(10).open(output, options)) {
            session.append(List.of(record));
        }

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals("源原始类型", workbook.getSheetAt(0).getRow(0).getCell(8).getStringCellValue());
            assertEquals("目标原始类型", workbook.getSheetAt(0).getRow(0).getCell(9).getStringCellValue());
            assertEquals("源原始长度", workbook.getSheetAt(0).getRow(0).getCell(11).getStringCellValue());
            assertEquals("目标原始长度", workbook.getSheetAt(0).getRow(0).getCell(12).getStringCellValue());
            assertEquals("CHARACTER", workbook.getSheetAt(0).getRow(1).getCell(8).getStringCellValue());
            assertEquals("VARCHAR", workbook.getSheetAt(0).getRow(1).getCell(9).getStringCellValue());
            assertEquals("16 OCTETS", workbook.getSheetAt(0).getRow(1).getCell(11).getStringCellValue());
            assertEquals("60", workbook.getSheetAt(0).getRow(1).getCell(12).getStringCellValue());
            assertEquals("原始类型集合", workbook.getSheet("类型判等规则").getRow(0).getCell(0).getStringCellValue());
            assertEquals("比较归一类型", workbook.getSheet("类型判等规则").getRow(0).getCell(1).getStringCellValue());
            assertEquals("判等说明", workbook.getSheet("类型判等规则").getRow(0).getCell(2).getStringCellValue());
            assertEquals("DATE, TIMESTAMP", workbook.getSheet("类型判等规则").getRow(1).getCell(0).getStringCellValue());
            assertEquals("DATE", workbook.getSheet("类型判等规则").getRow(1).getCell(1).getStringCellValue());
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
