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
import com.example.dbcompare.domain.model.TargetViewLineageEntry;
import com.example.dbcompare.domain.model.TargetViewLineageReportRow;
import com.example.dbcompare.infrastructure.output.CsvReportWriter;
import com.example.dbcompare.infrastructure.output.ExcelReportWriter;
import com.example.dbcompare.infrastructure.output.SqlReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryExcelReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryReportWriter;
import com.example.dbcompare.infrastructure.output.TargetViewLineageExcelWriter;
import com.example.dbcompare.util.NameNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CompareOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(CompareOrchestrator.class);

    private final MetadataLoadService metadataLoadService;
    private final MappingService mappingService;
    private final TableCompareService tableCompareService;
    private final CsvReportWriter csvReportWriter;
    private final ExcelReportWriter excelReportWriter;
    private final SqlReportWriter sqlReportWriter;
    private final TargetViewLineageExcelWriter targetViewLineageExcelWriter;
    private final SummaryReportWriter summaryReportWriter;
    private final SummaryExcelReportWriter summaryExcelReportWriter;
    private final TargetViewLineageService targetViewLineageService;
    private final ViewParser viewParser;

    public CompareOrchestrator(MetadataLoadService metadataLoadService,
                               MappingService mappingService,
                               TableCompareService tableCompareService,
                               CsvReportWriter csvReportWriter,
                               ExcelReportWriter excelReportWriter,
                               SqlReportWriter sqlReportWriter,
                               SummaryReportWriter summaryReportWriter) {
        this(metadataLoadService, mappingService, tableCompareService, csvReportWriter, excelReportWriter,
                sqlReportWriter, new TargetViewLineageExcelWriter(), summaryReportWriter,
                new SummaryExcelReportWriter(), new TargetViewLineageService(), new ViewParser());
    }

    public CompareOrchestrator(MetadataLoadService metadataLoadService,
                               MappingService mappingService,
                               TableCompareService tableCompareService,
                               CsvReportWriter csvReportWriter,
                               ExcelReportWriter excelReportWriter,
                               SqlReportWriter sqlReportWriter,
                               SummaryReportWriter summaryReportWriter,
                               SummaryExcelReportWriter summaryExcelReportWriter) {
        this(metadataLoadService, mappingService, tableCompareService, csvReportWriter, excelReportWriter,
                sqlReportWriter, new TargetViewLineageExcelWriter(), summaryReportWriter,
                summaryExcelReportWriter, new TargetViewLineageService(), new ViewParser());
    }

    public CompareOrchestrator(MetadataLoadService metadataLoadService,
                               MappingService mappingService,
                               TableCompareService tableCompareService,
                               CsvReportWriter csvReportWriter,
                               ExcelReportWriter excelReportWriter,
                               SqlReportWriter sqlReportWriter,
                               TargetViewLineageExcelWriter targetViewLineageExcelWriter,
                               SummaryReportWriter summaryReportWriter,
                               SummaryExcelReportWriter summaryExcelReportWriter,
                               TargetViewLineageService targetViewLineageService) {
        this(metadataLoadService, mappingService, tableCompareService, csvReportWriter, excelReportWriter,
                sqlReportWriter, targetViewLineageExcelWriter, summaryReportWriter,
                summaryExcelReportWriter, targetViewLineageService, new ViewParser());
    }

    CompareOrchestrator(MetadataLoadService metadataLoadService,
                        MappingService mappingService,
                        TableCompareService tableCompareService,
                        CsvReportWriter csvReportWriter,
                        ExcelReportWriter excelReportWriter,
                        SqlReportWriter sqlReportWriter,
                        TargetViewLineageExcelWriter targetViewLineageExcelWriter,
                        SummaryReportWriter summaryReportWriter,
                        SummaryExcelReportWriter summaryExcelReportWriter,
                        TargetViewLineageService targetViewLineageService,
                        ViewParser viewParser) {
        this.metadataLoadService = metadataLoadService;
        this.mappingService = mappingService;
        this.tableCompareService = tableCompareService;
        this.csvReportWriter = csvReportWriter;
        this.excelReportWriter = excelReportWriter;
        this.sqlReportWriter = sqlReportWriter;
        this.targetViewLineageExcelWriter = targetViewLineageExcelWriter;
        this.summaryReportWriter = summaryReportWriter;
        this.summaryExcelReportWriter = summaryExcelReportWriter;
        this.targetViewLineageService = targetViewLineageService;
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
                     compareConfig.getOutput().getSqlTableName());
             SummaryExcelReportWriter.SummaryExcelReportSession summaryExcelSession = summaryExcelReportWriter.open(
                     Path.of(compareConfig.getOutput().getSummaryExcelPath()))) {

            for (DataSourceInfo sourceInfo : compareConfig.getSources()) {
                DatabaseMeta sourceDbMeta = metadataLoadService.loadSource(sourceInfo);

                for (SchemaMeta sourceSchema : sourceDbMeta.getSchemas().values()) {
                    if (!shouldProcessSchema(compareConfig, sourceSchema.getSchemaName())) {
                        continue;
                    }
                    summary.incrementSourceSchemaCount();

                    for (TableMeta sourceTable : sourceSchema.getTables().values()) {
                        if (!shouldProcessTable(compareConfig, sourceTable.getTableName())) {
                            continue;
                        }
                        summary.incrementSourceTableCount();

                        MappingService.TableTarget tableTarget = mappingService.findTargetTable(
                                sourceInfo.getSourceName(), sourceSchema.getSchemaName(), sourceTable.getTableName());
                        if (tableTarget == null) {
                            continue;
                        }

                        SchemaMeta targetSchema = targetMetadata.getSchemas().get(NameNormalizer.normalize(tableTarget.getSchemaName()));
                        TableMeta targetTable = targetSchema == null ? null : targetSchema.getTables().get(NameNormalizer.normalize(tableTarget.getTableName()));
                        TableComparisonResult comparisonResult = tableCompareService.compareDetailed(sourceInfo, sourceSchema.getSchemaName(), sourceTable,
                                tableTarget.getSchemaName(), targetTable, compareConfig.getOptions());

                        appendResult(summary, csvSession, excelSession, sqlSession, summaryExcelSession, comparisonResult);
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

        TargetViewLineageExcelWriter.TargetViewLineageExcelSession lineageExcelSession = shouldWriteTargetViewLineage(compareConfig)
                ? targetViewLineageExcelWriter.open(Path.of(compareConfig.getOutput().getTargetViewLineageExcelPath()))
                : null;

        try (CsvReportWriter.CsvReportSession csvSession = csvReportWriter.open(Path.of(compareConfig.getOutput().getCsvPath()));
             ExcelReportWriter.ExcelReportSession excelSession = excelReportWriter.open(Path.of(compareConfig.getOutput().getExcelPath()));
             SqlReportWriter.SqlReportSession sqlSession = sqlReportWriter.open(
                     Path.of(compareConfig.getOutput().getSqlPath()),
                     compareConfig.getOutput().getSqlTableName());
             SummaryExcelReportWriter.SummaryExcelReportSession summaryExcelSession = summaryExcelReportWriter.open(
                     Path.of(compareConfig.getOutput().getSummaryExcelPath()))) {

            for (int index = 0; index < compareTasks.size(); index++) {
                CompareTask task = compareTasks.get(index);
                log.info("Target-driven compare [{}/{}]: target view {}.{} -> source {}.{}",
                        index + 1,
                        compareTasks.size(),
                        task.getTargetViewSchema(),
                        task.getTargetView(),
                        task.getSourceSchema() == null ? "<AUTO>" : task.getSourceSchema(),
                        task.getSourceTable());
                DataSourceInfo sourceInfo = sourceIndex.get(NameNormalizer.normalize(task.getSourceDatabase()));
                if (sourceInfo == null) {
                    continue;
                }
                String schemaKey = NameNormalizer.normalize(task.getSourceDatabase()) + "::" + NameNormalizer.normalize(task.getSourceSchema());
                if (visitedSourceSchemas.add(schemaKey)) {
                    summary.incrementSourceSchemaCount();
                }
                summary.incrementSourceTableCount();

                List<TargetViewLineageEntry> lineageEntries = targetViewLineageService.loadLineage(
                        compareConfig.getTarget(),
                        task.getTargetViewSchema(),
                        task.getTargetView());

                SourceTableLoadResult sourceTableLoadResult = metadataLoadService.loadSourceTable(sourceInfo, task.getSourceSchema(), task.getSourceTable());
                SchemaMeta targetSchema = targetMetadata.getSchemas().get(NameNormalizer.normalize(task.getTargetViewSchema()));
                TableMeta targetView = targetSchema == null ? null : targetSchema.getTables().get(NameNormalizer.normalize(task.getTargetView()));
                String sourceSchemaName = sourceTableLoadResult.getSchemaName() != null
                        ? sourceTableLoadResult.getSchemaName()
                        : task.getSourceSchema();
                appendTargetViewLineage(lineageExcelSession, task, sourceSchemaName, lineageEntries);

                TableComparisonResult comparisonResult;
                if (sourceTableLoadResult.isAmbiguous()) {
                    comparisonResult = tableCompareService.buildSourceIssueResult(sourceInfo,
                            sourceTableLoadResult.getSchemaName(),
                            sourceTableLoadResult.getTableName(),
                            task.getTargetViewSchema(),
                            task.getTargetView(),
                            DiffType.SOURCE_TABLE_AMBIGUOUS,
                            sourceTableLoadResult.getMessage());
                } else {
                    comparisonResult = tableCompareService.compareDetailed(sourceInfo,
                            sourceSchemaName,
                            sourceTableLoadResult.getTableMeta(),
                            task.getTargetViewSchema(),
                            targetView,
                            compareConfig.getOptions());
                }
                applyTargetViewContext(comparisonResult, task, lineageEntries);
                appendResult(summary, csvSession, excelSession, sqlSession, summaryExcelSession, comparisonResult);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close report writers", e);
        } finally {
            if (lineageExcelSession != null) {
                try {
                    lineageExcelSession.close();
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to close target view lineage Excel report", e);
                }
            }
        }

        summaryReportWriter.write(Path.of(compareConfig.getOutput().getSummaryPath()), summary);
        return summary;
    }

    private boolean shouldWriteTargetViewLineage(CompareConfig compareConfig) {
        return compareConfig.getMode() == CompareMode.TARGET_DRIVEN
                && resolveTargetObjectType(compareConfig) == CompareObjectType.VIEW
                && compareConfig.getOutput().getTargetViewLineageExcelPath() != null
                && !compareConfig.getOutput().getTargetViewLineageExcelPath().isBlank();
    }

    private void appendTargetViewLineage(TargetViewLineageExcelWriter.TargetViewLineageExcelSession lineageExcelSession,
                                         CompareTask task,
                                         String sourceSchemaName,
                                         List<TargetViewLineageEntry> lineageEntries) {
        if (lineageExcelSession == null) {
            return;
        }
        List<TargetViewLineageReportRow> rows = new ArrayList<>();
        if (lineageEntries.isEmpty()) {
            rows.add(new TargetViewLineageReportRow(
                    task.getSourceDatabase(),
                    sourceSchemaName,
                    task.getSourceTable(),
                    "",
                    "",
                    task.getTargetView(),
                    task.getTargetViewSchema()));
        } else {
            for (TargetViewLineageEntry lineageEntry : lineageEntries) {
                rows.add(new TargetViewLineageReportRow(
                        task.getSourceDatabase(),
                        sourceSchemaName,
                        task.getSourceTable(),
                        lineageEntry.getTargetTableSchema(),
                        lineageEntry.getTargetTableName(),
                        task.getTargetView(),
                        task.getTargetViewSchema()));
            }
        }
        lineageExcelSession.append(rows);
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

    private void applyTargetViewContext(TableComparisonResult comparisonResult,
                                        CompareTask task,
                                        List<TargetViewLineageEntry> lineageEntries) {
        String targetTableSchemas = joinUnique(lineageEntries.stream()
                .map(TargetViewLineageEntry::getTargetTableSchema)
                .collect(Collectors.toList()));
        String targetTables = joinUnique(lineageEntries.stream()
                .map(TargetViewLineageEntry::getTargetTableName)
                .collect(Collectors.toList()));

        comparisonResult.getDiffs().forEach(diff -> {
            diff.setTargetViewSchemaName(task.getTargetViewSchema());
            diff.setTargetViewName(task.getTargetView());
            diff.setTargetLineageTableSchemaName(targetTableSchemas);
            diff.setTargetLineageTableName(targetTables);
        });

        comparisonResult.getColumnRecords().forEach(record -> {
            record.setTargetViewSchemaName(task.getTargetViewSchema());
            record.setTargetViewName(task.getTargetView());
            record.setTargetLineageTableSchemaName(targetTableSchemas);
            record.setTargetLineageTableName(targetTables);
        });
    }

    private String joinUnique(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .collect(Collectors.joining("|"));
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
                              SummaryExcelReportWriter.SummaryExcelReportSession summaryExcelSession,
                              TableComparisonResult comparisonResult) {
        csvSession.append(comparisonResult.getDiffs());
        excelSession.append(comparisonResult.getColumnRecords());
        sqlSession.append(comparisonResult.getColumnRecords());
        summaryExcelSession.append(comparisonResult.getColumnRecords());
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
            if (value.equals(NameNormalizer.normalize(exclude))) {
                return false;
            }
        }
        if (includes.isEmpty()) {
            return true;
        }
        for (String include : includes) {
            if (value.equals(NameNormalizer.normalize(include))) {
                return true;
            }
        }
        return false;
    }
}
