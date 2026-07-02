## Context

本次变更只作用于 **自研 DSL（self-DSL）**，不涉及 Compose DSL。HarmonyOS 侧普通 `KRView` 已经原生支持 `hit-test-ohos`，当前框架公开口径只保留 `default`、`block`、`transparent`、`none` 四个字符串常量；但框架公共 `Attr` 没有开放正式 API，业务只能直接手写 `setProp("hit-test-ohos", "...")`。另一方面，`forwardview v1` 当前仍固定在初始化时使用 `ARKUI_HIT_TEST_MODE_TRANSPARENT`，并不会识别这条 prop，因此与普通 view 的能力模型不一致。

当前属性链路已经具备跨端下发能力：业务在 `Attr` 上设置属性后，会经过 `RenderView.setProp -> BridgeManager -> NativeBridge -> IKRRenderViewExport::ToSetProp` 进入 HarmonyOS native 渲染层。对普通 view 来说，`KRView` 已经在 native 层消费 `hit-test-ohos`；对 `forwardview v1` 来说，属性会先尝试走 `ToSetBaseProp`，未命中基础属性后再进入 `KRForwardArkTSView::SetProp`。这意味着本次只需要补公共 API 和 `forwardview v1` 的 native 消费，无需改动 Bridge 协议。

### NativeBridge 交互

- `core/IStyleAttr` 与 `core/Attr` 暴露新的 HarmonyOS 专属 API：`ohosViewHitTestMode(mode: String)`
- 该 API 内部将业务常量映射为对应字符串后，再调用 `setProp("hit-test-ohos", value)`，复用现有 native prop key
- `core` 提供公开常量，覆盖 `default` / `block` / `none` / `transparent` 四个取值
- 普通 `KRView` 继续沿用现有 `hit-test-ohos` 处理逻辑
- `KRForwardArkTSView::SetProp` 新增对 `hit-test-ohos` 的识别与消费，命中后不再透传给 ArkTS 内层视图

### 预期文件变更（按模块）

- `core/`
  - `core/src/commonMain/kotlin/com/tencent/kuikly/core/base/attr/IStyleAttr.kt`
  - `core/src/commonMain/kotlin/com/tencent/kuikly/core/base/Attr.kt`
- `core-render-ohos/`
  - `core-render-ohos/src/main/cpp/libohos_render/expand/components/forward/KRForwardArkTSView.h`
  - `core-render-ohos/src/main/cpp/libohos_render/expand/components/forward/KRForwardArkTSView.cpp`
- `docs/`
  - 更新 HarmonyOS 专属属性说明，明确 `ohosViewHitTestMode` 用法、可选常量值、平台范围和 `forwardview v1` 默认行为

## Goals / Non-Goals

**Goals:**
- 提供正式的公共 API，让业务不再手写 `setProp("hit-test-ohos", "...")`
- 将公开方法名统一为 `ohosViewHitTestMode`
- 为四个公开字符串常量提供统一封装，避免业务拼写错误
- 让 `forwardview v1` 和普通 `KRView` 共享同一条 `hit-test-ohos` 属性入口
- 保持既有默认行为兼容：普通 `KRView` 的默认行为不变，`forwardview v1` 未设置时仍保持 `TRANSPARENT`
- 明确跨平台兼容规则：HarmonyOS 生效，其他平台忽略

**Non-Goals:**
- 不修改 `forwardview v2` 的行为或属性协议
- 不新增 HarmonyOS 之外的平台 hit-test 对齐实现
- 不改变 `KRView` 现有命中模式取值的底层语义
- 不保留上一版 `forwardTransparentOhos(Boolean)` 方案

## Decisions

### 1. 公共 API 名固定为 `ohosViewHitTestMode`，底层复用 `hit-test-ohos`

**Decision**
- 在 `IStyleAttr` / `Attr` 中新增 HarmonyOS 专属方法：`ohosViewHitTestMode(mode: String)`
- 方法内部统一调用 `setProp("hit-test-ohos", mode)`
- 提供公开字符串常量集合，例如 `OhosViewHitTestMode.HIT_TEST_DEFAULT`、`HIT_TEST_BLOCK`、`HIT_TEST_TRANSPARENT`、`HIT_TEST_NONE`

**Rationale**
- `ohosViewHitTestMode` 比 `hitTestModeOhos` 更贴近业务调用习惯，也更明确这是 View 级别的 HarmonyOS 命中控制
- 与现有 `KRView` native 能力完全同构，不需要再发明一套新的布尔语义
- 业务不需要再直接手写 `setProp("hit-test-ohos", "...")`
- 提供常量后可显著降低字符串拼写错误和非法值输入风险

**Alternatives considered**
- 继续让业务手写 `setProp(...)`: 能工作，但没有公共 API 契约，也不利于文档化和后续维护
- `hitTestModeOhos(mode: String)`: 也可用，但本次统一按 `ohosViewHitTestMode` 定名
- 保留 `forwardTransparentOhos(Boolean)`: 只能覆盖两档语义，无法表达 `default` / `block`，且与 `KRView` 现有能力模型脱节

### 2. `forwardview v1` 补齐同 key 支持，但保持自身默认值兼容

**Decision**
- 在 `KRForwardArkTSView::SetProp` 中识别 `hit-test-ohos`
- 命中后按照与 `KRView` 一致的现有四种公开模式更新 wrapper 的 hit-test 模式，并停止继续透传给 ArkTS 内层视图
- 在 `KRForwardArkTSView` 中补 `ResetProp`（或等效重置逻辑），当该属性被移除时恢复到当前默认的 `ARKUI_HIT_TEST_MODE_TRANSPARENT`

**Rationale**
- `forwardview v1` 不是 `KRView` 体系，现有 `KRView` 的实现不会自动覆盖到它
- 业务使用同一个公共 API 时，普通 view 与 forward view 应该共用同一套取值模型
- `forwardview v1` 现有默认就是 `TRANSPARENT`，重置时必须保持兼容，而不是回到 `DEFAULT`

**Alternatives considered**
- 不支持 `forwardview v1`: 会导致公共 API 只对普通 view 生效，能力口径不完整
- 在 `forwardview v1` 上继续保留布尔专属 API: 会形成两套并行心智模型，增加业务理解成本

### 3. 普通 `KRView` 不改语义，只补公共封装

**Decision**
- `KRView` 的 native 行为继续保持现状
- 本次对普通 view 的改动只体现在：公开 API、公开常量、文档说明

**Rationale**
- `KRView` 当前已经具备完整的 `hit-test-ohos` 支持，本次只是在 DSL 与文档层补齐剩余两种模式说明
- 问题不在 `KRView` native 层，而在 DSL 可用性和 `forwardview v1` 缺口

### 4. 其他平台继续忽略该 API

**Decision**
- Android、iOS、Web、miniApp 不实现该属性的 native 消费
- 文档中明确标注该 API 为 HarmonyOS only

**Rationale**
- 这次需求聚焦于 HarmonyOS 原生 hit-test 模型与 `forwardview v1`
- 其他平台并没有同构能力和现成 key，强行对齐会扩大范围

## Risks / Trade-offs

- **[风险] API 使用字符串入参，仍可能传入非法值** → 通过公开常量对象引导业务使用标准值，并在文档中明确建议只使用常量
- **[风险] `forwardview v1` 的默认行为与普通 `KRView` 不同** → 在 spec 和文档中显式记录：普通 view 保持原默认，`forwardview v1` 未设置/重置时恢复 `TRANSPARENT`
- **[风险] 旧 Boolean 方案残留会混淆业务** → 本次先回滚 `forwardTransparentOhos` 的实现和文档，再推进新方案实现
- **[风险] 其他平台忽略该 API 带来认知落差** → 文档中显式声明 HarmonyOS only，避免误用预期

## Migration Plan

1. 回滚上一版 `forwardTransparentOhos(Boolean)` 方案的实现和文档残留
2. 在 `IStyleAttr` / `Attr` 中新增 `ohosViewHitTestMode(mode: String)` 与公开字符串常量
3. 在 `KRForwardArkTSView` 中补齐 `hit-test-ohos` 的公开模式消费与 reset 回退逻辑
4. 更新文档，说明四个公开常量值、平台范围以及普通 view / `forwardview v1` 的默认行为差异
5. 通过 HarmonyOS 验证普通 `KRView` 与 `forwardview v1` 的四种公开命中模式行为，以及 `forwardview v1` 未设置/重置后的默认 `transparent`

**Rollback strategy**
- 如果新方案上线后出现兼容问题，可删除业务侧 API 调用点，普通 view 和 `forwardview v1` 都会回到各自既有默认行为
- 若需要完全回滚框架改动，只需移除新增公共 API / 常量，以及 `KRForwardArkTSView` 对 `hit-test-ohos` 的补齐逻辑

## Open Questions

- 无。当前公共 API 命名已固定为 `ohosViewHitTestMode`，底层 prop key 固定复用 `hit-test-ohos`
