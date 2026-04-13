# db-compare-runnable

这是一个基于 Spring Boot + Gradle 的数据库结构比对工具，用来做源库与目标库之间的 schema、table 或 view、column 级别比对，并输出 CSV、明细 Excel、汇总 Excel、SQL 和文本汇总。

当前支持两种运行模式：
- `FULL_SCAN`：全量加载源库与目标库元数据后再统一比对。
- `TARGET_DRIVEN`：先从目标侧对象出发，再按映射精准回查源库，适合大库场景。

## 核心能力

- 支持字段是否存在、类型、长度、默认值、nullable 五类比对
- 支持多个源库映射到一个目标库 schema
- 支持目标侧按 `TABLE` 或 `VIEW` 二选一比对，源侧固定按 `TABLE` 加载
- 自动过滤数据库系统自带 schema / 表
- 支持全量字段明细导出，不一致和一致记录都会保留
- 支持 MySQL 可执行 SQL 导出
- 支持大结果集 Excel 分 sheet 输出
- 支持简化版汇总 Excel 输出

## 运行方式

配置文件：
- [application.properties](D:/develop/java_develop/temp/db-compare-runnable/src/main/resources/application.properties)

命令行：
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

IDEA 里可以直接运行主类：
- [CompareApplication.java](D:/develop/java_develop/temp/db-compare-runnable/src/main/java/com/example/dbcompare/app/CompareApplication.java)

## 驱动依赖

Gradle 已内置常用 JDBC 驱动：
- DB2：`com.ibm.db2:jcc:11.5.9.0`
- AS400：`net.sf.jt400:jt400:21.0.6`
- openGauss / PostgreSQL：`org.postgresql:postgresql:42.7.3`

配置中可使用这些驱动类名：
- DB2：`com.ibm.db2.jcc.DB2Driver`
- AS400：`com.ibm.as400.access.AS400JDBCDriver`
- openGauss：`org.postgresql.Driver`

兼容项：
- 标准配置键是 `driver-class-name`
- 历史别名 `drive-class-name` 仍然兼容

## 关键配置

配置文件位置：
- `src/main/resources/application.properties`

常用配置项：
- `dbcompare.mode`
  - 运行模式：`FULL_SCAN` / `TARGET_DRIVEN`
- `dbcompare.sources[n]`
  - 源库配置
- `dbcompare.target`
  - 目标库配置
- `dbcompare.mappings[n]`
  - 源库到目标 schema 的映射
- `dbcompare.table-mappings[n]`
  - 可选的表名映射
- `dbcompare.options.object-type`
  - 目标侧对象类型：`TABLE` 或 `VIEW`
- `dbcompare.options.compare-nullable`
- `dbcompare.options.compare-default-value`
- `dbcompare.options.compare-length`
- `dbcompare.output.csv-path`
- `dbcompare.output.excel-path`
- `dbcompare.output.summary-excel-path`
- `dbcompare.output.sql-path`
- `dbcompare.output.sql-table-name`
- `dbcompare.output.summary-path`

## 输出文件

默认输出：
- `build/reports/default-compare-report.csv`
- `build/reports/default-compare-detail.xlsx`
- `build/reports/default-compare-summary.xlsx`
- `build/reports/default-compare-detail.sql`
- `build/reports/default-compare-summary.txt`

### 明细 Excel

明细 Excel 输出全量字段记录：
- `MATCH`
- `MISMATCH`
- `NOT_APPLICABLE`

当单个 sheet 超过 `.xlsx` 限制时会自动拆分为：
- `Detail`
- `Detail_2`
- `Detail_3`
- ...

### 汇总 Excel

汇总 Excel 现在收敛为 3 个 sheet，便于直接查看：
- `Summary`
- `Table Status`
- `Detail`

`Summary` sheet 会把信息集中排版在一个页签里，包含：
- 视图 schema 划分
- 5 种比对类型的简单概况与占比
- 风险等级划分
- diff 分类占比
- 风险等级规则

`Table Status` sheet 输出表级汇总结果，字段包括：
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

`Detail` sheet 按 5 类问题分段输出明细：
- Field Existence Details
- Type Details
- Length Details
- Default Details
- Nullable Details

### SQL 导出

SQL 导出与 Excel 明细字段保持一致，方便导入 MySQL 做二次分析：
- 输出 `DROP TABLE IF EXISTS`
- 输出 `CREATE TABLE IF NOT EXISTS`
- 使用批量 `INSERT INTO ... VALUES (...), (...)`
- 带 MySQL 字符串转义与 `utf8mb4`

默认表名：
- `db_compare_detail_result`

## 过滤与路由规则

对象加载规则：
- 源库固定按 `TABLE` 加载
- 目标库按配置加载 `TABLE` 或 `VIEW`
- `TARGET_DRIVEN + dbcompare.target.view-only=true` 时，目标侧会按 view 驱动回查源表

系统 schema 过滤：
- DB2：`SYSCAT`、`SYSIBM`、`SYSTOOLS`
- openGauss：`pg_*`、`information_schema`
- AS400：`QSYS`、`QSYS2`、`QGPL`

## 示例资源

示例目录：
- `examples/demo`
- `examples/demo-large`
- `examples/sql`

说明：
- `examples/demo` 用于本地演示跑通完整流程
- `examples/demo-large` 提供大样本 snapshot 数据，默认生成 12 个 schema、120 张表，并混合字段缺失、类型、长度、默认值、nullable 等离散差异
- `examples/sql` 提供 DB2 与 openGauss 测试建表脚本
- `scripts/generate_large_demo.ps1` 可重新生成大样本 demo 数据

## 代码结构

- `src/main/java`
  - 核心业务与输出实现
- `src/main/resources`
  - Spring Boot 配置
- `src/test/java`
  - 单元测试与回归测试
- `examples/sql`
  - 测试 SQL
- `scripts`
  - 辅助脚本
- `db_compare_requirements.md`
  - 主需求文档
- `db_compare_excel_requirements.md`
  - 汇总 Excel 需求文档
