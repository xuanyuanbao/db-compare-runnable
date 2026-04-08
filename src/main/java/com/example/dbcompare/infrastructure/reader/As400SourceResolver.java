package com.example.dbcompare.infrastructure.reader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class As400SourceResolver {
    private static final String RESOLVE_SQL = """
            SELECT t.TABLE_SCHEMA
            FROM QSYS2.SYSTABLES t
            JOIN QSYS2.LIBRARY_LIST_INFO l
              ON t.TABLE_SCHEMA = l.SCHEMA_NAME
            WHERE t.TABLE_NAME = ?
            ORDER BY l.ORDINAL_POSITION
            """;

    public Resolution resolve(Connection connection, String explicitSchema, String tableName) throws SQLException {
        if (explicitSchema != null && !explicitSchema.isBlank()) {
            return Resolution.resolved(explicitSchema);
        }
        List<String> candidates = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(RESOLVE_SQL)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    candidates.add(resultSet.getString(1));
                }
            }
        }
        return resolveCandidates(tableName, candidates);
    }

    static Resolution resolveCandidates(String tableName, List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Resolution.missing("Source table not found in AS400 library list: " + tableName);
        }
        Set<String> uniqueSchemas = new LinkedHashSet<>();
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                uniqueSchemas.add(candidate.trim());
            }
        }
        if (uniqueSchemas.isEmpty()) {
            return Resolution.missing("Source table not found in AS400 library list: " + tableName);
        }
        String firstSchema = uniqueSchemas.iterator().next();
        if (uniqueSchemas.size() > 1) {
            return Resolution.ambiguous(firstSchema,
                    "Multiple AS400 library schemas matched table " + tableName + ": " + String.join(", ", uniqueSchemas));
        }
        return Resolution.resolved(firstSchema);
    }

    public static final class Resolution {
        private final String schemaName;
        private final boolean ambiguous;
        private final String message;

        private Resolution(String schemaName, boolean ambiguous, String message) {
            this.schemaName = schemaName;
            this.ambiguous = ambiguous;
            this.message = message;
        }

        public static Resolution resolved(String schemaName) {
            return new Resolution(schemaName, false, null);
        }

        public static Resolution ambiguous(String schemaName, String message) {
            return new Resolution(schemaName, true, message);
        }

        public static Resolution missing(String message) {
            return new Resolution(null, false, message);
        }

        public String getSchemaName() { return schemaName; }
        public boolean isAmbiguous() { return ambiguous; }
        public String getMessage() { return message; }
        public boolean isMissing() { return schemaName == null && !ambiguous; }
    }
}