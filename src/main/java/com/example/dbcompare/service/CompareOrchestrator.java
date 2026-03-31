package com.example.dbcompare.service;

import com.example.dbcompare.domain.model.*;
import com.example.dbcompare.infrastructure.output.CsvReportWriter;
import com.example.dbcompare.infrastructure.output.ExcelReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryReportWriter;
import com.example.dbcompare.util.NameNormalizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompareOrchestrator {
    private final MetadataLoadService metadataLoadService;
    private final MappingService mappingService;
    private final TableCompareService tableCompareService;
    private final CsvReportWriter csvReportWriter;
    private final ExcelReportWriter excelReportWriter;
    private final SummaryReportWriter summaryReportWriter;

    public CompareOrchestrator(MetadataLoadService metadataLoadService,
                               MappingService mappingService,
                               TableCompareService tableCompareService,
                               CsvReportWriter csvReportWriter,
                               ExcelReportWriter excelReportWriter,
                               SummaryReportWriter summaryReportWriter) {
        this.metadataLoadService = metadataLoadService;
        this.mappingService = mappingService;
        this.tableCompareService = tableCompareService;
        this.csvReportWriter = csvReportWriter;
        this.excelReportWriter = excelReportWriter;
        this.summaryReportWriter = summaryReportWriter;
    }

    public List<DiffRecord> execute(CompareConfig compareConfig) {
        Map<String, DatabaseMeta> sourceMetadata = metadataLoadService.loadSources(
                compareConfig.getSources(),
                compareConfig.getOptions().getSourceLoadThreads(),
                compareConfig.getOptions().getObjectType());
        DatabaseMeta targetMetadata = metadataLoadService.loadTarget(
                compareConfig.getTarget(),
                compareConfig.getOptions().getObjectType());

        List<DiffRecord> allDiffs = new ArrayList<>();
        List<ColumnComparisonRecord> allColumnRecords = new ArrayList<>();
        int sourceSchemaCount = 0;
        int sourceTableCount = 0;

        for (DataSourceInfo sourceInfo : compareConfig.getSources()) {
            DatabaseMeta sourceDbMeta = sourceMetadata.get(sourceInfo.getSourceName());
            if (sourceDbMeta == null) continue;

            for (SchemaMeta sourceSchema : sourceDbMeta.getSchemas().values()) {
                if (!shouldProcessSchema(compareConfig, sourceSchema.getSchemaName())) continue;
                sourceSchemaCount++;

                for (TableMeta sourceTable : sourceSchema.getTables().values()) {
                    if (!shouldProcessTable(compareConfig, sourceTable.getTableName())) continue;
                    sourceTableCount++;

                    MappingService.TableTarget tableTarget = mappingService.findTargetTable(
                            sourceInfo.getSourceName(), sourceSchema.getSchemaName(), sourceTable.getTableName());
                    if (tableTarget == null) continue;

                    SchemaMeta targetSchema = targetMetadata.getSchemas().get(NameNormalizer.normalize(tableTarget.getSchemaName()));
                    TableMeta targetTable = targetSchema == null ? null : targetSchema.getTables().get(NameNormalizer.normalize(tableTarget.getTableName()));
                    TableComparisonResult comparisonResult = tableCompareService.compareDetailed(sourceInfo, sourceSchema.getSchemaName(), sourceTable,
                            tableTarget.getSchemaName(), targetTable, compareConfig.getOptions());
                    allDiffs.addAll(comparisonResult.getDiffs());
                    allColumnRecords.addAll(comparisonResult.getColumnRecords());
                }
            }
        }

        csvReportWriter.write(Path.of(compareConfig.getOutput().getCsvPath()), allDiffs);
        excelReportWriter.write(Path.of(compareConfig.getOutput().getExcelPath()), allColumnRecords);
        CompareSummary summary = CompareSummary.from(allDiffs, compareConfig.getSources().size(), sourceSchemaCount, sourceTableCount);
        summaryReportWriter.write(Path.of(compareConfig.getOutput().getSummaryPath()), summary);
        return allDiffs;
    }

    private boolean shouldProcessSchema(CompareConfig compareConfig, String schemaName) {
        return matches(NameNormalizer.normalize(schemaName), compareConfig.getIncludeSchemas(), compareConfig.getExcludeSchemas());
    }

    private boolean shouldProcessTable(CompareConfig compareConfig, String tableName) {
        return matches(NameNormalizer.normalize(tableName), compareConfig.getIncludeTables(), compareConfig.getExcludeTables());
    }

    private boolean matches(String value, List<String> includes, List<String> excludes) {
        for (String exclude : excludes) {
            if (value.equals(NameNormalizer.normalize(exclude))) return false;
        }
        if (includes.isEmpty()) return true;
        for (String include : includes) {
            if (value.equals(NameNormalizer.normalize(include))) return true;
        }
        return false;
    }
}
