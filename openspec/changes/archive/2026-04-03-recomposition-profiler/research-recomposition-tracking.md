# Compose 重组追踪技术调研

> 调研日期：2026-03-18
> 背景：`TrackRecomposition` 外部包装器模式无法追踪 content 内部的重组（因 Compose skip 优化），需调研业界方案。

## 问题复现

当前 `TrackRecomposition` 采用**外部包装器**模式：

```kotlin
TrackRecomposition("CounterSection") {
    CounterSection()  // 内部有 var count = mutableStateOf(0)
}
```

当 `count` 变化时，Compose 只重组 `CounterSection` 内部（独立的 RecomposeScope），
**不会重组** `TrackRecomposition` 本身（参数 `name` 和 `content` lambda 引用没变，被 skip）。

实测结果：仅 Frame #1（profiler 启动时 `enabledState` 变化）记录了 4 次重组，
后续所有帧 `recomposed=0`，尽管 State 变更被 Snapshot 观察者正确捕获。

---

## 业界方案

### 方案 1：Compose Compiler 插桩（官方核心机制）

**实现层级**：编译器插件

Compose 编译器在编译时自动向每个 `@Composable` 函数注入追踪代码：

```kotlin
// 编译前
@Composable fun MyComposable(name: String) { Text(name) }

// 编译后（简化）
@Composable fun MyComposable(name: String, $composer: Composer, $changed: Int) {
    $composer.startRestartGroup(key)
    if ($changed and 0b0001 != 0 || !$composer.skipping) {
        // 函数体被执行 = 发生了一次重组
        traceEventStart(key, "MyComposable (file.kt:line)")
        Text(name)
        traceEventEnd()
    } else {
        $composer.skipToGroupEnd()  // skip，不计入重组次数
    }
    $composer.endRestartGroup()?.updateScope { ... }
}
```

- `startRestartGroup` / `endRestartGroup`：标记重组边界（RecomposeScope）
- `$changed` bitmask：编译器生成的参数变化标记，决定是否 skip
- `traceEventStart` / `traceEventEnd`：配合 `runtime-tracing` 库输出到 systrace
- `isTraceInProgress()`：运行时检查追踪是否开启

**Android Studio Layout Inspector** 的 "Show Recomposition Counts" 功能即基于此机制，
统计每个 `startRestartGroup` 被执行（非 skip）的次数。

**优势**：最底层最可靠，不受 skip 优化影响，覆盖所有 Composable
**劣势**：需修改 Compose 编译器插件，维护成本高

**参考**：[Compose Composition Tracing 文档](https://developer.android.com/develop/ui/compose/tooling/tracing)

---

### 方案 2：`CompositionObserver`（官方实验性 Runtime API）

**实现层级**：Compose Runtime

Compose Runtime 1.10.0-alpha03+ 新增的 `@ExperimentalComposeRuntimeApi` 接口：

```kotlin
@ExperimentalComposeRuntimeApi
interface CompositionObserver {
    fun onBeginComposition(composition: ObservableComposition)
    fun onScopeEnter(scope: RecomposeScope)
    fun onReadInScope(scope: RecomposeScope, value: Any)
    fun onScopeExit(scope: RecomposeScope)
    fun onEndComposition(composition: ObservableComposition)
    fun onScopeInvalidated(scope: RecomposeScope, value: Any?)
    fun onScopeDisposed(scope: RecomposeScope)
}
```

回调含义：

| 回调 | 触发时机 | 追踪用途 |
|------|---------|---------|
| `onScopeEnter` | RecomposeScope 进入组合（被重组） | 计数：重组次数 +1 |
| `onScopeExit` | RecomposeScope 退出组合 | 计算重组耗时 |
| `onReadInScope` | Scope 内读取 State | 关联 State → Composable |
| `onScopeInvalidated` | State 变更导致 Scope 失效 | 识别重组原因 |
| `onScopeDisposed` | Scope 被永久移除 | 清理追踪数据 |

**优势**：官方 API，在 Runtime 层面观察，不受 skip 优化影响，能观察所有 RecomposeScope
**劣势**：实验性 API，可能变更

**参考**：[CompositionObserver API 文档](https://composables.com/docs/androidx.compose.runtime/runtime/1.10.0-alpha03/interfaces/CompositionObserver)

> ⚠️ **重要更新**：经源码验证，KuiklyUI 使用的 JetBrains Compose Runtime 1.7.3 **已包含此 API**。
> 详见下方 [KuiklyUI Runtime 可用 API 验证](#kuiklyui-runtime-可用-api-验证)。

---

### 方案 3：Rebugger（开源库，内部调用 + remember 比较）

**实现层级**：Composable 函数（应用层）

[Rebugger](https://github.com/theapache64/rebugger) 是一个在 **目标 Composable 内部调用** 的追踪工具：

```kotlin
// 使用方式：在目标 Composable 内部调用
@Composable
fun VehicleUi(car: Car, bike: Bike) {
    Rebugger(trackMap = mapOf("car" to car, "bike" to bike))
    // ... UI 内容
}
```

核心实现（完整源码）：

```kotlin
@Composable
fun Rebugger(
    trackMap: Map<String, Any?>,
    composableName: String? = findComposableName()
) {
    val count = remember { Ref(0) }
    val flag = remember { Ref(false) }
    SideEffect { count.value++ }  // 统计重组次数

    val changeLog = StringBuilder()
    for ((key, newArg) in trackMap) {
        var recompositionTrigger by remember { mutableStateOf(false) }
        // 关键：用 trigger 控制 remember 重新计算
        val oldArg = remember(recompositionTrigger) { newArg }
        val reason = when {
            oldArg != newArg -> "`$key` changed from `$oldArg` to `$newArg`"
            oldArg !== newArg -> "`$key` instance changed"
            else -> null
        }
        if (reason != null) {
            changeLog.append("\n\t $reason")
            flag.value = true
            recompositionTrigger = !recompositionTrigger  // 更新 remember 的 key
        }
    }

    if (changeLog.isNotEmpty()) {
        logger("$composableName recomposed because $changeLog")
    }
}
```

**核心原理**：
1. `Rebugger` 作为 `@Composable` 在目标函数内部调用
2. 当目标函数被重组时，`Rebugger` 的代码也会被执行（同一个 RecomposeScope）
3. 用 `remember(trigger) { newArg }` 存储上次值，比较检测变化原因
4. `SideEffect { count.value++ }` 在每次成功重组后计数

**优势**：纯运行时实现，跨平台，无编译器修改
**劣势**：需手动在每个目标 Composable 内部调用，需显式传入参数 map

---

### 方案 4：vkompose（编译器插件自动注入）

**实现层级**：Compose 编译器插件

[vkompose](https://github.com/nicorougemont/vkompose) 通过编译器插件自动向 Composable 注入 `RecomposeLogger` 调用：

```kotlin
// 编译器自动注入，等价于在每个 Composable 开头添加：
RecomposeLogger(
    name = "SomeFunctionName",
    arguments = mapOf("param1" to param1, "param2" to param2),
)
```

`RecomposeLogger` 的实现原理与 Rebugger 类似（remember + 参数比较），
但通过编译器插件实现了**自动化**，无需手动修改代码。

**优势**：自动化，开发者无需修改代码
**劣势**：需维护编译器插件，与 KuiklyUI 的编译器 fork 集成有风险

---

## 方案对比

| 维度 | Compiler 插桩 | CompositionObserver | Rebugger 模式 | vkompose |
|------|:---:|:---:|:---:|:---:|
| 追踪可靠性 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| 自动覆盖所有 Composable | ✅ | ✅ | ❌ 需手动 | ✅ |
| 无需修改编译器 | ❌ | ✅ | ✅ | ❌ |
| KuiklyUI fork 可用 | 需改编译器 | ✅ **已确认可用** | ✅ 可用 | 需改编译器 |
| 跨平台 (KMP) | 取决于编译器 | ✅ | ✅ | 取决于编译器 |
| 追踪 skip 优化 | ✅ 天然支持 | ✅ 天然支持 | N/A（在 scope 内部） | ✅ 天然支持 |
| 重组原因追踪 | ❌（仅计数） | ✅ (onReadInScope) | ✅（参数比较） | ✅（参数比较） |

---

## 对 KuiklyUI Profiler 的启示

### 核心结论

**业界没有任何工具采用"外部包装器"模式来做重组计数。**

所有成熟方案都满足一个共同前提：**追踪代码必须在目标 Composable 的 RecomposeScope 内执行**。
实现方式要么是编译器自动注入（方案 1/4），要么是 Runtime 层观察（方案 2），
要么是开发者手动在目标函数内部放置追踪调用（方案 3）。

### 当前 `TrackRecomposition` 的根本问题

```kotlin
// 当前模式（外部包装器）
TrackRecomposition("CounterSection") {
    CounterSection()  // 独立的 RecomposeScope，skip 优化使外层不被重组
}
```

`TrackRecomposition` 和 `CounterSection` 是**不同的 RecomposeScope**。
Compose 的 skip 优化确保了：当 `CounterSection` 内部 State 变化时，
只有 `CounterSection` 被重组，`TrackRecomposition` 被 skip。

### 可行的改进方向

1. **Rebugger 模式（内部调用）** — 最可行，纯运行时，跨平台
   - 改 `TrackRecomposition` 为在目标 Composable 内部调用的函数
   - 如：`RecompositionTracker(name)` 放在函数体开头

2. **移植 CompositionObserver** — 中等工作量
   - 将 Compose 1.10+ 的 `CompositionObserver` 接口回移植到 KuiklyUI 的 Compose fork
   - 在 Runtime 层观察所有 RecomposeScope，无需修改用户代码

3. **编译器插件扩展** — 最全面但成本最高
   - 在 KuiklyUI 的 Compose 编译器插件中添加重组计数逻辑
   - 自动覆盖所有 Composable

---

## KuiklyUI Runtime 可用 API 验证

> 通过解压 Gradle 缓存中 `org.jetbrains.compose.runtime:runtime-macosarm64:1.7.3-sources.jar`
> 验证了以下 API 在 KuiklyUI 当前依赖的 Runtime 中**已经存在**。

### 1. `CompositionObserver` + `RecomposeScopeObserver`（可用 ✅）

**源码路径**：`commonMain/androidx/compose/runtime/tooling/CompositionObserver.kt`

```kotlin
@ExperimentalComposeRuntimeApi
interface CompositionObserver {
    // 组合开始，invalidationMap 包含将被重组的 RecomposeScope 及其触发的 State
    fun onBeginComposition(
        composition: Composition,
        invalidationMap: Map<RecomposeScope, Set<Any>?>
    )
    // 组合结束
    fun onEndComposition(composition: Composition)
}

@ExperimentalComposeRuntimeApi
interface RecomposeScopeObserver {
    // scope 的 recompose lambda 即将执行（即将被重组）
    fun onBeginScopeComposition(scope: RecomposeScope)
    // scope 的 recompose lambda 执行完毕
    fun onEndScopeComposition(scope: RecomposeScope)
    // scope 被销毁
    fun onScopeDisposed(scope: RecomposeScope)
}

// 注册观察者的扩展函数
fun Composition.observe(observer: CompositionObserver): CompositionObserverHandle?
fun RecomposeScope.observe(observer: RecomposeScopeObserver): CompositionObserverHandle
```

**关键发现**：这个版本的 API 与网上文档（1.10.0-alpha03）有差异：
- 没有 `onScopeEnter/onScopeExit/onReadInScope/onScopeInvalidated`
- 取而代之的是两个独立接口：`CompositionObserver`（组合级别）和 `RecomposeScopeObserver`（scope 级别）
- `CompositionObserver.onBeginComposition` 的 `invalidationMap` 参数直接提供了**将被重组的 scope 列表及其触发 State**
- `RecomposeScopeObserver.onBeginScopeComposition/onEndScopeComposition` 提供了 scope 级别的精确重组时序

**使用方式**：
```kotlin
// 在 Composition 级别观察
val handle = composition.observe(object : CompositionObserver {
    override fun onBeginComposition(composition, invalidationMap) {
        // invalidationMap: 哪些 scope 将被重组，以及触发它们的 State
        for ((scope, triggerStates) in invalidationMap) {
            println("Scope $scope will recompose, triggered by: $triggerStates")
        }
    }
    override fun onEndComposition(composition) { }
})

// 在单个 RecomposeScope 级别观察
val scopeHandle = recomposeScope.observe(object : RecomposeScopeObserver {
    override fun onBeginScopeComposition(scope) { /* 重组开始 */ }
    override fun onEndScopeComposition(scope) { /* 重组结束 */ }
    override fun onScopeDisposed(scope) { /* scope 销毁 */ }
})
```

### 2. `CompositionTracer`（可用 ✅）

**源码路径**：`commonMain/androidx/compose/runtime/Composer.kt`

```kotlin
// 全局 tracer 设置
@InternalComposeTracingApi
interface CompositionTracer {
    fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String)
    fun traceEventEnd()
    fun isTraceInProgress(): Boolean
}

// 通过 Companion 设置全局 tracer
Composer.Companion.setTracer(tracer: CompositionTracer)
```

**原理**：Compose 编译器已经在每个 `@Composable` 函数中注入了
`traceEventStart(key, dirty, dirty2, info)` / `traceEventEnd()` 调用。
只需注册一个 `CompositionTracer`，就能收到**所有** Composable 的重组通知。

- `key`: 编译器生成的唯一标识符（基于源码位置）
- `dirty1/dirty2`: `$changed` bitmask 的值（参数变化标记）
- `info`: 人类可读的函数名 + 源码位置（如 `"CounterSection (Demo.kt:195)"`）

**优势**：编译器已经插桩完毕，我们只需注册 tracer，零额外编译器改动
**劣势**：标记为 `@InternalComposeTracingApi`，API 不稳定

### 3. `@NonSkippableComposable`（可用 ✅）

**源码路径**：`commonMain/androidx/compose/runtime/NonSkippableComposable.kt`

```kotlin
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class NonSkippableComposable
```

给 `@Composable` 函数加上此注解后，编译器不会生成 skip 优化代码，
函数体在每次组合时都会执行。可以用于确保 `TrackRecomposition` 不被 skip。

---

## 更新后的推荐方案

基于源码验证，**方案 2（`CompositionObserver`）和 `CompositionTracer` 在当前 Runtime 中都可用**。
结合实际情况，推荐方案优先级：

### 优先推荐：`CompositionTracer`（方案 1 的运行时版本）✅ 已实现

- **原理**：注册全局 `CompositionTracer`，接收编译器已注入的 `traceEventStart/End` 回调
- **覆盖范围**：所有被编译器处理的 `@Composable` 函数（自动、全量）
- **信息丰富度**：函数名 + 源码位置 + `$changed` bitmask
- **代码改动量**：极小，只需在 `RecompositionProfiler.start()` 中调用 `Composer.setTracer()`
- **风险**：标记为 `@InternalComposeTracingApi`（Compose 内部 API），意味着不保证跨版本兼容，
  未来 Runtime 升级时可能被修改或删除。不过 KuiklyUI 锁定了 Runtime 1.7.3，短期内不会受影响

**实现状态**：✅ 已在 `RecompositionTracker.compositionTracer` 中实现并验证。
替代了原有的 `TrackRecomposition` 外部包装器模式，自动覆盖所有 Composable，无需手动标注。

### 备选：`CompositionObserver` + `RecomposeScopeObserver` ✅ 已实现

- **原理**：在 Composition 上注册观察者，监听组合级事件和 scope 级重组
- **优势**：`invalidationMap` 直接提供 scope → triggerStates 的映射关系
- **劣势**：需要获取 `Composition` 实例来注册（需从 `BaseComposeScene` 内部拿到）
- **风险**：`@ExperimentalComposeRuntimeApi`

**实现状态**：✅ 已在 `ProfilerCompositionObserver` 中实现。
通过 `ProfilerLifecycleListener` 机制在 `BaseComposeScene.setContent()` 后注册到 Composition，
结合 `RecomposeScope.observe()` 手动注册每个 invalidated scope 的 `RecomposeScopeObserver`，
实现了精确的 Composable→State 重组原因追踪。

### 不适用：`@NonSkippableComposable`

- 给 Composable 加上此注解后，编译器不会生成 skip 优化代码
- 但**对我们的需求没有帮助**：它只能确保"被调用时不被 skip"，无法解决"根本没被调用"的问题
- 子 Composable（如 `CounterSection`）有独立的 RecomposeScope，其内部 State 变化只会触发自身重组，
  不会向上传播到父级 `TrackRecomposition`，无论父级是否标记了 `@NonSkippableComposable`
- **结论：不纳入推荐方案**

---

## 实现进展总结

### 已完成 ✅

1. **CompositionTracer 全量重组追踪**：替代 TrackRecomposition，自动覆盖所有 Composable
2. **CompositionObserver 精确重组原因追踪**：通过 `invalidationMap` + `RecomposeScopeObserver` 实现精确的 Composable→State 关联
3. **框架 Composable 过滤**：默认只监控业务代码，`includeFrameworkComposables` 配置可打开全部
4. **ProfilerLifecycleListener 机制**：动态注册/注销 CompositionObserver，支持 profiler 启停切换

### 进行中 🔄

5. **State 对象源码位置标识**：当前 State 只显示 `MutableState(value=3)@128220496`（内存地址），无法定位到代码中的变量声明位置。详见下方调研。

---

## State 源码位置标识调研

> 调研日期：2026-03-19（更新 2026-03-19）
> 背景：CompositionObserver 的 `invalidationMap` 提供了精确的 State 对象，但 State 对象的 `toString()` 只有内存地址，无法定位到代码中哪个变量。

### 问题描述

当前 `triggers=` 输出为：
```
triggers=[MutableState(value=3)@128220496]
```

用户无法从中得知：
- 这个 State 是代码里哪个变量（如 `count`、`isEnabled`）
- 在哪个文件的哪一行声明的

### 业界现状

**没有任何现成工具能直接追踪 "哪个 `mutableStateOf` 实例触发了重组" 并关联到源码位置**：
- Android Studio Layout Inspector：只展示重组计数，不追踪 State 身份
- Rebugger：追踪参数引用变化，非 State 读取
- vkompose：追踪函数参数变化，非底层 State 对象
- **ovCompose（腾讯视频）**：在 Runtime 中为 `MutableState` 接口新增了 `sourceFile` 字段，但编译器未自动注入，需手动调用 `setSourceFile()`

### ovCompose 方案分析

> 来源：`/Users/qibu/Git/Work/ovCompose`，腾讯视频团队对 JetBrains Compose Multiplatform 的 fork

#### 核心机制：Runtime 内部插桩 + RecompositionHandler

ovCompose 在 Compose Runtime **内部的关键代码路径**上插入了观察回调，新增了 `RecompositionHandler` 接口：

```kotlin
// runtime/inspection/RecompositionHandler.kt
interface RecompositionHandler {
    // 参数变更通知 — 当 Composable 参数发生变化时
    fun change(old: Any?, new: Any?, composer: Composer)
    fun changedInstance(old: Any?, new: Any?, composer: Composer)

    // 跳过/重组决策通知
    fun skipping(skip: Boolean, composer: Composer)

    // State 失效通知 — "哪个 State 触发了重组" 的关键
    fun invalidate(scope: RecomposeScope, instance: Any?)

    // 重组作用域的开始/结束，instances 包含触发 State 集合
    fun composeStart(instances: Set<Any>?, scope: RecomposeScope)
    fun composeEnd(scope: RecomposeScope)

    // RestartGroup 生命周期
    fun startRestartGroupStart(key: Int, composer: Composer)
    fun startRestartGroupEnd(key: Int, composer: Composer)
    fun skipToGroupEnd(composer: Composer)
    fun endRestartGroup(composer: Composer)
}
```

注入方式：`RecompositionHandlerFactory.setProxy()` 全局注入，默认空实现（不注入时接近零开销）。

#### 修改的 Runtime 文件

| 文件 | 改动 |
|------|------|
| `Composer.kt` | 所有 `changed()` / `changedInstance()` / `skipping` / `startRestartGroup` / `endRestartGroup` 中插入 Handler 回调 |
| `Composition.kt` | `invalidate()` 中增加 `recompositionHandler.invalidate(scope, instance)` |
| `SnapshotState.kt` | 为 `MutableState<T>` 接口增加 `setSourceFile()` / `getSourceFile()` |
| 所有 State 实现类 | 添加 `private var sourceFile: String = ""` 字段 |

#### State → Composable 关联的完整路径

1. **State 失效** → `Composition.invalidate(scope, instance)` → Handler 的 `invalidate(scope, instance)` 被调用，`instance` 就是触发失效的 State 对象
2. **重组开始** → `Composer.recomposeToGroupEnd()` 中调用 `composeStart(invalidation.instances, scope)`，`instances` 是 `IdentityArraySet<Any>?`，包含**所有导致该 Scope 失效的 State 对象集合**
3. **参数变化** → `changed(old, new, composer)` 追踪每个参数的新旧值
4. **跳过决策** → `skipping(skip, composer)` 追踪重组/跳过

#### sourceFile 的局限

ovCompose 为 `MutableState<T>` 接口新增了 `setSourceFile()` / `getSourceFile()`，所有 State 实现类（`SnapshotMutableStateImpl`、`IntState`、`LongState` 等）都添加了 `sourceFile` 字段。

**但关键问题**：编译器插件（`ComposableFunctionBodyTransformer.kt`）**没有对应改动**来自动注入 sourceFile 信息。也就是说 `sourceFile` 永远是空字符串，除非用户手动调用 `state.setSourceFile("MyFile.kt:42")`。

这意味着 ovCompose 的 sourceFile 方案**设计了接口但未完成自动化**。

#### 与我们方案的对比

| 维度 | ovCompose RecompositionHandler | KuiklyUI CompositionObserver |
|------|-------------------------------|------------------------------|
| 实现方式 | 修改 Runtime 内部代码 | 使用 Runtime 公开 API |
| State→Scope 关联 | ✅ `composeStart(instances, scope)` | ✅ `invalidationMap: Map<RecomposeScope, Set<Any>?>` |
| 参数级变化检测 | ✅ `change(old, new)` | ❌ 不支持 |
| Skip 追踪 | ✅ `skipping(skip)` | ❌ 不支持 |
| sourceFile | 有接口但未自动化 | 无 |
| Runtime 侵入性 | ❌ 高（修改 Composer/Composition 等核心文件） | ✅ 零侵入 |
| 升级成本 | ❌ 高（Runtime 升级需重新 merge） | ✅ 低 |

**结论**：ovCompose 信息最完整（参数级别），但代价是深度侵入 Runtime。对于 State 源码位置追踪，ovCompose 也没有真正解决（sourceFile 未自动填充）。

### 可行方案（排除已淘汰方案后）

> 已排除：
> - ~~K/N 栈追踪~~：灾难性性能（每个 State 数百μs）
> - ~~namedStateOf 包装~~：需手动修改所有 `mutableStateOf`，侵入性太强

#### 方案 A: CompositionData SlotTable 遍历

**原理**：启用 `LocalInspectionTables`，遍历 `CompositionGroup` 树，找到 State 对象所在 group 的 `sourceInfo`。

**API 确认**（全部 commonMain，KMP 可用）：
- `CompositionData.compositionGroups: Iterable<CompositionGroup>` ✅
- `CompositionGroup.sourceInfo: String?` ✅ — 编译器注入的源码位置
- `CompositionGroup.data: Iterable<Any?>` ✅ — 包含 State 对象
- `LocalInspectionTables: CompositionLocal<MutableSet<CompositionData>?>` ✅

**sourceInfo 格式**：`"C(remember):File.kt#hashValue"` — 每个 `remember` 调用有独立 group。

**关键前提**：必须通过 `LocalInspectionTables` 启用 source info 收集，否则 sourceInfo 全是 null。

**性能考量**：
- `LocalInspectionTables` 启用后，每次组合都记录 source markers，有**持续性**性能开销
- 但遍历频率可控：**不需要每帧遍历**，可以只在 `getReport()` 时遍历一次，或定时遍历（如每秒一次）建立缓存
- State 变量数量问题：复杂页面可能有数百个 State，遍历整棵 CompositionGroup 树的内存和 CPU 开销需要实测评估

| 维度 | 评分 |
|------|------|
| API 可用性 | ✅ 全部 commonMain |
| 源码位置精度 | ⚠️ composable 函数名 + 文件名 + 偏移量（非行号） |
| 性能影响 | ⚠️ 启用 sourceInfo 收集有持续开销；遍历可控制频率 |
| 实现复杂度 | 中等 |
| 侵入性 | 低（只需提供 CompositionLocal） |

#### 方案 B: Snapshot readObserver（State→Composable 读取者映射）

**原理**：用 `Snapshot.observe(readObserver)` 追踪哪些 Composable 读取了哪些 State。

**效果示例**：
```
triggers=[State#1 (readers: CounterSection, AutoIncrementSection)]
```

不提供源码位置，但通过 reader Composable 名称间接帮助定位。

| 维度 | 评分 |
|------|------|
| API 可用性 | ✅ 全部 commonMain |
| 源码位置精度 | ❌ 无源码位置，只有读取者 Composable 名称 |
| 性能影响 | ✅ 低（readObserver 回调轻量） |
| 实现复杂度 | 低 |
| 侵入性 | ✅ 零侵入 |

#### 方案 C: identityHashCode 稳定 ID + 读取者映射

**原理**：用 `identityHashCode` 做 session 内稳定短 ID，结合 CompositionObserver 已有数据记录每个 State 被哪些 Composable 读取。

**效果示例**：
```
triggers=[State#1: MutableState<Int>(value=3), readers: CounterSection]
```

**API 确认**：
- `identityHashCode(instance: Any?): Int` — commonMain expect/actual，对象存活期间稳定 ✅

| 维度 | 评分 |
|------|------|
| 源码位置精度 | ❌ 无源码位置 |
| 稳定性 | ✅ 对象存活期间稳定 |
| 性能影响 | ✅ 极低 |
| 可读性提升 | ⚠️ 比内存地址好（短 ID + 类型 + 值），但无法定位到变量名 |

#### 方案 D: Runtime sourceFile 字段（ovCompose 模式）

**原理**：在 `MutableState<T>` 接口中增加 `sourceFile` 字段，通过编译器插件或手动调用设置。

**与 ovCompose 的区别**：ovCompose 只加了接口但编译器未自动注入。我们如果采用此方案，需要同时修改编译器插件。

| 维度 | 评分 |
|------|------|
| 源码位置精度 | ✅✅ 精确到文件名 + 行号（如果编译器注入） |
| 性能影响 | ✅ 极低（只在 State 创建时写一个字符串） |
| 实现复杂度 | ❌❌ 高（需修改 Runtime 接口 + 编译器插件） |
| 侵入性 | ❌❌ 高（修改 Runtime 核心接口和所有 State 实现类） |
| 升级风险 | ❌ 高（Runtime 升级时需重新 merge） |

### 方案对比总结

| 方案 | 源码位置 | 性能 | 侵入性 | 实现难度 |
|------|---------|------|--------|---------|
| **A: SlotTable 遍历** | ⚠️ 文件名+偏移量 | ⚠️ 启用 sourceInfo 有持续开销 | ✅ 低 | 中 |
| **B: readObserver** | ❌ 仅读取者名称 | ✅ 低 | ✅ 零侵入 | 低 |
| **C: identityHashCode + 读取者** | ❌ 稳定 ID + 读取者 | ✅ 极低 | ✅ 零侵入 | 低 |
| **D: Runtime sourceFile** | ✅✅ 精确 | ✅ 极低 | ❌❌ 高 | ❌❌ 高 |

### ⚠️ 已决定：方案 B+C（readObserver + identityHashCode 稳定 ID）

**决策日期**：2026-03-19

**选择理由**：
- 零 Runtime 侵入，性能开销极低
- 不提供源码位置，但通过稳定短 ID + 读取者 Composable 名称，已能有效辅助定位
- 通过配置开关控制（`enableStateIdentity`，默认关闭），不影响现有功能

**输出效果**：
```
// 开关关闭（默认）— 保持现有行为
triggers=[MutableState(value=3)@128220496]

// 开关开启 — 稳定 ID + 读取者 Composable 名称
triggers=[State#1(value=3), readers: CounterSection, StatusBar]
```

**已淘汰方案**：
- ~~方案 A（SlotTable 遍历）~~：`LocalInspectionTables` 带来持续性每帧开销
- ~~方案 D（Runtime sourceFile）~~：需改 Runtime + 编译器，侵入性太高
- ~~K/N 栈追踪~~：灾难性性能
- ~~namedStateOf~~：需手动修改所有 `mutableStateOf`

