package com.example.dbcompare.infrastructure.reader;

import com.example.dbcompare.infrastructure.reader.dialect.As400JdbcMetadataDialect;

public class As400MetadataReader extends AbstractJdbcMetadataReader {
    public As400MetadataReader() {
        super(new As400JdbcMetadataDialect());
    }
}
