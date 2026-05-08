## Why

HarmonyOS 端现有的 **capi（ArkUI_NodeAPI）** 版 `KRTextFieldView` / `KRTextAreaView` 分别基于 `ARKUI_NODE_TEXT_INPUT` / `ARKUI_NODE_TEXT_AREA` 这两个**独立**的 ArkUI Node 类型实现。两者存在实际痛点：

1. **两套代码维护成本高**：`KRTextFieldView` / `KRTextAreaView` 虽然通过 C++ 继承复用了大量逻辑（`KRTextAreaView : public KRTextFieldView`），但 `ARKUI_NODE_TEXT_INPUT` 与 `ARKUI_NODE_TEXT_AREA` 各自拥有一套独立的属性枚举（`NODE_TEXT_INPUT_*` vs `NODE_TEXT_AREA_*`）、事件枚举（`NODE_TEXT_INPUT_ON_CHANGE` vs `NODE_TEXT_AREA_ON_CHANGE`）、行为差异（单/多行、占位文本、IME 回车提交等），任何一个通用能力都需要在 `KRTextFieldView.cpp` / `KRTextAreaView.cpp` 两处同步适配（例如 `maxTextLength` 分支、`placeholder` 字体、光标控制、文本过滤等）。
2. **老 API 能力边界限制**：`TEXT_INPUT` / `TEXT_AREA` 不支持图文混排 inline image / inline node（业务需要图文混合输入、自定义表情、Mention 高亮等场景），现有 capi 实现无法支持，业务侧在鸿蒙端被迫走 ArkTS `RichEditor` 改造方案（见已存在的 `harmony-arkts-richeditor-textfield` change），引入了 ArkTS ↔ C++ 的跨语言桥接开销。
3. **单双行行为无法在运行时切换**：`ARKUI_NODE_TEXT_INPUT` 创建后无法切换为多行，反之亦然，导致业务动态改变 `multiline` 属性时必须重建节点，影响焦点 / 光标 / 输入法状态。

HarmonyOS **CAPI 于 API 24 新增了 `ARKUI_NODE_TEXT_EDITOR` 能力**（`arkui/native_type.h` 中的 `ArkUI_NodeType` 新枚举），这是一个**统一的文本编辑节点**：

- 同时覆盖单行 / 多行输入（通过 `NODE_TEXT_EDITOR_MAX_LINES` 控制）
- 属性 / 事件命名统一为 `NODE_TEXT_EDITOR_*`，一套代码即可覆盖单 / 多行
- 原生支持 inline image / inline span / style run，天然对齐图文混排场景
- 性能（测量、滚动、光标）由系统统一优化

我们希望基于 `ARKUI_NODE_TEXT_EDITOR` 提供**新版 capi 实现** `KRTextEditorFieldView` / `KRTextEditorAreaView`，与现有 `KRTextFieldView` / `KRTextAreaView` **并列存在**（文件级别并列，而非继承），并在 ViewCreator 闭包中通过 `OH_GetSdkApiVersion() >= 24` 做**运行时分支**，让 API 24+ 设备自动走新实现，低版本设备继续走老实现，对业务代码（`Input { }` / `TextArea { }`）完全透明。

## What Changes

### HarmonyOS 渲染端（新增 capi 实现，与老实现并列）

- **新增** [core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorFieldView.h](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorFieldView.h) / [KRTextEditorFieldView.cpp](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorFieldView.cpp)
  - 基于 `ARKUI_NODE_TEXT_EDITOR` 的单行输入实现（`maxLines = 1`，拦截换行）
  - 对齐老版 `KRTextFieldView` 全部属性 / 事件 / 方法（见下方"兼容性清单"）
  - **不继承** `KRTextFieldView`（老实现）；与 `KRTextFieldView` 是**并列关系**（平级 `IKRRenderViewExport` 子类），避免老实现的 `TEXT_INPUT` 相关虚函数耦合到新节点类型
- **新增** [core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorAreaView.h](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorAreaView.h) / [KRTextEditorAreaView.cpp](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorAreaView.cpp)
  - 基于 `ARKUI_NODE_TEXT_EDITOR` 的多行输入实现（不限制 `maxLines`，允许换行）
  - 对齐老版 `KRTextAreaView` 全部属性 / 事件 / 方法
  - 与 `KRTextEditorFieldView` **共享**一个 internal 工具头（`KRTextEditorCommon.h`）承载单 / 多行共用的属性映射 / 事件监听 / 文本长度过滤等逻辑；两个 `View` 子类只保留"差异点"（`maxLines` / IME 回车行为 / keyboardType 映射子集等）
- **改造** [core-render-ohos/src/main/cpp/libohos_render/expand/components/ComponentsRegisterEntry.h](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/ComponentsRegisterEntry.h)
  - `"KRTextFieldView"` 的注册闭包改为**运行时 API 版本判断**：`OH_GetSdkApiVersion() >= 24` 返回 `std::make_shared<KRTextEditorFieldView>()`，否则返回 `std::make_shared<KRTextFieldView>()`
  - `"KRTextAreaView"` 同上切换 `KRTextEditorAreaView` / `KRTextAreaView`
  - 保留老实现注册入口（仅闭包内部分支），不移除 `KRTextFieldView` / `KRTextAreaView` 源文件或头文件
- **改造** [core-render-ohos/src/main/cpp/CMakeLists.txt](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/CMakeLists.txt) — 将新增的 `KRTextEditorFieldView.cpp` / `KRTextEditorAreaView.cpp` 加入源码列表

### Core 层 / Demo 层

- **零改动**：`core/` 层的 `InputView` / `TextAreaView` / `ViewConst.TYPE_TEXT_FIELD` / `ViewConst.TYPE_TEXT_AREA` 全部不变；业务 DSL `Input { }` / `TextArea { }` 不变；跨端协议（`setProp` / `callMethod` / `event`）不变

### Non-goals（明确不做）

- **不影响 iOS / Android / Web / 小程序端** 的 TextField/TextArea 实现
- **不影响** 已有 ArkTS `KRRichTextFieldView.ets` / `KRRichTextAreaView.ets`（基于 `RichEditor`）及其 `"KRRichTextFieldView"` / `"KRRichTextAreaView"` ViewName；与本 change 正交，业务仍可显式使用 `RichInput { }` / `RichTextArea { }` 获取 ArkTS RichEditor 实现
- **不删除、不重构** 现有 capi `KRTextFieldView` / `KRTextAreaView`（API 24 以下设备仍需使用；且保留作为回归 baseline）
- **不引入图文混排能力**（本期目标是"新 capi 节点 + 版本分支切换"，图文混排场景继续走 ArkTS `RichEditor` 方案）；仅把 `ARKUI_NODE_TEXT_EDITOR` 用作"更干净的单 / 多行文本输入节点"
- **不引入新的跨端协议 / 新 ViewName**（ViewName 继续是 `"KRTextFieldView"` / `"KRTextAreaView"`）
- **不调整** `InputAttr` / `TextAreaAttr` 的键名、`InputSpan` 字段、默认值
- **不处理** API 24 以下设备的**部分新能力回退**（API 24 特有的 inline style 等；与目标无关）
- **不提供** 编译期版本分支（全部走运行时 `OH_GetSdkApiVersion()` 判断，避免两套产物）
- **不修改** Core 层，不依赖 core-render-ohos 之外任何模块

## Capabilities

> 模块名来自 `openspec/config.yaml` 中的 Module Map（`core-render-ohos`）。本次所有改动均位于 `core-render-ohos/`。

### New Capabilities

- **`ohos-capi-text-editor-renderer`**（模块：`core-render-ohos/`）
  - C++ 类 `KRTextEditorFieldView`（基于 `ARKUI_NODE_TEXT_EDITOR`，单行）
  - C++ 类 `KRTextEditorAreaView`（基于 `ARKUI_NODE_TEXT_EDITOR`，多行）
  - 单 / 多行之间的共享头 `KRTextEditorCommon.h`（属性映射 / 事件注册 / 文本长度过滤 / UTF-8/16 转换等工具，**不是基类**）
  - 对齐老实现的全部属性 / 事件 / 方法（见 spec 的兼容性矩阵）

- **`ohos-text-input-runtime-switch`**（模块：`core-render-ohos/`）
  - `ComponentsRegisterEntry` 中 `"KRTextFieldView"` / `"KRTextAreaView"` 注册闭包改为 `OH_GetSdkApiVersion()` 运行时判断
  - API 24+：创建 `KRTextEditorFieldView` / `KRTextEditorAreaView`
  - API < 24：创建 `KRTextFieldView` / `KRTextAreaView`（现状）
  - 业务 DSL `Input { }` / `TextArea { }` 完全无感

### Modified Capabilities

无（老版 `KRTextFieldView` / `KRTextAreaView` 的 C++ 源码零改动；仅 `ComponentsRegisterEntry.h` 的注册闭包从"直接 make_shared" 调整为"按 API 版本 make_shared"，属于注册位的局部改造）。

## Impact

### 受影响模块

- **`core-render-ohos/`** — 新增 4 个 C++ 文件、1 个共享头文件；改造 `ComponentsRegisterEntry.h`、`CMakeLists.txt`

### 受影响文件

**新增（HarmonyOS capi 层）：**

- `core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorCommon.h` — 单 / 多行共享的工具函数 / 属性枚举映射 / 事件类型映射（仅 header-only inline 或显式 cpp 皆可，不是 View 基类）
- `core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorFieldView.h`
- `core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorFieldView.cpp`
- `core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorAreaView.h`
- `core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorAreaView.cpp`

**修改：**

- `core-render-ohos/src/main/cpp/libohos_render/expand/components/ComponentsRegisterEntry.h` — 两处 `RegisterViewCreator` 闭包改造
- `core-render-ohos/src/main/cpp/CMakeLists.txt` — 加入新增的两个 `.cpp`

### 受影响平台

- **HarmonyOS**：主要改动面
  - **API 24 及以上设备**：`Input { }` / `TextArea { }` 底层切换到 `ARKUI_NODE_TEXT_EDITOR`，可见行为需**与老实现完全对齐**（焦点、光标、属性、事件、IME 回车、键盘类型、最大长度、长度限制类型、占位文本、光标颜色、对齐方式等）
  - **API 23 及以下设备**：继续使用 `ARKUI_NODE_TEXT_INPUT` / `ARKUI_NODE_TEXT_AREA` 的老实现，行为零变化
- **Android / iOS / Web / 小程序**：不涉及

### 风险与兼容性

- **可见行为对齐是关键风险**：`ARKUI_NODE_TEXT_EDITOR` 的 IME / 光标 / 默认样式可能与 `TEXT_INPUT` / `TEXT_AREA` 存在细微差异（比如默认 padding、默认光标颜色、`blurOnSubmit` 默认行为、`enterKeyType` 回车提交时是否自动失焦、password 键盘回退等）。必须通过跑现有 demo（`InputViewDemoPage` / `MaxTextLengthDemoPage` / `InputMeasureDemoPage`）**完整回归**，对比 API 23（老路径）与 API 24（新路径）的行为差异；不一致处在 `KRTextEditorCommon` 或各 view 子类中补齐
- **运行时版本判断的失效场景**：`OH_GetSdkApiVersion()` 在非 OpenHarmony / 部分 OEM 设备上可能返回异常值（例如 0 或 -1）。闭包里必须做**安全默认**：若版本号 < 24 或查询失败，一律走老实现
- **Node 类型枚举可用性**：`ARKUI_NODE_TEXT_EDITOR` 枚举值在较老的 SDK header 下可能不存在。项目目前的 `OH_CURRENT_API_VERSION >= 18`（见 `KREventUtil.cpp`）编译头保证可用，但若 CI 使用的 SDK header 不包含该枚举，需要在 `KRTextEditorCommon.h` 中以 `#ifdef ARKUI_NODE_TEXT_EDITOR` 宏兜底（若宏未定义，新 View 的 `CreateNode()` 直接返回 `nullptr`，注册闭包永远走老路径）
- **CMake 编译失败风险**：新增 `.cpp` 必须在 `CMakeLists.txt` 登记；漏改会导致符号缺失 → 注册闭包 `make_shared` 编译失败
- **性能 / 内存**：新节点类型由系统实现，预期不劣化；但 `ARKUI_NODE_TEXT_EDITOR` 的事件回调频率 / 测量策略与老节点可能不同，需要在 `OnTextDidChanged` / `SetProp` 的热路径上保持与老实现相当的最小动作
- **zero-break**：业务代码（Kotlin 层）与 ArkTS `RichEditor` 方案完全不受影响；API 24 以下设备零变化
