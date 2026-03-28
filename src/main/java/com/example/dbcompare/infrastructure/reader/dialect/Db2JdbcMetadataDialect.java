package com.example.dbcompare.infrastructure.reader.dialect;

import com.example.dbcompare.util.NameNormalizer;

import java.util.Set;

public class Db2JdbcMetadataDialect extends DefaultJdbcMetadataDialect {
    private static final Set<String> SYSTEM_SCHEMAS = Set.of(
            "NULLID",
            "SQLJ",
            "SYSCAT",
            "SYSFUN",
            "SYSIBM",
            "SYSIBMADM",
            "SYSIBMINTERNAL",
            "SYSIBMTS",
            "SYSPROC",
            "SYSSTAT",
            "SYSTOOLS"
    );

    @Override
    public boolean shouldIncludeSchema(String schemaName) {
        String normalized = NameNormalizer.normalize(schemaName);
        return normalized != null && !SYSTEM_SCHEMAS.contains(normalized);
    }
}
