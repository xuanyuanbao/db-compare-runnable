package com.example.dbcompare.domain.model;

public class SourceTableLoadResult {
    private String schemaName;
    private String tableName;
    private TableMeta tableMeta;
    private boolean ambiguous;
    private String message;

    public static SourceTableLoadResult found(String schemaName, String tableName, TableMeta tableMeta) {
        SourceTableLoadResult result = new SourceTableLoadResult();
        result.schemaName = schemaName;
        result.tableName = tableName;
        result.tableMeta = tableMeta;
        return result;
    }

    public static SourceTableLoadResult missing(String schemaName, String tableName, String message) {
        SourceTableLoadResult result = new SourceTableLoadResult();
        result.schemaName = schemaName;
        result.tableName = tableName;
        result.message = message;
        return result;
    }

    public static SourceTableLoadResult ambiguous(String schemaName, String tableName, String message) {
        SourceTableLoadResult result = new SourceTableLoadResult();
        result.schemaName = schemaName;
        result.tableName = tableName;
        result.ambiguous = true;
        result.message = message;
        return result;
    }

    public String getSchemaName() { return schemaName; }
    public String getTableName() { return tableName; }
    public TableMeta getTableMeta() { return tableMeta; }
    public boolean isAmbiguous() { return ambiguous; }
    public String getMessage() { return message; }
    public boolean isFound() { return tableMeta != null && !ambiguous; }
}