package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.enums.CompareRelationMode;
import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.domain.enums.DiffGroup;
import com.example.dbcompare.domain.enums.DiffType;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.domain.model.ColumnMeta;
import com.example.dbcompare.domain.model.CompareOptions;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.DiffRecord;
import com.example.dbcompare.domain.model.TableComparisonResult;
import com.example.dbcompare.domain.model.TableMeta;
import com.example.dbcompare.service.TableCompareService;

import java.util.List;

public final class TableCompareServiceTest {
    private TableCompareServiceTest() {
    }

    public static void run() {
        TableCompareService service = new TableCompareService();
        DataSourceInfo sourceInfo = new DataSourceInfo();
        sourceInfo.setSourceName("AS400_A");
        sourceInfo.setType(DatabaseType.AS400);

        CompareOptions options = new CompareOptions();
        options.setCompareDefaultValue(true);
        options.setCompareNullable(true);
        options.setSourceColumnMissingInTargetAffectResult(true);
        TableMeta sourceTable = new TableMeta("USER_INFO");
        TableMeta targetTable = new TableMeta("USER_INFO");

        sourceTable.getColumns().put("NAME", column("NAME", "VARGRAPHIC", "50", "YES", "'A'"));
        sourceTable.getColumns().put("ONLY_SRC", column("ONLY_SRC", "CHARACTER", "10", "NO", null));
        sourceTable.getColumns().put("CODE", column("CODE", "CHARACTER", "8", "NO", null));
        sourceTable.getColumns().put("ID", column("ID", "INTEGER", "10", "NO", null));

        targetTable.getColumns().put("NAME", column("NAME", "VARCHAR", "60", "NO", "'B'::character varying"));
        targetTable.getColumns().put("ONLY_TGT", column("ONLY_TGT", "VARCHAR", "10", "YES", null));
        targetTable.getColumns().put("CODE", column("CODE", "INTEGER", "8", "NO", null));
        targetTable.getColumns().put("ID", column("ID", "INTEGER", "10", "NO", null));

        List<DiffRecord> diffs = service.compare(sourceInfo, "LEGACY_A", sourceTable, "T_AS400_A", targetTable, options);
        TableComparisonResult detailed = service.compareDetailed(sourceInfo, "LEGACY_A", sourceTable, "T_AS400_A", targetTable, options);

        TestSupport.assertTrue(contains(diffs, DiffType.COLUMN_MISSING_IN_TARGET, "ONLY_SRC"),
                "missing target columns should be reported");
        TestSupport.assertTrue(contains(diffs, DiffType.COLUMN_MISSING_IN_SOURCE, "ONLY_TGT"),
                "missing source columns should be reported");
        TestSupport.assertTrue(contains(diffs, DiffType.COLUMN_LENGTH_MISMATCH, "NAME"),
                "length mismatches should be reported");
        TestSupport.assertTrue(contains(diffs, DiffType.COLUMN_DEFAULT_MISMATCH, "NAME"),
                "default value mismatches should be reported");
        TestSupport.assertTrue(contains(diffs, DiffType.COLUMN_NULLABLE_MISMATCH, "NAME"),
                "nullable mismatches should be reported");
        TestSupport.assertTrue(contains(diffs, DiffType.COLUMN_TYPE_MISMATCH, "CODE"),
                "type mismatches should be reported");
        TestSupport.assertEquals(5, detailed.getColumnRecords().size(),
                "detailed results should include matching and mismatching columns");
        TestSupport.assertEquals(ComparisonStatus.MATCH, statusOf(detailed.getColumnRecords(), "ID"),
                "matching columns should be marked as MATCH");
        TestSupport.assertEquals(ComparisonStatus.MISMATCH, statusOf(detailed.getColumnRecords(), "CODE"),
                "mismatching columns should be marked as MISMATCH");

        List<DiffRecord> missingTarget = service.compare(sourceInfo, "LEGACY_A", sourceTable, "T_AS400_A", null, options);
        TestSupport.assertEquals(1, missingTarget.size(), "missing target table should short-circuit to one diff");
        TestSupport.assertEquals(DiffType.TARGET_TABLE_NOT_FOUND, missingTarget.get(0).getDiffType(),
                "missing target table should use the expected diff type");

        CompareOptions viewOptions = new CompareOptions();
        viewOptions.setRelationMode(CompareRelationMode.TABLE_TO_VIEW);
        viewOptions.putTypeMapping("DATE", List.of("TIMESTAMP"));

        TableMeta sourceBusinessTable = new TableMeta("ORDER_INFO");
        sourceBusinessTable.getColumns().put("ID", column("ID", "INTEGER", "10", "NO", null));
        sourceBusinessTable.getColumns().put("CREATED_AT", column("CREATED_AT", "DATE", "10", "NO", null));
        sourceBusinessTable.getColumns().put("LEGACY_ONLY", column("LEGACY_ONLY", "VARCHAR", "20", "YES", null));

        TableMeta targetView = new TableMeta("ORDER_VIEW");
        targetView.getColumns().put("ID", column("ID", "INTEGER", "10", "NO", null));
        targetView.getColumns().put("CREATED_AT", column("CREATED_AT", "TIMESTAMP", "10", "NO", null));

        TableComparisonResult viewComparison = service.compareDetailed(sourceInfo, "LEGACY_A", sourceBusinessTable, "VIEW_APP", targetView, viewOptions);
        TestSupport.assertEquals(0, viewComparison.getMainDiffs().size(),
                "custom type mapping should prevent a view compare from reporting main diffs");
        TestSupport.assertEquals(1, viewComparison.getInfoDiffs().size(),
                "table-only columns should be emitted as info diffs in table-to-view mode");
        TestSupport.assertEquals(DiffType.VIEW_MISSING_COLUMN_INFO, viewComparison.getInfoDiffs().get(0).getDiffType(),
                "table-only columns should use the dedicated info diff type");
        TestSupport.assertEquals(DiffGroup.INFO, recordOf(viewComparison.getColumnRecords(), "LEGACY_ONLY").getDiffGroup(),
                "table-only column rows should be marked as info diff rows");
        TestSupport.assertEquals(ComparisonStatus.MATCH, recordOf(viewComparison.getColumnRecords(), "LEGACY_ONLY").getOverallStatus(),
                "info-only rows should not be treated as result-affecting mismatches");

        CompareOptions tableInfoOptions = new CompareOptions();
        tableInfoOptions.setRelationMode(CompareRelationMode.TABLE_TO_TABLE);
        tableInfoOptions.setSourceColumnMissingInTargetAffectResult(false);

        TableMeta sourceTableInfo = new TableMeta("USER_INFO_INFO");
        sourceTableInfo.getColumns().put("ID", column("ID", "INTEGER", "10", "NO", null));
        sourceTableInfo.getColumns().put("ONLY_SRC", column("ONLY_SRC", "CHARACTER", "10", "NO", null));

        TableMeta targetTableInfo = new TableMeta("USER_INFO_INFO");
        targetTableInfo.getColumns().put("ID", column("ID", "INTEGER", "10", "NO", null));

        TableComparisonResult tableInfoComparison = service.compareDetailed(sourceInfo, "LEGACY_A", sourceTableInfo, "T_AS400_A", targetTableInfo, tableInfoOptions);
        TestSupport.assertEquals(0, tableInfoComparison.getMainDiffs().size(),
                "source-only columns should become info diffs when the affect-result switch is disabled");
        TestSupport.assertEquals(1, tableInfoComparison.getInfoDiffs().size(),
                "source-only columns should still be recorded when the affect-result switch is disabled");
        TestSupport.assertEquals(DiffType.SOURCE_COLUMN_MISSING_IN_TARGET, tableInfoComparison.getInfoDiffs().get(0).getDiffType(),
                "source-only columns should use the dedicated configurable info diff type");
        TestSupport.assertEquals(DiffGroup.INFO, recordOf(tableInfoComparison.getColumnRecords(), "ONLY_SRC").getDiffGroup(),
                "source-only table rows should be downgraded to info diff rows when configured");
        TestSupport.assertEquals(ComparisonStatus.MATCH, recordOf(tableInfoComparison.getColumnRecords(), "ONLY_SRC").getOverallStatus(),
                "source-only table rows should not affect the overall status when the switch is disabled");
    }

    private static ColumnMeta column(String name, String type, String length, String nullable, String defaultValue) {
        ColumnMeta column = new ColumnMeta();
        column.setColumnName(name);
        column.setDataType(type);
        column.setLength(length);
        column.setNullable(nullable);
        column.setDefaultValue(defaultValue);
        return column;
    }

    private static boolean contains(List<DiffRecord> diffs, DiffType diffType, String columnName) {
        for (DiffRecord diff : diffs) {
            if (diffType == diff.getDiffType() && columnName.equals(diff.getColumnName())) {
                return true;
            }
        }
        return false;
    }

    private static ComparisonStatus statusOf(List<ColumnComparisonRecord> records, String columnName) {
        ColumnComparisonRecord record = recordOf(records, columnName);
        return record == null ? null : record.getOverallStatus();
    }

    private static ColumnComparisonRecord recordOf(List<ColumnComparisonRecord> records, String columnName) {
        for (ColumnComparisonRecord record : records) {
            if (columnName.equals(record.getColumnName())) {
                return record;
            }
        }
        return null;
    }
}
