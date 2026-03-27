package com.example.dbcompare.config;

import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.domain.model.CompareConfig;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.SchemaMapping;

import java.util.HashSet;
import java.util.Set;

public class ConfigValidator {
    public void validate(CompareConfig config) {
        if (config == null) throw new IllegalArgumentException("Compare config must not be null");
        if (config.getSources().isEmpty()) throw new IllegalArgumentException("At least one source datasource is required");
        if (config.getTarget() == null) throw new IllegalArgumentException("Target datasource is required");

        Set<String> sourceNames = new HashSet<>();
        for (DataSourceInfo source : config.getSources()) {
            validateDataSource(source, true);
            String normalized = normalize(source.getSourceName());
            if (!sourceNames.add(normalized)) {
                throw new IllegalArgumentException("Duplicate source name: " + source.getSourceName());
            }
        }
        validateDataSource(config.getTarget(), false);

        for (SchemaMapping mapping : config.getMappings()) {
            if (!sourceNames.contains(normalize(mapping.getSourceDatabaseName()))) {
                throw new IllegalArgumentException("Schema mapping points to unknown source: " + mapping.getSourceDatabaseName());
            }
            if (isBlank(mapping.getTargetSchemaName())) {
                throw new IllegalArgumentException("targetSchemaName must not be blank for source=" + mapping.getSourceDatabaseName());
            }
        }
    }

    private void validateDataSource(DataSourceInfo dataSourceInfo, boolean isSource) {
        if (dataSourceInfo == null) throw new IllegalArgumentException("Datasource must not be null");
        if (isBlank(dataSourceInfo.getSourceName())) throw new IllegalArgumentException("Datasource name must not be blank");
        if (dataSourceInfo.getType() == null) throw new IllegalArgumentException("Datasource type must not be null: " + dataSourceInfo.getSourceName());
        if (isSource && dataSourceInfo.getType() == DatabaseType.GAUSS) {
            throw new IllegalArgumentException("Source datasource cannot be GAUSS: " + dataSourceInfo.getSourceName());
        }
        if (dataSourceInfo.getType() == DatabaseType.SNAPSHOT) {
            if (isBlank(dataSourceInfo.getSnapshotFile())) {
                throw new IllegalArgumentException("snapshotFile must not be blank for SNAPSHOT datasource: " + dataSourceInfo.getSourceName());
            }
            return;
        }
        if (isBlank(dataSourceInfo.getJdbcUrl())) {
            throw new IllegalArgumentException("jdbcUrl must not be blank for datasource: " + dataSourceInfo.getSourceName());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
