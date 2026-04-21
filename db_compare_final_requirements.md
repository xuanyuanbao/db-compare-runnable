# 数据库对比工具重构需求说明书（新版·含信息类差异）

## 一、项目背景
当前数据库对比工具存在：
- 全量扫描（笛卡尔积），性能差
- view 默认全字段，误报严重
- 对比维度过多（默认值、nullable）
- 类型不支持跨库映射
- 无法按批次控制

---

## 二、重构目标
1. 支持 TABLE_TO_TABLE / TABLE_TO_VIEW
2. 对比规则可配置
3. 支持类型映射
4. 支持 view 批次控制
5. 避免笛卡尔积
6. 输出结构化结果（主差异 + 信息差异）

---

## 三、对比模式
compare-mode:
  - TABLE_TO_TABLE
  - TABLE_TO_VIEW

---

## 四、对比维度（精简）
compare-options:
  check-exists: true
  check-type: true
  check-length: true
  check-default: false
  check-nullable: false
  source-column-missing-in-target-affect-result: false
  type-mismatch-affect-result: true
  length-mismatch-affect-result: true
  length-target-longer-affect-result: false
  default-mismatch-affect-result: true
  nullable-mismatch-affect-result: true

---

## 五、类型映射
type-mapping:
  varchar: [varchar, char, character varying]
  int: [int, integer, number]
  date: [date, timestamp]

---

## 六、View 对比逻辑（核心）

### 主逻辑（以 view 为基准）
for each view:
    找到 table
    for each column in view:
        校验存在
        校验类型
        校验长度

### 补充逻辑（新增）
for each column in table:
    if column not in view:
        标记为 VIEW_MISSING_COLUMN_INFO

### 新增开关逻辑（本次补充）
当“源库表/视图存在字段，但目标库表/视图不存在该字段”时，
是否影响全局结果，不再写死，而是由配置项控制：

compare-options:
  source-column-missing-in-target-affect-result: true | false
  type-mismatch-affect-result: true | false
  length-mismatch-affect-result: true | false
  length-target-longer-affect-result: true | false
  default-mismatch-affect-result: true | false
  nullable-mismatch-affect-result: true | false

规则如下：
- `true`
  - 该类差异归入“主差异”
  - 影响表级状态
  - 影响最终 diffCount / 风险等级 / 汇总统计
- `false`
  - 该类差异归入“信息差异”
  - 不影响表级状态
  - 不影响最终 diffCount，只用于提示和补充说明

适用范围：
- `TABLE_TO_TABLE`
- `TABLE_TO_VIEW`
- 源对象可以是 source table，也可以是 source view
- 目标对象可以是 target table，也可以是 target view

### 长度差异特例开关（新增需求）
当发生“长度不一致”时，如果只是：
- 源长度 < 目标长度
- 也就是目标字段长度更长、理论上更宽松

则允许通过单独开关控制这类差异是否影响全局结果：

compare-options:
  length-target-longer-affect-result: true | false

规则如下：
- `true`
  - 即使目标长度更长，仍按主差异处理
  - 影响表级状态、diffCount、风险等级、汇总统计
- `false`
  - 目标长度更长的场景按信息差异处理
  - 仍保留长度不一致明细
  - 不影响表级状态、diffCount、风险等级、汇总统计

补充说明：
- 该开关只作用于“长度不一致”里的子场景：`targetLength > sourceLength`
- 第一版只作用于字符类型：
  - `CHAR`
  - `VARCHAR`
  - `CHARACTER`
  - `CHARACTER VARYING`
  - `GRAPHIC`
  - `VARGRAPHIC`
- 如果是 `targetLength < sourceLength`，仍按普通长度不一致规则处理
- 数值类型、时间类型、精度/小数位场景不走这个开关
- 该开关优先级高于普通长度差异结果判定，用于细化 `length-mismatch-affect-result`
- 前提是 `check-length=true`

---

## 七、关键原则

| 项目 | 是否校验 | 是否影响结果 |
|------|----------|--------------|
| view字段缺失 | ✅ | ✅ |
| 类型不一致 | ✅ | 由开关控制 |
| 长度不一致 | ✅ | 由开关控制 |
| 目标长度更长 | ✅ | 由专门开关控制 |
| 默认值不一致 | 按配置启用 | 由开关控制 |
| 可空不一致 | 按配置启用 | 由开关控制 |
| 源有目标无字段 | ✅ | 由开关控制 |

---

## 八、批次控制
view-batch:
  enabled: true
  include-views:
    - view_a
    - view_b

---

## 九、性能要求
- 禁止笛卡尔积
- 使用 view 驱动
- 单表查询

---

## 十、差异分类

| 类型 | 是否影响结果 |
|------|--------------|
| MISSING_COLUMN | 是 |
| TYPE_MISMATCH | 由开关控制 |
| LENGTH_MISMATCH | 由开关控制 |
| LENGTH_MISMATCH(target longer) | 由专门开关控制 |
| DEFAULT_MISMATCH | 由开关控制 |
| NULLABLE_MISMATCH | 由开关控制 |
| SOURCE_COLUMN_MISSING_IN_TARGET | 由开关控制 |
| VIEW_MISSING_COLUMN_INFO | 否（兼容旧口径时保留） |

---

## 十一、输出结构

{
  "table": "xxx",
  "mainDiffs": [...],
  "infoDiffs": [...]
}

---

## 十二、风险等级

compare-options:
  table-extra-columns-affect-result: false
  table-extra-columns-risk-level: LOW
  source-column-missing-in-target-affect-result: false
  type-mismatch-affect-result: true
  length-mismatch-affect-result: true
  length-target-longer-affect-result: false
  default-mismatch-affect-result: true
  nullable-mismatch-affect-result: true

说明：
- 当 `source-column-missing-in-target-affect-result=false` 时，
  “源有目标无字段”按信息类差异处理
- 当 `source-column-missing-in-target-affect-result=true` 时，
  “源有目标无字段”按主差异处理
- 当 `type-mismatch-affect-result=false`、`length-mismatch-affect-result=false`、
  `default-mismatch-affect-result=false` 或 `nullable-mismatch-affect-result=false` 时，
  对应属性差异继续展示，但按信息类差异处理
- 当 `length-target-longer-affect-result=false` 时，
  “目标长度更长”的长度差异按信息类差异处理
- 当 `length-target-longer-affect-result=true` 时，
  “目标长度更长”的长度差异仍按主差异处理

---

## 十三、验收标准
- 支持两类对比模式
- 支持类型映射
- 支持批次控制
- 主差异与信息差异分离
- “源有目标无字段”支持开关控制是否影响全局结果
- “类型 / 长度 / 默认值 / 可空”支持独立开关控制是否影响全局结果
- “目标长度更长”支持单独开关控制是否影响全局结果
- 性能提升明显

---

## 十四、核心总结
构建一个“可配置 + 模式化 + 批次化 + 高性能”的数据库对比引擎
