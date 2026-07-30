# 单元测试模块 (commonTest)

## 概述

本目录包含表格组件的单元测试，使用 Kotlin Test 框架，覆盖所有数据模型和 DSL 构建器。

## 测试文件

| 文件 | 测试对象 | 测试用例数 |
|------|----------|------------|
| `TableAttrTest.kt` | TableAttr 属性 | 11 |
| `TableEventTest.kt` | TableEvent 事件 | 5 |
| `model/CellDataTest.kt` | CellData 模型 | 4 |
| `model/ColumnDefTest.kt` | ColumnDef 模型 | 5 |
| `model/TableDataTest.kt` | TableData 模型 | 6 |
| `model/TableThemeTest.kt` | TableTheme 主题 | 5 |
| `model/TextAlignTest.kt` | TextAlign 枚举 | 4 |
| `dsl/TableDslTest.kt` | TableDsl 构建器 | 18 |

**总计: 8 个测试文件，58 个测试用例**

## 运行测试

```bash
# 运行所有 shared 模块单元测试
./gradlew :shared:testDebugUnitTest

# 仅运行 JVM 测试（不需要 Android 模拟器）
./gradlew :shared:jvmTest
```

## 测试覆盖范围

### 属性测试 (TableAttr)
- 默认值验证
- 列/行数据设置
- 固定列数量边界（超出时 clamp）
- 宽度计算（totalWidth / fixedWidth / scrollableWidth）
- 表头/滚动开关

### 事件测试 (TableEvent)
- 默认 handler 为 null
- 各事件 handler 注册与回调
- 回调参数正确传递

### 模型测试
- 数据类默认值
- data class 相等性
- 边界条件（越界访问返回空值）
- 预设主题值验证
- 自定义主题构建

### DSL 测试 (TableDsl)
- 列构建（基础/对齐/固定/自定义渲染）
- 行构建（单行/批量）
- data() 方法填充
- 主题设置（预设/DSL）
- 事件处理器注册

## 相关模块

- [core/](../commonMain/kotlin/com/kuikly/table/) — 被测试的核心组件
- [model/](../commonMain/kotlin/com/kuikly/table/model/) — 被测试的数据模型
- [dsl/](../commonMain/kotlin/com/kuikly/table/dsl/) — 被测试的 DSL 模块
