package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.enums.ManualConfirmationMatchStatus;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.domain.model.ManualConfirmationMergeResult;
import com.example.dbcompare.domain.model.ManualConfirmationRecord;
import com.example.dbcompare.service.ManualConfirmationExcelParser;
import com.example.dbcompare.service.ManualConfirmationMergeService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualConfirmationMergeServiceTest {
    @Test
    void classifiesMatchedUnmatchedAndAmbiguousRows() {
        ManualConfirmationMergeService service = new ManualConfirmationMergeService();

        ManualConfirmationRecord matchedRecord = manual("S1", 2, "CUSTOMER", "张三", "列长度不一致", "[NAME](50,60)", "无影响");
        ManualConfirmationRecord ambiguousA = manual("S1", 3, "ORDER_VIEW", "李四", "列类型不一致", "[STATUS](CHAR,VARCHAR)", "待确认");
        ManualConfirmationRecord ambiguousB = manual("S2", 4, "ORDER_VIEW", "王五", "列类型不一致", "[STATUS](CHAR,VARCHAR)", "待确认");
        ManualConfirmationRecord unmatchedTest = manual("S3", 5, "LEGACY_ONLY", "赵六", "列数量不一致", "[COL_A]", "无影响");

        ColumnComparisonRecord matchedAi = ai("CUSTOMER", "VIEW_APP", "CUSTOMER_VIEW", "NAME", "COLUMN_LENGTH_MISMATCH", "Length mismatch");
        ColumnComparisonRecord unmatchedAi = ai("ACCOUNT", "VIEW_APP", "ACCOUNT_VIEW", "STATUS", "COLUMN_DEFAULT_MISMATCH", "Default value mismatch");
        ColumnComparisonRecord ambiguousAi = ai("ORDER", "VIEW_APP", "ORDER_VIEW", "STATUS", "COLUMN_TYPE_MISMATCH", "Type mismatch");

        ManualConfirmationMergeResult result = service.merge(
                List.of(matchedAi, unmatchedAi, ambiguousAi),
                List.of(matchedRecord, ambiguousA, ambiguousB, unmatchedTest));

        assertEquals(3, result.getMergedRows().size(), "all AI mismatch rows should participate in merge output");
        assertRow(result, "CUSTOMER", ManualConfirmationMatchStatus.MATCHED, "张三");
        assertRow(result, "ACCOUNT", ManualConfirmationMatchStatus.UNMATCHED_AI, null);
        assertRow(result, "ORDER", ManualConfirmationMatchStatus.AMBIGUOUS_AI, null);
        assertEquals(1, result.getUnmatchedTestRecords().size(), "unmatched test rows should be exposed");
        assertEquals("LEGACY_ONLY", result.getUnmatchedTestRecords().get(0).getTableName(), "unmatched test row should be preserved");
        assertTrue(result.getUnmatchedTestRecords().get(0).getAnalysisReason().contains("未找到同表对象"), "unmatched test row should carry analysis reason");
        assertEquals(2, result.getAmbiguousTestRecords().size(), "ambiguous test rows should be exposed");
        assertTrue(result.getAmbiguousTestRecords().stream().allMatch(record -> record.getAnalysisReason() != null && !record.getAnalysisReason().isBlank()),
                "ambiguous test rows should carry analysis reasons");
    }

    private void assertRow(ManualConfirmationMergeResult result,
                           String sourceTable,
                           ManualConfirmationMatchStatus expectedStatus,
                           String expectedOwner) {
        var row = result.getMergedRows().stream()
                .filter(item -> sourceTable.equals(item.getAiRecord().getSourceTableName()))
                .findFirst()
                .orElse(null);
        assertNotNull(row, "expected merge row for source table " + sourceTable);
        assertEquals(expectedStatus, row.getMatchStatus(), "unexpected match status for " + sourceTable);
        assertEquals(expectedOwner, row.getOwner(), "unexpected owner for " + sourceTable);
    }

    private ManualConfirmationRecord manual(String sheet,
                                            int rowNo,
                                            String tableName,
                                            String owner,
                                            String diffType,
                                            String detail,
                                            String result) {
        ManualConfirmationExcelParser parser = new ManualConfirmationExcelParser();
        ManualConfirmationRecord record = new ManualConfirmationRecord();
        record.setSheetName(sheet);
        record.setRowNumber(rowNo);
        record.setTableName(tableName);
        record.setOwner(owner);
        record.setDiffTypeRaw(diffType);
        record.setDiffDetailRaw(detail);
        record.setConfirmResultRaw(result);
        record.setConfirmResultNormalized(parser.normalizeConfirmResult(result));
        record.setNormalizedTableName(parser.normalizeName(tableName));
        record.setNormalizedDiffCategory(parser.normalizeDiffCategory(diffType));
        return record;
    }

    private ColumnComparisonRecord ai(String sourceTable,
                                      String targetViewSchema,
                                      String targetView,
                                      String columnName,
                                      String diffType,
                                      String message) {
        ColumnComparisonRecord record = new ColumnComparisonRecord();
        record.setSourceDatabaseName("DB2_A");
        record.setSourceSchemaName("LEGACY_A");
        record.setSourceTableName(sourceTable);
        record.setTargetViewSchemaName(targetViewSchema);
        record.setTargetViewName(targetView);
        record.setTargetLineageTableSchemaName("ODS");
        record.setTargetLineageTableName(sourceTable + "_BASE");
        record.setColumnName(columnName);
        record.setOverallStatus(ComparisonStatus.MISMATCH);
        record.setDiffTypes(diffType);
        record.setMessage(message);
        return record;
    }
}
