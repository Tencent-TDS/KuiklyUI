# 使用 ViewModel

Kuikly Compose 内建了 Compose ViewModel API 的支持。使用 `ViewModel` 可以更好地封装业务逻辑，缓存状态，并可在配置更改后持久保留相应状态。

## 基本用法

### 创建 ViewModel

要创建一个 ViewModel，需要继承 `ViewModel` 类：

```kotlin
import com.tencent.kuikly.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CounterViewModel : ViewModel() {
    // 使用 StateFlow 管理状态
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()
    
    // UI 事件处理
    fun increment() {
        _count.value++
    }
    
    fun decrement() {
        _count.value--
    }
    
    fun reset() {
        _count.value = 0
    }
    
    // 清理资源
    override fun onCleared() {
        super.onCleared()
        // 在这里释放资源
    }
}
```

### 在 Composable 中使用 ViewModel

使用 `viewModel` 函数获取或创建 ViewModel 实例：

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tencent.kuikly.lifecycle.viewmodel.compose.viewModel

@Composable
fun CounterScreen() {
    // 获取或创建 ViewModel 实例
    val viewModel: CounterViewModel = viewModel { CounterViewModel() }
    
    // 收集状态
    val count by viewModel.count.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "计数: $count",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.decrement() }) {
                Text("-1")
            }
            Button(onClick = { viewModel.reset() }) {
                Text("重置")
            }
            Button(onClick = { viewModel.increment() }) {
                Text("+1")
            }
        }
    }
}
```

:::tip 提示
`ComposeContainer`容器提供了`viewModel`用到的`LocalLifecycleOwner`和`LocalViewModelStoreOwner`，你也可以在 Composable 中自定义自己的 LocalProvider。
:::

## viewModelScope 协程

ViewModel 提供了 `viewModelScope`，这是一个与 ViewModel 生命周期绑定的协程作用域。当 ViewModel 被清理时，所有在此作用域内启动的协程都会自动取消。

```kotlin
import com.tencent.kuikly.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class TimerViewModel : ViewModel() {
    private val _seconds = MutableStateFlow(0)
    val seconds: StateFlow<Int> = _seconds.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    fun start() {
        if (!_isRunning.value) {
            _isRunning.value = true
            viewModelScope.launch {
                while (_isRunning.value) {
                    delay(1000)
                    _seconds.value++
                }
            }
        }
    }
    
    fun pause() {
        _isRunning.value = false
    }
    
    fun reset() {
        _isRunning.value = false
        _seconds.value = 0
    }
    
    override fun onCleared() {
        super.onCleared()
        _isRunning.value = false
        // viewModelScope 中的协程会自动取消
    }
}
```

## ViewModel 与 Lifecycle 结合

### 监听生命周期事件

可以将 ViewModel 与 Lifecycle 结合使用，实现生命周期感知的功能：

```kotlin
import com.tencent.kuikly.lifecycle.Lifecycle
import com.tencent.kuikly.lifecycle.LifecycleEventObserver
import com.tencent.kuikly.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect

class LifecycleAwareViewModel : ViewModel() {
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()
    
    private val _taskCounter = MutableStateFlow(0)
    val taskCounter: StateFlow<Int> = _taskCounter.asStateFlow()
    
    fun onLifecycleEvent(event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                _isActive.value = true
                startBackgroundTask()
            }
            Lifecycle.Event.ON_PAUSE -> {
                _isActive.value = false
            }
            else -> {}
        }
    }
    
    private fun startBackgroundTask() {
        viewModelScope.launch {
            while (_isActive.value) {
                delay(1000)
                _taskCounter.value++
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        _isActive.value = false
    }
}

@Composable
fun LifecycleAwareDemo() {
    val viewModel: LifecycleAwareViewModel = viewModel { LifecycleAwareViewModel() }
    val isActive by viewModel.isActive.collectAsState()
    val taskCounter by viewModel.taskCounter.collectAsState()
    
    // 获取生命周期并监听事件
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            viewModel.onLifecycleEvent(event)
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Column {
        Text(text = "状态: ${if (isActive) "活跃" else "暂停"}")
        Text(text = "计数: $taskCounter")
    }
}
```

### Lifecycle.eventFlow

使用 `Lifecycle.eventFlow` 可以以 Flow 的方式观察生命周期事件：

```kotlin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.lifecycle.eventFlow

@Composable
fun LifecycleEventFlowDemo() {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var currentState by remember { mutableStateOf("INITIALIZED") }
    
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.eventFlow.collect { event ->
            currentState = when (event) {
                Lifecycle.Event.ON_CREATE -> "CREATED"
                Lifecycle.Event.ON_START -> "STARTED"
                Lifecycle.Event.ON_RESUME -> "RESUMED"
                Lifecycle.Event.ON_PAUSE -> "PAUSED"
                Lifecycle.Event.ON_STOP -> "STOPPED"
                Lifecycle.Event.ON_DESTROY -> "DESTROYED"
                else -> currentState
            }
        }
    }
    
    Text(text = "当前生命周期: $currentState")
}
```

## 最佳实践

1. **状态管理**: 使用 `StateFlow` 管理 UI 状态，使用 `collectAsState()` 在 Composable 中收集状态
2. **单向数据流**: ViewModel 暴露不可变的状态（`StateFlow`），UI 通过调用 ViewModel 方法来触发状态变化
3. **协程管理**: 使用 `viewModelScope` 启动协程，确保协程随 ViewModel 生命周期自动管理
4. **生命周期感知**: 结合 `Lifecycle` 实现生命周期感知的功能
5. **资源清理**: 在 `onCleared()` 中释放需要手动清理的资源

## 相关链接

- [更多 ViewModel 示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/ViewModelDemo.kt)
- [Compose ViewModel 官方文档](https://kotlinlang.org/docs/multiplatform/compose-viewmodel.html)
