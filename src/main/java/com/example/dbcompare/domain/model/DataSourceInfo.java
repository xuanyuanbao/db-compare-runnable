package com.example.dbcompare.domain.model;

import com.example.dbcompare.domain.enums.DatabaseType;

import java.util.ArrayList;
import java.util.List;

public class DataSourceInfo {
    private String sourceName;
    private DatabaseType type;
    private String jdbcUrl;
    private String username;
    private String password;
    private String driverClassName;
    private String driveClassName;
    private String catalog;
    private String schema;
    private boolean viewOnly;
    private String snapshotFile;
    private String viewLineageFile;
    private final List<String> includeSchemas = new ArrayList<>();
    private final List<String> excludeSchemas = new ArrayList<>();
    private final List<String> includeTables = new ArrayList<>();
    private final List<String> excludeTables = new ArrayList<>();

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public DatabaseType getType() { return type; }
    public void setType(DatabaseType type) { this.type = type; }
    public String getJdbcUrl() { return jdbcUrl; }
    public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDriverClassName() {
        return driverClassName == null || driverClassName.isBlank() ? driveClassName : driverClassName;
    }
    public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
    public String getDriveClassName() { return driveClassName; }
    public void setDriveClassName(String driveClassName) { this.driveClassName = driveClassName; }
    public String getCatalog() { return catalog; }
    public void setCatalog(String catalog) { this.catalog = catalog; }
    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
    public boolean isViewOnly() { return viewOnly; }
    public void setViewOnly(boolean viewOnly) { this.viewOnly = viewOnly; }
    public String getSnapshotFile() { return snapshotFile; }
    public void setSnapshotFile(String snapshotFile) { this.snapshotFile = snapshotFile; }
    public String getViewLineageFile() { return viewLineageFile; }
    public void setViewLineageFile(String viewLineageFile) { this.viewLineageFile = viewLineageFile; }
    public List<String> getIncludeSchemas() { return includeSchemas; }
    public List<String> getExcludeSchemas() { return excludeSchemas; }
    public List<String> getIncludeTables() { return includeTables; }
    public List<String> getExcludeTables() { return excludeTables; }
}
