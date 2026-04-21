package com.example.dbcompare.config;

import com.example.dbcompare.domain.model.CompareConfig;
import com.example.dbcompare.domain.model.CompareOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TypeRuleFileLoader {
    public static final String DEFAULT_RULE_FILE = "classpath:type-equality-rules.properties";

    public void apply(CompareConfig config) {
        if (config == null || config.getOptions() == null) {
            return;
        }
        apply(config.getOptions());
    }

    public void apply(CompareOptions options) {
        if (options == null) {
            return;
        }
        Map<String, List<String>> inlineMappings = snapshotMappings(options.getTypeMappings());
        Properties ruleProperties = loadProperties(options.getTypeRuleFile());
        options.clearTypeMappings();
        for (Map.Entry<Object, Object> entry : ruleProperties.entrySet()) {
            String canonicalType = entry.getKey() == null ? null : entry.getKey().toString().trim();
            if (canonicalType == null || canonicalType.isBlank()) {
                continue;
            }
            options.putTypeMapping(canonicalType, splitAliases(entry.getValue() == null ? null : entry.getValue().toString()));
        }
        for (Map.Entry<String, List<String>> entry : inlineMappings.entrySet()) {
            options.putTypeMapping(entry.getKey(), entry.getValue());
        }
    }

    public boolean exists(String location) {
        try (InputStream ignored = open(location)) {
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private Properties loadProperties(String location) {
        Properties properties = new Properties();
        try (InputStream in = open(location)) {
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load type rule file: " + normalizeLocation(location), e);
        }
    }

    private InputStream open(String location) throws IOException {
        String resolved = normalizeLocation(location);
        if (resolved.startsWith("classpath:")) {
            String classpathLocation = resolved.substring("classpath:".length());
            if (classpathLocation.startsWith("/")) {
                classpathLocation = classpathLocation.substring(1);
            }
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream in = classLoader == null ? null : classLoader.getResourceAsStream(classpathLocation);
            if (in == null) {
                throw new IOException("Classpath resource not found: " + classpathLocation);
            }
            return in;
        }
        if (resolved.startsWith("file:")) {
            return Files.newInputStream(Path.of(resolved.substring("file:".length())));
        }
        return Files.newInputStream(Path.of(resolved));
    }

    private String normalizeLocation(String location) {
        return location == null || location.isBlank() ? DEFAULT_RULE_FILE : location.trim();
    }

    private Map<String, List<String>> snapshotMappings(Map<String, List<String>> mappings) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        if (mappings == null || mappings.isEmpty()) {
            return copy;
        }
        for (Map.Entry<String, List<String>> entry : mappings.entrySet()) {
            copy.put(entry.getKey(), entry.getValue() == null ? List.of() : new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    private List<String> splitAliases(String raw) {
        List<String> values = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return values;
        }
        for (String part : raw.split(",")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }
}
