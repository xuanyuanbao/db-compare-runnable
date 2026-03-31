package com.example.dbcompare.domain.model;

import com.example.dbcompare.domain.enums.CompareObjectType;

public class CompareOptions {
    private boolean compareNullable = true;
    private boolean compareDefaultValue = true;
    private boolean compareLength = true;
    private int sourceLoadThreads = 4;
    private CompareObjectType objectType = CompareObjectType.TABLE;

    public boolean isCompareNullable() { return compareNullable; }
    public void setCompareNullable(boolean compareNullable) { this.compareNullable = compareNullable; }
    public boolean isCompareDefaultValue() { return compareDefaultValue; }
    public void setCompareDefaultValue(boolean compareDefaultValue) { this.compareDefaultValue = compareDefaultValue; }
    public boolean isCompareLength() { return compareLength; }
    public void setCompareLength(boolean compareLength) { this.compareLength = compareLength; }
    public int getSourceLoadThreads() { return sourceLoadThreads; }
    public void setSourceLoadThreads(int sourceLoadThreads) { this.sourceLoadThreads = sourceLoadThreads; }
    public CompareObjectType getObjectType() { return objectType; }
    public void setObjectType(CompareObjectType objectType) { this.objectType = objectType == null ? CompareObjectType.TABLE : objectType; }
}
