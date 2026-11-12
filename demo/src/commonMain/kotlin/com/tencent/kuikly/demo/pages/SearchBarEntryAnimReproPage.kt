package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * 搜索框进场动画复现页（自研 DSL 版）。
 *
 * ## 为什么必须用自研 DSL 而不是 Compose
 * 业务搜索框（UbaNeoSearchBarView.kt）是 Kuikly **自研 DSL**：圆角/描边/阴影下发给
 * `borderRadius + border + boxShadow`，iOS 侧由 `UIView+CSS` 转成 `CSSShapeLayer mask`，
 * 并在同时有 boxShadow 时包一层 `KRBoxShadowView`。压线/瞬变都发生在这条链路上；
 * Compose DSL 的圆角走另一套绘制路径，不会触发，因此无法佐证修复。
 *
 * ## 压线根因与修复（结构对照业务）
 * 业务（UbaNeoSearchBarView.kt:199-203）把圆角/描边/底色/阴影下移到**独立的框线层**，
 * 放大镜 / 输入区与框线层是**兄弟**节点。iOS 上圆角是挂在框线层的 `CSSShapeLayer mask`，
 * mask 只裁剪自身子树，兄弟越出框左缘的部分无人裁剪 → 放大镜压着描边进入。
 * 本页已按修复结构编排：放大镜与文字作为框线层的**子节点**，越界部分由圆角 mask 裁掉；
 * 阴影由 iOS 的 KRBoxShadowView 承载在外层，不会被 mask 裁掉。业务侧按同样方式调整即可。
 *
 * ```
 * 整行容器（transform translateY 承载整体位移）
 * ├── 返回键（常驻占位，仅 opacity）
 * └── 搜索框容器（无 borderRadius）
 *     ├── 框线层 View（absolute 形变，borderRadius+border+背景+boxShadow）
 *     │   ├── 放大镜 View（absolute，left 紧随文字滑入；内层 opacity 淡入）
 *     │   └── 文字 Text（absolute，left 从框内左侧顶格滑到放大镜右侧）
 *     └── 右侧按钮 View（absolute）
 * ```
 *
 * ## 动画（节奏参考业务 EntryAppearAnim.kt Anim2Enter，duration 放大便于观察）
 * - 框整体：transform translateY 200 → 0（不走布局）
 * - 框线层：left/top/width/height 形变，heightDelta = 20f（起始更高，上下各外扩 10）
 * - 文字 / 放大镜：位移走 left，与框线层**同一段动画**（业务 entryBoxInsetAnim 同款；
 *   业务已弃用 transform 做文字位移，见 EntryAppearAnim.kt entryInputAreaAnim 注释），全程单调右移：
 *   文字 20（贴框内左侧）→ 47（放大镜右侧）；放大镜 -21（整体在框外）→ 20，始终紧随文字左侧（坐标相对框线层）
 * - 放大镜淡入：前 30%，此时仍在框外，越界部分被圆角裁掉，只从框左缘逐渐露出
 * - 按钮组 / 返回键：后 50%
 *
 * ## 几何（390 宽屏，相对搜索框容器）
 * 框线层 起始 {-26,-10,342,72} → 终态 {0,0,324,52}；右侧按钮 275 → 283。
 */
@Page("98760")
internal class SearchBarEntryAnimReproPage : Pager() {

    /** false = 首页态（动画起点）；true = 起始页态（吸顶终点） */
    private var entered by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this

        // ===== 业务 EntryAppearAnimSpec 常量 =====
        val duration = 2f
        val bgDelay = duration * 0.1f             // 0.025，统一起播偏移
        val barHeight = 52f                        // 框终态高
        val heightDelta = 20f                      // 首页框高出终态的量
        val maxRadius = (barHeight + heightDelta) / 2f  // 36，全程固定，靠 clamp 保持胶囊
        val boxOffsetY = 200f                      // 框整体位移量（transform translateY）

        // ===== 页面几何 =====
        val backHitSize = 24f
        val backMarginStart = 16f
        val backMarginEnd = 10f
        val horizontalMargin = 16f
        val statusTop = ctx.pagerData.safeAreaInsets.top
        val w = ctx.pagerData.pageViewWidth
        val h = ctx.pagerData.pageViewHeight
        val topPadding = 12f
        // 搜索框容器：左侧让出返回键区，右侧留边距（业务 availableSearchBoxWidth 口径）
        val boxContainerW = w - (backMarginStart + backHitSize + backMarginEnd) - horizontalMargin

        // ===== 框内元素几何（相对框线层）=====
        val contentPadding = 20f                   // 框内左侧内边距
        val iconSize = 21f
        val iconGap = 6f
        val textHeight = 22f
        val textStartLeft = contentPadding                           // 20：首页态文字贴框内左侧
        val textEndLeft = contentPadding + iconSize + iconGap        // 47：终态文字在放大镜右侧
        val iconStartLeft = -iconSize                             // -21：首页态放大镜整体在框左缘外
        val iconEndLeft = contentPadding                             // 20：终态放大镜贴框内左侧

        // ===== 动画（全部 linear + 业务 delay）=====
        val animBar = Animation.linear(duration).delay(bgDelay)            // 框整体位移
        val animShrink = Animation.linear(duration).delay(bgDelay)         // 框线层形变 + 文字/放大镜位移
        val animIconFade = Animation.linear(duration * 0.3f).delay(bgDelay) // 放大镜淡入（仍在框外时已可见）
        val animAction = Animation.linear(duration * 0.5f)
            .delay(bgDelay + duration * 0.5f)                             // 按钮组
        val animBack = Animation.linear(duration * 0.5f)
            .delay(bgDelay + duration * 0.5f)                             // 返回键延迟淡入

        return {
            View {
                attr {
                    size(w, h)
                    backgroundColor(Color(0xFFF2F3F5))
                }

                // ================= 整行：布局恒在吸顶位，整体位移由 transform 承载 =================
                View {
                    attr {
                        positionAbsolute()
                        left(0f)
                        top(statusTop + topPadding)
                        width(w)
                        height(barHeight)
                        flexDirectionRow()
                        alignItemsFlexStart()
                        paddingLeft(backMarginStart)
                        paddingRight(horizontalMargin)
                        // 框整体位移：transform translateY（业务 offsetY=200f，避免布局位移触发 relayout）
                        transform(
                            translate = Translate(
                                0f, 0f,
                                offsetX = 0f,
                                offsetY = if (ctx.entered) 0f else boxOffsetY
                            )
                        )
                        animate(animBar, value = ctx.entered)
                    }

                    // ---- 返回键：常驻占位（宽 24 + 右间距 10），动画仅 opacity ----
                    // 若用条件渲染，进场瞬间框宽会突跳（frame 瞬变的根因之一）
                    View {
                        attr {
                            width(backHitSize + backMarginEnd)
                            height(barHeight)
                            opacity(if (ctx.entered) 1f else 0f)
                            animate(animBack, value = ctx.entered)
                        }
                        View {
                            attr {
                                size(backHitSize, backHitSize)
                                marginTop((barHeight - backHitSize) / 2f)
                                borderRadius(backHitSize / 2f)
                                border(Border(1.6f, BorderStyle.SOLID, Color(0xFF242424)))
                            }
                        }
                    }

                    // ================= 搜索框容器：无 borderRadius、overflow(false) =================
                    View {
                        attr {
                            flex(1f)
                            width(boxContainerW)
                            height(barHeight)
                        }

                        // ---- 框线层：独立子层，承载形变 + 圆角/描边/底色/阴影 ----
                        // 起始 {-26,-8, 容器宽+18, 52+20} → 终态铺满容器 {0,0,容器宽,52}
                        View {
                            attr {
                                borderRadius(maxRadius)
                                border(Border(0.5f, BorderStyle.SOLID, Color(0x40000000)))
                                backgroundColor(Color.WHITE)
                                boxShadow(BoxShadow(0f, 2f, 12f, Color(0x0F000000)))
                                positionAbsolute()
                                val insetY = if (ctx.entered) 0f else -heightDelta / 2f
                                // 起始态左右各 24 边距 → 相对本容器 left = 24 - 50 = -26
                                val ml = if (ctx.entered) 0f else -26f
                                val bw = if (ctx.entered) boxContainerW else (w - 48f)
                                left(ml)
                                top(insetY)
                                width(bw)
                                height(if (ctx.entered) barHeight else barHeight + heightDelta)
                                // 本层唯一一段 animate，须放 attr 最后一行（业务注释要求）
                                animate(animShrink, value = ctx.entered)
                            }

                            // ---- 放大镜：框线层的【子节点】（修复压线的关键）。
                            // 圆角 mask 只裁剪自身子树，放在框线层内越出左缘的部分才会被裁掉；
                            // 坐标相对框线层：起点整体在框左缘外(-21) → 终点框内 20，与框线层同一段动画 ----
                            View {
                                attr {
                                    positionAbsolute()
                                    left(if (ctx.entered) iconEndLeft else iconStartLeft)
                                    top(((if (ctx.entered) barHeight else barHeight + heightDelta) - iconSize) / 2f)
                                    size(iconSize, iconSize)
                                    animate(animShrink, value = ctx.entered)
                                }
                                // 透明度内层：单独一段动画（同一 attr 只能绑一段 animate），首页态不可见
                                View {
                                    attr {
                                        size(iconSize, iconSize)
                                        borderRadius(iconSize / 2f)
                                        border(Border(1.5f, BorderStyle.SOLID, Color(0xFF666666)))
                                        opacity(if (ctx.entered) 1f else 0f)
                                        animate(animIconFade, value = ctx.entered)
                                    }
                                }
                            }

                            // ---- 输入文字：框线层的子节点。起点贴框内左侧 20（顶格），终点放大镜右侧 47；
                            // 位移走 left 并与框线层同一段动画，全程单调右移 ----
                            Text {
                                attr {
                                    positionAbsolute()
                                    left(if (ctx.entered) textEndLeft else textStartLeft)
                                    top(((if (ctx.entered) barHeight else barHeight + heightDelta) - textHeight) / 2f)
                                    width(191f)
                                    height(textHeight)
                                    text("搜索或输入网址")
                                    fontSize(15f)
                                    color(Color(0xFF999999))
                                    animate(animShrink, value = ctx.entered)
                                }
                            }
                        }

                        // ---- 右侧按钮：与框线层平级，275 → 283 ----
                        View {
                            attr {
                                positionAbsolute()
                                left(if (ctx.entered) 283f else 275f)
                                top((barHeight - 21f) / 2f)
                                size(21f, 21f)
                                borderRadius(10.5f)
                                backgroundColor(Color(0xFF999999))
                                animate(animAction, value = ctx.entered)
                            }
                        }
                    }
                }

                // ===== 说明 =====
                View {
                    attr {
                        positionAbsolute()
                        left(16f)
                        top(statusTop + topPadding + barHeight + 24f)
                        width(w - 32f)
                    }
                    Text {
                        attr {
                            text("修复结构：放大镜/文字作为框线层子节点，越出框左缘的部分被圆角裁掉，不再压线")
                            fontSize(12f)
                            color(Color(0xFFCC3300))
                        }
                    }
                }

                // ===== 切换按钮 =====
                View {
                    attr {
                        positionAbsolute()
                        bottom(40f)
                        left(40f)
                        right(40f)
                        height(48f)
                        borderRadius(24f)
                        backgroundColor(Color(0xFF222222))
                        justifyContentCenter()
                        alignItemsCenter()
                    }
                    event {
                        click { ctx.entered = !ctx.entered }
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            color(Color.WHITE)
                            text(if (ctx.entered) "退场（飞回首页态）" else "进场（吸顶起始页）")
                        }
                    }
                }
            }
        }
    }
}
