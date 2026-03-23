# 导航组件

本页说明 Kuikly Compose 中导航（Navigation）组件的支持情况与使用注意事项。  
基础用法和官方保持一致，请**优先查阅 Jetpack Compose 官方文档**。

> 官方文档（推荐阅读）：[Navigation in Compose](https://developer.android.com/develop/ui/compose/navigation)

## 支持的导航组件

Kuikly 完全对齐 Jetpack Compose Navigation 的核心组件：

### 核心组件

- **NavHost** - 导航容器，管理导航图和页面切换
- **NavHostController** - 导航控制器，处理导航操作
- **rememberNavController()** - 创建并记住 NavController 实例
- **NavGraphBuilder** - DSL 构建导航图
- **NavBackStackEntry** - 导航返回栈条目，包含目的地信息和参数

### 导航 DSL 函数

- **composable()** - 定义可组合的目的地
- **navigation()** - 定义嵌套导航图
- **navArgument()** - 定义导航参数
- **createGraph()** - 预构建导航图

### 导航参数类型

- **NavType.IntType** - 整型参数
- **NavType.StringType** - 字符串参数
- **NavType.BoolType** - 布尔型参数
- **NavType.FloatType** - 浮点型参数
- **NavType.LongType** - 长整型参数

## 基础导航示例

```kotlin
@Composable
fun NavigationExample() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController)
        }
        composable("detail") {
            DetailScreen(navController)
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    Button(onClick = { navController.navigate("detail") }) {
        Text("Go to Detail")
    }
}
```

## 路由参数传递

支持在路由中定义参数，并通过 `NavBackStackEntry` 获取：

```kotlin
NavHost(navController, startDestination = "home") {
    composable("home") { HomeScreen() }
    
    // 路径参数
    composable("detail/{id}") { entry ->
        val id = entry.arguments?.getString("id") ?: ""
        DetailScreen(id)
    }
    
    // 类型安全的参数
    composable(
        route = "user/{userId}",
        arguments = listOf(
            navArgument("userId") {
                type = NavType.IntType
                defaultValue = 0
            }
        )
    ) { entry ->
        val userId = entry.arguments?.getInt("userId") ?: 0
        UserScreen(userId)
    }
}

// 导航时传递参数
navController.navigate("detail/123")
navController.navigate("user/42")
```

## 嵌套导航图

支持嵌套导航图，用于组织复杂的导航流程（如登录流程）：

```kotlin
NavHost(navController, startDestination = "home") {
    composable("home") { HomeScreen() }
    
    // 嵌套导航图：认证流程
    navigation(
        startDestination = "login",
        route = "auth"
    ) {
        composable("login") { LoginScreen() }
        composable("register") { RegisterScreen() }
    }
}

// 导航到嵌套图的起始目的地
navController.navigate("auth")  // 自动跳转到 login
```

## 页面切换动画

支持自定义页面进入/退出动画：

```kotlin
NavHost(
    navController = navController,
    startDestination = "home",
    enterTransition = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300)
        ) + fadeIn(tween(300))
    },
    exitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -it / 3 },
            animationSpec = tween(300)
        ) + fadeOut(tween(300))
    },
    popEnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -it / 3 },
            animationSpec = tween(300)
        ) + fadeIn(tween(300))
    },
    popExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(300)
        ) + fadeOut(tween(300))
    }
) {
    // destinations...
}
```

## 导航操作

### 基础导航

```kotlin
// 导航到指定路由
navController.navigate("detail")

// 携带参数导航
navController.navigate("detail/123")

// 返回上一页
navController.popBackStack()

// 返回到指定页面（不包含）
navController.popBackStack("home", inclusive = false)

// 返回到指定页面（包含）
navController.popBackStack("home", inclusive = true)
```

### 清空栈导航

```kotlin
// 导航并清空栈
navController.navigate("home") {
    popUpTo("home") { inclusive = true }
}

// 导航并清除到某个页面
navController.navigate("dashboard") {
    popUpTo("auth") { inclusive = true }  // 清除整个认证流程
}
```

## 预构建导航图

支持使用 `createGraph()` 预构建导航图：

```kotlin
val navController = rememberNavController()

val graph = navController.createGraph(
    startDestination = "home",
    route = "root"
) {
    composable("home") { HomeScreen() }
    composable("detail/{id}") { entry ->
        DetailScreen(entry.arguments?.getString("id") ?: "")
    }
    navigation(startDestination = "login", route = "auth") {
        composable("login") { LoginScreen() }
        composable("register") { RegisterScreen() }
    }
}

NavHost(navController, graph)
```

## 状态保存

导航组件自动保存每个目的地的状态，页面切换后状态会保留：

```kotlin
@Composable
fun StateExampleScreen() {
    // 使用 rememberSaveable 保存状态
    var counter by rememberSaveable { mutableIntStateOf(0) }
    
    Column {
        Text("Counter: $counter")
        Button(onClick = { counter++ }) {
            Text("Increment")
        }
    }
}
```

## 更多代码示例

以下 Demo 展示了导航组件的典型用法，可在开源仓库中查看完整代码：

- [`NavHostDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/NavHostDemo.kt)：完整的导航示例，包含参数传递、嵌套导航图、动画等
- [`NavHostTestDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/NavHostDemo.kt)：导航测试套件，验证动画方向、深层栈、参数传递等

## 注意事项

1. **API 对齐**：导航组件的 API 设计完全遵循 Jetpack Compose Navigation，可以参考官方文档学习
2. **动画一致性**：页面切换动画行为与官方实现一致，push 从右向左滑入，pop 从左向右滑入
3. **状态管理**：使用 `rememberSaveable` 保存页面状态，确保导航切换后状态不丢失
4. **返回键处理**：`NavHost` 自动处理系统返回键，无需手动处理
5. **参数类型**：推荐使用 `navArgument` 定义参数类型，确保类型安全
