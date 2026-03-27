package com.example.dbcompare.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DemoDataGenerator {
    public static void main(String[] args) throws IOException {
        Path demoDir = Path.of("examples/demo");
        Files.createDirectories(demoDir);
        Files.writeString(demoDir.resolve("source_as400_1.csv"), source1(), StandardCharsets.UTF_8);
        Files.writeString(demoDir.resolve("source_db2_1.csv"), source2(), StandardCharsets.UTF_8);
        Files.writeString(demoDir.resolve("gauss_target.csv"), target(), StandardCharsets.UTF_8);
        Files.writeString(demoDir.resolve("demo.properties"), properties(), StandardCharsets.UTF_8);
        System.out.println("Demo files generated under examples/demo");
    }

    private static String header() {
        return "databaseName,schemaName,tableName,columnName,dataType,length,nullable,defaultValue,ordinalPosition\n";
    }

    private static String source1() {
        return header() +
                "AS400_1,LEGACY_A,USER_INFO,ID,DECIMAL,18,NO,,1\n" +
                "AS400_1,LEGACY_A,USER_INFO,NAME,VARCHAR,50,YES,,2\n" +
                "AS400_1,LEGACY_A,USER_INFO,STATUS,CHAR,1,YES,A,3\n" +
                "AS400_1,LEGACY_A,ORDER_INFO,ORDER_ID,DECIMAL,18,NO,,1\n" +
                "AS400_1,LEGACY_A,ORDER_INFO,AMOUNT,DECIMAL,\"12,2\",NO,0,2\n";
    }

    private static String source2() {
        return header() +
                "DB2_1,LEGACY_B,CUSTOMER,ID,INTEGER,10,NO,,1\n" +
                "DB2_1,LEGACY_B,CUSTOMER,NICKNAME,VARCHAR,30,YES,,2\n";
    }

    private static String target() {
        return header() +
                "GAUSS,T_AS400_1,USER_INFO,ID,NUMERIC,18,NO,,1\n" +
                "GAUSS,T_AS400_1,USER_INFO,NAME,VARCHAR,60,YES,,2\n" +
                "GAUSS,T_AS400_1,USER_INFO,STATUS,CHAR,1,YES,B,3\n" +
                "GAUSS,T_AS400_1,ORDER_INFO,ORDER_ID,NUMERIC,18,NO,,1\n" +
                "GAUSS,T_AS400_1,ORDER_INFO,AMOUNT,NUMERIC,\"12,2\",NO,0,2\n" +
                "GAUSS,T_DB2_1,CUSTOMER,ID,INTEGER,10,NO,,1\n" +
                "GAUSS,T_DB2_1,CUSTOMER,NICKNAME,VARCHAR,50,YES,,2\n" +
                "GAUSS,T_DB2_1,CUSTOMER,EXTRA_COL,VARCHAR,10,YES,,3\n";
    }

    private static String properties() {
        return "source.count=2\n" +
                "source.1.name=AS400_1\n" +
                "source.1.type=SNAPSHOT\n" +
                "source.1.snapshotFile=examples/demo/source_as400_1.csv\n" +
                "source.2.name=DB2_1\n" +
                "source.2.type=SNAPSHOT\n" +
                "source.2.snapshotFile=examples/demo/source_db2_1.csv\n" +
                "target.name=GAUSS\n" +
                "target.type=SNAPSHOT\n" +
                "target.snapshotFile=examples/demo/gauss_target.csv\n" +
                "mapping.count=2\n" +
                "mapping.1.sourceDatabaseName=AS400_1\n" +
                "mapping.1.targetSchemaName=T_AS400_1\n" +
                "mapping.2.sourceDatabaseName=DB2_1\n" +
                "mapping.2.targetSchemaName=T_DB2_1\n" +
                "compare.options.compareNullable=true\n" +
                "compare.options.compareDefaultValue=true\n" +
                "compare.options.compareLength=true\n" +
                "compare.options.sourceLoadThreads=2\n" +
                "output.csvPath=build/reports/demo-compare-report.csv\n" +
                "output.summaryPath=build/reports/demo-compare-summary.txt\n";
    }
}
