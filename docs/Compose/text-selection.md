# 文本部分选中

`SelectionContainer`允许其内部的`Text`等文本组件被跨节点连续选中，类似系统原生交互，常用于AI聊天、富文本复制/分享等场景。

支持的平台：Android、iOS、鸿蒙。

> 示例：[TextSelectionDemo.kt](https://github.com/Tencent-TDS/KuiklyUI/tree/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TextSelectionDemo.kt) ｜ [TextSelectionGestureDemo.kt](https://github.com/Tencent-TDS/KuiklyUI/tree/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TextSelectionGestureDemo.kt)

:::tip 关于弹出菜单
渲染层只负责选区绘制、游标交互与放大镜效果。复制 / 分享 / 全选等弹出菜单由业务侧自行实现，可在`onSelectStart` / `onSelectEnd`回调里基于选区位置渲染菜单。
:::

## 基本用法

```kotlin
import androidx.compose.runtime.CompositionLocalProvider
import com.tencent.kuikly.compose.foundation.text.selection.LocalTextSelectionColors
import com.tencent.kuikly.compose.foundation.text.selection.TextSelectionColors
import com.tencent.kuikly.compose.text.SelectionContainer
import com.tencent.kuikly.compose.text.rememberSelectionContainerState

@Composable
fun BasicSelection() {
    val state = rememberSelectionContainerState()
    val customSelectionColors = TextSelectionColors(
        handleColor = Color(0xFF2196F3),
        backgroundColor = Color(0xFF2196F3).copy(alpha = 0.4f)
    )
    CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
        SelectionContainer(
            state = state,
            modifier = Modifier.padding(16.dp),
        ) {
            Column {
                Text("Kuikly支持跨多个文本组件的部分选中。")
                Text("文本选择允许业务复制、分享部分内容。")
            }
        }
    }
}
```

### 参数说明

| 参数 | 描述 | 类型 |
|:----|:-------|:--|
| state | 必填。选区状态对象，用于程序化创建/清除选区，由`rememberSelectionContainerState()`创建 | SelectionContainerState |
| modifier | 容器`Modifier` | Modifier |
| onSelectStart | 进入选中态回调，参数为选区矩形 | ((SelectionFrame) -> Unit)? |
| onSelectChange | 选区位置或大小变化回调 | ((SelectionFrame) -> Unit)? |
| onSelectEnd | 选区交互结束回调，常用于显示弹出菜单 | ((SelectionFrame) -> Unit)? |
| onSelectCancel | 退出选中态回调，无参数 | (() -> Unit)? |
| content | 容器内容 | @Composable BoxScope.() -> Unit |

### 自定义选区颜色

选区颜色通过 `LocalTextSelectionColors` CompositionLocal 提供，而非直接传参。在 `SelectionContainer` 的上层使用 `CompositionLocalProvider` 即可自定义：

```kotlin
val customSelectionColors = TextSelectionColors(
    handleColor = Color(0xFF2196F3),           // 游标颜色
    backgroundColor = Color(0xFF2196F3).copy(alpha = 0.4f)  // 选区底色
)
CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
    SelectionContainer(state = state) { /* ... */ }
}
```

如果不提供，将使用 `LocalTextSelectionColors` 的默认值。

`SelectionFrame`字段为 `x` / `y` / `width` / `height`，单位为px，相对于容器左上角。

## 禁用部分文本的选中

使用`DisableSelection`包裹的内容不参与选中，即使它位于`SelectionContainer`内部。

```kotlin
import com.tencent.kuikly.compose.text.DisableSelection

SelectionContainer(state = rememberSelectionContainerState()) {
    Column {
        Text("可选中文本")
        DisableSelection {
            Text("此段不可选中")
        }
        Text("继续可选中")
    }
}
```

## 程序化控制选区

通过`SelectionContainerState`可在业务侧主动创建选区、获取选中文本或清除选区。

```kotlin
val state = rememberSelectionContainerState()

// 在指定坐标创建选区
state.createSelection(x = 120f, y = 80f, type = TextSelectionType.WORD)

// 全选容器内文本
state.createSelectionAll()

// 异步获取选中文本
state.getSelection { result ->
    val selected = result.joinToString("")          // result 实现了 List<String>
    val pre = result.preContent.joinToString("")
    val post = result.postContent.joinToString("")
}

// 清除选区
state.clearSelection()
```

### SelectionContainerState

| 成员 | 描述 | 类型 |
|:----|:-------|:--|
| selectionFrame | 当前选区矩形（px），可作为可观察状态读取 | SelectionFrame |
| createSelection(x, y, type) | 在指定像素坐标创建选区 | (Float, Float, TextSelectionType) -> Unit |
| createSelectionAll() | 全选容器内所有可选文本 | () -> Unit |
| getSelection(callback) | 异步获取选中内容 | ((SelectionResult) -> Unit) -> Unit |
| clearSelection() | 清除当前选区 | () -> Unit |

### TextSelectionType

| 枚举值 | 说明 |
|:----|:----|
| `CHARACTER` | 单字符 |
| `WORD` | 词 |
| `PARAGRAPH` | 段落 |
| `SENTENCE` | 句子（仅iOS生效，Android、鸿蒙端会回退为`PARAGRAPH`） |

### SelectionResult

`getSelection`回调返回的结果，本身实现了`List<String>`接口，可直接当作选中文本数组使用。

| 字段 | 描述 | 类型 |
|:----|:-------|:--|
| content | 选区内文本，按阅读顺序（先y后x）排列 | List&lt;String&gt; |
| preContent | 选区起点之前的若干段文本（含选区起点所在`Text`节点的前半段） | List&lt;String&gt; |
| postContent | 选区终点之后的若干段文本（含选区终点所在`Text`节点的后半段） | List&lt;String&gt; |

> 约定：首个`content`元素与最后一个`preContent`元素属于同一`Text`节点；选区刚好覆盖整个`Text`时，对应的`preContent`末尾元素或`postContent`首元素为`""`；剩余`Text`节点不足时，会填入全部剩余节点。

## 配合手势与弹出菜单

`SelectionContainer`本身不带触发手势，可结合`pointerInput` + `detectTapGestures`，由业务自定义"长按触发选择 + 弹出菜单"的体验。

```kotlin
val state = rememberSelectionContainerState()
var showMenu by remember { mutableStateOf(false) }
var menuFrame by remember { mutableStateOf(SelectionFrame.Zero) }

SelectionContainer(
    state = state,
    modifier = Modifier
        .fillMaxWidth()
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = { offset ->
                    state.createSelection(offset.x, offset.y, TextSelectionType.WORD)
                },
                onTap = {
                    if (showMenu) {
                        showMenu = false
                        state.clearSelection()
                    }
                }
            )
        },
    onSelectStart = { frame ->
        menuFrame = frame
        showMenu = true
    },
    onSelectChange = { showMenu = false },
    onSelectEnd = { frame ->
        menuFrame = frame
        showMenu = true
    },
    onSelectCancel = { showMenu = false },
) {
    Column { /* Text(...) */ }
}

if (showMenu) {
    // 业务自行渲染浮层菜单，位置基于 menuFrame 计算
}
```

完整示例参见 [TextSelectionGestureDemo.kt](https://github.com/Tencent-TDS/KuiklyUI/tree/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/TextSelectionGestureDemo.kt)。

## 注意事项

- 容器内的可滚动组件发生滚动时，渲染层会自动刷新游标位置。
- 容器`Bounds`大小变化时（如旋转、键盘弹起），会自动取消当前选区并触发`onSelectCancel`。
- `LazyColumn` / `LazyRow`等列表内的可选文本，进入选中态后会被临时全局保活，退出选区后自动恢复。
- 不同平台在UI细节上存在差异：iOS暂未实现放大镜液态玻璃效果；鸿蒙在跨多个高度不一致的`Text`节点时，选区背景范围可能与系统略有差异。
