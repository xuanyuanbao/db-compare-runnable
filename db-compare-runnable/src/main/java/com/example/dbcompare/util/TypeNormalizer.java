package com.example.dbcompare.util;

import com.example.dbcompare.domain.enums.DatabaseType;

public class TypeNormalizer {
    public String normalize(DatabaseType databaseType, String rawType) {
        if (rawType == null) {
            return null;
        }
        String type = rawType.trim().toUpperCase();
        if ("CHARACTER VARYING".equals(type)) return "VARCHAR";
        if ("CHARACTER".equals(type)) return "CHAR";
        if ("DECIMAL".equals(type)) return "NUMERIC";
        if (databaseType == DatabaseType.GAUSS && "INT4".equals(type)) return "INTEGER";
        if (databaseType == DatabaseType.AS400 && "GRAPHIC".equals(type)) return "CHAR";
        if (databaseType == DatabaseType.AS400 && "VARGRAPHIC".equals(type)) return "VARCHAR";
        return type;
    }
}
