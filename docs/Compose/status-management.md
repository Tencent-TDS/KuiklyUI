# 状态管理

本页说明 Kuikly Compose 中状态管理 API 的支持情况与使用注意事项。  
基础用法和官方保持一致，请**优先查阅 Jetpack Compose 官方文档**。

> 官方文档（推荐阅读）：[State in Compose](https://developer.android.com/develop/ui/compose/state)

## 常用 API 分类

### 状态创建与持有

- `remember`：在组合范围内保存对象引用  
- `mutableStateOf` / `mutableIntStateOf` 等：可观察的可变状态  
- `rememberSaveable`：在配置变更或重建后自动恢复状态  
- `mutableStateListOf` / `mutableStateMapOf`：列表 / 字典形式的可观察状态容器

### 派生状态

- `derivedStateOf`：基于一个或多个源 state 计算出的只读派生 state，用于减少不必要重组  
- `snapshotFlow`：将快照状态转换为 Flow，适合与协程流式处理结合

### 副作用与生命周期

- `LaunchedEffect`：在进入组合时启动协程，key 变化时自动 cancel + 重启  
- `DisposableEffect`：适合订阅/注册场景，在离开组合时自动清理  
- `SideEffect`：在成功提交组合帧后执行，适合与外部系统同步状态  
- `produceState`：在协程中异步生产 state，并自动与生命周期绑定  
- `rememberCoroutineScope`：获取与组合生命周期绑定的协程作用域  
- `rememberUpdatedState`：在副作用内部安全持有“最新版本”的值或 lambda

### 组合范围与环境

- `CompositionLocalProvider`：在局部组合树中提供依赖（如主题、环境配置等）  
- `staticCompositionLocalOf` / `compositionLocalOf`：定义可在组合树中传递的环境对象

## 示例：计数与副作用

```kotlin
import androidx.compose.runtime.*
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun CounterCard(onReport: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var count by remember { mutableStateOf(0) }
    val latestOnReport = rememberUpdatedState(onReport)

    LaunchedEffect(count) {
        latestOnReport.value(count)
    }

    Column(Modifier.padding(16.dp)) {
        Text("当前计数：$count")
        Button(onClick = { scope.launch { count++ } }) {
            Text("点我 +1")
        }
    }
}
```

## 注意事项

- 行为对齐：重组、快照模型与官方一致，跨端渲染不影响状态语义。
- 性能：避免在组合外持有 `mutableStateOf`，必要时用 `derivedStateOf` 降低重组范围。
- 副作用：`LaunchedEffect` key 变化时会 cancel 旧协程并重启新协程；`DisposableEffect` 适合订阅/反订阅场景。
- 保存：跨重建保存状态时，可按需使用 `rememberSaveable`（与官方一致）。


