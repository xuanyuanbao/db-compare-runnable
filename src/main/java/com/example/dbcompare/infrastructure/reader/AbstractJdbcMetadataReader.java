package com.example.dbcompare.infrastructure.reader;

import com.example.dbcompare.domain.model.ColumnMeta;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.DatabaseMeta;
import com.example.dbcompare.domain.model.SchemaMeta;
import com.example.dbcompare.domain.model.TableMeta;
import com.example.dbcompare.util.NameNormalizer;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public abstract class AbstractJdbcMetadataReader implements MetadataReader {
    @Override
    public DatabaseMeta loadMetadata(DataSourceInfo dataSourceInfo) {
        DatabaseMeta databaseMeta = new DatabaseMeta(dataSourceInfo.getSourceName());
        loadDriverIfNecessary(dataSourceInfo);
        try (Connection connection = createConnection(dataSourceInfo)) {
            loadTables(connection.getMetaData(), dataSourceInfo, databaseMeta);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load metadata for " + dataSourceInfo.getSourceName(), e);
        }
        return databaseMeta;
    }

    protected Connection createConnection(DataSourceInfo dataSourceInfo) throws SQLException {
        Properties properties = new Properties();
        if (dataSourceInfo.getUsername() != null) properties.setProperty("user", dataSourceInfo.getUsername());
        if (dataSourceInfo.getPassword() != null) properties.setProperty("password", dataSourceInfo.getPassword());
        return DriverManager.getConnection(dataSourceInfo.getJdbcUrl(), properties);
    }

    protected void loadDriverIfNecessary(DataSourceInfo dataSourceInfo) {
        if (dataSourceInfo.getDriverClassName() == null || dataSourceInfo.getDriverClassName().isBlank()) return;
        try {
            Class.forName(dataSourceInfo.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC driver not found: " + dataSourceInfo.getDriverClassName(), e);
        }
    }

    protected void loadTables(DatabaseMetaData metaData, DataSourceInfo dataSourceInfo, DatabaseMeta databaseMeta) throws SQLException {
        try (ResultSet tables = metaData.getTables(dataSourceInfo.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String schemaName = NameNormalizer.normalize(tables.getString("TABLE_SCHEM"));
                String tableName = NameNormalizer.normalize(tables.getString("TABLE_NAME"));
                if (!shouldReadSchema(schemaName, dataSourceInfo) || !shouldReadTable(tableName, dataSourceInfo)) {
                    continue;
                }
                SchemaMeta schemaMeta = databaseMeta.getSchemas().computeIfAbsent(schemaName, SchemaMeta::new);
                TableMeta tableMeta = schemaMeta.getTables().computeIfAbsent(tableName, TableMeta::new);
                loadColumns(metaData, dataSourceInfo, schemaName, tableName, tableMeta);
            }
        }
    }

    protected void loadColumns(DatabaseMetaData metaData, DataSourceInfo dataSourceInfo,
                               String schemaName, String tableName, TableMeta tableMeta) throws SQLException {
        try (ResultSet columns = metaData.getColumns(dataSourceInfo.getCatalog(), schemaName, tableName, "%")) {
            while (columns.next()) {
                ColumnMeta columnMeta = new ColumnMeta();
                String columnName = NameNormalizer.normalize(columns.getString("COLUMN_NAME"));
                columnMeta.setColumnName(columnName);
                columnMeta.setDataType(columns.getString("TYPE_NAME"));
                columnMeta.setLength(buildLength(columns));
                columnMeta.setNullable(columns.getString("IS_NULLABLE"));
                columnMeta.setDefaultValue(columns.getString("COLUMN_DEF"));
                columnMeta.setOrdinalPosition(columns.getInt("ORDINAL_POSITION"));
                tableMeta.getColumns().put(columnName, columnMeta);
            }
        }
    }

    protected String buildLength(ResultSet columns) throws SQLException {
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

    protected boolean shouldReadSchema(String schemaName, DataSourceInfo source) {
        return matches(schemaName, source.getIncludeSchemas(), source.getExcludeSchemas());
    }

    protected boolean shouldReadTable(String tableName, DataSourceInfo source) {
        return matches(tableName, source.getIncludeTables(), source.getExcludeTables());
    }

    private boolean matches(String value, List<String> includes, List<String> excludes) {
        String normalized = NameNormalizer.normalize(value);
        for (String exclude : excludes) {
            if (normalized != null && normalized.equals(NameNormalizer.normalize(exclude))) return false;
        }
        if (includes.isEmpty()) return true;
        for (String include : includes) {
            if (normalized != null && normalized.equals(NameNormalizer.normalize(include))) return true;
        }
        return false;
    }
}
