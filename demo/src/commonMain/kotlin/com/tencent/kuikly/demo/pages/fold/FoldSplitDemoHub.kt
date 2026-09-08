/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.demo.pages.fold

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.layoutFrameDidChange
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * [FoldSplitStackController] 场景示例的统一路由入口。
 *
 * 对外只需要进入 `FoldSplitDemoHub`，各业务场景仍保留独立 Page，方便单独调试。
 */
@Page("FoldSplitDemoHub")
internal class FoldSplitDemoHub : BasePager() {

    private var contentWidth by observable(0f)

    fun paneWidth(): Float =
        if (contentWidth > 0f) contentWidth else {
            if (pageData.pageViewWidth > 0f) pageData.pageViewWidth else pageData.deviceWidth
        }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(FOLD_HUB_BG)
            }
            event {
                layoutFrameDidChange { frame ->
                    ctx.contentWidth = frame.width
                }
            }
            NavBar {
                attr {
                    title = "自适应工作台"
                }
            }
            List {
                attr {
                    flex(1f)
                    showScrollerIndicator(false)
                    backgroundColor(FOLD_HUB_BG)
                }
                View {
                    attr {
                        width(ctx.paneWidth())
                        padding(left = 16f, top = 18f, right = 16f, bottom = 16f)
                    }
                    Text {
                        attr {
                            text("FoldSplit 工作台")
                            fontSize(22f)
                            fontWeightBold()
                            color(FOLD_HUB_TEXT)
                        }
                    }
                    Text {
                        attr {
                            text("一套架构，自适应大屏双栏与小屏页面栈")
                            marginTop(6f)
                            fontSize(13f)
                            color(FOLD_HUB_SECONDARY)
                        }
                    }
                    View {
                        attr {
                            marginTop(14f)
                            flexDirectionRow()
                        }
                        foldHubCapabilityPill("4 个场景")
                        foldHubCapabilityPill("多级导航")
                        foldHubCapabilityPill("手势返回")
                    }
                }
                Text {
                    attr {
                        width(ctx.paneWidth())
                        text("场景")
                        marginTop(4f)
                        marginLeft(16f)
                        marginBottom(8f)
                        fontSize(13f)
                        fontWeightMedium()
                        color(FOLD_HUB_SECONDARY)
                    }
                }
                View {
                    attr {
                        width(ctx.paneWidth())
                        backgroundColor(Color.WHITE)
                    }
                    FOLD_HUB_ROUTES.forEachIndexed { index, route ->
                        foldHubRouteRow(route, showSeparator = index != FOLD_HUB_ROUTES.lastIndex) {
                            ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                                .openPage(route.pageName)
                        }
                    }
                }
                View {
                    attr {
                        height(36f)
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldHubRouteRow(
    route: FoldHubRoute,
    showSeparator: Boolean,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(76f)
            paddingLeft(16f)
            paddingRight(16f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(Color.WHITE)
        }
        event {
            click {
                onClick()
            }
        }
        View {
            attr {
                size(44f, 44f)
                marginRight(12f)
                borderRadius(12f)
                allCenter()
                backgroundColor(route.color)
            }
            Text {
                attr {
                    text(route.icon)
                    fontSize(18f)
                    color(Color.WHITE)
                }
            }
        }
        View {
            attr {
                flex(1f)
            }
            Text {
                attr {
                    text(route.title)
                    fontSize(17f)
                    fontWeightMedium()
                    color(FOLD_HUB_TEXT)
                }
            }
            Text {
                attr {
                    text(route.subtitle)
                    marginTop(3f)
                    fontSize(12f)
                    color(FOLD_HUB_SECONDARY)
                    lines(1)
                }
            }
        }
        View {
            attr {
                height(22f)
                marginLeft(8f)
                paddingLeft(8f)
                paddingRight(8f)
                borderRadius(11f)
                allCenter()
                backgroundColor(route.tint)
            }
            Text {
                attr {
                    text(route.badge)
                    fontSize(10f)
                    fontWeightMedium()
                    color(route.color)
                }
            }
        }
        Text {
            attr {
                text("›")
                marginLeft(6f)
                fontSize(22f)
                color(FOLD_HUB_CHEVRON)
            }
        }
        if (showSeparator) {
            View {
                attr {
                    positionAbsolute()
                    left(72f)
                    right(0f)
                    bottom(0f)
                    height(0.33f)
                    backgroundColor(FOLD_HUB_SEPARATOR)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldHubCapabilityPill(title: String) {
    View {
        attr {
            height(26f)
            marginRight(8f)
            paddingLeft(10f)
            paddingRight(10f)
            borderRadius(13f)
            allCenter()
            backgroundColor(FOLD_HUB_ACCENT_SOFT)
        }
        Text {
            attr {
                text(title)
                fontSize(11f)
                fontWeightMedium()
                color(FOLD_HUB_ACCENT)
            }
        }
    }
}

private data class FoldHubRoute(
    val pageName: String,
    val icon: String,
    val title: String,
    val subtitle: String,
    val color: Color,
    val tint: Color,
    val badge: String,
)

private val FOLD_HUB_ROUTES = listOf(
    FoldHubRoute(
        pageName = "FoldAdaptiveSettingsDemo",
        icon = "⚙",
        title = "设置",
        subtitle = "玻璃侧栏、可调主窗和四级设置导航",
        color = Color(0xFF8E8E93),
        tint = Color(0xFFF0F0F3),
        badge = "系统",
    ),
    FoldHubRoute(
        pageName = "FoldSplitFilesDemo",
        icon = "▣",
        title = "文件管理",
        subtitle = "目录网格、素材预览和多级文件夹",
        color = Color(0xFF0A84FF),
        tint = Color(0xFFE7F2FF),
        badge = "文件",
    ),
    FoldHubRoute(
        pageName = "FoldSplitMailDemo",
        icon = "✉",
        title = "邮件",
        subtitle = "邮件列表、正文和附件四级导航",
        color = Color(0xFF5E5CE6),
        tint = Color(0xFFEFEEFF),
        badge = "通信",
    ),
    FoldHubRoute(
        pageName = "FoldSplitAiChatDemo",
        icon = "✦",
        title = "AI 聊天",
        subtitle = "会话列表、对话输入和资料页面",
        color = Color(0xFF30B0C7),
        tint = Color(0xFFE8F8FA),
        badge = "AI",
    ),
)

private val FOLD_HUB_BG = Color(0xFFF2F2F7)
private val FOLD_HUB_TEXT = Color(0xFF1C1C1E)
private val FOLD_HUB_SECONDARY = Color(0xFF6C6C70)
private val FOLD_HUB_CHEVRON = Color(0xFFC7C7CC)
private val FOLD_HUB_SEPARATOR = Color(0xFFE4E4E8)
private val FOLD_HUB_ACCENT = Color(0xFF5E5CE6)
private val FOLD_HUB_ACCENT_SOFT = Color(0xFFEFEEFF)
