package com.example.dbcompare.infrastructure.reader;

import com.example.dbcompare.infrastructure.reader.dialect.GaussJdbcMetadataDialect;

public class GaussMetadataReader extends AbstractJdbcMetadataReader {
    public GaussMetadataReader() {
        super(new GaussJdbcMetadataDialect());
    }
}
