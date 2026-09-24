# 主题与样式

Kuikly Compose 提供了 Material3 主题体系：`MaterialTheme` 作为主题入口，`ColorScheme`、`Typography`、`Shapes` 分别描述配色、字体排印与形状。

## MaterialTheme

`MaterialTheme` 是一个 `@Composable` 函数，用于在组合树中注入主题；同时提供同名的 `object MaterialTheme`，用于在 `@Composable` 中读取当前主题值。

**设置主题**

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| shapes <Badge text="非必需" type="warn"/> | 形状体系 | Shapes |
| content <Badge text="必需" type="warn"/> | 主题作用域内的内容 | @Composable () -> Unit |

```kotlin
setContent {
    MaterialTheme {
        DemoScreen()
    }
}
```

**读取主题值**

| 属性 | 描述 | 类型 |
| -- | -- | -- |
| `MaterialTheme.colorScheme` | 当前配色 | ColorScheme |
| `MaterialTheme.typography` | 当前字体排印 | Typography |
| `MaterialTheme.shapes` | 当前形状体系 | Shapes |

```kotlin
@Composable
fun ThemedText() {
    Text(
        text = "标题",
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleLarge
    )
}
```

:::: warning 当前支持范围
`MaterialTheme()` 函数当前**仅开放 `shapes` 参数**，`colorScheme` 与 `typography` 使用框架默认值，不支持在调用处传入自定义配色与字体排印。

如需自定义颜色或文本样式，请在组件上直接传参，例如 `Text(color = ...)`、`Text(style = ...)`、`Modifier.background(color = ...)`。
::::

## ColorScheme

`ColorScheme` 描述 Material3 配色，字段采用「角色 + 容器」命名（如 `primary` / `onPrimary` / `primaryContainer`）。

常用字段：

| 字段 | 说明 |
| -- | -- |
| `primary` / `onPrimary` / `primaryContainer` / `onPrimaryContainer` | 主色及其前景色、容器色 |
| `secondary` / `onSecondary` / `secondaryContainer` / `onSecondaryContainer` | 辅助色 |
| `tertiary` / `onTertiary` / `tertiaryContainer` / `onTertiaryContainer` | 第三色 |
| `background` / `onBackground` | 背景色 |
| `surface` / `onSurface` / `surfaceVariant` / `onSurfaceVariant` | 表面色 |
| `surfaceContainerLow` / `surfaceContainer` / `surfaceContainerHigh` / `surfaceContainerHighest` / `surfaceContainerLowest` | 分级表面容器色 |
| `surfaceBright` / `surfaceDim` | 明暗表面色 |
| `error` / `onError` / `errorContainer` / `onErrorContainer` | 错误色 |
| `outline` / `outlineVariant` | 描边色 |
| `inverseSurface` / `inverseOnSurface` / `inversePrimary` | 反色 |
| `scrim` / `surfaceTint` | 遮罩色 / 表面着色 |

::: tip
完整字段列表以源码为准。
:::

## Typography

`Typography` 描述字体排印，字段按 Material3 的类型比例划分：

| 分类 | 字段 |
| -- | -- |
| Display | `displayLarge`、`displayMedium`、`displaySmall` |
| Headline | `headlineLarge`、`headlineMedium`、`headlineSmall` |
| Title | `titleLarge`、`titleMedium`、`titleSmall` |
| Body | `bodyLarge`、`bodyMedium`、`bodySmall` |
| Label | `labelLarge`、`labelMedium`、`labelSmall` |

## Shapes

`Shapes` 描述形状体系，`MaterialTheme()` 当前支持传入该参数以覆盖默认形状。
