package com.example.dbcompare.infrastructure.reader.dialect;

import com.example.dbcompare.util.NameNormalizer;

public class GaussJdbcMetadataDialect extends DefaultJdbcMetadataDialect {
    @Override
    public boolean shouldIncludeSchema(String schemaName) {
        String normalized = NameNormalizer.normalize(schemaName);
        if (normalized == null) {
            return false;
        }
        return !normalized.startsWith("PG_") && !"INFORMATION_SCHEMA".equals(normalized);
    }
}
