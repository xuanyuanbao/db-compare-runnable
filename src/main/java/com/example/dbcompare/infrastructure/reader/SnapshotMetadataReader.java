package com.example.dbcompare.infrastructure.reader;

import com.example.dbcompare.domain.enums.CompareObjectType;
import com.example.dbcompare.domain.model.ColumnMeta;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.DatabaseMeta;
import com.example.dbcompare.domain.model.SchemaMeta;
import com.example.dbcompare.domain.model.SourceTableLoadResult;
import com.example.dbcompare.domain.model.TableMeta;
import com.example.dbcompare.util.NameNormalizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SnapshotMetadataReader implements MetadataReader {
    @Override
    public DatabaseMeta loadMetadata(DataSourceInfo dataSourceInfo, CompareObjectType objectType) {
        Path path = Path.of(dataSourceInfo.getSnapshotFile());
        DatabaseMeta databaseMeta = new DatabaseMeta(dataSourceInfo.getSourceName());
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                if (line.isBlank()) continue;
                List<String> parts = parseCsvLine(line);
                if (parts.size() < 9) {
                    throw new IllegalStateException("Snapshot row must contain 9 columns: " + line);
                }
                String schemaName = NameNormalizer.normalize(parts.get(1));
                String tableName = NameNormalizer.normalize(parts.get(2));
                if (!matchesSchema(dataSourceInfo, schemaName) || !matchesTable(dataSourceInfo, tableName)) {
                    continue;
                }
                appendColumn(databaseMeta, schemaName, tableName, parts);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read snapshot file: " + path, e);
        }
        return databaseMeta;
    }

    @Override
    public SourceTableLoadResult loadTableMetadata(DataSourceInfo dataSourceInfo, String schemaName, String tableName) {
        Path path = Path.of(dataSourceInfo.getSnapshotFile());
        String preferredSchema = preferredSchema(dataSourceInfo, schemaName);
        String normalizedTableName = NameNormalizer.normalize(tableName);
        Map<String, TableMeta> matches = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                if (line.isBlank()) continue;
                List<String> parts = parseCsvLine(line);
                if (parts.size() < 9) {
                    throw new IllegalStateException("Snapshot row must contain 9 columns: " + line);
                }
                String rowSchema = NameNormalizer.normalize(parts.get(1));
                String rowTable = NameNormalizer.normalize(parts.get(2));
                if (!normalizedTableName.equals(rowTable)) {
                    continue;
                }
                if (preferredSchema != null && !preferredSchema.equals(rowSchema)) {
                    continue;
                }
                if (!matchesSchema(dataSourceInfo, rowSchema) || !matchesTable(dataSourceInfo, rowTable)) {
                    continue;
                }
                TableMeta tableMeta = matches.computeIfAbsent(rowSchema, ignored -> new TableMeta(rowTable));
                appendColumn(tableMeta, parts);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read snapshot file: " + path, e);
        }
        if (matches.isEmpty()) {
            return SourceTableLoadResult.missing(preferredSchema, normalizedTableName, "Source table not found");
        }
        if (matches.size() > 1) {
            return SourceTableLoadResult.ambiguous(preferredSchema, normalizedTableName,
                    "Multiple source schemas matched the same table name: " + String.join(", ", matches.keySet()));
        }
        Map.Entry<String, TableMeta> entry = matches.entrySet().iterator().next();
        return SourceTableLoadResult.found(entry.getKey(), normalizedTableName, entry.getValue());
    }

    private void appendColumn(DatabaseMeta databaseMeta, String schemaName, String tableName, List<String> parts) {
        SchemaMeta schemaMeta = databaseMeta.getSchemas().computeIfAbsent(schemaName, SchemaMeta::new);
        TableMeta tableMeta = schemaMeta.getTables().computeIfAbsent(tableName, TableMeta::new);
        appendColumn(tableMeta, parts);
    }

    private void appendColumn(TableMeta tableMeta, List<String> parts) {
        ColumnMeta columnMeta = new ColumnMeta();
        String columnName = NameNormalizer.normalize(parts.get(3));
        columnMeta.setColumnName(columnName);
        columnMeta.setDataType(parts.get(4));
        columnMeta.setLength(emptyToNull(parts.get(5)));
        columnMeta.setNullable(emptyToNull(parts.get(6)));
        columnMeta.setDefaultValue(emptyToNull(parts.get(7)));
        columnMeta.setOrdinalPosition(parseInt(parts.get(8)));
        tableMeta.getColumns().put(columnName, columnMeta);
    }

    private boolean matchesSchema(DataSourceInfo dataSourceInfo, String schemaName) {
        String forcedSchema = NameNormalizer.normalize(dataSourceInfo.getSchema());
        if (forcedSchema != null && !forcedSchema.equals(schemaName)) {
            return false;
        }
        return matches(schemaName, dataSourceInfo.getIncludeSchemas(), dataSourceInfo.getExcludeSchemas());
    }

    private boolean matchesTable(DataSourceInfo dataSourceInfo, String tableName) {
        return matches(tableName, dataSourceInfo.getIncludeTables(), dataSourceInfo.getExcludeTables());
    }

    private boolean matches(String value, List<String> includes, List<String> excludes) {
        String normalized = NameNormalizer.normalize(value);
        for (String exclude : excludes) {
            if (normalized != null && normalized.equals(NameNormalizer.normalize(exclude))) return false;
        }
        if (includes.isEmpty()) return true;
        for (String include : includes) {
            if (normalized != null && normalized.equals(NameNormalizer.normalize(include))) return true;
        }
        return false;
    }

    private String preferredSchema(DataSourceInfo dataSourceInfo, String schemaName) {
        String preferredSchema = NameNormalizer.normalize(schemaName);
        if (preferredSchema == null) {
            preferredSchema = NameNormalizer.normalize(dataSourceInfo.getSchema());
        }
        if (preferredSchema == null && dataSourceInfo.getIncludeSchemas().size() == 1) {
            preferredSchema = NameNormalizer.normalize(dataSourceInfo.getIncludeSchemas().get(0));
        }
        return preferredSchema;
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) return null;
        return Integer.parseInt(value.trim());
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }
}