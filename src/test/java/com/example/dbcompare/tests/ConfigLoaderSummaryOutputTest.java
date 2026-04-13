package com.example.dbcompare.tests;

import com.example.dbcompare.config.ConfigLoader;
import com.example.dbcompare.domain.model.CompareConfig;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigLoaderSummaryOutputTest {
    @Test
    void parsesSummaryExcelOutputPath() {
        Properties properties = new Properties();
        properties.setProperty("source.count", "0");
        properties.setProperty("output.summaryExcelPath", "build/reports/custom-summary.xlsx");

        CompareConfig config = new ConfigLoader().parse(properties);

        assertEquals("build/reports/custom-summary.xlsx", config.getOutput().getSummaryExcelPath(),
                "legacy property parser should load the summary Excel output path");
    }
}