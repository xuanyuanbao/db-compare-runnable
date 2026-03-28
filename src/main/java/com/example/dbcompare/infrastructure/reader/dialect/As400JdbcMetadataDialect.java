package com.example.dbcompare.infrastructure.reader.dialect;

import com.example.dbcompare.util.NameNormalizer;

import java.util.Set;

public class As400JdbcMetadataDialect extends DefaultJdbcMetadataDialect {
    private static final Set<String> SYSTEM_SCHEMAS = Set.of(
            "QDOC",
            "QGPL",
            "QHTTPSVR",
            "QIWS",
            "QRECOVERY",
            "QSPL",
            "QSYS",
            "QSYS2",
            "QSYSINC",
            "QTEMP",
            "QUSRBRM",
            "QUSRSYS"
    );

    @Override
    public boolean shouldIncludeSchema(String schemaName) {
        String normalized = NameNormalizer.normalize(schemaName);
        return normalized != null && !SYSTEM_SCHEMAS.contains(normalized);
    }
}
