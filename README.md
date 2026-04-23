# db-compare-runnable

这是一个基于 Spring Boot + Gradle 的数据库结构比对工具，用来做源库与目标库之间的 schema、table 或 view、column 级别比对，并输出 CSV、明细 Excel、汇总 Excel、SQL 和文本汇总。

当前支持两种运行模式：
- `FULL_SCAN`：全量加载源库与目标库元数据后再统一比对。
- `TARGET_DRIVEN`：先从目标侧对象出发，再按映射精准回查源库，适合大库场景。

## 核心能力

- 支持字段是否存在、类型、长度、默认值、nullable 五类比对
- 支持多个源库映射到一个目标库 schema
- 支持目标侧按 `TABLE` 或 `VIEW` 二选一比对，源侧固定按 `TABLE` 加载
- 支持关系模式 `TABLE_TO_TABLE` / `TABLE_TO_VIEW`
- 支持把差异拆分为“主差异”和“信息差异”
- 支持按配置覆盖类型映射，减少跨库类型误报
- 支持新增 `类型长度联合判断` 字段，保留类型/长度原子状态的同时补充联合语义
- 自动过滤数据库系统自带 schema / 表
- 支持全量字段明细导出，不一致和一致记录都会保留
- 支持导入测试组人工确认 Excel，并生成独立的融合结果 Excel
- 支持 MySQL 可执行 SQL 导出
- 支持大结果集 Excel 分 sheet 输出
- 支持简化版汇总 Excel 输出
- 导出默认使用中文表头、中文状态值和中文说明

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
- `target.viewLineageFile`
  - 当 `TARGET_DRIVEN + VIEW` 启用时，指定目标 View 血缘文件
- `dbcompare.mappings[n]`
  - 源库到目标 schema 的映射
- `dbcompare.table-mappings[n]`
  - 可选的表名映射
- `dbcompare.options.object-type`
  - 目标侧对象类型：`TABLE` 或 `VIEW`
- `dbcompare.options.relation-mode`
  - 关系模式：`TABLE_TO_TABLE` / `TABLE_TO_VIEW`
- `dbcompare.options.compare-exists`
- `dbcompare.options.compare-type`
- `dbcompare.options.compare-nullable`
- `dbcompare.options.compare-default-value`
- `dbcompare.options.compare-length`
- `dbcompare.options.source-column-missing-in-target-affect-result`
  - 控制“源有目标无字段”是否影响最终结果
- `dbcompare.options.type-mismatch-affect-result`
  - 控制“类型不一致”是否影响最终结果
- `dbcompare.options.length-mismatch-affect-result`
  - 控制“长度不一致”是否影响最终结果
- `dbcompare.options.length-target-longer-affect-result`
  - 控制“目标长度更长”的字符类型长度差异是否影响最终结果
- `dbcompare.options.default-mismatch-affect-result`
  - 控制“默认值不一致”是否影响最终结果
- `dbcompare.options.nullable-mismatch-affect-result`
  - 控制“可空不一致”是否影响最终结果
- `dbcompare.options.type-rule-file`
  - 类型判等规则文件位置，默认 `classpath:type-equality-rules.properties`
- `dbcompare.options.type-mappings.*`
  - 可选覆盖项，用来在默认规则文件之上补充或覆盖某个归一类型
- `dbcompare.output.csv-path`
- `dbcompare.output.excel-path`
- `dbcompare.output.summary-excel-path`
- `dbcompare.output.target-view-lineage-excel-path`
- `dbcompare.report.manual-confirmation.enabled`
- `dbcompare.report.manual-confirmation.excel-dir`
- `dbcompare.report.manual-confirmation.excel-name`
- `dbcompare.report.manual-confirmation.excel-path`
- `dbcompare.output.manual-confirmation-excel-path`
- `dbcompare.output.sql-path`
- `dbcompare.output.sql-table-name`
- `dbcompare.output.summary-path`

## 输出文件

默认输出：
- `build/reports/default-compare-report.csv`
- `build/reports/default-compare-detail.xlsx`
- `build/reports/default-compare-summary.xlsx`
- `build/reports/default-target-view-lineage.xlsx`
- `build/reports/default-manual-confirmation-merged.xlsx`
- `build/reports/default-compare-detail.sql`
- `build/reports/default-compare-summary.txt`

### 明细 Excel

明细 Excel 输出全量字段记录，状态值默认使用中文：
- `一致`
- `不一致`
- `不适用`

数据库原始元数据列默认按原样输出，不做展示层转换：
- `源原始类型 / 目标原始类型`
- `源原始长度 / 目标原始长度`
- `源原始默认值 / 目标原始默认值`
- `源原始可空 / 目标原始可空`

明细中还会额外输出：
- `类型长度联合判断`
  - 用 6 维输入综合判断：
    - `源端存在`
    - `目标端存在`
    - `源类型`
    - `目标类型`
    - `源长度`
    - `目标长度`

也就是说：
- Excel 里展示的是数据库实际读出来的原始值
- 是否一致只看右侧状态列
- 不会因为类型映射、长度归一化、默认值标准化而把原始展示值改掉

并且会额外带出：
- `差异分组`
  - `主差异`
  - `信息差异`
- `是否影响结果`
  - 方便区分真正失败项和仅提示项

当单个 sheet 超过 `.xlsx` 限制时会自动拆分为：
- `明细`
- `明细_2`
- `明细_3`
- ...

明细工作簿还会额外生成一个规则 sheet：
- `类型判等规则`

该 sheet 用来展示当前生效的类型判等原则：
- `原始类型集合`
- `比较归一类型`
- `判等说明`

这样你可以同时看到：
- 明细里字段本身的原始类型
- 系统判定“一致/不一致”时采用的当前生效规则

类型判等规则现在默认来自：
- [type-equality-rules.properties](D:/develop/java_develop/temp/db-compare-runnable/src/main/resources/type-equality-rules.properties)

这意味着：
- 程序会先加载这份默认规则文件
- 你可以直接修改这份文件，决定哪些原始类型应该判定为一致
- 如果某个环境还需要额外覆盖，可以继续使用 `dbcompare.options.type-mappings.*`
- Excel 里的 `类型判等规则` sheet 展示的是当前生效后的规则

### 汇总 Excel

汇总 Excel 现在采用“少量汇总 + 分类型明细”的结构：
- `汇总`
- `表级状态`
- `类型长度联合汇总`
- `字段存在明细`
- `类型明细`
- `长度明细`
- `默认值明细`
- `可空明细`

其中 `类型长度联合汇总` 是新增的表级聚合 sheet，按以下粒度汇总：
- `源数据库`
- `源Schema`
- `源表`
- `目标Schema`
- `目标对象`

并汇总这些统计项：
- `字段总数`
- `联合判断一致数`
- `联合判断类型不一致数`
- `联合判断目标长度大于源长度数`
- `联合判断目标长度小于源长度数`
- `联合判断缺少源字段数`
- `联合判断缺少目标字段数`
- `表级联合判断结论`

当配置关闭某类比对时，对应内容会在 Excel 中自动隐藏：
- `dbcompare.options.compare-default-value=false`
  - 明细 Excel 不再输出 `源默认值 / 目标默认值 / 默认值状态`
  - 汇总 Excel 不再输出 `默认值状态`，也不会生成 `默认值明细`
- `dbcompare.options.compare-nullable=false`
  - 明细 Excel 不再输出 `源端可空 / 目标端可空 / 可空状态`
  - 汇总 Excel 不再输出 `可空状态`，也不会生成 `可空明细`

`汇总` sheet 会把信息集中排版在一个页签里，包含：
- 视图 schema 划分
- 5 种比对类型的简单概况与占比
- 风险等级划分
- diff 分类占比
- 风险等级规则
- 状态色高亮，方便快速区分一致、一般风险和高风险
- 中文表头、中文状态值，便于直接交付业务侧查看

当开启：
- `dbcompare.options.source-column-missing-in-target-affect-result=true`

则“源对象存在字段、目标对象不存在字段”会按主差异处理：
- 影响 `diffCount`
- 影响表级状态
- 影响风险等级和汇总统计

当关闭：
- `dbcompare.options.source-column-missing-in-target-affect-result=false`

则该类差异只作为信息提示保留：
- 不影响 `diffCount`
- 不影响表级状态
- 明细里仍会保留对应记录，便于人工查看

同样的规则也适用于属性类差异：
- `dbcompare.options.type-mismatch-affect-result`
- `dbcompare.options.length-mismatch-affect-result`
- `dbcompare.options.length-target-longer-affect-result`
- `dbcompare.options.default-mismatch-affect-result`
- `dbcompare.options.nullable-mismatch-affect-result`

这些开关允许把“类型 / 长度 / 默认值 / 可空”做成：
- 继续参与比对和明细输出
- 但可以按需降级为信息差异
- 降级后不影响 `diffCount`、表级状态、风险等级和汇总统计

其中 `dbcompare.options.length-target-longer-affect-result` 是长度差异的特例开关：
- 第一版只对字符类型生效
  - `CHAR`
  - `VARCHAR`
  - `CHARACTER`
  - `CHARACTER VARYING`
  - `GRAPHIC`
  - `VARGRAPHIC`
- 当目标长度大于源长度时：
  - `true`：仍按主差异处理
  - `false`：降级为信息差异
- 数值类型、时间类型和精度/小数位场景不走这个开关

`表级状态` sheet 输出表级汇总结果，字段包括：
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

其中 `默认值状态 / 可空状态` 仅在对应比对项开启时才会出现。

5 类 detail 现在拆成独立 sheet，便于大数据量查看：
- `字段存在明细`
- `类型明细`
- `长度明细`
- `默认值明细`
- `可空明细`

如果某一类明细数据特别多，还会继续拆成：
- `字段存在明细_2`
- `类型明细_2`
- ...

### 目标 View 血缘 Excel

当配置为：
- `dbcompare.mode=TARGET_DRIVEN`
- `dbcompare.options.object-type=VIEW`

程序会额外生成一份独立 Excel，不改动原有明细 Excel 和汇总 Excel。

默认文件：
- `build/reports/default-target-view-lineage.xlsx`

该工作簿当前包含一个 sheet：
- `目标View血缘`

字段顺序固定为：
- `源数据库`
- `源Schema`
- `源表`
- `目标基表Schema`
- `目标基表`
- `目标View`
- `目标ViewSchema`

说明：
- 原有主报表中的 `目标Schema / 目标表` 继续保留，避免影响已有查看习惯
- 新增血缘 Excel 专门用来展示 `targetView -> targetTable` 的展开关系
- 如果同一条血缘关系因字段级明细重复出现，导出时会自动去重

### 人工确认融合 Excel

当配置为：
- `report.manualConfirmation.enabled=true`

并且提供测试组人工确认 Excel 后，程序会额外生成一份独立融合工作簿，不改动原有自动生成的明细 Excel、汇总 Excel 和目标 View 血缘 Excel。

默认文件：
- `build/reports/default-manual-confirmation-merged.xlsx`

当前工作簿包含这些 sheet：
- `说明`
- `人工确认融合明细`
- `表级融合状态`
- `确认结果汇总`
- `责任人汇总`
- `未匹配AI记录`
- `未匹配测试组记录`
- `歧义匹配记录`

融合逻辑说明：
- AI 结果直接使用程序内部结构化字段级差异结果，不再反向读取 AI 自己生成的 Excel
- 测试组 Excel 当前按“表名 + 不一致类型 + 字段名/详细信息”逐步增强匹配
- 融合明细会额外展示 `候选数`，方便判断本次匹配是不是唯一命中
- 匹配不上和存在多候选的记录会单独输出，并带上“未匹配原因 / 歧义原因”，便于人工复核

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
- `examples/demo-view-lineage`
- `examples/demo-view-lineage-large`
- `examples/demo-manual-confirmation`
- `examples/sql`

说明：
- `examples/demo` 用于本地演示跑通完整流程
- `examples/demo-final` 提供本次最终规则样例，包含 `TABLE_TO_VIEW`、主差异/信息差异、自定义类型映射
- `examples/demo-final-large` 提供放大后的最终规则样例，默认包含 12 个 schema、216 个 view、504 条血缘关系
- `examples/demo-large` 提供大样本 snapshot 数据，默认生成 12 个 schema、120 张表，并混合字段缺失、类型、长度、默认值、nullable 等离散差异
- `examples/demo-view-lineage` 提供 `TARGET_DRIVEN + VIEW` 的最小样例，包含 View 血缘文件
- `examples/demo-view-lineage-large` 提供放大后的 `TARGET_DRIVEN + VIEW` 大样本数据，默认包含 12 个 schema、144 张源表、144 个目标 View 和 336 条血缘关系
- `examples/demo-manual-confirmation` 提供测试组人工确认融合样例，包含人工确认 Excel、目标 View 血缘和融合输出配置
- `examples/sql` 提供 DB2 与 openGauss 测试建表脚本
- `scripts/generate_large_demo.ps1` 可重新生成大样本 demo 数据
- `scripts/generate_large_final_demo.ps1` 可重新生成放大版最终规则 demo 数据
- `scripts/generate_large_view_lineage_demo.ps1` 可重新生成大样本目标 View 血缘 demo 数据

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
