package com.example.dbcompare.config;

import com.example.dbcompare.infrastructure.output.CsvReportWriter;
import com.example.dbcompare.infrastructure.output.ExcelReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryReportWriter;
import com.example.dbcompare.service.CompareOrchestrator;
import com.example.dbcompare.service.MappingService;
import com.example.dbcompare.service.MetadataLoadService;
import com.example.dbcompare.service.TableCompareService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompareBeansConfig {
    @Bean
    public ConfigValidator configValidator() {
        return new ConfigValidator();
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
    public MappingService mappingService(DbCompareProperties properties) {
        return new MappingService(properties.getMappings(), properties.getTableMappings());
    }

    @Bean
    public CompareOrchestrator compareOrchestrator(MetadataLoadService metadataLoadService,
                                                   MappingService mappingService,
                                                   TableCompareService tableCompareService,
                                                   CsvReportWriter csvReportWriter,
                                                   ExcelReportWriter excelReportWriter,
                                                   SummaryReportWriter summaryReportWriter) {
        return new CompareOrchestrator(
                metadataLoadService,
                mappingService,
                tableCompareService,
                csvReportWriter,
                excelReportWriter,
                summaryReportWriter);
    }
}
