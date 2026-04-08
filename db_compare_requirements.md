# 数据库结构对比工具（Target-Driven优化版）需求规范说明书

## 1. 背景

当前系统用于对比源库（AS400 / DB2）与目标库（openGauss / GhostDB）的表结构一致性。

原实现采用“全量加载源库 + 全量加载目标库 + 笛卡尔积对比”的方式，存在严重性能问题：

- 数据量爆炸（N × M）
- 查询压力大
- 内存占用高
- 对比时间不可控

## 2. 优化目标

将对比模式由：

> 全量扫描 + 笛卡尔积对比

优化为：

> 以目标库 View 为驱动（Target-Driven）进行精准对比

## 3. 核心思想

- 目标库 View 作为“驱动源”
- 只对“有映射关系的表”进行对比
- 避免无效数据加载
- 避免全库扫描

## 4. 新方案整体流程

1. 加载目标库所有 View（或指定 Schema 下 View）
2. 遍历每个 View：
   - 解析 View 对应的基础表（table name）
3. 根据 mapping 规则定位源库
4. 按表名精准查询源库结构（非全量）
5. 执行结构对比
6. 输出结果

## 5. 详细流程说明

### 5.1 加载目标库 View

从目标数据库中查询：

- schema
- view_name
- view definition（如可获取）

输出结构：

{
  "schema": "og_db2_1",
  "viewName": "CAZHC"
}

---

### 5.2 View → 表名解析

解析规则：

- 默认：viewName == tableName
- 或解析 view SQL 获取 base table

输出：

{
  "tableName": "CAZHC"
}

---

### 5.3 Mapping 映射

通过配置：

dbcompare.mappings[x].source-database-name  
dbcompare.mappings[x].target-schema-name  

将：

targetSchema → sourceDatabase

---

### 5.4 源库查询（按需加载）

禁止：

SELECT * FROM SYSCAT.TABLES

必须改为：

SELECT * FROM SYSCAT.COLUMNS WHERE TABNAME = ?

仅查询单表。

---

### 5.5 AS400 特殊处理（重点）

当表名未带 schema 时：

采用 Library List 解析：

SELECT TABLE_SCHEMA
FROM QSYS2.SYSTABLES t
JOIN QSYS2.LIBRARY_LIST_INFO l
  ON t.TABLE_SCHEMA = l.SCHEMA_NAME
WHERE t.TABLE_NAME = ?
ORDER BY l.ORDINAL_POSITION
FETCH FIRST 1 ROW ONLY;

规则：

1. 按 Library List 顺序查找
2. 命中第一个即为实际表
3. 若多命中 → 标记候选

---

## 6. 数据模型设计

### CompareTask

class CompareTask {
    String sourceDb;
    String sourceSchema;
    String tableName;

    String targetSchema;
    String viewName;
}

---

### ColumnMeta

class ColumnMeta {
    String name;
    String type;
    int length;
    boolean nullable;
    String defaultValue;
}

---

## 7. 对比规则

对比维度：

- 字段存在性
- 类型一致性
- 长度一致性
- nullable
- default value

---

## 8. 性能优化

| 项目 | 原方案 | 新方案 |
|------|--------|--------|
| 查询量 | 全库 | 单表 |
| 复杂度 | N × M | N |
| 内存 | 高 | 低 |
| 速度 | 慢 | 快 |

---

## 9. 配置新增项

dbcompare.mode=TARGET_DRIVEN  
dbcompare.target.view-only=true  

---

## 10. 模块改造要求

### TargetLoader

- 只加载 View
- 支持 schema 过滤

### SourceLoader

- 支持按表名加载
- 禁止全量扫描

### CompareEngine

- 改为基于 CompareTask 执行
- 不再做全量对比

### 新增模块

- ViewParser
- SourceResolver（AS400 专用）

---

## 11. 异常处理

- 表不存在 → missing
- 多 library 命中 → ambiguous
- 类型不一致 → type_mismatch

---

## 12. 输出结果

CSV：

table,column,diff_type,detail

Summary：

总表数  
差异表数  
错误统计  

---

## 13. 验收标准

- 不允许全库扫描
- 单表查询
- 对比结果正确
- 性能明显提升
- 支持 AS400 Library List 解析

---

## 14. 核心需求一句话

将现有“全量加载 + 笛卡尔积对比”模式，改为“以目标库 View 为驱动的精准对比模式”。
