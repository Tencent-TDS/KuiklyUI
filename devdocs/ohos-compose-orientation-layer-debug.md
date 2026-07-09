# 鸿蒙 Compose 转屏 — 视图层级与漏色问题

> 最后更新：2026-07-08  
> 关联页面：`ComposeOrientationOverlayDemo`（转屏调试）、`ComposeVideoOrientationDemo`（完整视频 Demo）  
> **完整方案交接：** [compose-orientation-overlay-handoff.md](./compose-orientation-overlay-handoff.md)  
> 真机验证：FMR0223C13000246

---

## 1. 默认视图层级（含透明度）

从外到内，**默认参与绘制的层级**如下：

```
L0  Window（系统窗口）                    ← 默认白底，不透明
 └─ L1  Index.ets — Stack                 ← 默认透明
     └─ L2  Kuikly = KRNativeRender Stack ← 当前黑色（调试用）
         └─ ContentSlot
             └─ L4  KRRenderView root_node_（Native 根） ← 透明 + 调试红框
                 └─ L5  Compose 内容树（DivView + setContent）
```

| 层级 | 文件 | 默认背景 | 当前调试态 |
|------|------|----------|------------|
| **L0** | `EntryAbility.ets` → `window.Window` | 系统白底 | 未上色 |
| **L1** | `Index.ets` — `Stack` | **透明** | 无调试浮层（已删除） |
| **L2** | `KRNativeRender.ets` — `Stack` | **黑色** | `Color.Black` |
| **L4** | `KRRenderView.cpp` — `root_node_` | **透明**（`bg = 0`） | **8px 红框** |
| **L5** | `ComposeOrientationOverlayDemo.kt` | 业务决定 | 紫/绿/黄 `DebugFrame` |

> **无 L3 垫层。** 早期 L3 蓝垫层是临时调试 artifact，已移除。

---

## 2. 尺寸同步链路（转屏时）

```
系统转屏
 → KRNativeRender.onSizeChange / onAreaChange
 → notifyContainerSizeChanged → OnContainerSizeChanged
 → onRenderViewSizeChanged → OnRenderViewSizeChanged
 → Kotlin PAGER_EVENT_ROOT_VIEW_SIZE_CHANGED
 → ComposeContainer.updateWindowContainer
```

L4 尺寸可能晚于 L2 扩窗 1～2 帧。

**L4 定位策略（当前）：** 始终贴 `(0,0)`，转屏居中由 Compose L5 根尺寸冻结处理。详见交接文档 §4.2。

---

## 3. 当前调试色

| 层级 | 调试标记 | 文件 |
|------|----------|------|
| **L2** | 黑色铺底 | `KRNativeRender.ets` |
| **L4** | 红框 8px | `KRRenderView.cpp` |
| **L5 根** | 紫框 | `ComposeOrientationOverlayDemo.kt` |
| **L5 列表** | 绿框 | 同上 |
| **L5 蒙层** | 黄框 | `PhoneStyleFullscreenOverlay` |

转屏时读框：

| 观察 | 含义 |
|------|------|
| 紫+黄一起倾斜 | L5 根在转屏中间态被横屏约束撑开（Compose 问题） |
| 红框偏、紫框正常 | Native L4 定位问题 |
| 边缘露白 | L0 白底或 L2 未铺满 |

---

## 4. 已解决问题摘要

| 问题 | 处理 |
|------|------|
| 转屏露白/绿 | L2 黑底 + L5 外层黑底铺底 |
| 竖屏时就开始放大 | 等 `isLandscape` 再动画 |
| 竖屏闪偏移 | L4 始终 (0,0)，去掉 Native 居中 |
| 紫黄框转屏倾斜 | L5 根冻结竖/横屏尺寸（见交接文档） |
| Index 调试浮层挡按钮 | 已删除 |

---

## 5. 相关文件

```
ohosApp/entry/src/main/ets/entryability/EntryAbility.ets
ohosApp/entry/src/main/ets/pages/Index.ets
core-render-ohos/src/main/ets/KRNativeRender.ets
core-render-ohos/src/main/ets/KRNativeRenderController.ets
core-render-ohos/src/main/cpp/libohos_render/view/KRRenderView.cpp
demo/.../ComposeOrientationOverlayDemo.kt
devdocs/compose-orientation-overlay-handoff.md
devdocs/compose-video-orientation-handoff.md
```

---

## 6. 待办

- [ ] 验证最新 L5 根冻结方案真机效果
- [ ] 稳定后移除所有调试色/框
- [ ] L2 黑底作为正式默认（非调试）
- [ ] 策略同步到 `ComposeVideoOrientationDemo`
