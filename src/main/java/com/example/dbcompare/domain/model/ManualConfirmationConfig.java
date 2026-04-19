package com.example.dbcompare.domain.model;

import java.nio.file.Path;

public class ManualConfirmationConfig {
    private boolean enabled = false;
    private String excelDir;
    private String excelName;
    private String excelPath;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExcelDir() {
        return excelDir;
    }

    public void setExcelDir(String excelDir) {
        this.excelDir = excelDir;
    }

    public String getExcelName() {
        return excelName;
    }

    public void setExcelName(String excelName) {
        this.excelName = excelName;
    }

    public String getExcelPath() {
        return excelPath;
    }

    public void setExcelPath(String excelPath) {
        this.excelPath = excelPath;
    }

    public String resolveExcelPath() {
        if (excelPath != null && !excelPath.isBlank()) {
            return excelPath.trim();
        }
        if (excelDir != null && !excelDir.isBlank() && excelName != null && !excelName.isBlank()) {
            return Path.of(excelDir.trim(), excelName.trim()).toString();
        }
        return null;
    }
}
