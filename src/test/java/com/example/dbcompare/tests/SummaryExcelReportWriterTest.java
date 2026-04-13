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
    void writesOverviewAndSummarySheets(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("summary.xlsx");
        SummaryExcelReportWriter writer = new SummaryExcelReportWriter(10);

        try (SummaryExcelReportWriter.SummaryExcelReportSession session = writer.open(output)) {
            session.append(List.of(
                    matchRecord("SRC_A", "SCH_A", "TABLE_OK", "ID"),
                    mismatchRecord("SRC_A", "SCH_A", "TABLE_TYPE", "NAME", "COLUMN_TYPE_MISMATCH", "Type mismatch"),
                    mismatchRecord("SRC_B", "SCH_B", "TABLE_LEN", "AMOUNT", "COLUMN_LENGTH_MISMATCH", "Length mismatch")
            ));
        }

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals("Overview", workbook.getSheetAt(0).getSheetName(), "overview sheet should be first");
            assertEquals("Diff Summary", workbook.getSheetAt(1).getSheetName(), "diff summary sheet should exist");
            assertEquals("Risk Summary", workbook.getSheetAt(2).getSheetName(), "risk summary sheet should exist");
            assertEquals("Schema Distribution", workbook.getSheetAt(3).getSheetName(), "schema distribution sheet should exist");
            assertEquals("Top Issue Tables", workbook.getSheetAt(4).getSheetName(), "top issues sheet should exist");
            assertEquals("Detail", workbook.getSheetAt(5).getSheetName(), "detail sheet should be included in the summary workbook");

            assertEquals("3", workbook.getSheet("Overview").getRow(1).getCell(1).getStringCellValue(), "overview should count unique tables");
            assertEquals("1", workbook.getSheet("Overview").getRow(2).getCell(1).getStringCellValue(), "overview should count fully matched tables");
            assertEquals("2", workbook.getSheet("Overview").getRow(3).getCell(1).getStringCellValue(), "overview should count problematic tables");
            assertEquals("33.33%", workbook.getSheet("Overview").getRow(4).getCell(1).getStringCellValue(), "coverage should be based on fully matched tables");

            assertEquals("1", workbook.getSheet("Diff Summary").getRow(1).getCell(1).getStringCellValue(), "diff summary should count fully matched tables");
            assertEquals("1", workbook.getSheet("Diff Summary").getRow(3).getCell(1).getStringCellValue(), "diff summary should count type mismatch tables");
            assertEquals("1", workbook.getSheet("Diff Summary").getRow(4).getCell(1).getStringCellValue(), "diff summary should count length mismatch tables");

            assertEquals("1", workbook.getSheet("Risk Summary").getRow(2).getCell(1).getStringCellValue(), "risk summary should classify length issues as medium risk");
            assertEquals("1", workbook.getSheet("Risk Summary").getRow(3).getCell(1).getStringCellValue(), "risk summary should classify type issues as high risk");
            assertEquals("SRC_A.SCH_A.TABLE_TYPE", workbook.getSheet("Top Issue Tables").getRow(1).getCell(0).getStringCellValue(), "top issue sheet should list the worst table first when counts tie by name");
            assertEquals("COLUMN_TYPE_MISMATCH", workbook.getSheet("Detail").getRow(2).getCell(4).getStringCellValue(), "detail sheet should keep the raw diff type");
        }
    }

    private ColumnComparisonRecord matchRecord(String sourceDb, String schema, String table, String column) {
        ColumnComparisonRecord record = baseRecord(sourceDb, schema, table, column);
        record.setSourceColumnExists(true);
        record.setTargetColumnExists(true);
        record.setOverallStatus(ComparisonStatus.MATCH);
        record.setMessage("MATCH");
        return record;
    }

    private ColumnComparisonRecord mismatchRecord(String sourceDb, String schema, String table, String column, String diffType, String message) {
        ColumnComparisonRecord record = baseRecord(sourceDb, schema, table, column);
        record.setSourceColumnExists(true);
        record.setTargetColumnExists(true);
        record.setOverallStatus(ComparisonStatus.MISMATCH);
        record.setDiffTypes(diffType);
        record.setMessage(message);
        return record;
    }

    private ColumnComparisonRecord baseRecord(String sourceDb, String schema, String table, String column) {
        ColumnComparisonRecord record = new ColumnComparisonRecord();
        record.setSourceDatabaseName(sourceDb);
        record.setSourceSchemaName(schema);
        record.setSourceTableName(table);
        record.setTargetSchemaName(schema);
        record.setTargetTableName(table);
        record.setColumnName(column);
        return record;
    }
}