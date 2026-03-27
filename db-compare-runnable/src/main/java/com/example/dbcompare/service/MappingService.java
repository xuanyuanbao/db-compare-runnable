package com.example.dbcompare.service;

import com.example.dbcompare.domain.model.SchemaMapping;
import com.example.dbcompare.domain.model.TableMapping;
import com.example.dbcompare.util.NameNormalizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingService {
    private final Map<String, String> schemaIndex = new HashMap<>();
    private final Map<String, TableTarget> tableIndex = new HashMap<>();

    public MappingService(List<SchemaMapping> mappings, List<TableMapping> tableMappings) {
        for (SchemaMapping mapping : mappings) {
            schemaIndex.put(NameNormalizer.normalize(mapping.getSourceDatabaseName()),
                    NameNormalizer.normalize(mapping.getTargetSchemaName()));
        }
        for (TableMapping mapping : tableMappings) {
            tableIndex.put(buildTableKey(mapping.getSourceDatabaseName(), mapping.getSourceSchemaName(), mapping.getSourceTableName()),
                    new TableTarget(NameNormalizer.normalize(mapping.getTargetSchemaName()), NameNormalizer.normalize(mapping.getTargetTableName())));
        }
    }

    public String findTargetSchema(String sourceDatabaseName) {
        return schemaIndex.get(NameNormalizer.normalize(sourceDatabaseName));
    }

    public TableTarget findTargetTable(String sourceDatabaseName, String sourceSchemaName, String sourceTableName) {
        TableTarget exact = tableIndex.get(buildTableKey(sourceDatabaseName, sourceSchemaName, sourceTableName));
        if (exact != null) return exact;
        String targetSchema = findTargetSchema(sourceDatabaseName);
        return targetSchema == null ? null : new TableTarget(targetSchema, NameNormalizer.normalize(sourceTableName));
    }

    private String buildTableKey(String sourceDatabaseName, String sourceSchemaName, String sourceTableName) {
        return NameNormalizer.normalize(sourceDatabaseName) + "::" +
                NameNormalizer.normalize(sourceSchemaName) + "::" +
                NameNormalizer.normalize(sourceTableName);
    }

    public static class TableTarget {
        private final String schemaName;
        private final String tableName;

        public TableTarget(String schemaName, String tableName) {
            this.schemaName = schemaName;
            this.tableName = tableName;
        }

        public String getSchemaName() { return schemaName; }
        public String getTableName() { return tableName; }
    }
}
