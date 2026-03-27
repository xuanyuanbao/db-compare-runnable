package com.example.dbcompare.infrastructure.reader;

import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.DatabaseMeta;

public interface MetadataReader {
    DatabaseMeta loadMetadata(DataSourceInfo dataSourceInfo);
}
