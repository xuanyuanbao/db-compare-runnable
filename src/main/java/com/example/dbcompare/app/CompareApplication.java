package com.example.dbcompare.app;

import com.example.dbcompare.config.DbCompareProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DbCompareProperties.class)
public class CompareApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompareApplication.class, args);
    }
}
