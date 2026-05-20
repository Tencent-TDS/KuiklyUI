## Applies To

- **DSL 模式**：**自研 DSL**（`Pager + body()` + `Input { }` / `TextArea { }`）与 **Compose DSL**（`TextField`，底层也会通过 `core` 层复用 `InputView` / `TextAreaView`）两者均适用。本次改动落在 C++ 渲染层的 `IKRRenderViewExport` 子类与注册入口，对两种 DSL **透明**，故 design 不对任一 DSL 做特别处理。

- **受影响平台**：仅 **HarmonyOS**。

- **受影响模块**：仅 **`core-render-ohos/`**（C++ 渲染层）。

## Context

现有 capi 实现结构：

```
IKRRenderViewExport
   ├── KRTextFieldView     (ARKUI_NODE_TEXT_INPUT)
   │     └── KRTextAreaView (ARKUI_NODE_TEXT_AREA) // C++ 继承 KRTextFieldView 复用逻辑
```

ViewCreator 注册位 [ComponentsRegisterEntry.h](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/ComponentsRegisterEntry.h)：

```cpp
IKRRenderViewExport::RegisterViewCreator("KRTextFieldView", [] { return std::make_shared<KRTextFieldView>(); });
IKRRenderViewExport::RegisterViewCreator("KRTextAreaView",  [] { return std::make_shared<KRTextAreaView>();  });
```

目标：**保留老实现不动**，新增两套基于 `ARKUI_NODE_TEXT_EDITOR` 的实现，在注册闭包里按 `OH_GetSdkApiVersion()` 运行时分支。

关键跨端协议（Kotlin → C++）通过 `KRAnyValue` 承载的 `setProp` / `callMethod` / `event`，协议**不变**，新老实现只需保持协议兼容即可。

NativeBridge 交互：本次**不新增**任何协议键 / 方法 / 事件。`setProp` / `callMethod` / `RegisterEvent` 语义沿用现有 `KRTextFieldView` 定义。

## Goals / Non-Goals

### Goals

- **G1** 新增基于 `ARKUI_NODE_TEXT_EDITOR` 的单行 (`KRTextEditorFieldView`) 与多行 (`KRTextEditorAreaView`) 实现，与老实现并列存在
- **G2** 通过 `OH_GetSdkApiVersion() >= 24` 运行时切换，API 24+ 使用新实现，API < 24 使用老实现
- **G3** 新实现与老实现的**可见行为完全对齐**（属性、事件、方法、IME、光标、占位、对齐、键盘类型、最大长度等）
- **G4** 业务代码 / core 层 / DSL / ArkTS RichEditor 方案**零影响**

### Non-Goals

- **NG1** 不支持图文混排 inline image / inline node（继续走已有 ArkTS RichEditor 方案）
- **NG2** 不删除 / 不重构老实现
- **NG3** 不引入新的跨端协议 / ViewName / DSL 入口
- **NG4** 不做编译期版本分支（全部走运行时判断）
- **NG5** 不修改 core 层

## Decisions

### Decision 1 — 新旧实现的类结构

**Context**：老实现 `KRTextAreaView : public KRTextFieldView`（C++ 继承），利用虚函数覆盖 `TEXT_INPUT` 与 `TEXT_AREA` 的属性差异。新节点类型 `ARKUI_NODE_TEXT_EDITOR` 的属性完全通过 controller（StyledString / TextStyle / ParagraphStyle）插入，单 / 多行差异仅 4 点（SingleLine、换行拦截、password warn、lineHeight）。

**Decision**：新实现**与老实现完全并列**（不继承老 `KRTextFieldView` / `KRTextAreaView`），但新实现**内部**采用继承复用：

```
IKRRenderViewExport
   ├── KRTextFieldView       (老 TEXT_INPUT，保留)
   │     └── KRTextAreaView  (老 TEXT_AREA，保留)
   └── KRTextEditorFieldView (新 TEXT_EDITOR，单行)
         └── KRTextEditorAreaView  (新 TEXT_EDITOR，多行)
```

- **单行类提供两个虚函数 hook**：`IsSingleLine()` 默认 `true` 、`InterceptNewline()` 默认 `true`；多行类 override 为 `false`。
- **共享的状态 / 工具函数**放在 header-only `KRTextEditorCommon.h`（`namespace kuikly::text_editor`）：`KRTextEditorState` / `SetStyledText` / `GetStyledText` / `ApplyPlaceholder` / `ApplyTypingStyle` 等，接收 `KRTextEditorState&` 调用。
- **与老实现并列**：Field / Area 新类不继承老类，老类零改动。

**Alternatives**：
- 并列不继承（设计初稿）— 被拒绝：若 Area 与 Field 完全不继承，需将 `SetProp` / `OnEvent` / `CallMethod` 全部以 free function 方式抽到 Common；实测成本远高于继承，而且事件路径 `OnWillChangeText` 内部依赖 `GetContentText()` 等 View 级语义，直接继承更简洁也与老实现惯例一致。
- 共用基类（抽 `KRTextEditorBaseView` 供 Field/Area 同段继承）— 被拒绝： Field 与 Area 的差异太小，不值得额外引入一个抽象层。

### Decision 2 — 运行时版本判断放在 ViewCreator 闭包

**Context**：需要在 API 24+ 设备创建新实现，API < 24 设备创建老实现。有三个可选切换点：

1. **注册时分支**：`OH_GetSdkApiVersion()` 只查一次，启动时决定注册哪个类
2. **ViewCreator 闭包内分支**：每次 `make_shared` 时判断（每次创建 View 都查 API 版本）
3. **类内分支**：同一个类内按版本走不同节点类型 / 属性代码

**Decision**：采用**方案 2**（ViewCreator 闭包内分支）。

```cpp
IKRRenderViewExport::RegisterViewCreator("KRTextFieldView", [] {
    if (OH_GetSdkApiVersion() >= 24) {
        return std::static_pointer_cast<IKRRenderViewExport>(std::make_shared<KRTextEditorFieldView>());
    }
    return std::static_pointer_cast<IKRRenderViewExport>(std::make_shared<KRTextFieldView>());
});
```

**原因**：

- 写法最直观、改动面最小（仅 `ComponentsRegisterEntry.h` 两行闭包）
- `OH_GetSdkApiVersion()` 是 syscall-cheap 的版本查询（项目其他处如 `KRImageView.cpp` / `KRParagraph.cpp` 已在热路径里用），不会成为瓶颈
- **不将结果缓存为 `static` 局部**：可以，但省得没多少（版本号在进程生命周期内恒定）；为了代码更干净，可以在闭包内用 `static const int gApiVersion = OH_GetSdkApiVersion();` 缓存一次——实施时采用这种缓存写法

**Alternatives 拒绝原因**：

- 方案 1（注册时决定）：需要改 `ComponentsRegisterEntry()` 函数签名（变成"启动时查 API 版本 → 分支注册"），且启动顺序耦合到 `OH_GetSdkApiVersion()` 可用性；闭包内写法更解耦
- 方案 3（类内分支）：违反 Decision 1 的"并列"原则，两套节点 API 混在一个类里会劣化可维护性

### Decision 3 — `KRTextEditorCommon` 的组织形式

**Context**：单 / 多行实现共享：UTF-8 / UTF-16 转换、文本长度计算（按 BYTE / CHARACTER / VISUAL_WIDTH）、属性键名常量、keyboardType / enterKeyType 枚举映射、事件回调分发、max_length 过滤等。

**Decision**：

- 以 `KRTextEditorCommon.h` 为主（header-only inline functions + constexpr 常量）
- 确有需要再提供 `.cpp`（复杂的 UTF-8/16 转换等可抽 cpp）
- **命名空间** `kuikly::text_editor`，避免污染全局
- **状态 struct** `KRTextEditorState`（持有 max_length / length_limit_type / font_size / 各事件回调等），由两个 View 子类各自持有实例 → 共享工具函数接收 `KRTextEditorState&` 参数

**原因**：

- 避免 C++ 继承（Decision 1 已明确）
- 避免使用全局静态（需要 per-view 实例状态）
- 保留老实现 `KRTextFieldView` 中 UTF-8 / UTF-16 转换工具代码作参考（`GetUTF8ByteCount` / `CalculateTruncateIndex` 等），在新 common 中按需复刻

### Decision 4 — 新实现不跟老实现共享 `KRTextFieldView.h` 的私有函数

**Context**：老实现 `KRTextFieldView.h` 中有 30+ 私有函数（`GetUTF8ByteCount`, `CalculateTruncateIndex`, `OnWillInsertText`, `OnPasteText`, `OnWillChangeText` 等）。复用最省事，但意味着 "友元 + 暴露私有"。

**Decision**：**不共享老实现的私有实现**。

- 老实现中真正**与节点类型无关**的工具函数（UTF-8 / UTF-16 转换、长度计算）在新实现中**独立复刻**到 `KRTextEditorCommon`，避免头文件循环依赖和修改老实现
- 事件处理（`OnTextDidChanged` / `OnInputFocus` / `OnInputBlur` / `OnInputReturn` / `OnWillInsertText` / `OnPasteText` / `OnWillChangeText`）在新实现里**独立实现**，各自处理 `NODE_TEXT_EDITOR_*` 事件枚举
- 好处：老实现完全"只读"；新实现可以独立演进；未来下线老实现时可直接整块删除

### Decision 5 — 节点枚举 / SDK header 兼容性兜底

**Context**：`ARKUI_NODE_TEXT_EDITOR` 需 API 24 的 SDK header 支持。项目当前编译基线 `OH_CURRENT_API_VERSION >= 18`（见 `KREventUtil.cpp`）并不保证 SDK header 中一定有该枚举。

**Decision**：在 `KRTextEditorSwitch.h` 开头始终显式定义三态门控宏，避免"未定义则默认 0"在错误 include 顺序下造成静默错位：

```cpp
// 始终显式定义为 0 或 1，不采用"条件 #define / 不定义"头衅接。
// 1 = SDK header >= API 24，`ARKUI_NODE_TEXT_EDITOR` 等配套类型 / API 均可用；
// 0 = SDK header <  API 24，需要裁掉带 API 24 类型的实现。
#include <arkui/native_type.h>

#ifndef KUIKLY_TEXT_EDITOR_AVAILABLE
#if defined(OH_CURRENT_API_VERSION) && OH_CURRENT_API_VERSION >= 24
#define KUIKLY_TEXT_EDITOR_AVAILABLE 1
#else
#define KUIKLY_TEXT_EDITOR_AVAILABLE 0
#endif
#endif
```

- 若 `KUIKLY_TEXT_EDITOR_AVAILABLE == 0`：新 View 的 `CreateNode()` 返回 `nullptr`，`CreateNode()` 调用方负责兜底（日志 + 不 crash）
- 若 `KUIKLY_TEXT_EDITOR_AVAILABLE == 1`：正常走 `ARKUI_NODE_TEXT_EDITOR` 路径
- `ComponentsRegisterEntry` 注册闭包内**额外** `OH_GetSdkApiVersion() >= 24` 做双保险（即使 SDK header 有枚举，但运行设备 API < 24 仍走老实现）

**Alternative**：在 `CMakeLists.txt` 里用 `if(API_VERSION LESS 24)` 跳过新 cpp 编译 — 被拒绝：会让 CI 产物与不同 API 设备表现耦合，不如 runtime 判断干净。

### Decision 6 — 单 / 多行差异点清单

新实现的单 / 多行差异仅在以下点（其他逻辑全部走 `KRTextEditorCommon` 或由 Field 基类统一提供）：

| 差异点 | KRTextEditorFieldView（单行） | KRTextEditorAreaView（多行） |
|--|--|--|
| `IsSingleLine()` | `true` → `DidInit` 时写入 `NODE_TEXT_EDITOR_SINGLE_LINE=true` | `false`，不写入 → 多行默认 |
| `InterceptNewline()` | `true` → `OnWillChangeText` 中 `\n` 被拒绝 | `false`，允许插入 |
| `keyboardType` | `ApplyKeyboardType` 统一 warn 降级 | 额外对 `password` 打一条 warn（对齐老 `KRTextAreaView` 不支持 password 的语义） |
| `lineHeight` | 不处理 | 专属支持，`TextStyle.SetLineHeight` + `SetTypingStyle` |

### Decision 7 — 事件注册粒度

**Context**：老 `KRTextFieldView::DidInit()` 只默认 `RegisterEvent(NODE_TEXT_INPUT_ON_CHANGE)`，其他事件（focus / blur / return / paste / will-insert / will-change）都在 `SetProp` 走 `event_call_back` 非空时**按需注册**。

**Decision**：新实现沿用该**按需注册**策略（避免无谓的 JNI 回调）。

- 唯一默认注册：`NODE_TEXT_EDITOR_ON_CHANGE`（业务通常总要监听 text 变化）
- 其他（`ON_SUBMIT` / `ON_FOCUS` / `ON_BLUR` / `ON_PASTE` / `ON_WILL_INSERT` / `ON_WILL_CHANGE`）仅在 `SetProp` 的对应事件回调被设置时才注册
- 所有事件类型常量从 `KRTextEditorCommon` 中取（集中管理）

### Decision 8 — `maxTextLength` / `lengthLimitType` 的实现策略

**Context**：老实现对 `maxTextLength` 有两套策略：

- `length_limit_type == -1`（未设置，用旧逻辑）：直接调 `UpdateInputNodeMaxLength(maxLength)`（系统级限制）
- `length_limit_type != -1`（BYTE / CHARACTER / VISUAL_WIDTH）：走 `SetupLengthInputFilter`，在 `OnWillInsertText` / `OnPasteText` / `OnWillChangeText` 中手动过滤

**Decision**：新实现**复刻**这套策略（不引入新行为）。

- 将 `CalculateTextLength` / `CalculateTruncateIndex` / `GetUTF8ByteLengthOf*` / `GetVisualWidthOfCodePoint` / `GetUTF16Length` / `GetUTF8ByteCount` 这些**纯算法函数**独立写在 `KRTextEditorCommon.h` 的 `namespace kuikly::text_editor::length`（与老实现的同名私有函数是**平级拷贝**，不复用老实现符号）
- `ARKUI_NODE_TEXT_EDITOR` 对应的节点级 max-length 属性（如果 API 24 提供的话，命名大概率为 `NODE_TEXT_EDITOR_MAX_LENGTH`）用于 `length_limit_type == -1` 的快路径
- 其他路径继续手动过滤

### Decision 9 — `ArkUI_StyledString_Descriptor` 生命周期 work-around

**Context**：实施期发现 `OH_ArkUI_StyledString_Descriptor_Create()` 在 API 24 SDK 上存在缺陷：

- 返回的 descriptor 内部成员未被初始化，导致紧随其后的 `OH_ArkUI_StyledString_Descriptor_Destroy()` 在 `free_default` 时直接崩溃；
- 同样的 descriptor 经过 `OH_ArkUI_TextEditorStyledStringController_SetStyledString` 后，再调用 `GetString` 也无法读出文本（疑似与未初始化的字段相关）。

最初的临时规避是把所有 `Destroy()` 注释掉（对应 7 处泄漏），靠系统在进程退出时清理；这显然不能上线。

**Decision**：放弃 `OH_ArkUI_StyledString_Descriptor_Create()`，统一改为
`OH_ArkUI_StyledString_Descriptor_CreateWithString("", spanStyles, 1)` 创建"空 descriptor"——该 API 内部会完整初始化所有成员，对应的 `Destroy()` 路径安全可用。改造范围：

- [`KRTextEditorCommon.h`](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorCommon.h) 中 `GetStyledText` / `SetStyledText`；
- [`KRTextEditorFieldView.cpp`](/Users/steven/code/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorFieldView.cpp) 中 `OnWillChangeText` 的两处 descriptor 分配点。

放开此前被注释掉的 6 处 `Destroy(descriptor)` / `Destroy(spanStyle)` 调用，恢复正常的"per-call alloc + destroy"模型。

**Alternatives**：

- 主动初始化 `_Create()` 返回值（按已知 ABI 把内部两个指针写 `nullptr`）— 被拒绝：依赖未公开内部布局，未来 SDK 更新即破坏；
- 长期保持 `Destroy` 注释 / 进程级泄漏 — 被拒绝：每次 OnWillChange / Get / Set 都会泄漏一个 descriptor，长输入页面会触发明显内存增长。

**Knowledge base**：背景、调用面与排查路径已归档到 [.ai/references/ohos-styledstring-descriptor-quirks.md](/Users/steven/code/KuiklyUI/.ai/references/ohos-styledstring-descriptor-quirks.md)，后续若 OS 修复了 `_Create()` 的初始化缺陷，可参考该文档评估是否回切。

## Risks / Trade-offs

### 风险 1 — 新节点行为与老节点细微差异

- **表现**：IME 弹出位置、默认光标颜色、默认 padding、clear button、手势（长按选词）等系统行为可能不同
- **缓解**：design 阶段无法穷举，**强制通过现有 demo 回归**（`InputViewDemoPage` / `MaxTextLengthDemoPage` / `InputMeasureDemoPage`）在 API 23 与 API 24 设备上双跑；任何可见差异在 `KRTextEditorFieldView::DidInit()` 中显式对齐（设置 `BlurOnSubmit=false`、`CaretColor=黑色`、`Padding=0`、`BorderRadius=0`、透明背景等）
- **兜底**：若对齐成本过高，保留一个编译开关 `KUIKLY_FORCE_LEGACY_TEXT_INPUT`（环境变量或宏），可强制走老实现——在 tasks 中未列入必需项，只作为 contingency

### 风险 2 — API 24 设备覆盖率

- **现状**：HarmonyOS API 24 对应较新的 OS 版本；大量在用设备仍在 API 23 及以下
- **影响**：新实现初期只覆盖小部分设备，但正因如此，回归不对齐的代价也更低
- **缓解**：无需缓解，这是切换策略的预期

### 风险 3 — 未来维护两套实现

- **现状**：新老实现长期并存，修 bug 时容易漏同步
- **缓解**：
  - 文档声明：**新问题优先在新实现修，老实现只修 P0 安全 bug**
  - 设置老实现下线计划（另提 change，待 API 24 覆盖率达到阈值后删除）

### Trade-off — 不共享老实现代码 vs 代码重复

- 选择：**接受代码重复**（UTF 工具、长度计算函数在新老实现各有一份）
- 代价：约 300 行代码重复
- 收益：
  - 老实现 `KRTextFieldView.h` 完全不改（本 change 对老实现零入侵）
  - 新实现可独立演进（光标、IME 策略优化不影响老实现）
  - 未来下线老实现时整块删除，不用拆分共享符号

## Migration Plan

**对业务 / 上层**：无迁移成本。业务代码、DSL、core 层、Compose DSL 层**完全不动**。

**对 HarmonyOS 运行时**：

- 升级到本 change 之后的包：API 24+ 设备自动使用新实现；API < 24 设备继续使用老实现
- **回滚方案**：若新实现出现严重问题，`ComponentsRegisterEntry.h` 中注册闭包**单点回滚**（把 `OH_GetSdkApiVersion() >= 24` 改成 `false`）即可瞬间切回老实现——无需删除新代码

**CI / 编译**：

- 确认 DevEco Studio / NDK 版本支持 `ARKUI_NODE_TEXT_EDITOR` 枚举（需 API 24 SDK header）
- 若不支持，`KUIKLY_TEXT_EDITOR_AVAILABLE` 将为 `0`，新实现被运行期跳过，编译仍可通过

## File Changes (by module)

### `core-render-ohos/` 新增文件

- `core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorCommon.h`
  - `namespace kuikly::text_editor`
  - SDK header 可用性 guard（`KUIKLY_TEXT_EDITOR_AVAILABLE`，始终显式定义为 0/1）
  - `struct KRTextEditorState`（共享状态字段）
  - `constexpr char*` 属性键名常量（`kText` / `kPlaceholder` / `kFontSize` / ...）
  - `ArkUI_NodeEventType` 事件类型常量（`kEventTypeChange` / `kEventTypeSubmit` / ...）
  - inline 工具函数：`SetFont` / `UpdateMaxLength` / `CalculateTextLength` / `CalculateTruncateIndex` / `GetUTF8ByteCount` / `GetUTF16Length` / `GetVisualWidthOfCodePoint` / ...
  - inline 工具函数：`HandleSetPropCommon(node, state, prop_key, prop_value, event_cb)` — 处理两个 View 公共的 `setProp` 分支（`text` / `placeholder` / `fontSize` / `color` / 等），返回 `bool` 表示是否命中

- `core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorFieldView.h`
  - `class KRTextEditorFieldView : public IKRRenderViewExport`
  - 重写 `CreateNode()` / `DidInit()` / `OnDestroy()` / `SetProp()` / `OnEvent()` / `CallMethod()`
  - 内部成员：`KRTextEditorState state_`

- `core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorFieldView.cpp`
  - `CreateNode()`：`GetNodeApi()->createNode(ARKUI_NODE_TEXT_EDITOR)`（若 guard 生效返回 nullptr）
  - `DidInit()`：设 `maxLines=1`、默认 padding=0、默认 borderRadius=0、默认背景透明、默认 `BlurOnSubmit=false`、注册 `ON_CHANGE` 事件、调 `kuikly::text_editor::SetFont(...)`
  - `SetProp()`：先调 `kuikly::text_editor::HandleSetPropCommon(...)`；未命中则处理单行专属（keyboardType 的 password 支持、returnKeyType 行为、auto_hide_KeyBoard_on_ImeAction）
  - `OnEvent()`：switch 处理各 `NODE_TEXT_EDITOR_*` 事件；文本变化在本地 cache 并回调；`aboutToIMEInput` 拦截 `\n` 触发 `inputReturn`
  - `CallMethod()`：`focus` / `blur` / `setText` / `getCursorIndex` / `setCursorIndex`

- `core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorAreaView.h`
  - `class KRTextEditorAreaView : public IKRRenderViewExport`（与 Field 并列，不继承）
  - 重写同样的 6 个方法
  - 内部成员：`KRTextEditorState state_`

- `core-render-ohos/src/main/cpp/libohos_render/expand/components/input/KRTextEditorAreaView.cpp`
  - `CreateNode()`：`GetNodeApi()->createNode(ARKUI_NODE_TEXT_EDITOR)`
  - `DidInit()`：**不设置** `maxLines`（默认允许多行）；其余默认与 Field 一致
  - `SetProp()`：调用 common 的 HandleSetPropCommon；多行专属差异：keyboard type 降级处理（无 password）
  - `OnEvent()`：不拦截 `\n`，允许换行插入
  - `CallMethod()`：同 Field

### `core-render-ohos/` 修改文件

- `core-render-ohos/src/main/cpp/libohos_render/expand/components/ComponentsRegisterEntry.h`
  - 增加 include：`#include "libohos_render/expand/components/input/KRTextEditorFieldView.h"` / `KRTextEditorAreaView.h`
  - 改 `"KRTextFieldView"` 注册闭包：

    ```cpp
    IKRRenderViewExport::RegisterViewCreator("KRTextFieldView", [] {
        static const int gApiVersion = OH_GetSdkApiVersion();
        if (gApiVersion >= 24) {
            return std::static_pointer_cast<IKRRenderViewExport>(std::make_shared<KRTextEditorFieldView>());
        }
        return std::static_pointer_cast<IKRRenderViewExport>(std::make_shared<KRTextFieldView>());
    });
    ```
  - 改 `"KRTextAreaView"` 注册闭包：类似，切换 `KRTextEditorAreaView` / `KRTextAreaView`
  - 顶部增加 `#include <deviceinfo.h>`（若未引入）

- `core-render-ohos/src/main/cpp/CMakeLists.txt`
  - 在现有 `KRTextFieldView.cpp` / `KRTextAreaView.cpp` 旁边增加：

    ```cmake
    libohos_render/expand/components/input/KRTextEditorFieldView.cpp
    libohos_render/expand/components/input/KRTextEditorAreaView.cpp
    ```
  - （如果 `KRTextEditorCommon` 有 cpp 则同步加入；否则 header-only 无需）

## Validation

- **编译**：
  - 本地：`./gradlew :core-render-ohos:build`（如已有）；或使用 DevEco Studio 构建 ohosApp
  - CI：确保 CMake 包含新增源文件
- **运行时自测**：
  - 在 API 24+ 真机 / 模拟器上跑 `InputViewDemoPage` / `MaxTextLengthDemoPage` / `InputMeasureDemoPage`，确认视觉 / 行为与 API 23 设备一致
  - 关键路径人工对比：
    - 输入 / 删除 / 光标 / 焦点切换
    - 占位符显示 / 颜色 / 字体
    - `maxTextLength` 三种 `LengthLimitType` 截断
    - 键盘类型 / returnKeyType / `autoHideKeyboardOnImeAction`
    - `setText` / `setCursorIndex` / `getCursorIndex`
- **日志核查**：添加 `KR_LOG` 在 `ComponentsRegisterEntry` 闭包首次触发时打印一次"使用 TextEditor / TextInput 实现，apiVersion=X"，用于确认分支命中
- **回归**：执行 `openspec validate harmony-capi-text-editor-input`（若 CLI 可用）

## Open Questions

- `ARKUI_NODE_TEXT_EDITOR` 对应的属性 / 事件枚举具体命名以 API 24 官方 SDK header 为准（`NODE_TEXT_EDITOR_PLACEHOLDER` / `NODE_TEXT_EDITOR_FONT` / `NODE_TEXT_EDITOR_MAX_LENGTH` / `NODE_TEXT_EDITOR_ON_CHANGE` / `NODE_TEXT_EDITOR_ON_SUBMIT` / ...）— 实施阶段对照 header 确认后填入 `KRTextEditorCommon.h`
- `kuikly::util::` 工具函数（`UpdateNodeBackgroundColor` / `UpdateInputNodePlaceholder` 等）是否同时支持新节点类型？需在实施阶段验证；若不支持则在 `KRTextEditorCommon` 中实现对应的 `UpdateTextEditorNodePlaceholder` 等版本
- `maxTextLength` 在 `NODE_TEXT_EDITOR` 上是否有直接节点属性（类似 `NODE_TEXT_INPUT_MAX_LENGTH`）— 若无，走手动过滤路径
- `RegisterForwardArkTSViewCreator` 是否也要注册新 ArkTS 回退路径？（当前 change 不涉及，跨语言回退仍走现有 ArkTS View 注册）
