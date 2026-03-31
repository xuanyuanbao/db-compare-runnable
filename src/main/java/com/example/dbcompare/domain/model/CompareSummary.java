package com.example.dbcompare.domain.model;

import com.example.dbcompare.domain.enums.DiffType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CompareSummary {
    private int sourceCount;
    private int sourceSchemaCount;
    private int sourceTableCount;
    private int diffCount;
    private final Map<DiffType, Integer> diffTypeCount = new EnumMap<>(DiffType.class);

    public static CompareSummary start(int sourceCount) {
        CompareSummary summary = new CompareSummary();
        summary.sourceCount = sourceCount;
        return summary;
    }

    public void incrementSourceSchemaCount() {
        sourceSchemaCount++;
    }

    public void incrementSourceTableCount() {
        sourceTableCount++;
    }

    public void recordDiffs(List<DiffRecord> diffRecords) {
        diffCount += diffRecords.size();
        for (DiffRecord diffRecord : diffRecords) {
            diffTypeCount.merge(diffRecord.getDiffType(), 1, Integer::sum);
        }
    }

    public int getSourceCount() { return sourceCount; }
    public int getSourceSchemaCount() { return sourceSchemaCount; }
    public int getSourceTableCount() { return sourceTableCount; }
    public int getDiffCount() { return diffCount; }
    public Map<DiffType, Integer> getDiffTypeCount() { return diffTypeCount; }
}
