package com.example.dbcompare.infrastructure.reader;

import com.example.dbcompare.domain.enums.CompareObjectType;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.DatabaseMeta;
import com.example.dbcompare.domain.model.SourceTableLoadResult;

public interface MetadataReader {
    DatabaseMeta loadMetadata(DataSourceInfo dataSourceInfo, CompareObjectType objectType);

    default SourceTableLoadResult loadTableMetadata(DataSourceInfo dataSourceInfo, String schemaName, String tableName) {
        throw new UnsupportedOperationException("Targeted table loading is not supported by " + getClass().getSimpleName());
    }
}