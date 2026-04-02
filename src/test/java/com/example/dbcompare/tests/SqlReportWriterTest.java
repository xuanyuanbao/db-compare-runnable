package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.infrastructure.output.SqlReportWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlReportWriterTest {
    @Test
    void writesCreateTableAndInsertStatements(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("detail.sql");
        SqlReportWriter writer = new SqlReportWriter();

        try (SqlReportWriter.SqlReportSession session = writer.open(output, "compare_detail_tmp")) {
            session.append(List.of(record()));
        }

        String sql = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(sql.contains("CREATE TABLE \"compare_detail_tmp\""), "sql output should include the create table statement");
        assertTrue(sql.contains("\"sourceDatabase\" VARCHAR(512)"), "sql output should keep the same detail columns as Excel");
        assertTrue(sql.contains("\"message\" VARCHAR(4000)"), "sql output should include the message column in the ddl");
        assertTrue(sql.contains("INSERT INTO \"compare_detail_tmp\""), "sql output should include insert statements");
        assertTrue(sql.contains("'O''Reilly default'"), "sql output should escape single quotes for inserts");
    }

    private ColumnComparisonRecord record() {
        ColumnComparisonRecord record = new ColumnComparisonRecord();
        record.setSourceDatabaseName("SRC_DB");
        record.setSourceSchemaName("SRC_SCHEMA");
        record.setSourceTableName("SRC_TABLE");
        record.setTargetSchemaName("TGT_SCHEMA");
        record.setTargetTableName("TGT_TABLE");
        record.setColumnName("COL_A");
        record.setSourceColumnExists(true);
        record.setTargetColumnExists(false);
        record.setSourceType("VARCHAR");
        record.setTypeStatus(ComparisonStatus.MISMATCH);
        record.setSourceDefaultValue("O'Reilly default");
        record.setDefaultStatus(ComparisonStatus.MISMATCH);
        record.setOverallStatus(ComparisonStatus.MISMATCH);
        record.setDiffTypes("COLUMN_MISSING_IN_TARGET");
        record.setMessage("target column missing");
        return record;
    }
}