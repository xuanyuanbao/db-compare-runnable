package com.example.dbcompare.infrastructure.reader;

import com.example.dbcompare.domain.enums.CompareObjectType;
import com.example.dbcompare.domain.model.ColumnMeta;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.DatabaseMeta;
import com.example.dbcompare.domain.model.SchemaMeta;
import com.example.dbcompare.domain.model.TableMeta;
import com.example.dbcompare.util.NameNormalizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
                String columnName = NameNormalizer.normalize(parts.get(3));

                SchemaMeta schemaMeta = databaseMeta.getSchemas().computeIfAbsent(schemaName, SchemaMeta::new);
                TableMeta tableMeta = schemaMeta.getTables().computeIfAbsent(tableName, TableMeta::new);
                ColumnMeta columnMeta = new ColumnMeta();
                columnMeta.setColumnName(columnName);
                columnMeta.setDataType(parts.get(4));
                columnMeta.setLength(emptyToNull(parts.get(5)));
                columnMeta.setNullable(emptyToNull(parts.get(6)));
                columnMeta.setDefaultValue(emptyToNull(parts.get(7)));
                columnMeta.setOrdinalPosition(parseInt(parts.get(8)));
                tableMeta.getColumns().put(columnName, columnMeta);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read snapshot file: " + path, e);
        }
        return databaseMeta;
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
