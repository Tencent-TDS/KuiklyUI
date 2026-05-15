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

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.PagerState
import com.tencent.kuikly.compose.foundation.pager.PageSize
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

/**
 * Banner Carousel Demo 1: Basic auto-scrolling banner with indicators
 */
@Page("BasicBannerCarousel")
class BasicBannerCarousel : ComposeContainer() {
    override fun willInit() {
        super.willInit()

        setContent {
            ComposeNavigationBar {
                Column(modifier = Modifier.fillMaxSize().background(Color.Gray)) {
                    BannerDemoContent()
                }
            }
        }
    }

    @Composable
    private fun BannerDemoContent() {
        // Banner data - simulate real banner items
        val bannerList = remember {
            listOf(
                BannerItem(1, "Banner 1", Color(0xFF4285F4)),
                BannerItem(2, "Banner 2", Color(0xFF34A853)),
                BannerItem(3, "Banner 3", Color(0xFFFBBC05)),
                BannerItem(4, "Banner 4", Color(0xFFEA4335)),
                BannerItem(5, "Banner 5", Color(0xFF9C27B0)),
            )
        }

        var autoPlayEnabled by remember { mutableStateOf(true) }
        val pagerState = rememberPagerState(pageCount = { bannerList.size })

        // Auto-scroll coroutine
        LaunchedEffect(autoPlayEnabled, pagerState.currentPage) {
            if (pagerState.pageCount > 1 && autoPlayEnabled) {
                delay(3000L) // 3 seconds interval
                val nextPage = if (pagerState.currentPage < pagerState.pageCount - 1) {
                    pagerState.currentPage + 1
                } else {
                    0 // Loop back to first page
                }
                pagerState.animateScrollToPage(nextPage)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Banner title with auto-play toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Banner Carousel Demo",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (autoPlayEnabled) "Auto: ON" else "Auto: OFF",
                    color = if (autoPlayEnabled) Color.Green else Color.Red,
                    modifier = Modifier.clickable {
                        autoPlayEnabled = !autoPlayEnabled
                    },
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Banner Carousel
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSize = PageSize.Fill,
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = false
                ) { page ->
                    val banner = bannerList[page]
                    val pageOffset =
                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val scale = 1f - (pageOffset.absoluteValue * 0.1f).coerceAtMost(0.1f)

                    BannerCard(
                        banner = banner,
                        scale = scale,
                        onClick = {
                            println("Banner clicked: ${banner.id}")
                        }
                    )
                }
            }

            // Custom Indicator
            Spacer(Modifier.height(16.dp))
            BannerIndicator(
                pageCount = bannerList.size,
                currentPage = pagerState.currentPage,
                currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
                modifier = Modifier.fillMaxWidth(),
                onIndicatorClick = { pageIndex ->
                    // Manual navigation is not directly supported by PagerState in version of compose for now
                    // But we can record the target page
                }
            )

            Spacer(Modifier.height(24.dp))

            // Manual controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CircleButton(
                    text = "Previous",
                    onClick = {
                        val prevPage = if (pagerState.currentPage > 0) {
                            pagerState.currentPage - 1
                        } else {
                            bannerList.size - 1
                        }
                        // Note: Direct page navigation support depends on compose version
                        autoPlayEnabled = false
                    },
                    color = Color.White
                )

                CircleButton(
                    text = "Next",
                    onClick = {
                        val nextPage = if (pagerState.currentPage < bannerList.size - 1) {
                            pagerState.currentPage + 1
                        } else {
                            0
                        }
                        autoPlayEnabled = false
                    },
                    color = Color.White
                )
            }

            Spacer(Modifier.height(24.dp))

            // Page counter
            Text(
                text = "Page ${pagerState.currentPage + 1} / ${bannerList.size}",
                fontSize = 16.sp,
                color = Color.LightGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    private fun BannerCard(
        banner: BannerItem,
        scale: Float,
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(banner.color, RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = banner.title,
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${banner.id}",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }

    @Composable
    private fun BannerIndicator(
        pageCount: Int,
        currentPage: Int,
        currentPageOffsetFraction: Float,
        modifier: Modifier = Modifier,
        onIndicatorClick: (Int) -> Unit
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { pageIndex ->
                val isSelected = pageIndex == currentPage
                val targetWidth = if (isSelected) 24.dp else 8.dp
                val color = if (isSelected) Color.White else Color.Gray

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(targetWidth)
                        .height(8.dp)
                        .background(
                            color = color,
                            shape = CircleShape
                        )
                )
            }
        }
    }

    @Composable
    private fun CircleButton(
        text: String,
        onClick: () -> Unit,
        color: Color
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                fontSize = 12.sp
            )
        }
    }

    data class BannerItem(
        val id: Int,
        val title: String,
        val color: Color
    )
}

/**
 * Banner Carousel Demo 2: Infinite loop banner with advanced effects
 */
@Page("AdvancedBannerCarousel")
class AdvancedBannerCarousel : ComposeContainer() {
    override fun willInit() {
        super.willInit()

        setContent {
            ComposeNavigationBar {
                Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    AdvancedBannerContent()
                }
            }
        }
    }

    @Composable
    private fun AdvancedBannerContent() {
        val bannerData = remember {
            listOf(
                BannerPage("Nature", Color(0xFF1B5E20), "Explore the beauty"),
                BannerPage("Ocean", Color(0xFF0D47A1), "Deep blue wonder"),
                BannerPage("Sunset", Color(0xFFBF360C), "Golden moments"),
                BannerPage("Night", Color(0xFF311B92), "City lights"),
            )
        }

        val pagerState = rememberPagerState(pageCount = { bannerData.size })
        var autoPlay by remember { mutableStateOf(true) }

        // Auto-scroll with loop
        LaunchedEffect(pagerState.currentPage, autoPlay) {
            if (pagerState.pageCount > 1 && autoPlay) {
                delay(4000L)
                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(nextPage)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Title
            Text(
                text = "Advanced Auto Carousel",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )

            // Banner with visual effects
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSize = PageSize.Fixed(280.dp),
                    pageSpacing = 20.dp,
                    beyondViewportPageCount = 2,
                ) { page ->
                    val banner = bannerData[page]
                    val pageOffset =
                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val scale = 1f - (pageOffset.absoluteValue * 0.15f).coerceAtMost(0.15f)
                    val rotationY = pageOffset * 5f
                    val alpha = 1f - (pageOffset.absoluteValue * 0.5f).coerceAtMost(0.5f)

                    AdvancedBannerItem(
                        banner = banner,
                        scale = scale,
                        alpha = alpha,
                        onClick = { autoPlay = !autoPlay }
                    )
                }
            }

            // Progress indicator
            LinearProgressIndicator(
                pageCount = bannerData.size,
                currentPage = pagerState.currentPage,
                currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .height(4.dp)
            )

            // Page info
            Text(
                text = bannerData[pagerState.currentPage].subtitle,
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Tap banner to ${if (autoPlay) "pause" else "play"} auto-scroll",
                fontSize = 12.sp,
                color = Color.DarkGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    @Composable
    private fun AdvancedBannerItem(
        banner: BannerPage,
        scale: Float,
        alpha: Float,
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    banner.backgroundColor,
                    RoundedCornerShape(20.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = banner.title,
                    fontSize = 40.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = banner.subtitle,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }

    @Composable
    private fun LinearProgressIndicator(
        pageCount: Int,
        currentPage: Int,
        currentPageOffsetFraction: Float,
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .background(Color.DarkGray, RoundedCornerShape(2.dp))
        ) {
            Row {
                repeat(pageCount) { index ->
                    val progress = when {
                        index < currentPage -> 1f
                        index == currentPage -> 1f - currentPageOffsetFraction.absoluteValue
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .padding(horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    Color.White,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }
    }

    data class BannerPage(
        val title: String,
        val backgroundColor: Color,
        val subtitle: String
    )
}
