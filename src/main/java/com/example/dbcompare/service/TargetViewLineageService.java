package com.example.dbcompare.service;

import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.TargetViewLineageEntry;
import com.example.dbcompare.util.NameNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TargetViewLineageService {
    private static final Logger log = LoggerFactory.getLogger(TargetViewLineageService.class);

    private static final String GAUSS_VIEW_LINEAGE_SQL = """
            SELECT DISTINCT
                view_ns.nspname AS target_view_schema,
                view_cls.relname AS target_view_name,
                table_ns.nspname AS target_table_schema,
                table_cls.relname AS target_table_name
            FROM pg_rewrite rw
            JOIN pg_class view_cls ON view_cls.oid = rw.ev_class
            JOIN pg_namespace view_ns ON view_ns.oid = view_cls.relnamespace
            JOIN pg_depend dep ON dep.objid = rw.oid
            JOIN pg_class table_cls ON table_cls.oid = dep.refobjid
            JOIN pg_namespace table_ns ON table_ns.oid = table_cls.relnamespace
            WHERE view_cls.relkind = 'v'
              AND table_cls.relkind IN ('r', 'p', 'm', 'f')
              AND view_ns.nspname = ?
              AND view_cls.relname = ?
            ORDER BY table_ns.nspname, table_cls.relname
            """;

    public List<TargetViewLineageEntry> loadLineage(DataSourceInfo target, String targetViewSchema, String targetView) {
        if (target == null || targetViewSchema == null || targetView == null) {
            return List.of();
        }
        if (target.getViewLineageFile() != null && !target.getViewLineageFile().isBlank()) {
            return loadFromFile(Path.of(target.getViewLineageFile()), targetViewSchema, targetView);
        }
        if (target.getType() == DatabaseType.GAUSS) {
            return loadFromGauss(target, targetViewSchema, targetView);
        }
        return List.of();
    }

    private List<TargetViewLineageEntry> loadFromFile(Path path, String targetViewSchema, String targetView) {
        List<TargetViewLineageEntry> entries = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                if (line == null || line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                List<String> parts = splitCsv(line);
                if (parts.size() < 4) {
                    continue;
                }
                if (isHeader(parts)) {
                    continue;
                }
                String rowViewSchema = NameNormalizer.normalize(parts.get(0));
                String rowViewName = NameNormalizer.normalize(parts.get(1));
                if (!NameNormalizer.normalize(targetViewSchema).equals(rowViewSchema)
                        || !NameNormalizer.normalize(targetView).equals(rowViewName)) {
                    continue;
                }
                entries.add(new TargetViewLineageEntry(
                        rowViewSchema,
                        rowViewName,
                        NameNormalizer.normalize(parts.get(2)),
                        NameNormalizer.normalize(parts.get(3))));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load target view lineage file: " + path, e);
        }
        return deduplicate(entries);
    }

    private boolean isHeader(List<String> parts) {
        return "targetviewschema".equalsIgnoreCase(parts.get(0).replace("_", ""))
                || "target_view_schema".equalsIgnoreCase(parts.get(0));
    }

    private List<TargetViewLineageEntry> loadFromGauss(DataSourceInfo target, String targetViewSchema, String targetView) {
        loadDriverIfNecessary(target);
        List<TargetViewLineageEntry> entries = new ArrayList<>();
        try (Connection connection = createConnection(target);
             PreparedStatement statement = connection.prepareStatement(GAUSS_VIEW_LINEAGE_SQL)) {
            statement.setString(1, targetViewSchema);
            statement.setString(2, targetView);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new TargetViewLineageEntry(
                            NameNormalizer.normalize(resultSet.getString("target_view_schema")),
                            NameNormalizer.normalize(resultSet.getString("target_view_name")),
                            NameNormalizer.normalize(resultSet.getString("target_table_schema")),
                            NameNormalizer.normalize(resultSet.getString("target_table_name"))));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load target view lineage for "
                    + targetViewSchema + "." + targetView, e);
        }
        log.info("Loaded {} lineage tables for target view {}.{}", entries.size(), targetViewSchema, targetView);
        return deduplicate(entries);
    }

    protected Connection createConnection(DataSourceInfo dataSourceInfo) throws SQLException {
        Properties properties = new Properties();
        if (dataSourceInfo.getUsername() != null) {
            properties.setProperty("user", dataSourceInfo.getUsername());
        }
        if (dataSourceInfo.getPassword() != null) {
            properties.setProperty("password", dataSourceInfo.getPassword());
        }
        return DriverManager.getConnection(dataSourceInfo.getJdbcUrl(), properties);
    }

    protected void loadDriverIfNecessary(DataSourceInfo dataSourceInfo) {
        if (dataSourceInfo.getDriverClassName() == null || dataSourceInfo.getDriverClassName().isBlank()) {
            return;
        }
        try {
            Class.forName(dataSourceInfo.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC driver not found: " + dataSourceInfo.getDriverClassName(), e);
        }
    }

    private List<TargetViewLineageEntry> deduplicate(List<TargetViewLineageEntry> entries) {
        Map<String, TargetViewLineageEntry> unique = new LinkedHashMap<>();
        for (TargetViewLineageEntry entry : entries) {
            String key = NameNormalizer.normalize(entry.getTargetViewSchema()) + "::"
                    + NameNormalizer.normalize(entry.getTargetViewName()) + "::"
                    + NameNormalizer.normalize(entry.getTargetTableSchema()) + "::"
                    + NameNormalizer.normalize(entry.getTargetTableName());
            unique.putIfAbsent(key, entry);
        }
        return new ArrayList<>(unique.values());
    }

    private List<String> splitCsv(String rawLine) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < rawLine.length(); index++) {
            char ch = rawLine.charAt(index);
            if (ch == '"') {
                quoted = !quoted;
                continue;
            }
            if (ch == ',' && !quoted) {
                parts.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        parts.add(current.toString().trim());
        return parts;
    }
}
