package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.CompareMode;
import com.example.dbcompare.domain.enums.CompareObjectType;
import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.domain.model.ColumnMeta;
import com.example.dbcompare.domain.model.CompareConfig;
import com.example.dbcompare.domain.model.CompareSummary;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.DatabaseMeta;
import com.example.dbcompare.domain.model.SchemaMapping;
import com.example.dbcompare.domain.model.SchemaMeta;
import com.example.dbcompare.domain.model.SourceTableLoadResult;
import com.example.dbcompare.domain.model.TableMapping;
import com.example.dbcompare.domain.model.TableMeta;
import com.example.dbcompare.domain.model.TargetViewLineageEntry;
import com.example.dbcompare.infrastructure.output.CsvReportWriter;
import com.example.dbcompare.infrastructure.output.ExcelReportWriter;
import com.example.dbcompare.infrastructure.output.SqlReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryExcelReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryReportWriter;
import com.example.dbcompare.service.CompareOrchestrator;
import com.example.dbcompare.service.MappingService;
import com.example.dbcompare.service.MetadataLoadService;
import com.example.dbcompare.service.TableCompareService;
import com.example.dbcompare.service.TargetViewLineageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetDrivenCompareOrchestratorTest {
    @Test
    void comparesOnlyTargetDrivenTasksAndLoadsSourceTablesOneByOne(@TempDir Path tempDir) throws Exception {
        CompareConfig config = new CompareConfig();
        config.setMode(CompareMode.TARGET_DRIVEN);

        DataSourceInfo source = new DataSourceInfo();
        source.setSourceName("DB2_A");
        source.setType(DatabaseType.SNAPSHOT);
        source.setSchema("LEGACY_A");
        config.getSources().add(source);

        DataSourceInfo target = new DataSourceInfo();
        target.setSourceName("GAUSS");
        target.setType(DatabaseType.SNAPSHOT);
        target.setViewOnly(true);
        config.setTarget(target);

        SchemaMapping schemaMapping = new SchemaMapping();
        schemaMapping.setSourceDatabaseName("DB2_A");
        schemaMapping.setTargetSchemaName("OG_DB2_1");
        config.getMappings().add(schemaMapping);

        TableMapping tableMapping = new TableMapping();
        tableMapping.setSourceDatabaseName("DB2_A");
        tableMapping.setSourceSchemaName("LEGACY_A");
        tableMapping.setSourceTableName("CUSTOMER");
        tableMapping.setTargetSchemaName("OG_DB2_1");
        tableMapping.setTargetTableName("CUSTOMER_VIEW");
        config.getTableMappings().add(tableMapping);

        config.getOutput().setCsvPath(tempDir.resolve("report.csv").toString());
        config.getOutput().setExcelPath(tempDir.resolve("detail.xlsx").toString());
        config.getOutput().setSummaryExcelPath(tempDir.resolve("summary.xlsx").toString());
        config.getOutput().setSqlPath(tempDir.resolve("detail.sql").toString());
        config.getOutput().setSummaryPath(tempDir.resolve("summary.txt").toString());

        RecordingMetadataLoadService metadataLoadService = new RecordingMetadataLoadService();
        CompareOrchestrator orchestrator = new CompareOrchestrator(
                metadataLoadService,
                new MappingService(config.getMappings(), config.getTableMappings()),
                new TableCompareService(),
                new CsvReportWriter(),
                new ExcelReportWriter(),
                new SqlReportWriter(),
                new SummaryReportWriter(),
                new SummaryExcelReportWriter(),
                new StubTargetViewLineageService());

        CompareSummary summary = orchestrator.execute(config);

        assertEquals(CompareObjectType.VIEW, metadataLoadService.targetObjectType, "target-driven mode should load target views");
        assertEquals(0, metadataLoadService.fullSourceLoadCount, "target-driven mode should not full-scan source metadata");
        assertEquals(1, metadataLoadService.singleTableLoadCount, "target-driven mode should load one source table per task");
        assertEquals(1, summary.getDiffCount(), "the targeted compare should preserve the column mismatch result");
        assertTrue(Files.exists(Path.of(config.getOutput().getCsvPath())), "csv report should be created");
        assertTrue(Files.exists(Path.of(config.getOutput().getExcelPath())), "excel report should be created");
        assertTrue(Files.exists(Path.of(config.getOutput().getSummaryExcelPath())), "summary excel report should be created");
        assertTrue(Files.exists(Path.of(config.getOutput().getSqlPath())), "sql report should be created");
        String csv = Files.readString(Path.of(config.getOutput().getCsvPath()));
        assertTrue(csv.contains("目标ViewSchema"), "csv report should expose target view columns");
        assertTrue(csv.contains("OG_DB2_1"), "csv report should contain target view schema values");
        assertTrue(csv.contains("CUSTOMER_VIEW"), "csv report should contain target view values");
        assertTrue(csv.contains("ODS"), "csv report should contain target lineage table schema values");
        assertTrue(csv.contains("CUSTOMER_BASE"), "csv report should contain target lineage table values");
    }

    private static final class RecordingMetadataLoadService extends MetadataLoadService {
        private CompareObjectType targetObjectType;
        private int fullSourceLoadCount;
        private int singleTableLoadCount;

        @Override
        public DatabaseMeta loadTarget(DataSourceInfo target, CompareObjectType objectType) {
            this.targetObjectType = objectType;
            DatabaseMeta databaseMeta = new DatabaseMeta(target.getSourceName());
            SchemaMeta schemaMeta = databaseMeta.getSchemas().computeIfAbsent("OG_DB2_1", SchemaMeta::new);
            TableMeta tableMeta = new TableMeta("CUSTOMER_VIEW");
            tableMeta.getColumns().put("ID", column("ID", "INTEGER", "10", "NO", null, 1));
            tableMeta.getColumns().put("NAME", column("NAME", "VARCHAR", "50", "YES", null, 2));
            schemaMeta.getTables().put("CUSTOMER_VIEW", tableMeta);
            return databaseMeta;
        }

        @Override
        public DatabaseMeta loadSource(DataSourceInfo source) {
            fullSourceLoadCount++;
            return new DatabaseMeta(source.getSourceName());
        }

        @Override
        public SourceTableLoadResult loadSourceTable(DataSourceInfo source, String schemaName, String tableName) {
            singleTableLoadCount++;
            TableMeta tableMeta = new TableMeta(tableName);
            tableMeta.getColumns().put("ID", column("ID", "INTEGER", "10", "NO", null, 1));
            tableMeta.getColumns().put("NAME", column("NAME", "VARCHAR", "30", "YES", null, 2));
            return SourceTableLoadResult.found(schemaName, tableName, tableMeta);
        }

        private ColumnMeta column(String name, String type, String length, String nullable, String defaultValue, int ordinal) {
            ColumnMeta columnMeta = new ColumnMeta();
            columnMeta.setColumnName(name);
            columnMeta.setDataType(type);
            columnMeta.setLength(length);
            columnMeta.setNullable(nullable);
            columnMeta.setDefaultValue(defaultValue);
            columnMeta.setOrdinalPosition(ordinal);
            return columnMeta;
        }
    }

    private static final class StubTargetViewLineageService extends TargetViewLineageService {
        @Override
        public List<TargetViewLineageEntry> loadLineage(DataSourceInfo target, String targetViewSchema, String targetView) {
            return List.of(
                    new TargetViewLineageEntry(targetViewSchema, targetView, "ODS", "CUSTOMER_BASE"),
                    new TargetViewLineageEntry(targetViewSchema, targetView, "DWD", "CUSTOMER_PROFILE")
            );
        }
    }
}
