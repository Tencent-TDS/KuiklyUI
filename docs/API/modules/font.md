# FontModule

字体缩放适配模块，按端侧字体缩放系数换算字号，用于适配系统字体大小设置。

:::tip 说明
多数业务场景无需直接调用本模块——`Text`、`Input`、`TextArea` 的字号设置已内置 `scaleFontSizeEnable` 开关，会自动走该换算链路。仅在需要自行换算字号（如自定义绘制、Canvas 文本）时才直接使用。
:::

## scaleFontSize方法

按端侧字体缩放系数换算字号，内部带结果缓存。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| fontSize <Badge text="必需" type="warn"/> | 原始字号 | Float |

**返回值**

| 类型 | 描述 |
|:----|:-------|
| Float | 换算后的字号；端侧未实现该能力时返回原始值 |

**示例**

```kotlin
val scaled = acquireModule<FontModule>(FontModule.MODULE_NAME).scaleFontSize(16f)
```

## 静态便捷方法

`FontModule.scaleFontSize(fontSize, scaleFontSizeEnable)` 为静态入口，内部取当前 Pager 的模块实例执行换算，无需先获取模块。

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| fontSize <Badge text="必需" type="warn"/> | 原始字号 | Float |
| scaleFontSizeEnable <Badge text="非必需" type="warn"/> | 是否启用换算，不传时取页面的 `scaleFontSizeEnable()` 配置 | Boolean |

::::warning 降级说明
当页面未启用字体缩放（`scaleFontSizeEnable` 为 false）或端侧版本较低（`nativeBuild < 3`）时，该方法直接返回原始字号，不做换算。
::::

**示例**

```kotlin
val scaled = FontModule.scaleFontSize(16f)
```
