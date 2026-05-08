## ADDED Requirements

### Requirement: 运行时 API 版本分支

`core-render-ohos` 的 `ComponentsRegisterEntry` SHALL 在注册 `"KRTextFieldView"` / `"KRTextAreaView"` 的 ViewCreator 闭包内，通过 `OH_GetSdkApiVersion()` 在**运行时**判断当前设备的 API 版本，分别路由到 **新实现**（`KRTextEditorFieldView` / `KRTextEditorAreaView`）或 **老实现**（`KRTextFieldView` / `KRTextAreaView`）。

#### Scenario: API 24 及以上使用新实现

- **GIVEN** 设备运行 HarmonyOS API 24 及以上
- **WHEN** 业务 DSL `Input { }` 触发 core 层创建 `"KRTextFieldView"` 类型的 View
- **THEN** 注册闭包 SHALL 调用 `std::make_shared<KRTextEditorFieldView>()`
- **AND** 返回值 SHALL 被 `static_pointer_cast` 到 `std::shared_ptr<IKRRenderViewExport>` 后交给渲染管线

- **GIVEN** 设备运行 HarmonyOS API 24 及以上
- **WHEN** 业务 DSL `TextArea { }` 触发 core 层创建 `"KRTextAreaView"` 类型的 View
- **THEN** 注册闭包 SHALL 调用 `std::make_shared<KRTextEditorAreaView>()`

#### Scenario: API 23 及以下使用老实现

- **GIVEN** 设备运行 HarmonyOS API 23 或更低版本
- **WHEN** 业务 DSL `Input { }` / `TextArea { }` 触发创建
- **THEN** 注册闭包 SHALL 分别调用 `std::make_shared<KRTextFieldView>()` / `std::make_shared<KRTextAreaView>()`
- **AND** 老实现的行为 SHALL 完全不受本 change 影响

#### Scenario: API 版本查询失败兜底

- **GIVEN** `OH_GetSdkApiVersion()` 返回值 < 24（包括 0 / -1 等异常值）
- **WHEN** 注册闭包被触发
- **THEN** 注册闭包 SHALL 走老实现路径，**不**创建新实现
- **AND** 这保证了未知 / 异常情况下行为降级到已知可用的路径

### Requirement: 版本查询缓存

注册闭包内 SHALL 使用 `static const int` 局部变量缓存 `OH_GetSdkApiVersion()` 的首次返回值，避免每次创建 View 时都执行一次系统调用。

#### Scenario: 缓存命中

- **GIVEN** 首次调用 `"KRTextFieldView"` 的注册闭包
- **WHEN** 闭包执行
- **THEN** SHALL 执行一次 `OH_GetSdkApiVersion()` 并缓存到 `static const int gApiVersion`

- **GIVEN** 同一进程内第 N 次（N > 1）调用同一闭包
- **WHEN** 闭包执行
- **THEN** SHALL 直接读取 `gApiVersion`，**不**再次调用 `OH_GetSdkApiVersion()`

#### Scenario: 两个 ViewName 的缓存独立

- **GIVEN** `"KRTextFieldView"` 与 `"KRTextAreaView"` 的注册闭包是两个独立的 lambda
- **THEN** 两者各自拥有独立的 `static` 缓存变量
- **AND** 两者的语义等价（同一进程内 `OH_GetSdkApiVersion()` 恒定，所以两次缓存值相同）

### Requirement: ViewName 保持不变

本 change SHALL **不**引入新的 ViewName 字符串。`"KRTextFieldView"` / `"KRTextAreaView"` 继续作为唯一的 ViewName。

#### Scenario: 跨端协议零破坏

- **GIVEN** core 层 `InputView::viewName()` 返回 `"KRTextFieldView"`
- **AND** core 层 `TextAreaView::viewName()` 返回 `"KRTextAreaView"`
- **WHEN** 本 change 完成
- **THEN** 上述两个字符串 SHALL 不变
- **AND** `core/src/commonMain/kotlin/com/tencent/kuikly/core/base/ViewConst.kt` SHALL **不**新增常量
- **AND** `core/` 模块 SHALL **不**因此 change 而被改动

#### Scenario: ArkTS RichEditor 方案正交

- **GIVEN** 已有 ArkTS 层注册 `"KRRichTextFieldView"` / `"KRRichTextAreaView"`（`harmony-arkts-richeditor-textfield` change）
- **WHEN** 本 change 完成
- **THEN** 两者 SHALL 正交共存：业务用 `Input { }` / `TextArea { }` 走 capi 路径（新老二选一），用 `RichInput { }` / `RichTextArea { }` 走 ArkTS RichEditor 路径

### Requirement: 老实现源码零改动

`KRTextFieldView.h` / `KRTextFieldView.cpp` / `KRTextAreaView.h` / `KRTextAreaView.cpp` SHALL 在本 change 中**不被改动**（除了可能的 include 顺序调整）。

#### Scenario: 静态验证

- **GIVEN** 本 change 合入前后的 diff
- **WHEN** 对比 4 个老实现文件的修改行数
- **THEN** 修改行数 SHALL 为 0
- **AND** 所有新增代码 SHALL 位于新文件（`KRTextEditorCommon.h` / `KRTextEditorFieldView.h|cpp` / `KRTextEditorAreaView.h|cpp`）或注册入口 / CMake

### Requirement: 构建系统登记

新增的 `.cpp` 源文件 SHALL 被加入 CMake 构建列表，保证链接期符号可用。

#### Scenario: CMake 收录新源文件

- **GIVEN** `core-render-ohos/src/main/cpp/CMakeLists.txt`
- **WHEN** 本 change 完成
- **THEN** 该文件 SHALL 在现有 `KRTextFieldView.cpp` / `KRTextAreaView.cpp` 条目之后新增：
  - `libohos_render/expand/components/input/KRTextEditorFieldView.cpp`
  - `libohos_render/expand/components/input/KRTextEditorAreaView.cpp`
- **AND** 若 `KRTextEditorCommon` 需要独立 cpp 实现，SHALL 一并登记

### Requirement: 回滚可控

本 change SHALL 提供**单点回滚**能力：只需修改注册闭包内一条语句即可回到"全量使用老实现"状态。

#### Scenario: 单点回滚

- **GIVEN** 新实现出现严重线上问题
- **WHEN** 开发者将注册闭包内的 `gApiVersion >= 24` 改为 `false`
- **THEN** 所有设备 SHALL 一律走老实现路径
- **AND** 无需删除 `KRTextEditorFieldView` / `KRTextEditorAreaView` 相关源文件
- **AND** 回滚 diff SHALL 仅涉及 `ComponentsRegisterEntry.h` 的两处布尔表达式

### Requirement: 业务透明切换

业务代码（core 层、Compose DSL 层、demo）SHALL **不感知**该版本分支的存在。

#### Scenario: 业务代码零改动

- **GIVEN** 现有业务代码使用 `Input { attr { placeholder("...") } }` 或 `TextArea { attr { ... } }`
- **WHEN** 本 change 合入并部署到 API 24+ 设备
- **THEN** 业务代码 SHALL 零改动仍正常工作
- **AND** 可见行为 SHALL 与 API 23 设备（或老实现路径）等价

#### Scenario: 跨端协议零改动

- **GIVEN** Android / iOS / Web / 小程序端的渲染器实现
- **WHEN** 本 change 合入
- **THEN** 其他端的 `"KRTextFieldView"` / `"KRTextAreaView"` 实现 SHALL 不被影响
- **AND** `setProp` / `callMethod` / `event` 协议 SHALL 不变
