package com.example.dbcompare.domain.model;

public class CompareTask {
    private String sourceDatabase;
    private String sourceSchema;
    private String sourceTable;
    private String targetViewSchema;
    private String targetView;
    private String targetTableSchema;
    private String targetTable;

    public CompareTask() {
    }

    public CompareTask(String sourceDatabase, String sourceSchema, String sourceTable, String targetViewSchema, String targetView) {
        this.sourceDatabase = sourceDatabase;
        this.sourceSchema = sourceSchema;
        this.sourceTable = sourceTable;
        this.targetViewSchema = targetViewSchema;
        this.targetView = targetView;
    }

    public String getSourceDatabase() { return sourceDatabase; }
    public void setSourceDatabase(String sourceDatabase) { this.sourceDatabase = sourceDatabase; }
    public String getSourceSchema() { return sourceSchema; }
    public void setSourceSchema(String sourceSchema) { this.sourceSchema = sourceSchema; }
    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
    public String getTargetViewSchema() { return targetViewSchema; }
    public void setTargetViewSchema(String targetViewSchema) { this.targetViewSchema = targetViewSchema; }
    public String getTargetView() { return targetView; }
    public void setTargetView(String targetView) { this.targetView = targetView; }
    public String getTargetTableSchema() { return targetTableSchema; }
    public void setTargetTableSchema(String targetTableSchema) { this.targetTableSchema = targetTableSchema; }
    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }

    public String getSourceDb() { return sourceDatabase; }
    public void setSourceDb(String sourceDb) { this.sourceDatabase = sourceDb; }
    public String getTableName() { return sourceTable; }
    public void setTableName(String tableName) { this.sourceTable = tableName; }
    public String getTargetSchema() { return targetViewSchema; }
    public void setTargetSchema(String targetSchema) { this.targetViewSchema = targetSchema; }
    public String getViewName() { return targetView; }
    public void setViewName(String viewName) { this.targetView = viewName; }
}
