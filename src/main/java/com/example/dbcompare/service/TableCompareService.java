package com.example.dbcompare.service;

import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.domain.enums.DiffType;
import com.example.dbcompare.domain.model.ColumnMeta;
import com.example.dbcompare.domain.model.CompareOptions;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.DiffRecord;
import com.example.dbcompare.domain.model.TableMeta;
import com.example.dbcompare.util.DefaultValueNormalizer;
import com.example.dbcompare.util.TypeNormalizer;

import java.util.*;

public class TableCompareService {
    private final TypeNormalizer typeNormalizer = new TypeNormalizer();

    public List<DiffRecord> compare(DataSourceInfo sourceInfo,
                                    String sourceSchema,
                                    TableMeta sourceTable,
                                    String targetSchema,
                                    TableMeta targetTable,
                                    CompareOptions options) {
        List<DiffRecord> diffs = new ArrayList<>();
        if (sourceTable == null) {
            diffs.add(buildTableDiff(sourceInfo.getSourceName(), sourceSchema, null, targetSchema,
                    targetTable == null ? null : targetTable.getTableName(), DiffType.SOURCE_TABLE_NOT_FOUND,
                    null, null, "源表不存在"));
            return diffs;
        }
        if (targetTable == null) {
            diffs.add(buildTableDiff(sourceInfo.getSourceName(), sourceSchema, sourceTable.getTableName(), targetSchema,
                    sourceTable.getTableName(), DiffType.TARGET_TABLE_NOT_FOUND,
                    "存在", "不存在", "高斯目标表不存在"));
            return diffs;
        }
        compareColumnExistence(sourceInfo, sourceSchema, sourceTable, targetSchema, targetTable, diffs);
        compareColumnAttributes(sourceInfo, sourceSchema, sourceTable, targetSchema, targetTable, diffs, options);
        return diffs;
    }

    private void compareColumnExistence(DataSourceInfo sourceInfo, String sourceSchema, TableMeta sourceTable,
                                        String targetSchema, TableMeta targetTable, List<DiffRecord> diffs) {
        Set<String> sourceColumns = new HashSet<>(sourceTable.getColumns().keySet());
        Set<String> targetColumns = new HashSet<>(targetTable.getColumns().keySet());

        Set<String> onlyInSource = new HashSet<>(sourceColumns);
        onlyInSource.removeAll(targetColumns);
        for (String column : onlyInSource) {
            diffs.add(buildColumnDiff(sourceInfo.getSourceName(), sourceSchema, sourceTable.getTableName(), targetSchema,
                    targetTable.getTableName(), column, DiffType.COLUMN_MISSING_IN_TARGET,
                    "存在", "不存在", "源库存在但高斯不存在该字段"));
        }

        Set<String> onlyInTarget = new HashSet<>(targetColumns);
        onlyInTarget.removeAll(sourceColumns);
        for (String column : onlyInTarget) {
            diffs.add(buildColumnDiff(sourceInfo.getSourceName(), sourceSchema, sourceTable.getTableName(), targetSchema,
                    targetTable.getTableName(), column, DiffType.COLUMN_MISSING_IN_SOURCE,
                    "不存在", "存在", "高斯存在但源库不存在该字段"));
        }
    }

    private void compareColumnAttributes(DataSourceInfo sourceInfo, String sourceSchema, TableMeta sourceTable,
                                         String targetSchema, TableMeta targetTable, List<DiffRecord> diffs,
                                         CompareOptions options) {
        for (Map.Entry<String, ColumnMeta> entry : sourceTable.getColumns().entrySet()) {
            String columnName = entry.getKey();
            ColumnMeta sourceColumn = entry.getValue();
            ColumnMeta targetColumn = targetTable.getColumns().get(columnName);
            if (targetColumn == null) continue;

            compareType(sourceInfo, sourceSchema, sourceTable, targetSchema, targetTable, diffs, columnName, sourceColumn, targetColumn);
            if (options.isCompareLength()) compareLength(sourceInfo, sourceSchema, sourceTable, targetSchema, targetTable, diffs, columnName, sourceColumn, targetColumn);
            if (options.isCompareDefaultValue()) compareDefaultValue(sourceInfo, sourceSchema, sourceTable, targetSchema, targetTable, diffs, columnName, sourceColumn, targetColumn);
            if (options.isCompareNullable()) compareNullable(sourceInfo, sourceSchema, sourceTable, targetSchema, targetTable, diffs, columnName, sourceColumn, targetColumn);
        }
    }

    private void compareType(DataSourceInfo sourceInfo, String sourceSchema, TableMeta sourceTable, String targetSchema,
                             TableMeta targetTable, List<DiffRecord> diffs, String columnName,
                             ColumnMeta sourceColumn, ColumnMeta targetColumn) {
        String sourceType = typeNormalizer.normalize(sourceInfo.getType(), sourceColumn.getDataType());
        String targetType = typeNormalizer.normalize(DatabaseType.GAUSS, targetColumn.getDataType());
        if (!Objects.equals(sourceType, targetType)) {
            diffs.add(buildColumnDiff(sourceInfo.getSourceName(), sourceSchema, sourceTable.getTableName(), targetSchema,
                    targetTable.getTableName(), columnName, DiffType.COLUMN_TYPE_MISMATCH,
                    sourceType, targetType, "字段类型不一致"));
        }
    }

    private void compareLength(DataSourceInfo sourceInfo, String sourceSchema, TableMeta sourceTable, String targetSchema,
                               TableMeta targetTable, List<DiffRecord> diffs, String columnName,
                               ColumnMeta sourceColumn, ColumnMeta targetColumn) {
        String sourceLength = normalizeLength(sourceColumn.getLength());
        String targetLength = normalizeLength(targetColumn.getLength());
        if (!Objects.equals(sourceLength, targetLength)) {
            diffs.add(buildColumnDiff(sourceInfo.getSourceName(), sourceSchema, sourceTable.getTableName(), targetSchema,
                    targetTable.getTableName(), columnName, DiffType.COLUMN_LENGTH_MISMATCH,
                    sourceLength, targetLength, "字段长度不一致"));
        }
    }

    private void compareDefaultValue(DataSourceInfo sourceInfo, String sourceSchema, TableMeta sourceTable, String targetSchema,
                                     TableMeta targetTable, List<DiffRecord> diffs, String columnName,
                                     ColumnMeta sourceColumn, ColumnMeta targetColumn) {
        String sourceDefault = DefaultValueNormalizer.normalize(sourceColumn.getDefaultValue());
        String targetDefault = DefaultValueNormalizer.normalize(targetColumn.getDefaultValue());
        if (!Objects.equals(sourceDefault, targetDefault)) {
            diffs.add(buildColumnDiff(sourceInfo.getSourceName(), sourceSchema, sourceTable.getTableName(), targetSchema,
                    targetTable.getTableName(), columnName, DiffType.COLUMN_DEFAULT_MISMATCH,
                    sourceDefault, targetDefault, "字段默认值不一致"));
        }
    }

    private void compareNullable(DataSourceInfo sourceInfo, String sourceSchema, TableMeta sourceTable, String targetSchema,
                                 TableMeta targetTable, List<DiffRecord> diffs, String columnName,
                                 ColumnMeta sourceColumn, ColumnMeta targetColumn) {
        String sourceNullable = normalizeNullable(sourceColumn.getNullable());
        String targetNullable = normalizeNullable(targetColumn.getNullable());
        if (!Objects.equals(sourceNullable, targetNullable)) {
            diffs.add(buildColumnDiff(sourceInfo.getSourceName(), sourceSchema, sourceTable.getTableName(), targetSchema,
                    targetTable.getTableName(), columnName, DiffType.COLUMN_NULLABLE_MISMATCH,
                    sourceNullable, targetNullable, "字段可空属性不一致"));
        }
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
}
