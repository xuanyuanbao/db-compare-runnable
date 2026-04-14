package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.infrastructure.output.SummaryExcelReportWriter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SummaryExcelReportWriterTest {
    @Test
    void writesCompactSummaryWorkbook(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("summary.xlsx");
        SummaryExcelReportWriter writer = new SummaryExcelReportWriter(100);

        try (SummaryExcelReportWriter.SummaryExcelReportSession session = writer.open(output)) {
            session.append(List.of(
                    record("SRC_A", "SCH_A", "TABLE_DEFAULT", "VIEW_SCH", "VIEW_DEFAULT", "BASE_SCH", "BASE_DEFAULT", "COL_1", true, true, ComparisonStatus.MISMATCH, "COLUMN_DEFAULT_MISMATCH", "Default mismatch"),
                    record("SRC_A", "SCH_A", "TABLE_LENGTH", "VIEW_SCH", "VIEW_LENGTH", "BASE_SCH", "BASE_LENGTH", "COL_1", true, true, ComparisonStatus.MISMATCH, "COLUMN_LENGTH_MISMATCH", "Length mismatch"),
                    record("SRC_A", "SCH_A", "TABLE_MISSING", "VIEW_SCH", "VIEW_MISSING", "BASE_SCH", "BASE_MISSING", "COL_1", true, false, ComparisonStatus.MISMATCH, "COLUMN_MISSING_IN_TARGET", "Column exists only in source"),
                    record("SRC_A", "SCH_A", "TABLE_OK", "VIEW_SCH", "VIEW_OK", "BASE_SCH", "BASE_OK", "COL_1", true, true, ComparisonStatus.MATCH, "", "MATCH"),
                    record("SRC_A", "SCH_A", "TABLE_TYPE", "VIEW_SCH", "VIEW_TYPE", "BASE_SCH", "BASE_TYPE", "COL_1", true, true, ComparisonStatus.MISMATCH, "COLUMN_TYPE_MISMATCH", "Type mismatch")
            ));
        }

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals("汇总", workbook.getSheetAt(0).getSheetName(), "summary sheet should be first");
            assertEquals("表级状态", workbook.getSheetAt(1).getSheetName(), "table status sheet should be second");
            assertEquals("字段存在明细", workbook.getSheetAt(2).getSheetName(), "field existence detail sheet should be third");
            assertEquals("类型明细", workbook.getSheetAt(3).getSheetName(), "type detail sheet should be present");
            assertEquals("长度明细", workbook.getSheetAt(4).getSheetName(), "length detail sheet should be present");
            assertEquals("默认值明细", workbook.getSheetAt(5).getSheetName(), "default detail sheet should be present");
            assertEquals("可空明细", workbook.getSheetAt(6).getSheetName(), "nullable detail sheet should be present");

            assertEquals("视图Schema", workbook.getSheet("汇总").getRow(0).getCell(0).getStringCellValue(), "summary should render schema block title");
            assertEquals("VIEW_SCH", workbook.getSheet("汇总").getRow(2).getCell(0).getStringCellValue(), "summary should aggregate target view schema");
            assertEquals("5", workbook.getSheet("汇总").getRow(2).getCell(1).getStringCellValue(), "summary should count tables per target schema");

            assertEquals("指标概况", workbook.getSheet("汇总").getRow(0).getCell(3).getStringCellValue(), "summary should render overview block title");
            assertEquals("总表数", workbook.getSheet("汇总").getRow(2).getCell(3).getStringCellValue(), "overview should list total table metric");
            assertEquals("5", workbook.getSheet("汇总").getRow(2).getCell(4).getStringCellValue(), "overview should count unique tables");
            assertEquals("可空性不一致表数", workbook.getSheet("汇总").getRow(8).getCell(3).getStringCellValue(), "overview should include nullable metric");
            assertEquals("0", workbook.getSheet("汇总").getRow(8).getCell(4).getStringCellValue(), "overview should count nullable mismatch tables");
            assertEquals("80.00%", workbook.getSheet("汇总").getRow(9).getCell(4).getStringCellValue(), "overview should calculate full exists ratio");

            assertEquals("字段存在状态", workbook.getSheet("汇总").getRow(0).getCell(6).getStringCellValue(), "summary should include field existence block");
            assertEquals("4", workbook.getSheet("汇总").getRow(2).getCell(7).getStringCellValue(), "field existence summary should count fully existing tables");
            assertEquals("1", workbook.getSheet("汇总").getRow(3).getCell(7).getStringCellValue(), "field existence summary should count non fully existing tables");
            assertEquals("完全存在", workbook.getSheet("汇总").getRow(2).getCell(6).getStringCellValue(), "field existence summary should localize status text");

            assertEquals("类型状态", workbook.getSheet("汇总").getRow(4).getCell(6).getStringCellValue(), "summary should include type block");
            assertEquals("1", workbook.getSheet("汇总").getRow(7).getCell(7).getStringCellValue(), "type summary should count type mismatch tables");
            assertEquals("长度状态", workbook.getSheet("汇总").getRow(8).getCell(6).getStringCellValue(), "summary should include length block");
            assertEquals("1", workbook.getSheet("汇总").getRow(11).getCell(7).getStringCellValue(), "length summary should count length mismatch tables");
            assertEquals("默认值状态", workbook.getSheet("汇总").getRow(12).getCell(6).getStringCellValue(), "summary should include default block");
            assertEquals("1", workbook.getSheet("汇总").getRow(15).getCell(7).getStringCellValue(), "default summary should count default mismatch tables");

            assertEquals("可空状态", workbook.getSheet("汇总").getRow(16).getCell(6).getStringCellValue(), "summary should include nullable block in the main status column");
            assertEquals("5", workbook.getSheet("汇总").getRow(18).getCell(7).getStringCellValue(), "nullable summary should count matching tables");
            assertEquals("风险等级", workbook.getSheet("汇总").getRow(0).getCell(10).getStringCellValue(), "summary should include risk block");
            assertEquals("1", workbook.getSheet("汇总").getRow(2).getCell(11).getStringCellValue(), "risk summary should count low risk tables");
            assertEquals("2", workbook.getSheet("汇总").getRow(3).getCell(11).getStringCellValue(), "risk summary should count medium risk tables");
            assertEquals("2", workbook.getSheet("汇总").getRow(4).getCell(11).getStringCellValue(), "risk summary should count high risk tables");
            assertEquals("差异分类", workbook.getSheet("汇总").getRow(5).getCell(10).getStringCellValue(), "summary should include diff category block");
            assertEquals("1", workbook.getSheet("汇总").getRow(7).getCell(11).getStringCellValue(), "diff summary should count full match tables");
            assertEquals("1", workbook.getSheet("汇总").getRow(8).getCell(11).getStringCellValue(), "diff summary should count missing-column tables");
            assertEquals("1", workbook.getSheet("汇总").getRow(9).getCell(11).getStringCellValue(), "diff summary should count type mismatch tables");
            assertEquals("1", workbook.getSheet("汇总").getRow(10).getCell(11).getStringCellValue(), "diff summary should count length mismatch tables");
            assertEquals("1", workbook.getSheet("汇总").getRow(11).getCell(11).getStringCellValue(), "diff summary should count other tables");

            assertEquals("风险规则", workbook.getSheet("汇总").getRow(0).getCell(14).getStringCellValue(), "summary should include risk rules");
            assertEquals("没有任何差异", workbook.getSheet("汇总").getRow(2).getCell(14).getStringCellValue(), "risk rules should describe low risk");
            assertEquals("低", workbook.getSheet("汇总").getRow(2).getCell(15).getStringCellValue(), "risk rules should map low risk");

            short fullExistsColor = workbook.getSheet("汇总").getRow(2).getCell(6).getCellStyle().getFillForegroundColor();
            short riskHighColor = workbook.getSheet("汇总").getRow(4).getCell(10).getCellStyle().getFillForegroundColor();
            short missingColumnColor = workbook.getSheet("表级状态").getRow(3).getCell(13).getCellStyle().getFillForegroundColor();
            assertNotEquals(0, fullExistsColor, "summary status cells should have a fill color");
            assertNotEquals(fullExistsColor, riskHighColor, "high risk should use a different highlight than positive status");
            assertEquals(riskHighColor, missingColumnColor, "table status should reuse strong highlight for severe mismatch categories");

            assertEquals("TABLE_MISSING", workbook.getSheet("表级状态").getRow(3).getCell(2).getStringCellValue(), "table status sheet should list the missing-field table");
            assertEquals("VIEW_MISSING", workbook.getSheet("表级状态").getRow(3).getCell(4).getStringCellValue(), "table status sheet should expose the target view");
            assertEquals("BASE_MISSING", workbook.getSheet("表级状态").getRow(3).getCell(6).getStringCellValue(), "table status sheet should expose the lineage target table");
            assertEquals("不完全存在", workbook.getSheet("表级状态").getRow(3).getCell(7).getStringCellValue(), "table status sheet should classify missing columns as not fully existing");
            assertEquals("高", workbook.getSheet("表级状态").getRow(3).getCell(12).getStringCellValue(), "table status sheet should classify missing columns as high risk");

            assertEquals("TABLE_MISSING", workbook.getSheet("字段存在明细").getRow(1).getCell(2).getStringCellValue(), "field existence detail should keep missing table row");
            assertEquals("VIEW_MISSING", workbook.getSheet("字段存在明细").getRow(1).getCell(4).getStringCellValue(), "field existence detail should expose target view columns");
            assertEquals("BASE_MISSING", workbook.getSheet("字段存在明细").getRow(1).getCell(6).getStringCellValue(), "field existence detail should expose lineage target tables");
            assertEquals("目标端缺字段", workbook.getSheet("字段存在明细").getRow(1).getCell(8).getStringCellValue(), "field existence detail should localize diff type");
            assertEquals("TABLE_TYPE", workbook.getSheet("类型明细").getRow(1).getCell(2).getStringCellValue(), "type detail should keep type mismatch row");
            assertEquals("TABLE_LENGTH", workbook.getSheet("长度明细").getRow(1).getCell(2).getStringCellValue(), "length detail should keep length mismatch row");
            assertEquals("TABLE_DEFAULT", workbook.getSheet("默认值明细").getRow(1).getCell(2).getStringCellValue(), "default detail should keep default mismatch row");
            assertEquals("无数据", workbook.getSheet("可空明细").getRow(1).getCell(0).getStringCellValue(), "nullable detail should display no-data marker when empty");
        }
    }

    private ColumnComparisonRecord record(String sourceDb,
                                          String sourceSchema,
                                          String sourceTable,
                                          String targetViewSchema,
                                          String targetView,
                                          String targetTableSchema,
                                          String targetTable,
                                          String columnName,
                                          boolean sourceExists,
                                          boolean targetExists,
                                          ComparisonStatus overallStatus,
                                          String diffType,
                                          String message) {
        ColumnComparisonRecord record = new ColumnComparisonRecord();
        record.setSourceDatabaseName(sourceDb);
        record.setSourceSchemaName(sourceSchema);
        record.setSourceTableName(sourceTable);
        record.setTargetViewSchemaName(targetViewSchema);
        record.setTargetViewName(targetView);
        record.setTargetLineageTableSchemaName(targetTableSchema);
        record.setTargetLineageTableName(targetTable);
        record.setColumnName(columnName);
        record.setSourceColumnExists(sourceExists);
        record.setTargetColumnExists(targetExists);
        record.setOverallStatus(overallStatus);
        record.setDiffTypes(diffType);
        record.setMessage(message);
        return record;
    }
}
