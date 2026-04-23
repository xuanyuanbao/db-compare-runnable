# 字段类型与长度联合判断规则开发文档

## 1. 文档目的

本文档用于在当前项目中新增一个“字段类型与长度联合判断”能力。

这项能力的定位是：

- 在现有字段比对能力之上新增一层增强判断
- 不是替换当前的字段存在判断
- 也不是删除当前的“类型状态”“长度状态”
- 而是新增一个更贴近业务语义的联合判断结果

这份文档同时兼容两部分背景：

- 当前 Java 项目已经实现的字段对比能力
- 你提供的历史 `TypeDiff` 规则说明

---

## 2. 当前项目基线

当前项目已经具备以下能力：

- 读取数据库原始字段信息
  - 原始类型
  - 原始长度
  - 原始默认值
  - 原始可空值
- 独立判断字段是否存在
  - `源端存在`
  - `目标端存在`
- 独立判断字段属性状态
  - `类型状态`
  - `长度状态`
  - `默认值状态`
  - `可空状态`
- 支持主差异 / 信息差异
- 支持是否影响结果的开关
- 支持类型规则文件
- 支持 Excel / CSV / SQL / 汇总输出

当前主要落点：

- 主比较逻辑：
  - [TableCompareService.java](/D:/develop/java_develop/temp/db-compare-runnable/src/main/java/com/example/dbcompare/service/TableCompareService.java)
- 类型规则与归一能力：
  - [TypeNormalizer.java](/D:/develop/java_develop/temp/db-compare-runnable/src/main/java/com/example/dbcompare/util/TypeNormalizer.java)
- 类型规则文件加载：
  - [TypeRuleFileLoader.java](/D:/develop/java_develop/temp/db-compare-runnable/src/main/java/com/example/dbcompare/config/TypeRuleFileLoader.java)
- 默认规则文件：
  - [type-equality-rules.properties](/D:/develop/java_develop/temp/db-compare-runnable/src/main/resources/type-equality-rules.properties)

---

## 3. 本次增强的核心结论

### 3.1 这是一个新增能力

本次开发的是：

- `类型长度联合判断`

它是新增的判断结果，不替换现有字段。

现有字段继续保留：

- `源端存在`
- `目标端存在`
- `类型状态`
- `长度状态`

新增字段建议为：

- `类型长度联合判断`

说明：

- 现有字段用于保留细粒度信息
- 新增字段用于输出最终联合语义
- 导出层、汇总层后续可以逐步转向使用该联合结果

### 3.2 不是 4 字段模型，而是 6 维联合判断模型

表面上这个规则看起来输入是：

- `源类型`
- `目标类型`
- `源长度`
- `目标长度`

但从真实业务语义看，它其实是一个 6 维联合判断模型：

- `源端存在`
- `目标端存在`
- `源类型`
- `目标类型`
- `源长度`
- `目标长度`

原因是：

- `字段不存在` 和 `类型为空` 在业务上不能简单视为同一种概念
- 是否进入“类型长度联合判断”，首先取决于字段两端是否存在
- 所以存在性是联合判断的前置条件

---

## 4. 新规则与现有规则的关系

### 4.1 不处理字段是否存在

本次新增的“类型长度联合判断”不负责判断：

- 是否缺少源字段
- 是否缺少目标字段

因为这部分当前系统已经有明确字段：

- `源端存在`
- `目标端存在`

所以这次新规则的边界是：

1. 先由现有逻辑判断字段是否存在
2. 只有“源端存在 = 是 且 目标端存在 = 是”时
3. 才进入“类型长度联合判断”

### 4.2 不删除旧字段

这次不替换、不删除以下字段：

- `类型状态`
- `长度状态`

原因：

- 这些字段仍然能提供细粒度排查信息
- 当前汇总、差异输出、开关判断也仍然依赖它们
- 新增联合判断更适合作为“主语义结果”，而不是立刻替代底层明细字段

---

## 5. 新增字段设计

建议新增字段：

```text
类型长度联合判断
```

建议内部字段名：

```java
typeLengthCombinedStatus
```

建议候选值：

- `缺少源字段`
- `缺少目标字段`
- `一致`
- `类型不一致`
- `目标长度大于源长度`
- `目标长度小于源长度`

说明：

- 虽然本次规则本身不负责“字段是否存在”判断
- 但联合结果对象仍然可以承载这两种状态
- 这样更完整，也便于统一输出
- 只是生成顺序上，存在性结果应优先来自现有逻辑

### 5.1 汇总工作簿新增表级聚合 Sheet

除明细层新增 `类型长度联合判断` 字段外，汇总 Excel 中还需要新增一个独立 Sheet，
用于按“表级/目标对象级”聚合展示联合判断结果。

建议 Sheet 名称：

```text
类型长度联合汇总
```

设计目标：

- 不替换现有 `汇总`、`表级状态`、各类明细 Sheet
- 专门服务于“按表观察联合判断结果”
- 将字段级联合判断结果聚合成表级统计结果
- 让使用者快速看到每张表/视图整体的联合判断分布情况

聚合粒度建议：

- `源数据库`
- `源Schema`
- `源表`
- `目标Schema`
- `目标对象`

其中：

- `TABLE_TO_TABLE` 下，`目标对象` 为目标表名
- `TABLE_TO_VIEW` 下，`目标对象` 为目标视图名

建议输出字段至少包括：

- `源数据库`
- `源Schema`
- `源表`
- `目标Schema`
- `目标对象`
- `字段总数`
- `联合判断一致数`
- `联合判断类型不一致数`
- `联合判断目标长度大于源长度数`
- `联合判断目标长度小于源长度数`
- `联合判断缺少源字段数`
- `联合判断缺少目标字段数`
- `表级联合判断结论`

表级联合判断结论建议规则：

- 只要存在 `缺少源字段` 或 `缺少目标字段`，优先标记为 `存在性异常`
- 否则只要存在 `类型不一致`，标记为 `类型不一致`
- 否则只要存在 `目标长度小于源长度`，标记为 `目标长度不足`
- 否则如果仅存在 `目标长度大于源长度`，标记为 `目标更宽松`
- 否则标记为 `一致`

说明：

- 这个 Sheet 是“表级聚合结果”，不是新增一套独立比较逻辑
- 它的数据来源仍然是字段级 `类型长度联合判断`
- 后续如果需要按风险颜色展示，也应优先基于这个表级结论做样式映射

---

## 6. 规则输入

建议规则判断器输入如下：

```java
TypeLengthRuleResult compare(
    boolean sourceExists,
    boolean targetExists,
    DatabaseType sourceDatabaseType,
    DatabaseType targetDatabaseType,
    String sourceType,
    String targetType,
    String sourceLength,
    String targetLength
)
```

说明：

- `sourceExists / targetExists` 是 6 维模型里的前置条件
- `sourceType / targetType / sourceLength / targetLength` 使用数据库原始值
- `sourceDatabaseType / targetDatabaseType` 为后续扩展数据库特例预留

---

## 7. 输入预处理

进入规则前，对字符串输入统一做预处理：

1. `null -> ""`
2. 去掉首尾空格
3. 转大写

建议工具方法：

```java
private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase();
}
```

注意：

- 这里只是规则层内部预处理
- 不改变 Excel 中展示的数据库原始值

---

## 8. 输出设计

建议不要直接返回中文字符串，建议返回枚举 + 结果对象。

### 8.1 结果枚举

```java
public enum TypeLengthRuleCode {
    MISSING_SOURCE,
    MISSING_TARGET,
    MATCH,
    TYPE_MISMATCH,
    TARGET_LENGTH_LONGER,
    TARGET_LENGTH_SHORTER
}
```

### 8.2 结果对象

```java
public class TypeLengthRuleResult {
    private TypeLengthRuleCode code;
    private String message;
    private String normalizedSourceType;
    private String normalizedTargetType;
}
```

展示层映射：

- `MISSING_SOURCE` -> `缺少源字段`
- `MISSING_TARGET` -> `缺少目标字段`
- `MATCH` -> `一致`
- `TYPE_MISMATCH` -> `类型不一致`
- `TARGET_LENGTH_LONGER` -> `目标长度大于源长度`
- `TARGET_LENGTH_SHORTER` -> `目标长度小于源长度`

---

## 9. 主流程规则

### 9.1 存在性优先

规则优先级最高。

#### 规则 1

如果：

- `sourceExists = false`

返回：

- `MISSING_SOURCE`

#### 规则 2

如果：

- `targetExists = false`

返回：

- `MISSING_TARGET`

#### 规则 3

只有当：

- `sourceExists = true`
- `targetExists = true`

才进入后续类型/长度联合判断。

---

### 9.2 字符型规则

当源类型属于字符型集合时，进入字符型规则。

源字符型集合：

- `CHAR`
- `VARCHAR`

目标字符型集合：

- `CHAR`
- `VARCHAR`
- `BPCHAR`

目标大文本集合：

- `TEXT`
- `MEDIUMTEXT`
- `LONGTEXT`

#### 规则 4

如果：

- `sourceType in [CHAR, VARCHAR]`
- `targetType in [CHAR, VARCHAR, BPCHAR]`

则进入普通长度比较。

#### 规则 5

如果：

- `sourceType in [CHAR, VARCHAR]`
- `targetType in [TEXT, MEDIUMTEXT, LONGTEXT]`

则返回：

- `TARGET_LENGTH_LONGER`

---

### 9.3 数值型规则

源数值型集合：

- `BIGINT`
- `NUMERIC`
- `DECIMAL`
- `INTEGER`

目标数值型集合：

- `BIGINT`
- `NUMERIC`
- `INT4`
- `INT8`
- `UINT8`
- `INT`
- `INTEGER`

#### 规则 6

如果：

- `sourceType in [BIGINT, NUMERIC, DECIMAL, INTEGER]`
- `targetType in [BIGINT, NUMERIC, INT4, INT8, UINT8, INT, INTEGER]`

则进入数值长度比较。

#### 规则 7

如果：

- `sourceType in [BIGINT, NUMERIC, DECIMAL, INTEGER]`
- `targetType == FLOAT4`

则：

- `sourcePrecision <= 7` -> `MATCH`
- `sourcePrecision > 7` -> `TYPE_MISMATCH`

#### 规则 8

如果：

- `sourceType in [BIGINT, NUMERIC, DECIMAL, INTEGER]`
- `targetType == FLOAT8`

则：

- `sourcePrecision <= 15` -> `MATCH`
- `sourcePrecision > 15` -> `TYPE_MISMATCH`

---

### 9.4 通用兜底规则

#### 规则 9

如果：

- `sourceType != targetType`

返回：

- `TYPE_MISMATCH`

#### 规则 10

如果：

- `sourceType == targetType`

则继续比较长度。

---

## 10. 长度比较规则

### 10.1 普通长度比较

适用于：

- `CHAR`
- `VARCHAR`
- `BPCHAR`

规则：

- 相等 -> `MATCH`
- 目标大于源 -> `TARGET_LENGTH_LONGER`
- 目标小于源 -> `TARGET_LENGTH_SHORTER`

### 10.2 数值长度比较

适用于：

- `10`
- `15,2`
- `9,5`

规则：

1. 字符串完全相等 -> `MATCH`
2. 解析 `precision / scale`
3. 如果二者完全相等 -> `MATCH`
4. 如果：
   - `targetPrecision >= sourcePrecision`
   - 且 `targetScale >= sourceScale`
   -> `TARGET_LENGTH_LONGER`
5. 否则 -> `TARGET_LENGTH_SHORTER`

---

## 11. 与当前项目的集成方式

### 11.1 新增，不替换

本次新增：

- `类型长度联合判断`
- `类型长度联合汇总` Sheet

本次不替换：

- `类型状态`
- `长度状态`

### 11.2 当前字段职责划分

建议职责如下：

- `源端存在 / 目标端存在`
  - 负责存在性基础判断
- `类型状态`
  - 继续保留，用于细粒度类型不一致排查
- `长度状态`
  - 继续保留，用于细粒度长度不一致排查
- `类型长度联合判断`
  - 作为增强后的联合语义结果
- `类型长度联合汇总`
  - 作为汇总工作簿中的表级聚合视图

### 11.3 与现有 CompareOptions 保持兼容

以下开关继续保留：

- `dbcompare.options.compare-type`
- `dbcompare.options.compare-length`
- `dbcompare.options.type-mismatch-affect-result`
- `dbcompare.options.length-mismatch-affect-result`
- `dbcompare.options.length-target-longer-affect-result`

联合判断结果本身只负责产出语义，不直接改变这些开关的职责。

### 11.4 与用户规则文件兼容

现有规则文件：

- [type-equality-rules.properties](/D:/develop/java_develop/temp/db-compare-runnable/src/main/resources/type-equality-rules.properties)

建议优先级：

1. 用户规则文件显式命中
2. 特殊业务规则命中
3. 通用兜底规则

这样可以保证：

- 用户仍然保有最高控制权
- 业务增强规则不会无意覆盖用户配置

---

## 12. 第一阶段建议交付范围

建议第一阶段只做：

1. 新增 `TypeLengthRuleCode`
2. 新增 `TypeLengthRuleResult`
3. 新增 `TypeDiffComparator`
4. 把 6 维联合判断接入现有 `TableCompareService`
5. 输出新增字段 `类型长度联合判断`
6. 保留现有 `类型状态` 和 `长度状态`
7. 在汇总工作簿中新增 `类型长度联合汇总` Sheet
8. 补单元测试

第一阶段不做：

- 默认值规则
- 可空规则
- 字段存在规则重构
- 旧字段移除

---

## 13. 后续可扩展方向

这套模型后续可以继续扩展到其他类的对比：

### 13.1 默认值规则类

例如：

- `NULL` vs 空串
- `CURRENT DATE` vs `CURRENT_DATE`
- `'0'` vs `0`

### 13.2 可空规则类

例如：

- `YES / Y / TRUE`
- `NO / N / FALSE`

### 13.3 字段存在规则类

虽然本次不纳入该联合判断，但后续可以独立规则化。

### 13.4 数值精度规则类

例如：

- `DECIMAL(15,2)` vs `FLOAT8`
- `NUMERIC(10,0)` vs `BIGINT`

### 13.5 大文本兼容规则类

例如：

- `VARCHAR -> TEXT`
- `CHAR -> LONGTEXT`

---

## 14. 测试要求

至少覆盖：

1. 源不存在
2. 目标不存在
3. 字符型等长
4. 字符型目标更长
5. 字符型目标更短
6. 字符型转 TEXT
7. DECIMAL 与 NUMERIC 精度相同
8. DECIMAL 与 NUMERIC 目标更大
9. DECIMAL 与 NUMERIC 目标更小
10. DECIMAL 转 FLOAT4
11. DECIMAL 转 FLOAT8
12. 用户规则文件优先级覆盖

---

## 15. 最终结论

本次规则增强的正式口径是：

- 这是一个 **6 维联合判断模型**
- 这是一个 **新增判断结果**
- 它 **不替换** 当前的 `类型状态` 和 `长度状态`
- 它 **不接管** 当前的 `源端存在 / 目标端存在` 判断

最适合的落地方式是：

1. 新增独立规则类
2. 挂接当前主流程
3. 先新增联合结果字段
4. 保留旧字段用于兼容和排查

这样风险最小，也最符合当前项目已经形成的输出结构与配置体系。
