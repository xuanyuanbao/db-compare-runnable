package com.example.dbcompare.config;

import com.example.dbcompare.domain.enums.CompareMode;
import com.example.dbcompare.domain.enums.CompareObjectType;
import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.domain.model.CompareConfig;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.SchemaMapping;
import com.example.dbcompare.domain.model.TableMapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigLoader {
    public CompareConfig load(Path path) {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config: " + path, e);
        }
        return parse(properties);
    }

    public CompareConfig parse(Properties properties) {
        CompareConfig config = new CompareConfig();
        config.setMode(enumValue(properties, CompareMode.class, CompareMode.FULL_SCAN,
                "compare.mode", "mode"));

        int sourceCount = intValue(properties, "source.count", 0);
        for (int i = 1; i <= sourceCount; i++) {
            config.getSources().add(loadDataSource(properties, "source." + i + "."));
        }

        config.setTarget(loadDataSource(properties, "target."));

        int mappingCount = intValue(properties, "mapping.count", 0);
        for (int i = 1; i <= mappingCount; i++) {
            SchemaMapping mapping = new SchemaMapping();
            mapping.setSourceDatabaseName(properties.getProperty("mapping." + i + ".sourceDatabaseName"));
            mapping.setTargetSchemaName(properties.getProperty("mapping." + i + ".targetSchemaName"));
            config.getMappings().add(mapping);
        }

        int tableMappingCount = intValue(properties, "tableMapping.count", 0);
        for (int i = 1; i <= tableMappingCount; i++) {
            TableMapping mapping = new TableMapping();
            mapping.setSourceDatabaseName(properties.getProperty("tableMapping." + i + ".sourceDatabaseName"));
            mapping.setSourceSchemaName(properties.getProperty("tableMapping." + i + ".sourceSchemaName"));
            mapping.setSourceTableName(properties.getProperty("tableMapping." + i + ".sourceTableName"));
            mapping.setTargetSchemaName(properties.getProperty("tableMapping." + i + ".targetSchemaName"));
            mapping.setTargetTableName(properties.getProperty("tableMapping." + i + ".targetTableName"));
            config.getTableMappings().add(mapping);
        }

        splitToList(properties.getProperty("compare.includeSchemas"), config.getIncludeSchemas());
        splitToList(properties.getProperty("compare.excludeSchemas"), config.getExcludeSchemas());
        splitToList(properties.getProperty("compare.includeTables"), config.getIncludeTables());
        splitToList(properties.getProperty("compare.excludeTables"), config.getExcludeTables());

        config.getOptions().setCompareNullable(boolValue(properties, "compare.options.compareNullable", true));
        config.getOptions().setCompareDefaultValue(boolValue(properties, "compare.options.compareDefaultValue", true));
        config.getOptions().setCompareLength(boolValue(properties, "compare.options.compareLength", true));
        config.getOptions().setSourceLoadThreads(intValue(properties, "compare.options.sourceLoadThreads", 4));
        config.getOptions().setObjectType(enumValue(properties, CompareObjectType.class, CompareObjectType.TABLE,
                "compare.options.objectType", "compare.options.object-type"));

        String csvPath = properties.getProperty("output.csvPath");
        if (csvPath != null && !csvPath.isBlank()) config.getOutput().setCsvPath(csvPath.trim());
        String excelPath = properties.getProperty("output.excelPath");
        if (excelPath != null && !excelPath.isBlank()) config.getOutput().setExcelPath(excelPath.trim());
        String summaryExcelPath = firstNonBlank(properties.getProperty("output.summaryExcelPath"), properties.getProperty("output.summary-excel-path"));
        if (summaryExcelPath != null) config.getOutput().setSummaryExcelPath(summaryExcelPath);
        String sqlPath = properties.getProperty("output.sqlPath");
        if (sqlPath != null && !sqlPath.isBlank()) config.getOutput().setSqlPath(sqlPath.trim());
        String sqlTableName = properties.getProperty("output.sqlTableName");
        if (sqlTableName != null && !sqlTableName.isBlank()) config.getOutput().setSqlTableName(sqlTableName.trim());
        String summaryPath = properties.getProperty("output.summaryPath");
        if (summaryPath != null && !summaryPath.isBlank()) config.getOutput().setSummaryPath(summaryPath.trim());

        return config;
    }

    private DataSourceInfo loadDataSource(Properties properties, String prefix) {
        DataSourceInfo dataSourceInfo = new DataSourceInfo();
        dataSourceInfo.setSourceName(properties.getProperty(prefix + "name"));
        String typeValue = properties.getProperty(prefix + "type");
        if (typeValue != null && !typeValue.isBlank()) {
            dataSourceInfo.setType(DatabaseType.valueOf(typeValue.trim().toUpperCase()));
        }
        dataSourceInfo.setJdbcUrl(properties.getProperty(prefix + "jdbcUrl"));
        dataSourceInfo.setUsername(properties.getProperty(prefix + "username"));
        dataSourceInfo.setPassword(properties.getProperty(prefix + "password"));
        String driverClassName = firstNonBlank(
                properties.getProperty(prefix + "driverClassName"),
                properties.getProperty(prefix + "driveClassName"));
        dataSourceInfo.setDriverClassName(driverClassName);
        dataSourceInfo.setCatalog(properties.getProperty(prefix + "catalog"));
        dataSourceInfo.setSchema(properties.getProperty(prefix + "schema"));
        dataSourceInfo.setViewOnly(boolValue(properties, prefix + "viewOnly", false));
        dataSourceInfo.setSnapshotFile(properties.getProperty(prefix + "snapshotFile"));
        dataSourceInfo.setViewLineageFile(properties.getProperty(prefix + "viewLineageFile"));
        splitToList(properties.getProperty(prefix + "includeSchemas"), dataSourceInfo.getIncludeSchemas());
        splitToList(properties.getProperty(prefix + "excludeSchemas"), dataSourceInfo.getExcludeSchemas());
        splitToList(properties.getProperty(prefix + "includeTables"), dataSourceInfo.getIncludeTables());
        splitToList(properties.getProperty(prefix + "excludeTables"), dataSourceInfo.getExcludeTables());
        return dataSourceInfo;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private void splitToList(String raw, java.util.List<String> out) {
        if (raw == null || raw.isBlank()) return;
        for (String part : raw.split(",")) {
            String value = part.trim();
            if (!value.isEmpty()) out.add(value);
        }
    }

    private boolean boolValue(Properties properties, String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private int intValue(Properties properties, String key, int defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
    }

    private <T extends Enum<T>> T enumValue(Properties properties, Class<T> type, T defaultValue, String... keys) {
        for (String key : keys) {
            String value = properties.getProperty(key);
            if (value != null && !value.isBlank()) {
                return Enum.valueOf(type, value.trim().toUpperCase());
            }
        }
        return defaultValue;
    }
}
