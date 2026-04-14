package com.example.dbcompare.infrastructure.output;

import com.example.dbcompare.domain.model.CompareSummary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class SummaryReportWriter {
    public void write(Path path, CompareSummary summary) {
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            StringBuilder builder = new StringBuilder();
            builder.append("源库数量=").append(summary.getSourceCount()).append(System.lineSeparator());
            builder.append("源Schema数量=").append(summary.getSourceSchemaCount()).append(System.lineSeparator());
            builder.append("源表数量=").append(summary.getSourceTableCount()).append(System.lineSeparator());
            builder.append("差异数量=").append(summary.getDiffCount()).append(System.lineSeparator());
            builder.append("差异类型统计:").append(System.lineSeparator());
            for (Map.Entry<?, Integer> entry : summary.getDiffTypeCount().entrySet()) {
                builder.append("  ").append(OutputTextFormatter.diffTypesText(String.valueOf(entry.getKey())))
                        .append('=').append(entry.getValue()).append(System.lineSeparator());
            }
            Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write summary report: " + path, e);
        }
    }
}
