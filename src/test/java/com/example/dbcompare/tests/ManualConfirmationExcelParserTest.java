package com.example.dbcompare.tests;

import com.example.dbcompare.domain.model.ManualConfirmationRecord;
import com.example.dbcompare.service.ManualConfirmationExcelParser;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManualConfirmationExcelParserTest {
    @Test
    void parsesMultipleSheetsAndSkipsInstructionPages(@TempDir Path tempDir) throws Exception {
        Path workbookPath = tempDir.resolve("manual.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(workbookPath)) {
            var instructionSheet = workbook.createSheet("说明");
            instructionSheet.createRow(0).createCell(0).setCellValue("这是说明页");

            var dataSheet = workbook.createSheet("as400");
            dataSheet.createRow(0).createCell(0).setCellValue("标题");
            var headerRow = dataSheet.createRow(1);
            headerRow.createCell(0).setCellValue("表名");
            headerRow.createCell(1).setCellValue("责任人");
            headerRow.createCell(2).setCellValue("不一致类型");
            headerRow.createCell(3).setCellValue("不一致详细");
            headerRow.createCell(4).setCellValue("确认结果");
            headerRow.createCell(5).setCellValue("附加说明");
            var row1 = dataSheet.createRow(2);
            row1.createCell(0).setCellValue("bkzjfl");
            row1.createCell(1).setCellValue("张三");
            row1.createCell(2).setCellValue("列类型不一致");
            row1.createCell(3).setCellValue("[ID](INTEGER, DECIMAL)");
            row1.createCell(4).setCellValue("无影响");
            row1.createCell(5).setCellValue("已确认");
            var row2 = dataSheet.createRow(3);
            row2.createCell(0).setCellValue("bkzjfl");
            row2.createCell(2).setCellValue("列长度不一致(qrydb大于400)");
            row2.createCell(3).setCellValue("[NAME](50, 60)");
            row2.createCell(4).setCellValue("已修复并上传代码");

            workbook.write(outputStream);
        }

        ManualConfirmationExcelParser parser = new ManualConfirmationExcelParser();
        List<ManualConfirmationRecord> records = parser.parse(workbookPath);

        assertEquals(2, records.size(), "parser should only load rows from data sheets");
        assertEquals("as400", records.get(0).getSheetName(), "parser should keep source sheet name");
        assertEquals("TYPE", records.get(0).getNormalizedDiffCategory(), "type mismatch should be normalized");
        assertEquals("无影响", records.get(0).getConfirmResultNormalized(), "confirm result should be normalized");
        assertEquals("LENGTH", records.get(1).getNormalizedDiffCategory(), "length mismatch should be normalized");
        assertEquals("已修复", records.get(1).getConfirmResultNormalized(), "repair result should be normalized");
    }
}
