# 2.25.0 破坏性与迁移

> 本版导航：[版本说明](./announcement.md) · [变更汇总](./changelog.md) · **破坏性与迁移**

## 条件性破坏性

### #1596 鸿蒙 C 头文件枚举值改名：`NULL` → `NULL_VALUE`

- 变化：鸿蒙对外头文件 `KRRenderCValue.h` 的 `enum Type` 中 `NULL` 改名为 `NULL_VALUE`（与权威头文件统一）。
- 触发条件：宿主原生扩展代码中直接引用了 `KRRenderCValue` 的 `NULL` 枚举值。
- 迁移：将引用处改为 `NULL_VALUE`。
- 证据：#1596

## 扫描依据

[2.24.0...2.25.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.24.0...2.25.0) 共 18 个 commit。按业务能力面（属性 / 事件 / 内置组件 / 内置 Module / 页面注解）核对：除上述鸿蒙 C 头文件枚举值改名外，其余为修复、性能优化与头文件整理，不影响业务侧能力。
