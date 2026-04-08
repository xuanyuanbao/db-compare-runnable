package com.example.dbcompare.service;

import com.example.dbcompare.domain.model.SchemaMapping;
import com.example.dbcompare.domain.model.TableMapping;
import com.example.dbcompare.util.NameNormalizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingService {
    private final Map<String, String> schemaIndex = new HashMap<>();
    private final Map<String, String> reverseSchemaIndex = new HashMap<>();
    private final Map<String, TableTarget> tableIndex = new HashMap<>();
    private final Map<String, SourceTarget> reverseTableIndex = new HashMap<>();

    public MappingService(List<SchemaMapping> mappings, List<TableMapping> tableMappings) {
        for (SchemaMapping mapping : mappings) {
            String sourceDatabaseName = NameNormalizer.normalize(mapping.getSourceDatabaseName());
            String targetSchemaName = NameNormalizer.normalize(mapping.getTargetSchemaName());
            schemaIndex.put(sourceDatabaseName, targetSchemaName);
            reverseSchemaIndex.put(targetSchemaName, sourceDatabaseName);
        }
        for (TableMapping mapping : tableMappings) {
            tableIndex.put(buildTableKey(mapping.getSourceDatabaseName(), mapping.getSourceSchemaName(), mapping.getSourceTableName()),
                    new TableTarget(NameNormalizer.normalize(mapping.getTargetSchemaName()), NameNormalizer.normalize(mapping.getTargetTableName())));
            reverseTableIndex.put(buildReverseTableKey(mapping.getTargetSchemaName(), mapping.getTargetTableName()),
                    new SourceTarget(NameNormalizer.normalize(mapping.getSourceDatabaseName()),
                            NameNormalizer.normalize(mapping.getSourceSchemaName()),
                            NameNormalizer.normalize(mapping.getSourceTableName())));
        }
    }

    public String findTargetSchema(String sourceDatabaseName) {
        return schemaIndex.get(NameNormalizer.normalize(sourceDatabaseName));
    }

    public String findSourceDatabaseByTargetSchema(String targetSchemaName) {
        return reverseSchemaIndex.get(NameNormalizer.normalize(targetSchemaName));
    }

    public TableTarget findTargetTable(String sourceDatabaseName, String sourceSchemaName, String sourceTableName) {
        TableTarget exact = tableIndex.get(buildTableKey(sourceDatabaseName, sourceSchemaName, sourceTableName));
        if (exact != null) return exact;
        String targetSchema = findTargetSchema(sourceDatabaseName);
        return targetSchema == null ? null : new TableTarget(targetSchema, NameNormalizer.normalize(sourceTableName));
    }

    public SourceTarget findSourceForTarget(String targetSchemaName, String targetTableName) {
        return reverseTableIndex.get(buildReverseTableKey(targetSchemaName, targetTableName));
    }

    private String buildTableKey(String sourceDatabaseName, String sourceSchemaName, String sourceTableName) {
        return NameNormalizer.normalize(sourceDatabaseName) + "::" +
                NameNormalizer.normalize(sourceSchemaName) + "::" +
                NameNormalizer.normalize(sourceTableName);
    }

    private String buildReverseTableKey(String targetSchemaName, String targetTableName) {
        return NameNormalizer.normalize(targetSchemaName) + "::" + NameNormalizer.normalize(targetTableName);
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

    public static class SourceTarget {
        private final String sourceDatabaseName;
        private final String sourceSchemaName;
        private final String sourceTableName;

        public SourceTarget(String sourceDatabaseName, String sourceSchemaName, String sourceTableName) {
            this.sourceDatabaseName = sourceDatabaseName;
            this.sourceSchemaName = sourceSchemaName;
            this.sourceTableName = sourceTableName;
        }

        public String getSourceDatabaseName() { return sourceDatabaseName; }
        public String getSourceSchemaName() { return sourceSchemaName; }
        public String getSourceTableName() { return sourceTableName; }
    }
}