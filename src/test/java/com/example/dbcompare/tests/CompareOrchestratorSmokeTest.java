package com.example.dbcompare.tests;

import com.example.dbcompare.config.ConfigLoader;
import com.example.dbcompare.config.ConfigValidator;
import com.example.dbcompare.domain.model.CompareConfig;
import com.example.dbcompare.infrastructure.output.CsvReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryReportWriter;
import com.example.dbcompare.service.CompareOrchestrator;
import com.example.dbcompare.service.MappingService;
import com.example.dbcompare.service.MetadataLoadService;
import com.example.dbcompare.service.TableCompareService;

import java.nio.file.Files;
import java.nio.file.Path;

public final class CompareOrchestratorSmokeTest {
    private CompareOrchestratorSmokeTest() {
    }

    public static void run() throws Exception {
        CompareConfig config = new ConfigLoader().load(Path.of("examples/demo/demo.properties"));
        new ConfigValidator().validate(config);

        Path tempDir = Files.createTempDirectory("dbcompare-tests-");
        config.getOutput().setCsvPath(tempDir.resolve("demo-report.csv").toString());
        config.getOutput().setSummaryPath(tempDir.resolve("demo-summary.txt").toString());

        CompareOrchestrator orchestrator = new CompareOrchestrator(
                new MetadataLoadService(),
                new MappingService(config.getMappings(), config.getTableMappings()),
                new TableCompareService(),
                new CsvReportWriter(),
                new SummaryReportWriter());

        TestSupport.assertEquals(4, orchestrator.execute(config).size(),
                "demo config should keep producing the same diff count");
        TestSupport.assertTrue(Files.exists(Path.of(config.getOutput().getCsvPath())),
                "smoke test should create csv report");
        TestSupport.assertTrue(Files.readString(Path.of(config.getOutput().getSummaryPath())).contains("diffCount=4"),
                "summary output should contain the expected diff count");
    }
}
