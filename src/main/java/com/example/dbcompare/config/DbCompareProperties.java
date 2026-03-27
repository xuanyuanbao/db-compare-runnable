package com.example.dbcompare.config;

import com.example.dbcompare.domain.model.CompareConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dbcompare")
public class DbCompareProperties extends CompareConfig {
}
