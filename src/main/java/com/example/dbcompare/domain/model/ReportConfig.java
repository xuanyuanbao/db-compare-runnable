package com.example.dbcompare.domain.model;

public class ReportConfig {
    private final ManualConfirmationConfig manualConfirmation = new ManualConfirmationConfig();

    public ManualConfirmationConfig getManualConfirmation() {
        return manualConfirmation;
    }
}
