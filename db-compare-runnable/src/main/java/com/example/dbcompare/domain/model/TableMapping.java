package com.example.dbcompare.domain.model;

public class TableMapping {
    private String sourceDatabaseName;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;

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
}
