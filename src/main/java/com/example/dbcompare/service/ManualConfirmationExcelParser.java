package com.example.dbcompare.service;

import com.example.dbcompare.domain.model.ManualConfirmationRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManualConfirmationExcelParser {
    private static final int HEADER_SCAN_LIMIT = 10;

    private static final String TABLE_NAME = "tableName";
    private static final String OWNER = "owner";
    private static final String DIFF_TYPE = "diffType";
    private static final String DIFF_DETAIL = "diffDetail";
    private static final String CONFIRM_RESULT = "confirmResult";
    private static final String COMMENT = "comment";

    private final DataFormatter dataFormatter = new DataFormatter(Locale.ROOT);

    public List<ManualConfirmationRecord> parse(Path path) {
        List<ManualConfirmationRecord> records = new ArrayList<>();
        try (InputStream inputStream = Files.newInputStream(path);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                ParsedHeader parsedHeader = findHeader(sheet);
                if (parsedHeader == null) {
                    continue;
                }
                for (int rowIndex = parsedHeader.rowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null || isRowEmpty(row)) {
                        continue;
                    }
                    ManualConfirmationRecord record = buildRecord(path, sheet, rowIndex, row, parsedHeader.columnMapping);
                    if (record == null) {
                        continue;
                    }
                    records.add(record);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read manual confirmation Excel: " + path, e);
        }
        return records;
    }

    private ParsedHeader findHeader(Sheet sheet) {
        int lastRow = Math.min(sheet.getLastRowNum(), HEADER_SCAN_LIMIT);
        for (int rowIndex = 0; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, Integer> mapping = new LinkedHashMap<>();
            for (Cell cell : row) {
                String normalized = normalizeHeader(cellValue(cell));
                if (matches(normalized, "表名", "表名称", "对象名", "视图名", "tablename")) {
                    mapping.putIfAbsent(TABLE_NAME, cell.getColumnIndex());
                } else if (matches(normalized, "责任人", "负责人", "owner")) {
                    mapping.putIfAbsent(OWNER, cell.getColumnIndex());
                } else if (matches(normalized, "不一致类型", "差异类型", "问题类型")) {
                    mapping.putIfAbsent(DIFF_TYPE, cell.getColumnIndex());
                } else if (matches(normalized, "不一致详细", "不一致明细", "差异详情", "问题详情", "详细")) {
                    mapping.putIfAbsent(DIFF_DETAIL, cell.getColumnIndex());
                } else if (matches(normalized, "确认结果", "确认结论", "结论")) {
                    mapping.putIfAbsent(CONFIRM_RESULT, cell.getColumnIndex());
                } else if (matches(normalized, "附加说明", "备注", "说明", "补充说明")) {
                    mapping.putIfAbsent(COMMENT, cell.getColumnIndex());
                }
            }
            if (mapping.containsKey(TABLE_NAME) && mapping.containsKey(DIFF_TYPE)) {
                return new ParsedHeader(rowIndex, mapping);
            }
        }
        return null;
    }

    private ManualConfirmationRecord buildRecord(Path sourcePath,
                                                 Sheet sheet,
                                                 int rowIndex,
                                                 Row row,
                                                 Map<String, Integer> columnMapping) {
        String tableName = cellValue(row.getCell(columnMapping.get(TABLE_NAME)));
        String diffType = cellValue(row.getCell(columnMapping.get(DIFF_TYPE)));
        String diffDetail = cellValue(row.getCell(columnMapping.get(DIFF_DETAIL)));
        String owner = cellValue(row.getCell(columnMapping.get(OWNER)));
        String confirmResult = cellValue(row.getCell(columnMapping.get(CONFIRM_RESULT)));
        String comment = cellValue(row.getCell(columnMapping.get(COMMENT)));

        if (isBlank(tableName) && isBlank(diffType) && isBlank(diffDetail) && isBlank(owner) && isBlank(confirmResult) && isBlank(comment)) {
            return null;
        }

        ManualConfirmationRecord record = new ManualConfirmationRecord();
        record.setSourceFileName(sourcePath.getFileName() == null ? sourcePath.toString() : sourcePath.getFileName().toString());
        record.setSheetName(sheet.getSheetName());
        record.setRowNumber(rowIndex + 1);
        record.setTableName(trimToNull(tableName));
        record.setOwner(trimToNull(owner));
        record.setDiffTypeRaw(trimToNull(diffType));
        record.setDiffDetailRaw(trimToNull(diffDetail));
        record.setConfirmResultRaw(trimToNull(confirmResult));
        record.setComment(trimToNull(comment));
        record.setNormalizedTableName(normalizeName(tableName));
        record.setNormalizedDiffCategory(normalizeDiffCategory(diffType));
        record.setConfirmResultNormalized(normalizeConfirmResult(confirmResult));
        return record;
    }

    public String normalizeDiffCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return "OTHER";
        }
        String normalized = raw.replace(" ", "").toUpperCase(Locale.ROOT);
        if (normalized.contains("类型") || normalized.contains("TYPE")) {
            return "TYPE";
        }
        if (normalized.contains("长度") || normalized.contains("LENGTH")) {
            return "LENGTH";
        }
        if (normalized.contains("默认") || normalized.contains("DEFAULT")) {
            return "DEFAULT";
        }
        if (normalized.contains("可空") || normalized.contains("NULLABLE") || normalized.contains("NULL")) {
            return "NULLABLE";
        }
        if (normalized.contains("数量") || normalized.contains("缺") || normalized.contains("存在") || normalized.contains("MISSING")) {
            return "EXISTENCE";
        }
        return "OTHER";
    }

    public String normalizeConfirmResult(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.replace(" ", "").toUpperCase(Locale.ROOT);
        if (normalized.contains("无影响")) {
            return "无影响";
        }
        if (normalized.contains("修复")) {
            return "已修复";
        }
        if (normalized.contains("废弃")) {
            return "已废弃";
        }
        if (normalized.contains("待确认") || normalized.contains("待定")) {
            return "待确认";
        }
        return "其他";
    }

    public String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (char ch : raw.trim().toUpperCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(ch) || ch == '_') {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private boolean matches(String normalizedValue, String... aliases) {
        for (String alias : aliases) {
            if (normalizeHeader(alias).equals(normalizedValue)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeHeader(String raw) {
        return raw == null ? "" : raw.replace(" ", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace("\t", "")
                .replace("：", ":")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private String cellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell);
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (!cellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private record ParsedHeader(int rowIndex, Map<String, Integer> columnMapping) {
    }
}
