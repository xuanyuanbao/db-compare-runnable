package com.example.dbcompare.domain.model;

public class CompareTask {
    private String sourceDb;
    private String sourceSchema;
    private String tableName;
    private String targetSchema;
    private String viewName;

    public CompareTask() {
    }

    public CompareTask(String sourceDb, String sourceSchema, String tableName, String targetSchema, String viewName) {
        this.sourceDb = sourceDb;
        this.sourceSchema = sourceSchema;
        this.tableName = tableName;
        this.targetSchema = targetSchema;
        this.viewName = viewName;
    }

    public String getSourceDb() { return sourceDb; }
    public void setSourceDb(String sourceDb) { this.sourceDb = sourceDb; }
    public String getSourceSchema() { return sourceSchema; }
    public void setSourceSchema(String sourceSchema) { this.sourceSchema = sourceSchema; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getTargetSchema() { return targetSchema; }
    public void setTargetSchema(String targetSchema) { this.targetSchema = targetSchema; }
    public String getViewName() { return viewName; }
    public void setViewName(String viewName) { this.viewName = viewName; }
}