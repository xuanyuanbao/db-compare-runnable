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

---

## 七、关键原则

| 项目 | 是否校验 | 是否影响结果 |
|------|----------|--------------|
| view字段缺失 | ✅ | ✅ |
| 类型不一致 | ✅ | ✅ |
| 长度不一致 | ✅ | ✅ |
| table多余字段 | ✅（仅记录） | ❌ |

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
| TYPE_MISMATCH | 是 |
| LENGTH_MISMATCH | 是 |
| VIEW_MISSING_COLUMN_INFO | 否 |

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

---

## 十三、验收标准
- 支持两类对比模式
- 支持类型映射
- 支持批次控制
- 主差异与信息差异分离
- 性能提升明显

---

## 十四、核心总结
构建一个“可配置 + 模式化 + 批次化 + 高性能”的数据库对比引擎
