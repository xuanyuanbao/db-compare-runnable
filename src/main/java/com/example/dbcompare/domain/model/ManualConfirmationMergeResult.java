package com.example.dbcompare.domain.model;

import java.util.ArrayList;
import java.util.List;

public class ManualConfirmationMergeResult {
    private final List<ManualConfirmationMergedRow> mergedRows = new ArrayList<>();
    private final List<ManualConfirmationRecord> unmatchedTestRecords = new ArrayList<>();
    private final List<ManualConfirmationRecord> ambiguousTestRecords = new ArrayList<>();

    public List<ManualConfirmationMergedRow> getMergedRows() {
        return mergedRows;
    }

    public List<ManualConfirmationRecord> getUnmatchedTestRecords() {
        return unmatchedTestRecords;
    }

    public List<ManualConfirmationRecord> getAmbiguousTestRecords() {
        return ambiguousTestRecords;
    }
}
