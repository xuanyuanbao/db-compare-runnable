package com.example.dbcompare.domain.model;

import com.example.dbcompare.domain.enums.CompareObjectType;
import com.example.dbcompare.domain.enums.CompareRelationMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CompareOptions {
    private boolean compareExists = true;
    private boolean compareType = true;
    private boolean compareNullable = false;
    private boolean compareDefaultValue = false;
    private boolean compareLength = true;
    private boolean sourceColumnMissingInTargetAffectResult = false;
    private int sourceLoadThreads = 4;
    private CompareObjectType objectType = CompareObjectType.TABLE;
    private CompareRelationMode relationMode = CompareRelationMode.TABLE_TO_TABLE;
    private final Map<String, List<String>> typeMappings = new LinkedHashMap<>();

    public boolean isCompareExists() { return compareExists; }
    public void setCompareExists(boolean compareExists) { this.compareExists = compareExists; }
    public boolean isCompareType() { return compareType; }
    public void setCompareType(boolean compareType) { this.compareType = compareType; }
    public boolean isCompareNullable() { return compareNullable; }
    public void setCompareNullable(boolean compareNullable) { this.compareNullable = compareNullable; }
    public boolean isCompareDefaultValue() { return compareDefaultValue; }
    public void setCompareDefaultValue(boolean compareDefaultValue) { this.compareDefaultValue = compareDefaultValue; }
    public boolean isCompareLength() { return compareLength; }
    public void setCompareLength(boolean compareLength) { this.compareLength = compareLength; }
    public boolean isSourceColumnMissingInTargetAffectResult() { return sourceColumnMissingInTargetAffectResult; }
    public void setSourceColumnMissingInTargetAffectResult(boolean sourceColumnMissingInTargetAffectResult) {
        this.sourceColumnMissingInTargetAffectResult = sourceColumnMissingInTargetAffectResult;
    }
    public int getSourceLoadThreads() { return sourceLoadThreads; }
    public void setSourceLoadThreads(int sourceLoadThreads) { this.sourceLoadThreads = sourceLoadThreads; }
    public CompareObjectType getObjectType() { return objectType; }
    public void setObjectType(CompareObjectType objectType) { this.objectType = objectType == null ? CompareObjectType.TABLE : objectType; }
    public CompareRelationMode getRelationMode() { return relationMode; }
    public void setRelationMode(CompareRelationMode relationMode) {
        this.relationMode = relationMode == null ? CompareRelationMode.TABLE_TO_TABLE : relationMode;
    }
    public Map<String, List<String>> getTypeMappings() { return typeMappings; }
    public void putTypeMapping(String canonicalType, List<String> aliases) {
        if (canonicalType == null || canonicalType.isBlank()) {
            return;
        }
        List<String> values = new ArrayList<>();
        values.add(canonicalType);
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias != null && !alias.isBlank()) {
                    values.add(alias.trim());
                }
            }
        }
        typeMappings.put(canonicalType.trim(), values);
    }
}
