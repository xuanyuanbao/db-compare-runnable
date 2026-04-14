# 目标 View 血缘对比需求说明

## 1. 背景

当前项目已经支持：
- `FULL_SCAN` 全量比对
- `TARGET_DRIVEN` 目标驱动比对
- `TABLE / VIEW` 两种目标对象类型

但在以下配置下：
- `dbcompare.mode=TARGET_DRIVEN`
- `dbcompare.options.object-type=VIEW`

现有程序内部仍然沿用了旧的目标对象语义：
- `targetSchema`
- `targetTable`

这在语义上已经不准确了。

因为当目标驱动模式的目标对象是 `VIEW` 时，真正的主目标对象应该是：
- `targetViewSchema`
- `targetView`

而不是：
- `targetTableSchema`
- `targetTable`

`targetTableSchema/targetTable` 在这个模式下只是该 `targetView` 血缘展开后的依赖表信息，不应继续充当主目标对象。

## 2. 核心目标

在 `TARGET_DRIVEN + VIEW` 模式下，完成以下改造：

1. 将程序内部原先承载目标对象语义的：
   - `targetSchema`
   - `targetTable`
   替换为：
   - `targetViewSchema`
   - `targetView`

2. 在 `targetViewSchema + targetView` 的基础上，再向下展开：
   - `targetTableSchema`
   - `targetTable`

3. 支持从 GaussDB 查询：
   - 某个 `targetView` 依赖了哪些 `targetTable`

4. 在结果输出中同时体现：
   - 当前比对针对哪个 `targetView`
   - 该 `targetView` 依赖了哪些 `targetTable`

## 3. 语义定义

### 3.1 主目标对象

当配置满足：
- `dbcompare.mode=TARGET_DRIVEN`
- `dbcompare.options.object-type=VIEW`

则主目标对象定义为：
- `targetViewSchema`
- `targetView`

这两个字段用于：
- 组织 compare task
- 组织目标对象匹配
- 输出结果中的主目标信息
- 作为该模式下的目标对象主键语义

### 3.2 血缘附加对象

在主目标对象基础上，额外增加血缘展开字段：
- `targetTableSchema`
- `targetTable`

这两个字段用于表达：
- 某个 `targetView` 依赖了哪些迁移后的目标表

注意：
- `targetViewSchema` 和 `targetTableSchema` 不能默认相同
- 一个 `targetView` 可能位于 schema A
- 但它依赖的 `targetTable` 可能位于 schema B、schema C

## 4. 关系模型

### 4.1 新的关系方向

这里的核心方向必须明确：

- 不是 `targetTable -> targetView`
- 而是 `targetView -> targetTable`

也就是说，系统需要回答的问题是：

1. 某个源表最终对应哪个 `targetView`
2. 某个 `targetView` 依赖了哪些 `targetTable`

因此新的组织顺序为：

`sourceDatabase/sourceSchema/sourceTable -> targetViewSchema/targetView -> targetTableSchema/targetTable`

### 4.2 新的核心字段

需求二输出至少要包含以下 7 个字段：

- `sourceDatabase`
- `sourceSchema`
- `sourceTable`
- `targetViewSchema`
- `targetView`
- `targetTableSchema`
- `targetTable`

其中：
- 主目标对象字段是 `targetViewSchema + targetView`
- 血缘展开字段是 `targetTableSchema + targetTable`

## 5. 程序改造要求

### 5.1 主目标字段替换

在 `TARGET_DRIVEN + VIEW` 模式下，程序中原先使用以下字段承载目标对象语义的地方：
- `targetSchema`
- `targetTable`

都需要切换为：
- `targetViewSchema`
- `targetView`

适用范围包括但不限于：
- compare task
- 目标对象匹配
- 内部结果模型
- 导出表头
- SQL 字段
- Excel 明细
- 汇总统计维度

### 5.2 血缘展开字段追加

在完成主目标对象替换后，再新增以下字段：
- `targetTableSchema`
- `targetTable`

这两个字段不替代主目标对象，只作为血缘展开信息追加输出。

### 5.3 组织规则

在该模式下，任务主键优先按以下字段组织：
- `sourceDatabase`
- `sourceSchema`
- `sourceTable`
- `targetViewSchema`
- `targetView`

如果需要展示 View 依赖关系，再附加：
- `targetTableSchema`
- `targetTable`

## 6. 数据来源

`targetView -> targetTable` 的关系不由人工维护，而是由 GaussDB 查询得到。

第一版要求：
- 支持从 GaussDB 查询指定 `targetView` 的依赖表
- 查询结果至少能返回：
  - `target_view_schema`
  - `target_view_name`
  - `target_table_schema`
  - `target_table_name`

第一版默认只实现：
- GaussDB

第一版暂不实现：
- 非 GaussDB 的血缘查询
- 多层嵌套 View 的递归展开
- 复杂别名和跨 schema 的高级消歧

## 7. 建议新增模型

### 7.1 View 血缘关系模型

建议新增中间模型或中间表：
- `target_view_lineage`

建议字段：
- `target_view_schema`
- `target_view_name`
- `target_table_schema`
- `target_table_name`
- `lineage_source`
- `load_time`

### 7.2 任务模型

建议扩展或重构 compare task，使其在 `TARGET_DRIVEN + VIEW` 模式下显式包含：
- `source_database`
- `source_schema`
- `source_table`
- `target_view_schema`
- `target_view`
- `target_table_schema`
- `target_table`
- `task_type`

说明：
- `task_type=TARGET_DRIVEN_VIEW`
- 此时主目标键为 `target_view_schema + target_view`
- `target_table_schema + target_table` 为血缘附加信息

## 8. 输出要求

### 8.1 明细输出

在 `TARGET_DRIVEN + VIEW` 模式下，结果明细至少要输出：
- `sourceDatabase`
- `sourceSchema`
- `sourceTable`
- `targetViewSchema`
- `targetView`
- `targetTableSchema`
- `targetTable`

### 8.2 字段含义要求

要求明确区分：
- 主目标对象字段
  - `targetViewSchema`
  - `targetView`
- 血缘展开字段
  - `targetTableSchema`
  - `targetTable`

不能再把：
- `targetTableSchema`
- `targetTable`

当成该模式下的主目标对象字段。

### 8.3 汇总要求

汇总中建议新增按以下维度统计：
- `targetViewSchema`
- `targetView`
- `targetView` 依赖的目标表数量
- 每个 `targetView` 的问题数量

## 9. 第一版范围

第一版建议只做：

1. 明确替换程序内部的主目标对象语义
   - `targetSchema/targetTable`
   - 替换为 `targetViewSchema/targetView`

2. 增加 GaussDB View 血缘查询
   - 查询 `targetView -> targetTable`

3. 在现有结果中追加输出：
   - `targetTableSchema`
   - `targetTable`

4. 同步修改：
   - Excel
   - SQL
   - CSV
   - 汇总逻辑
   - 单元测试
   - 文档

## 10. 验收标准

满足以下条件即视为需求二第一版完成：

1. 当配置为：
   - `dbcompare.mode=TARGET_DRIVEN`
   - `dbcompare.options.object-type=VIEW`

   程序内部与输出中的主目标对象语义已切换为：
   - `targetViewSchema`
   - `targetView`

2. 输出中至少包含 7 个字段：
   - `sourceDatabase`
   - `sourceSchema`
   - `sourceTable`
   - `targetViewSchema`
   - `targetView`
   - `targetTableSchema`
   - `targetTable`

3. `targetView -> targetTable` 的关系来自 GaussDB 查询

4. `targetViewSchema` 与 `targetTableSchema` 可以不同，并能正确输出

5. 现有测试、文档和导出结构同步更新
