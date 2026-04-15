package com.example.dbcompare.domain.model;

public class ManualConfirmationRecord {
    private String sourceFileName;
    private String sheetName;
    private int rowNumber;
    private String tableName;
    private String owner;
    private String diffTypeRaw;
    private String diffDetailRaw;
    private String confirmResultRaw;
    private String confirmResultNormalized;
    private String comment;
    private String normalizedTableName;
    private String normalizedDiffCategory;

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDiffTypeRaw() {
        return diffTypeRaw;
    }

    public void setDiffTypeRaw(String diffTypeRaw) {
        this.diffTypeRaw = diffTypeRaw;
    }

    public String getDiffDetailRaw() {
        return diffDetailRaw;
    }

    public void setDiffDetailRaw(String diffDetailRaw) {
        this.diffDetailRaw = diffDetailRaw;
    }

    public String getConfirmResultRaw() {
        return confirmResultRaw;
    }

    public void setConfirmResultRaw(String confirmResultRaw) {
        this.confirmResultRaw = confirmResultRaw;
    }

    public String getConfirmResultNormalized() {
        return confirmResultNormalized;
    }

    public void setConfirmResultNormalized(String confirmResultNormalized) {
        this.confirmResultNormalized = confirmResultNormalized;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getNormalizedTableName() {
        return normalizedTableName;
    }

    public void setNormalizedTableName(String normalizedTableName) {
        this.normalizedTableName = normalizedTableName;
    }

    public String getNormalizedDiffCategory() {
        return normalizedDiffCategory;
    }

    public void setNormalizedDiffCategory(String normalizedDiffCategory) {
        this.normalizedDiffCategory = normalizedDiffCategory;
    }
}
