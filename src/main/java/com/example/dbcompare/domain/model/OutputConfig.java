package com.example.dbcompare.domain.model;

public class OutputConfig {
    public static final String DEFAULT_SQL_TABLE_NAME = "db_compare_detail_result";

    private String csvPath = "build/reports/compare-report.csv";
    private String excelPath = "build/reports/compare-detail.xlsx";
    private String summaryExcelPath = "build/reports/compare-summary.xlsx";
    private String sqlPath = "build/reports/compare-detail.sql";
    private String sqlTableName = DEFAULT_SQL_TABLE_NAME;
    private String summaryPath = "build/reports/compare-summary.txt";

    public String getCsvPath() { return csvPath; }
    public void setCsvPath(String csvPath) { this.csvPath = csvPath; }
    public String getExcelPath() { return excelPath; }
    public void setExcelPath(String excelPath) { this.excelPath = excelPath; }
    public String getSummaryExcelPath() { return summaryExcelPath; }
    public void setSummaryExcelPath(String summaryExcelPath) { this.summaryExcelPath = summaryExcelPath; }
    public String getSqlPath() { return sqlPath; }
    public void setSqlPath(String sqlPath) { this.sqlPath = sqlPath; }
    public String getSqlTableName() { return sqlTableName; }
    public void setSqlTableName(String sqlTableName) {
        this.sqlTableName = sqlTableName == null || sqlTableName.isBlank()
                ? DEFAULT_SQL_TABLE_NAME
                : sqlTableName.trim();
    }
    public String getSummaryPath() { return summaryPath; }
    public void setSummaryPath(String summaryPath) { this.summaryPath = summaryPath; }
}