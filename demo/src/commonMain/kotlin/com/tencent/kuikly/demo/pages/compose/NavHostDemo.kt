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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.animation.slideInHorizontally
import com.tencent.kuikly.compose.animation.slideOutHorizontally
import com.tencent.kuikly.compose.animation.core.FastOutSlowInEasing
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.ButtonDefaults
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.material3.Scaffold
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.navigation.NavBackStackEntry
import com.tencent.kuikly.compose.material3.navigation.NavDeepLink
import com.tencent.kuikly.compose.material3.navigation.NavDestination
import com.tencent.kuikly.compose.material3.navigation.NavGraphBuilder
import com.tencent.kuikly.compose.material3.navigation.NavHost
import com.tencent.kuikly.compose.material3.navigation.NavHostController
import com.tencent.kuikly.compose.material3.navigation.NavOptions
import com.tencent.kuikly.compose.material3.navigation.NavType
import com.tencent.kuikly.compose.material3.navigation.Navigator
import com.tencent.kuikly.compose.material3.navigation.NavigatorState
import com.tencent.kuikly.compose.material3.navigation.composable
import com.tencent.kuikly.compose.material3.navigation.createGraph
import com.tencent.kuikly.compose.material3.navigation.currentBackStackEntryAsState
import com.tencent.kuikly.compose.material3.navigation.dialog
import com.tencent.kuikly.compose.material3.navigation.navArgument
import com.tencent.kuikly.compose.material3.navigation.navDeepLink
import com.tencent.kuikly.compose.material3.navigation.navigation
import com.tencent.kuikly.compose.material3.navigation.rememberNavController
import com.tencent.kuikly.core.bundle.bundleOf
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * NavHost Demo - Demonstrates cross-platform NavHost usage.
 *
 * Shows:
 * - Basic navigation with NavHost + NavHostController
 * - Route parameters (e.g. "detail/{id}")
 * - Nested navigation graph (auth flow via navigation())
 * - Type-safe arguments (via navArgument)
 * - System back button handling
 * - Animated transitions between screens
 * - NavHostController.createGraph() / NavigatorProvider.navigation() for pre-built graphs
 */
@Page("NavHostDemo")
internal class NavHostDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            NavHostDemoNavigationBar("NavHost Demo") {
                NavHostDemoContent()
            }
        }
    }
}

@Composable
private fun NavHostDemoContent() {
val navController = rememberNavController()

    // Alternative: Use createGraph() / NavigatorProvider.navigation() for pre-built graphs.
    // This matches the official pattern where NavigatorProvider.navigation() is the
    // top-level graph builder:
    //
    // val graph = navController.createGraph(startDestination = "home", route = "root") {
    //     composable("home") { HomeScreen() }
    //     composable("detail/{id}") { DetailScreen(it.arguments?.getString("id") ?: "") }
    //     navigation(startDestination = "login", route = "auth") {
    //         composable("login") { LoginScreen() }
    //         composable("register") { RegisterScreen() }
    //     }
    // }
    // NavHost(navController, graph)

    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = {
            // 新页面从右向左滑入
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            // 当前页面向左滑出
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },  // 只滑出一部分，保持视觉连续性
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            // 返回时，上一个页面从左向右滑入
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            // 当前页面向右滑出
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable("home") {
            HomeScreen(navController)
        }
        composable("list") {
            ListScreen(navController)
        }
        composable(
            route = "detail/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: "unknown"
            DetailScreen(navController, id)
        }
        composable("settings") {
            SettingsScreen(navController)
        }

        // Nested navigation graph: auth flow
        navigation(startDestination = "login", route = "auth") {
            composable("login") {
                LoginScreen(navController)
            }
            composable("register") {
                RegisterScreen(navController)
            }
        }
    }
}

// ===== Screens =====

@Composable
private fun HomeScreen(navController: NavHostController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4FF))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF2196F3)),
            contentAlignment = Alignment.Center
        ) {
            Text("🏠", fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Home",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cross-platform NavHost Demo",
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Navigation buttons
        NavButton(
            text = "Go to List",
            color = Color(0xFF4CAF50),
            onClick = { navController.navigate("list") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavButton(
            text = "Go to Detail (id=42)",
            color = Color(0xFFFF9800),
            onClick = { navController.navigate("detail/42") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavButton(
            text = "Go to Settings",
            color = Color(0xFF9C27B0),
            onClick = { navController.navigate("settings") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavButton(
            text = "Go to Auth Flow (nested graph)",
            color = Color(0xFF00BCD4),
            onClick = { navController.navigate("auth") }
        )
    }
}

@Composable
private fun ListScreen(navController: NavHostController) {
    val items = remember { (1..10).map { "Item #$it" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5FFF5))
            .padding(16.dp)
    ) {
        Text(
            text = "List Screen",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(16.dp))

        items.forEachIndexed { index, item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { navController.navigate("detail/${index + 1}") },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4CAF50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item,
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(navController: NavHostController, id: String) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F0))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFF9800)),
            contentAlignment = Alignment.Center
        ) {
            Text("📄", fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Detail Screen",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Route parameter id = $id",
            fontSize = 16.sp,
            color = Color(0xFF666666)
        )

        Spacer(modifier = Modifier.height(32.dp))

        NavButton(
            text = "Go to Settings",
            color = Color(0xFF9C27B0),
            onClick = { navController.navigate("settings") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavButton(
            text = "Back to Home (clear stack)",
            color = Color(0xFFF44336),
            onClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavButton(
            text = "Back",
            color = Color(0xFF607D8B),
            onClick = { navController.popBackStack() }
        )
    }
}

@Composable
private fun SettingsScreen(navController: NavHostController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F0FF))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF9C27B0)),
            contentAlignment = Alignment.Center
        ) {
            Text("⚙️", fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(32.dp))

        NavButton(
            text = "Back to Home",
            color = Color(0xFF2196F3),
            onClick = { navController.popBackStack("home", inclusive = false) }
        )
    }
}

@Composable
private fun LoginScreen(navController: NavHostController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0F7FA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login Screen",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Part of the Auth nested graph",
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )

        Spacer(modifier = Modifier.height(32.dp))

        NavButton(
            text = "Login (Go to Home)",
            color = Color(0xFF00BCD4),
            onClick = {
                navController.navigate("home") {
                    popUpTo("auth") { inclusive = true }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavButton(
            text = "Don't have an account? Register",
            color = Color(0xFF009688),
            onClick = { navController.navigate("register") }
        )
    }
}

@Composable
private fun RegisterScreen(navController: NavHostController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0F2F1))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Register",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(32.dp))

        NavButton(
            text = "Create Account (Go to Login)",
            color = Color(0xFF009688),
            onClick = { navController.popBackStack() }
        )
    }
}

// ===== Components =====

@Composable
private fun NavButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NavHostDemoNavigationBar(
    title: String,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.White)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

/**
 * NavHost 自动化测试 Demo - 用于验证与官方原生表现对齐
 *
 * 测试场景：
 * 1. 动画方向正确性 - push从右滑入，pop从左滑入
 * 2. 深层导航栈 - 连续push多个页面，验证pop行为
 * 3. 参数传递 - 验证不同类型参数的正确传递
 * 4. 状态保存 - 页面切换后状态是否正确保存
 * 5. 边界情况 - 快速双击、空返回等
 */
@Page("NavHostTestDemo")
internal class NavHostTestDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            NavHostDemoNavigationBar("NavHost Test") {
                NavHostTestContent()
            }
        }
    }
}

@Composable
private fun NavHostTestContent() {
    // 简单的测试页面占位
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NavHost Test Content",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "用于验证与官方原生表现对齐的测试页面",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )
    }
}

/**
 * NavHost 新增 API Demo - 演示新增的导航 API
 *
 * 演示内容：
 * 1. currentBackStackEntryAsState() - 响应式获取当前路由
 * 2. getBackStackEntry() - 获取指定路由的 back stack entry
 * 3. previousBackStackEntry - 获取前一个页面并传递结果
 * 4. handleDeepLink() - 处理深链接
 * 5. bundleOf() - 便捷创建 Bundle
 * 6. NavDeepLink - 深链接定义与匹配
 */
@Page("NavHostNewAPIDemo")
internal class NavHostNewAPIDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            NavHostDemoNavigationBar("NavHost New APIs") {
                NavHostNewAPIContent()
            }
        }
    }
}

@Composable
private fun NavHostNewAPIContent() {
    // API 演示: rememberNavController 支持传递自定义 Navigator
    // 这样可以在 NavHostController 中注册自定义的导航器
    val customNavigator = remember { CustomDialogNavigator() }
    val navController = rememberNavController(customNavigator)

    // API 1: currentBackStackEntryAsState() - 响应式获取当前路由
    // 常用于 BottomNavigation 高亮当前选中项
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // 验证自定义 Navigator 已注册
    val hasCustomNavigator = remember { 
        navController.navigatorProvider.navigators.containsKey("custom_dialog") 
    }

    NavHost(
        navController = navController,
        startDestination = "api_home",
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut(tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(300))
        }
    ) {
        composable("api_home") {
            APIHomeScreen(navController, currentRoute = currentRoute, hasCustomNavigator = hasCustomNavigator)
        }
        composable("api_result_source") {
            APIResultSourceScreen(navController)
        }
        composable("api_result_target") {
            APIResultTargetScreen(navController)
        }
        composable("api_deeplink") {
            APIDeepLinkScreen(navController)
        }
        // DeepLink 目标页面 - 用于演示 DeepLink 跳转
        composable(
            route = "deeplink_target/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "myapp://target/{id}"
                }
            )
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: ""
            DeepLinkTargetScreen(navController, id = id)
        }
        composable("api_bundle") {
            APIBundleScreen(navController)
        }
        composable("api_get_entry/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.IntType })
        ) { entry ->
            val itemId = entry.arguments?.getInt("itemId") ?: 0
            APIGetEntryScreen(navController, itemId = itemId)
        }
        
        // 使用 dialog() 函数创建 dialog 风格的 destination
        // dialog destination 会作为叠加层显示，底层页面保持可见
        dialog(
            route = "api_custom_dialog/{message}",
            arguments = listOf(navArgument("message") { type = NavType.StringType })
        ) { entry ->
            val message = entry.arguments?.getString("message") ?: ""
            APICustomDialogScreen(navController, message = message)
        }
    }
}

// ===== Custom Navigator for Testing =====

/**
 * Custom Navigator - 演示 rememberNavController(vararg navigators) 参数
 * 
 * 这是一个自定义 Navigator，用于演示如何通过 rememberNavController
 * 注册自定义导航器并实际使用它进行导航
 */
private class CustomDialogNavigator : Navigator<CustomDialogNavigator.Destination>() {
    
    override val name: String = NAME
    
    private val _state = NavigatorState()
    
    override val state: NavigatorState
        get() = _state
    
    override fun createDestination(): Destination {
        return Destination(this)
    }
    
    /**
     * 自定义导航：将 entry 推入 back stack
     */
    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        entries.forEach { entry ->
            state.push(entry)
        }
    }
    
    /**
     * 自定义返回：弹出 back stack
     */
    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.pop(popUpTo, savedState)
    }
    
    /**
     * Custom destination for dialog-style navigation
     */
    class Destination(
        navigator: CustomDialogNavigator
    ) : NavDestination(navigator) {
        // 可以在这里添加自定义属性，如 dialog 的样式配置
        var dialogTitle: String = "Custom Dialog"
        var dismissOnBackPress: Boolean = true
    }
    
    companion object {
        const val NAME = "custom_dialog"
    }
}

// ===== API Demo Screens =====

@Composable
private fun APIHomeScreen(navController: NavHostController, currentRoute: String?, hasCustomNavigator: Boolean) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(16.dp)
    ) {
        // 展示 currentBackStackEntryAsState 的效果
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "currentBackStackEntryAsState()",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF88)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current Route: $currentRoute",
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = "实时响应路由变化，常用于 BottomNavigation",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 展示 rememberNavController(navigators) 的效果
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "rememberNavController(navigators)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Custom Navigator Registered: $hasCustomNavigator",
                    fontSize = 14.sp,
                    color = if (hasCustomNavigator) Color(0xFF00FF88) else Color(0xFFFF5555)
                )
                Text(
                    text = "通过 vararg navigators 注册自定义导航器",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """// 创建自定义 Navigator
val customNavigator = remember { CustomDialogNavigator() }

// 传递给 rememberNavController
val navController = rememberNavController(customNavigator)

// 验证已注册
navController.navigatorProvider.navigators
    .containsKey("custom_dialog") // true""",
                    fontSize = 10.sp,
                    color = Color(0xFF666666),
                    fontFamily = com.tencent.kuikly.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "New API Demos",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        // API 演示按钮
        APIButton(
            text = "1. previousBackStackEntry 结果传递",
            description = "演示如何向前一个页面传递结果",
            color = Color(0xFF4CAF50),
            onClick = { navController.navigate("api_result_source") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        APIButton(
            text = "2. handleDeepLink 深链接",
            description = "演示深链接处理和 NavDeepLink",
            color = Color(0xFF2196F3),
            onClick = { navController.navigate("api_deeplink") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        APIButton(
            text = "3. bundleOf 便捷方法",
            description = "演示 bundleOf 创建 Bundle",
            color = Color(0xFFFF9800),
            onClick = { navController.navigate("api_bundle") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        APIButton(
            text = "4. getBackStackEntry",
            description = "演示获取指定路由的 back stack entry",
            color = Color(0xFF9C27B0),
            onClick = { navController.navigate("api_get_entry/100") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 新增：测试自定义 Navigator
        APIButton(
            text = "5. Custom Navigator 实战",
            description = "Dialog 覆盖效果：底层页面保持可见",
            color = Color(0xFFE91E63),
            onClick = { navController.navigate("api_custom_dialog/Hello%20from%20Custom%20Navigator") }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 显示已注册的 Navigator 列表
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A3E))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "已注册的 Navigators:",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.height(4.dp))
                val navigators = navController.navigatorProvider.navigators.keys
                navigators.forEach { navigatorName ->
                    Text(
                        text = "• $navigatorName",
                        fontSize = 11.sp,
                        color = Color(0xFF00FF88),
                        fontFamily = com.tencent.kuikly.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * API 2: previousBackStackEntry - 结果传递演示
 * 
 * 演示如何通过 previousBackStackEntry 向前一个页面传递结果
 */
@Composable
private fun APIResultSourceScreen(navController: NavHostController) {
    // 使用 SavedStateHandle.getState() 获取响应式状态，当目标页面设置结果后会自动更新
    val resultState = navController.currentBackStackEntry?.savedStateHandle?.getState<String?>("result")
    val resultText = resultState?.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF4CAF50))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Result Source Screen",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 显示从目标页面返回的结果
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "收到的结果:",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resultText ?: "(等待结果...)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (resultText != null) Color(0xFF4CAF50) else Color(0xFF999999)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("api_result_target") },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Text("Go to Target Screen", color = Color(0xFF4CAF50))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
        ) {
            Text("Back", color = Color.White)
        }
    }
}

@Composable
private fun APIResultTargetScreen(navController: NavHostController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2196F3))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Result Target Screen",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "设置结果并返回给前一个页面",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 演示 previousBackStackEntry 用法
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "API: previousBackStackEntry",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "val prevEntry = navController.previousBackStackEntry\nprevEntry?.savedState?.set(\"result\", \"成功!\")",
                    fontSize = 11.sp,
                    color = Color(0xFF666666),
fontFamily = com.tencent.kuikly.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // 使用 previousBackStackEntry 设置结果
                navController.previousBackStackEntry?.savedStateHandle?.set("result", "Success from target!")
                navController.popBackStack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Text("Set Result & Go Back", color = Color(0xFF2196F3))
        }
    }
}

/**
 * API 3: handleDeepLink & NavDeepLink
 * 
 * 演示深链接处理
 */
@Composable
private fun APIDeepLinkScreen(navController: NavHostController) {
    var deepLinkResult by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2196F3))
            .padding(16.dp)
    ) {
        Text(
            text = "Deep Link Demo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // NavDeepLink API 演示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "NavDeepLink API",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """// 创建深链接
val deepLink = NavDeepLink(
    uriPattern = "myapp://user/{userId}",
    action = "android.intent.action.VIEW",
    mimeType = "text/plain"
)

// 或使用 DSL
navDeepLink {
    uriPattern = "myapp://detail/{id}"
}

// 提取参数名
deepLink.argumentNames // ["userId"]

// 匹配 URI
val args = deepLink.matchUri("myapp://user/123")
// args.getString("userId") == "123"
""",
                    fontSize = 10.sp,
                    color = Color(0xFF666666),
fontFamily = com.tencent.kuikly.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // handleDeepLink API 演示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF388E3C))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "handleDeepLink API",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """// 处理深链接 URI
val handled = navController.handleDeepLink(
    "myapp://user/456"
)
// 如果找到匹配的目的地，返回 true
""",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.9f),
fontFamily = com.tencent.kuikly.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 测试深链接
        Text(
            text = "Test handleDeepLink:",
            fontSize = 14.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    // 测试 DeepLink 跳转到目标页面
                    val handled = navController.handleDeepLink("myapp://target/123")
                    deepLinkResult = "handleDeepLink result: $handled"
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Test Deep Link", color = Color(0xFF2196F3), fontSize = 12.sp)
            }
        }

        if (deepLinkResult != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = deepLinkResult!!,
                fontSize = 12.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) {
            Text("Back", color = Color.White)
        }
    }
}

/**
 * API 4: bundleOf
 * 
 * 演示 bundleOf 便捷方法创建 Bundle
 */
@Composable
private fun APIBundleScreen(navController: NavHostController) {

    // 使用 bundleOf 创建 Bundle
    val bundle = remember {
        bundleOf(
            "name" to "Kuikly",
            "version" to 1.0,
            "isDebug" to true,
            "tags" to arrayListOf("kotlin", "compose", "cross-platform"),
            "count" to 42
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFF9800))
            .padding(16.dp)
    ) {
        Text(
            text = "bundleOf Demo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // bundleOf API 演示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "bundleOf API",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """// 使用 bundleOf 创建 Bundle
val bundle = bundleOf(
    "name" to "Kuikly",
    "version" to 1.0,
    "isDebug" to true,
    "tags" to arrayListOf("kotlin", "compose"),
    "count" to 42
)

// 读取值
bundle.getString("name")      // "Kuikly"
bundle.getDouble("version")   // 1.0
bundle.getBoolean("isDebug")  // true
bundle.getInt("count")        // 42
""",
                    fontSize = 10.sp,
                    color = Color(0xFF666666),
fontFamily = com.tencent.kuikly.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 显示 Bundle 内容
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF57C00))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Bundle Contents:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                BundleItem("name", bundle.getString("name") ?: "null")
                BundleItem("version", bundle.getDouble("version").toString())
                BundleItem("isDebug", bundle.getBoolean("isDebug").toString())
                BundleItem("count", bundle.getInt("count").toString())
                BundleItem("tags", bundle.getStringArrayList("tags")?.joinToString(", ") ?: "null")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
        ) {
            Text("Back", color = Color.White)
        }
    }
}

@Composable
private fun BundleItem(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = key,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * API 5: getBackStackEntry
 * 
 * 演示获取指定路由的 back stack entry
 */
@Composable
private fun APIGetEntryScreen(navController: NavHostController, itemId: Int) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF9C27B0))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Get BackStack Entry",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Item ID: $itemId",
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // getBackStackEntry API 演示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "getBackStackEntry API",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """// 获取指定路由的 back stack entry
val entry = navController.getBackStackEntry("api_home")

// 可用于：
// 1. 访问该页面的 savedState
// 2. 获取该页面的参数
// 3. ViewModel 共享（同一路由）

// 如果路由不存在，抛出异常
try {
    val entry = navController.getBackStackEntry("unknown")
} catch (e: IllegalArgumentException) {
    // 路由不在 back stack 中
}
""",
                    fontSize = 10.sp,
                    color = Color(0xFF666666),
                    fontFamily = com.tencent.kuikly.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 演示获取 api_home 的 entry
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF7B1FA2))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "当前 Back Stack:",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Size: ${navController.backStackSize}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Current Route: ${navController.currentRoute}",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Text("Back", color = Color(0xFF9C27B0))
        }
    }
}

@Composable
private fun APIButton(
    text: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFFAAAAAA)
                )
            }
        }
    }
}

/**
 * API 6: Custom Navigator 实战演示
 * 
 * 演示使用自定义 Navigator 进行导航
 */
@Composable
private fun APICustomDialogScreen(navController: NavHostController, message: String) {

    // 模拟 Dialog 样式的页面
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { 
                // 点击背景关闭
                navController.popBackStack() 
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clickable(enabled = false) { /* 阻止点击穿透 */ },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE91E63)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dialog 标题
                Text(
                    text = "🚀 Custom Navigator",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 消息内容
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // API 说明
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "✨ Custom Navigator 的真正价值:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE91E63)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = """1. 【覆盖而非替换】
   Dialog 覆盖在底层页面上，底层页面保持可见

2. 【自定义导航行为】
   可以实现：BottomSheet、SlideOver、Popup 等

3. 【独立 Back Stack 管理】
   Navigator 管理自己的导航栈

4. 【自定义转场动画】
   不同的 Navigator 可以有不同的动画效果

💡 注意：底层页面 (APIHomeScreen) 仍然可见！
   这就是 Dialog Navigator 的核心价值""",
                            fontSize = 10.sp,
                            color = Color(0xFF666666),
                            fontFamily = com.tencent.kuikly.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 关闭按钮
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Close",
                        color = Color(0xFFE91E63),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ===== DeepLink Target Screen =====

/**
 * DeepLink 目标页面 - 用于演示 DeepLink 跳转效果
 */
@Composable
private fun DeepLinkTargetScreen(navController: NavHostController, id: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF9C27B0)) // Purple
            .padding(16.dp)
    ) {
        Text(
            text = "🎯 DeepLink Target",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "成功通过 DeepLink 跳转！",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "接收到的参数:",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
                Text(
                    text = "id = $id",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF7B1FA2))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DeepLink URI:",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "myapp://target/$id",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back", color = Color(0xFF9C27B0), fontWeight = FontWeight.Bold)
        }
    }
}