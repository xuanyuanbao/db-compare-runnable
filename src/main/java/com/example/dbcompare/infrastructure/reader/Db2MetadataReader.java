package com.example.dbcompare.infrastructure.reader;

import com.example.dbcompare.infrastructure.reader.dialect.Db2JdbcMetadataDialect;

public class Db2MetadataReader extends AbstractJdbcMetadataReader {
    public Db2MetadataReader() {
        super(new Db2JdbcMetadataDialect());
    }
}
