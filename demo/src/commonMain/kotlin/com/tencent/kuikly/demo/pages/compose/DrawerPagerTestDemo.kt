package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.*
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.VerticalPager
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.material3.*
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.launch

@Page("DrawerPagerTestDemo")
class DrawerPagerTestDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            DrawerPagerTestContent()
        }
    }

    @Composable
    fun DrawerPagerTestContent() {
        val drawerState = rememberDrawerState()
        val scope = rememberCoroutineScope()
        // true = HorizontalPager, false = VerticalPager
        var useHorizontalPager by remember { mutableStateOf(true) }

        val pageColors = listOf(
            Color(0xFFE3F2FD), Color(0xFFFCE4EC), Color(0xFFF3E5F5),
            Color(0xFFE8F5E9), Color(0xFFFFF8E1), Color(0xFFE0F7FA),
            Color(0xFFFFF3E0), Color(0xFFE8EAF6), Color(0xFFE0F2F1),
            Color(0xFFFBE9E7)
        )

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerWidth = 300.dp,
            drawerContent = {
                ModalDrawerSheet(
                    drawerWidth = 300.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color(0xFF6200EA)),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Drawer + Pager 测试",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (useHorizontalPager) "当前: HorizontalPager" else "当前: VerticalPager",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 开关切换区域
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (useHorizontalPager) "水平 Pager" else "垂直 Pager",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6200EA)
                            )
                            Switch(
                                checked = useHorizontalPager,
                                onCheckedChange = { useHorizontalPager = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF6200EA),
                                    checkedTrackColor = Color(0xFFD1C4E9)
                                )
                            )
                        }

                        Text(
                            text = if (useHorizontalPager)
                                "← 左右滑动翻页（测试与 Drawer 手势冲突） →"
                            else
                                "↑ 上下滑动翻页（测试与 Drawer 手势冲突） ↓",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Pager 区域
                        if (useHorizontalPager) {
                            HorizontalPagerContent(pageColors)
                        } else {
                            VerticalPagerContent(pageColors)
                        }
                    }
                }
            },
        ) {
            // ===== 主内容区域 =====
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF6200EA)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { scope.launch { drawerState.open() } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("☰", fontSize = 24.sp, color = Color.White)
                        }
                        Text(
                            text = "Drawer + Pager 手势测试",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { scope.launch { drawerState.open() } }
                    ) {
                        Text("打开 Drawer")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "当前模式: ${if (useHorizontalPager) "HorizontalPager" else "VerticalPager"}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Drawer 状态信息
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Drawer 状态", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("isOpen: ${drawerState.isOpen}")
                            Text("isClosed: ${drawerState.isClosed}")
                            Text("progress: ${drawerState.progress}")
                            Text("offsetX: ${if (drawerState.offset.isNaN()) -1f else drawerState.offset}")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 测试说明
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🧪 测试要点", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("1. 打开 Drawer，切换 Pager 模式", fontSize = 13.sp)
                            Text("2. HorizontalPager: 左右滑动翻页", fontSize = 13.sp)
                            Text("   - 是否与 Drawer 手势冲突？", fontSize = 12.sp, color = Color(0xFFE65100))
                            Text("   - 向右滑是翻页还是关闭 Drawer？", fontSize = 12.sp, color = Color(0xFFE65100))
                            Text("3. VerticalPager: 上下滑动翻页", fontSize = 13.sp)
                            Text("   - 是否能正常上下翻页？", fontSize = 12.sp, color = Color(0xFFE65100))
                            Text("   - 左右滑动是否能关闭 Drawer？", fontSize = 12.sp, color = Color(0xFFE65100))
                            Text("4. 观察 Logcat 日志 (TAG: KRView_TouchI)", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HorizontalPagerContent(pageColors: List<Color>) {
        val pagerState = rememberPagerState(pageCount = { pageColors.size })

        Column(modifier = Modifier.fillMaxWidth()) {
            // 页码指示
            Text(
                text = "页码: ${pagerState.currentPage + 1} / ${pageColors.size}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6200EA)
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageColors[page]),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "H-Page ${page + 1}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "← 左右滑动翻页 →",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "测试: 滑动是翻页还是关闭 Drawer？",
                            fontSize = 12.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun VerticalPagerContent(pageColors: List<Color>) {
        val pagerState = rememberPagerState(pageCount = { pageColors.size })

        Column(modifier = Modifier.fillMaxWidth()) {
            // 页码指示
            Text(
                text = "页码: ${pagerState.currentPage + 1} / ${pageColors.size}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6200EA)
            )

            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageColors[page]),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "V-Page ${page + 1}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "↑ 上下滑动翻页 ↓",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "测试: 能正常翻页吗？左右能关 Drawer 吗？",
                            fontSize = 12.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }
    }
}
