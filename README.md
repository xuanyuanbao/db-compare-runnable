# db-compare-runnable

一个基于 Spring Boot 的数据库表结构比对工具，当前主要用于：

- 对比多个 DB2 源库与 openGauss 目标库的 schema / table / column 结构
- 输出差异 CSV
- 输出全量 Excel 明细，区分一致和不一致
- 过滤数据库系统自带的 schema，只比对用户创建的对象

## 当前运行方式

项目已经改成 Spring Boot + Gradle 方式运行，默认从 `src/main/resources/application.properties` 读取配置。

常用命令：

```bash
./gradlew bootRun
./gradlew test
./gradlew clean build
```

Windows:

```bat
gradlew.bat bootRun
gradlew.bat test
gradlew.bat clean build
```

如果你在 IDEA 中运行，直接启动 `CompareApplication` 即可。

## 配置文件

主配置文件：

- `src/main/resources/application.properties`

当前配置结构：

- `dbcompare.sources[n]`：源库配置
- `dbcompare.target`：目标库配置
- `dbcompare.mappings[n]`：源库到目标 schema 的映射
- `dbcompare.table-mappings[n]`：可选的表级覆盖映射
- `dbcompare.options.*`：对比选项
- `dbcompare.output.*`：输出路径

当前默认输出：

- `build/reports/default-compare-report.csv`
- `build/reports/default-compare-detail.xlsx`
- `build/reports/default-compare-summary.txt`

## 对比范围

当前支持的数据库类型：

- `DB2`
- `GAUSS`
- `AS400`
- `SNAPSHOT`

当前对比项：

- 表是否存在
- 字段是否存在
- 字段类型是否一致
- 字段长度 / 精度是否一致
- 默认值是否一致
- nullable 是否一致

另外，JDBC 模式下已经默认过滤系统 schema：

- DB2：如 `SYSCAT`、`SYSIBM`、`SYSTOOLS`
- openGauss：如 `pg_*`、`information_schema`
- AS400：如 `QSYS`、`QSYS2`、`QGPL`

## 报表说明

### CSV

CSV 只输出差异项。

### Excel

Excel 输出全量字段明细，包括：

- 一致项
- 不一致项
- 目标缺表 / 缺字段

并带有状态列：

- `MATCH`
- `MISMATCH`
- `NOT_APPLICABLE`

## 示例文件

示例目录：

- `examples/demo`
- `examples/sql`

其中：

- `examples/demo` 保留了 snapshot 测试数据，主要用于测试和回归
- `examples/sql` 提供了 DB2_A、DB2_B、openGauss 的建表和测试数据脚本

## 项目结构

主要目录：

- `src/main/java`：核心代码
- `src/main/resources`：Spring Boot 配置
- `src/test/java`：测试代码
- `examples/sql`：DB2 / openGauss 示例 SQL
- `scripts`：简单脚本封装

核心入口：

- `src/main/java/com/example/dbcompare/app/CompareApplication.java`

## 开发说明

当前项目已经不再使用老的“手工 properties + out 目录 classpath 启动”方式，README 中如果你看到那种旧说明，可以认为已经过时。

如果后续继续扩展，比较适合的方向有：

- 更细的 DB2 / openGauss / AS400 类型兼容规则
- 表级和字段级忽略规则
- 更丰富的 Excel 样式和汇总页
- 通过 profile 区分不同环境配置
