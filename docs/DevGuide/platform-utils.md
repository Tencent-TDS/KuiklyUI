# 平台判定工具

Kuikly 的页面数据 `PageData` 提供了 `isIOS`、`isAndroid` 等平台字段，但散落使用容易造成判断逻辑不一致。`PlatformUtils` 把这些判断收敛为统一入口。

## 平台判定

| 方法 | 说明 | 返回类型 |
| -- | -- | -- |
| `PlatformUtils.isIOS()` | 当前是否为 iOS | Boolean |
| `PlatformUtils.isMacOS()` | 当前是否为 macOS | Boolean |
| `PlatformUtils.isIOSOrMacOS()` | 当前是否为 Apple 平台 | Boolean |
| `PlatformUtils.isAndroid()` | 当前是否为 Android | Boolean |
| `PlatformUtils.isOhOs()` | 当前是否为鸿蒙 | Boolean |

::::tip 取值来源
以上方法内部读取当前 Pager 的 `pageData`；在 Pager 上下文之外调用或取值失败时返回 `false`，不会抛异常。
::::

## 平台与系统信息

| 方法 | 说明 | 返回类型 |
| -- | -- | -- |
| `PlatformUtils.getPlatform()` | 当前平台名称，取值失败时返回 `"unknown"` | String |
| `PlatformUtils.getOSVersion()` | 当前系统版本，取值失败时返回空串 | String |

## 能力判定

| 方法 | 说明 | 返回类型 |
| -- | -- | -- |
| `PlatformUtils.isLiquidGlassSupported()` | 当前设备是否支持液态玻璃效果 | Boolean |

`isLiquidGlassSupported()` 仅在 Apple 平台（iOS / macOS）返回可能为 true，并要求系统版本为 **26.0 及以上**；其余平台直接返回 `false`。macOS 的系统版本字符串形如 `Version 26.1 (Build 25B5042k)`，该方法内部已做格式适配。

**示例**

```kotlin
if (PlatformUtils.isIOS()) {
    // iOS 专属逻辑
}

if (PlatformUtils.isLiquidGlassSupported()) {
    // 启用液态玻璃样式
}
```
