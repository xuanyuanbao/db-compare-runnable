package com.example.dbcompare.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class TableMeta {
    private String tableName;
    private final Map<String, ColumnMeta> columns = new LinkedHashMap<>();

    public TableMeta() {
    }

    public TableMeta(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public Map<String, ColumnMeta> getColumns() { return columns; }
}
