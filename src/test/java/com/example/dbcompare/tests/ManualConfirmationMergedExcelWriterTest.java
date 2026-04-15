package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.enums.ManualConfirmationMatchStatus;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.domain.model.CompareConfig;
import com.example.dbcompare.domain.model.ManualConfirmationMergeResult;
import com.example.dbcompare.domain.model.ManualConfirmationMergedRow;
import com.example.dbcompare.domain.model.ManualConfirmationRecord;
import com.example.dbcompare.infrastructure.output.ManualConfirmationMergedExcelWriter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManualConfirmationMergedExcelWriterTest {
    @Test
    void writesExpectedWorkbookStructure(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("merged.xlsx");
        CompareConfig config = new CompareConfig();
        config.getReport().getManualConfirmation().setEnabled(true);
        config.getReport().getManualConfirmation().setExcelPath(tempDir.resolve("manual.xlsx").toString());
        config.getOutput().setManualConfirmationExcelPath(output.toString());

        ManualConfirmationMergeResult result = new ManualConfirmationMergeResult();
        result.getMergedRows().add(matchedRow());
        result.getUnmatchedTestRecords().add(unmatchedTestRecord());
        result.getAmbiguousTestRecords().add(ambiguousTestRecord());

        new ManualConfirmationMergedExcelWriter().write(output, config, result);

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals("说明", workbook.getSheetAt(0).getSheetName(), "instruction sheet should be first");
            assertEquals("人工确认融合明细", workbook.getSheetAt(1).getSheetName(), "merged detail sheet should be present");
            assertEquals("表级融合状态", workbook.getSheetAt(2).getSheetName(), "table summary sheet should be present");
            assertEquals("未匹配测试组记录", workbook.getSheetAt(6).getSheetName(), "unmatched test sheet should be present");
            assertEquals("歧义匹配记录", workbook.getSheetAt(7).getSheetName(), "ambiguous sheet should be present");
            assertEquals("责任人", workbook.getSheet("人工确认融合明细").getRow(0).getCell(11).getStringCellValue(), "merged sheet should include owner column");
            assertEquals("张三", workbook.getSheet("人工确认融合明细").getRow(1).getCell(11).getStringCellValue(), "merged sheet should carry matched owner");
            assertEquals("已匹配", workbook.getSheet("人工确认融合明细").getRow(1).getCell(15).getStringCellValue(), "merged sheet should localize match status");
            assertEquals("分析原因", workbook.getSheet("未匹配测试组记录").getRow(0).getCell(8).getStringCellValue(), "unmatched test sheet should include analysis column");
            assertEquals("未找到同表对象", workbook.getSheet("未匹配测试组记录").getRow(1).getCell(8).getStringCellValue(), "unmatched test sheet should keep analysis reason");
        }
    }

    private ManualConfirmationMergedRow matchedRow() {
        ColumnComparisonRecord ai = new ColumnComparisonRecord();
        ai.setSourceDatabaseName("DB2_A");
        ai.setSourceSchemaName("LEGACY_A");
        ai.setSourceTableName("CUSTOMER");
        ai.setTargetViewSchemaName("VIEW_APP");
        ai.setTargetViewName("CUSTOMER_VIEW");
        ai.setTargetLineageTableSchemaName("ODS");
        ai.setTargetLineageTableName("CUSTOMER_BASE");
        ai.setColumnName("NAME");
        ai.setOverallStatus(ComparisonStatus.MISMATCH);
        ai.setDiffTypes("COLUMN_LENGTH_MISMATCH");
        ai.setMessage("Length mismatch");

        ManualConfirmationMergedRow row = new ManualConfirmationMergedRow();
        row.setAiRecord(ai);
        row.setAiDiffCategory("字段长度问题");
        row.setAiDiffDetail("Length mismatch");
        row.setRiskLevel("中");
        row.setTargetViewSchema("VIEW_APP");
        row.setTargetView("CUSTOMER_VIEW");
        row.setTargetTableSchema("ODS");
        row.setTargetTable("CUSTOMER_BASE");
        row.setOwner("张三");
        row.setConfirmResultRaw("无影响");
        row.setConfirmResultNormalized("无影响");
        row.setComment("人工确认");
        row.setMatchStatus(ManualConfirmationMatchStatus.MATCHED);
        row.setMatchBasis("表名 + 差异类型唯一匹配");
        row.setCandidateCount(1);
        row.setMatchedSheetName("as400");
        row.setMatchedTableName("CUSTOMER");
        row.setMatchedDiffType("列长度不一致");
        row.setMatchedDiffDetail("[NAME](50,60)");
        return row;
    }

    private ManualConfirmationRecord unmatchedTestRecord() {
        ManualConfirmationRecord record = new ManualConfirmationRecord();
        record.setSheetName("opsp400");
        record.setRowNumber(5);
        record.setTableName("LEGACY_ONLY");
        record.setDiffTypeRaw("列数量不一致");
        record.setDiffDetailRaw("[COL_A]");
        record.setConfirmResultRaw("无影响");
        record.setAnalysisReason("未找到同表对象");
        return record;
    }

    private ManualConfirmationRecord ambiguousTestRecord() {
        ManualConfirmationRecord record = new ManualConfirmationRecord();
        record.setSheetName("opsp400");
        record.setRowNumber(6);
        record.setTableName("ORDER_VIEW");
        record.setDiffTypeRaw("列类型不一致");
        record.setDiffDetailRaw("[STATUS](CHAR,VARCHAR)");
        record.setAnalysisReason("同表同类型候选过多");
        return record;
    }
}
