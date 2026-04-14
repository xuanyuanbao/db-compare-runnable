# 目标 View 血缘独立导出需求

## 1. 背景

当前项目已经支持：
- `FULL_SCAN`
- `TARGET_DRIVEN`
- 目标侧 `TABLE / VIEW` 二选一比对

在以下配置下：
- `dbcompare.mode=TARGET_DRIVEN`
- `dbcompare.options.object-type=VIEW`

程序已经能够围绕目标 View 做比对，但业务希望新增一份更直观的血缘结果，专门展示：

`sourceDatabase/sourceSchema/sourceTable -> targetTableSchema/targetTable -> targetView/targetViewSchema`

同时要求：
- 原有明细 Excel 不改
- 原有汇总 Excel 不改
- 只新增一份独立 Excel

## 2. 目标

新增一份独立的目标 View 血缘 Excel，用来展示目标 View 依赖了哪些目标基表。

这个新 Excel 只承载血缘关系，不承载字段级明细。

## 3. 输出要求

### 3.1 输出文件

新增输出文件：
- `dbcompare.output.target-view-lineage-excel-path`

默认值建议为：
- `build/reports/default-target-view-lineage.xlsx`

### 3.2 工作簿结构

新工作簿当前只需要一个 sheet：
- `目标View血缘`

### 3.3 字段顺序

sheet 字段顺序固定为：
- `源数据库`
- `源Schema`
- `源表`
- `目标基表Schema`
- `目标基表`
- `目标View`
- `目标ViewSchema`

## 4. 数据来源

血缘关系方向是：
- `targetView -> targetTable`

数据来源优先级：
1. 如果配置了 `target.viewLineageFile`，优先读取外部血缘文件
2. 否则在目标库为 GaussDB 时，直接查询系统视图获取血缘关系

## 5. 设计约束

### 5.1 不改原有主报表

以下输出维持现状：
- CSV
- 明细 Excel
- 汇总 Excel
- SQL

也就是说：
- 原有明细 Excel 继续保留 `目标Schema / 目标表`
- 原有汇总 Excel 继续保留 `目标Schema / 目标表`

新增的 View 血缘信息不挤进原有 sheet。

### 5.2 去重规则

如果同一条血缘关系在字段级比对过程中重复出现，只保留一条。

唯一键按以下字段组合去重：
- `源数据库`
- `源Schema`
- `源表`
- `目标基表Schema`
- `目标基表`
- `目标View`
- `目标ViewSchema`

## 6. 验收标准

满足以下条件即可视为完成：

1. 在 `TARGET_DRIVEN + VIEW` 模式下，新增一份独立 Excel
2. 原有明细 Excel 和汇总 Excel 结构不变
3. 新 Excel 中包含 7 个字段，顺序与需求一致
4. 新 Excel 中同一条血缘关系不会重复输出
5. 示例配置可以直接生成演示结果
6. 单元测试和 README 同步更新
