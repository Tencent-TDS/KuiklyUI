# 主题与样式

Kuikly Compose 提供了 Material3 主题体系：`MaterialTheme` 作为主题入口，`ColorScheme`、`Typography`、`Shapes` 分别描述配色、字体排印与形状。

## MaterialTheme

`MaterialTheme` 是一个 `@Composable` 函数，作为主题入口包裹页面内容；同时提供同名的 `object MaterialTheme`，用于在 `@Composable` 中读取当前主题值。

**设置主题**

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| colorScheme | 主题配色；必填，以区分现有仅设置 `shapes` 的重载 | ColorScheme |
| typography | 字体排印；默认继承当前主题 | Typography |
| shapes | 形状体系；默认继承当前主题 | Shapes |
| content <Badge text="必需" type="warn"/> | 主题作用域内的内容 | @Composable () -> Unit |

```kotlin
setContent {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF07C160),
        ),
        typography = Typography(),
        shapes = Shapes(),
    ) {
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

传入 `colorScheme` 后，直接读取 `MaterialTheme.colorScheme` 的 Material3 组件会使用对应颜色角色。未显式传入的 `typography` 和 `shapes` 会继承当前主题值。

`Text`、`TextField` 的默认样式仍来自 `LocalTextStyle` 和 `LocalContentColor`，不会仅因传入 `colorScheme` 或 `typography` 自动变化。使用这类组件时，请显式传入 `color`/`style`，或通过 `CompositionLocalProvider` 提供相应默认值。

仅需要覆盖形状时，仍可使用兼容重载：

```kotlin
MaterialTheme(shapes = customShapes) {
    DemoScreen()
}
```

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

`Shapes` 描述形状体系，通过 `MaterialTheme.shapes` 读取。可以在 `MaterialTheme` 中传入自定义 `Shapes`，也可以只在局部作用域覆盖。
