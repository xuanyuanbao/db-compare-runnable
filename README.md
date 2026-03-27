# db-compare-runnable

这是一个可直接运行的 Java 版数据库表结构对比项目骨架，而且这次不是空壳。

它包含两种运行方式：

1. **SNAPSHOT 模式**：直接读取 CSV 元数据快照，不需要任何第三方 JDBC 驱动，开箱就能跑。
2. **JDBC 模式**：通过 `DatabaseMetaData` 读取 AS400 / DB2 / Gauss 元数据。这个模式能用于真实数据库，但你需要自行把对应厂商的 JDBC 驱动 jar 放到运行时 classpath。

## 目录说明

- `src/main/java`：核心源码
- `examples/demo`：可直接运行的演示数据
- `examples/jdbc-template.properties`：真实 JDBC 配置模板
- `scripts/build.sh`：JDK 直接编译脚本
- `scripts/run-demo.sh`：一键跑演示

## 一键跑演示

前提：本机安装 JDK 17+。

```bash
cd db-compare-runnable
./scripts/run-demo.sh
```

成功后会生成：

- `build/reports/demo-compare-report.csv`
- `build/reports/demo-compare-summary.txt`

## 手动运行

```bash
cd db-compare-runnable
./scripts/build.sh
java -cp out com.example.dbcompare.app.CompareApplication examples/demo/demo.properties
```

## 真实 JDBC 模式

1. 复制并修改 `examples/jdbc-template.properties`
2. 准备驱动 jar
3. 编译源码
4. 运行时把驱动 jar 放进 classpath

示例：

```bash
./scripts/build.sh
java -cp "out:libs/*" com.example.dbcompare.app.CompareApplication examples/jdbc-template.properties
```

Windows：

```bat
scripts\build.sh
java -cp "out;libs/*" com.example.dbcompare.app.CompareApplication examples\jdbc-template.properties
```

## 配置说明

### 基础配置

```properties
source.count=2
source.1.name=AS400_A
source.1.type=AS400
source.1.jdbcUrl=jdbc:as400://...
source.1.username=...
source.1.password=...
source.1.driverClassName=com.ibm.as400.access.AS400JDBCDriver

source.2.name=DB2_B
source.2.type=DB2
source.2.jdbcUrl=jdbc:db2://...
source.2.username=...
source.2.password=...
source.2.driverClassName=com.ibm.db2.jcc.DB2Driver

target.name=GAUSS
target.type=GAUSS
target.jdbcUrl=jdbc:postgresql://...
target.username=...
target.password=...
target.driverClassName=org.postgresql.Driver
```

### 映射规则

```properties
mapping.count=2
mapping.1.sourceDatabaseName=AS400_A
mapping.1.targetSchemaName=T_AS400_A
mapping.2.sourceDatabaseName=DB2_B
mapping.2.targetSchemaName=T_DB2_B
```

### 表级覆盖映射

```properties
tableMapping.count=1
tableMapping.1.sourceDatabaseName=AS400_A
tableMapping.1.sourceSchemaName=LEGACYA
tableMapping.1.sourceTableName=OLD_TABLE
tableMapping.1.targetSchemaName=T_AS400_A
tableMapping.1.targetTableName=NEW_TABLE
```

### 可选过滤

```properties
compare.includeSchemas=LEGACYA,LEGACYB
compare.excludeTables=TMP_A,TMP_B
```

## 当前支持的对比项

- 表是否存在
- 字段是否存在
- 字段类型是否一致
- 字段长度是否一致
- 默认值是否一致
- nullable 是否一致

## 说明

这版代码是**能跑的完整版工程基础版**，但有一个必须说清楚的点：

- **SNAPSHOT 模式**：我已经保证它可以直接跑。
- **真实 AS400 / DB2 / Gauss JDBC 模式**：代码已补全，但由于厂商驱动通常有分发限制，我没有把这些驱动 jar 放进项目里。你本地补上驱动后即可运行。

## 适合你下一步继续补的地方

- 针对 AS400 / DB2 / Gauss 做更细的类型兼容规则
- 增加 Excel 报告导出
- 增加按单库 / 单 schema / 单表执行
- 增加白名单 / 黑名单文件
- 增加 Spring Boot 包装和 REST 接口
