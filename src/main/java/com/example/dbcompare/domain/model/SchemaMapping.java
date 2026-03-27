package com.example.dbcompare.domain.model;

public class SchemaMapping {
    private String sourceDatabaseName;
    private String targetSchemaName;

    public String getSourceDatabaseName() { return sourceDatabaseName; }
    public void setSourceDatabaseName(String sourceDatabaseName) { this.sourceDatabaseName = sourceDatabaseName; }
    public String getTargetSchemaName() { return targetSchemaName; }
    public void setTargetSchemaName(String targetSchemaName) { this.targetSchemaName = targetSchemaName; }
}
