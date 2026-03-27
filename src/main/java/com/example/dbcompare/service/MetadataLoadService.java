package com.example.dbcompare.service;

import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.DatabaseMeta;
import com.example.dbcompare.infrastructure.reader.As400MetadataReader;
import com.example.dbcompare.infrastructure.reader.Db2MetadataReader;
import com.example.dbcompare.infrastructure.reader.GaussMetadataReader;
import com.example.dbcompare.infrastructure.reader.MetadataReader;
import com.example.dbcompare.infrastructure.reader.SnapshotMetadataReader;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class MetadataLoadService {
    private final Map<DatabaseType, MetadataReader> readers = new HashMap<>();

    public MetadataLoadService() {
        readers.put(DatabaseType.AS400, new As400MetadataReader());
        readers.put(DatabaseType.DB2, new Db2MetadataReader());
        readers.put(DatabaseType.GAUSS, new GaussMetadataReader());
        readers.put(DatabaseType.SNAPSHOT, new SnapshotMetadataReader());
    }

    public Map<String, DatabaseMeta> loadSources(List<DataSourceInfo> sources, int threadCount) {
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, threadCount));
        try {
            Map<String, Future<DatabaseMeta>> futures = new LinkedHashMap<>();
            for (DataSourceInfo source : sources) {
                MetadataReader reader = getReader(source.getType());
                futures.put(source.getSourceName(), executor.submit(() -> reader.loadMetadata(source)));
            }
            Map<String, DatabaseMeta> result = new LinkedHashMap<>();
            for (Map.Entry<String, Future<DatabaseMeta>> entry : futures.entrySet()) {
                try {
                    result.put(entry.getKey(), entry.getValue().get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Metadata load interrupted: " + entry.getKey(), e);
                } catch (ExecutionException e) {
                    throw new IllegalStateException("Metadata load failed: " + entry.getKey(), e.getCause());
                }
            }
            return result;
        } finally {
            executor.shutdownNow();
        }
    }

    public DatabaseMeta loadTarget(DataSourceInfo target) {
        return getReader(target.getType()).loadMetadata(target);
    }

    private MetadataReader getReader(DatabaseType type) {
        MetadataReader reader = readers.get(type);
        if (reader == null) throw new IllegalArgumentException("Unsupported database type: " + type);
        return reader;
    }
}
