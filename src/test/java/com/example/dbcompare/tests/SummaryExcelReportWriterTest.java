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
    void writesCompactSummaryWorkbook(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("summary.xlsx");
        SummaryExcelReportWriter writer = new SummaryExcelReportWriter(100);

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
            assertEquals("Summary", workbook.getSheetAt(0).getSheetName(), "summary sheet should be first");
            assertEquals("Table Status", workbook.getSheetAt(1).getSheetName(), "table status sheet should be second");
            assertEquals("Detail", workbook.getSheetAt(2).getSheetName(), "detail sheet should be third");

            assertEquals("view schema", workbook.getSheet("Summary").getRow(0).getCell(0).getStringCellValue(), "summary should render schema block title");
            assertEquals("TGT_A", workbook.getSheet("Summary").getRow(2).getCell(0).getStringCellValue(), "summary should aggregate target schema");
            assertEquals("5", workbook.getSheet("Summary").getRow(2).getCell(1).getStringCellValue(), "summary should count tables per target schema");

            assertEquals("metric", workbook.getSheet("Summary").getRow(0).getCell(3).getStringCellValue(), "summary should render overview block title");
            assertEquals("totalTables", workbook.getSheet("Summary").getRow(2).getCell(3).getStringCellValue(), "overview should list total table metric");
            assertEquals("5", workbook.getSheet("Summary").getRow(2).getCell(4).getStringCellValue(), "overview should count unique tables");
            assertEquals("nullableMismatchTables", workbook.getSheet("Summary").getRow(8).getCell(3).getStringCellValue(), "overview should include nullable metric");
            assertEquals("0", workbook.getSheet("Summary").getRow(8).getCell(4).getStringCellValue(), "overview should count nullable mismatch tables");
            assertEquals("80.00%", workbook.getSheet("Summary").getRow(9).getCell(4).getStringCellValue(), "overview should calculate full exists ratio");

            assertEquals("fieldExistenceStatus", workbook.getSheet("Summary").getRow(0).getCell(6).getStringCellValue(), "summary should include field existence block");
            assertEquals("4", workbook.getSheet("Summary").getRow(2).getCell(7).getStringCellValue(), "field existence summary should count fully existing tables");
            assertEquals("1", workbook.getSheet("Summary").getRow(3).getCell(7).getStringCellValue(), "field existence summary should count non fully existing tables");

            assertEquals("typeStatus", workbook.getSheet("Summary").getRow(4).getCell(6).getStringCellValue(), "summary should include type block");
            assertEquals("1", workbook.getSheet("Summary").getRow(7).getCell(7).getStringCellValue(), "type summary should count type mismatch tables");
            assertEquals("lengthStatus", workbook.getSheet("Summary").getRow(8).getCell(6).getStringCellValue(), "summary should include length block");
            assertEquals("1", workbook.getSheet("Summary").getRow(11).getCell(7).getStringCellValue(), "length summary should count length mismatch tables");
            assertEquals("defaultStatus", workbook.getSheet("Summary").getRow(12).getCell(6).getStringCellValue(), "summary should include default block");
            assertEquals("1", workbook.getSheet("Summary").getRow(15).getCell(7).getStringCellValue(), "default summary should count default mismatch tables");

            assertEquals("nullableStatus", workbook.getSheet("Summary").getRow(0).getCell(10).getStringCellValue(), "summary should include nullable block");
            assertEquals("5", workbook.getSheet("Summary").getRow(2).getCell(11).getStringCellValue(), "nullable summary should count matching tables");
            assertEquals("riskLevel", workbook.getSheet("Summary").getRow(4).getCell(10).getStringCellValue(), "summary should include risk block");
            assertEquals("1", workbook.getSheet("Summary").getRow(6).getCell(11).getStringCellValue(), "risk summary should count low risk tables");
            assertEquals("2", workbook.getSheet("Summary").getRow(7).getCell(11).getStringCellValue(), "risk summary should count medium risk tables");
            assertEquals("2", workbook.getSheet("Summary").getRow(8).getCell(11).getStringCellValue(), "risk summary should count high risk tables");
            assertEquals("diffCategory", workbook.getSheet("Summary").getRow(8).getCell(10).getStringCellValue(), "summary should include diff category block");
            assertEquals("1", workbook.getSheet("Summary").getRow(10).getCell(11).getStringCellValue(), "diff summary should count full match tables");
            assertEquals("1", workbook.getSheet("Summary").getRow(11).getCell(11).getStringCellValue(), "diff summary should count missing-column tables");
            assertEquals("1", workbook.getSheet("Summary").getRow(12).getCell(11).getStringCellValue(), "diff summary should count type mismatch tables");
            assertEquals("1", workbook.getSheet("Summary").getRow(13).getCell(11).getStringCellValue(), "diff summary should count length mismatch tables");
            assertEquals("1", workbook.getSheet("Summary").getRow(14).getCell(11).getStringCellValue(), "diff summary should count other tables");

            assertEquals("risk rules", workbook.getSheet("Summary").getRow(0).getCell(14).getStringCellValue(), "summary should include risk rules");
            assertEquals("No mismatch", workbook.getSheet("Summary").getRow(2).getCell(14).getStringCellValue(), "risk rules should describe low risk");
            assertEquals("LOW", workbook.getSheet("Summary").getRow(2).getCell(15).getStringCellValue(), "risk rules should map low risk");

            assertEquals("TABLE_MISSING", workbook.getSheet("Table Status").getRow(3).getCell(2).getStringCellValue(), "table status sheet should list the missing-field table");
            assertEquals("NOT_FULL_EXISTS", workbook.getSheet("Table Status").getRow(3).getCell(5).getStringCellValue(), "table status sheet should classify missing columns as not fully existing");
            assertEquals("HIGH", workbook.getSheet("Table Status").getRow(3).getCell(10).getStringCellValue(), "table status sheet should classify missing columns as high risk");

            assertEquals("Field Existence Details", workbook.getSheet("Detail").getRow(0).getCell(0).getStringCellValue(), "detail sheet should start with field existence section");
            assertEquals("TABLE_MISSING", workbook.getSheet("Detail").getRow(2).getCell(2).getStringCellValue(), "field existence detail should keep missing table row");
            assertEquals("Type Details", workbook.getSheet("Detail").getRow(4).getCell(0).getStringCellValue(), "detail sheet should contain type section");
            assertEquals("TABLE_TYPE", workbook.getSheet("Detail").getRow(6).getCell(2).getStringCellValue(), "type section should keep type mismatch row");
            assertEquals("Length Details", workbook.getSheet("Detail").getRow(8).getCell(0).getStringCellValue(), "detail sheet should contain length section");
            assertEquals("TABLE_LENGTH", workbook.getSheet("Detail").getRow(10).getCell(2).getStringCellValue(), "length section should keep length mismatch row");
            assertEquals("Default Details", workbook.getSheet("Detail").getRow(12).getCell(0).getStringCellValue(), "detail sheet should contain default section");
            assertEquals("TABLE_DEFAULT", workbook.getSheet("Detail").getRow(14).getCell(2).getStringCellValue(), "default section should keep default mismatch row");
            assertEquals("Nullable Details", workbook.getSheet("Detail").getRow(16).getCell(0).getStringCellValue(), "detail sheet should contain nullable section");
            assertEquals("NO_DATA", workbook.getSheet("Detail").getRow(18).getCell(0).getStringCellValue(), "nullable section should display no-data marker when empty");
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