package com.example.dbcompare.domain.model;

public class TargetViewLineageEntry {
    private String targetViewSchema;
    private String targetViewName;
    private String targetTableSchema;
    private String targetTableName;

    public TargetViewLineageEntry() {
    }

    public TargetViewLineageEntry(String targetViewSchema,
                                  String targetViewName,
                                  String targetTableSchema,
                                  String targetTableName) {
        this.targetViewSchema = targetViewSchema;
        this.targetViewName = targetViewName;
        this.targetTableSchema = targetTableSchema;
        this.targetTableName = targetTableName;
    }

    public String getTargetViewSchema() {
        return targetViewSchema;
    }

    public void setTargetViewSchema(String targetViewSchema) {
        this.targetViewSchema = targetViewSchema;
    }

    public String getTargetViewName() {
        return targetViewName;
    }

    public void setTargetViewName(String targetViewName) {
        this.targetViewName = targetViewName;
    }

    public String getTargetTableSchema() {
        return targetTableSchema;
    }

    public void setTargetTableSchema(String targetTableSchema) {
        this.targetTableSchema = targetTableSchema;
    }

    public String getTargetTableName() {
        return targetTableName;
    }

    public void setTargetTableName(String targetTableName) {
        this.targetTableName = targetTableName;
    }
}
