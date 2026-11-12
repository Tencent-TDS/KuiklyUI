# 多线程与协程

## Compose 中的协程调用

Compose 管理的协程（`LaunchedEffect`、`rememberCoroutineScope()` 等）默认在 **Kuikly 线程** 执行，可以直接修改 `mutableStateOf` 状态，无需手动切换线程。

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

在 `ComposeContainer` 的 Page 生命周期方法（如 `created()`、`willInit()` 等）中，可以使用 `kotlinx.coroutines` 库配合 `Dispatchers.Kuikly` 进行线程切换：

```kotlin
@Page("MyComposePage")
class MyComposePage : ComposeContainer() {

    override fun created() {
        super.created()
        val ctx = this
        GlobalScope.launch(Dispatchers.Kuikly[ctx]) {
            val data = withContext(Dispatchers.IO) {
                kmpFetchData()
            }
            // 回到 Kuikly 线程，安全更新 UI
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

依赖配置参考 [协程和多线程编程指引](../DevGuide/thread-and-coroutines.md#接入方式) 中的 kotlinx 协程库和 kuiklyx 协程库接入方式。