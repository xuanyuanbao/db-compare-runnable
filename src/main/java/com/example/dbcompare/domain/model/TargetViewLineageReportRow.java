package com.example.dbcompare.domain.model;

public class TargetViewLineageReportRow {
    private String sourceDatabase;
    private String sourceSchema;
    private String sourceTable;
    private String targetTableSchema;
    private String targetTable;
    private String targetView;
    private String targetViewSchema;

    public TargetViewLineageReportRow() {
    }

    public TargetViewLineageReportRow(String sourceDatabase,
                                      String sourceSchema,
                                      String sourceTable,
                                      String targetTableSchema,
                                      String targetTable,
                                      String targetView,
                                      String targetViewSchema) {
        this.sourceDatabase = sourceDatabase;
        this.sourceSchema = sourceSchema;
        this.sourceTable = sourceTable;
        this.targetTableSchema = targetTableSchema;
        this.targetTable = targetTable;
        this.targetView = targetView;
        this.targetViewSchema = targetViewSchema;
    }

    public String getSourceDatabase() {
        return sourceDatabase;
    }

    public void setSourceDatabase(String sourceDatabase) {
        this.sourceDatabase = sourceDatabase;
    }

    public String getSourceSchema() {
        return sourceSchema;
    }

    public void setSourceSchema(String sourceSchema) {
        this.sourceSchema = sourceSchema;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public String getTargetTableSchema() {
        return targetTableSchema;
    }

    public void setTargetTableSchema(String targetTableSchema) {
        this.targetTableSchema = targetTableSchema;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getTargetView() {
        return targetView;
    }

    public void setTargetView(String targetView) {
        this.targetView = targetView;
    }

    public String getTargetViewSchema() {
        return targetViewSchema;
    }

    public void setTargetViewSchema(String targetViewSchema) {
        this.targetViewSchema = targetViewSchema;
    }
}
