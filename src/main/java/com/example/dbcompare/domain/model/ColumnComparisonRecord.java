package com.example.dbcompare.domain.model;

import com.example.dbcompare.domain.enums.ComparisonStatus;

public class ColumnComparisonRecord {
    private String sourceDatabaseName;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
    private String targetLineageTableSchemaName;
    private String targetLineageTableName;
    private String columnName;
    private boolean sourceColumnExists;
    private boolean targetColumnExists;
    private String sourceType;
    private String targetType;
    private ComparisonStatus typeStatus;
    private String sourceLength;
    private String targetLength;
    private ComparisonStatus lengthStatus;
    private String sourceDefaultValue;
    private String targetDefaultValue;
    private ComparisonStatus defaultStatus;
    private String sourceNullable;
    private String targetNullable;
    private ComparisonStatus nullableStatus;
    private ComparisonStatus overallStatus;
    private String diffTypes;
    private String message;

    public String getSourceDatabaseName() { return sourceDatabaseName; }
    public void setSourceDatabaseName(String sourceDatabaseName) { this.sourceDatabaseName = sourceDatabaseName; }
    public String getSourceSchemaName() { return sourceSchemaName; }
    public void setSourceSchemaName(String sourceSchemaName) { this.sourceSchemaName = sourceSchemaName; }
    public String getSourceTableName() { return sourceTableName; }
    public void setSourceTableName(String sourceTableName) { this.sourceTableName = sourceTableName; }
    public String getTargetSchemaName() { return targetSchemaName; }
    public void setTargetSchemaName(String targetSchemaName) { this.targetSchemaName = targetSchemaName; }
    public String getTargetTableName() { return targetTableName; }
    public void setTargetTableName(String targetTableName) { this.targetTableName = targetTableName; }
    public String getTargetViewSchemaName() { return targetSchemaName; }
    public void setTargetViewSchemaName(String targetViewSchemaName) { this.targetSchemaName = targetViewSchemaName; }
    public String getTargetViewName() { return targetTableName; }
    public void setTargetViewName(String targetViewName) { this.targetTableName = targetViewName; }
    public String getTargetLineageTableSchemaName() { return targetLineageTableSchemaName; }
    public void setTargetLineageTableSchemaName(String targetLineageTableSchemaName) { this.targetLineageTableSchemaName = targetLineageTableSchemaName; }
    public String getTargetLineageTableName() { return targetLineageTableName; }
    public void setTargetLineageTableName(String targetLineageTableName) { this.targetLineageTableName = targetLineageTableName; }
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public boolean isSourceColumnExists() { return sourceColumnExists; }
    public void setSourceColumnExists(boolean sourceColumnExists) { this.sourceColumnExists = sourceColumnExists; }
    public boolean isTargetColumnExists() { return targetColumnExists; }
    public void setTargetColumnExists(boolean targetColumnExists) { this.targetColumnExists = targetColumnExists; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public ComparisonStatus getTypeStatus() { return typeStatus; }
    public void setTypeStatus(ComparisonStatus typeStatus) { this.typeStatus = typeStatus; }
    public String getSourceLength() { return sourceLength; }
    public void setSourceLength(String sourceLength) { this.sourceLength = sourceLength; }
    public String getTargetLength() { return targetLength; }
    public void setTargetLength(String targetLength) { this.targetLength = targetLength; }
    public ComparisonStatus getLengthStatus() { return lengthStatus; }
    public void setLengthStatus(ComparisonStatus lengthStatus) { this.lengthStatus = lengthStatus; }
    public String getSourceDefaultValue() { return sourceDefaultValue; }
    public void setSourceDefaultValue(String sourceDefaultValue) { this.sourceDefaultValue = sourceDefaultValue; }
    public String getTargetDefaultValue() { return targetDefaultValue; }
    public void setTargetDefaultValue(String targetDefaultValue) { this.targetDefaultValue = targetDefaultValue; }
    public ComparisonStatus getDefaultStatus() { return defaultStatus; }
    public void setDefaultStatus(ComparisonStatus defaultStatus) { this.defaultStatus = defaultStatus; }
    public String getSourceNullable() { return sourceNullable; }
    public void setSourceNullable(String sourceNullable) { this.sourceNullable = sourceNullable; }
    public String getTargetNullable() { return targetNullable; }
    public void setTargetNullable(String targetNullable) { this.targetNullable = targetNullable; }
    public ComparisonStatus getNullableStatus() { return nullableStatus; }
    public void setNullableStatus(ComparisonStatus nullableStatus) { this.nullableStatus = nullableStatus; }
    public ComparisonStatus getOverallStatus() { return overallStatus; }
    public void setOverallStatus(ComparisonStatus overallStatus) { this.overallStatus = overallStatus; }
    public String getDiffTypes() { return diffTypes; }
    public void setDiffTypes(String diffTypes) { this.diffTypes = diffTypes; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
