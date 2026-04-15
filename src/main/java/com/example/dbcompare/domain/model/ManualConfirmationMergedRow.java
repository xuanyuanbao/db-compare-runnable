package com.example.dbcompare.domain.model;

import com.example.dbcompare.domain.enums.ManualConfirmationMatchStatus;

import java.util.ArrayList;
import java.util.List;

public class ManualConfirmationMergedRow {
    private ColumnComparisonRecord aiRecord;
    private String aiDiffCategory;
    private String aiDiffDetail;
    private String riskLevel;
    private String targetViewSchema;
    private String targetView;
    private String targetTableSchema;
    private String targetTable;
    private String owner;
    private String confirmResultRaw;
    private String confirmResultNormalized;
    private String comment;
    private ManualConfirmationMatchStatus matchStatus;
    private String matchBasis;
    private String matchedSheetName;
    private String matchedTableName;
    private String matchedDiffType;
    private String matchedDiffDetail;
    private int candidateCount;
    private final List<ManualConfirmationRecord> candidateRecords = new ArrayList<>();

    public ColumnComparisonRecord getAiRecord() {
        return aiRecord;
    }

    public void setAiRecord(ColumnComparisonRecord aiRecord) {
        this.aiRecord = aiRecord;
    }

    public String getAiDiffCategory() {
        return aiDiffCategory;
    }

    public void setAiDiffCategory(String aiDiffCategory) {
        this.aiDiffCategory = aiDiffCategory;
    }

    public String getAiDiffDetail() {
        return aiDiffDetail;
    }

    public void setAiDiffDetail(String aiDiffDetail) {
        this.aiDiffDetail = aiDiffDetail;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getTargetViewSchema() {
        return targetViewSchema;
    }

    public void setTargetViewSchema(String targetViewSchema) {
        this.targetViewSchema = targetViewSchema;
    }

    public String getTargetView() {
        return targetView;
    }

    public void setTargetView(String targetView) {
        this.targetView = targetView;
    }

    public String getTargetTableSchema() {
        return targetTableSchema;
    }

    public void setTargetTableSchema(String targetTableSchema) {
        this.targetTableSchema = targetTableSchema;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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

    public ManualConfirmationMatchStatus getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(ManualConfirmationMatchStatus matchStatus) {
        this.matchStatus = matchStatus;
    }

    public String getMatchBasis() {
        return matchBasis;
    }

    public void setMatchBasis(String matchBasis) {
        this.matchBasis = matchBasis;
    }

    public String getMatchedSheetName() {
        return matchedSheetName;
    }

    public void setMatchedSheetName(String matchedSheetName) {
        this.matchedSheetName = matchedSheetName;
    }

    public String getMatchedTableName() {
        return matchedTableName;
    }

    public void setMatchedTableName(String matchedTableName) {
        this.matchedTableName = matchedTableName;
    }

    public String getMatchedDiffType() {
        return matchedDiffType;
    }

    public void setMatchedDiffType(String matchedDiffType) {
        this.matchedDiffType = matchedDiffType;
    }

    public String getMatchedDiffDetail() {
        return matchedDiffDetail;
    }

    public void setMatchedDiffDetail(String matchedDiffDetail) {
        this.matchedDiffDetail = matchedDiffDetail;
    }

    public int getCandidateCount() {
        return candidateCount;
    }

    public void setCandidateCount(int candidateCount) {
        this.candidateCount = candidateCount;
    }

    public List<ManualConfirmationRecord> getCandidateRecords() {
        return candidateRecords;
    }
}
