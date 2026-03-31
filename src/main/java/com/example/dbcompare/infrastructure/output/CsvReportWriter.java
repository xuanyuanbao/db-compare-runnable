package com.example.dbcompare.infrastructure.output;

import com.example.dbcompare.domain.model.DiffRecord;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvReportWriter {
    public CsvReportSession open(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
            writer.write("sourceDatabase,sourceSchema,sourceTable,targetSchema,targetTable,columnName,diffType,sourceValue,targetValue,message");
            writer.newLine();
            return new CsvReportSession(writer);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open CSV report: " + path, e);
        }
    }

    public static class CsvReportSession implements Closeable {
        private final BufferedWriter writer;

        private CsvReportSession(BufferedWriter writer) {
            this.writer = writer;
        }

        public void append(List<DiffRecord> records) {
            try {
                for (DiffRecord record : records) {
                    writer.write(csv(record.getSourceDatabaseName()));
                    writer.write(',');
                    writer.write(csv(record.getSourceSchemaName()));
                    writer.write(',');
                    writer.write(csv(record.getSourceTableName()));
                    writer.write(',');
                    writer.write(csv(record.getTargetSchemaName()));
                    writer.write(',');
                    writer.write(csv(record.getTargetTableName()));
                    writer.write(',');
                    writer.write(csv(record.getColumnName()));
                    writer.write(',');
                    writer.write(csv(record.getDiffType() == null ? null : record.getDiffType().name()));
                    writer.write(',');
                    writer.write(csv(record.getSourceValue()));
                    writer.write(',');
                    writer.write(csv(record.getTargetValue()));
                    writer.write(',');
                    writer.write(csv(record.getMessage()));
                    writer.newLine();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to append CSV report", e);
            }
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        private String csv(String value) {
            if (value == null) return "";
            return '"' + value.replace("\"", "\"\"") + '"';
        }
    }
}
