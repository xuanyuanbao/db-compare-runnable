package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.enums.DatabaseType;
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
        for (ColumnComparisonRecord record : records) {
            if (columnName.equals(record.getColumnName())) {
                return record.getOverallStatus();
            }
        }
        return null;
    }
}
