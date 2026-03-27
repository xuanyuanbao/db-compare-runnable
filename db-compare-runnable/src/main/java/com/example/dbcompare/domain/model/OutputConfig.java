package com.example.dbcompare.domain.model;

public class OutputConfig {
    private String csvPath = "build/reports/compare-report.csv";
    private String summaryPath = "build/reports/compare-summary.txt";

    public String getCsvPath() { return csvPath; }
    public void setCsvPath(String csvPath) { this.csvPath = csvPath; }
    public String getSummaryPath() { return summaryPath; }
    public void setSummaryPath(String summaryPath) { this.summaryPath = summaryPath; }
}
