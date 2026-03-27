package com.example.dbcompare.app;

import com.example.dbcompare.config.ConfigLoader;
import com.example.dbcompare.config.ConfigValidator;
import com.example.dbcompare.domain.model.CompareConfig;
import com.example.dbcompare.infrastructure.output.CsvReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryReportWriter;
import com.example.dbcompare.service.CompareOrchestrator;
import com.example.dbcompare.service.MappingService;
import com.example.dbcompare.service.MetadataLoadService;
import com.example.dbcompare.service.TableCompareService;

import java.nio.file.Path;
import java.util.List;

public class CompareApplication {
    public static void main(String[] args) {
        ConfigLoader configLoader = new ConfigLoader();
        CompareConfig compareConfig;
        if (args.length == 0) {
            compareConfig = configLoader.loadDefault();
            System.out.println("No config argument provided. Using default config: " + ConfigLoader.DEFAULT_CONFIG_PATH);
        } else {
            Path configPath = Path.of(args[0]);
            compareConfig = configLoader.load(configPath);
            System.out.println("Using config: " + configPath.toAbsolutePath());
        }
        new ConfigValidator().validate(compareConfig);

        CompareOrchestrator orchestrator = new CompareOrchestrator(
                new MetadataLoadService(),
                new MappingService(compareConfig.getMappings(), compareConfig.getTableMappings()),
                new TableCompareService(),
                new CsvReportWriter(),
                new SummaryReportWriter());

        List<?> diffs = orchestrator.execute(compareConfig);
        System.out.println("Compare finished. diffCount=" + diffs.size());
        System.out.println("CSV report: " + compareConfig.getOutput().getCsvPath());
        System.out.println("Summary report: " + compareConfig.getOutput().getSummaryPath());
    }

    private static void printUsage() {
        System.out.println("Usage: java -cp out com.example.dbcompare.app.CompareApplication [config.properties]");
        System.out.println("Without arguments it loads src/main/resources/jdbc.properties");
        System.out.println("Example: java -cp out com.example.dbcompare.app.CompareApplication examples/demo/demo.properties");
    }
}
