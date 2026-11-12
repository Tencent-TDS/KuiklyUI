# 多线程与协程

在 Compose DSL（`ComposeContainer` + `setContent{}`）中，协程和线程模型与自研 DSL 有所不同。

## Compose 内部的线程调度

`ComposeContainer` 创建时会构造一个 `ComposeDispatcher(pagerId)`，该调度器内部通过 `KuiklyContextScheduler.runOnKuiklyThread(pagerId)` 将所有协程任务投递到 Kuikly 线程。这意味着 Compose 管理的协程（`LaunchedEffect`、`rememberCoroutineScope()` 等）默认在 **Kuikly 线程** 执行，直接修改 `mutableStateOf` 状态是线程安全的。

## Compose 中的协程调用

`LaunchedEffect` 和 `rememberCoroutineScope().launch` 启动的协程均在 **Kuikly 线程** 中执行：

```kotlin
@Composable
fun MyContent() {
    var data by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // 在 Kuikly 线程中执行
        data = fetchData()
    }

    Text(text = data, fontSize = 24.sp)
}
```

也可以通过 `rememberCoroutineScope()` 响应用户交互时启动协程：

```kotlin
@Composable
fun MyContent() {
    var data by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Button(onClick = {
        scope.launch {
            // 在 Kuikly 线程中执行
            data = fetchData()
        }
    }) {
        Text("加载数据")
    }
}
```

## Page 层使用 kotlinx 协程库

在 `ComposeContainer` 的 Page 生命周期方法（如 `created()`、`willInit()` 等）中，可以直接使用 `kotlinx.coroutines` 库配合 `Dispatchers.Kuikly` 进行线程切换：

```kotlin
@Page("MyComposePage")
class MyComposePage : ComposeContainer() {

    override fun created() {
        super.created()
        val ctx = this
        // 使用 kotlinx 协程库 + kuiklyx 协程库切换线程
        GlobalScope.launch(Dispatchers.Kuikly[ctx]) {
            val data = withContext(Dispatchers.IO) {
                kmpFetchData()
            }
            // 回到 Kuikly 线程，可以安全更新 UI
            // ...
        }
    }

    override fun willInit() {
        super.willInit()
        setContent {
            MyContent()
        }
    }
}
```

需要在 `build.gradle.kts` 中添加依赖：

```gradle
val commonMain by getting {
    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$KOTLINX_COROUTINES_VERSION")
        implementation("com.tencent.kuiklyx:coroutines:$KUIKLYX_COROUTINES_VERSION")
    }
}
```

> **总结**：Compose 管理的协程（`LaunchedEffect` / `rememberCoroutineScope()`）自动运行在 Kuikly 线程。在 Page 生命周期方法中则使用 `kotlinx` 协程库配合 `Dispatchers.Kuikly` 切换线程。