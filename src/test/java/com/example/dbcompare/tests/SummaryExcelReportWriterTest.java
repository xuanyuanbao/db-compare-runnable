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

class SummaryExcelReportWriterTest {
    @Test
    void keepsOriginalSummaryWorkbookLayout(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("summary.xlsx");
        SummaryExcelReportWriter writer = new SummaryExcelReportWriter(100);

        try (SummaryExcelReportWriter.SummaryExcelReportSession session = writer.open(output)) {
            session.append(List.of(
                    record("SRC_A", "SCH_A", "TABLE_DEFAULT", "VIEW_SCH", "VIEW_DEFAULT", "COL_1", true, true, ComparisonStatus.MISMATCH, "COLUMN_DEFAULT_MISMATCH", "Default mismatch"),
                    record("SRC_A", "SCH_A", "TABLE_LENGTH", "VIEW_SCH", "VIEW_LENGTH", "COL_1", true, true, ComparisonStatus.MISMATCH, "COLUMN_LENGTH_MISMATCH", "Length mismatch"),
                    record("SRC_A", "SCH_A", "TABLE_MISSING", "VIEW_SCH", "VIEW_MISSING", "COL_1", true, false, ComparisonStatus.MISMATCH, "COLUMN_MISSING_IN_TARGET", "Column exists only in source"),
                    record("SRC_A", "SCH_A", "TABLE_OK", "VIEW_SCH", "VIEW_OK", "COL_1", true, true, ComparisonStatus.MATCH, "", "MATCH"),
                    record("SRC_A", "SCH_A", "TABLE_TYPE", "VIEW_SCH", "VIEW_TYPE", "COL_1", true, true, ComparisonStatus.MISMATCH, "COLUMN_TYPE_MISMATCH", "Type mismatch")
            ));
        }

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals("汇总", workbook.getSheetAt(0).getSheetName(), "summary sheet should be first");
            assertEquals("表级状态", workbook.getSheetAt(1).getSheetName(), "table status sheet should be second");
            assertEquals("字段存在明细", workbook.getSheetAt(2).getSheetName(), "field existence detail sheet should be present");

            assertEquals("视图Schema", workbook.getSheet("汇总").getRow(0).getCell(0).getStringCellValue(), "summary should keep the existing schema block title");
            assertEquals("VIEW_SCH", workbook.getSheet("汇总").getRow(2).getCell(0).getStringCellValue(), "summary should still aggregate by target schema");
            assertEquals("5", workbook.getSheet("汇总").getRow(2).getCell(1).getStringCellValue(), "summary should count unique target objects");

            assertEquals("目标Schema", workbook.getSheet("表级状态").getRow(0).getCell(3).getStringCellValue(), "table status should keep the original target schema header");
            assertEquals("目标表", workbook.getSheet("表级状态").getRow(0).getCell(4).getStringCellValue(), "table status should keep the original target table header");
            assertEquals("VIEW_MISSING", workbook.getSheet("表级状态").getRow(3).getCell(4).getStringCellValue(), "table status should continue exposing the main target object");
            assertEquals("不完全存在", workbook.getSheet("表级状态").getRow(3).getCell(5).getStringCellValue(), "missing columns should still be classified as not fully existing");

            assertEquals("目标Schema", workbook.getSheet("字段存在明细").getRow(0).getCell(3).getStringCellValue(), "detail sheet should keep the original target schema header");
            assertEquals("VIEW_MISSING", workbook.getSheet("字段存在明细").getRow(1).getCell(4).getStringCellValue(), "detail sheet should keep the original target value");
            assertEquals("目标端缺字段", workbook.getSheet("字段存在明细").getRow(1).getCell(6).getStringCellValue(), "detail sheet should localize diff types");
        }
    }

    private ColumnComparisonRecord record(String sourceDb,
                                          String sourceSchema,
                                          String sourceTable,
                                          String targetSchema,
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
        record.setTargetSchemaName(targetSchema);
        record.setTargetTableName(targetTable);
        record.setColumnName(columnName);
        record.setSourceColumnExists(sourceExists);
        record.setTargetColumnExists(targetExists);
        record.setOverallStatus(overallStatus);
        record.setDiffTypes(diffType);
        record.setMessage(message);
        return record;
    }
}
