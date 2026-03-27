package com.example.dbcompare.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class SchemaMeta {
    private String schemaName;
    private final Map<String, TableMeta> tables = new LinkedHashMap<>();

    public SchemaMeta() {
    }

    public SchemaMeta(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    public Map<String, TableMeta> getTables() { return tables; }
}
