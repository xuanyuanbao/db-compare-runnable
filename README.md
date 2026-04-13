# db-compare-runnable

一个基于 Spring Boot + Gradle 的数据库结构比对工具，支持多个源库与一个目标库之间的 schema、table、view、column 结构比对。

当前支持两种执行模式：

- `FULL_SCAN`：先加载源库完整元数据，再与目标库做结构比对。
- `TARGET_DRIVEN`：以目标库对象为驱动，只按任务逐张加载源表，避免全库扫描带来的高内存占用。

当前默认配置：

- `dbcompare.mode=TARGET_DRIVEN`
- `dbcompare.target.view-only=true`

## 当前能力

- 对比多个源库和一个目标库的 schema / table / column 结构
- 目标驱动模式下按单表精准加载源库元数据
- 过滤数据库系统自带 schema，只比对用户对象
- 支持 AS400 library list 解析
- 输出差异 CSV
- 输出全量 Excel 明细
- 输出管理视角的汇总 Excel
- 输出可直接导入数据库的 SQL 明细

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

## 驱动依赖

当前 Gradle 已内置以下 JDBC 驱动依赖：

- DB2：`com.ibm.db2:jcc:11.5.9.0`
- AS400：`net.sf.jt400:jt400:21.0.6`
- openGauss / PostgreSQL：`org.postgresql:postgresql:42.7.3`

如果你在配置文件里显式指定驱动类，可使用：

- DB2：`com.ibm.db2.jcc.DB2Driver`
- AS400：`com.ibm.as400.access.AS400JDBCDriver`
- openGauss：`org.postgresql.Driver`

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

- `dbcompare.sources[n].driver-class-name`
  - 源库 JDBC 驱动类名
- `dbcompare.target.driver-class-name`
  - 目标库 JDBC 驱动类名
- `dbcompare.target.view-only=true`
  - 目标驱动模式下只加载目标 view
- `dbcompare.sources[n].schema`
  - 指定源库默认 schema，便于单表精准查询
- `dbcompare.output.csv-path`
- `dbcompare.output.excel-path`
- `dbcompare.output.summary-excel-path`
- `dbcompare.output.sql-path`
- `dbcompare.output.sql-table-name`
- `dbcompare.output.summary-path`

兼容说明：

- 标准配置项是 `driver-class-name`
- 旧配置里如果误写成 `drive-class-name`，程序仍会兼容识别

## Target-Driven 模式

`TARGET_DRIVEN` 模式的核心流程：

1. 加载目标库的 view 元数据
2. 根据目标 schema 反向定位源库
3. 根据表级 mapping 或 view 名解析出源表名
4. 对源库执行单表元数据加载
5. 执行结构比对并输出结果

这个模式主要用于：

- 避免一次性加载多个源库的全部表结构
- 降低多源库场景下的堆内存占用
- 只聚焦真实参与映射的对象

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
- 在 `TARGET_DRIVEN + target.view-only=true` 下，目标库按 `VIEW` 驱动

系统 schema 过滤：

- DB2：如 `SYSCAT`、`SYSIBM`、`SYSTOOLS`
- openGauss：如 `pg_*`、`information_schema`
- AS400：如 `QSYS`、`QSYS2`、`QGPL`

## 报表说明

### CSV

CSV 只输出差异项。

### Detail Excel

明细 Excel 输出全量字段明细，包括一致项和不一致项。

状态列包括：

- `MATCH`
- `MISMATCH`
- `NOT_APPLICABLE`

当单个 sheet 超过 `.xlsx` 行数上限时，会自动拆分为：

- `Detail`
- `Detail_2`
- `Detail_3`
- ...

### Summary Excel

汇总 Excel 用于汇报和风险分析，会输出多个 sheet：

- `Overview`
- `Diff Summary`
- `Risk Summary`
- `Schema Distribution`
- `Top Issue Tables`
- `Detail`

其中：

- `Overview`：总表数、已匹配表数、未匹配表数、覆盖率
- `Diff Summary`：按表级主差异分类统计
- `Risk Summary`：按低/中/高风险统计
- `Schema Distribution`：按 schema 统计表数量
- `Top Issue Tables`：按差异数量排序的问题表
- `Detail`：按 `source_db / schema / table / column / diff_type / detail` 输出简化明细

当前汇总规则：

- 以表为统计单位
- 只要表内任意字段存在差异，就视为问题表
- 风险分级默认规则：
  - 无差异：低风险
  - 长度 / 默认值 / nullable 差异：中风险
  - 缺字段 / 缺表 / 类型不一致 / 歧义表：高风险

### SQL

SQL 输出与明细 Excel 保持同一套列结构。

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
- `build/reports/default-compare-summary.xlsx`
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
- `db_compare_requirements.md`：目标驱动需求文档
- `db_compare_excel_requirements.md`：汇总 Excel 需求文档

核心入口：

- `src/main/java/com/example/dbcompare/app/CompareApplication.java`

## 测试覆盖

当前已覆盖：

- 目标驱动编排模式
- View 名解析
- 反向 mapping
- 新配置项解析
- Excel / SQL 导出能力
- 汇总 Excel 多 sheet 输出能力
- 驱动配置兼容解析

## 后续可继续增强的方向

- 从目标 View definition 中更完整地解析真实 base table
- 针对 DB2 / AS400 的单表查询进一步做数据库专用 SQL 优化
- 将 target-driven 任务规划结果输出为单独审计报表
- 对超大结果集按 schema 或 source database 自动拆分输出文件
- 支持对汇总 Excel 的风险规则和 Top N 行数进行配置化