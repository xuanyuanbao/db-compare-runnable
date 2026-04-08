package com.example.dbcompare.service;

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
            result.getDiffs().add(buildTableDiff(sourceInfo.getSourceName(), sourceSchema, null, targetSchema,
                    targetTable == null ? null : targetTable.getTableName(), DiffType.SOURCE_TABLE_NOT_FOUND,
                    null, null, "Source table not found"));
            result.getColumnRecords().add(buildTableMissingRow(sourceInfo, sourceSchema, null, targetSchema,
                    targetTable == null ? null : targetTable.getTableName(), false, targetTable != null,
                    DiffType.SOURCE_TABLE_NOT_FOUND, "Source table not found"));
            return result;
        }
        if (targetTable == null) {
            result.getDiffs().add(buildTableDiff(sourceInfo.getSourceName(), sourceSchema, sourceTable.getTableName(), targetSchema,
                    sourceTable.getTableName(), DiffType.TARGET_TABLE_NOT_FOUND,
                    "PRESENT", "MISSING", "Target table not found"));
            appendMissingTargetTableRows(result, sourceInfo, sourceSchema, sourceTable, targetSchema);
            return result;
        }

        for (String columnName : mergeColumns(sourceTable, targetTable)) {
            ColumnMeta sourceColumn = sourceTable.getColumns().get(columnName);
            ColumnMeta targetColumn = targetTable.getColumns().get(columnName);

            if (sourceColumn == null) {
                result.getDiffs().add(buildColumnDiff(sourceInfo.getSourceName(), sourceSchema, sourceTable.getTableName(), targetSchema,
                        targetTable.getTableName(), columnName, DiffType.COLUMN_MISSING_IN_SOURCE,
                        "MISSING", "PRESENT", "Column exists only in target"));
                result.getColumnRecords().add(buildMissingColumnRow(sourceInfo, sourceSchema, sourceTable.getTableName(),
                        targetSchema, targetTable.getTableName(), columnName, sourceColumn, targetColumn,
                        false, true, DiffType.COLUMN_MISSING_IN_SOURCE, "Column exists only in target"));
                continue;
            }

            if (targetColumn == null) {
                result.getDiffs().add(buildColumnDiff(sourceInfo.getSourceName(), sourceSchema, sourceTable.getTableName(), targetSchema,
                        targetTable.getTableName(), columnName, DiffType.COLUMN_MISSING_IN_TARGET,
                        "PRESENT", "MISSING", "Column exists only in source"));
                result.getColumnRecords().add(buildMissingColumnRow(sourceInfo, sourceSchema, sourceTable.getTableName(),
                        targetSchema, targetTable.getTableName(), columnName, sourceColumn, null,
                        true, false, DiffType.COLUMN_MISSING_IN_TARGET, "Column exists only in source"));
                continue;
            }

            result.getColumnRecords().add(compareExistingColumn(result, sourceInfo, sourceSchema, sourceTable.getTableName(),
                    targetSchema, targetTable.getTableName(), columnName, sourceColumn, targetColumn, options));
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
        result.getDiffs().add(buildTableDiff(sourceInfo.getSourceName(), sourceSchema, sourceTableName, targetSchema,
                targetTableName, diffType, null, null, message));
        result.getColumnRecords().add(buildTableMissingRow(sourceInfo, sourceSchema, sourceTableName, targetSchema,
                targetTableName, false, true, diffType, message));
        return result;
    }

    private void appendMissingTargetTableRows(TableComparisonResult result,
                                              DataSourceInfo sourceInfo,
                                              String sourceSchema,
                                              TableMeta sourceTable,
                                              String targetSchema) {
        if (sourceTable.getColumns().isEmpty()) {
            result.getColumnRecords().add(buildTableMissingRow(sourceInfo, sourceSchema, sourceTable.getTableName(), targetSchema,
                    sourceTable.getTableName(), true, false, DiffType.TARGET_TABLE_NOT_FOUND, "Target table not found"));
            return;
        }
        for (ColumnMeta sourceColumn : sourceTable.getColumns().values()) {
            result.getColumnRecords().add(buildMissingColumnRow(sourceInfo, sourceSchema, sourceTable.getTableName(),
                    targetSchema, sourceTable.getTableName(), sourceColumn.getColumnName(), sourceColumn, null,
                    true, false, DiffType.TARGET_TABLE_NOT_FOUND, "Target table not found"));
        }
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

        String sourceType = normalizeType(sourceInfo.getType(), sourceColumn);
        String targetType = normalizeType(DatabaseType.GAUSS, targetColumn);
        record.setSourceType(sourceType);
        record.setTargetType(targetType);
        applyAttributeComparison(record::setTypeStatus, Objects.equals(sourceType, targetType),
                DiffType.COLUMN_TYPE_MISMATCH, "Type mismatch", diffTypes, messages, result,
                sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName,
                sourceType, targetType);

        String sourceLength = normalizeLength(sourceColumn.getLength());
        String targetLength = normalizeLength(targetColumn.getLength());
        record.setSourceLength(sourceLength);
        record.setTargetLength(targetLength);
        applyOptionalAttributeComparison(record::setLengthStatus, options.isCompareLength(), Objects.equals(sourceLength, targetLength),
                DiffType.COLUMN_LENGTH_MISMATCH, "Length mismatch", diffTypes, messages, result,
                sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName,
                sourceLength, targetLength);

        String sourceDefault = DefaultValueNormalizer.normalize(sourceColumn.getDefaultValue());
        String targetDefault = DefaultValueNormalizer.normalize(targetColumn.getDefaultValue());
        record.setSourceDefaultValue(sourceDefault);
        record.setTargetDefaultValue(targetDefault);
        applyOptionalAttributeComparison(record::setDefaultStatus, options.isCompareDefaultValue(), Objects.equals(sourceDefault, targetDefault),
                DiffType.COLUMN_DEFAULT_MISMATCH, "Default value mismatch", diffTypes, messages, result,
                sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName,
                sourceDefault, targetDefault);

        String sourceNullable = normalizeNullable(sourceColumn.getNullable());
        String targetNullable = normalizeNullable(targetColumn.getNullable());
        record.setSourceNullable(sourceNullable);
        record.setTargetNullable(targetNullable);
        applyOptionalAttributeComparison(record::setNullableStatus, options.isCompareNullable(), Objects.equals(sourceNullable, targetNullable),
                DiffType.COLUMN_NULLABLE_MISMATCH, "Nullable mismatch", diffTypes, messages, result,
                sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName,
                sourceNullable, targetNullable);

        if (diffTypes.isEmpty()) {
            record.setOverallStatus(ComparisonStatus.MATCH);
            record.setDiffTypes("");
            record.setMessage(MATCH_MESSAGE);
        } else {
            record.setOverallStatus(ComparisonStatus.MISMATCH);
            record.setDiffTypes(String.join("|", diffTypes));
            record.setMessage(String.join("; ", messages));
        }
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
                                          String targetValue) {
        statusConsumer.accept(matched ? ComparisonStatus.MATCH : ComparisonStatus.MISMATCH);
        if (!matched) {
            diffTypes.add(diffType.name());
            messages.add(message);
            result.getDiffs().add(buildColumnDiff(sourceInfo.getSourceName(), sourceSchema, sourceTableName, targetSchema,
                    targetTableName, columnName, diffType, sourceValue, targetValue, message));
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
                                                  String targetValue) {
        if (!enabled) {
            statusConsumer.accept(ComparisonStatus.NOT_APPLICABLE);
            return;
        }
        applyAttributeComparison(statusConsumer, matched, diffType, message, diffTypes, messages, result,
                sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName, sourceValue, targetValue);
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
                                                         String message) {
        ColumnComparisonRecord record = buildBaseRow(sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName, columnName);
        record.setSourceColumnExists(sourceExists);
        record.setTargetColumnExists(targetExists);
        record.setSourceType(sourceColumn == null ? null : normalizeType(sourceInfo.getType(), sourceColumn));
        record.setTargetType(targetColumn == null ? null : normalizeType(DatabaseType.GAUSS, targetColumn));
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
        record.setOverallStatus(ComparisonStatus.MISMATCH);
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
                                                        String message) {
        return buildMissingColumnRow(sourceInfo, sourceSchema, sourceTableName, targetSchema, targetTableName,
                "(TABLE)", null, null, sourceExists, targetExists, diffType, message);
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

    private String normalizeType(DatabaseType databaseType, ColumnMeta columnMeta) {
        return typeNormalizer.normalize(databaseType, columnMeta.getDataType());
    }

    private DiffRecord buildTableDiff(String sourceDb, String sourceSchema, String sourceTable, String targetSchema,
                                      String targetTable, DiffType diffType, String sourceValue,
                                      String targetValue, String message) {
        DiffRecord record = new DiffRecord();
        record.setSourceDatabaseName(sourceDb);
        record.setSourceSchemaName(sourceSchema);
        record.setSourceTableName(sourceTable);
        record.setTargetSchemaName(targetSchema);
        record.setTargetTableName(targetTable);
        record.setDiffType(diffType);
        record.setSourceValue(sourceValue);
        record.setTargetValue(targetValue);
        record.setMessage(message);
        return record;
    }

    private DiffRecord buildColumnDiff(String sourceDb, String sourceSchema, String sourceTable, String targetSchema,
                                       String targetTable, String columnName, DiffType diffType,
                                       String sourceValue, String targetValue, String message) {
        DiffRecord record = buildTableDiff(sourceDb, sourceSchema, sourceTable, targetSchema, targetTable, diffType, sourceValue, targetValue, message);
        record.setColumnName(columnName);
        return record;
    }

    @FunctionalInterface
    private interface StatusConsumer {
        void accept(ComparisonStatus status);
    }
}