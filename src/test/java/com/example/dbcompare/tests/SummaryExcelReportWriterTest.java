package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.domain.model.CompareOptions;
import com.example.dbcompare.infrastructure.output.SummaryExcelReportWriter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SummaryExcelReportWriterTest {
    @Test
    void omitsDefaultAndNullableOutputsWhenThoseComparisonsAreDisabled(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("summary.xlsx");
        CompareOptions options = new CompareOptions();
        options.setCompareDefaultValue(false);
        options.setCompareNullable(false);

        try (SummaryExcelReportWriter.SummaryExcelReportSession session = new SummaryExcelReportWriter().open(output, options)) {
            session.append(List.of(
                    record("SRC_A", "SCH_A", "TABLE_LENGTH", "VIEW_SCH", "VIEW_LENGTH", "COL_1", true, true, ComparisonStatus.MISMATCH, "COLUMN_LENGTH_MISMATCH", "Length mismatch"),
                    record("SRC_A", "SCH_A", "TABLE_MISSING", "VIEW_SCH", "VIEW_MISSING", "COL_1", true, false, ComparisonStatus.MISMATCH, "COLUMN_MISSING_IN_TARGET", "Column exists only in source"),
                    record("SRC_A", "SCH_A", "TABLE_OK", "VIEW_SCH", "VIEW_OK", "COL_1", true, true, ComparisonStatus.MATCH, "", "MATCH"),
                    record("SRC_A", "SCH_A", "TABLE_TYPE", "VIEW_SCH", "VIEW_TYPE", "COL_1", true, true, ComparisonStatus.MISMATCH, "COLUMN_TYPE_MISMATCH", "Type mismatch")
            ));
        }

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals("汇总", workbook.getSheetAt(0).getSheetName());
            assertEquals("表级状态", workbook.getSheetAt(1).getSheetName());
            assertEquals("字段存在明细", workbook.getSheetAt(2).getSheetName());
            assertEquals("类型明细", workbook.getSheetAt(3).getSheetName());
            assertEquals("长度明细", workbook.getSheetAt(4).getSheetName());
            assertEquals(5, workbook.getNumberOfSheets(), "summary workbook should not create default/null detail sheets when disabled");
            assertNull(workbook.getSheet("默认值明细"));
            assertNull(workbook.getSheet("可空明细"));
            assertEquals("风险等级", workbook.getSheet("表级状态").getRow(0).getCell(8).getStringCellValue(), "table status should shift columns after removing default/null");
            assertEquals("差异分类", workbook.getSheet("表级状态").getRow(0).getCell(9).getStringCellValue(), "table status should keep the final classification column");
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
