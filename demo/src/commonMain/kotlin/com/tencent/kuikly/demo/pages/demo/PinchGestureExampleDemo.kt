/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * pinch(双指捏合)事件示例页。
 *
 * 提供两种缩放建模方式:
 * - 示例一(transform): 以捏合点为焦点缩放，适用于图片浏览等需要跟手的场景
 * - 示例二(size): 手势结束时把倍数吸收进宽高，适用于只需最终尺寸的场景
 *
 * 示例一覆盖四个验证点:
 * 1. scale语义: 是否为「相对手势开始时的累计倍数」，而非相邻回调的增量
 * 2. 捏合中心点: 以捏合点为焦点缩放，该点应保持静止，且多次不同位置捏合不跳变
 * 3. 手势共存: 位于可滚动容器内时，捏合是否会被外层滚动打断
 * 4. 手势职责边界: pan / pinch / click 三者的触发时机是否符合预期
 *
 * ## 缩放建模说明(示例一)
 *
 * 采用 `scale + translate` 而非 `anchor` 表达缩放中心。
 *
 * 锚点恒为组件中心 c，渲染位置为 `V(p) = c + s·(p − c) + t`。
 * 由于锚点 c 出现在表达式中，若在 s != 1 时改变 c，V 会跳变，
 * 因此无法同时满足「绕任意点缩放」与「不跳变」。改用 translate 补偿:
 *
 * 手势开始:  V0 = c + s0·(p0 − c) + t0
 * 手势过程中要求 V(p0) 恒等于 V0，解得:
 *
 *     t = t0 + (s0 − s)·(p0 − c)
 *
 * 手势首帧 s == s0，代入得 t == t0，故起手连续不跳变。
 *
 * 两端复合顺序一致(均为先 scale 后 translate，且平移量 = 百分比 × 布局宽高，
 * 不随 scale 放大)，故同一公式在 Android/iOS 表现一致:
 * - iOS: UIView+CSS.m 中 CGAffineTransformTranslate 后接 CGAffineTransformScale
 * - Android: KRCSSAnimation.kt 中 translationX = 百分比 × frameWidth，后置于 scaleX
 */
@Page("PinchGestureExampleDemo")
internal class PinchGestureExampleDemo : BasePager() {

    // ---- 示例一: 渲染状态 ----

    /** 当前缩放倍数 */
    private var currentScale: Float by observable(1f)

    /** 当前平移量(dp)，用于补偿缩放引起的焦点位移 */
    private var translateX: Float by observable(0f)
    private var translateY: Float by observable(0f)

    // ---- 示例一: 手势基准(上次手势结束时固化) ----

    private var baseScale: Float = 1f
    private var baseTranslateX: Float = 0f
    private var baseTranslateY: Float = 0f

    // ---- 示例一: 本次手势内固定的量 ----

    /** 手势开始时的缩放倍数，即公式中的 s0 */
    private var startScale: Float = 1f

    /** 手势开始时的捏合焦点(组件内 dp 坐标)，即公式中的 p0，手势内不再变化 */
    private var focusX: Float = CENTER
    private var focusY: Float = CENTER

    // ---- 示例一: 界面回显 ----

    private var stateText: String by observable("等待双指捏合")
    private var scaleText: String by observable("scale: -")
    private var focusText: String by observable("focus: -")
    private var translateText: String by observable("translate: -")
    private var pageCenterText: String by observable("pageCenter: -")

    /** 校验scale是否为累计语义: 若为增量语义，该区间会持续贴在1.0附近抖动 */
    private var rawScaleRangeText: String by observable("本次手势 scale 区间: -")
    private var minRawScale: Float = 1f
    private var maxRawScale: Float = 1f

    /** 最近触发的手势，用于观察 pinch / pan / click 三者的触发边界是否符合预期 */
    private var gestureLogText: String by observable("最近手势: -")

    // ---- 示例一: pan(单指拖拽)状态 ----
    //
    // 拖拽使用 pageX/pageY 而非 x/y 计算位移。
    // x/y 是组件局部坐标，会随本组件自身的 scale 变化，用它计算增量会引入
    // 与缩放耦合的误差; pageX/pageY 位于页面坐标系，不受本组件transform影响。

    private var panStartPageX: Float = 0f
    private var panStartPageY: Float = 0f
    private var panStartTranslateX: Float = 0f
    private var panStartTranslateY: Float = 0f

    // ---- 示例二: 以宽高承载缩放 ----

    private var imageWidth by observable(IMAGE_SIZE)
    private var imageHeight by observable(IMAGE_SIZE)
    private var sizeScale by observable(1f)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFF1E1E1EL))
            }

            NavBar {
                attr {
                    title = "PinchGestureExampleDemo"
                }
            }

            List {
                attr {
                    flex(1f)
                }

                // 数值回显区
                View {
                    attr {
                        margin(12f)
                        padding(12f)
                        backgroundColor(Color(0xFF2C2C2CL))
                        borderRadius(8f)
                    }
                    Text {
                        attr {
                            text(ctx.stateText)
                            color(Color.YELLOW)
                            fontSize(16f)
                        }
                    }
                    Text {
                        attr {
                            text(ctx.scaleText)
                            color(Color.WHITE)
                            fontSize(14f)
                            marginTop(6f)
                        }
                    }
                    Text {
                        attr {
                            text(ctx.rawScaleRangeText)
                            color(Color(0xFF7FD1AEL))
                            fontSize(14f)
                            marginTop(6f)
                        }
                    }
                    Text {
                        attr {
                            text(ctx.focusText)
                            color(Color.WHITE)
                            fontSize(14f)
                            marginTop(6f)
                        }
                    }
                    Text {
                        attr {
                            text(ctx.translateText)
                            color(Color.WHITE)
                            fontSize(14f)
                            marginTop(6f)
                        }
                    }
                    Text {
                        attr {
                            text(ctx.pageCenterText)
                            color(Color.WHITE)
                            fontSize(14f)
                            marginTop(6f)
                        }
                    }
                    Text {
                        attr {
                            text(ctx.gestureLogText)
                            color(Color(0xFF9AD1FFL))
                            fontSize(14f)
                            marginTop(6f)
                        }
                    }
                }

                Text {
                    attr {
                        text("示例一: transform 缩放(以捏合点为焦点)")
                        color(Color.WHITE)
                        fontSize(15f)
                        marginLeft(12f)
                        marginRight(12f)
                        marginTop(4f)
                    }
                }
                Text {
                    attr {
                        text("验证点1: 放大后松手，再次放大，应从上次结果继续放大（scale 为累计语义）")
                        color(Color(0xFFAAAAAAL))
                        fontSize(12f)
                        marginLeft(12f)
                        marginRight(12f)
                        marginTop(4f)
                    }
                }
                Text {
                    attr {
                        text("验证点2: 在任意位置捏合，该点应保持静止；连续在不同位置捏合均不应跳变")
                        color(Color(0xFFAAAAAAL))
                        fontSize(12f)
                        marginLeft(12f)
                        marginRight(12f)
                        marginTop(4f)
                    }
                }
                Text {
                    attr {
                        text("验证点3: 本页在 List 内，捏合过程不应被列表滚动打断")
                        color(Color(0xFFAAAAAAL))
                        fontSize(12f)
                        marginLeft(12f)
                        marginRight(12f)
                        marginTop(4f)
                    }
                }
                Text {
                    attr {
                        text("验证点4: 手势职责边界 —— 单指拖动图片、双指缩放、单击切换2倍；双指时不应派发 pan，捏合后不应误触发单击")
                        color(Color(0xFFAAAAAAL))
                        fontSize(12f)
                        marginLeft(12f)
                        marginRight(12f)
                        marginTop(4f)
                    }
                }

                // 捏合目标区域
                View {
                    attr {
                        height(360f)
                        margin(12f)
                        backgroundColor(Color(0xFF2C2C2CL))
                        borderRadius(8f)
                        allCenter()
                        overflow(true)
                    }

                    Image {
                        attr {
                            size(IMAGE_SIZE, IMAGE_SIZE)
                            src(IMAGE_URL)
                            // 锚点保持默认(组件中心)，缩放中心由 translate 补偿实现
                            transform(
                                scale = Scale(ctx.currentScale, ctx.currentScale),
                                translate = Translate(
                                    ctx.translateX / IMAGE_SIZE,
                                    ctx.translateY / IMAGE_SIZE
                                )
                            )
                        }

                        event {
                            pinch { params ->
                                ctx.onPinch(params.state, params.x, params.y, params.scale)
                                ctx.pageCenterText =
                                    "pageCenter: (${ctx.format(params.pageX)}, ${ctx.format(params.pageY)})"
                                ctx.gestureLogText = "最近手势: pinch(${params.state})"
                            }
                            // 单指拖拽: 与pinch共存，验证二者的触发边界
                            pan { params ->
                                ctx.onPan(params.state, params.pageX, params.pageY)
                                ctx.gestureLogText = "最近手势: pan(${params.state})"
                            }
                            // 单击切换缩放: 验证click不会被pinch/pan误触发
                            click {
                                ctx.onClick()
                                ctx.gestureLogText = "最近手势: click"
                            }
                        }
                    }
                }

                // 重置按钮
                View {
                    attr {
                        height(44f)
                        margin(12f)
                        borderRadius(22f)
                        backgroundColor(Color(0xFF4A90D9L))
                        allCenter()
                    }
                    event {
                        click {
                            ctx.reset()
                        }
                    }
                    Text {
                        attr {
                            text("重置缩放")
                            color(Color.WHITE)
                            fontSize(16f)
                        }
                    }
                }

                Text {
                    attr {
                        text("示例二: 以宽高承载缩放，手势结束时把倍数吸收进 size")
                        color(Color.WHITE)
                        fontSize(15f)
                        marginLeft(12f)
                        marginRight(12f)
                        marginTop(8f)
                    }
                }

                View {
                    attr {
                        height(280f)
                        margin(12f)
                        backgroundColor(Color(0xFF2C2C2CL))
                        borderRadius(8f)
                        alignItems(FlexAlign.CENTER)
                        overflow(true)
                    }
                    Image {
                        attr {
                            size(ctx.sizeScale * ctx.imageWidth, ctx.sizeScale * ctx.imageHeight)
                            src(IMAGE_URL)
                        }
                        event {
                            pinch {
                                if (it.state != STATE_END) {
                                    ctx.sizeScale = it.scale
                                } else {
                                    ctx.imageWidth *= it.scale
                                    ctx.imageHeight *= it.scale
                                    ctx.sizeScale = 1f
                                }
                            }
                        }
                    }
                }

                // 占位，使列表可滚动，便于验证手势共存
                View {
                    attr {
                        height(500f)
                    }
                }
            }
        }
    }

    /**
     * 处理捏合回调。
     *
     * @param state 手势状态 start/move/end
     * @param x 捏合中心点在组件内的 x(dp)
     * @param y 捏合中心点在组件内的 y(dp)
     * @param rawScale 框架回调的缩放倍数(相对手势起点的累计值)
     */
    private fun onPinch(state: String, x: Float, y: Float, rawScale: Float) {
        if (state == STATE_START) {
            // 手势开始: 固化基准与焦点，整个手势内不再变化
            startScale = baseScale
            focusX = x
            focusY = y
            minRawScale = rawScale
            maxRawScale = rawScale
        } else {
            if (rawScale < minRawScale) {
                minRawScale = rawScale
            }
            if (rawScale > maxRawScale) {
                maxRawScale = rawScale
            }
        }

        // 累计倍数 × 上次手势结束时的基准，并夹紧到允许范围
        val scale = (startScale * rawScale).coerceIn(MIN_SCALE, MAX_SCALE)

        // t = t0 + (s0 − s)·(p0 − c)
        // 注: 此处使用夹紧后的实际 scale 参与计算，保证到达边界时不跳变
        //（代价是边界处焦点会缓慢漂移，demo 可接受）
        val deltaScale = startScale - scale
        currentScale = scale
        translateX = baseTranslateX + deltaScale * (focusX - CENTER)
        translateY = baseTranslateY + deltaScale * (focusY - CENTER)

        stateText = "state: $state"
        scaleText = "scale(回调原值): ${format(rawScale)}   实际渲染: ${format(scale)}"
        focusText = "focus: (${format(focusX)}, ${format(focusY)})"
        translateText = "translate: (${format(translateX)}, ${format(translateY)})"
        rawScaleRangeText =
            "本次手势 scale 区间: ${format(minRawScale)} ~ ${format(maxRawScale)}"

        if (state == STATE_END) {
            // 手势结束: 固化结果，供下次手势累积
            baseScale = currentScale
            baseTranslateX = translateX
            baseTranslateY = translateY
        }
    }

    /**
     * 处理单指拖拽。
     *
     * @param state 手势状态 start/move/end
     * @param pageX 触点在页面坐标系的x
     * @param pageY 触点在页面坐标系的y
     */
    private fun onPan(state: String, pageX: Float, pageY: Float) {
        when (state) {
            STATE_START -> {
                panStartPageX = pageX
                panStartPageY = pageY
                panStartTranslateX = baseTranslateX
                panStartTranslateY = baseTranslateY
            }

            STATE_END -> {
                baseTranslateX = translateX
                baseTranslateY = translateY
            }

            else -> {
                translateX = panStartTranslateX + (pageX - panStartPageX)
                translateY = panStartTranslateY + (pageY - panStartPageY)
                translateText = "translate: (${format(translateX)}, ${format(translateY)})"
            }
        }
    }

    /** 单击在原始大小与2倍之间切换，切换时回到居中 */
    private fun onClick() {
        val targetScale = if (baseScale > 1f + SCALE_EPSILON) 1f else CLICK_ZOOM_SCALE
        baseScale = targetScale
        currentScale = targetScale
        baseTranslateX = 0f
        baseTranslateY = 0f
        translateX = 0f
        translateY = 0f
        scaleText = "scale: 单击切换至 ${format(targetScale)}"
        translateText = "translate: (0.00, 0.00)"
    }

    private fun reset() {
        baseScale = 1f
        baseTranslateX = 0f
        baseTranslateY = 0f
        startScale = 1f
        focusX = CENTER
        focusY = CENTER
        currentScale = 1f
        translateX = 0f
        translateY = 0f
        stateText = "已重置，等待双指捏合"
        scaleText = "scale: -"
        focusText = "focus: -"
        translateText = "translate: -"
        pageCenterText = "pageCenter: -"
        rawScaleRangeText = "本次手势 scale 区间: -"
        gestureLogText = "最近手势: -"
        panStartPageX = 0f
        panStartPageY = 0f
        panStartTranslateX = 0f
        panStartTranslateY = 0f
        imageWidth = IMAGE_SIZE
        imageHeight = IMAGE_SIZE
        sizeScale = 1f
    }

    private fun format(value: Float): String {
        val scaled = (value * 100).toInt()
        val intPart = scaled / 100
        val decPart = if (scaled < 0) -(scaled % 100) else scaled % 100
        return "$intPart.${decPart.toString().padStart(2, '0')}"
    }

    companion object {
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 4f
        private const val IMAGE_SIZE = 200f

        /** 组件中心点坐标(dp)，即公式中的 c */
        private const val CENTER = IMAGE_SIZE / 2

        private const val STATE_START = "start"
        private const val STATE_END = "end"

        /** 单击切换到的放大倍数 */
        private const val CLICK_ZOOM_SCALE = 2f

        /** 浮点比较容差 */
        private const val SCALE_EPSILON = 0.01f

        private const val IMAGE_URL =
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg"
    }
}
