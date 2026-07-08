package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.textPostProcessor
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.drawBehind
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.style.TextDecoration
import com.tencent.kuikly.compose.ui.text.withStyle
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("DashedUnderlineDemo")
class DashedUnderlineDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).background(Color.White)
            ) {
                // ============================================
                // 场景1：实线下划线（验证 Kuikly 文本基线能力）
                // ============================================
                Text("场景1: 实线下划线 (TextDecoration.Underline)")
                Spacer(Modifier.height(4.dp))
                Text(
                    "这是实线下划线（Underline）",
                    textDecoration = TextDecoration.Underline
                )

                Spacer(Modifier.height(24.dp))

                // ============================================
                // 场景2：AnnotatedString + SpanStyle 实下划线
                // 仅给文本中的一小段加下划线（Span 级别），其余文字不加。
                // ============================================
                Text("场景2: AnnotatedString + SpanStyle 下划线（验证 Span 链路）")
                Spacer(Modifier.height(4.dp))
                Text(
                    buildAnnotatedString {
                        append("这是一段普通文字，")
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("这里是实线下划线")
                        }
                        append("。")
                    }
                )

                Spacer(Modifier.height(24.dp))

                // ============================================
                // 场景3：Text + drawBehind 画不出虚线（Kuikly 限制演示）
                // 说明：想给文本加一条虚线下划线，尝试用 drawBehind 在 Text 上
                // 手动画多段短线。但 Kuikly 当前 drawBehind 在 Text 上会被直接
                // 跳过（底层不是 CanvasView），因此下面这行文字下方看不到任何线。
                // 结论：Kuikly 文本目前只支持实线下划线，做不出虚线下划线。
                // ============================================
                Text("场景3: 文本做不出虚线下划线（Text + drawBehind 被跳过）")
                Spacer(Modifier.height(4.dp))
                Text(
                    "这是文本尝试用 drawBehind 画虚线，但画不出来（Kuikly 限制）",
                    modifier = Modifier.drawBehind {
                        val y = size.height - 2.dp.toPx()
                        val dashWidth = 6.dp.toPx()
                        val gapWidth = 4.dp.toPx()
                        val strokeWidth = 1.dp.toPx()
                        var startX = 0f

                        while (startX < size.width) {
                            val endX = minOf(startX + dashWidth, size.width)
                            drawLine(
                                color = Color.Blue,
                                start = Offset(startX, y),
                                end = Offset(endX, y),
                                strokeWidth = strokeWidth
                            )
                            startX += dashWidth + gapWidth
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))

                // ============================================
                // 场景4：文本虚线下划线（跨端统一演示）
                // 说明：Kuikly 的 Text 装饰没有虚线，但 Compose 提供
                // Modifier.textPostProcessor("dashed")。三端共用一个入口组件
                // DashedUnderlineText，由各自原生适配器实现：
                //   - Android：走原生 KRTextPostProcessorAdapter 的 "dashed" 分支，
                //     由自定义 DashedUnderlineSpan 在文字 baseline 下方画出贴合文字宽度的虚线；
                //   - iOS：走原生 KuiklyRenderComponentExpandHandler 的 "dashed" 分支，用
                //     NSAttributedString 的 NSUnderlineStyleSingle | NSUnderlinePatternDash
                //     给整段文本加虚线下划线（原生富文本自带虚线样式）；
                //   - OHOS：走 core-render-ohos 新增的 kDashedUnderline span（方案 C），
                //     framework 在基线处用 OH_Drawing 手画一段段短线，实现真·文本虚线
                //     （跟字形、换行逐行各一条）。
                // 这是目前三端都能在 Kuikly Compose 里得到"真正文本虚线"的官方路径。
                // ============================================
                Text("场景4: textPostProcessor 原生虚线下划线（Android & iOS 走原生适配器，OHOS 走 kDashedUnderline span 基线手画）")
                Spacer(Modifier.height(4.dp))
                DashedUnderlineText("这是一条真正贴合文字宽度的虚线下划线")
            }
        }
    }
}

/**
 * 文本虚线下划线组件（跨端统一入口）：
 * 三端统一走原生 [Modifier.textPostProcessor]("dashed") 适配器，由原生画出贴合文字的虚线。
 *
 * 各端实现：
 *   - Android：DashedUnderlineSpan 在 baseline 下手画虚线
 *   - iOS：NSAttributedString NSUnderlinePatternDash 原生虚线样式
 *   - OHOS：core-render-ohos kDashedUnderline span → OH_Drawing 基线手画虚线段（方案 C）
 */
@Composable
private fun DashedUnderlineText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(text = text, modifier = Modifier.textPostProcessor("dashed"))
}
