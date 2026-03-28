package com.example.dbcompare.domain.model;

public class OutputConfig {
    private String csvPath = "build/reports/compare-report.csv";
    private String excelPath = "build/reports/compare-detail.xlsx";
    private String summaryPath = "build/reports/compare-summary.txt";

    public String getCsvPath() { return csvPath; }
    public void setCsvPath(String csvPath) { this.csvPath = csvPath; }
    public String getExcelPath() { return excelPath; }
    public void setExcelPath(String excelPath) { this.excelPath = excelPath; }
    public String getSummaryPath() { return summaryPath; }
    public void setSummaryPath(String summaryPath) { this.summaryPath = summaryPath; }
}
