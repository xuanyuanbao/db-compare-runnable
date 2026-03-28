package com.example.dbcompare.domain.model;

import java.util.ArrayList;
import java.util.List;

public class TableComparisonResult {
    private final List<DiffRecord> diffs = new ArrayList<>();
    private final List<ColumnComparisonRecord> columnRecords = new ArrayList<>();

    public List<DiffRecord> getDiffs() {
        return diffs;
    }

    public List<ColumnComparisonRecord> getColumnRecords() {
        return columnRecords;
    }
}
