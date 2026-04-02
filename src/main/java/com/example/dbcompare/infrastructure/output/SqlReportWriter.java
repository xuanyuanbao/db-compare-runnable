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
    private static final String[] COLUMN_TYPES = {
            "VARCHAR(512)", "VARCHAR(512)", "VARCHAR(512)", "VARCHAR(512)", "VARCHAR(512)", "VARCHAR(512)",
            "VARCHAR(16)", "VARCHAR(16)",
            "VARCHAR(256)", "VARCHAR(256)", "VARCHAR(32)",
            "VARCHAR(256)", "VARCHAR(256)", "VARCHAR(32)",
            "VARCHAR(4000)", "VARCHAR(4000)", "VARCHAR(32)",
            "VARCHAR(64)", "VARCHAR(64)", "VARCHAR(32)",
            "VARCHAR(32)", "VARCHAR(4000)", "VARCHAR(4000)"
    };

    public SqlReportSession open(Path path, String tableName) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
            String resolvedTableName = resolveTableName(tableName);
            writeCreateTable(writer, resolvedTableName);
            writer.newLine();
            return new SqlReportSession(writer, resolvedTableName);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open SQL report: " + path, e);
        }
    }

    private void writeCreateTable(BufferedWriter writer, String tableName) throws IOException {
        writer.write("CREATE TABLE ");
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
        writer.write(");");
        writer.newLine();
    }

    private String resolveTableName(String tableName) {
        return tableName == null || tableName.isBlank()
                ? OutputConfig.DEFAULT_SQL_TABLE_NAME
                : tableName.trim();
    }

    private String quotedIdentifier(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    public class SqlReportSession implements Closeable {
        private final BufferedWriter writer;
        private final String tableName;

        private SqlReportSession(BufferedWriter writer, String tableName) {
            this.writer = writer;
            this.tableName = tableName;
        }

        public void append(List<ColumnComparisonRecord> records) {
            try {
                for (ColumnComparisonRecord record : records) {
                    writer.write("INSERT INTO ");
                    writer.write(quotedIdentifier(tableName));
                    writer.write(" (");
                    for (int index = 0; index < ExcelReportWriter.DETAIL_HEADERS.length; index++) {
                        if (index > 0) {
                            writer.write(", ");
                        }
                        writer.write(quotedIdentifier(ExcelReportWriter.DETAIL_HEADERS[index]));
                    }
                    writer.write(") VALUES (");
                    String[] values = valuesOf(record);
                    for (int index = 0; index < values.length; index++) {
                        if (index > 0) {
                            writer.write(", ");
                        }
                        writer.write(sqlLiteral(values[index]));
                    }
                    writer.write(");");
                    writer.newLine();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to append SQL report", e);
            }
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        private String[] valuesOf(ColumnComparisonRecord record) {
            return new String[]{
                    record.getSourceDatabaseName(),
                    record.getSourceSchemaName(),
                    record.getSourceTableName(),
                    record.getTargetSchemaName(),
                    record.getTargetTableName(),
                    record.getColumnName(),
                    Boolean.toString(record.isSourceColumnExists()),
                    Boolean.toString(record.isTargetColumnExists()),
                    record.getSourceType(),
                    record.getTargetType(),
                    statusText(record.getTypeStatus()),
                    record.getSourceLength(),
                    record.getTargetLength(),
                    statusText(record.getLengthStatus()),
                    record.getSourceDefaultValue(),
                    record.getTargetDefaultValue(),
                    statusText(record.getDefaultStatus()),
                    record.getSourceNullable(),
                    record.getTargetNullable(),
                    statusText(record.getNullableStatus()),
                    statusText(record.getOverallStatus()),
                    record.getDiffTypes(),
                    record.getMessage()
            };
        }

        private String statusText(ComparisonStatus status) {
            return status == null ? null : status.name();
        }

        private String sqlLiteral(String value) {
            if (value == null) {
                return "NULL";
            }
            return '\'' + value.replace("'", "''") + '\'';
        }
    }
}