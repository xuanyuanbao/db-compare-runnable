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
    void writesMysqlFriendlyCreateTableAndBatchedInsertStatements(@TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("detail.sql");
        SqlReportWriter writer = new SqlReportWriter();

        try (SqlReportWriter.SqlReportSession session = writer.open(output, "compare_detail_tmp")) {
            session.append(List.of(record("COL_A"), record("COL_B")));
        }

        String sql = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(sql.contains("DROP TABLE IF EXISTS `compare_detail_tmp`;"), "sql output should recreate the target table for MySQL imports");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `compare_detail_tmp`"), "sql output should include MySQL create table syntax");
        assertTrue(sql.contains("`sourceDatabase` VARCHAR(128)"), "sql output should keep the detail columns in the ddl");
        assertTrue(sql.contains("`sourceExists` TINYINT(1)"), "sql output should use MySQL-friendly boolean storage");
        assertTrue(sql.contains("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"), "sql output should use a MySQL-friendly table definition");
        assertTrue(sql.contains("INSERT INTO `compare_detail_tmp`"), "sql output should include insert statements");
        assertTrue(sql.contains("),\n(") || sql.contains("),\r\n("), "sql output should batch multiple rows into one insert");
        assertTrue(sql.contains("'O''Reilly \\\\ default\\nline'"), "sql output should escape quotes, backslashes, and new lines for MySQL");
    }

    private ColumnComparisonRecord record(String columnName) {
        ColumnComparisonRecord record = new ColumnComparisonRecord();
        record.setSourceDatabaseName("SRC_DB");
        record.setSourceSchemaName("SRC_SCHEMA");
        record.setSourceTableName("SRC_TABLE");
        record.setTargetSchemaName("TGT_SCHEMA");
        record.setTargetTableName("TGT_TABLE");
        record.setColumnName(columnName);
        record.setSourceColumnExists(true);
        record.setTargetColumnExists(false);
        record.setSourceType("VARCHAR");
        record.setTypeStatus(ComparisonStatus.MISMATCH);
        record.setSourceDefaultValue("O'Reilly \\ default\nline");
        record.setDefaultStatus(ComparisonStatus.MISMATCH);
        record.setOverallStatus(ComparisonStatus.MISMATCH);
        record.setDiffTypes("COLUMN_MISSING_IN_TARGET");
        record.setMessage("target column missing");
        return record;
    }
}