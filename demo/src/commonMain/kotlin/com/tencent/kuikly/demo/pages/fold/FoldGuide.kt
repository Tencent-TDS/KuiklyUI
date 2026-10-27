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

package com.tencent.kuikly.demo.pages.fold

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.pager.HingeStatus
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

internal const val FOLD_BREAKPOINT = 600f

internal object FoldPalette {
    val bg = Color(0xFFF5F5F7)
    val card = Color.WHITE
    val ink = Color(0xFF1C1C1E)
    val secondary = Color(0xFF636366)
    val tertiary = Color(0xFF8E8E93)
    val accent = Color(0xFF007AFF)
    val chip = Color(0xFFE5E5EA)
    val line = Color(0xFFD1D1D6)
}

internal enum class FoldShot(val label: String) {
    Auto("跟随"),
    Narrow("折叠"),
    Wide("展开"),
    Hover("悬停"),
}

/** 跟随窗口时：半开优先看成悬停，宽度达到断点看成展开。手动选项只用于截图。 */
internal enum class FoldLayout {
    Narrow,
    Wide,
    Hover,
}

internal fun foldLayout(selection: FoldShot, width: Float, hinge: HingeStatus): FoldLayout {
    when (selection) {
        FoldShot.Narrow -> return FoldLayout.Narrow
        FoldShot.Wide -> return FoldLayout.Wide
        FoldShot.Hover -> return FoldLayout.Hover
        FoldShot.Auto -> Unit
    }
    if (hinge == HingeStatus.PARTIALLY_OPEN) {
        return FoldLayout.Hover
    }
    if (width >= FOLD_BREAKPOINT) {
        return FoldLayout.Wide
    }
    return FoldLayout.Narrow
}

internal fun foldLayoutLabel(layout: FoldLayout): String {
    return when (layout) {
        FoldLayout.Narrow -> "折叠"
        FoldLayout.Wide -> "展开"
        FoldLayout.Hover -> "悬停"
    }
}

internal fun foldHingeLabel(hinge: HingeStatus): String {
    return when (hinge) {
        HingeStatus.UNKNOWN -> "未知"
        HingeStatus.CLOSED -> "合上"
        HingeStatus.PARTIALLY_OPEN -> "部分打开"
        HingeStatus.FULLY_OPEN -> "全开"
    }
}

internal fun foldSwatch(index: Int): Color {
    return when (index % 4) {
        0 -> Color(0xFF64D2FF)
        1 -> Color(0xFF30D158)
        2 -> Color(0xFFFF9F0A)
        else -> Color(0xFFBF5AF2)
    }
}

/**
 * 正文区只补左右和底部安全区。
 * 顶部状态栏已经算在导航栏里，不要再加 safeAreaInsets.top。
 */
internal fun ContainerAttr.foldContentPadding(extra: Float = 16f) {
    val insets = pagerData.safeAreaInsets
    paddingLeft(insets.left + extra)
    paddingRight(insets.right + extra)
    paddingBottom(insets.bottom + extra)
    paddingTop(extra)
}

internal fun <T> ViewContainer<*, *>.foldChips(
    options: List<T>,
    label: (T) -> String,
    selected: () -> T,
    onSelect: (T) -> Unit,
) {
    View {
        attr {
            flexDirectionRow()
            flexWrapWrap()
            backgroundColor(FoldPalette.card)
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(8f)
            paddingBottom(4f)
        }
        options.forEach { option ->
            View {
                attr {
                    height(32f)
                    paddingLeft(12f)
                    paddingRight(12f)
                    marginRight(8f)
                    marginBottom(8f)
                    borderRadius(16f)
                    allCenter()
                    backgroundColor(
                        if (selected() == option) FoldPalette.accent else FoldPalette.chip
                    )
                }
                Text {
                    attr {
                        text(label(option))
                        fontSize(13f)
                        fontWeightSemiBold()
                        color(if (selected() == option) Color.WHITE else FoldPalette.ink)
                    }
                }
                event {
                    click {
                        onSelect(option)
                    }
                }
            }
        }
    }
}

internal fun ViewContainer<*, *>.foldNote(text: () -> String) {
    Text {
        attr {
            marginLeft(16f)
            marginRight(16f)
            marginTop(8f)
            marginBottom(4f)
            text(text())
            fontSize(12f)
            color(FoldPalette.tertiary)
        }
    }
}

internal fun ViewContainer<*, *>.foldHeading(text: String) {
    Text {
        attr {
            text(text)
            fontSize(22f)
            fontWeightSemiBold()
            color(FoldPalette.ink)
        }
    }
}

internal fun ViewContainer<*, *>.foldParagraph(text: String) {
    Text {
        attr {
            marginTop(8f)
            text(text)
            fontSize(15f)
            lineHeight(22f)
            color(FoldPalette.secondary)
        }
    }
}

internal fun ViewContainer<*, *>.foldSwatchBlock(index: Int, label: String, height: Float) {
    View {
        attr {
            height(height)
            borderRadius(12f)
            backgroundColor(foldSwatch(index))
            allCenter()
        }
        Text {
            attr {
                text(label)
                fontSize(14f)
                fontWeightSemiBold()
                color(Color.WHITE)
            }
        }
    }
}

internal fun ViewContainer<*, *>.foldAction(title: String, filled: Boolean, onClick: () -> Unit) {
    View {
        attr {
            height(44f)
            paddingLeft(18f)
            paddingRight(18f)
            borderRadius(22f)
            allCenter()
            backgroundColor(if (filled) FoldPalette.accent else FoldPalette.chip)
        }
        Text {
            attr {
                text(title)
                fontSize(15f)
                fontWeightSemiBold()
                color(if (filled) Color.WHITE else FoldPalette.ink)
            }
        }
        event {
            click {
                onClick()
            }
        }
    }
}
