package com.example.dbcompare.config;

import com.example.dbcompare.infrastructure.output.CsvReportWriter;
import com.example.dbcompare.infrastructure.output.ExcelReportWriter;
import com.example.dbcompare.infrastructure.output.ManualConfirmationMergedExcelWriter;
import com.example.dbcompare.infrastructure.output.SqlReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryExcelReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryReportWriter;
import com.example.dbcompare.infrastructure.output.TargetViewLineageExcelWriter;
import com.example.dbcompare.service.CompareOrchestrator;
import com.example.dbcompare.service.ManualConfirmationExcelParser;
import com.example.dbcompare.service.ManualConfirmationMergeService;
import com.example.dbcompare.service.MappingService;
import com.example.dbcompare.service.MetadataLoadService;
import com.example.dbcompare.service.TableCompareService;
import com.example.dbcompare.service.TargetViewLineageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompareBeansConfig {
    @Bean
    public ConfigValidator configValidator() {
        return new ConfigValidator();
    }

    @Bean
    public TypeRuleFileLoader typeRuleFileLoader() {
        return new TypeRuleFileLoader();
    }

    @Bean
    public MetadataLoadService metadataLoadService() {
        return new MetadataLoadService();
    }

    @Bean
    public TableCompareService tableCompareService() {
        return new TableCompareService();
    }

    @Bean
    public CsvReportWriter csvReportWriter() {
        return new CsvReportWriter();
    }

    @Bean
    public SummaryReportWriter summaryReportWriter() {
        return new SummaryReportWriter();
    }

    @Bean
    public ExcelReportWriter excelReportWriter() {
        return new ExcelReportWriter();
    }

    @Bean
    public SummaryExcelReportWriter summaryExcelReportWriter() {
        return new SummaryExcelReportWriter();
    }

    @Bean
    public SqlReportWriter sqlReportWriter() {
        return new SqlReportWriter();
    }

    @Bean
    public ManualConfirmationMergedExcelWriter manualConfirmationMergedExcelWriter() {
        return new ManualConfirmationMergedExcelWriter();
    }

    @Bean
    public TargetViewLineageExcelWriter targetViewLineageExcelWriter() {
        return new TargetViewLineageExcelWriter();
    }

    @Bean
    public ManualConfirmationExcelParser manualConfirmationExcelParser() {
        return new ManualConfirmationExcelParser();
    }

    @Bean
    public ManualConfirmationMergeService manualConfirmationMergeService(ManualConfirmationExcelParser parser) {
        return new ManualConfirmationMergeService(parser);
    }

    @Bean
    public TargetViewLineageService targetViewLineageService() {
        return new TargetViewLineageService();
    }

    @Bean
    public MappingService mappingService(DbCompareProperties properties) {
        return new MappingService(properties.getMappings(), properties.getTableMappings());
    }

    @Bean
    public CompareOrchestrator compareOrchestrator(MetadataLoadService metadataLoadService,
                                                   MappingService mappingService,
                                                   TableCompareService tableCompareService,
                                                   CsvReportWriter csvReportWriter,
                                                   ExcelReportWriter excelReportWriter,
                                                   ManualConfirmationMergedExcelWriter manualConfirmationMergedExcelWriter,
                                                   SqlReportWriter sqlReportWriter,
                                                   TargetViewLineageExcelWriter targetViewLineageExcelWriter,
                                                   SummaryReportWriter summaryReportWriter,
                                                   SummaryExcelReportWriter summaryExcelReportWriter,
                                                   ManualConfirmationMergeService manualConfirmationMergeService,
                                                   TargetViewLineageService targetViewLineageService) {
        return new CompareOrchestrator(
                metadataLoadService,
                mappingService,
                tableCompareService,
                csvReportWriter,
                excelReportWriter,
                manualConfirmationMergedExcelWriter,
                sqlReportWriter,
                targetViewLineageExcelWriter,
                summaryReportWriter,
                summaryExcelReportWriter,
                manualConfirmationMergeService,
                targetViewLineageService);
    }
}
