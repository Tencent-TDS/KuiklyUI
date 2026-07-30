# 统一设计尺寸与字号缩放最佳实践

## 适用场景

跨端业务通常只产出**一套设计稿**（例如以 iPhone 的 `393` 逻辑宽度为基准），却希望在所有设备、所有平台上呈现**完全一致的视觉尺寸与比例**。

要做到这一点有两个相互独立的关注点，按需选用：

| 关注点 | 作用 | 支持平台 |
|--------|------|---------|
| **统一设计宽度（核心）** | 让整页逻辑坐标宽度在所有设备上恒等于设计基准（如 393），实现"一套尺寸跨端一致" | Android（对齐）/ iOS（基准） |
| **字号缩放** | 文本字号是否跟随系统「字体大小」设置 | Android / iOS / HarmonyOS |

---

## 一、统一设计宽度：393dp 基准（核心）

### 原理：逻辑宽度 = 屏幕像素宽 ÷ density

Kuikly 的布局以逻辑单位（Android 为 dp）描述。Android 渲染层把根视图宽度上报给 Kotlin 布局侧时，换算公式是：

```
页面逻辑宽度 = 屏幕像素宽(px) / density
```

默认 `density` 取系统真机值，于是不同机型算出的逻辑宽度各不相同（如 1080px/2.75 ≈ 392.7，1440px/3.5 ≈ 411.4……）。**同一套 dp 数值在不同设备上显示的物理尺寸/比例就会不一致**，与按 `393` 设计的稿子对不齐。

> ⚠️ 常见误区：在 `getDisplayMetrics` 里写死 `density = 2f`。这只是换了个固定缩放比，逻辑宽度变成 `屏幕像素宽/2`，仍随设备变化，**并不能统一尺寸**。

### 做法：把逻辑宽度锁定到设计基准 393

选定统一设计宽度 `DESIGN_WIDTH = 393`（与 iOS 设计稿基准一致），让每个平台的逻辑宽度都恒等于它：

- **iOS：作为基准平台**，界面直接按 `393` 设计宽度实现（iOS 以 pt 为逻辑单位）。
- **Android：通过 FontAdapter 对齐**，把 `density` 动态计算为 `屏幕像素宽 / 393`，反推出"逻辑宽度恒为 393"。
- **总开关**：在 Delegate 中开启 `useHostDisplayMetrics()`，框架才会采用 FontAdapter 提供的 `DisplayMetrics`。

#### Android 实现

```kotlin
object KRFontAdapter : IKRFontAdapter {

    // 统一设计宽度基准：与 iOS 设计稿保持一致
    private const val DESIGN_WIDTH_DP = 393f

    override fun getDisplayMetrics(useHostDisplayMetrics: Boolean?): DisplayMetrics {
        val system = Resources.getSystem().displayMetrics
        // 精华：density 动态 = 真机像素宽 / 393，使「逻辑宽度 = 像素宽 / density」恒等于 393
        val density = system.widthPixels / DESIGN_WIDTH_DP
        return DisplayMetrics().apply {
            this.density = density
            this.scaledDensity = density
            this.densityDpi = (density * DisplayMetrics.DENSITY_DEFAULT).toInt()
            this.widthPixels = system.widthPixels
            this.heightPixels = system.heightPixels
        }
    }
}
```

启用入口（Delegate）：

```kotlin
val delegate = object : KuiklyRenderViewBaseDelegatorDelegate {
    override fun useHostDisplayMetrics(): Boolean = true
}
```

这样无论真机分辨率多少，Kuikly 页面的逻辑宽度都恒为 `393`，与 iOS 按 `393` 实现的界面在尺寸与比例上一一对齐。

> - 设计基准可按团队设计稿调整（如 `375`），三端务必使用同一个值。
> - 上例以全屏宽度（`widthPixels`）为基准；若 Kuikly 容器不是全屏，应改用容器实际宽度参与计算。
> - iOS / HarmonyOS 无 Android 这种由系统「显示大小」改变 density 的机制，按基准设计稿实现即可，无需额外对齐代码。

---

## 二、字号不跟随系统（跨端）

字号缩放与「统一设计宽度」是**两件事**：前者只影响文字大小，后者影响整页布局换算。字号缩放由 Kotlin 侧总开关控制，端侧再各自实现。

### 1. 打开总开关（Kotlin 侧，跨端共用）

在 `Pager` 中重写 `scaleFontSizeEnable()` 返回 `true`，框架才会把文本字号交给端侧处理；默认 `false` 表示字号不做端侧缩放：

```kotlin
override fun scaleFontSizeEnable(): Boolean {
    return true
}
```

### 2. 端侧实现缩放算法

#### Android

在 `IKRFontAdapter` 中重写 `scaleFontSize(fontSize)`，返回最终生效字号。要「不跟随系统字体大小」，原样返回即可：

```kotlin
override fun scaleFontSize(fontSize: Float): Float {
    return fontSize // 不跟随系统字号；按倍率缩放则返回 fontSize * ratio
}
```

#### iOS

实现 `KuiklyFontProtocol` 的 `scaleFitWithFontSize:`，并通过 `registerFontHandler:` 注册：

```objc
- (CGFloat)scaleFitWithFontSize:(CGFloat)fontSize {
    return fontSize; // 不跟随系统字号
}

// 注册
[KuiklyRenderBridge registerFontHandler:[[MyFontHandler alloc] init]];
```

#### HarmonyOS

在页面控制器中重写 `fontSizeScaleFollowSystem()` 返回 `false`（不跟随系统，缩放比例固定为 1）：

```typescript
fontSizeScaleFollowSystem(): boolean {
    return false
}
```

---

## 注意事项

- **两条能力独立**：只想统一布局尺寸就只做「统一设计宽度」；只想锁字号就只做「字号不跟随系统」；可同时启用。
- **务必先接入字体适配器**：Android `getDisplayMetrics` / `scaleFontSize` 都属于 `IKRFontAdapter`，需先注册 `krFontAdapter`；iOS 需 `registerFontHandler:`。详见各端接入文档：[Android 接入](../QuickStart/android.md)、[iOS 接入](../QuickStart/iOS.md)、[HarmonyOS 接入](../QuickStart/harmony.md)。
- **总开关易遗漏**：Android / iOS 即使实现了 `scaleFontSize` / `scaleFitWithFontSize:`，若 `Pager.scaleFontSizeEnable()` 仍为默认 `false`，缩放算法不会被调用。
- **`useHostDisplayMetrics` 易遗漏**：FontAdapter 即使返回了自定义 `DisplayMetrics`，若 Delegate 的 `useHostDisplayMetrics()` 未返回 `true`，框架仍使用系统默认 metrics，统一宽度不生效。
