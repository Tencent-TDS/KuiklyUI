# 文本尺寸测量

本节介绍如何通过 `TextShadow` 测量文本尺寸，以便在某些特殊场景下获取文本的宽高信息。

:::warning 性能提示
**大部分场景下，你不需要手动测量文本尺寸。** 文本测量需要通过 Bridge 调用原生端排版引擎进行计算，存在较大的性能开销。每次测量都涉及跨端通信和原生文本排版计算，如果在列表滚动、高频刷新等场景中频繁调用，会严重影响页面性能。

请优先考虑使用 **FlexBox 布局**（如居中、弹性伸缩）和**绝对定位**来实现你的 UI 效果。只有在确实无法通过布局手段实现时，才考虑使用文本测量。
:::

## 大多数时候你不需要测量文本

在使用 `TextShadow` 之前，先看看以下两个常见场景。很多开发者以为需要测量文本才能实现，但实际上通过布局属性就可以轻松解决。

### 场景一：文本居中显示

**❌ 错误做法**：先测量文本宽高，再手动计算偏移量来居中。

**✅ 正确做法**：直接使用 FlexBox 的对齐属性。

```kotlin
@Page("center_text_demo")
internal class CenterTextDemoPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            // 方式1：容器整体居中
            View {
                attr {
                    width(300f)
                    height(200f)
                    backgroundColor(Color.WHITE)
                    allCenter() // 一行代码搞定水平+垂直居中
                }
                Text {
                    attr {
                        text("我是居中文本")
                        fontSize(16f)
                        color(Color.BLACK)
                    }
                }
            }

            // 方式2：只水平居中，文本靠上
            View {
                attr {
                    width(300f)
                    height(200f)
                    backgroundColor(Color(0xFFF5F5F5L))
                    alignItems(FlexAlign.CENTER) // 水平居中
                    paddingTop(20f)
                }
                Text {
                    attr {
                        text("水平居中的标题")
                        fontSize(18f)
                        fontWeightBold()
                        color(Color.BLACK)
                    }
                }
                Text {
                    attr {
                        text("这是一段副标题文字")
                        fontSize(14f)
                        color(Color(0xFF999999L))
                        marginTop(8f)
                    }
                }
            }
        }
    }
}
```

无论文本内容多长，FlexBox 都能自动处理对齐，无需知道文本的具体尺寸。

### 场景二：背景与文本等大

**❌ 错误做法**：先测量文本尺寸，再创建一个同样大小的背景 View。

**✅ 正确做法**：使用绝对定位让背景自动撑满父容器，而父容器的大小由文本内容自动决定。

```kotlin
@Page("text_bg_demo")
internal class TextBgDemoPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }
            // 标签样式：背景自适应文本内容大小
            View {
                attr {
                    // 不设固定宽高，由子内容撑开
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingTop(6f)
                    paddingBottom(6f)
                }
                // 背景层：绝对定位铺满父容器
                View {
                    attr {
                        absolutePosition(0f, 0f, 0f, 0f) // 上、左、下、右均为0，自动与父容器等大
                        backgroundColor(Color(0xFF1890FFL))
                        borderRadius(16f)
                    }
                }
                // 文本层：正常流布局，决定父容器大小
                Text {
                    attr {
                        text("自适应标签")
                        fontSize(14f)
                        color(Color.WHITE)
                    }
                }
            }

            // 更复杂的例子：带图标的按钮，背景自适应
            View {
                attr {
                    marginTop(20f)
                    flexDirection(FlexDirection.ROW)
                    alignItems(FlexAlign.CENTER)
                    paddingLeft(16f)
                    paddingRight(16f)
                    paddingTop(10f)
                    paddingBottom(10f)
                }
                // 渐变背景层
                View {
                    attr {
                        absolutePosition(0f, 0f, 0f, 0f)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFF667eeaL), 0f),
                            ColorStop(Color(0xFF764ba2L), 1f)
                        )
                        borderRadius(20f)
                    }
                }
                // 内容层
                Text {
                    attr {
                        text("🚀")
                        fontSize(16f)
                    }
                }
                Text {
                    attr {
                        text("开始使用")
                        fontSize(15f)
                        fontWeightMedium()
                        color(Color.WHITE)
                        marginLeft(6f)
                    }
                }
            }
        }
    }
}
```

核心技巧是：
- 父容器**不设固定宽高**，让内容自然撑开
- 背景 View 使用 `absolutePosition(0f, 0f, 0f, 0f)` 铺满父容器
- 通过 `padding` 控制文本与背景边缘的间距

这种方式不仅代码简洁，而且性能零开销，是最推荐的做法。

## 使用 TextShadow 测量文本

当你确实需要获取文本渲染后的精确尺寸时（例如：实现文字截断展开/收起逻辑、Canvas 绘制文本定位、自定义文本排版等），可以通过 `TextShadow` 进行测量。

### 原理简介

`TextShadow` 是 Kuikly 内部用于文本布局计算的核心类。它通过 Bridge 在原生端创建一个对应的文本排版对象（iOS 上使用 CoreText，Android 上使用 StaticLayout，鸿蒙上使用 OH_Drawing_Typography），将文本属性传递给原生排版引擎，计算出文本在给定约束宽高下的实际渲染尺寸。

### 基本用法

```kotlin
import com.tencent.kuikly.core.views.shadow.TextShadow
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.Size
import com.tencent.kuikly.core.views.TextConst
import com.tencent.kuikly.core.views.FontWeight

/**
 * 测量文本尺寸
 * @param pagerId 当前页面ID
 * @param nativeRef 当前视图的 nativeRef
 * @param text 要测量的文本
 * @param fontSize 字体大小
 * @param fontWeight 字体粗细
 * @param maxWidth 最大宽度约束（不限制宽度则传一个极大值）
 * @param maxHeight 最大高度约束（不限制高度则传一个极大值）
 * @return 文本的渲染尺寸 Size(width, height)
 */
fun measureTextSize(
    pagerId: String,
    nativeRef: Int,
    text: String,
    fontSize: Float,
    fontWeight: FontWeight = FontWeight.NORMAL,
    maxWidth: Float = 100000f,
    maxHeight: Float = 100000f
): Size {
    // 1. 创建 TextShadow 实例
    val shadow = TextShadow(pagerId, nativeRef, ViewConst.TYPE_RICH_TEXT)
    
    // 2. 设置文本属性（需要与实际显示的 Text 组件属性一致）
    shadow.setProp(TextConst.VALUE, text)           // 文本内容
    shadow.setProp(TextConst.FONT_SIZE, fontSize)    // 字号
    shadow.setProp(TextConst.FONT_WEIGHT, fontWeight.value) // 字重
    shadow.setProp(TextConst.TEXT_USE_DP_FONT_SIZE_DIM, 1)  // 使用 dp 字号
    
    // 3. 计算文本渲染尺寸
    val size = shadow.calculateRenderViewSize(maxWidth, maxHeight)
    
    // 4. 销毁 Shadow 释放原生资源（重要！）
    shadow.removeFromParentComponent()
    
    return size // size.width = 文本宽度, size.height = 文本高度
}
```

### 完整示例：展开/收起文本

以下是一个常见的实际应用场景——文本超过指定行数时显示"展开"按钮：

```kotlin
@Page("expand_text_demo")
internal class ExpandTextDemoPage : BasePager() {

    companion object {
        private val longText = "Kuikly 是一个基于 Kotlin Multiplatform 的高性能跨端 UI 框架，" +
                "支持 Android、iOS、鸿蒙、Web 和微信小程序等多个平台。" +
                "它采用声明式 DSL 语法，结合原生渲染引擎，提供接近原生的性能体验。" +
                "开发者只需编写一份 Kotlin 代码，即可在多个平台上运行。"

        private val maxLines = 2
        private val textFontSize = 14f
        private val containerWidth = 300f
    }

    private var isExpanded by observable(false)
    private var needExpand by observable(false) // 文本是否超出限制
    
    override fun created() {
        super.created()
        // 在页面创建后测量文本，判断是否需要展开按钮
        checkNeedExpand()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
                backgroundColor(Color(0xFFF5F5F5L))
            }

            View {
                attr {
                    width(containerWidth)
                    backgroundColor(Color.WHITE)
                    borderRadius(8f)
                    padding(16f)
                }
                Text {
                    attr {
                        text(longText)
                        fontSize(textFontSize)
                        color(Color(0xFF333333L))
                        lineHeight(20f)
                        if (!ctx.isExpanded) {
                            lines(maxLines) // 限制行数
                        } else {
                            lines(10000) // 展开时不限制行数
                        }
                    }
                }

                if (ctx.needExpand) {
                    Text {
                        attr {
                            text(if (ctx.isExpanded) "收起" else "展开")
                            fontSize(textFontSize)
                            color(Color(0xFF1890FFL))
                            marginTop(4f)
                        }
                        event {
                            click {
                                ctx.isExpanded = !ctx.isExpanded
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkNeedExpand() {
        val shadow = TextShadow(pagerId, nativeRef, ViewConst.TYPE_RICH_TEXT)
        shadow.setProp(TextConst.VALUE, longText)
        shadow.setProp(TextConst.FONT_SIZE, textFontSize)
        shadow.setProp("lineHeight", 20f)
        shadow.setProp(TextConst.TEXT_USE_DP_FONT_SIZE_DIM, 1)

        // 不限制行数，计算文本完整渲染的高度
        val fullSize = shadow.calculateRenderViewSize(containerWidth - 32f, 100000f)
        shadow.removeFromParentComponent()

        // 计算限制行数时的高度（lineHeight * maxLines）
        val collapsedHeight = 20f * maxLines

        // 如果完整高度超过折叠高度，说明需要展开按钮
        needExpand = fullSize.height > collapsedHeight
    }
}
```

### 注意事项

1. **必须释放资源**：调用 `calculateRenderViewSize` 后，务必调用 `shadow.removeFromParentComponent()` 释放原生端资源，否则会造成内存泄漏。

2. **属性必须一致**：TextShadow 设置的属性（字号、字重、行高等）必须与实际 `Text` 组件的属性保持一致，否则测量结果将不准确。

3. **约束宽度的影响**：`calculateRenderViewSize(width, height)` 中的 `width` 参数是文本排版的最大宽度约束。如果文本不需要换行，传入一个极大值（如 `100000f`）即可获取单行文本的自然宽度。如果需要计算在特定宽度下的多行文本高度，则传入实际的容器宽度。

4. **避免频繁调用**：不要在 `attr {}` 或 `body()` 闭包中调用文本测量，因为这些闭包会在每次状态更新时重新执行。建议在 `created()` 生命周期或事件回调中进行一次性测量，并将结果缓存到响应式变量中。

5. **必须绑定真实存在的 View**：`TextShadow` 创建时传入的 `nativeRef` 必须是一个真实存在的 View 的原生引用，不能使用已销毁或虚构的 `nativeRef`，否则测量将失败或产生不可预期的结果。

6. **一个 View 最多持有一个 TextShadow**：同一个 View（同一个 `nativeRef`）最多只能关联一个 `TextShadow` 实例。如果需要多次测量不同文本，应复用同一个 `TextShadow`（通过 `setProp` 更新属性后重新调用 `calculateRenderViewSize`），而不是创建多个 `TextShadow` 绑定到同一个 View 上。

7. **可设置的属性列表**：通过 `shadow.setProp(key, value)` 可以设置以下文本属性：

| Key | 说明 | 示例值 |
|:----|:-----|:------|
| `"text"` | 文本内容 | `"Hello Kuikly"` |
| `"fontSize"` | 字体大小 | `16f` |
| `"fontWeight"` | 字体粗细 | `"400"` / `"700"` |
| `"fontStyle"` | 字体样式 | `"normal"` / `"italic"` |
| `"fontFamily"` | 字体族 | `"monospace"` |
| `"lineHeight"` | 行高 | `24f` |
| `"lineSpacing"` | 行间距 | `4f` |
| `"letterSpacing"` | 字间距 | `2f` |
| `"numberOfLines"` | 最大行数 | `3` |
| `"useDpFontSizeDim"` | 使用 dp 字号 | `1` |

## 在 Canvas 中测量文本

如果你在 Canvas 组件中需要测量文本（例如绘制图表标签、自定义文本排版），可以直接使用 Canvas 上下文提供的 `measureText` 方法：

```kotlin
Canvas({
    attr {
        width(300f)
        height(200f)
    }
}) { context, _, _ ->
    // 设置字体属性
    context.font(FontStyle.NORMAL, FontWeight.BOLD, 16f)
    
    // 测量文本
    val metrics = context.measureText("Hello Kuikly")
    
    // metrics.width          - 文本宽度
    // metrics.actualBoundingBoxAscent  - 基线到顶部的距离
    // metrics.actualBoundingBoxDescent - 基线到底部的距离
    // metrics.actualBoundingBoxLeft    - 对齐点到文本左边界
    // metrics.actualBoundingBoxRight   - 对齐点到文本右边界
    
    // 利用测量结果精确定位绘制
    val x = 150f
    val y = 100f
    context.textAlign(TextAlign.CENTER)
    context.fillStyle(Color.BLACK)
    context.fillText("Hello Kuikly", x, y)
    
    // 绘制文本边界框
    val left = x - metrics.actualBoundingBoxLeft
    val top = y - metrics.actualBoundingBoxAscent
    val right = x + metrics.actualBoundingBoxRight
    val bottom = y + metrics.actualBoundingBoxDescent
    context.strokeStyle(Color(0xFFFF0000L))
    context.beginPath()
    context.moveTo(left, top)
    context.lineTo(right, top)
    context.lineTo(right, bottom)
    context.lineTo(left, bottom)
    context.closePath()
    context.stroke()
}
```

:::tip
Canvas 的 `measureText` 内部也是通过 `TextShadow` 实现的，同样存在性能开销。在 Canvas 绘制回调中应尽量减少测量次数，可将测量结果缓存复用。
:::

## 小结

| 方案 | 适用场景 | 性能 |
|:----|:-------|:----|
| FlexBox 布局（`allCenter()`、`alignItems` 等） | 文本居中、对齐、自适应容器 | ⭐⭐⭐ 零开销 |
| 绝对定位（`absolutePosition`） | 背景与文本等大、层叠布局 | ⭐⭐⭐ 零开销 |
| TextShadow 测量 | 展开/收起判断、Canvas 绘制定位、自定义排版 | ⭐ 性能开销大 |

**优先使用布局方案，谨慎使用文本测量。**
