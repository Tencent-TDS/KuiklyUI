# 补充组件

本篇收录核心组件之外、同样可直接使用但尚未在其他章节展开的组件：卡片、底部导航、浮层与返回键拦截。

## 卡片

### Card

填充式卡片，内容区为 `ColumnScope`。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| modifier <Badge text="非必需" type="warn"/> | 修饰符 | Modifier |
| shape <Badge text="非必需" type="warn"/> | 卡片形状 | Shape |
| colors <Badge text="非必需" type="warn"/> | 各状态下的配色 | CardColors |
| elevation <Badge text="非必需" type="warn"/> | 各状态下的阴影高度 | CardElevation |
| border <Badge text="非必需" type="warn"/> | 卡片边框 | BorderStroke |
| content <Badge text="必需" type="warn"/> | 卡片内容 | @Composable ColumnScope.() -> Unit |

### ElevatedCard

带阴影的卡片，参数与 `Card` 一致，默认阴影更高。

### OutlinedCard

描边式卡片，参数与 `Card` 一致，默认带边框、无阴影。

**示例**

```kotlin
Card(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    shape = RoundedCornerShape(12.dp)
) {
    Text(
        text = "卡片标题",
        modifier = Modifier.padding(16.dp)
    )
}
```

## 底部导航

### NavigationBar

底部导航栏，内容区为 `RowScope`，通常放置 3~5 个 `NavigationBarItem`。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| modifier <Badge text="非必需" type="warn"/> | 修饰符 | Modifier |
| containerColor <Badge text="非必需" type="warn"/> | 容器背景色 | Color |
| contentColor <Badge text="非必需" type="warn"/> | 内容首选色 | Color |
| tonalElevation <Badge text="非必需" type="warn"/> | 色调高度 | Dp |
| windowInsets <Badge text="非必需" type="warn"/> | 窗口内边距 | WindowInsets |
| content <Badge text="必需" type="warn"/> | 导航项内容 | @Composable RowScope.() -> Unit |

### NavigationBarItem

底部导航项，只能在 `NavigationBar` 的 `RowScope` 中调用。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| selected <Badge text="必需" type="warn"/> | 是否选中 | Boolean |
| onClick <Badge text="必需" type="warn"/> | 点击回调 | () -> Unit |
| icon <Badge text="必需" type="warn"/> | 图标内容 | @Composable () -> Unit |
| modifier <Badge text="非必需" type="warn"/> | 修饰符 | Modifier |
| enabled <Badge text="非必需" type="warn"/> | 是否可交互 | Boolean |
| label <Badge text="非必需" type="warn"/> | 文本标签 | @Composable (() -> Unit)? |
| alwaysShowLabel <Badge text="非必需" type="warn"/> | 是否始终显示标签，false 时仅选中态显示 | Boolean |
| colors <Badge text="非必需" type="warn"/> | 各状态下的配色 | NavigationBarItemColors |
| interactionSource <Badge text="非必需" type="warn"/> | 交互源 | MutableInteractionSource |

:::: warning 关于图标
Kuikly Compose 当前**未提供 `Icon` 组件**，`icon` 参数需要自行用 `Image`、`Canvas` 或 `Text` 组合实现。
::::

**示例**

```kotlin
NavigationBar {
    NavigationBarItem(
        selected = selectedIndex == 0,
        onClick = { selectedIndex = 0 },
        icon = { Text("首页") }
    )
    NavigationBarItem(
        selected = selectedIndex == 1,
        onClick = { selectedIndex = 1 },
        icon = { Text("我的") }
    )
}
```

## Popup

浮层容器，相对父组件按 `alignment` 与 `offset` 定位，只要仍在组合树中就保持可见。适用于非模态的浮动菜单等场景。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| alignment <Badge text="必需" type="warn"/> | 相对父组件的对齐方式 | Alignment |
| offset <Badge text="非必需" type="warn"/> | 基于对齐位置的偏移 | IntOffset |
| onDismissRequest <Badge text="必需" type="warn"/> | 点击浮层外部时的回调 | (() -> Unit)? |
| properties <Badge text="必需" type="warn"/> | 浮层行为配置 | PopupProperties |
| content <Badge text="必需" type="warn"/> | 浮层内容 | @Composable () -> Unit |

## BackHandler

拦截系统返回键，在页面内接管返回逻辑。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| onBack <Badge text="必需" type="warn"/> | 返回键触发回调 | () -> Unit |

```kotlin
BackHandler {
    // 返回键按下，自行处理（如关闭弹层、返回上一级）
}
```

## 三态复选框

### TriStateCheckbox

支持选中、未选中、部分选中三种状态。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| state <Badge text="必需" type="warn"/> | 三态值 | ToggleableState |
| onClick <Badge text="必需" type="warn"/> | 点击回调，传 null 表示不响应点击 | (() -> Unit)? |
| modifier <Badge text="非必需" type="warn"/> | 修饰符 | Modifier |
| enabled <Badge text="非必需" type="warn"/> | 是否可交互 | Boolean |
| colors <Badge text="非必需" type="warn"/> | 各状态下的配色 | CheckboxColors |
| interactionSource <Badge text="非必需" type="warn"/> | 交互源 | MutableInteractionSource |

## 标签行

### LeadingIconTab

带前置图标的标签，配合 `TabRow` 系列使用。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| selected <Badge text="必需" type="warn"/> | 是否选中 | Boolean |
| onClick <Badge text="必需" type="warn"/> | 点击回调 | () -> Unit |
| text <Badge text="必需" type="warn"/> | 文本内容 | @Composable () -> Unit |
| icon <Badge text="必需" type="warn"/> | 图标内容 | @Composable () -> Unit |
| modifier <Badge text="非必需" type="warn"/> | 修饰符 | Modifier |
| enabled <Badge text="非必需" type="warn"/> | 是否可交互 | Boolean |
| selectedContentColor <Badge text="非必需" type="warn"/> | 选中态内容色 | Color |
| unselectedContentColor <Badge text="非必需" type="warn"/> | 未选中态内容色 | Color |
| interactionSource <Badge text="非必需" type="warn"/> | 交互源 | MutableInteractionSource |

### PrimaryTabRow / SecondaryTabRow

Material3 的主 / 次级标签行，`Primary` 用于页面级切换，`Secondary` 用于内容区内二级切换。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| selectedTabIndex <Badge text="必需" type="warn"/> | 当前选中下标 | Int |
| modifier <Badge text="非必需" type="warn"/> | 修饰符 | Modifier |
| containerColor <Badge text="非必需" type="warn"/> | 容器背景色 | Color |
| contentColor <Badge text="非必需" type="warn"/> | 内容首选色 | Color |
| indicator <Badge text="非必需" type="warn"/> | 指示器 | @Composable TabIndicatorScope.() -> Unit |
| divider <Badge text="非必需" type="warn"/> | 底部分割线 | @Composable () -> Unit |
| tabs <Badge text="必需" type="warn"/> | 标签内容，通常为多个 `Tab` | @Composable () -> Unit |

::::warning 实验性 API
`PrimaryTabRow` 与 `SecondaryTabRow` 标注 `@ExperimentalMaterial3Api`，使用前需要 opt-in。
::::

**示例**

```kotlin
PrimaryTabRow(selectedTabIndex = selectedIndex) {
    Tab(
        selected = selectedIndex == 0,
        onClick = { selectedIndex = 0 },
        text = { Text("推荐") }
    )
    Tab(
        selected = selectedIndex == 1,
        onClick = { selectedIndex = 1 },
        text = { Text("关注") }
    )
}
```
