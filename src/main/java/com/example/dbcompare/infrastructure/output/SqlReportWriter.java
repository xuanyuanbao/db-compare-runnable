package com.example.dbcompare.infrastructure.output;

import com.example.dbcompare.domain.enums.ComparisonStatus;
import com.example.dbcompare.domain.model.ColumnComparisonRecord;
import com.example.dbcompare.domain.model.OutputConfig;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SqlReportWriter {
    private static final int INSERT_BATCH_SIZE = 500;
    private static final int SOURCE_EXISTS_INDEX = 6;
    private static final int TARGET_EXISTS_INDEX = 7;
    private static final int AFFECTS_RESULT_INDEX = 22;
    private static final String[] COLUMN_TYPES = {
            "VARCHAR(128)", "VARCHAR(128)", "VARCHAR(128)", "VARCHAR(128)", "VARCHAR(128)", "VARCHAR(128)",
            "TINYINT(1)", "TINYINT(1)",
            "VARCHAR(128)", "VARCHAR(128)", "VARCHAR(32)",
            "VARCHAR(64)", "VARCHAR(64)", "VARCHAR(32)",
            "TEXT", "TEXT", "VARCHAR(32)",
            "VARCHAR(32)", "VARCHAR(32)", "VARCHAR(32)",
            "VARCHAR(32)", "VARCHAR(32)", "TINYINT(1)", "VARCHAR(512)", "TEXT"
    };

    public SqlReportSession open(Path path, String tableName) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
            String resolvedTableName = resolveTableName(tableName);
            writePreamble(writer, resolvedTableName);
            return new SqlReportSession(writer, resolvedTableName);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open SQL report: " + path, e);
        }
    }

    private void writePreamble(BufferedWriter writer, String tableName) throws IOException {
        writer.write("-- db-compare-runnable 生成的 MySQL 导入脚本");
        writer.newLine();
        writer.write("SET NAMES utf8mb4;");
        writer.newLine();
        writer.write("DROP TABLE IF EXISTS ");
        writer.write(quotedIdentifier(tableName));
        writer.write(';');
        writer.newLine();
        writer.write("CREATE TABLE IF NOT EXISTS ");
        writer.write(quotedIdentifier(tableName));
        writer.write(" (");
        writer.newLine();
        for (int index = 0; index < ExcelReportWriter.DETAIL_HEADERS.length; index++) {
            writer.write("    ");
            writer.write(quotedIdentifier(ExcelReportWriter.DETAIL_HEADERS[index]));
            writer.write(' ');
            writer.write(COLUMN_TYPES[index]);
            if (index < ExcelReportWriter.DETAIL_HEADERS.length - 1) {
                writer.write(',');
            }
            writer.newLine();
        }
        writer.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
        writer.newLine();
        writer.newLine();
    }

    private String resolveTableName(String tableName) {
        return tableName == null || tableName.isBlank()
                ? OutputConfig.DEFAULT_SQL_TABLE_NAME
                : tableName.trim();
    }

    private String quotedIdentifier(String identifier) {
        return '`' + identifier.replace("`", "``") + '`';
    }

    public class SqlReportSession implements Closeable {
        private final BufferedWriter writer;
        private final String tableName;
        private int pendingInsertCount;

        private SqlReportSession(BufferedWriter writer, String tableName) {
            this.writer = writer;
            this.tableName = tableName;
        }

        public void append(List<ColumnComparisonRecord> records) {
            try {
                for (ColumnComparisonRecord record : records) {
                    if (pendingInsertCount == 0) {
                        writeInsertHeader();
                    } else {
                        writer.write(',');
                        writer.newLine();
                    }
                    writeValues(record);
                    pendingInsertCount++;
                    if (pendingInsertCount >= INSERT_BATCH_SIZE) {
                        flushPendingInsert();
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to append SQL report", e);
            }
        }

        @Override
        public void close() throws IOException {
            flushPendingInsert();
            writer.close();
        }

        private void writeInsertHeader() throws IOException {
            writer.write("INSERT INTO ");
            writer.write(quotedIdentifier(tableName));
            writer.write(" (");
            for (int index = 0; index < ExcelReportWriter.DETAIL_HEADERS.length; index++) {
                if (index > 0) {
                    writer.write(", ");
                }
                writer.write(quotedIdentifier(ExcelReportWriter.DETAIL_HEADERS[index]));
            }
            writer.write(") VALUES");
            writer.newLine();
        }

        private void writeValues(ColumnComparisonRecord record) throws IOException {
            writer.write('(');
            String[] values = valuesOf(record);
            for (int index = 0; index < values.length; index++) {
                if (index > 0) {
                    writer.write(", ");
                }
                writer.write(isBooleanColumn(index) ? numericLiteral(values[index]) : sqlLiteral(values[index]));
            }
            writer.write(')');
        }

        private void flushPendingInsert() throws IOException {
            if (pendingInsertCount == 0) {
                return;
            }
            writer.write(';');
            writer.newLine();
            writer.newLine();
            pendingInsertCount = 0;
        }

        private boolean isBooleanColumn(int columnIndex) {
            return columnIndex == SOURCE_EXISTS_INDEX || columnIndex == TARGET_EXISTS_INDEX || columnIndex == AFFECTS_RESULT_INDEX;
        }

        private String[] valuesOf(ColumnComparisonRecord record) {
            return new String[]{
                    record.getSourceDatabaseName(),
                    record.getSourceSchemaName(),
                    record.getSourceTableName(),
                    record.getTargetSchemaName(),
                    record.getTargetTableName(),
                    record.getColumnName(),
                    record.isSourceColumnExists() ? "1" : "0",
                    record.isTargetColumnExists() ? "1" : "0",
                    record.getSourceType(),
                    record.getTargetType(),
                    statusText(record.getTypeStatus()),
                    record.getSourceLength(),
                    record.getTargetLength(),
                    statusText(record.getLengthStatus()),
                    record.getSourceDefaultValue(),
                    record.getTargetDefaultValue(),
                    statusText(record.getDefaultStatus()),
                    OutputTextFormatter.nullableText(record.getSourceNullable()),
                    OutputTextFormatter.nullableText(record.getTargetNullable()),
                    statusText(record.getNullableStatus()),
                    statusText(record.getOverallStatus()),
                    OutputTextFormatter.diffGroupText(record.getDiffGroup()),
                    record.isAffectsResult() ? "1" : "0",
                    OutputTextFormatter.diffTypesText(record.getDiffTypes()),
                    OutputTextFormatter.messageText(record.getMessage())
            };
        }

        private String statusText(ComparisonStatus status) {
            return OutputTextFormatter.comparisonStatusText(status);
        }

        private String numericLiteral(String value) {
            return value == null || value.isBlank() ? "NULL" : value;
        }

        private String sqlLiteral(String value) {
            if (value == null) {
                return "NULL";
            }
            return '\'' + escapeMysqlString(value) + '\'';
        }

        private String escapeMysqlString(String value) {
            return value
                    .replace("\\", "\\\\")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n")
                    .replace("\t", "\\t")
                    .replace("'", "''");
        }
    }
}
