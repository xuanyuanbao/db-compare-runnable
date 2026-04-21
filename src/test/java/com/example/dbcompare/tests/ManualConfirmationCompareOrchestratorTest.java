package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.CompareMode;
import com.example.dbcompare.domain.enums.CompareObjectType;
import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.domain.model.CompareConfig;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.SchemaMapping;
import com.example.dbcompare.domain.model.TableMapping;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualConfirmationCompareOrchestratorTest {
    @Test
    void generatesManualConfirmationMergedWorkbook(@TempDir Path tempDir) throws Exception {
        Path sourceSnapshot = tempDir.resolve("source.csv");
        Path targetSnapshot = tempDir.resolve("target.csv");
        Path lineageCsv = tempDir.resolve("lineage.csv");
        Path manualWorkbook = tempDir.resolve("manual.xlsx");
        Files.writeString(sourceSnapshot,
                "databaseName,schemaName,tableName,columnName,dataType,length,nullable,defaultValue,ordinalPosition\n" +
                        "DB2_A,LEGACY_A,CUSTOMER,ID,INTEGER,10,NO,,1\n" +
                        "DB2_A,LEGACY_A,CUSTOMER,NAME,VARCHAR,50,YES,,2\n");
        Files.writeString(targetSnapshot,
                "databaseName,schemaName,tableName,columnName,dataType,length,nullable,defaultValue,ordinalPosition\n" +
                        "GAUSS,VIEW_APP,CUSTOMER_VIEW,ID,INTEGER,10,NO,,1\n" +
                        "GAUSS,VIEW_APP,CUSTOMER_VIEW,NAME,VARCHAR,60,YES,,2\n");
        Files.writeString(lineageCsv,
                "targetViewSchema,targetView,targetTableSchema,targetTable\n" +
                        "VIEW_APP,CUSTOMER_VIEW,ODS,CUSTOMER_BASE\n");
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(manualWorkbook)) {
            var sheet = workbook.createSheet("as400");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("表名");
            header.createCell(1).setCellValue("责任人");
            header.createCell(2).setCellValue("不一致类型");
            header.createCell(3).setCellValue("不一致详细");
            header.createCell(4).setCellValue("确认结果");
            header.createCell(5).setCellValue("附加说明");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("CUSTOMER");
            row.createCell(1).setCellValue("张三");
            row.createCell(2).setCellValue("列长度不一致");
            row.createCell(3).setCellValue("[NAME](50,60)");
            row.createCell(4).setCellValue("无影响");
            row.createCell(5).setCellValue("来自测试组");
            workbook.write(outputStream);
        }

        CompareConfig config = new CompareConfig();
        config.setMode(CompareMode.TARGET_DRIVEN);
        DataSourceInfo source = new DataSourceInfo();
        source.setSourceName("DB2_A");
        source.setType(DatabaseType.SNAPSHOT);
        source.setSchema("LEGACY_A");
        source.setSnapshotFile(sourceSnapshot.toString());
        config.getSources().add(source);
        DataSourceInfo target = new DataSourceInfo();
        target.setSourceName("GAUSS");
        target.setType(DatabaseType.SNAPSHOT);
        target.setViewOnly(true);
        target.setSnapshotFile(targetSnapshot.toString());
        target.setViewLineageFile(lineageCsv.toString());
        config.setTarget(target);
        config.getOptions().setObjectType(CompareObjectType.VIEW);
        config.getOptions().setLengthTargetLongerAffectResult(true);
        config.getReport().getManualConfirmation().setEnabled(true);
        config.getReport().getManualConfirmation().setExcelPath(manualWorkbook.toString());
        config.getOutput().setManualConfirmationExcelPath(tempDir.resolve("merged.xlsx").toString());
        config.getOutput().setCsvPath(tempDir.resolve("detail.csv").toString());
        config.getOutput().setExcelPath(tempDir.resolve("detail.xlsx").toString());
        config.getOutput().setSummaryExcelPath(tempDir.resolve("summary.xlsx").toString());
        config.getOutput().setTargetViewLineageExcelPath(tempDir.resolve("lineage.xlsx").toString());
        config.getOutput().setSqlPath(tempDir.resolve("detail.sql").toString());
        config.getOutput().setSummaryPath(tempDir.resolve("summary.txt").toString());

        SchemaMapping schemaMapping = new SchemaMapping();
        schemaMapping.setSourceDatabaseName("DB2_A");
        schemaMapping.setTargetSchemaName("VIEW_APP");
        config.getMappings().add(schemaMapping);
        TableMapping tableMapping = new TableMapping();
        tableMapping.setSourceDatabaseName("DB2_A");
        tableMapping.setSourceSchemaName("LEGACY_A");
        tableMapping.setSourceTableName("CUSTOMER");
        tableMapping.setTargetSchemaName("VIEW_APP");
        tableMapping.setTargetTableName("CUSTOMER_VIEW");
        config.getTableMappings().add(tableMapping);

        CompareOrchestrator orchestrator = new CompareOrchestrator(
                new MetadataLoadService(),
                new MappingService(config.getMappings(), config.getTableMappings()),
                new TableCompareService(),
                new CsvReportWriter(),
                new ExcelReportWriter(),
                new ManualConfirmationMergedExcelWriter(),
                new SqlReportWriter(),
                new TargetViewLineageExcelWriter(),
                new SummaryReportWriter(),
                new SummaryExcelReportWriter(),
                new ManualConfirmationMergeService(new ManualConfirmationExcelParser()),
                new TargetViewLineageService());

        orchestrator.execute(config);

        assertTrue(Files.exists(Path.of(config.getOutput().getManualConfirmationExcelPath())), "manual confirmation workbook should be generated");
        try (var inputStream = Files.newInputStream(Path.of(config.getOutput().getManualConfirmationExcelPath()));
             var workbook = new XSSFWorkbook(inputStream)) {
            assertEquals("人工确认融合明细", workbook.getSheetAt(1).getSheetName(), "merged workbook should include merged detail sheet");
            assertEquals("张三", workbook.getSheet("人工确认融合明细").getRow(1).getCell(11).getStringCellValue(), "merged workbook should inherit owner");
            assertEquals("无影响", workbook.getSheet("人工确认融合明细").getRow(1).getCell(13).getStringCellValue(), "merged workbook should include normalized confirmation result");
            assertEquals("候选数", workbook.getSheet("人工确认融合明细").getRow(0).getCell(17).getStringCellValue(), "merged workbook should include candidate count");
        }
    }
}
