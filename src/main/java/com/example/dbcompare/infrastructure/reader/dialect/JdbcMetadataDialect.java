package com.example.dbcompare.infrastructure.reader.dialect;

import com.example.dbcompare.domain.enums.CompareObjectType;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.util.NameNormalizer;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface JdbcMetadataDialect {
    default String schemaPattern(DataSourceInfo dataSourceInfo) {
        return null;
    }

    default String tableNamePattern(DataSourceInfo dataSourceInfo) {
        return "%";
    }

    default String[] tableTypes(CompareObjectType objectType) {
        return objectType == CompareObjectType.VIEW
                ? new String[]{"VIEW"}
                : new String[]{"TABLE"};
    }

    default String normalizeSchemaName(String schemaName) {
        return NameNormalizer.normalize(schemaName);
    }

    default String normalizeTableName(String tableName) {
        return NameNormalizer.normalize(tableName);
    }

    default String normalizeColumnName(String columnName) {
        return NameNormalizer.normalize(columnName);
    }

    default boolean shouldIncludeSchema(String schemaName) {
        return true;
    }

    default boolean shouldIncludeTable(String schemaName, String tableName) {
        return true;
    }

    default String buildLength(ResultSet columns) throws SQLException {
        int size = columns.getInt("COLUMN_SIZE");
        int decimalDigits = columns.getInt("DECIMAL_DIGITS");
        if (columns.wasNull()) {
            return null;
        }
        if (decimalDigits > 0) {
            return size + "," + decimalDigits;
        }
        return String.valueOf(size);
    }
}

