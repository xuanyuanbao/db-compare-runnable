package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.TargetViewLineageEntry;
import com.example.dbcompare.service.TargetViewLineageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TargetViewLineageServiceTest {
    @Test
    void loadsTargetViewLineageFromConfiguredFile(@TempDir Path tempDir) throws Exception {
        Path lineageFile = tempDir.resolve("lineage.csv");
        Files.writeString(lineageFile, """
                targetViewSchema,targetView,targetTableSchema,targetTable
                VIEW_APP,CUSTOMER_VIEW,ODS,CUSTOMER_BASE
                VIEW_APP,CUSTOMER_VIEW,DWD,CUSTOMER_PROFILE
                VIEW_APP,ORDER_VIEW,ODS,ORDER_BASE
                """);

        DataSourceInfo target = new DataSourceInfo();
        target.setSourceName("GAUSS");
        target.setType(DatabaseType.SNAPSHOT);
        target.setViewLineageFile(lineageFile.toString());

        List<TargetViewLineageEntry> entries = new TargetViewLineageService().loadLineage(target, "VIEW_APP", "CUSTOMER_VIEW");

        assertEquals(2, entries.size(), "configured lineage file should return all target tables for the selected view");
        assertEquals("ODS", entries.get(0).getTargetTableSchema(), "lineage should preserve target table schema");
        assertEquals("CUSTOMER_BASE", entries.get(0).getTargetTableName(), "lineage should preserve target table name");
        assertEquals("DWD", entries.get(1).getTargetTableSchema(), "lineage should keep the second target table schema");
        assertEquals("CUSTOMER_PROFILE", entries.get(1).getTargetTableName(), "lineage should keep the second target table name");
    }
}
