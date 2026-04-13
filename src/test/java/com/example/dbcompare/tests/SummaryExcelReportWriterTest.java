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
    void writesTableLevelStatusAndAttributeSummaries(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("summary.xlsx");
        SummaryExcelReportWriter writer = new SummaryExcelReportWriter(10);

        try (SummaryExcelReportWriter.SummaryExcelReportSession session = writer.open(output)) {
            session.append(List.of(
                    record("SRC_A", "SCH_A", "TABLE_DEFAULT", "TGT_A", "TABLE_DEFAULT", "COL_1", true, true, ComparisonStatus.MISMATCH, "COLUMN_DEFAULT_MISMATCH", "Default mismatch"),
                    record("SRC_A", "SCH_A", "TABLE_LENGTH", "TGT_A", "TABLE_LENGTH", "COL_1", true, true, ComparisonStatus.MISMATCH, "COLUMN_LENGTH_MISMATCH", "Length mismatch"),
                    record("SRC_A", "SCH_A", "TABLE_MISSING", "TGT_A", "TABLE_MISSING", "COL_1", true, false, ComparisonStatus.MISMATCH, "COLUMN_MISSING_IN_TARGET", "Column exists only in source"),
                    record("SRC_A", "SCH_A", "TABLE_OK", "TGT_A", "TABLE_OK", "COL_1", true, true, ComparisonStatus.MATCH, "", "MATCH"),
                    record("SRC_A", "SCH_A", "TABLE_TYPE", "TGT_A", "TABLE_TYPE", "COL_1", true, true, ComparisonStatus.MISMATCH, "COLUMN_TYPE_MISMATCH", "Type mismatch")
            ));
        }

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals("Overview", workbook.getSheetAt(0).getSheetName(), "overview sheet should be first");
            assertEquals("Field Existence Summary", workbook.getSheetAt(1).getSheetName(), "field existence summary sheet should exist");
            assertEquals("Type Summary", workbook.getSheetAt(2).getSheetName(), "type summary sheet should exist");
            assertEquals("Length Summary", workbook.getSheetAt(3).getSheetName(), "length summary sheet should exist");
            assertEquals("Default Summary", workbook.getSheetAt(4).getSheetName(), "default summary sheet should exist");
            assertEquals("Table Status", workbook.getSheetAt(5).getSheetName(), "table status detail sheet should exist");
            assertEquals("Diff Summary", workbook.getSheetAt(6).getSheetName(), "diff summary sheet should exist");
            assertEquals("Risk Summary", workbook.getSheetAt(7).getSheetName(), "risk summary sheet should exist");
            assertEquals("Schema Distribution", workbook.getSheetAt(8).getSheetName(), "schema distribution sheet should exist");
            assertEquals("Top Issue Tables", workbook.getSheetAt(9).getSheetName(), "top issues sheet should exist");
            assertEquals("Detail", workbook.getSheetAt(10).getSheetName(), "detail sheet should be included in the summary workbook");

            assertEquals("5", workbook.getSheet("Overview").getRow(1).getCell(1).getStringCellValue(), "overview should count unique tables");
            assertEquals("4", workbook.getSheet("Overview").getRow(2).getCell(1).getStringCellValue(), "overview should count fully existing tables");
            assertEquals("1", workbook.getSheet("Overview").getRow(3).getCell(1).getStringCellValue(), "overview should count non fully existing tables");
            assertEquals("1", workbook.getSheet("Overview").getRow(4).getCell(1).getStringCellValue(), "overview should count type mismatch tables");
            assertEquals("1", workbook.getSheet("Overview").getRow(5).getCell(1).getStringCellValue(), "overview should count length mismatch tables");
            assertEquals("1", workbook.getSheet("Overview").getRow(6).getCell(1).getStringCellValue(), "overview should count default mismatch tables");
            assertEquals("80.00%", workbook.getSheet("Overview").getRow(7).getCell(1).getStringCellValue(), "overview should calculate full exists ratio");

            assertEquals("4", workbook.getSheet("Field Existence Summary").getRow(1).getCell(1).getStringCellValue(), "field existence summary should count fully existing tables");
            assertEquals("1", workbook.getSheet("Field Existence Summary").getRow(2).getCell(1).getStringCellValue(), "field existence summary should count non fully existing tables");

            assertEquals("1", workbook.getSheet("Type Summary").getRow(2).getCell(1).getStringCellValue(), "type summary should count type mismatch tables");
            assertEquals("1", workbook.getSheet("Length Summary").getRow(2).getCell(1).getStringCellValue(), "length summary should count length mismatch tables");
            assertEquals("1", workbook.getSheet("Default Summary").getRow(2).getCell(1).getStringCellValue(), "default summary should count default mismatch tables");

            assertEquals("TABLE_MISSING", workbook.getSheet("Table Status").getRow(3).getCell(2).getStringCellValue(), "table status sheet should list the missing-field table");
            assertEquals("NOT_FULL_EXISTS", workbook.getSheet("Table Status").getRow(3).getCell(5).getStringCellValue(), "table status sheet should classify missing columns as not fully existing");

            assertEquals("1", workbook.getSheet("Diff Summary").getRow(1).getCell(1).getStringCellValue(), "diff summary should count fully matched tables");
            assertEquals("1", workbook.getSheet("Diff Summary").getRow(2).getCell(1).getStringCellValue(), "diff summary should count missing-column tables");
            assertEquals("1", workbook.getSheet("Diff Summary").getRow(3).getCell(1).getStringCellValue(), "diff summary should count type mismatch tables");
            assertEquals("1", workbook.getSheet("Diff Summary").getRow(4).getCell(1).getStringCellValue(), "diff summary should count length mismatch tables");
            assertEquals("1", workbook.getSheet("Diff Summary").getRow(5).getCell(1).getStringCellValue(), "diff summary should count other tables such as default mismatch");

            assertEquals("1", workbook.getSheet("Risk Summary").getRow(1).getCell(1).getStringCellValue(), "risk summary should count low risk tables");
            assertEquals("2", workbook.getSheet("Risk Summary").getRow(2).getCell(1).getStringCellValue(), "risk summary should count medium risk tables");
            assertEquals("2", workbook.getSheet("Risk Summary").getRow(3).getCell(1).getStringCellValue(), "risk summary should count high risk tables");

            assertEquals("COLUMN_DEFAULT_MISMATCH", workbook.getSheet("Detail").getRow(1).getCell(4).getStringCellValue(), "detail sheet should keep raw diff types");
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