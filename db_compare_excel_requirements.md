# 数据对比汇总 Excel 需求说明

## 1. 背景

当前系统已经能够输出字段级明细，但原先的汇总 Excel 拆成了过多 sheet，浏览成本比较高。现在需要把汇总信息收敛成更直接的结构，便于业务侧快速看总览、表级状态和问题明细。

## 2. 输出目标

新的汇总 Excel 只保留 3 个 sheet：
- `Summary`
- `Table Status`
- `Detail`

其中：
- `Summary`：集中展示总览、占比、风险规则和 schema 分布
- `Table Status`：按表级输出 5 类对比状态
- `Detail`：按 5 类问题分段输出字段级明细

## 3. 统计范围

比对维度固定为 5 类：
- 字段是否存在
- 字段类型是否一致
- 字段长度是否一致
- 字段默认值是否一致
- 字段 nullable 是否一致

表级分组键：
- `sourceDatabase`
- `sourceSchema`
- `sourceTable`
- `targetSchema`
- `targetTable`

## 4. 表级聚合规则

### 4.1 字段存在性

参考 SQL 逻辑：
```sql
CASE
    WHEN SUM(CASE WHEN sourceExists != 'Y' OR targetExists != 'Y' THEN 1 ELSE 0 END) > 0
    THEN 'NOT_FULL_EXISTS'
    ELSE 'FULL_EXISTS'
END
```

Java 侧规则：
- 同一张表下，只要任一字段明细出现 `sourceExists=false` 或 `targetExists=false`，则该表记为 `NOT_FULL_EXISTS`
- 否则记为 `FULL_EXISTS`

### 4.2 类型状态

- 同一张表下，只要任一字段出现 `COLUMN_TYPE_MISMATCH`，则该表记为 `TYPE_MISMATCH`
- 否则记为 `TYPE_MATCH`

### 4.3 长度状态

- 同一张表下，只要任一字段出现 `COLUMN_LENGTH_MISMATCH`，则该表记为 `LENGTH_MISMATCH`
- 否则记为 `LENGTH_MATCH`

### 4.4 默认值状态

- 同一张表下，只要任一字段出现 `COLUMN_DEFAULT_MISMATCH`，则该表记为 `DEFAULT_MISMATCH`
- 否则记为 `DEFAULT_MATCH`

### 4.5 Nullable 状态

- 同一张表下，只要任一字段出现 `COLUMN_NULLABLE_MISMATCH`，则该表记为 `NULLABLE_MISMATCH`
- 否则记为 `NULLABLE_MATCH`

## 5. 风险等级规则

风险等级统一按表级判断：
- `LOW`
  - 没有任何 mismatch
- `MEDIUM`
  - 只有长度、默认值、nullable 类问题
- `HIGH`
  - 存在字段缺失、表缺失、表歧义或类型不一致

## 6. Summary Sheet 设计

`Summary` 需要在一个 sheet 里集中排版以下信息：

### 6.1 视图 schema 划分

字段：
- `viewSchema`
- `viewCount`

说明：
- 按目标 schema 统计表数或 view 数
- 最后一行输出 `total`

### 6.2 总体概况

字段：
- `metric`
- `value`

至少包含：
- `totalTables`
- `fullExistsTables`
- `notFullExistsTables`
- `typeMismatchTables`
- `lengthMismatchTables`
- `defaultMismatchTables`
- `nullableMismatchTables`
- `fullExistsRatio`

### 6.3 五类状态占比

每类都输出：
- `status`
- `tableCount`
- `ratio`

包括：
- `fieldExistenceStatus`
- `typeStatus`
- `lengthStatus`
- `defaultStatus`
- `nullableStatus`

### 6.4 diff 分类占比

字段：
- `status`
- `tableCount`
- `ratio`

状态包括：
- `FULL_MATCH`
- `MISSING_COLUMN`
- `TYPE_MISMATCH`
- `LENGTH_MISMATCH`
- `OTHER`

### 6.5 风险等级占比

字段：
- `status`
- `tableCount`
- `ratio`

状态包括：
- `LOW`
- `MEDIUM`
- `HIGH`

### 6.6 风险规则说明

字段：
- `condition`
- `riskLevel`

至少包含：
- `No mismatch -> LOW`
- `Length/default/nullable mismatch -> MEDIUM`
- `Missing field/table or type mismatch -> HIGH`

## 7. Table Status Sheet 设计

sheet 名称：`Table Status`

字段：
- `sourceDatabase`
- `sourceSchema`
- `sourceTable`
- `targetSchema`
- `targetTable`
- `fieldExistenceStatus`
- `typeStatus`
- `lengthStatus`
- `defaultStatus`
- `nullableStatus`
- `riskLevel`
- `diffCategory`

用途：
- 直接按表查看整体状态
- 适合透视、筛选、统计和人工复核

## 8. Detail Sheet 设计

sheet 名称：`Detail`

在同一个 sheet 中按 5 个区段输出：
- `Field Existence Details`
- `Type Details`
- `Length Details`
- `Default Details`
- `Nullable Details`

每个区段字段一致：
- `sourceDatabase`
- `sourceSchema`
- `sourceTable`
- `targetSchema`
- `targetTable`
- `columnName`
- `diffType`
- `detail`

当某个区段无数据时，输出 `NO_DATA` 标记。

## 9. 技术要求

- Java 实现
- 支持大结果集输出
- Excel 可直接打开
- 汇总信息尽量集中，减少 sheet 数量
- Detail 仍要支持超大数据量分 sheet

## 10. 验收标准

- 汇总 Excel 只保留 `Summary`、`Table Status`、`Detail` 3 个核心 sheet
- Summary 中能同时看到 5 类对比类型的概况、占比、风险规则、schema 分布
- Table Status 能按表级输出 5 类状态
- Detail 能按 5 类问题分段输出明细
- 统计结果与字段级原始记录一致
