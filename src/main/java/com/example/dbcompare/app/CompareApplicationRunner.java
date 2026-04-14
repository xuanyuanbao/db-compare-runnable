package com.example.dbcompare.app;

import com.example.dbcompare.config.ConfigValidator;
import com.example.dbcompare.config.DbCompareProperties;
import com.example.dbcompare.domain.model.CompareSummary;
import com.example.dbcompare.service.CompareOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CompareApplicationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CompareApplicationRunner.class);

    private final DbCompareProperties properties;
    private final ConfigValidator configValidator;
    private final CompareOrchestrator compareOrchestrator;

    public CompareApplicationRunner(DbCompareProperties properties,
                                    ConfigValidator configValidator,
                                    CompareOrchestrator compareOrchestrator) {
        this.properties = properties;
        this.configValidator = configValidator;
        this.compareOrchestrator = compareOrchestrator;
    }

    @Override
    public void run(ApplicationArguments args) {
        configValidator.validate(properties);
        CompareSummary summary = compareOrchestrator.execute(properties);
        log.info("Compare finished. diffCount={}", summary.getDiffCount());
        log.info("CSV report: {}", properties.getOutput().getCsvPath());
        log.info("Excel report: {}", properties.getOutput().getExcelPath());
        log.info("Target view lineage Excel report: {}", properties.getOutput().getTargetViewLineageExcelPath());
        log.info("SQL report: {} (table={})", properties.getOutput().getSqlPath(), properties.getOutput().getSqlTableName());
        log.info("Summary report: {}", properties.getOutput().getSummaryPath());
    }
}
