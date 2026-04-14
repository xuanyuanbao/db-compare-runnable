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
    private static final String[] HEADERS = {
            "源数据库", "源Schema", "源表", "目标Schema", "目标表", "字段名", "差异类型", "源值", "目标值", "说明"
    };

    public CsvReportSession open(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
            writer.write(String.join(",", HEADERS));
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
                    writer.write(csv(OutputTextFormatter.diffTypeText(record.getDiffType())));
                    writer.write(',');
                    writer.write(csv(record.getSourceValue()));
                    writer.write(',');
                    writer.write(csv(record.getTargetValue()));
                    writer.write(',');
                    writer.write(csv(OutputTextFormatter.messageText(record.getMessage())));
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
