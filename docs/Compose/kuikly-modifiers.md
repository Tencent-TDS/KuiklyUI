# Kuikly 扩展 Modifier

本篇收录 Kuikly Compose 相对标准 Compose 额外提供的 Modifier，主要用于把能力透传到底层 Kuikly 组件。

## 滚动与布局

### Modifier.bouncesEnable

控制滚动容器是否允许回弹效果。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| enable | 是否开启回弹 | Boolean |

### Modifier.fixScrollOffset

修正滚动容器在特定场景下的内容偏移。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| needFix | 是否启用偏移修正 | Boolean |

## LazyList 预取

### Modifier.enableLazyListPrefetch <Badge text="2.23.3 及以上支持" type="warn"/>

为单个 `LazyColumn` / `LazyRow` 覆盖预取开关，开启后会提前组合滚动方向上的后续 item。未设置时使用全局开关：runtime19 产物全局默认已开启，此时该 Modifier 主要用于传 `false` 关闭；legacy 产物不支持预取，该 Modifier 不生效。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| enabled | 是否开启预取，默认 `true` | Boolean |

## 文本输入

### Modifier.autoFocusOnTextInputState <Badge text="仅 iOS" type="warn"/>

控制程序化同步 `TextFieldValue`（原生 `setTextInputState`）时，非空文本是否自动获取焦点并弹起键盘。不设置时默认为 `false`，即进页带预填文本时不自动弹键盘；需要自动聚焦时显式传 `true`。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| enabled | 是否自动获取焦点 | Boolean |

## 属性与事件透传

### Modifier.setProp

向底层 Kuikly 组件透传属性。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| key <Badge text="必需" type="warn"/> | 属性键名，如 `"alpha"` | String |
| value <Badge text="必需" type="warn"/> | 属性值，支持任意类型 | Any |

::::tip 适用场景
当某个 Kuikly 组件属性尚无对应的 Compose API 时，可用 `setProp` 直接透传。使用前请确认键名与端侧约定一致。
::::

### Modifier.setEvent

向底层 Kuikly 组件透传事件。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| key <Badge text="必需" type="warn"/> | 事件键名 | String |
| value <Badge text="必需" type="warn"/> | 事件处理器 | EventHandlerFn |

## 滚动控制

### Modifier.flingEnable <Badge text="2.23.0 及以上支持" type="warn"/>

动态开关底层 Kuikly `ScrollerView` 的原生惯性滚动。

::::tip 平台说明
Android、iOS、鸿蒙支持；Web 不支持。
::::

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| enable <Badge text="必需" type="warn"/> | 是否开启惯性滚动 | Boolean |

### Modifier.flingSpeedLimit

限制最大初始惯性速度。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| speedLimit <Badge text="必需" type="warn"/> | 速度上限，单位 vp/s；传 ≤ 0 恢复系统默认 | Float |

::::warning 平台差异
`flingSpeedLimit` 仅在**鸿蒙 API 18 及以上**生效，低于该版本为 no-op。
::::

## 液态玻璃（仅 iOS / macOS）

### Modifier.liquidGlass

为组件应用原生液态玻璃效果。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| style <Badge text="非必需" type="warn"/> | 玻璃样式，支持 `LiquidGlassStyle.REGULAR`、`LiquidGlassStyle.CLEAR` | LiquidGlassStyle |
| tintColor <Badge text="非必需" type="warn"/> | 玻璃着色，默认 null | Color? |
| interactive <Badge text="非必需" type="warn"/> | 是否启用交互效果 | Boolean |
| enable <Badge text="非必需" type="warn"/> | 是否启用，传 false 时不添加该效果 | Boolean |

### Modifier.liquidGlassContainer

创建液态玻璃容器，使邻近的玻璃组件之间产生融合过渡效果。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| spacing <Badge text="必需" type="warn"/> | 组件间距，用于控制融合强度，传负值会被纠正为 0 | Float |

::::warning 平台差异
两个 Modifier 均**仅在 iOS 26.0+ / macOS 26.0+ 生效**，其余平台与系统版本自动降级为透明效果，不会报错。

使用 `PlatformUtils.isLiquidGlassSupported()` 可在运行时判断当前系统是否支持（iOS 26.0+ / macOS 26.0+）。
::::

**示例**

```kotlin
Box(
    modifier = Modifier
        .size(200.dp)
        .liquidGlass(
            style = LiquidGlassStyle.CLEAR,
            tintColor = Color.Blue,
            interactive = true
        )
) {
    Text("玻璃效果内容")
}
```

## 已废弃

| Modifier | 状态 | 替代方案 |
| -- | -- | -- |
| `Modifier.visibility(visible: Boolean)` | <Badge text="已废弃" type="danger"/> | 使用变量配合 if/else 控制显示 |

::::danger 废弃说明
`Modifier.visibility` 源码已标注 `@Deprecated`，提示「已废弃，请使用变量 If Else 控制显示」。新代码请勿使用。
::::
