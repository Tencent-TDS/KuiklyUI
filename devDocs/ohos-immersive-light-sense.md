# 鸿蒙沉浸光感（Immersive Light Sense）Kuikly 适配方案

> 目标：**只使用系统原生沉浸光感 API**，不做自绘模拟。
> 文档状态：实验验证阶段已结束，方案设计阶段。所有"已验证"结论均附真机/模拟器证据。

---

## 一、结论先行

1. **接口层完全打通。** CAPI（`NODE_SYSTEM_MATERIAL` + `native_material.h`）在 Kuikly 的 C++ 渲染层可直接调用，材质对象创建成功、属性下发返回 0、设备判定为最高算力档 EXQUISITE。
2. **视觉没有生效，原因不是属性或开关配置错误，而是"节点位置不合法"。** 系统报 `Material inactive: out of scope`，明确要求组件必须位于 Navigation 标题栏或底部 TabBar，或在弹窗白名单内。
3. **存在组件覆盖缺口。** Text / Image 等组件的渲染类不继承 `KRView`，当前接入的属性分发没有覆盖到它们，属性根本没下发给系统。
4. **必须在 ArkTS 侧破局。** NDK 无法创建 Navigation / Tabs 容器（见 §5.2），纯 C++ 渲染层无法满足生效范围要求。

---

## 二、能力全景

### 2.1 两套 API，不要混用

| | ArkUI `uiMaterial` | HDS `hdsMaterial` |
|---|---|---|
| 来源 | `@kit.ArkUI` | `@kit.UIDesignKit` |
| 起始 | API 26.0.0 | 6.1.0(23) |
| 适用 | 任意组件通用属性 + 弹窗 options | 仅 HDS 导航 / HDS 底部页签 |
| 厚薄档位 | `ImmersiveStyle` 五档 | 无对等概念，由 `materialType` + `materialLevel` 决定 |

Kuikly 走 ArkUI 的 `uiMaterial` 体系（其 CAPI 对等物），不使用 HDS。

### 2.2 CAPI 接口清单（已在本机 SDK 26.0.0 核实）

头文件 `<arkui/native_material.h>`，库 `libace_ndk.z.so`，全部 `@since 26.0.0`：

- 能力探测：`OH_ArkUI_NativeModule_GetSystemMaterialSupported()`
- 算力档：`OH_ArkUI_NativeModule_GetGlobalMaterialLevel()` → `EXQUISITE` / `GENTLE` / `SMOOTH`
- 材质对象：`ImmersiveMaterial_Create(style)` / `Destroy` / `SetStyle` / `SetMaterialColor` / `SetApplyShadow` / `SetInteractive` / `SetLightEffect`
- 光效：`LightEffectOptions_Create` / `Destroy` / `SetColor`
- 节点属性：`NODE_SYSTEM_MATERIAL = 127`，`ArkUI_AttributeItem.object` 传 `ArkUI_ImmersiveMaterialHandle`
- 弹窗专用：`OH_ArkUI_NativeModule_CustomDialog_SetSystemMaterial(handle, material)`、`..._SetSystemMaterialInOptions(options, material)`

### 2.3 材质参数

- `style` 五档：`ULTRA_THIN(0)` / `THIN(1)` / `REGULAR(2)` / `THICK(3)` / `ULTRA_THICK(4)`。仅高/中算力生效，低算力不报错但无效。
- `materialColor`：0xAARRGGBB，**必须带透明度**；低算力档下直接作为 `backgroundColor`。
- `applyShadow`：默认 true，优先于 `shadow` 通用属性。
- `interactive`：按压弹性形变，全档位生效。
- `lightEffect`：触点光效，传 NULL 表示禁用，全档位生效。

### 2.4 算力档行为差异

- 高/中算力：影响材质层滤镜（毛玻璃、折射、高光）与阴影。
- 低算力：只影响 `backgroundColor` / `borderColor` / `borderWidth` / `shadow`，**且 `style` 与自动反色不生效**。

---

## 三、已验证事实（模拟器，API 26.0.0）

### 3.1 证据链

| 环节 | 证据 | 判定 |
|---|---|---|
| 设备是否支持 | 日志 `KRSystemMaterial: create ok`（该日志前置 `GetSystemMaterialSupported()` 判空门） | 支持 |
| 算力档 | `level=0` | EXQUISITE 最高档 |
| 材质对象 | `create ok, style=N` 全部成功 | 创建成功 |
| 属性下发 | `setAttribute NODE_SYSTEM_MATERIAL ret=0` | 属性键与参数均合法 |
| 渲染结果 | `Material inactive: out of scope. Use component in navigation title bar or Tabbar.` | 被生效范围规则丢弃 |

### 3.2 关键发现：属性下发条数与组件数不匹配

Demo 共 14 个组件设置材质，日志只有 **12 条** 下发记录。

按 style 序列比对，缺失的正是 **Text（style=2）与 Image（style=3）**。

原因已定位（源码事实）：

```125:125:core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextView.h
class KRRichTextView : public IKRRenderViewExport {
```

```33:33:core-render-ohos/src/main/cpp/libohos_render/expand/components/image/KRImageView.h
class KRImageView : public IKRRenderViewExport {
```

这两个类**直接继承 `IKRRenderViewExport`，不继承 `KRView`**，而当前的属性分发加在 `KRView::SetProp`，因此 Text / Image 的属性从未被处理。同类未覆盖的还有 `KRScrollerView`、`KRImageViewWrapper`、`KRGradientRichTextView`。

### 3.3 为什么不是"开关没开"

- `module.json5` 未配置时是 DEFAULT 模式，组件级设置**仍然生效**；
- 若为 DISABLE 模式，官方语义是"任何组件都无法启用"，不会走到位置校验这一步，也就不会打印 `out of scope`；
- 且应用级开关只作用于系统组件清单（Dialog / Toast / AlphabetIndexer / Chip / Select / Menu / Toggle / SegmentButton / Slider / bindSheet / SelectionMenu），Kuikly 的组件是 `ARKUI_NODE_STACK` 自绘节点，**不在这个清单里**，配 metadata 也不会改变结果。

---

## 四、生效范围（唯一的硬约束）

系统规定，以下三类**全页面生效**：

1. 弹窗类组件：AlertDialog / ActionSheet / CustomDialog / 各类 PickerDialog / SelectionMenu / AlphabetIndexer 弹窗 / Text `copyOption` 菜单
2. 弹窗类接口：**PromptAction / ArkUI_NativeDialog** / `@ohos.promptAction` / Popup / Tips / 菜单 / 半模态
3. 原生控件：Slider / Toggle / Select

**其余所有组件**，只在 Navigation/NavDestination 标题栏，或横向 Tabs（`barOverlap(true)` + `vertical(false)` + `barPosition(BarPosition.End)`）的底部 TabBar 内生效。

Kuikly 的自绘 Stack 属于"其余"，因此内容区全军覆没。

> 注意：Kuikly 的 Slider / Switch 是 `commonMain` 自绘实现（`core/src/commonMain/.../views/SliderView.kt`、`SwitchView.kt`），**不是** ArkUI 原生 Slider / Toggle，因此不享受白名单。

---

## 四·补 官方支持的组件清单与 Kuikly 映射

来源：官方《组件适配沉浸光感》。共四大类。

### 1. 导航类

| 组件 | 适配接口 | 要点 |
|---|---|---|
| Navigation 标题栏 | `NavigationTitleOptions.systemMaterial` | 生效范围是返回键与非自定义 Menu；建议 `barStyle=STACK` |
| 底部页签 Tabs | `barFloatingStyle.FloatingTabBarStyle.systemMaterial` | 需 `barOverlap(true)` + `vertical(false)` + `barPosition(End)` 三条件同时满足；`TabContent` 不支持 |
| AlphabetIndexer 索引条弹窗 | 不设置 `popupBackground` / `popupBackgroundBlurStyle` 时默认开启 | 默认 THICK；与那两个属性互斥 |

### 2. 弹窗类（全页面生效，无需满足位置约束）

| 组件 | 适配接口 | 默认样式 |
|---|---|---|
| Dialog（CustomDialog / AlertDialog / ActionSheet / bindSheet / 各类 PickerDialog） | options 中的 `systemMaterial` | ULTRA_THICK |
| Toast | `ShowToastOptions.systemMaterial` | THICK |
| Menu | `ContextMenuOptions.systemMaterial` | THICK |
| Popup | `PopupOptions.systemMaterial` | 不默认开启，需显式设置 |
| Tips | `TipsOptions.systemMaterial` | 不默认开启，需显式设置 |

### 3. 按钮与选择类

| 组件 | 适配接口 | 要点 |
|---|---|---|
| Button | 通用属性 `systemMaterial` | THIN/ULTRA_THIN 时 fontColor 用系统资源可自动反色 |
| Select | `systemMaterial` + `menuSystemMaterial` | 下拉按钮与下拉菜单相互独立 |
| Toggle | 通用属性 `systemMaterial` | 仅 Switch 类型生效；材质参数只作开关标记 |
| Slider | 通用属性 `systemMaterial` | 材质参数只作开关标记；滑块形状 DEFAULT 且 SliderStyle 非 NONE 才生效 |
| ChipGroup / SegmentButton | `backgroundSystemMaterial` 等专属字段 | 部分子类型不支持 |

### 4. 其余组件

通用属性 `systemMaterial`，生效区域限 Navigation/NavDestination 标题栏或横向 Tabs 底部 TabBar。

### Kuikly 组件映射与可行性

| Kuikly 组件 | OHOS 侧实现 | 是否白名单原生组件 | 结论 |
|---|---|---|---|
| Modal | `ArkUI_NativeDialogHandle` | **是**（弹窗类接口） | 唯一可直接生效的通道 |
| View / 容器 | `ARKUI_NODE_STACK` 自绘 | 否 | 属"其余组件"，受位置限制 |
| Text | `KRRichTextView` 自绘 | 否 | 同上 |
| Image | `KRImageView` 自绘 | 否 | 同上 |
| Slider | `commonMain` 自绘 | 否（非原生 Slider） | 改用 `ARKUI_NODE_SLIDER` 可进白名单 |
| Switch | `commonMain` 自绘 | 否（非原生 Toggle） | 改用 `ARKUI_NODE_TOGGLE` 可进白名单 |
| Input | `ARKUI_NODE_TEXT_INPUT` | 否 | 属"其余组件" |

NDK 侧确认存在 `ARKUI_NODE_SLIDER = 17`、`ARKUI_NODE_TOGGLE = 5`、`ARKUI_NODE_CHECKBOX = 11`。iOS 侧已有 `iOSSlider` / `iOSSwitch` 原生分支先例，OHOS 侧可照此新增 `OhosSlider` / `OhosSwitch` 原生映射，使这两个组件真正进入白名单并全页面生效。

---

## 四·二 二阶段结论：自绘节点进 scope 后完全生效（已验证）

### 实验方法

把 Kuikly 的原生节点树挂载点 `ContentSlot(nodeContent)`（原位于 `core-render-ohos/src/main/ets/KRNativeRender.ets` 的普通 `Stack` 内）临时移入 **Navigation 标题栏 builder**，结构照抄官方 FAQ 验证过的写法：

```
Navigation() { ... }
  .title({ builder: <内含 ContentSlot 的 Stack>, height: 500 }, { barStyle: BarStyle.STACK })
```

属性层完全未改动，仍是同一个 `NODE_SYSTEM_MATERIAL` 与同一套 Kuikly 自绘节点。开关：`KRNativeRender.ets` 的 `navScopeTest`（验证完需恢复为 `false`）。

### 结果

| 观察项 | 结果 |
|---|---|
| 页面自绘节点材质 | **全部生效**：档位差异、`materialColor` 赋色、`applyShadow` 开关、边框折射均可见 |
| 页面节点 out of scope 告警 | **零条** |
| 弹窗背板（CustomDialog 通道） | 生效（薄档透、厚档糊、边缘流光） |
| 弹窗内容节点 | 仍报 out of scope（每次开弹窗 2 条），位于 dialog 内容区，不在标题栏 scope 内 |

### 结论

1. **阻碍自绘组件的从来不是"自绘身份"，而是"位置"。** 只要落在官方规定的生效区域内，Kuikly 的自绘节点与 ArkTS 原生节点行为一致。
2. **属性层与 CAPI 通道无需任何改动**，纯位置问题。
3. 弹窗内容节点与弹窗背板是两条独立路径：背板走「弹窗类接口」白名单，内容节点仍受位置约束。

### 产品化的关键难点

当前验证态是把**整棵 Kuikly 页面**塞进标题栏，语义上不成立（页面会被标题栏行为裁剪、滚动隐藏等影响）。要产品化，需要具备"**把 Kuikly 视图树的某个子树单独挂到另一个 ArkTS 容器**"的能力，也就是：

- Kuikly 侧新增一个语义组件（如「标题栏容器」），声明"这棵子树要放进系统标题栏 scope"；
- OHOS 侧需要支持**多个 NodeContent 挂载点**，让同一棵 Kuikly 视图树按需切分挂载，而不是只有一个根 `ContentSlot`。

这是本方案里唯一需要动渲染层架构的部分，建议作为独立课题评估。

---

## 五、破局路径

### 5.1 路径 A：弹窗通道（成本最低，优先验证）

`ArkUI_NativeDialog` 在白名单内且可全页面生效，CAPI 提供 `CustomDialog_SetSystemMaterial`。

当前状态：**未验证**。Demo 两次打开 Modal，均未出现 `CustomDialog_SetSystemMaterial ret=` 日志，推断为该 API 26 符号在模拟器 ROM 未导出（弱符号取 nullptr，代码静默返回）。

> 推断，待验证：需在调用点补分支日志，区分"符号未解析"与"调用返回错误码"。

待确认事项：`KRModalView` 创建 dialog 时调用了 `enableCustomStyle(true)`，官方明确 CustomDialog 背板自定义（`customStyle: true`）不支持沉浸材质，NativeDialog 是否同理需实测。

### 5.2 路径 B：ArkTS 侧容器（能拿到完整效果，改动大）

NDK 的 `ArkUI_NodeType` 枚举共 147 项，**不含 Navigation / NavDestination / Tabs / TabContent**。这意味着纯 C++ 侧无法构造满足生效范围的容器。

可行做法：

1. 在宿主工程 ets 侧声明 `Navigation` 或 `Tabs`；
2. 需要材质的元素通过 Kuikly 已有的 **`KRForwardArkTSView` 转发机制**落到 ArkTS 侧容器里；
3. 或为 Kuikly 新增"标题栏容器 / 底部页签容器"组件类型，由 ArkTS 侧承载外层结构，Kuikly 只填充内容。

限制：只有放进标题栏 / 底部 TabBar 的元素才有效，通用性受限。

### 5.3 路径 C：不做

不做自绘模拟（用户明确要求），不使用 `backgroundEffect` / `backgroundBlurStyle` 冒充系统材质。

---

## 六、适配方案（分阶段）

### 阶段 0：补齐组件覆盖（必须做，与破局路径正交）

把材质属性的分发从 `KRView::SetProp` 上提到基类 `IKRRenderViewExport::SetProp`，或在各直系子类中分别接入，覆盖：

- `KRRichTextView`（Text / RichText）
- `KRGradientRichTextView`
- `KRImageView` / `KRImageViewWrapper`
- `KRScrollerView` / `KRScrollerContentView`
- `KRView` 系（View / Mask / Modal / Button 底层）

`core-render-ohos/src/main/cpp/libohos_render/expand/components/ComponentsRegisterEntry.h:48-139` 是完整注册清单，以此为准。

### 阶段 1：验证并落地弹窗通道

- 补日志确认 `CustomDialog_SetSystemMaterial` 可用性；
- 若可用，为 Modal / Dialog 类组件提供材质开关；
- 若 `enableCustomStyle(true)` 导致失效，评估改用 `SetSystemMaterialInOptions` 或调整 dialog 配置。

### 阶段 2：ArkTS 容器方案

- 设计"标题栏容器 / 底部页签容器"的 Kuikly 组件语义；
- 用 `KRForwardArkTSView` 打通 ets 侧承载；
- 明确告知业务：只有这两个位置的材质才生效。

### 阶段 3：Kotlin 跨端 API 定稿

- 统一命名（与 iOS `glassEffectIOS` 对齐，需决策是否合并为跨端 `glassEffect()`）；
- 参数：style / interactive / lightEffectColor / materialColor / applyShadow；
- 平台差异处理：iOS 可作用于任意容器，OHOS 受位置约束，需在文档与降级策略中明确；
- 提供能力探测接口（对应 `GetSystemMaterialSupported`）。

---

## 七、CAPI 相对 ArkTS 的能力缺口

| 能力 | ArkTS | CAPI | 影响 |
|---|---|---|---|
| `colorInvert` 自动反色 | 有 | **无** | 无法在 C 侧做子树颜色反色 |
| `Material.empty` 显式关闭 | 有 | **无** | "关闭材质"语义无法表达；`resetAttribute` 只对应"恢复默认" |
| `MaterialLevel` 设置 | HDS 可设 | 只读 | 跟随设备档位即可，无需设置 |
| `lightEffect` 触点光效 | 有 | **有**（`LightEffectOptions_Create/SetColor` + `ImmersiveMaterial_SetLightEffect`） | 两侧都支持，不是缺口 |

> 已核实：官方 CAPI 文档函数汇总与本机构 `native_material.h`（1–406 行）均无 ColorInvert / empty 相关接口。

### 关于 `colorInvert` 的补充

**它是"官方有、CAPI 没有"的典型**，但有两个额外约束决定了它现阶段基本不可用：

1. 官方要求材质必须是 **THIN / ULTRA_THIN** 档才可能触发反色；
2. 前景色**必须来自资源接口**（`$r(...)`）才行，而 Kuikly 下发的是数值色值 —— 即使接上也大概率无效。

另外：容器级材质是在 **ArkTS 侧**创建的（`uiMaterial.ImmersiveMaterial`），那里 `colorInvert` 是可用的。若将来要支持，容器级比元素级更容易做。

### 关于 `lightEffect` 的澄清

它**不是**官方缺口。CAPI 与 ArkTS 两侧都提供，元素级材质的链路（`KRSystemMaterialUtil`）已经接好。
当前只有**容器级**材质没接 —— 因为容器材质在 ArkTS 侧创建，而 `LightEffectOptions` 的构造方式尚未确认。属于"活没干完"，而非"能力缺失"。

---

## 八、工程约束

1. **弱符号声明。** `native_material.h` 中所有函数带 `__attribute__((availability(ohos, introduced=26.0.0)))`，必须以 `__attribute__((weak))` 重声明，否则运行在低版本系统时 `libkuikly.so` 会因符号未解析整体加载失败。仓库已有先例：`KRTextEditorCommon.h:79-90`。
2. **编译期门控。** 用 `#if __has_include(<arkui/native_material.h>)` 保护，SDK < 26 时整体裁剪。
3. **版本门槛。** 应用 `targetSdkVersion >= 26.0.0`。
4. **生命周期。** `ImmersiveMaterial_Create` 返回的对象必须 `Destroy`；`getAttribute` 返回的是静态成员，**不可** Destroy。
5. **属性顺序。** 通用属性设置时，`systemMaterial` 必须写在其他样式属性**之后**（通过 options 参数设置时无此限制）。
6. **背景冲突。** 组件必须保持 `backgroundColor` 透明，不可叠加 `backgroundBlurStyle` / `backgroundEffect`。
7. **功耗约束（官方 8 条）。** 控制面积、禁止材质嵌套、不叠加模糊、控制弹窗尺寸、避开视频/动图、控制反色范围、保持参数稳定、避免重复阴影。

---

## 九、当前代码状态

### 框架（core）

| 文件 | 内容 |
|---|---|
| `core/.../base/Attr.kt` | `systemMaterialOhos()`：元素级材质属性，挂在基类 `Attr` 上，所有组件可用；`modalCustomStyleOhos()`；属性键常量 |
| `core/.../views/ohos/ImmersiveLightSenseOhosView.kt` | **沉浸光感容器组件**：`ImmersiveLightSenseOhos` + `ImmersiveLightSenseOhosAttr.immersiveMaterial(...)` + 独立 Event 类 |
| `core/.../utils/PlatformUtils.kt` | `isImmersiveMaterialSupported()` 能力探测 |

### 渲染层（core-render-ohos）

| 文件 | 内容 |
|---|---|
| `.../utils/KRSystemMaterialUtil.h/.cpp` | CAPI 弱符号封装、参数解析、节点与弹窗双通道 |
| `.../export/IKRRenderViewExport.cpp` | **基类统一分发**材质属性，覆盖所有组件（含不继承 KRView 的 Text/Image/Scroller） |
| `.../components/KRImmersiveLightSenseView.ets` | 容器组件 ArkTS 实现：`Navigation` 标题栏 + `ContentSlot` + 容器级材质对象 |
| `.../components/forward/KRForwardArkTSViewV2.h/.cpp` | V2 转发通道（**已修 frame 链路缺陷**，见 §十二） |
| `.../manager/KRNativeRenderController.ets` | 能力探测上报：`feature.immersive_material` |
| `.../modal/KRModalView.cpp` | Modal 弹窗通道 + `enableCustomStyle` 可控 |

### Demo

| 页面 | 内容 |
|---|---|
| `ImmersiveLightSenseOhosDemoPage` | 裸容器版：业务自定义标题栏内容（返回/标题/操作），材质 REGULAR(2)，带降级对照 |
| `ImmersiveLightSenseNavBarDemoPage` | NavBar 版：只加一个开关，材质 ULTRA_THICK(4) |
| `demo/.../base/NavigationBar.kt` | 原 NavBar 新增 `immersiveLightSense` 开关 + `immersiveMaterialStyle` 档位 |

## 十、验证结论汇总（实测）

| 观察项 | 结果 |
|---|---|
| 设备算力档 | 两次实测分别为 `level=0`(EXQUISITE) 与 `level=1`(GENTLE)，**两档下 style 均生效** |
| 材质档位差异 | ULTRA_THIN(0) / REGULAR(2) / ULTRA_THICK(4) 三档模糊度肉眼可区分 |
| 容器级"玻璃底" | 生效（标题栏整条区域呈磨砂，与内容区有可见分界） |
| scope 判定 | 容器内节点 0 条 `out of scope`；容器外对照组按预期报 1 条 |
| V2 frame 链路 | `SetRenderViewFrame` → `sizeDidChanged` 高度正确下发（如 93.85vp） |

判定口径：**容器内的材质节点应 0 条 out of scope**；若出现，说明该节点没真正进入标题栏的 ContentSlot。

## 十一、下一步待办

1. **（后续方向）Dialog 通道**：`ArkUI_NativeDialog` 在官方全页面生效白名单内，走 `CustomDialog_SetSystemMaterial`；需确认 `enableCustomStyle(true)` 是否会像 CustomDialog 背板自定义一样禁用材质。
2. **（后续方向）Slider / Switch 原生映射**：参照 `iOSSlider` / `iOSSwitch` 做 `OhosSlider` / `OhosSwitch`，使这两个组件真正进入白名单、无需标题栏容器。
3. **降级路径实测**：目前所有设备 `isImmersiveMaterialSupported()` 均为 true，未跑过"不支持"分支，需低版本真机验证。
4. **（未完成）容器级 lightEffect**：CAPI 与 ArkTS 都有该能力，子元素已接；容器材质在 ArkTS 侧创建，`LightEffectOptions` 构造方式未确认故未接。

---

## 十二、路线甲（标题栏容器）已验证生效

### 结构

`ImmersiveLightSenseOhos`（Kotlin）→ `KRImmersiveLightSenseView`（V2 转发）→ ArkTS 侧 `@Component struct ImmersiveLightSenseContainer`
渲染 `Navigation`，子节点经 `OH_ArkUI_NodeContent_AddNode` 挂进标题栏 builder 里的 `ContentSlot`。

### 结论

自绘节点只要落在标题栏 scope 内就能渲染沉浸材质，且**不需要把整棵页面塞进标题栏**——
子树的按需挂载已通过 `KRForwardArkTSViewV2` + `NodeContent` 打通。Demo 实测：容器内的胶囊出现材质填充，
容器外的对照组只有描边、无材质。

### 踩过的坑（按耗时排序）

1. **ArkTS 语法限制。** `.title()` 的 builder 只接受 `@Component struct` 的成员 `@Builder` **引用**
   （`builder: this.Xxx`），这是唯一可行写法：
   - 不能在 lambda 里直接写 `Stack(){}` —— ArkTS 解析不过（报 `';' expected`）；
   - 不能在 lambda 里调用全局 `@Builder` —— 能编译，运行时抛 `observeComponentCreation2 of undefined`。
2. **标题栏高度必须是 `@Prop`。** 首次 build 早于布局，高度为 0；普通属性不接收父层更新，
   标题栏会一直停在 0 高度、内容被裁掉（现象：Inspector 里节点齐全，屏幕上不渲染）。
3. **`KRForwardArkTSViewV2` 的 frame 链路是断的（框架缺陷，已修）。** 两个问题叠加：
   - `ToSetProp` 直接透传给 ArkTS，绕过了基类的 frame 处理，`SetRenderViewFrame` → ArkTS `sizeDidChanged` 永不回调；
   - 其 props handler（`KRArkTSViewBasePropsHandler`）对所有属性 `return false`，frame 不会落到原生节点上，
     转发节点因此没有尺寸，子树里的百分比布局无从计算。

   修法：`ToSetProp` 对 `frame` 补一次基类处理；`SetRenderViewFrame` 里显式 `UpdateNodeFrame`；
   `CreateNode` 末尾补一次当前 frame。
4. **语义约束。** 标题栏容器应占据 NavBar 的位置，与自绘 NavBar 二选一，不能塞在页面中间（早期 Demo 就是错的）。

### 生效范围补充

容器内的节点受 scope 约束：容器外对照组的节点会打印 `Material inactive: out of scope.`，容器内的不再打印。
材质档位用 `ULTRA_THIN(0)` 时模糊最轻、透明感最强，最容易肉眼判定底层内容是否透出。

---

## 十三、API 决策（已定）

| 议题 | 决策 | 说明 |
|---|---|---|
| 跨端统一 | **不做** | 沉浸光感与 iOS 液态玻璃是两个独立赛道：视觉表现、生效范围、API 都不同。鸿蒙单独一套 API，不与 `glassEffectIOS` 合并。 |
| 组件命名 | 体现「沉浸光感」英文 | `ImmersiveLightSenseOhos`（Kotlin）/ `KRImmersiveLightSenseView`（ArkTS）。名字即语义，业务一眼可知这是鸿蒙专属的沉浸光感容器，与自绘标题栏二选一。 |
| Dialog / Slider / Switch 原生映射 | **后续方向** | 当前不投入。等正式标题栏组件建设完成后，可参考本容器的实现（V2 转发 + NodeContent 挂载）。 |

### 对外 API 清单

1. **容器**：`ImmersiveLightSenseOhos { ... }`，属性继承通用布局属性；**高度即系统标题栏高度**。
   不支持沉浸光感的设备上自动降级为普通容器，子视图照常显示（只是没有材质），业务无需写降级分支。
2. **材质属性**：`attr.systemMaterialOhos(enable, style, interactive, applyShadow, lightEffectColor, materialColor)`，
   挂在基类 `Attr` 上，因此容器内的 View / Text / Image / Input 等都能设。生效范围不满足时静默忽略。
3. **能力探测**：`PlatformUtils.isImmersiveMaterialSupported()`。

### 能力探测的通路

渲染层在构建 pageData 时，用系统接口 `uiMaterial.isImmersiveMaterialSupported()` 判定，
写入 `pageData["feature"]["immersive_material"]`（与既有的 `box_shadow_fill` 同一机制）；
Kotlin 侧 `PlatformUtils` 通过 `PagerData.hasFeature("immersive_material")` 读取。

**为什么不用 OS 版本号判断**（iOS 的 `isLiquidGlassSupported` 就是按版本号）：同一版本号的机型
也可能因算力档不支持，用系统接口才是准确的。低版本 ROM 上该接口不存在，故做了异常兜底返回 false。

---

## 十四、官方《组件适配沉浸光感》全量对照与路线定论（终版）

> 依据：官方文档《组件适配沉浸光感》（arkts-immersive-light-sense-component-adaptation）。
> 核心规则：**沉浸光感按「生效区域」划分，不按组件划分** ——
> 通用属性 `systemMaterial` 仅在 Navigation/NavDestination 标题栏或横向 Tabs（BarPosition.End）
> 的底部 TabBar 中生效；白名单控件（弹窗类/Select/Toggle/Slider）则全页面生效。

### A. 全页面生效（无区域限制）—— 主战场，已全部交付

| 官方组件 | 开启方式 | 官方特殊规则 | Kuikly 现状 |
|---|---|---|---|
| Select 下拉按钮 | `systemMaterial`(按钮) + `menuSystemMaterial`(菜单,**独立接口**) | 按钮与菜单相互独立；关闭必须 `Material.empty`(不能 undefined,否则组件不上屏)；lightEffect 替代按压态 | ✅ `ImmersiveLightSenseSelectOhos`（按钮+菜单都配） |
| Toggle 开关 | `systemMaterial` | Checkbox 型官方未适配；**Switch 型参数仅作开关标记**(视觉用系统预设)；Button 型同 Button | ✅ `ImmersiveLightSenseSwitchOhos`(CAPI) |
| Slider 滑动条 | `systemMaterial` | **参数仅作标记**；交互反馈需 BlockType.DEFAULT 且 Style≠NONE | ✅ `ImmersiveLightSenseSliderOhos`(CAPI, OutSet) |
| Dialog 系弹窗 | 各 options 的 `systemMaterial` | 默认 ULTRA_THICK；大面积弹窗不建议开；CalendarPicker 弹窗不支持 | ✅ `ImmersiveLightSenseDialogOhos` |
| Toast / Menu | 应用级 ENABLE 时默认 THICK | 浮层 API,非视图组件 | 见路线③ |
| Popup / Tips | **仅组件级显式开启** | 必须在调用 options 传 systemMaterial | 见路线③ |

### B. 区域限定（仅标题栏 / 底部 TabBar）—— 容器方案覆盖

| 官方组件 | 开启方式 | Kuikly 现状 |
|---|---|---|
| Navigation 标题栏 | 应用级 + `systemMaterial` | ✅ `ImmersiveLightSenseOhos` 容器 |
| Tabs 底部页签 | `barFloatingStyle.systemMaterial`，三条件缺一不可(barOverlap=true + vertical=false + BarPosition.End)；TabContent 不支持 | ✅ `ImmersiveLightSenseTabBarOhos`(HdsTabs) |
| **Button 按钮** | 仅组件级 `systemMaterial` | ✅ **无需原生组件**（定论见下） |
| ChipGroup / SegmentButton | `backgroundSystemMaterial` 等 | 暂缓（低频 HMS 高级组件，Toggle-Button 型可顶替） |
| **其余任意组件**（通用属性） | `systemMaterial` | ✅ **容器方案等价覆盖**（定论见下） |

### C. 路线定论（三条）

**① 已交付（6 组件）**：标题栏容器 / TabBar / Dialog / Select / Toggle / Slider。
覆盖官方全部「无区域限制」控件 + 两个区域大件。

**② 无需额外原生组件 —— 含 Button**：

Button 只在标题栏/TabBar 生效（官方原文），而这两个区域我们的容器已覆盖。
关键事实链：容器内的 Kuikly 自绘节点是**真实的 ArkUI CAPI 节点**，
其 `immersiveMaterial()` 由 C++ 基类统一下发 `NODE_SYSTEM_MATERIAL`（日志已证 ret=0），
节点又落在系统生效区域内 —— 因此 **「Kuikly 自绘按钮 + immersiveMaterial() + 放进容器」
与官方 Button 的用法等价**，无需 NODE_BUTTON 原生组件。
12013 标题栏内的 glassPill（自绘胶囊 + 材质 + interactive）即此模式，已视觉验收。
保留条件：若后续实测发现通用属性路径在 CAPI 节点上**缺 per-button 交互形变/点光源**，
再启动原生 Button（CAPI 有 NODE_BUTTON，模式照抄 Slider）。

**③ 浮层类是能力桥接，不是组件**：

Toast/Menu/Popup 是 API 调用而非视图组件，不落地为 Kuikly 组件。
- 应用级开关（MaterialState.ENABLE）开启后，**Toast/Menu 默认带 THICK 材质，自然生效**
  （Kuikly 的 Toast 若路由到系统 showToast 即自动继承）；
- Popup/Tips 官方要求组件级显式传 systemMaterial —— 由宿主/业务在鸿蒙侧调用处负责；
- **唯一待落地的桥**：把「应用级 MaterialState 开关」暴露为 Kuikly API（一个 module method，
  调用 native 设置即可），这是浮层自然生效的前提。单独立项，工作量极小。

### D. 业务侧必须知道的四条

1. 自绘节点拿不到材质 —— 除非放进容器（标题栏 scope）或用原生组件；
2. Toggle(Switch)/Slider 的 style/interactive 参数**实际只是开关标记**，视觉由系统预设决定；
3. TabContent 不支持材质 —— TabBar 的玻璃底只在胶囊上；
4. 自动反色只认系统颜色资源（`$r('sys.color.font_primary')`），Kuikly 硬编码颜色不反色，
   业务在浅色材质上需手动用深色文字。

### E. Select 适配的三条硬规则（实测踩坑记录）

1. **关闭材质必须 `uiMaterial.Material.empty`**，传 undefined 会导致组件渲染异常
   （实测：material created 有日志、onAppear 零次，控件空白）；
2. **菜单材质独立接口 `menuSystemMaterial`**，只配按钮则下拉菜单无光感；
3. **V2 转发组件不能放进滚动容器**：宿主位置为一次性静态下发（NODE_POSITION），
   Scroller 内位置不生效且不跟随滚动 —— Select 需 absolutePosition 固定定位
   （标题栏/TabBar 的固定定位形态不受影响）。
