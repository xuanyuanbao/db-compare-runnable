package com.example.dbcompare.service;

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
import com.example.dbcompare.util.DefaultValueNormalizer;
import com.example.dbcompare.util.TypeNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TableCompareService {
    private static final String MATCH_MESSAGE = "MATCH";

    private final TypeNormalizer typeNormalizer = new TypeNormalizer();

    public List<DiffRecord> compare(DataSourceInfo sourceInfo,
                                    String sourceSchema,
                                    TableMeta sourceTable,
                                    String targetSchema,
                                    TableMeta targetTable,
                                    CompareOptions options) {
        return compareDetailed(sourceInfo, sourceSchema, sourceTable, targetSchema, targetTable, options).getDiffs();
    }

    public TableComparisonResult compareDetailed(DataSourceInfo sourceInfo,
                                                 String sourceSchema,
                                                 TableMeta sourceTable,
                                                 String targetSchema,
                                                 TableMeta targetTable,
                                                 CompareOptions options) {
        TableComparisonResult result = new TableComparisonResult();
        if (sourceTable == null) {
            result.addDiff(buildTableDiff(sourceInfo.getSourceName(), sourceSchema, null, targetSchema,
                    targetTable == null ? null : targetTable.getTableName(), DiffType.SOURCE_TABLE_NOT_FOUND,
                    null, null, "Source table not found"));
            result.getColumnRecords().add(buildTableMissingRow(sourceInfo, sourceSchema, null, targetSchema,
                    targetTable == null ? null : targetTable.getTableName(), false, targetTable != null,
                    DiffType.SOURCE_TABLE_NOT_FOUND, "Source table not found", DiffGroup.MAIN, true, options));
            return result;
        }
        if (targetTable == null) {
            result.addDiff(buildTableDiff(sourceInfo.getSourceName(), sourceSchema, sourceTable.getTableName(), targetSchema,
                    sourceTable.getTableName(), DiffType.TARGET_TABLE_NOT_FOUND,
                    "PRESENT", "MISSING", "Target table not found"));
            appendMissingTargetTableRows(result, sourceInfo, sourceSchema, sourceTable, targetSchema);
            return result;
        }

        if (options.getRelationMode() == CompareRelationMode.TABLE_TO_VIEW) {
            compareTableToView(result, sourceInfo, sourceSchema, sourceTable, targetSchema, targetTable, options);
        } else {
            compareTableToTable(result, sourceInfo, sourceSchema, sourceTable, targetSchema, targetTable, options);
        }
        return result;
    }

    public TableComparisonResult buildSourceIssueResult(DataSourceInfo sourceInfo,
                                                        String sourceSchema,
                                                        String sourceTableName,
                                                        String targetSchema,
                                                        String targetTableName,
                                                        DiffType diffType,
                                                        String message) {
        TableComparisonResult result = new TableComparisonResult();
        result.addDiff(buildTableDiff(sourceInfo.getSourceName(), sourceSchema, sourceTableName, targetSchema,
                targetTableName, diffType, null, null, message));
        result.getColumnRecords().add(buildTableMissingRow(sourceInfo, sourceSchema, sourceTableName, targetSchema,
                targetTableName, false, true, diffType, message, DiffGroup.MAIN, true, new CompareOptions()));
        return result;
    }

    private void compareTableToTable(TableComparisonResult result,
                                     DataSourceInfo sourceInfo,
                                     String sourceSchema,
                                     TableMeta sourceTable,
                                     String targetSchema,
                                     TableMeta targetTable,
                                     CompareOptions options) {
        for (String columnName : mergeColumns(sourceTable, targetTable)) {
            ColumnMeta sourceColumn = sourceTable.getColumns().get(columnName);
            ColumnMeta targetColumn = targetTable.getColumns().get(columnName);

            if (sourceColumn == null) {
                appendMissingColumnDiff(result, sourceInfo, sourceSchema, sourceTable.getTableName(), targetSchema,
                        targetTable.getTableName(), columnName, sourceColumn, targetColumn,
                        false, true, DiffType.COLUMN_MISSING_IN_SOURCE, "Column exists only in target",
                        DiffGroup.MAIN, true, options);
                continue;
            }

            if (targetColumn == null) {
                appendMissingColumnDiff(result, sourceInfo, sourceSchema, sourceTable.getTableName(), targetSchema,
                        targetTable.getTableName(), columnName, sourceColumn, null,
                        true, false, DiffType.COLUMN_MISSING_IN_TARGET, "Column exists only in source",
                        DiffGroup.MAIN, true, options);
                continue;
            }

            result.getColumnRecords().add(compareExistingColumn(result, sourceInfo, sourceSchema, sourceTable.getTableName(),
                    targetSchema, targetTable.getTableName(), columnName, sourceColumn, targetColumn, options));
        }
    }

    private void compareTableToView(TableComparisonResult result,
                                    DataSourceInfo sourceInfo,
                                    String sourceSchema,
                                    TableMeta sourceTable,
                                    String targetSchema,
                                    TableMeta targetView,
                                    CompareOptions options) {
        for (String columnName : targetView.getColumns().keySet()) {
            ColumnMeta sourceColumn = sourceTable.getColumns().get(columnName);
            ColumnMeta targetColumn = targetView.getColumns().get(columnName);

            if (sourceColumn == null) {
                appendMissingColumnDiff(result, sourceInfo, sourceSchema, sourceTable.getTableName(), targetSchema,
                        targetView.getTableName(), columnName, null, targetColumn,
                        false, true, DiffType.COLUMN_MISSING_IN_SOURCE, "Column exists only in target",
                        DiffGroup.MAIN, true, options);
                continue;
            }

            result.getColumnRecords().add(compareExistingColumn(result, sourceInfo, sourceSchema, sourceTable.getTableName(),
                    targetSchema, targetView.getTableName(), columnName, sourceColumn, targetColumn, options));
        }

        if (!options.isCompareExists()) {
            return;
        }

        for (String columnName : sourceTable.getColumns().keySet()) {
            if (targetView.getColumns().containsKey(columnName)) {
                continue;
            }
            appendMissingColumnDiff(result, sourceInfo, sourceSchema, sourceTable.getTableName(), targetSchema,
                    targetView.getTableName(), columnName, sourceTable.getColumns().get(columnName), null,
                    true, false, DiffType.VIEW_MISSING_COLUMN_INFO, "View does not expose source column",
                    DiffGroup.INFO, false, options);
        }
    }

    private void appendMissingTargetTableRows(TableComparisonResult result,
                                              DataSourceInfo sourceInfo,
                                              String sourceSchema,
                                              TableMeta sourceTable,
                                              String targetSchema) {
        if (sourceTable.getColumns().isEmpty()) {
            result.getColumnRecords().add(buildTableMissingRow(sourceInfo, sourceSchema, sourceTable.getTableName(), targetSchema,
                    sourceTable.getTableName(), true, false, DiffType.TARGET_TABLE_NOT_FOUND, "Target table not found",
                    DiffGroup.MAIN, true, new CompareOptions()));
            return;
        }
        for (ColumnMeta sourceColumn : sourceTable.getColumns().values()) {
            result.getColumnRecords().add(buildMissingColumnRow(sourceInfo, sourceSchema, sourceTable.getTableName(),
                    targetSchema, sourceTable.getTableName(), sourceColumn.getColumnName(), sourceColumn, null,
                    true, false, DiffType.TARGET_TABLE_NOT_FOUND, "Target table not found", DiffGroup.MAIN, true, new CompareOptions()));
        }
    }

    private void appendMissingColumnDiff(TableComparisonResult result,
                                         DataSourceInfo sourceInfo,
                                         String sourceSchema,
                                         String sourceTableName,
                                         String targetSchema,
                                         String targetTableName,
                                         String columnName,
                                         ColumnMeta sourceColumn,
                                         ColumnMeta targetColumn,
                                         boolean sourceExists,
                                         boolean targetExists,
                                         DiffType diffType,
                                         String message,
                                         DiffGroup diffGroup,
                                         boolean affectsResult,
                                         CompareOptions options) {
        result.addDiff(buildColumnDiff(sourceInfo.getSourceName(), sourceSchema, sourceTableName, targetSchema,
                targetTableName, columnName, diffType,
                sourceExists ? "PRESENT" : "MISSING",
                targetExists ? "PRESENT" : "MISSING",
                message,
                diffGroup));
        result.getColumnRecords().add(buildMissingColumnRow(sourceInfo, sourceSchema, sourceTableName,
                targetSchema, targetTableName, columnName, sourceColumn, targetColumn,
                sourceExists, targetExists, diffType, message, diffGroup, affectsResult, options));
    }

    private ColumnComparisonRecord compareExistingColumn(TableComparisonResult result,
                                                         DataSourceInfo sourceInfo,
                                                         String sourceSchema,
                                                         String sourceTableName,
                                                         String targetSchema,
                                                         String targetTableName,
                                                         String columnName,
                                                         ColumnMeta sourceColumn,
                                                         ColumnMeta targetColumn,
                                                         CompareOptions options) {
        ColumnComparisonRecord record = buildBaseRow(sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName);
        record.setSourceColumnExists(true);
        record.setTargetColumnExists(true);

        List<String> messages = new ArrayList<>();
        List<String> diffTypes = new ArrayList<>();
        DiffGroup diffGroup = DiffGroup.MAIN;

        String sourceType = normalizeType(sourceInfo.getType(), sourceColumn, options);
        String targetType = normalizeType(DatabaseType.GAUSS, targetColumn, options);
        record.setSourceType(sourceType);
        record.setTargetType(targetType);
        applyOptionalAttributeComparison(record::setTypeStatus, options.isCompareType(), Objects.equals(sourceType, targetType),
                DiffType.COLUMN_TYPE_MISMATCH, "Type mismatch", diffTypes, messages, result,
                sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName,
                sourceType, targetType, diffGroup);

        String sourceLength = normalizeLength(sourceColumn.getLength());
        String targetLength = normalizeLength(targetColumn.getLength());
        record.setSourceLength(sourceLength);
        record.setTargetLength(targetLength);
        applyOptionalAttributeComparison(record::setLengthStatus, options.isCompareLength(), Objects.equals(sourceLength, targetLength),
                DiffType.COLUMN_LENGTH_MISMATCH, "Length mismatch", diffTypes, messages, result,
                sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName,
                sourceLength, targetLength, diffGroup);

        String sourceDefault = DefaultValueNormalizer.normalize(sourceColumn.getDefaultValue());
        String targetDefault = DefaultValueNormalizer.normalize(targetColumn.getDefaultValue());
        record.setSourceDefaultValue(sourceDefault);
        record.setTargetDefaultValue(targetDefault);
        applyOptionalAttributeComparison(record::setDefaultStatus, options.isCompareDefaultValue(), Objects.equals(sourceDefault, targetDefault),
                DiffType.COLUMN_DEFAULT_MISMATCH, "Default value mismatch", diffTypes, messages, result,
                sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName,
                sourceDefault, targetDefault, diffGroup);

        String sourceNullable = normalizeNullable(sourceColumn.getNullable());
        String targetNullable = normalizeNullable(targetColumn.getNullable());
        record.setSourceNullable(sourceNullable);
        record.setTargetNullable(targetNullable);
        applyOptionalAttributeComparison(record::setNullableStatus, options.isCompareNullable(), Objects.equals(sourceNullable, targetNullable),
                DiffType.COLUMN_NULLABLE_MISMATCH, "Nullable mismatch", diffTypes, messages, result,
                sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName,
                sourceNullable, targetNullable, diffGroup);

        if (diffTypes.isEmpty()) {
            record.setOverallStatus(ComparisonStatus.MATCH);
            record.setDiffTypes("");
            record.setMessage(MATCH_MESSAGE);
        } else {
            record.setOverallStatus(ComparisonStatus.MISMATCH);
            record.setDiffTypes(String.join("|", diffTypes));
            record.setMessage(String.join("; ", messages));
        }
        record.setDiffGroup(diffGroup);
        return record;
    }

    private void applyAttributeComparison(StatusConsumer statusConsumer,
                                          boolean matched,
                                          DiffType diffType,
                                          String message,
                                          List<String> diffTypes,
                                          List<String> messages,
                                          TableComparisonResult result,
                                          DataSourceInfo sourceInfo,
                                          String sourceSchema,
                                          String sourceTableName,
                                          String targetSchema,
                                          String targetTableName,
                                          String columnName,
                                          String sourceValue,
                                          String targetValue,
                                          DiffGroup diffGroup) {
        statusConsumer.accept(matched ? ComparisonStatus.MATCH : ComparisonStatus.MISMATCH);
        if (!matched) {
            diffTypes.add(diffType.name());
            messages.add(message);
            result.addDiff(buildColumnDiff(sourceInfo.getSourceName(), sourceSchema, sourceTableName, targetSchema,
                    targetTableName, columnName, diffType, sourceValue, targetValue, message, diffGroup));
        }
    }

    private void applyOptionalAttributeComparison(StatusConsumer statusConsumer,
                                                  boolean enabled,
                                                  boolean matched,
                                                  DiffType diffType,
                                                  String message,
                                                  List<String> diffTypes,
                                                  List<String> messages,
                                                  TableComparisonResult result,
                                                  DataSourceInfo sourceInfo,
                                                  String sourceSchema,
                                                  String sourceTableName,
                                                  String targetSchema,
                                                  String targetTableName,
                                                  String columnName,
                                                  String sourceValue,
                                                  String targetValue,
                                                  DiffGroup diffGroup) {
        if (!enabled) {
            statusConsumer.accept(ComparisonStatus.NOT_APPLICABLE);
            return;
        }
        applyAttributeComparison(statusConsumer, matched, diffType, message, diffTypes, messages, result,
                sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName, sourceValue, targetValue, diffGroup);
    }

    private ColumnComparisonRecord buildMissingColumnRow(DataSourceInfo sourceInfo,
                                                         String sourceSchema,
                                                         String sourceTableName,
                                                         String targetSchema,
                                                         String targetTableName,
                                                         String columnName,
                                                         ColumnMeta sourceColumn,
                                                         ColumnMeta targetColumn,
                                                         boolean sourceExists,
                                                         boolean targetExists,
                                                         DiffType diffType,
                                                         String message,
                                                         DiffGroup diffGroup,
                                                         boolean affectsResult,
                                                         CompareOptions options) {
        ColumnComparisonRecord record = buildBaseRow(sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName);
        record.setSourceColumnExists(sourceExists);
        record.setTargetColumnExists(targetExists);
        record.setSourceType(sourceColumn == null ? null : normalizeType(sourceInfo.getType(), sourceColumn, options));
        record.setTargetType(targetColumn == null ? null : normalizeType(DatabaseType.GAUSS, targetColumn, options));
        record.setSourceLength(sourceColumn == null ? null : normalizeLength(sourceColumn.getLength()));
        record.setTargetLength(targetColumn == null ? null : normalizeLength(targetColumn.getLength()));
        record.setSourceDefaultValue(sourceColumn == null ? null : DefaultValueNormalizer.normalize(sourceColumn.getDefaultValue()));
        record.setTargetDefaultValue(targetColumn == null ? null : DefaultValueNormalizer.normalize(targetColumn.getDefaultValue()));
        record.setSourceNullable(sourceColumn == null ? null : normalizeNullable(sourceColumn.getNullable()));
        record.setTargetNullable(targetColumn == null ? null : normalizeNullable(targetColumn.getNullable()));
        record.setTypeStatus(ComparisonStatus.NOT_APPLICABLE);
        record.setLengthStatus(ComparisonStatus.NOT_APPLICABLE);
        record.setDefaultStatus(ComparisonStatus.NOT_APPLICABLE);
        record.setNullableStatus(ComparisonStatus.NOT_APPLICABLE);
        record.setOverallStatus(affectsResult ? ComparisonStatus.MISMATCH : ComparisonStatus.MATCH);
        record.setDiffGroup(diffGroup);
        record.setDiffTypes(diffType.name());
        record.setMessage(message);
        return record;
    }

    private ColumnComparisonRecord buildTableMissingRow(DataSourceInfo sourceInfo,
                                                        String sourceSchema,
                                                        String sourceTableName,
                                                        String targetSchema,
                                                        String targetTableName,
                                                        boolean sourceExists,
                                                        boolean targetExists,
                                                        DiffType diffType,
                                                        String message,
                                                        DiffGroup diffGroup,
                                                        boolean affectsResult,
                                                        CompareOptions options) {
        return buildMissingColumnRow(sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName,
                "(TABLE)", null, null, sourceExists, targetExists, diffType, message, diffGroup, affectsResult, options);
    }

    private ColumnComparisonRecord buildBaseRow(DataSourceInfo sourceInfo,
                                                String sourceSchema,
                                                String sourceTableName,
                                                String targetSchema,
                                                String targetTableName,
                                                String columnName) {
        ColumnComparisonRecord record = new ColumnComparisonRecord();
        record.setSourceDatabaseName(sourceInfo.getSourceName());
        record.setSourceSchemaName(sourceSchema);
        record.setSourceTableName(sourceTableName);
        record.setTargetSchemaName(targetSchema);
        record.setTargetTableName(targetTableName);
        record.setColumnName(columnName);
        return record;
    }

    private List<String> mergeColumns(TableMeta sourceTable, TableMeta targetTable) {
        List<String> columns = new ArrayList<>();
        columns.addAll(sourceTable.getColumns().keySet());
        for (String targetColumn : targetTable.getColumns().keySet()) {
            if (!columns.contains(targetColumn)) {
                columns.add(targetColumn);
            }
        }
        return columns;
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase();
        if (normalized.equals("YES") || normalized.equals("Y") || normalized.equals("TRUE")) return "YES";
        if (normalized.equals("NO") || normalized.equals("N") || normalized.equals("FALSE")) return "NO";
        return normalized;
    }

    private String normalizeLength(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.endsWith(",0") ? normalized.substring(0, normalized.length() - 2) : normalized;
    }

    private String normalizeType(DatabaseType databaseType, ColumnMeta columnMeta, CompareOptions options) {
        return typeNormalizer.normalize(databaseType, columnMeta.getDataType(), options.getTypeMappings());
    }

    private DiffRecord buildTableDiff(String sourceDb, String sourceSchema, String sourceTable, String targetSchema,
                                      String targetTable, DiffType diffType, String sourceValue,
                                      String targetValue, String message) {
        return buildTableDiff(sourceDb, sourceSchema, sourceTable, targetSchema, targetTable, diffType, sourceValue, targetValue, message, DiffGroup.MAIN);
    }

    private DiffRecord buildTableDiff(String sourceDb, String sourceSchema, String sourceTable, String targetSchema,
                                      String targetTable, DiffType diffType, String sourceValue,
                                      String targetValue, String message, DiffGroup diffGroup) {
        DiffRecord record = new DiffRecord();
        record.setSourceDatabaseName(sourceDb);
        record.setSourceSchemaName(sourceSchema);
        record.setSourceTableName(sourceTable);
        record.setTargetSchemaName(targetSchema);
        record.setTargetTableName(targetTable);
        record.setDiffGroup(diffGroup);
        record.setDiffType(diffType);
        record.setSourceValue(sourceValue);
        record.setTargetValue(targetValue);
        record.setMessage(message);
        return record;
    }

    private DiffRecord buildColumnDiff(String sourceDb, String sourceSchema, String sourceTable, String targetSchema,
                                       String targetTable, String columnName, DiffType diffType,
                                       String sourceValue, String targetValue, String message) {
        return buildColumnDiff(sourceDb, sourceSchema, sourceTable, targetSchema, targetTable, columnName, diffType, sourceValue, targetValue, message, DiffGroup.MAIN);
    }

    private DiffRecord buildColumnDiff(String sourceDb, String sourceSchema, String sourceTable, String targetSchema,
                                       String targetTable, String columnName, DiffType diffType,
                                       String sourceValue, String targetValue, String message, DiffGroup diffGroup) {
        DiffRecord record = buildTableDiff(sourceDb, sourceSchema, sourceTable, targetSchema, targetTable, diffType, sourceValue, targetValue, message, diffGroup);
        record.setColumnName(columnName);
        return record;
    }

    @FunctionalInterface
    private interface StatusConsumer {
        void accept(ComparisonStatus status);
    }
}
