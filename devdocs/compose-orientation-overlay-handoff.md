# Compose 转屏蒙层 Demo — 交接文档

> 最后更新：2026-07-08  
> 用途：新会话接续 `ComposeOrientationOverlayDemo` 开发与真机验证  
> 关联文档：[ohos-compose-orientation-layer-debug.md](./ohos-compose-orientation-layer-debug.md)、[compose-video-orientation-handoff.md](./compose-video-orientation-handoff.md)

---

## 1. 工作环境

| 项 | 值 |
|---|---|
| **工作区** | `/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg` |
| **DSL 类型** | **Compose DSL**（`ComposeContainer` / `setContent`） |
| **页面名** | `ComposeOrientationOverlayDemo` |
| **默认启动** | `ohosApp/entry/src/main/ets/pages/Index.ets` → `ComposeOrientationOverlayDemo` |
| **真机** | `FMR0223C13000246` |
| **参考实现** | `ComposeVideoOrientationDemo.kt`（真视频 + `movableContentOf`） |
| **Git 状态** | 大量未提交改动 |

---

## 2. 需求目标

实现 **Compose DSL** 横竖屏 Demo：

| 状态 | 预期 |
|------|------|
| 竖屏 | 顶栏黑色蒙层 + 横条视频（220dp）+ 评论区 |
| 点横屏 | `requestLandscape()` + 全屏黑蒙层 + 视频居中放大（360ms） |
| 点竖屏 | 回竖屏列表，底层页**始终保持竖屏布局** |

**硬性约束：**

- 不用 `Dialog`，只用 `Box`
- 视频和蒙层**不旋转**（旋转交给系统）
- 点横屏：`requestLandscape()` 与 `showOverlay = true` **同时触发**
- 返回：冻结横屏布局 → `requestPortrait()` → **100ms** 后再卸蒙层

---

## 3. 视图层级（默认）

```
L0  Window（系统窗口）
 └─ L1  Index.ets — Stack
     └─ L2  KRNativeRender Stack（黑色背景）
         └─ ContentSlot
             └─ L4  KRRenderView root_node_（透明 + 调试红框 8px）
                 └─ L5  Compose 内容（ComposeOrientationOverlayDemo）
```

| 层级 | 文件 | 当前状态 |
|------|------|----------|
| L0 | `EntryAbility.ets` | 系统窗口 |
| L1 | `Index.ets` | 透明 Stack（**已移除**调试浮层） |
| L2 | `KRNativeRender.ets` | **黑色** `backgroundColor(Color.Black)` |
| L4 | `KRRenderView.cpp` | 透明 + **8px 红框**（调试用） |
| L5 | `ComposeOrientationOverlayDemo.kt` | 业务布局 + 调试彩框 |

> **无默认 L3 垫层。** 早期文档中的 L3 蓝垫层是临时调试 artifact，已移除。

---

## 4. 当前方案（核心思路）

**原则：转屏中间态不用实时窗口约束重算布局，用「冻结尺寸」；Native 在 root 尺寸与容器尺寸不一致时负责居中（各轴分别居中），避免转屏黑边。**

> **方案变更（2026-07-08）**：旧方案为「Native 不居中，Compose 负责居中」。实测不可靠——竖→横时系统先扩窗（容器变横屏），但 Compose 上报的 root 内容尺寸仍滞后 1~2 帧为竖屏尺寸，Native 把 L4 顶在 (0,0) → 竖屏尺寸内容被顶到左上角，下方/右侧露黑。改为由 **Native 在 `UpdateRootNodeLayoutInContainer` 中当 root 与容器尺寸不一致时居中 L4**，尺寸对齐后自然落到 (0,0)。Compose 侧的 `Alignment.Center`/`TopStart` 冻结对齐保留（仅在尺寸已匹配时生效，不会与 Native 双重偏移）。

### 4.1 Compose 侧（`ComposeOrientationOverlayDemo.kt`）

**L5 根尺寸锁定（紫框 `DebugFrame`）：**

| 场景 | L5 根尺寸 | 对齐 |
|------|-----------|------|
| 竖屏列表 | `portraitWidth × portraitHeight` | TopStart |
| 进横屏（`enterPortraitLock`） | 点击时冻结的**竖屏**尺寸 | TopStart |
| 回竖屏（`frozenOverlayLayout`） | 点击时冻结的**横屏**尺寸 | Center |
| 横屏稳定后 | `fillMaxSize()` | — |

**进横屏流程：**

```
点「横屏」
  → 冻结竖屏尺寸 enterPortraitLock（含 window + video 尺寸）
  → requestLandscape() + showOverlay = true
  → 不渲染底层 PortraitMockPage
  → 蒙层内视频保持横条静止（竖屏锁）
  → 检测到 isLandscape=true 后 360ms 放大动画
  → L5 根切 fillMaxSize()
```

**回竖屏流程：**

```
点「竖屏」
  → 冻结横屏尺寸 frozenOverlayLayout
  → shouldRequestPortrait = true
  → requestPortrait()
  → 蒙层保留 100ms（PORTRAIT_HOLD_MS）
  → showOverlay = false，清除冻结状态
  → 恢复竖屏列表
```

**关键常量：**

```kotlin
VIDEO_EXPAND_ANIM_MS = 360
PORTRAIT_HOLD_MS = 100L
PortraitVideoHeaderHeight = 220.dp
```

**`OrientationFrozenLayout` 数据结构：**

```kotlin
data class OrientationFrozenLayout(
    val windowWidth: Dp,
    val windowHeight: Dp,
    val videoWidth: Dp,
    val videoHeight: Dp,
)
```

- `enterPortraitLock`：进横屏时锁定竖屏
- `frozenOverlayLayout`：回竖屏时锁定横屏（与 `ComposeVideoOrientationDemo` 的 `FrozenOverlayLayout` 同理，类名不同避免冲突）

**动画门控：** 仅 `isLandscape = windowW > windowH` 时才开始 `videoProgress.animateTo(1f)`；竖屏蒙层阶段 `snapTo(0f)`。

**外层黑色铺底：** `Box(fillMaxSize + Color.Black)` 包裹 L5 根，转屏过渡时未覆盖区域不露底色。

### 4.2 Native 侧（`KRRenderView.cpp`）

```cpp
void KRRenderView::UpdateRootNodeLayoutInContainer() {
    // ...
    if (容器尺寸与 root 尺寸一致) {
        UpdateNodeFrame(root_node_, KRRect(0, 0, rootW, rootH));  // 稳定态，无偏移
        return;
    }
    // root 与容器尺寸不一致（转屏中间态）→ L4 在容器内居中，黑边均匀分布在四边
    const float offsetX = (containerW - rootW) * 0.5f;
    const float offsetY = (containerH - rootH) * 0.5f;
    kuikly::util::UpdateNodeFrame(root_node_, KRRect(offsetX, offsetY, rootW, rootH));
}
```

- `OnContainerSizeChanged`：L2 容器尺寸变化时由 `KRNativeRender.ets` → `notifyContainerSizeChanged` 调用
- **已移除**竖→横双轴居中、部分轴居中（曾导致竖屏闪一帧非 (0,0) 偏移）

### 4.3 尺寸同步链路

```
系统转屏
 → KRNativeRender.onSizeChange / onAreaChange
 → notifyContainerSizeChanged → OnContainerSizeChanged
 → onRenderViewSizeChanged → OnRenderViewSizeChanged
 → Kotlin PAGER_EVENT_ROOT_VIEW_SIZE_CHANGED
 → ComposeContainer.updateWindowContainer
```

L4 尺寸可能晚于 L2 扩窗 1～2 帧。

---

## 5. 调试配色（代码中仍存在，稳定后应移除）

| 颜色 | 层级 | 文件 |
|------|------|------|
| **红框 8px** | L4 Native 根 | `KRRenderView.cpp` — `UpdateNodeBorder("8 solid #FFFF0000")` |
| **紫框** | L5 Compose 根 | `ComposeOrientationOverlayDemo.kt` — `DebugFrame(0xFFFF00FF)` |
| **绿框** | 竖屏列表页 | `DebugFrame(0xFF00C853)` |
| **黄框** | 蒙层内层 | `PhoneStyleFullscreenOverlay` — `DebugFrame(0xFFFFEB3B)` |
| **L2 黑底** | Kuikly Stack | `KRNativeRender.ets` |

`Index.ets` 的「视图层级调试」浮层**已删除**（曾固定左上角挡按钮、颜色说明过时）。

**读框技巧：** 紫+黄框一起倾斜 → L5 根在转屏中间态被横屏约束撑开（Compose 问题）；红框偏、紫框正常 → Native 定位问题。

---

## 6. 问题排查历程

| 现象 | 根因 | 处理 |
|------|------|------|
| 转屏露绿/白 | L2 透明或 L0 白底露出 | L2 改黑；外层黑底铺底 |
| 竖屏时视频就开始放大 | 蒙层一显示就 `animateTo` | 等 `isLandscape` 再动画 |
| 横屏后视频贴顶 | Native 把 L4 顶 (0,0)，竖屏尺寸内容被顶左上角露黑 | Native 在 root≠容器尺寸时居中 L4 |
| 竖屏闪一帧非 (0,0) | 旧 `else` 分支部分轴居中 | 改为不一致即整轴居中，对齐后为 (0,0) |
| 调试浮层挡「竖屏」按钮 | `Index.ets` 固定左上角 | 已删除浮层 |
| **紫+黄框一起倾斜**（截图） | L5 根在转屏中间态被横屏窗口约束撑开，叠 Native 偏移 | **L5 根冻结尺寸** + Native 不居中 |

---

## 7. 当前状态

### 已完成

- [x] `ComposeOrientationOverlayDemo` 完整转屏流程
- [x] L5 根尺寸冻结（进横屏锁竖屏、回竖屏锁横屏）
- [x] Native `OnContainerSizeChanged` + L4 贴 (0,0)
- [x] 动画门控、返回冻结、蒙层期间不渲染底层页
- [x] 调试边框便于分层观察
- [x] 删除 `Index.ets` 调试浮层
- [x] 真机多次编译安装

### 待验证 / 待办

- [ ] **最新版**（L5 根冻结 + **Native 居中 L4**）转屏时红/紫框是否居中、是否还露黑边
- [ ] 若紫框正常、红框偏 → 继续查 Native；若紫框仍斜 → 查系统转屏动画
- [ ] 稳定后去掉调试边框（红/紫/绿/黄）
- [ ] L2 黑底合入（非调试色）
- [ ] 与 `ComposeVideoOrientationDemo` 对齐（`movableContentOf`、真视频）
- [ ] 更新 [ohos-compose-orientation-layer-debug.md](./ohos-compose-orientation-layer-debug.md)
- [ ] Git 整理提交

---

## 8. 关键文件

```
demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/
  ComposeOrientationOverlayDemo.kt    # L5 主逻辑（本 Demo）
  ComposeVideoOrientationDemo.kt      # 参考实现

demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/base/
  BridgeModule.kt                     # requestLandscape / requestPortrait

ohosApp/entry/src/main/ets/pages/Index.ets
ohosApp/entry/src/main/ets/entryability/EntryAbility.ets

core-render-ohos/src/main/ets/KRNativeRender.ets
core-render-ohos/src/main/ets/KRNativeRenderController.ets
core-render-ohos/src/main/cpp/libohos_render/view/KRRenderView.cpp
```

---

## 9. 构建与安装

```bash
# 1. 编译 libshared.so（改 kotlin 必须）
# 注：需临时把 gradle-wrapper 7.6.3 → 8.0
KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="2.0.21-KBA-010" \
  ./gradlew -c settings.2.0.ohos.gradle.kts :demo:linkSharedDebugSharedOhosArm64

cp demo/build/bin/ohosArm64/sharedDebugShared/libshared.so \
   ohosApp/entry/libs/arm64-v8a/

# 2. 编译 HAP（改 cpp 也必须重编 HAP）
export DEVECO_SDK_HOME="/Applications/DevEco-Studio.app/Contents/sdk"
cd ohosApp
hvigorw assembleHap --mode module -p module=entry@default -p product=default -p buildMode=debug

# 3. 安装真机
hdc -t FMR0223C13000246 install -r entry/build/default/outputs/default/entry-default-signed.hap
hdc -t FMR0223C13000246 shell aa force-stop com.tencent.kuiklyohosdemo.luoyibu
hdc -t FMR0223C13000246 shell aa start -a EntryAbility -b com.tencent.kuiklyohosdemo.luoyibu
```

---

## 10. 下一会话建议起手

1. 真机走一遍：竖屏 → 横屏 → 竖屏，观察紫/红框是否还倾斜
2. 截图对比：乱帧时是**红框**还是**紫框**在动
3. 若 OK → 去调试色、整理提交
4. 若不行 → 查鸿蒙 `requestOrientation` 与窗口旋转动画配置；或对比 `ComposeVideoOrientationDemo` 的 `PhoneFullscreenHoldOverlay` 实现

---

## 11. 与 ComposeVideoOrientationDemo 的差异

| 项 | ComposeOrientationOverlayDemo | ComposeVideoOrientationDemo |
|----|------------------------------|----------------------------|
| 视频 | 蓝色 `MockVideoRect` 模拟 | 真视频 + `movableContentOf` |
| 进横屏动画 | 等 `isLandscape` 后动画 | 蒙层显示即动画（landscape 已 request） |
| L5 根锁定 | **整页根**冻结（最新方案） | 蒙层内层 `size(windowW, windowH)` |
| 折叠屏 | 未实现 | `FoldableExpandedMinWidth = 600.dp` |
| 冻结类名 | `OrientationFrozenLayout` | `FrozenOverlayLayout` |

最终目标：Overlay Demo 验证通过后，将尺寸冻结策略同步到 Video Demo。
