package com.example.dbcompare.tests;

import com.example.dbcompare.domain.model.SchemaMapping;
import com.example.dbcompare.domain.model.TableMapping;
import com.example.dbcompare.service.MappingService;

import java.util.List;

public final class MappingServiceTest {
    private MappingServiceTest() {
    }

    public static void run() {
        SchemaMapping schemaMapping = new SchemaMapping();
        schemaMapping.setSourceDatabaseName("source_a");
        schemaMapping.setTargetSchemaName("target_schema");

        TableMapping tableMapping = new TableMapping();
        tableMapping.setSourceDatabaseName("source_a");
        tableMapping.setSourceSchemaName("legacy_a");
        tableMapping.setSourceTableName("old_table");
        tableMapping.setTargetSchemaName("mapped_schema");
        tableMapping.setTargetTableName("new_table");

        MappingService mappingService = new MappingService(List.of(schemaMapping), List.of(tableMapping));

        MappingService.TableTarget exact = mappingService.findTargetTable("SOURCE_A", "LEGACY_A", "OLD_TABLE");
        TestSupport.assertEquals("MAPPED_SCHEMA", exact.getSchemaName(),
                "table mapping should override schema-level mapping");
        TestSupport.assertEquals("NEW_TABLE", exact.getTableName(),
                "table mapping should override target table name");

        MappingService.TableTarget fallback = mappingService.findTargetTable("SOURCE_A", "LEGACY_A", "CUSTOMER");
        TestSupport.assertEquals("TARGET_SCHEMA", fallback.getSchemaName(),
                "schema mapping should be used when table mapping is absent");
        TestSupport.assertEquals("CUSTOMER", fallback.getTableName(),
                "fallback should preserve normalized table name");

        TestSupport.assertNull(mappingService.findTargetTable("UNKNOWN", "A", "B"),
                "unknown source should not resolve to a target table");
    }
}
