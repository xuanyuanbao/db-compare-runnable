package com.example.dbcompare.tests;

import com.example.dbcompare.config.ConfigLoader;
import com.example.dbcompare.domain.enums.CompareMode;
import com.example.dbcompare.domain.enums.CompareObjectType;
import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.domain.model.CompareConfig;

import java.util.Properties;

public final class ConfigLoaderTest {
    private ConfigLoaderTest() {
    }

    public static void run() {
        Properties properties = new Properties();
        properties.setProperty("mode", "target_driven");
        properties.setProperty("source.count", "1");
        properties.setProperty("source.1.name", "AS400_A");
        properties.setProperty("source.1.type", "snapshot");
        properties.setProperty("source.1.schema", "LEGACY_A");
        properties.setProperty("source.1.snapshotFile", "examples/demo/source_as400_1.csv");
        properties.setProperty("source.1.includeSchemas", "LEGACY_A, LEGACY_B");
        properties.setProperty("source.1.excludeTables", "TMP_A");
        properties.setProperty("source.1.driveClassName", "com.ibm.as400.access.AS400JDBCDriver");

        properties.setProperty("target.name", "GAUSS");
        properties.setProperty("target.type", "SNAPSHOT");
        properties.setProperty("target.viewOnly", "true");
        properties.setProperty("target.snapshotFile", "examples/demo/gauss_target.csv");

        properties.setProperty("mapping.count", "1");
        properties.setProperty("mapping.1.sourceDatabaseName", "AS400_A");
        properties.setProperty("mapping.1.targetSchemaName", "T_AS400_A");

        properties.setProperty("tableMapping.count", "1");
        properties.setProperty("tableMapping.1.sourceDatabaseName", "AS400_A");
        properties.setProperty("tableMapping.1.sourceSchemaName", "LEGACY_A");
        properties.setProperty("tableMapping.1.sourceTableName", "OLD_TABLE");
        properties.setProperty("tableMapping.1.targetSchemaName", "T_AS400_A");
        properties.setProperty("tableMapping.1.targetTableName", "NEW_TABLE");

        properties.setProperty("compare.includeSchemas", "LEGACY_A");
        properties.setProperty("compare.excludeTables", "TMP_A,TMP_B");
        properties.setProperty("compare.options.compareNullable", "false");
        properties.setProperty("compare.options.compareDefaultValue", "false");
        properties.setProperty("compare.options.compareLength", "true");
        properties.setProperty("compare.options.objectType", "view");
        properties.setProperty("compare.options.sourceLoadThreads", "2");
        properties.setProperty("output.csvPath", "build/reports/result.csv");
        properties.setProperty("output.excelPath", "build/reports/result.xlsx");
        properties.setProperty("output.sqlPath", "build/reports/result.sql");
        properties.setProperty("output.sqlTableName", "compare_detail_tmp");
        properties.setProperty("output.summaryPath", "build/reports/summary.txt");

        CompareConfig config = new ConfigLoader().parse(properties);

        TestSupport.assertEquals(CompareMode.TARGET_DRIVEN, config.getMode(),
                "compare mode should be loaded from config");
        TestSupport.assertEquals(1, config.getSources().size(), "source count should be loaded");
        TestSupport.assertEquals(DatabaseType.SNAPSHOT, config.getSources().get(0).getType(),
                "source type should be parsed case-insensitively");
        TestSupport.assertEquals("LEGACY_A", config.getSources().get(0).getSchema(),
                "source schema should be loaded");
        TestSupport.assertEquals("com.ibm.as400.access.AS400JDBCDriver",
                config.getSources().get(0).getDriverClassName(),
                "legacy driveClassName key should map to driverClassName");
        TestSupport.assertEquals("examples/demo/source_as400_1.csv", config.getSources().get(0).getSnapshotFile(),
                "source snapshot path should be loaded");
        TestSupport.assertEquals(2, config.getSources().get(0).getIncludeSchemas().size(),
                "source include schemas should be split");
        TestSupport.assertTrue(config.getTarget().isViewOnly(),
                "target viewOnly should be loaded");
        TestSupport.assertEquals(1, config.getMappings().size(), "schema mapping count should be loaded");
        TestSupport.assertEquals(1, config.getTableMappings().size(), "table mapping count should be loaded");
        TestSupport.assertEquals(1, config.getIncludeSchemas().size(), "global include schemas should be loaded");
        TestSupport.assertEquals(2, config.getExcludeTables().size(), "global exclude tables should be loaded");
        TestSupport.assertTrue(!config.getOptions().isCompareNullable(),
                "compare nullable option should honor explicit false");
        TestSupport.assertTrue(!config.getOptions().isCompareDefaultValue(),
                "compare default option should honor explicit false");
        TestSupport.assertEquals(CompareObjectType.VIEW, config.getOptions().getObjectType(),
                "object type should be loaded from config");
        TestSupport.assertEquals(2, config.getOptions().getSourceLoadThreads(),
                "source load thread count should be loaded");
        TestSupport.assertEquals("build/reports/result.csv", config.getOutput().getCsvPath(),
                "csv output path should be loaded");
        TestSupport.assertEquals("build/reports/result.xlsx", config.getOutput().getExcelPath(),
                "excel output path should be loaded");
        TestSupport.assertEquals("build/reports/result.sql", config.getOutput().getSqlPath(),
                "sql output path should be loaded");
        TestSupport.assertEquals("compare_detail_tmp", config.getOutput().getSqlTableName(),
                "sql table name should be loaded");
        TestSupport.assertEquals("build/reports/summary.txt", config.getOutput().getSummaryPath(),
                "summary output path should be loaded");
    }
}