# 数据对比汇总 Excel 需求说明

## 1. 背景

当前系统已经能够输出字段级明细，但原先的汇总 Excel 拆成了过多 sheet，浏览成本比较高。现在需要把汇总信息收敛成更直接的结构，便于业务侧快速看总览、表级状态和问题明细。

## 2. 输出目标

新的汇总 Excel 保持“少量汇总 + 分类型明细”的结构：
- `汇总`
- `表级状态`
- `字段存在明细`
- `类型明细`
- `长度明细`
- `默认值明细`
- `可空明细`

其中：
- `汇总`：集中展示总览、占比、风险规则和 schema 分布，并通过颜色增强状态区分
- `表级状态`：按表级输出 5 类对比状态
- 各 Detail sheet：每种问题类型单独一个 sheet，超大数据量时继续拆成 `_2`、`_3`
- 导出表头、状态值、说明文案默认使用中文

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

## 6. 汇总 Sheet 设计

`汇总` 需要在一个 sheet 里集中排版以下信息：

### 6.1 视图 schema 划分

字段：
- `视图Schema`
- `视图数量`

说明：
- 按目标 schema 统计表数或 view 数
- 最后一行输出 `合计`

### 6.2 总体概况

字段：
- `指标`
- `值`

至少包含：
- `总表数`
- `完全存在表数`
- `不完全存在表数`
- `类型不一致表数`
- `长度不一致表数`
- `默认值不一致表数`
- `可空性不一致表数`
- `完全存在占比`

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

布局要求：
- `nullableStatus` 与前 4 类状态块保持同一列纵向排布
- `riskLevel` 与 `diffCategory` 需要单独放置，不能遮挡统计值
- 各状态块列宽需要保证完整显示长文本，例如 `NULLABLE_MISMATCH`、`HIGH`

### 6.4 diff 分类占比

字段：
- `status`
- `tableCount`
- `ratio`

状态包括：
- `完全一致`
- `字段缺失`
- `类型不一致`
- `长度不一致`
- `其他差异`

### 6.5 风险等级占比

字段：
- `status`
- `tableCount`
- `ratio`

状态包括：
- `低`
- `中`
- `高`

### 6.6 风险规则说明

字段：
- `condition`
- `riskLevel`

至少包含：
- `没有任何差异 -> 低`
- `长度/默认值/可空性不一致 -> 中`
- `字段缺失、表缺失或类型不一致 -> 高`

## 7. 表级状态 Sheet 设计

sheet 名称：`表级状态`

字段：
- `源数据库`
- `源Schema`
- `源表`
- `目标Schema`
- `目标表`
- `字段存在状态`
- `类型状态`
- `长度状态`
- `默认值状态`
- `可空状态`
- `风险等级`
- `差异分类`

用途：
- 直接按表查看整体状态
- 适合透视、筛选、统计和人工复核

## 8. Detail Sheet 设计

sheet 名称：
- `字段存在明细`
- `类型明细`
- `长度明细`
- `默认值明细`
- `可空明细`

每个 Detail sheet 字段一致：
- `源数据库`
- `源Schema`
- `源表`
- `目标Schema`
- `目标表`
- `字段名`
- `差异类型`
- `说明`

当某个 Detail sheet 无数据时，输出 `无数据` 标记。
当某类问题数据量超出单个 sheet 视觉或行数承载时，继续输出：
- `字段存在明细_2`
- `类型明细_2`
- ...

## 9. 技术要求

- Java 实现
- 支持大结果集输出
- Excel 可直接打开
- 汇总信息尽量集中，减少 sheet 数量
- Detail 仍要支持超大数据量分 sheet
- 汇总、表级状态、明细中的关键状态需要有明显颜色或样式区分，不能只靠文字辨认

## 10. 验收标准

- 汇总 Excel 至少包含 `汇总`、`表级状态` 和 5 个分类型 Detail sheet
- `汇总` 中能同时看到 5 类对比类型的概况、占比、风险规则、schema 分布
- `表级状态` 能按表级输出 5 类状态
- Detail 能按 5 类问题拆成多个 sheet 输出明细
- 统计结果与字段级原始记录一致
