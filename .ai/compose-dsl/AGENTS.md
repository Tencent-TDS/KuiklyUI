# Compose DSL 代码模式

> 以下场景读取本文件：使用 `ComposeContainer` + `setContent` 开发页面、修改 `compose/` 模块源码、排查 Compose 组件渲染异常、扩展 Modifier 属性或事件映射。

## 1. 模块定位

`compose` 模块是基于 Jetpack Compose 1.7 深度改造的声明式 UI 层：

- **基于 Compose 1.7**：保持与官方 Compose API 的高度一致性
- **渲染层切换**：将 Compose 渲染层从 Android View/Skia 切换到 KuiklyCore 渲染引擎
- **Runtime 保持原生**：直接依赖官方 `androidx.compose.runtime`，重组和快照系统保持原生实现
- **Compose 层不使用 Core 的高阶组件和 Flexbox 布局**，使用 Compose 自己的布局系统

---

## 2. 包名规则（最重要⚠️）

| 用途 | 正确 import | ❌ 错误 import |
|------|-----------|--------------|
| `@Composable`, `remember`, `mutableStateOf` | `androidx.compose.runtime.*` | — |
| `Modifier`, `Color`, `dp`, `sp` | `com.tencent.kuikly.compose.ui.*` | `androidx.compose.ui.*` |
| `Column`, `Row`, `Box`, `LazyColumn` | `com.tencent.kuikly.compose.foundation.*` | `androidx.compose.foundation.*` |
| `Text`, `Button`, `Card`, `Scaffold` | `com.tencent.kuikly.compose.material3.*` | `androidx.compose.material3.*` |
| `AnimatedVisibility`, `fadeIn` | `com.tencent.kuikly.compose.animation.*` | `androidx.compose.animation.*` |

**总结**: 只有 `androidx.compose.runtime.*` 用官方包，其他一律用 `com.tencent.kuikly.compose.*`。

---

## 3. 架构原理

```
业务层（demo/pages/compose/）
    ↓
Compose DSL 层（compose/）
  • Layout / Material3 / Foundation / Animation
    ↓
KuiklyApplier 适配层
  • Compose KNode → Kuikly 原子组件转换
  • Modifier → Kuikly Attr/Event 映射
    ↓
Core 层（core/）← Compose 不使用 Core 的高阶组件和 Flexbox 布局
  • 原子组件：TextView、ImageView、ScrollView...
  • Module：Router、Network、Bridge...
    ↓
各平台 Render 层（core-render-android/ios/ohos/web/）
```

**关键文件**：
- `compose/.../ComposeContainer.kt` — Compose 页面基类
- `compose/.../KuiklyApplier.kt` — 渲染适配器（Compose KNode → Kuikly 原子组件）
- `compose/.../extension/ModifierSetProp.kt` — Modifier 属性映射
- `compose/.../extension/ModifierSetEvent.kt` — Modifier 事件映射

---

## 4. 页面基本结构

```kotlin
import androidx.compose.runtime.*                          // 官方 runtime
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.foundation.layout.*      // Kuikly 布局
import com.tencent.kuikly.compose.foundation.lazy.*        // Kuikly 懒加载列表
import com.tencent.kuikly.compose.material3.*              // Kuikly Material3
import com.tencent.kuikly.compose.ui.*
import com.tencent.kuikly.compose.ui.unit.*                // dp, sp 等
import com.tencent.kuikly.core.annotations.Page

@Page("MyComposePage")
class MyComposePage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            MyContent()
        }
    }

    @Composable
    fun MyContent() {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Hello Kuikly Compose", fontSize = 24.sp)
        }
    }
}
```

**要点**：
- 用 `@Page("pageName")` 注册
- 在 `willInit()` 中调用 `setContent {}`，**不重写** `body()`
- import 规则见第 1 节包名规则

---

## 5. 桥接原生视图（MakeKuiklyComposeNode）

在 Compose 中使用自研 DSL 的原生视图，或扩展原生 UI 能力时使用。

> 详细文档：[docs/Compose/extend-native-ui.md](../../docs/Compose/extend-native-ui.md)

---

## 6. Demo 示例参考

> 完整示例在 `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/` 下，可直接参考。

| 场景 | 参考文件 |
|------|---------|
| 列表基础 / 加载更多 | `LazyColumnDemo3/4/5.kt` |
| 粘性标题 | `LazyColumnStickyHeader.kt` |
| 嵌套列表 | `LazyColumnNestDemoContainer.kt` |
| 水平列表 | `LazyRowDemo1/2/3.kt` |
| 网格布局 | `LazyHorizontalGridDemo1/2/3.kt`、`StaggeredHorizontalGridDemo1/3.kt` |
| 水平分页 / 轮播 | `HorizontalPagerDemo1/2/3.kt` |
| 竖向分页 | `VerticalPagerDemo.kt` |
| 下拉刷新 | `PullToRefreshDemo.kt` |
| 动画（可见性/内容/过渡） | `AnimatedVisibilityDemo.kt`、`AnimatedContentDemo.kt`、`TransitionDemo.kt` |
| 手势冲突 | `GestureConflictDemo.kt` |
| 拖拽 | `AnchoredDraggableDemo.kt` |
| Scaffold / AppBar / 导航栏 | `ScaffoldDemo.kt`、`AppBarDemo.kt`、`NavigationBarDemo.kt` |
| Dialog / BottomSheet | `DialogDemoPage.kt`、`BottomSheetDemo1.kt` |
| TextField 交互 | `TextFieldDemo.kt`、`TextFieldInteractionSourceDemo.kt` |
| ViewModel / 协程 | `ViewModelDemo.kt`、`CoroutinesDemoPage.kt` |
| Canvas 自绘 | `CanvasDemo.kt` |
| 液态玻璃（iOS 26+） | `LiquidGlassComposeDemo.kt` |
