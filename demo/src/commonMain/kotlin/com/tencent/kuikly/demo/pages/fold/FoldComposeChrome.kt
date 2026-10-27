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

@file:OptIn(InternalResourceApi::class)

package com.tencent.kuikly.demo.pages.fold

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.absolutePadding
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.resources.DrawableResource
import com.tencent.kuikly.compose.resources.InternalResourceApi
import com.tencent.kuikly.compose.resources.painterResource
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.pager.ReservedRegionKind
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.ios.UIBarAxis
import com.tencent.kuikly.core.views.ios.UIBarButtonItemPlacement
import com.tencent.kuikly.core.views.ios.UILargeTitleDisplayMode
import com.tencent.kuikly.core.views.ios.UINavigationBar
import com.tencent.kuikly.core.views.ios.UINavigationBarBackgroundStyle
import com.tencent.kuikly.core.views.ios.UINavigationBarMinimizationBehavior
import com.tencent.kuikly.core.views.ios.UINavigationBarMinimizationSafeAreaAdjustment
import com.tencent.kuikly.core.views.ios.navbarItems
import com.tencent.kuikly.core.views.ios.text
import kotlin.math.max
import com.tencent.kuikly.core.base.Color as KuiklyColor

internal const val NAV_BACK_ICON_BASE64 =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII="

/**
 * 桥接 Kuikly 的 `observable`（`reservedRegions` / `safeAreaInsets` / `hingeStatus`）到 Compose 状态。
 * iOS 首帧上屏后上报 `reservedRegions` 时视图宽高可能未变，仅靠 `pageViewWidth` 不会触发重组；
 * 这里通过 0dp 节点订阅 Kuikly 响应式属性，首帧区域到达时立即触发 Compose 重组。
 */
@Composable
internal fun rememberFoldMetricsTick(): Int {
    var tick by remember { mutableStateOf(0) }
    MakeKuiklyComposeNode<DivView>(
        factory = { DivView() },
        modifier = Modifier.size(0.dp),
        viewInit = {
            attr {
                pagerData.reservedRegions
                pagerData.safeAreaInsets
                pagerData.hingeStatus
                tick++
            }
        },
    )
    return tick
}

/** 100% 对齐 `demo/base/NavigationBar.kt` 的 `NavBar`。 */
@Composable
internal fun ComposeDemoNavBar(
    title: String,
    onBack: () -> Unit,
) {
    val pageData = LocalConfiguration.current.pageData
    val topInset = max(pageData.safeAreaInsets.top, pageData.statusBarHeight)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .absolutePadding(top = topInset.dp)
            .height(44.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable { onBack() }
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Image(
                painter = painterResource(DrawableResource(NAV_BACK_ICON_BASE64)),
                contentDescription = "返回",
                modifier = Modifier.size(width = 10.dp, height = 17.dp),
            )
        }
    }
}

/** 100% 对齐 `FoldGuide.kt` 的 `foldParagraph`。 */
@Composable
internal fun ComposeFoldParagraph(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = Color(0xFF636366),
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
internal fun ComposeFactLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF8E8E93),
            modifier = Modifier.width(52.dp),
        )
        Text(
            text = value,
            fontSize = 15.sp,
            color = Color(0xFF1C1C1E),
        )
    }
}

@Composable
internal fun SampleCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(text)
    }
}

/** 示例页左下角的返回目录按钮，避开底部和左侧安全区，使用与 ComposeAllSample 相同的返回图标。 */
@Composable
internal fun BackChip(onClick: () -> Unit) {
    rememberFoldMetricsTick()
    val configuration = LocalConfiguration.current
    configuration.pageViewWidth // 订阅尺寸变化，旋转后重新读取安全区
    val safe = configuration.pageData.safeAreaInsets
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
        Row(
            modifier = Modifier
                .absolutePadding(left = (safe.left + 16f).dp, bottom = (safe.bottom + 16f).dp)
                .height(36.dp)
                .background(Color(0xFFFFFFFF), RoundedCornerShape(18.dp))
                .clickable { onClick() }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(
                painter = painterResource(DrawableResource(NAV_BACK_ICON_BASE64)),
                contentDescription = "返回",
                modifier = Modifier.size(width = 10.dp, height = 17.dp),
            )
            Text(
                text = "返回目录",
                color = Color(0xFF1C1C1E),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

internal class ComposeNavAction(
    val title: String,
    val symbol: String,
    val onClick: () -> Unit = {},
)

/**
 * 通过 [MakeKuiklyComposeNode] 在 Compose 中挂一个 DSL 容器，再在里面声明 UINavigationBar。
 * 系统栏只配置宿主的 navigationItem，本身不参与 Compose 布局，所以节点尺寸为 0。
 */
@Composable
internal fun SystemNavigationBar(
    title: String = "",
    onBack: () -> Unit,
    actions: List<ComposeNavAction> = emptyList(),
) {
    val currentOnBack by rememberUpdatedState(onBack)
    val currentActions by rememberUpdatedState(actions)
    MakeKuiklyComposeNode<DivView>(
        factory = { DivView() },
        modifier = Modifier.size(0.dp),
        viewInit = {
            UINavigationBar {
                attr {
                    tintColor(KuiklyColor.BLACK)
                    barBackground(UINavigationBarBackgroundStyle.Opaque)
                    barScrollEdgeBackground(UINavigationBarBackgroundStyle.Opaque)
                    barBackgroundColor(KuiklyColor.WHITE)
                    barScrollEdgeBackgroundColor(KuiklyColor.WHITE)
                    barShadowColor(KuiklyColor.WHITE)
                    hideScrollEdgeEffects(true)
                    navigationBarMinimizationBehavior(UINavigationBarMinimizationBehavior.Never)
                    navigationBarMinimizationSafeAreaAdjustment(
                        UINavigationBarMinimizationSafeAreaAdjustment.Disabled,
                    )
                }
                if (title.isNotEmpty()) {
                    title {
                        text {
                            attr {
                                title(title)
                                largeTitle(UILargeTitleDisplayMode.Never)
                            }
                        }
                    }
                }
                items {
                    navbarItems {
                        navbarItem {
                            attr {
                                placement(UIBarButtonItemPlacement.Leading)
                                symbol("chevron.backward")
                                title("返回")
                                index(0)
                                axis(UIBarAxis.VerticalPreferred)
                            }
                            event {
                                click { currentOnBack() }
                            }
                        }
                        actions.forEachIndexed { index, action ->
                            navbarItem {
                                attr {
                                    placement(UIBarButtonItemPlacement.Trailing)
                                    symbol(action.symbol)
                                    title(action.title)
                                    index(index)
                                    axis(UIBarAxis.VerticalPreferred)
                                }
                                event {
                                    click { currentActions.getOrNull(index)?.onClick?.invoke() }
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

/**
 * 业务层封装示例：页面只提供标题、返回和正文，各端差异集中在这里处理。
 */
@Composable
internal fun AdaptiveNavScaffold(
    title: String,
    useSystemBars: Boolean,
    onBack: () -> Unit,
    actions: List<ComposeNavAction> = emptyList(),
    content: @Composable () -> Unit,
) {
    rememberFoldMetricsTick()
    val configuration = LocalConfiguration.current
    val pageWidth = configuration.pageViewWidth
    val pageHeight = configuration.pageViewHeight
    val pageData = configuration.pageData
    val safe = pageData.safeAreaInsets

    var left = safe.left
    var right = safe.right
    if (pageWidth > pageHeight) {
        pageData.reservedRegions.forEach { region ->
            if (region.active && region.kind == ReservedRegionKind.OCCLUSION &&
                region.height >= region.width
            ) {
                if (region.x <= 0.5f) {
                    left = max(left, region.x + region.width)
                }
                if (region.x + region.width >= pageWidth - 0.5f) {
                    right = max(right, pageWidth - region.x)
                }
            }
        }
    }

    if (useSystemBars) {
        Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F7))) {
            SystemNavigationBar(title = title, onBack = onBack, actions = actions)
            Box(
                Modifier
                    .fillMaxSize()
                    .absolutePadding(
                        left = left.dp,
                        right = right.dp,
                        bottom = safe.bottom.dp,
                    ),
            ) { content() }
        }
    } else {
        Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F7))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .height((max(safe.top, pageData.statusBarHeight) + 44f).dp)
                    .absolutePadding(
                        top = max(safe.top, pageData.statusBarHeight).dp,
                        left = (left + 8f).dp,
                        right = (right + 8f).dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(DrawableResource(NAV_BACK_ICON_BASE64)),
                        contentDescription = "返回",
                        modifier = Modifier.size(width = 10.dp, height = 17.dp),
                    )
                }
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E),
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                actions.forEach { action ->
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .padding(end = 4.dp)
                            .clickable { action.onClick() }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = action.title,
                            fontSize = 15.sp,
                            color = Color(0xFF007AFF),
                        )
                    }
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .absolutePadding(left = left.dp, right = right.dp, bottom = safe.bottom.dp),
            ) { content() }
        }
    }
}
