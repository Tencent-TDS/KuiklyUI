# 文本部分选中(SelectionContainer)

`View`组件提供了跨多个文本组件的部分选中能力，类似系统原生交互，常用于AI聊天、富文本复制/分享等场景。任意`View`容器都可以通过`selectable`属性开启选中能力，容器内的`Text`、`RichText`等文本组件即可参与连续选中。

支持的平台：Android、iOS、鸿蒙。

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/tree/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/kit_demo/DeclarativeDemo/TextSelectionExamplePage.kt)

:::tip 关于弹出菜单
渲染层只负责选中区域绘制、游标交互、放大镜等显示效果。复制/分享等弹出菜单由业务侧自行实现，可结合`selectStart` / `selectEnd`事件回调的选区位置进行布局。
:::

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)，并支持以下选中相关属性：

### selectable方法

设置容器是否启用文本选中。开启后，容器内的所有`Text` / `RichText`等文本节点都会被视为可选中文本，按渲染位置（先y后x）参与连续选区。

<div class="table-01">

**selectable方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| option | 容器选中态。`INHERIT`继承父容器，`ENABLE`启用，`DISABLE`禁用 | SelectableOption |

</div>

`SelectableOption`枚举：

| 枚举值 | 说明 |
|:----|:----|
| `INHERIT` | 跟随父容器（默认） |
| `ENABLE` | 启用文本选中 |
| `DISABLE` | 禁用文本选中。可在已开启选中的容器内的子`View`上设置，用于排除特定区域 |

### selectionColor方法

设置选区高亮颜色。该方法会根据传入颜色派生出选区底色（带透明度）与游标颜色，使两者保持一致。

<div class="table-01">

**selectionColor方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| color | 选区颜色 | Color |

</div>

## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)，并支持以下选中相关事件：

`selectStart`、`selectChange`、`selectEnd`回调中均带有`Frame`类型参数，描述当前选区的位置（单位dp，相对于容器左上角）。

<div class="table-01">

**Frame**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| x | 选区左上角x坐标 | Float |
| y | 选区左上角y坐标 | Float |
| width | 选区宽度 | Float |
| height | 选区高度 | Float |

</div>

### selectStart

进入选中态时触发。常用于业务侧弹出菜单或开始记录选区位置。

### selectChange

选区位置或大小发生变化时触发，例如游标拖动、容器内滚动导致的选区位置刷新。

### selectEnd

游标拖动等交互结束、选区稳定时触发。常用于显示弹出菜单。

### selectCancel

退出选中态时触发，无参数。常见触发原因：用户点击空白处、容器尺寸变化、调用`clearSelection()`等。

## 方法

### createSelection

在指定坐标处创建选区。常配合`longPress`等手势使用，由业务自定义触发时机。

<div class="table-01">

**createSelection方法**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| x | 触发点x坐标（相对于容器左上角，dp） | Float |
| y | 触发点y坐标（相对于容器左上角，dp） | Float |
| type | 选区类型，参见`SelectionType` | SelectionType |

</div>

`SelectionType`枚举：

| 枚举值 | 说明 |
|:----|:----|
| `CHARACTER` | 选中触发点处的单个字符 |
| `WORD` | 选中触发点所在的词 |
| `PARAGRAPH` | 选中触发点所在的段落 |
| `SENTENCE`<Badge text="仅iOS" type="warn"/> | 选中触发点所在的句子。Android、鸿蒙端会回退为`PARAGRAPH` |

### createSelectionAll

全选容器内所有可选文本。

### getSelection

异步获取当前选中的文本内容。回调参数`SelectionResult`字段如下：

<div class="table-01">

**SelectionResult**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| content | 选区内的文本数组，按阅读顺序（先y后x）排列。`SelectionResult`同时实现了`List<String>`接口，可直接当作`List<String>`使用 | List&lt;String&gt; |
| preContent | 选区起点之前的若干段文本（含选区起点所在`Text`的前半段） | List&lt;String&gt; |
| postContent | 选区终点之后的若干段文本（含选区终点所在`Text`的后半段） | List&lt;String&gt; |

</div>

:::tip preContent / postContent 使用约定
- 首个`content`元素与最后一个`preContent`元素属于同一`Text`节点；末尾`content`与首个`postContent`同理。
- 选区刚好覆盖整个`Text`节点时，对应的`preContent`末尾元素或`postContent`首元素为`""`。
- 选区前/后剩余`Text`节点不足时，会填入全部剩余节点。
:::

### clearSelection

清除当前选区，同时触发`selectCancel`事件。

## 完整示例

```kotlin
@Page("TextSelectionExamplePage")
internal class TextSelectionExamplePage : BasePager() {

    private var selectedText by observable("")
    private var selectableContainer: ViewRef<DivView>? = null

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { flexDirectionColumn() }

            View {
                attr { padding(top = 12f, bottom = 12f, left = 16f, right = 16f) }
                event {
                    click {
                        ctx.selectableContainer?.view?.getSelection { result ->
                            ctx.selectedText = result.joinToString(" | ")
                        }
                    }
                }
                Text { attr { text("获取选中内容: ${ctx.selectedText}") } }
            }

            View {
                ref { ctx.selectableContainer = it }
                attr {
                    padding(all = 16f)
                    selectable(SelectableOption.ENABLE)
                    selectionColor(Color(0xFF2196F3))
                }
                event {
                    longPress {
                        if (it.state == "start") {
                            ctx.selectableContainer?.view
                                ?.createSelection(it.x, it.y, SelectionType.WORD)
                        }
                    }
                    selectEnd { frame ->
                        KLog.i("TextSelection", "selectEnd: $frame")
                    }
                }

                Text {
                    attr {
                        fontSize(15f)
                        text("Kuikly是一个跨平台的UI框架，支持iOS、Android、鸿蒙等多端。")
                    }
                }
                Text {
                    attr {
                        fontSize(15f)
                        text("文本选择功能允许用户跨多个文本组件进行连续选择。")
                    }
                }
            }
        }
    }
}
```

## 注意事项

- 容器内若包含可滚动组件，渲染层会自动监听其滚动并刷新游标位置。
- 容器`frame`大小发生变化时（如旋转、键盘弹起），会自动取消当前选区。
- `List`等列表内的可选文本，进入选中态后会被临时全局保活（关闭`keepAlive`），退出选区后自动恢复。
- 不同平台在UI细节上存在差异：iOS暂未实现放大镜液态玻璃效果；鸿蒙在跨多个高度不一致的`Text`节点时，选区背景范围可能与系统略有差异。
