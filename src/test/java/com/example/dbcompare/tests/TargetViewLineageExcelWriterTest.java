package com.example.dbcompare.tests;

import com.example.dbcompare.domain.model.TargetViewLineageReportRow;
import com.example.dbcompare.infrastructure.output.TargetViewLineageExcelWriter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TargetViewLineageExcelWriterTest {
    @Test
    void writesStandaloneLineageWorkbookWithoutChangingMainDetailLayout(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("target-view-lineage.xlsx");
        TargetViewLineageExcelWriter writer = new TargetViewLineageExcelWriter();

        try (TargetViewLineageExcelWriter.TargetViewLineageExcelSession session = writer.open(output)) {
            session.append(List.of(
                    new TargetViewLineageReportRow("DB2_A", "LEGACY_A", "CUSTOMER", "ODS", "CUSTOMER_BASE", "CUSTOMER_VIEW", "VIEW_APP"),
                    new TargetViewLineageReportRow("DB2_A", "LEGACY_A", "CUSTOMER", "ODS", "CUSTOMER_BASE", "CUSTOMER_VIEW", "VIEW_APP"),
                    new TargetViewLineageReportRow("DB2_A", "LEGACY_A", "CUSTOMER", "DWD", "CUSTOMER_PROFILE", "CUSTOMER_VIEW", "VIEW_APP")
            ));
        }

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals(1, workbook.getNumberOfSheets(), "lineage workbook should be independent and compact");
            assertEquals("目标View血缘", workbook.getSheetAt(0).getSheetName(), "lineage workbook should use the dedicated sheet name");
            assertEquals("源数据库", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue(), "lineage workbook should expose the requested source columns");
            assertEquals("目标基表Schema", workbook.getSheetAt(0).getRow(0).getCell(3).getStringCellValue(), "lineage workbook should expose lineage table schema");
            assertEquals("目标View", workbook.getSheetAt(0).getRow(0).getCell(5).getStringCellValue(), "lineage workbook should expose target view");
            assertEquals(2, workbook.getSheetAt(0).getLastRowNum(), "duplicate lineage rows should be deduplicated");
            assertEquals("ODS", workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue(), "first lineage row should keep the target base table schema");
            assertEquals("VIEW_APP", workbook.getSheetAt(0).getRow(2).getCell(6).getStringCellValue(), "last column should be target view schema");
        }
    }
}
