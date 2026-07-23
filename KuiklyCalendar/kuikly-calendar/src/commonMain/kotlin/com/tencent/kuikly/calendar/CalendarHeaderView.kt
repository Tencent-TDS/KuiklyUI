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

package com.tencent.kuikly.calendar

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * 日历头部视图，显示月份标题和左右切换箭头。
 *
 * 组件结构：
 * ```
 * [‹]         2025年7月         [›]
 * ```
 *
 * 使用方式：
 * ```
 * CalendarHeader {
 *     attr {
 *         title = "2025年7月"
 *         headerHeight = 44f
 *     }
 *     event {
 *         onPreviousMonth = { /* 上月 */ }
 *         onNextMonth = { /* 下月 */ }
 *     }
 * }
 * ```
 */
class CalendarHeaderView : ComposeView<CalendarHeaderAttr, CalendarHeaderEvent>() {

    override fun createAttr(): CalendarHeaderAttr = CalendarHeaderAttr()

    override fun createEvent(): CalendarHeaderEvent = CalendarHeaderEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flexDirectionRow()
                    justifyContentSpaceBetween()
                    alignItemsCenter()
                    height(ctx.attr.headerHeight)
                    paddingLeft(ctx.attr.horizontalPadding)
                    paddingRight(ctx.attr.horizontalPadding)
                }

                // 左箭头区域
                vif({ ctx.attr.showArrows }) {
                    View {
                        attr {
                            size(ctx.attr.headerHeight, ctx.attr.headerHeight)
                            allCenter()
                        }
                        event {
                            click { ctx.event.onPreviousMonth?.invoke() }
                        }
                        Text {
                            attr {
                                text("‹")
                                fontSize(24f)
                                color(ctx.attr.headerTextColor)
                                textAlignCenter()
                            }
                        }
                    }
                }

                // 月份标题
                Text {
                    attr {
                        text(ctx.attr.title)
                        fontSize(17f)
                        fontWeightSemiBold()
                        color(ctx.attr.headerTextColor)
                        flex(1f)
                        textAlignCenter()
                    }
                }

                // 右箭头区域
                vif({ ctx.attr.showArrows }) {
                    View {
                        attr {
                            size(ctx.attr.headerHeight, ctx.attr.headerHeight)
                            allCenter()
                        }
                        event {
                            click { ctx.event.onNextMonth?.invoke() }
                        }
                        Text {
                            attr {
                                text("›")
                                fontSize(24f)
                                color(ctx.attr.headerTextColor)
                                textAlignCenter()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 日历头部属性配置。
 *
 * @property title 月份标题文本，如 "2025年7月" 或 "July 2025"
 * @property headerHeight 头部区域高度，默认 44f
 * @property headerTextColor 标题和箭头文字颜色，默认 0xFF333333
 * @property showArrows 是否显示左右切换箭头，默认 true
 * @property horizontalPadding 左右内边距，默认 15f
 */
class CalendarHeaderAttr : ComposeAttr() {
    /** 月份标题文本 */
    var title: String = ""

    /** 头部区域高度 */
    var headerHeight: Float = 44f

    /** 标题和箭头文字颜色 */
    var headerTextColor: Color = Color(0xFF333333)

    /** 是否显示左右切换箭头 */
    var showArrows: Boolean = true

    /** 左右内边距 */
    var horizontalPadding: Float = 15f
}

/**
 * 日历头部事件回调。
 *
 * @property onPreviousMonth 点击左箭头时触发，切换到上个月
 * @property onNextMonth 点击右箭头时触发，切换到下个月
 */
class CalendarHeaderEvent : ComposeEvent() {
    /** 切换到上一个月 */
    var onPreviousMonth: (() -> Unit)? = null

    /** 切换到下一个月 */
    var onNextMonth: (() -> Unit)? = null
}

/**
 * 在 [ViewContainer] 中添加 [CalendarHeaderView] 的扩展函数。
 *
 * @param init 初始化回调，用于设置属性和事件
 */
internal fun ViewContainer<*, *>.CalendarHeader(init: CalendarHeaderView.() -> Unit) {
    addChild(CalendarHeaderView(), init)
}
