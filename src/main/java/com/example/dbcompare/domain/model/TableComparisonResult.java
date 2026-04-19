package com.example.dbcompare.domain.model;

import java.util.ArrayList;
import java.util.List;

public class TableComparisonResult {
    private final List<DiffRecord> diffs = new ArrayList<>();
    private final List<DiffRecord> mainDiffs = new ArrayList<>();
    private final List<DiffRecord> infoDiffs = new ArrayList<>();
    private final List<ColumnComparisonRecord> columnRecords = new ArrayList<>();

    public List<DiffRecord> getDiffs() {
        return diffs;
    }

    public List<DiffRecord> getMainDiffs() {
        return mainDiffs;
    }

    public List<DiffRecord> getInfoDiffs() {
        return infoDiffs;
    }

    public List<ColumnComparisonRecord> getColumnRecords() {
        return columnRecords;
    }

    public void addDiff(DiffRecord diffRecord) {
        diffs.add(diffRecord);
        if (diffRecord.isAffectsResult()) {
            mainDiffs.add(diffRecord);
        } else {
            infoDiffs.add(diffRecord);
        }
    }
}
