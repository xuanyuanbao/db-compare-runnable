package com.example.dbcompare.tests;

import com.example.dbcompare.domain.enums.CompareObjectType;
import com.example.dbcompare.domain.enums.DatabaseType;
import com.example.dbcompare.domain.model.CompareConfig;
import com.example.dbcompare.domain.model.CompareSummary;
import com.example.dbcompare.domain.model.DataSourceInfo;
import com.example.dbcompare.domain.model.DatabaseMeta;
import com.example.dbcompare.infrastructure.output.CsvReportWriter;
import com.example.dbcompare.infrastructure.output.ExcelReportWriter;
import com.example.dbcompare.infrastructure.output.SummaryReportWriter;
import com.example.dbcompare.service.CompareOrchestrator;
import com.example.dbcompare.service.MappingService;
import com.example.dbcompare.service.MetadataLoadService;
import com.example.dbcompare.service.TableCompareService;

import java.nio.file.Files;
import java.nio.file.Path;

public final class CompareObjectTypeRoutingTest {
    private CompareObjectTypeRoutingTest() {
    }

    public static void run() throws Exception {
        CompareConfig config = new CompareConfig();
        config.getOptions().setObjectType(CompareObjectType.VIEW);

        DataSourceInfo source = new DataSourceInfo();
        source.setSourceName("SRC_DB");
        source.setType(DatabaseType.SNAPSHOT);
        source.setSnapshotFile("examples/demo/source_as400_1.csv");
        config.getSources().add(source);

        DataSourceInfo target = new DataSourceInfo();
        target.setSourceName("TARGET_DB");
        target.setType(DatabaseType.SNAPSHOT);
        target.setSnapshotFile("examples/demo/gauss_target.csv");
        config.setTarget(target);

        Path tempDir = Files.createTempDirectory("dbcompare-object-type-");
        config.getOutput().setCsvPath(tempDir.resolve("report.csv").toString());
        config.getOutput().setExcelPath(tempDir.resolve("detail.xlsx").toString());
        config.getOutput().setSummaryPath(tempDir.resolve("summary.txt").toString());

        RecordingMetadataLoadService metadataLoadService = new RecordingMetadataLoadService();
        CompareOrchestrator orchestrator = new CompareOrchestrator(
                metadataLoadService,
                new MappingService(config.getMappings(), config.getTableMappings()),
                new TableCompareService(),
                new CsvReportWriter(),
                new ExcelReportWriter(),
                new SummaryReportWriter());

        CompareSummary summary = orchestrator.execute(config);

        TestSupport.assertEquals(1, metadataLoadService.sourceLoadCount,
                "source metadata should be loaded once");
        TestSupport.assertEquals(CompareObjectType.VIEW, metadataLoadService.targetObjectType,
                "target metadata should honor configured object type");
        TestSupport.assertEquals(0, summary.getDiffCount(),
                "empty recording metadata should not produce diffs");
    }

    private static final class RecordingMetadataLoadService extends MetadataLoadService {
        private CompareObjectType targetObjectType;
        private int sourceLoadCount;

        @Override
        public DatabaseMeta loadTarget(DataSourceInfo target, CompareObjectType objectType) {
            targetObjectType = objectType;
            return new DatabaseMeta(target.getSourceName());
        }

        @Override
        public DatabaseMeta loadSource(DataSourceInfo source) {
            sourceLoadCount++;
            return new DatabaseMeta(source.getSourceName());
        }
    }
}