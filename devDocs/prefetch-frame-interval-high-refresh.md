# TODO：高刷屏下 Prefetch 帧间隔预算（改 VSync 时一并处理）

> 临时备忘。正式归档时可并入 `.ai/references/` 或 `openspec/changes/enable-lazy-list-prefetch/`。
> 触发时机：**改造 VSync / 帧时钟时**再修，不挡普通 2.1.21 发布线。

## 现状

- 常量：`KUIKLY_PREFETCH_FRAME_INTERVAL_NS = 16_666_667L`（按 60fps = 16.67ms）
- 调用点：`BaseComposeScene.render()` 末尾把该常量传给 `KuiklyPrefetchScheduler.processRequests(...)`
- 预算：`nextFrameTimeNs ≈ 本帧 VSync 时间戳 + frameIntervalNs`  
  `availableTimeNanos = max(0, nextFrameTimeNs - now)`（非 idle）
- idle 判定也用同一常量：`2 * frameIntervalNs` 内无新帧 → 视为 idle，预算 `Long.MAX_VALUE`

## 问题（高刷屏）

设备 120Hz 时真实帧间隔约 **8.33ms**，仍按 16.67ms 估下一帧：

1. **非 idle 预算偏松约一帧**（多给 ~8ms），prefetch 与主帧同线程、在 draw 之后跑，超支会直接挤占下一帧
2. **idle 阈值也偏松**（按 2×16.67ms 而不是 2×8.33ms），高刷下更不容易进无限预算（相对次要）

影响范围：

- **普通线（Compose 1.7.3 / `runtimeLegacyMain`）**：`lazyListPrefetchBuildSupportsPrefetch = false`，prefetch 硬关 → **不受影响**
- **`-prefetch` 产物（Runtime 1.9.3）**：默认开 prefetch → **会踩坑**；业务若在高刷屏验证/使用这条线，需要修

## 建议改法（与 VSync 改造一起做）

优先用**真实显示刷新间隔**，而不是写死 60fps。

### 方案 A（推荐，跟 VSync 改造绑定）

在平台 VSync 回调里拿到真实 interval（或 refresh rate → ns），经 `BaseComposeScene.render(...)` 传入 `processRequests` 的 `frameIntervalNs`。

| 平台方向 | 可能来源 |
|---------|---------|
| Android | `Choreographer` / display refresh rate |
| iOS | `CADisplayLink.duration` / preferred frame rate |
| OHOS | vsync 回调时间戳差或显示刷新率 API |
| Web | `requestAnimationFrame` 连续时间戳 |

### 方案 B（无平台 API 时的过渡）

用连续两次 `render(nanoTime)` 的差值估算：

```text
measured = nanoTime - previousDrawNanoTime
frameIntervalNs = clamp(measured, ~4ms, ~20ms)   // 约 240Hz～50Hz
首帧 / 间隔异常 → fallback 16.67ms
```

滚动连帧时估得较准；idle 大间隔不要当 refresh interval 用（应走现有 idle 分支）。

### 改动锚点

- `compose/.../lazy/layout/KuiklyPrefetchScheduler.kt`：`KUIKLY_PREFETCH_FRAME_INTERVAL_NS`
- `compose/.../ui/scene/BaseComposeScene.kt`：`render()` 里取 `frameIntervalNs` 并传给 `processRequests`
- 若引入平台真实 interval：帧时钟 / VSync 桥接层（改 VSync 时的主战场）

## 验证建议

- 120Hz 设备滚动 LazyList（prefetch 产物）：对比修前/修后单帧 `spentNs`、是否更容易抢主帧
- 打开 `ComposeFoundationFlags.isLazyListPrefetchTraceEnabled`，看 `processRequests` / `available_time_nanos`
- 60Hz 回归：行为应接近当前（常量本就按 60fps）

## 决议

- **要修**，但延后到 **VSync 改造** 一并做
- 普通线发布不依赖本修复
