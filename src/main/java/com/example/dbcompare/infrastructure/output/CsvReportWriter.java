package com.example.dbcompare.infrastructure.output;

import com.example.dbcompare.domain.model.DiffRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvReportWriter {
    public void write(Path path, List<DiffRecord> records) {
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            StringBuilder builder = new StringBuilder();
            builder.append("sourceDatabase,sourceSchema,sourceTable,targetSchema,targetTable,columnName,diffType,sourceValue,targetValue,message")
                    .append(System.lineSeparator());
            for (DiffRecord record : records) {
                builder.append(csv(record.getSourceDatabaseName())).append(',')
                        .append(csv(record.getSourceSchemaName())).append(',')
                        .append(csv(record.getSourceTableName())).append(',')
                        .append(csv(record.getTargetSchemaName())).append(',')
                        .append(csv(record.getTargetTableName())).append(',')
                        .append(csv(record.getColumnName())).append(',')
                        .append(csv(record.getDiffType() == null ? null : record.getDiffType().name())).append(',')
                        .append(csv(record.getSourceValue())).append(',')
                        .append(csv(record.getTargetValue())).append(',')
                        .append(csv(record.getMessage())).append(System.lineSeparator());
            }
            Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write CSV report: " + path, e);
        }
    }

    private String csv(String value) {
        if (value == null) return "";
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
