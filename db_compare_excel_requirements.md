# 数据对比结果汇总（Excel 输出）需求说明书

## 1. 背景

当前系统已经能够输出字段级明细数据，但明细粒度过细，不方便直接用于汇报和表级统计分析。

现在需要基于原始字段级对比结果，新增一份更适合管理和分析使用的汇总 Excel，并补充按表级统计的存在性、类型、长度、默认值汇总能力。

## 2. 目标

生成一份汇总 Excel 文件，至少包含以下内容：

1. 总体统计
2. 字段存在情况汇总（按表级）
3. 字段类型汇总（按表级）
4. 字段长度汇总（按表级）
5. 字段默认值汇总（按表级）
6. 表级状态明细
7. 其他辅助汇总与明细

## 3. 输入数据

输入仍然使用当前系统的字段级对比明细记录，关键字段包括：

- `sourceDatabase`
- `sourceSchema`
- `sourceTable`
- `targetSchema`
- `targetTable`
- `columnName`
- `sourceExists`
- `targetExists`
- `diffTypes`
- `message`

## 4. 表级聚合规则

表级分组键：

- `sourceDatabase`
- `sourceSchema`
- `sourceTable`
- `targetSchema`
- `targetTable`

### 4.1 字段存在情况汇总规则

参考 SQL 逻辑：

```sql
CASE
    WHEN SUM(CASE WHEN sourceExists != 'Y' OR targetExists != 'Y' THEN 1 ELSE 0 END) > 0
    THEN 'NOT_FULL_EXISTS'
    ELSE 'FULL_EXISTS'
END
```

在 Java 实现中含义为：

- 同一张表下，只要任意一条字段明细出现 `sourceExists=false` 或 `targetExists=false`，则该表状态为 `NOT_FULL_EXISTS`
- 若该表所有字段明细都满足 `sourceExists=true` 且 `targetExists=true`，则该表状态为 `FULL_EXISTS`

### 4.2 类型汇总规则（按表级）

- 同一张表下，只要任意字段出现 `COLUMN_TYPE_MISMATCH`，则该表记为 `TYPE_MISMATCH`
- 否则记为 `TYPE_MATCH`

### 4.3 长度汇总规则（按表级）

- 同一张表下，只要任意字段出现 `COLUMN_LENGTH_MISMATCH`，则该表记为 `LENGTH_MISMATCH`
- 否则记为 `LENGTH_MATCH`

### 4.4 默认值汇总规则（按表级）

- 同一张表下，只要任意字段出现 `COLUMN_DEFAULT_MISMATCH`，则该表记为 `DEFAULT_MISMATCH`
- 否则记为 `DEFAULT_MATCH`

## 5. 输出文件说明

输出文件：

- `compare_summary.xlsx`

## 6. Sheet 设计

### 6.1 Overview

建议字段：

- `metric`
- `value`

至少包含：

- `totalTables`
- `fullExistsTables`
- `notFullExistsTables`
- `typeMismatchTables`
- `lengthMismatchTables`
- `defaultMismatchTables`
- `fullExistsRatio`

### 6.2 Field Existence Summary

字段：

- `fieldExistenceStatus`
- `tableCount`
- `ratio`

状态值：

- `FULL_EXISTS`
- `NOT_FULL_EXISTS`

### 6.3 Type Summary

字段：

- `typeStatus`
- `tableCount`
- `ratio`

状态值：

- `TYPE_MATCH`
- `TYPE_MISMATCH`

### 6.4 Length Summary

字段：

- `lengthStatus`
- `tableCount`
- `ratio`

状态值：

- `LENGTH_MATCH`
- `LENGTH_MISMATCH`

### 6.5 Default Summary

字段：

- `defaultStatus`
- `tableCount`
- `ratio`

状态值：

- `DEFAULT_MATCH`
- `DEFAULT_MISMATCH`

### 6.6 Table Status

字段：

- `sourceDatabase`
- `sourceSchema`
- `sourceTable`
- `targetSchema`
- `targetTable`
- `tableStatus`

用于输出每张表的最终存在性状态，效果应接近用户提供的 Excel 示例。

### 6.7 Diff Summary

字段：

- `diffCategory`
- `tableCount`
- `ratio`

### 6.8 Risk Summary

字段：

- `riskLevel`
- `tableCount`
- `ratio`

### 6.9 Schema Distribution

字段：

- `schema`
- `tableCount`

### 6.10 Top Issue Tables

字段：

- `table`
- `diffCount`

### 6.11 Detail

字段：

- `source_db`
- `schema`
- `table`
- `column`
- `diff_type`
- `detail`

## 7. 技术要求

- Java 实现
- 支持较大结果集输出
- Excel 可直接打开
- 多 sheet 输出结构清晰

## 8. 验收标准

- 表级状态与 SQL 聚合规则一致
- 存在性、类型、长度、默认值汇总结果正确
- Excel sheet 完整
- 结果可用于汇报与后续人工筛选分析