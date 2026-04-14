package com.example.dbcompare.domain.model;

import com.example.dbcompare.domain.enums.DiffType;

public class DiffRecord {
    private String sourceDatabaseName;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
    private String targetLineageTableSchemaName;
    private String targetLineageTableName;
    private String columnName;
    private DiffType diffType;
    private String sourceValue;
    private String targetValue;
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
    public DiffType getDiffType() { return diffType; }
    public void setDiffType(DiffType diffType) { this.diffType = diffType; }
    public String getSourceValue() { return sourceValue; }
    public void setSourceValue(String sourceValue) { this.sourceValue = sourceValue; }
    public String getTargetValue() { return targetValue; }
    public void setTargetValue(String targetValue) { this.targetValue = targetValue; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
