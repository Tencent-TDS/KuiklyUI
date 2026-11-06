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
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.pager.HingeStatus
import com.tencent.kuikly.core.pager.PageData
import com.tencent.kuikly.core.pager.ReservedRegionKind
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.ios.UIBarAxis
import com.tencent.kuikly.core.views.ios.UIBarButtonItemPlacement
import com.tencent.kuikly.core.views.ios.UILargeTitleDisplayMode
import com.tencent.kuikly.core.views.ios.UINavigationBar
import com.tencent.kuikly.core.views.ios.UINavigationBarBackgroundStyle
import com.tencent.kuikly.core.views.ios.UINavigationBarMinimizationBehavior
import com.tencent.kuikly.core.views.ios.UINavigationBarMinimizationSafeAreaAdjustment
import kotlin.math.max

internal class FoldNavAction(
    val title: String,
    val symbol: String,
    val onClick: () -> Unit = {},
)

private const val FOLD_BAR_HEIGHT = 44f
private const val FOLD_BACK_ICON =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII="

/**
 * 业务层导航容器。页面只声明标题、返回和操作。
 * iOS 接入系统导航栏；其他端用页内栏。正文占剩余空间，不再单独加安全区。
 */
internal fun ViewContainer<*, *>.FoldChrome(
    title: String,
    onBack: () -> Unit,
    actions: List<FoldNavAction> = emptyList(),
    content: ViewContainer<*, *>.() -> Unit,
) {
    if (PlatformUtils.isIOS()) {
        foldSystemChrome(title, onBack, actions, content)
    } else {
        foldLocalChrome(title, onBack, actions, content)
    }
}

private fun ViewContainer<*, *>.foldSystemChrome(
    title: String,
    onBack: () -> Unit,
    actions: List<FoldNavAction>,
    content: ViewContainer<*, *>.() -> Unit,
) {
    View {
        attr {
            flex(1f)
            val data = pagerData
            paddingBottom(data.safeAreaInsets.bottom)
            paddingLeft(contentInsetLeft(data))
            paddingRight(contentInsetRight(data))
        }
        content()
    }
    UINavigationBar {
        attr {
            tintColor(Color.BLACK)
            barBackground(UINavigationBarBackgroundStyle.Opaque)
            barScrollEdgeBackground(UINavigationBarBackgroundStyle.Opaque)
            barBackgroundColor(FoldPalette.card)
            barScrollEdgeBackgroundColor(FoldPalette.card)
            barShadowColor(FoldPalette.card)
            hideScrollEdgeEffects(true)
            navigationBarMinimizationBehavior(UINavigationBarMinimizationBehavior.Never)
            navigationBarMinimizationSafeAreaAdjustment(
                UINavigationBarMinimizationSafeAreaAdjustment.Disabled,
            )
            leadingInset(barInsetLeading(pagerData))
            trailingInset(barInsetTrailing(pagerData))
        }
        title {
            text {
                attr {
                    title(title)
                    largeTitle(UILargeTitleDisplayMode.Never)
                }
            }
        }
        items {
            navbarItems {
                navbarItem {
                    attr {
                        symbol("chevron.backward")
                        placement(UIBarButtonItemPlacement.Leading)
                        index(0)
                        axis(UIBarAxis.VerticalPreferred)
                    }
                    event {
                        click { onBack() }
                    }
                }
                actions.forEachIndexed { index, action ->
                    navbarItem {
                        attr {
                            symbol(action.symbol)
                            title(action.title)
                            placement(UIBarButtonItemPlacement.Trailing)
                            index(index)
                            axis(UIBarAxis.VerticalPreferred)
                        }
                        event {
                            click { action.onClick() }
                        }
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldLocalChrome(
    title: String,
    onBack: () -> Unit,
    actions: List<FoldNavAction>,
    content: ViewContainer<*, *>.() -> Unit,
) {
    View {
        attr {
            paddingTop(max(pagerData.safeAreaInsets.top, pagerData.statusBarHeight))
            paddingLeft(contentInsetLeft(pagerData))
            paddingRight(contentInsetRight(pagerData))
            backgroundColor(FoldPalette.card)
        }
        View {
            attr {
                height(FOLD_BAR_HEIGHT)
                flexDirectionRow()
                alignItemsCenter()
                paddingLeft(8f)
                paddingRight(8f)
            }
            foldBackButton(onBack)
            Text {
                attr {
                    flex(1f)
                    marginLeft(8f)
                    text(title)
                    fontSize(17f)
                    fontWeightSemiBold()
                    color(FoldPalette.ink)
                }
            }
            actions.forEach { action ->
                foldBarButton(action.title, action.onClick)
            }
        }
    }
    View {
        attr {
            flex(1f)
            paddingLeft(contentInsetLeft(pagerData))
            paddingRight(contentInsetRight(pagerData))
            paddingBottom(pagerData.safeAreaInsets.bottom)
        }
        content()
    }
}

private fun ViewContainer<*, *>.foldBackButton(onClick: () -> Unit) {
    View {
        attr {
            width(44f)
            height(44f)
            allCenter()
        }
        Image {
            attr {
                size(10f, 17f)
                src(FOLD_BACK_ICON)
            }
        }
        event {
            click { onClick() }
        }
    }
}

private fun ViewContainer<*, *>.foldBarButton(title: String, onClick: () -> Unit) {
    View {
        attr {
            height(32f)
            paddingLeft(10f)
            paddingRight(10f)
            marginRight(4f)
            borderRadius(16f)
            allCenter()
        }
        Text {
            attr {
                text(title)
                fontSize(15f)
                color(FoldPalette.accent)
            }
        }
        event {
            click { onClick() }
        }
    }
}

/**
 * 系统导航叠在内容上方时，由列表/Scroller 自行顶距（见 [foldSystemNavScrollInsetTop]），
 * 不要在 FoldChrome 外包一层 paddingTop，否则在 iOS 27 上易与系统栏 safe area 叠算出一截灰底。
 */
internal fun foldSystemNavScrollInsetTop(data: PageData): Float {
    if (data.hingeStatus != HingeStatus.UNKNOWN) {
        return 0f
    }
    return data.statusBarHeight + FOLD_BAR_HEIGHT
}

/** iPhone Duo 不再重复左右 safeArea；普通机跟 safeAreaInsets。 */
private fun contentInsetLeft(data: PageData): Float {
    if (data.hingeStatus != HingeStatus.UNKNOWN) {
        return 0f
    }
    return data.safeAreaInsets.left
}

/** 竖屏跟 safeAreaInsets.right；横屏再为右侧生效遮挡留白。Duo 为 0。 */
private fun contentInsetRight(data: PageData): Float {
    if (data.hingeStatus != HingeStatus.UNKNOWN) {
        return 0f
    }
    val safe = data.safeAreaInsets.right
    if (isPortrait(data)) return safe
    return safe + occlusionInset(data, trailing = true)
}

/** 系统栏前缘内边距。竖屏为 0，横屏让开左侧生效遮挡。 */
private fun barInsetLeading(data: PageData): Float {
    if (isPortrait(data)) return 0f
    return occlusionInset(data, trailing = false)
}

/** 系统栏尾部内边距。竖屏为 0，横屏让开右侧生效遮挡。 */
private fun barInsetTrailing(data: PageData): Float {
    if (isPortrait(data)) return 0f
    return occlusionInset(data, trailing = true)
}

private fun isPortrait(data: PageData): Boolean {
    return data.pageViewHeight >= data.pageViewWidth
}

/**
 * 生效遮挡在内容区留下的宽度。区域宽高已含边距，这里只用外缘。
 * [trailing] 为 true 时处理屏幕右半边的遮挡，否则处理左半边。
 */
private fun occlusionInset(data: PageData, trailing: Boolean): Float {
    val pageWidth = data.pageViewWidth
    val half = pageWidth / 2f
    val occlusion = data.reservedRegions.firstOrNull {
        if (!it.active || it.kind != ReservedRegionKind.OCCLUSION) return@firstOrNull false
        val center = it.x + it.width / 2f
        if (trailing) center >= half else center < half
    } ?: return 0f
    return if (trailing) {
        (pageWidth - occlusion.x).coerceAtLeast(0f)
    } else {
        (occlusion.x + occlusion.width).coerceAtLeast(0f)
    }
}
