package com.example.dbcompare.tests;

import com.example.dbcompare.config.ConfigLoader;
import com.example.dbcompare.domain.enums.CompareMode;
import com.example.dbcompare.domain.enums.CompareObjectType;
import com.example.dbcompare.domain.enums.CompareRelationMode;
import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.domain.model.CompareConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigLoaderTest {
    private ConfigLoaderTest() {
    }

    public static void run() {
        Path ruleFile = null;
        try {
            ruleFile = Files.createTempFile("type-rules-", ".properties");
            Files.writeString(ruleFile, String.join(System.lineSeparator(),
                    "DATE=DATE,TIMESTAMP",
                    "INT=INT,INTEGER",
                    "VARCHAR=VARCHAR,CHARACTER VARYING"));

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
            properties.setProperty("target.viewLineageFile", "examples/demo-view-lineage/target_view_lineage.csv");

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
            properties.setProperty("compare.options.compareExists", "true");
            properties.setProperty("compare.options.compareType", "true");
            properties.setProperty("compare.options.compareNullable", "false");
            properties.setProperty("compare.options.compareDefaultValue", "false");
            properties.setProperty("compare.options.compareLength", "true");
            properties.setProperty("compare.options.source-column-missing-in-target-affect-result", "true");
            properties.setProperty("compare.options.type-mismatch-affect-result", "false");
            properties.setProperty("compare.options.length-mismatch-affect-result", "false");
            properties.setProperty("compare.options.length-target-longer-affect-result", "true");
            properties.setProperty("compare.options.default-mismatch-affect-result", "false");
            properties.setProperty("compare.options.nullable-mismatch-affect-result", "false");
            properties.setProperty("compare.options.relationMode", "table_to_view");
            properties.setProperty("compare.options.type-rule-file", ruleFile.toString());
            properties.setProperty("compare.options.typeMappings.INT", "INT,INTEGER,NUMBER");
            properties.setProperty("compare.options.objectType", "view");
            properties.setProperty("compare.options.sourceLoadThreads", "2");
            properties.setProperty("report.manualConfirmation.enabled", "true");
            properties.setProperty("report.manualConfirmation.excelDir", "build/reports");
            properties.setProperty("report.manualConfirmation.excelName", "manual-confirmation.xlsx");
            properties.setProperty("output.csvPath", "build/reports/result.csv");
            properties.setProperty("output.excelPath", "build/reports/result.xlsx");
            properties.setProperty("output.target-view-lineage-excel-path", "build/reports/result-view-lineage.xlsx");
            properties.setProperty("output.manual-confirmation-excel-path", "build/reports/result-manual.xlsx");
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
            TestSupport.assertEquals("examples/demo-view-lineage/target_view_lineage.csv", config.getTarget().getViewLineageFile(),
                    "target view lineage file should be loaded");
            TestSupport.assertEquals(1, config.getMappings().size(), "schema mapping count should be loaded");
            TestSupport.assertEquals(1, config.getTableMappings().size(), "table mapping count should be loaded");
            TestSupport.assertEquals(1, config.getIncludeSchemas().size(), "global include schemas should be loaded");
            TestSupport.assertEquals(2, config.getExcludeTables().size(), "global exclude tables should be loaded");
            TestSupport.assertTrue(config.getOptions().isCompareExists(),
                    "compare exists option should default to true when explicitly enabled");
            TestSupport.assertTrue(config.getOptions().isCompareType(),
                    "compare type option should default to true when explicitly enabled");
            TestSupport.assertTrue(!config.getOptions().isCompareNullable(),
                    "compare nullable option should honor explicit false");
            TestSupport.assertTrue(!config.getOptions().isCompareDefaultValue(),
                    "compare default option should honor explicit false");
            TestSupport.assertTrue(config.getOptions().isSourceColumnMissingInTargetAffectResult(),
                    "source-column-missing-in-target-affect-result should be loaded from config");
            TestSupport.assertTrue(!config.getOptions().isTypeMismatchAffectResult(),
                    "type-mismatch-affect-result should be loaded from config");
            TestSupport.assertTrue(!config.getOptions().isLengthMismatchAffectResult(),
                    "length-mismatch-affect-result should be loaded from config");
            TestSupport.assertTrue(config.getOptions().isLengthTargetLongerAffectResult(),
                    "length-target-longer-affect-result should be loaded from config");
            TestSupport.assertTrue(!config.getOptions().isDefaultMismatchAffectResult(),
                    "default-mismatch-affect-result should be loaded from config");
            TestSupport.assertTrue(!config.getOptions().isNullableMismatchAffectResult(),
                    "nullable-mismatch-affect-result should be loaded from config");
            TestSupport.assertEquals(CompareRelationMode.TABLE_TO_VIEW, config.getOptions().getRelationMode(),
                    "relation mode should be loaded from config");
            TestSupport.assertEquals(ruleFile.toString(), config.getOptions().getTypeRuleFile(),
                    "type rule file should be loaded from config");
            TestSupport.assertTrue(config.getOptions().getTypeMappings().get("DATE").contains("TIMESTAMP"),
                    "type rule file mappings should be parsed");
            TestSupport.assertTrue(config.getOptions().getTypeMappings().get("INT").contains("NUMBER"),
                    "inline type mappings should override or extend type rule file mappings");
            TestSupport.assertEquals(CompareObjectType.VIEW, config.getOptions().getObjectType(),
                    "object type should be loaded from config");
            TestSupport.assertEquals(2, config.getOptions().getSourceLoadThreads(),
                    "source load thread count should be loaded");
            TestSupport.assertTrue(config.getReport().getManualConfirmation().isEnabled(),
                    "manual confirmation switch should be loaded");
            TestSupport.assertEquals("build/reports", config.getReport().getManualConfirmation().getExcelDir(),
                    "manual confirmation excel dir should be loaded");
            TestSupport.assertEquals("manual-confirmation.xlsx", config.getReport().getManualConfirmation().getExcelName(),
                    "manual confirmation excel name should be loaded");
            TestSupport.assertEquals("build/reports/result.csv", config.getOutput().getCsvPath(),
                    "csv output path should be loaded");
            TestSupport.assertEquals("build/reports/result.xlsx", config.getOutput().getExcelPath(),
                    "excel output path should be loaded");
            TestSupport.assertEquals("build/reports/result-view-lineage.xlsx", config.getOutput().getTargetViewLineageExcelPath(),
                    "target view lineage excel path should be loaded");
            TestSupport.assertEquals("build/reports/result-manual.xlsx", config.getOutput().getManualConfirmationExcelPath(),
                    "manual confirmation excel output path should be loaded");
            TestSupport.assertEquals("build/reports/result.sql", config.getOutput().getSqlPath(),
                    "sql output path should be loaded");
            TestSupport.assertEquals("compare_detail_tmp", config.getOutput().getSqlTableName(),
                    "sql table name should be loaded");
            TestSupport.assertEquals("build/reports/summary.txt", config.getOutput().getSummaryPath(),
                    "summary output path should be loaded");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare config loader test", e);
        } finally {
            if (ruleFile != null) {
                try {
                    Files.deleteIfExists(ruleFile);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
