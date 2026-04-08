package com.example.dbcompare.infrastructure.reader;

import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.infrastructure.reader.dialect.As400JdbcMetadataDialect;

import java.sql.Connection;
import java.sql.SQLException;

public class As400MetadataReader extends AbstractJdbcMetadataReader {
    private final As400SourceResolver sourceResolver = new As400SourceResolver();

    public As400MetadataReader() {
        super(new As400JdbcMetadataDialect());
    }

    @Override
    protected ResolvedTableReference resolveTableReference(Connection connection,
                                                           DataSourceInfo dataSourceInfo,
                                                           String schemaName,
                                                           String tableName) {
        try {
            As400SourceResolver.Resolution resolution = sourceResolver.resolve(connection, schemaName, tableName);
            if (resolution.isAmbiguous()) {
                return new ResolvedTableReference(resolution.getSchemaName(), tableName, true, resolution.getMessage());
            }
            if (resolution.isMissing()) {
                return new ResolvedTableReference(schemaName, tableName, false, resolution.getMessage());
            }
            return new ResolvedTableReference(resolution.getSchemaName(), tableName, false, null);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to resolve AS400 source schema for table: " + tableName, e);
        }
    }
}