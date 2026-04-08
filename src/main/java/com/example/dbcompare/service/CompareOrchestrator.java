package com.example.dbcompare.service;

import com.example.dbcompare.domain.enums.CompareMode;
import com.example.dbcompare.domain.enums.CompareObjectType;
import com.example.dbcompare.domain.enums.DiffType;
import com.example.dbcompare.domain.model.CompareConfig;
import com.example.dbcompare.domain.model.CompareSummary;
import com.example.dbcompare.domain.model.CompareTask;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.DatabaseMeta;
import com.example.dbcompare.domain.model.SchemaMeta;
import com.example.dbcompare.domain.model.SourceTableLoadResult;
import com.example.dbcompare.domain.model.TableComparisonResult;
import com.example.dbcompare.domain.model.TableMeta;
import com.example.dbcompare.infrastructure.output.CsvReportWriter;
import com.example.dbcompare.infrastructure.output.ExcelReportWriter;
import com.example.dbcompare.infrastructure.output.SqlReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryReportWriter;
import com.example.dbcompare.util.NameNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CompareOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(CompareOrchestrator.class);

    private final MetadataLoadService metadataLoadService;
    private final MappingService mappingService;
    private final TableCompareService tableCompareService;
    private final CsvReportWriter csvReportWriter;
    private final ExcelReportWriter excelReportWriter;
    private final SqlReportWriter sqlReportWriter;
    private final SummaryReportWriter summaryReportWriter;
    private final ViewParser viewParser;

    public CompareOrchestrator(MetadataLoadService metadataLoadService,
                               MappingService mappingService,
                               TableCompareService tableCompareService,
                               CsvReportWriter csvReportWriter,
                               ExcelReportWriter excelReportWriter,
                               SqlReportWriter sqlReportWriter,
                               SummaryReportWriter summaryReportWriter) {
        this(metadataLoadService, mappingService, tableCompareService, csvReportWriter, excelReportWriter,
                sqlReportWriter, summaryReportWriter, new ViewParser());
    }

    CompareOrchestrator(MetadataLoadService metadataLoadService,
                        MappingService mappingService,
                        TableCompareService tableCompareService,
                        CsvReportWriter csvReportWriter,
                        ExcelReportWriter excelReportWriter,
                        SqlReportWriter sqlReportWriter,
                        SummaryReportWriter summaryReportWriter,
                        ViewParser viewParser) {
        this.metadataLoadService = metadataLoadService;
        this.mappingService = mappingService;
        this.tableCompareService = tableCompareService;
        this.csvReportWriter = csvReportWriter;
        this.excelReportWriter = excelReportWriter;
        this.sqlReportWriter = sqlReportWriter;
        this.summaryReportWriter = summaryReportWriter;
        this.viewParser = viewParser;
    }

    public CompareSummary execute(CompareConfig compareConfig) {
        return compareConfig.getMode() == CompareMode.TARGET_DRIVEN
                ? executeTargetDriven(compareConfig)
                : executeFullScan(compareConfig);
    }

    private CompareSummary executeFullScan(CompareConfig compareConfig) {
        DatabaseMeta targetMetadata = metadataLoadService.loadTarget(
                compareConfig.getTarget(),
                compareConfig.getOptions().getObjectType());

        CompareSummary summary = CompareSummary.start(compareConfig.getSources().size());
        try (CsvReportWriter.CsvReportSession csvSession = csvReportWriter.open(Path.of(compareConfig.getOutput().getCsvPath()));
             ExcelReportWriter.ExcelReportSession excelSession = excelReportWriter.open(Path.of(compareConfig.getOutput().getExcelPath()));
             SqlReportWriter.SqlReportSession sqlSession = sqlReportWriter.open(
                     Path.of(compareConfig.getOutput().getSqlPath()),
                     compareConfig.getOutput().getSqlTableName())) {

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

                        appendResult(summary, csvSession, excelSession, sqlSession, comparisonResult);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close report writers", e);
        }

        summaryReportWriter.write(Path.of(compareConfig.getOutput().getSummaryPath()), summary);
        return summary;
    }

    private CompareSummary executeTargetDriven(CompareConfig compareConfig) {
        CompareObjectType targetObjectType = resolveTargetObjectType(compareConfig);
        DatabaseMeta targetMetadata = metadataLoadService.loadTarget(compareConfig.getTarget(), targetObjectType);
        List<CompareTask> compareTasks = buildTargetDrivenTasks(compareConfig, targetMetadata);
        log.info("Target-driven compare prepared {} tasks using target {} objects", compareTasks.size(), targetObjectType.name().toLowerCase());

        CompareSummary summary = CompareSummary.start(compareConfig.getSources().size());
        Map<String, DataSourceInfo> sourceIndex = indexSources(compareConfig.getSources());
        Set<String> visitedSourceSchemas = new HashSet<>();

        try (CsvReportWriter.CsvReportSession csvSession = csvReportWriter.open(Path.of(compareConfig.getOutput().getCsvPath()));
             ExcelReportWriter.ExcelReportSession excelSession = excelReportWriter.open(Path.of(compareConfig.getOutput().getExcelPath()));
             SqlReportWriter.SqlReportSession sqlSession = sqlReportWriter.open(
                     Path.of(compareConfig.getOutput().getSqlPath()),
                     compareConfig.getOutput().getSqlTableName())) {

            for (int index = 0; index < compareTasks.size(); index++) {
                CompareTask task = compareTasks.get(index);
                log.info("Target-driven compare [{}/{}]: target {}.{} -> source {}.{}",
                        index + 1,
                        compareTasks.size(),
                        task.getTargetSchema(),
                        task.getViewName(),
                        task.getSourceSchema() == null ? "<AUTO>" : task.getSourceSchema(),
                        task.getTableName());
                DataSourceInfo sourceInfo = sourceIndex.get(NameNormalizer.normalize(task.getSourceDb()));
                if (sourceInfo == null) {
                    continue;
                }
                String schemaKey = NameNormalizer.normalize(task.getSourceDb()) + "::" + NameNormalizer.normalize(task.getSourceSchema());
                if (visitedSourceSchemas.add(schemaKey)) {
                    summary.incrementSourceSchemaCount();
                }
                summary.incrementSourceTableCount();

                SourceTableLoadResult sourceTableLoadResult = metadataLoadService.loadSourceTable(sourceInfo, task.getSourceSchema(), task.getTableName());
                SchemaMeta targetSchema = targetMetadata.getSchemas().get(NameNormalizer.normalize(task.getTargetSchema()));
                TableMeta targetView = targetSchema == null ? null : targetSchema.getTables().get(NameNormalizer.normalize(task.getViewName()));

                TableComparisonResult comparisonResult;
                if (sourceTableLoadResult.isAmbiguous()) {
                    comparisonResult = tableCompareService.buildSourceIssueResult(sourceInfo,
                            sourceTableLoadResult.getSchemaName(),
                            sourceTableLoadResult.getTableName(),
                            task.getTargetSchema(),
                            task.getViewName(),
                            DiffType.SOURCE_TABLE_AMBIGUOUS,
                            sourceTableLoadResult.getMessage());
                } else {
                    String sourceSchemaName = sourceTableLoadResult.getSchemaName() != null
                            ? sourceTableLoadResult.getSchemaName()
                            : task.getSourceSchema();
                    comparisonResult = tableCompareService.compareDetailed(sourceInfo,
                            sourceSchemaName,
                            sourceTableLoadResult.getTableMeta(),
                            task.getTargetSchema(),
                            targetView,
                            compareConfig.getOptions());
                }
                appendResult(summary, csvSession, excelSession, sqlSession, comparisonResult);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close report writers", e);
        }

        summaryReportWriter.write(Path.of(compareConfig.getOutput().getSummaryPath()), summary);
        return summary;
    }

    private List<CompareTask> buildTargetDrivenTasks(CompareConfig compareConfig, DatabaseMeta targetMetadata) {
        List<CompareTask> tasks = new ArrayList<>();
        Map<String, DataSourceInfo> sourceIndex = indexSources(compareConfig.getSources());
        for (SchemaMeta targetSchema : targetMetadata.getSchemas().values()) {
            if (!shouldProcessSchema(compareConfig, targetSchema.getSchemaName())) {
                continue;
            }
            for (TableMeta targetView : targetSchema.getTables().values()) {
                if (!shouldProcessTable(compareConfig, targetView.getTableName())) {
                    continue;
                }
                MappingService.SourceTarget explicitSource = mappingService.findSourceForTarget(targetSchema.getSchemaName(), targetView.getTableName());
                String sourceDbName = explicitSource == null
                        ? mappingService.findSourceDatabaseByTargetSchema(targetSchema.getSchemaName())
                        : explicitSource.getSourceDatabaseName();
                if (sourceDbName == null) {
                    log.info("Skipping target {}.{} because no source database mapping was found",
                            targetSchema.getSchemaName(), targetView.getTableName());
                    continue;
                }
                DataSourceInfo sourceInfo = sourceIndex.get(NameNormalizer.normalize(sourceDbName));
                if (sourceInfo == null) {
                    log.info("Skipping target {}.{} because mapped source {} is not configured",
                            targetSchema.getSchemaName(), targetView.getTableName(), sourceDbName);
                    continue;
                }
                String sourceSchema = explicitSource != null && explicitSource.getSourceSchemaName() != null
                        ? explicitSource.getSourceSchemaName()
                        : preferredSourceSchema(sourceInfo);
                String sourceTableName = explicitSource != null && explicitSource.getSourceTableName() != null
                        ? explicitSource.getSourceTableName()
                        : viewParser.resolveBaseTableName(targetView.getTableName(), null);
                tasks.add(new CompareTask(sourceInfo.getSourceName(), sourceSchema, sourceTableName,
                        targetSchema.getSchemaName(), targetView.getTableName()));
            }
        }
        return tasks;
    }

    private CompareObjectType resolveTargetObjectType(CompareConfig compareConfig) {
        if (compareConfig.getTarget() != null && compareConfig.getTarget().isViewOnly()) {
            return CompareObjectType.VIEW;
        }
        return compareConfig.getOptions().getObjectType();
    }

    private void appendResult(CompareSummary summary,
                              CsvReportWriter.CsvReportSession csvSession,
                              ExcelReportWriter.ExcelReportSession excelSession,
                              SqlReportWriter.SqlReportSession sqlSession,
                              TableComparisonResult comparisonResult) {
        csvSession.append(comparisonResult.getDiffs());
        excelSession.append(comparisonResult.getColumnRecords());
        sqlSession.append(comparisonResult.getColumnRecords());
        summary.recordDiffs(comparisonResult.getDiffs());
    }

    private Map<String, DataSourceInfo> indexSources(List<DataSourceInfo> sources) {
        Map<String, DataSourceInfo> sourceIndex = new HashMap<>();
        for (DataSourceInfo source : sources) {
            sourceIndex.put(NameNormalizer.normalize(source.getSourceName()), source);
        }
        return sourceIndex;
    }

    private String preferredSourceSchema(DataSourceInfo sourceInfo) {
        if (sourceInfo.getSchema() != null && !sourceInfo.getSchema().isBlank()) {
            return NameNormalizer.normalize(sourceInfo.getSchema());
        }
        if (sourceInfo.getIncludeSchemas().size() == 1) {
            return NameNormalizer.normalize(sourceInfo.getIncludeSchemas().get(0));
        }
        return null;
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