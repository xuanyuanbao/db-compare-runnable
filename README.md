# db-compare-runnable

一个基于 Spring Boot + Gradle 的数据库结构比对工具，当前主要用于：

- 对比多个源库和一个目标库的 schema / table / column 结构
- 输出差异 CSV
- 输出全量 Excel 明细
- 输出可直接导入数据库的 SQL 明细
- 过滤数据库系统自带的 schema，只对比用户创建对象

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

- `dbcompare.sources[n]`：源库配置
- `dbcompare.target`：目标库配置
- `dbcompare.mappings[n]`：源库到目标 schema 的映射
- `dbcompare.table-mappings[n]`：表级覆盖映射
- `dbcompare.options.*`：比对选项
- `dbcompare.output.*`：输出配置

当前支持的重要输出配置：

- `dbcompare.output.csv-path`
- `dbcompare.output.excel-path`
- `dbcompare.output.sql-path`
- `dbcompare.output.sql-table-name`
- `dbcompare.output.summary-path`

## 对比规则

当前支持的数据库类型：

- `DB2`
- `GAUSS`
- `AS400`
- `SNAPSHOT`

当前主要比对项：

- 表是否存在
- 字段是否存在
- 字段类型是否一致
- 字段长度 / 精度是否一致
- 默认值是否一致
- nullable 是否一致

对象类型规则：

- 源库固定按 `TABLE` 加载
- 目标库按配置决定比对 `TABLE` 或 `VIEW`

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

- `examples/demo`：snapshot 回归测试数据
- `examples/sql`：DB2 / openGauss 示例建表和测试数据脚本

## 项目结构

主要目录：

- `src/main/java`：核心代码
- `src/main/resources`：Spring Boot 配置
- `src/test/java`：测试代码
- `examples/sql`：示例 SQL
- `scripts`：简单脚本封装

核心入口：

- `src/main/java/com/example/dbcompare/app/CompareApplication.java`

## 后续扩展方向

可以继续增强的方向包括：

- 更细的 DB2 / openGauss / AS400 类型兼容规则
- 更灵活的表级和字段级忽略规则
- 更丰富的 Excel 汇总页和样式
- 按环境拆分不同配置文件
- 超大结果集按库或 schema 拆分多个导出文件