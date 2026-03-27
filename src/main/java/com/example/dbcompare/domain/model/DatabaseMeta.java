package com.example.dbcompare.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseMeta {
    private String databaseName;
    private final Map<String, SchemaMeta> schemas = new LinkedHashMap<>();

    public DatabaseMeta() {
    }

    public DatabaseMeta(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    public Map<String, SchemaMeta> getSchemas() { return schemas; }
}
