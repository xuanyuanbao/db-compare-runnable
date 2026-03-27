package com.example.dbcompare.domain.model;

import java.util.ArrayList;
import java.util.List;

public class CompareConfig {
    private final List<DataSourceInfo> sources = new ArrayList<>();
    private DataSourceInfo target;
    private final List<SchemaMapping> mappings = new ArrayList<>();
    private final List<TableMapping> tableMappings = new ArrayList<>();
    private final List<String> includeSchemas = new ArrayList<>();
    private final List<String> excludeSchemas = new ArrayList<>();
    private final List<String> includeTables = new ArrayList<>();
    private final List<String> excludeTables = new ArrayList<>();
    private final CompareOptions options = new CompareOptions();
    private final OutputConfig output = new OutputConfig();

    public List<DataSourceInfo> getSources() { return sources; }
    public DataSourceInfo getTarget() { return target; }
    public void setTarget(DataSourceInfo target) { this.target = target; }
    public List<SchemaMapping> getMappings() { return mappings; }
    public List<TableMapping> getTableMappings() { return tableMappings; }
    public List<String> getIncludeSchemas() { return includeSchemas; }
    public List<String> getExcludeSchemas() { return excludeSchemas; }
    public List<String> getIncludeTables() { return includeTables; }
    public List<String> getExcludeTables() { return excludeTables; }
    public CompareOptions getOptions() { return options; }
    public OutputConfig getOutput() { return output; }
}
