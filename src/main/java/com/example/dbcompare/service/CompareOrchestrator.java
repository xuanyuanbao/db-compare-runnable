package com.example.dbcompare.service;

import com.example.dbcompare.domain.model.*;
import com.example.dbcompare.infrastructure.output.CsvReportWriter;
import com.example.dbcompare.infrastructure.output.ExcelReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryReportWriter;
import com.example.dbcompare.util.NameNormalizer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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

    public CompareSummary execute(CompareConfig compareConfig) {
        DatabaseMeta targetMetadata = metadataLoadService.loadTarget(
                compareConfig.getTarget(),
                compareConfig.getOptions().getObjectType());

        CompareSummary summary = CompareSummary.start(compareConfig.getSources().size());
        try (CsvReportWriter.CsvReportSession csvSession = csvReportWriter.open(Path.of(compareConfig.getOutput().getCsvPath()));
             ExcelReportWriter.ExcelReportSession excelSession = excelReportWriter.open(Path.of(compareConfig.getOutput().getExcelPath()))) {

            for (DataSourceInfo sourceInfo : compareConfig.getSources()) {
                DatabaseMeta sourceDbMeta = metadataLoadService.loadSource(sourceInfo);

                for (SchemaMeta sourceSchema : sourceDbMeta.getSchemas().values()) {
                    if (!shouldProcessSchema(compareConfig, sourceSchema.getSchemaName())) continue;
                    summary.incrementSourceSchemaCount();

                    for (TableMeta sourceTable : sourceSchema.getTables().values()) {
                        if (!shouldProcessTable(compareConfig, sourceTable.getTableName())) continue;
                        summary.incrementSourceTableCount();

                        MappingService.TableTarget tableTarget = mappingService.findTargetTable(
                                sourceInfo.getSourceName(), sourceSchema.getSchemaName(), sourceTable.getTableName());
                        if (tableTarget == null) continue;

                        SchemaMeta targetSchema = targetMetadata.getSchemas().get(NameNormalizer.normalize(tableTarget.getSchemaName()));
                        TableMeta targetTable = targetSchema == null ? null : targetSchema.getTables().get(NameNormalizer.normalize(tableTarget.getTableName()));
                        TableComparisonResult comparisonResult = tableCompareService.compareDetailed(sourceInfo, sourceSchema.getSchemaName(), sourceTable,
                                tableTarget.getSchemaName(), targetTable, compareConfig.getOptions());

                        csvSession.append(comparisonResult.getDiffs());
                        excelSession.append(comparisonResult.getColumnRecords());
                        summary.recordDiffs(comparisonResult.getDiffs());
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close report writers", e);
        }

        summaryReportWriter.write(Path.of(compareConfig.getOutput().getSummaryPath()), summary);
        return summary;
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