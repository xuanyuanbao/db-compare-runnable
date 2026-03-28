package com.example.dbcompare.tests;

import com.example.dbcompare.infrastructure.reader.dialect.As400JdbcMetadataDialect;
import com.example.dbcompare.infrastructure.reader.dialect.Db2JdbcMetadataDialect;
import com.example.dbcompare.infrastructure.reader.dialect.GaussJdbcMetadataDialect;

public final class JdbcMetadataDialectTest {
    private JdbcMetadataDialectTest() {
    }

    public static void run() {
        As400JdbcMetadataDialect as400Dialect = new As400JdbcMetadataDialect();
        Db2JdbcMetadataDialect db2Dialect = new Db2JdbcMetadataDialect();
        GaussJdbcMetadataDialect gaussDialect = new GaussJdbcMetadataDialect();

        TestSupport.assertTrue(!as400Dialect.shouldIncludeSchema("QSYS2"),
                "AS400 system schemas should be excluded");
        TestSupport.assertTrue(as400Dialect.shouldIncludeSchema("APP_LIB"),
                "AS400 user schemas should be included");

        TestSupport.assertTrue(!db2Dialect.shouldIncludeSchema("SYSCAT"),
                "DB2 system schemas should be excluded");
        TestSupport.assertTrue(db2Dialect.shouldIncludeSchema("APP_SCHEMA"),
                "DB2 user schemas should be included");

        TestSupport.assertTrue(!gaussDialect.shouldIncludeSchema("pg_catalog"),
                "openGauss internal schemas should be excluded");
        TestSupport.assertTrue(!gaussDialect.shouldIncludeSchema("information_schema"),
                "information_schema should be excluded");
        TestSupport.assertTrue(gaussDialect.shouldIncludeSchema("biz_schema"),
                "openGauss business schemas should be included");
    }
}
