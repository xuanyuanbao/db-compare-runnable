# db-compare-runnable

一个基于 Spring Boot + Gradle 的数据库结构比对工具，当前支持两种执行模式：

- `FULL_SCAN`：旧模式，先加载源库完整元数据，再与目标库做结构比对
- `TARGET_DRIVEN`：新模式，以目标库 View 为驱动，只按任务逐张加载源表，避免全库扫描

当前项目默认采用：

- `dbcompare.mode=TARGET_DRIVEN`
- `dbcompare.target.view-only=true`

## 当前能力

- 对比多个源库和一个目标库的 schema / table / column 结构
- 输出差异 CSV
- 输出全量 Excel 明细
- 输出可直接导入数据库的 SQL 明细
- 过滤数据库系统自带的 schema，只对比用户创建对象
- 支持目标驱动模式下的单表精准加载
- 支持 AS400 的 library list 解析

## 运行方式

项目默认从 `src/main/resources/application.properties` 读取配置。

常用命令：

```bash
./gradlew bootRun
./gradlew test
./gradlew clean build
```

Windows：

```bat
gradlew.bat bootRun
gradlew.bat test
gradlew.bat clean build
```

在 IDEA 中可直接运行：

- `src/main/java/com/example/dbcompare/app/CompareApplication.java`

## 配置说明

主配置文件：

- `src/main/resources/application.properties`

主要配置分组：

- `dbcompare.mode`：执行模式，支持 `FULL_SCAN` / `TARGET_DRIVEN`
- `dbcompare.sources[n]`：源库配置
- `dbcompare.target`：目标库配置
- `dbcompare.mappings[n]`：源库到目标 schema 的映射
- `dbcompare.table-mappings[n]`：表级覆盖映射
- `dbcompare.options.*`：比对选项
- `dbcompare.output.*`：输出配置

重点配置项：

- `dbcompare.target.view-only=true`
  - 目标驱动模式下只加载目标 View
- `dbcompare.sources[n].schema`
  - 指定源库默认 schema，便于单表精准查询
- `dbcompare.output.csv-path`
- `dbcompare.output.excel-path`
- `dbcompare.output.sql-path`
- `dbcompare.output.sql-table-name`
- `dbcompare.output.summary-path`

## Target-Driven 模式

`TARGET_DRIVEN` 模式的核心思路是：

1. 加载目标库的 View 元数据
2. 根据目标 schema 反向找到源库
3. 根据表级 mapping 或 View 名解析出源表名
4. 对源库执行单表元数据加载
5. 执行结构比对并输出结果

这个模式的目标是避免：

- 全量加载所有源库表结构
- 多源库情况下的高内存占用
- 无映射表的无效扫描

## AS400 特殊处理

当源库类型是 `AS400` 且未显式提供 schema 时：

- 系统会通过 `QSYS2.SYSTABLES` 与 `QSYS2.LIBRARY_LIST_INFO` 按 library list 顺序解析实际 schema
- 若解析到多个候选 schema，会输出 `SOURCE_TABLE_AMBIGUOUS` 差异

## 对比规则

当前主要比对项：

- 表是否存在
- 字段是否存在
- 字段类型是否一致
- 字段长度 / 精度是否一致
- 默认值是否一致
- nullable 是否一致

对象类型规则：

- 源库固定按 `TABLE` 加载
- 目标库可按配置选择 `TABLE` 或 `VIEW`
- 在 `TARGET_DRIVEN` + `target.view-only=true` 下，目标库按 `VIEW` 驱动

系统 schema 过滤：

- DB2：如 `SYSCAT`、`SYSIBM`、`SYSTOOLS`
- openGauss：如 `pg_*`、`information_schema`
- AS400：如 `QSYS`、`QSYS2`、`QGPL`

## 报表说明

### CSV

CSV 只输出差异项。

### Excel

Excel 输出全量字段明细，包括一致项和不一致项。

状态列包括：

- `MATCH`
- `MISMATCH`
- `NOT_APPLICABLE`

当单个 sheet 超过 `.xlsx` 行数上限时，会自动拆分为：

- `Detail`
- `Detail_2`
- `Detail_3`
- ...

### SQL

SQL 输出与 Excel 明细保持同一套列结构。

内容包括：

- `CREATE TABLE`
- 多条 `INSERT INTO`

默认表名：

- `db_compare_detail_result`

默认输出文件：

- `build/reports/default-compare-detail.sql`

## 默认输出文件

默认会生成：

- `build/reports/default-compare-report.csv`
- `build/reports/default-compare-detail.xlsx`
- `build/reports/default-compare-detail.sql`
- `build/reports/default-compare-summary.txt`

## 示例文件

示例目录：

- `examples/demo`
- `examples/sql`

其中：

- `examples/demo`：保留 `FULL_SCAN` 的 snapshot 回归数据
- `examples/sql`：DB2 / openGauss 示例建表和测试数据脚本

## 项目结构

主要目录：

- `src/main/java`：核心代码
- `src/main/resources`：Spring Boot 配置
- `src/test/java`：测试代码
- `examples/sql`：示例 SQL
- `scripts`：简单脚本封装
- `db_compare_requirements.md`：本次 target-driven 改造需求文档

核心入口：

- `src/main/java/com/example/dbcompare/app/CompareApplication.java`

## 已补充的测试

这次改造新增或更新了以下测试覆盖：

- 目标驱动编排模式
- View 名解析
- 反向 mapping
- 新配置项解析
- 既有 Excel / SQL 导出能力

## 后续可继续增强的方向

- 从目标 View definition 中更完整地解析真实 base table
- 针对 DB2 / AS400 的单表查询进一步做数据库专用 SQL 优化
- 将 target-driven 任务规划结果输出为单独的审计报表
- 对超大结果集按 schema 或 source database 自动拆分输出文件