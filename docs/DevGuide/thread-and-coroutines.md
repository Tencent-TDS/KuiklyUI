# 协程和多线程编程指引

在本章节，我们将学习如何在Kuikly中进行异步编程，讨论如何选择合适的异步编程方案。

## 概念和术语

异步编程涉及的概念和术语比较广泛，本小节将介绍一些核心概念和术语，如果你已经熟悉这些概念，可以跳过本小节。

### Kuikly线程
Kuikly UI执行的线程，所有Kuikly的UI操作（View、Attr、Event、observable、setTimeout等）都**只能在Kuikly线程调用**。

### Kuikly Module机制
将Native的API暴露给kuikly使用的方案，可实现在Native的API中使用不同平台的多线程能力。
:::tip 提示
具体可以参考[Kuikly Module机制](expand-native-api.md)。
:::

### KMP的expect和actual机制
`expect`和`actual`是KMP的核心机制，用于定义和实现跨平台的接口，从而使用不同平台的多线程能力（以下简称**KMP多线程**）。

### 协程
协程是轻量级的并发单元，由程序逻辑控制调度而非操作系统，适用于异步任务和非阻塞操作。
协程的两个主要功能：**1. 替代回调地狱，简化异步代码；2. 执行并发任务。**

### 挂起函数（Suspend Function）
用 suspend 关键字标记的函数，可在不阻塞线程的情况下暂停和恢复执行。
**挂起函数是 Kotlin 语言的核心特性**，是协程语法的基础，在所有目标平台均可使用。
挂起函数只能在协程或其他挂起函数中调用。示例：
```kotlin
suspend fun fetchData(): String {
    delay(1000)
    return "data"
}
```

### Kuikly协程API和依赖库

要在Kuikly中使用协程，首先需要了解以下三种API和依赖库：

#### Kuikly CoroutineScope API
CoroutineScope 是协程的上下文，用于执行挂起函数。Kuikly框架提供了自己的 CoroutineScope API（以下简称**Kuikly内建协程**），包括`GlobalScope`和`Pager.lifecycleScope`。

Kuikly内建协程是在Kuikly线程中执行的，不会有线程切换的开销，也不会有线程安全问题，支持在动态化场景中使用。示例：
```kotlin
GlobalScope.launch {
    val data = fetchData()
    ...
}
```

:::tip 备注
「不会有线程安全问题」是指Kuikly内建协程始终在Kuikly线程中执行，不会遇到线程安全问题，需要注意的是，Kuikly内建协程API本身非线程安全，因此不能在Kuikly线程外调用。
:::

#### kotlinx.coroutines库
`kotlinx.coroutines`库是 Kotlin 官方提供的协程库（以下简称**kotlinx协程**），定义了协程的上下文（CoroutineScope）和调度器（Dispatchers）API，并在不同目标平台提供了不同的实现。应用也可以自定义自己的协程作用域和调度器。

通过调度器指定协程运行的线程或线程池。示例：
```kotlin
GlobalScope.launch {
    println("running in default dispatcher")
    withContext(Dispatchers.Main) {
        println("running in Main dispatcher")
    }
}
```
:::tip 提示
不同平台支持的调度器有所不同，例如，除了各平台共有的`Dispatchers.Default`和`Dispatchers.Unconfined`，Android平台还提供了`Dispatchers.Main`、`Dispatchers.IO`等。
具体可以参考[kotlinx.coroutines的API文档](https://kotlinlang.org/api/kotlinx.coroutines/)。
:::

#### kuiklyx.coroutines库
前面提到，Kuikly UI操作都只能在Kuikly线程调用。`kuiklyx.coroutines`库（以下简称**kuiklyx协程**）提供了切换到kuikly线程的能力，当我们在非Kuikly线程执行异步任务后，就可以通过`kuiklyx协程`切换到Kuikly线程进行UI操作。
```kotlin
private suspend fun fetchData() = withContext(Dispatchers.Main) {
    // 在Main线程执行异步任务
    return@withContext "mock"
}

override fun created() {
    super.created()
    val ctx = this
    GlobalScope.launch(Dispatchers.Kuikly[ctx]) {
        // 调用挂起函数fetchData
        val data = ctx.fetchData()
        // 切换到Kuikly线程更新响应式字段
        ctx.dataObservable = data
    }
}
```
:::tip kuiklyx协程使用方法
* 添加依赖（[查看KUIKLYX_COROUTINES_VERSION最新版本号](https://repo1.maven.org/maven2/com/tencent/kuiklyx-open/coroutines/)）
```kotlin
dependencies {
    implementation("com.tencent.kuiklyx-open:coroutines:$KUIKLYX_COROUTINES_VERSION")
}
```
* 协程方式API
```kotlin
// case 0: js模式使用前需要触发KuiklyContextScheduler初始化，其它模式可忽略
KuiklyContextScheduler
// case 1: 启动协程
GlobalScope.launch(Dispatchers.Kuikly[ctx]) { ... }
// case 2: 在协程中切换上下文
withContext(Dispatchers.Kuikly[ctx]) { ... }
```
* 回调方式API
```kotlin
KuiklyContextScheduler.runOnKuiklyThread(pagerId) { cancel ->
    if (cancel) {
        // pager is destroyed
        return
    }
    // do something
}
```
:::

## Kuikly异步编程介绍

**协程**和**多线程**是Kuikly异步编程的两个维度，它们可以组合出不同的异步编程方式，每种方式都有其适用的场景和优缺点。

多线程实现方式：

|             | Module机制 | KMP多线程     |
|-------------|----------|------------|
| 动态化         | 支持       | 不支持        |
| 依赖库包增量      | 无        | kuiklyx协程库 |
| 切换到Kuikly线程 | 框架自动完成   | 需要主动切换     |
| 通信开销        | 有        | 无          |

协程实现方式：

|        | 回调（无协程） | Kuikly内建协程 | kotlinx协程  |
|--------|---------|------------|------------|
| 动态化    | 支持      | 支持         | 不支持        |
| 依赖库包增量 | 无       | 无          | kotlinx协程库 |
| 线程安全   | 不涉及     | 自动保障       | 需要考虑       |

## 选择合适的异步编程方式

### 方式1：Module机制和（或）Kuikly内建协程

#### 场景一
**需要使用协程语法代替回调，提升代码可读性，没有多线程诉求**：建议使用Kuikly内建协程。

示例：
```kotlin
private suspend fun fetchLocal(): Int {
    return suspendCoroutine { ... } // 把回调式API转换为挂起函数
}

private suspend fun fetchRemote(type: Int): String {
    return suspendCoroutine { ... } // 把回调式API转换为挂起函数
}

override fun created() {
    super.created()
    val ctx = this
    lifecycleScope.launch { // 通过Pager.lifecycleScope启动协程
        val type = fetchLocal() // 调用挂起函数fetchLocal
        val data = fetchRemote(type) // 调用挂起函数fetchRemote
        ctx.dataObservable = data // 更新响应式字段
    }
}
```

#### 场景二
**需要执行耗时任务，同时需要支持动态化**：建议使用Module机制，将耗时任务放到平台侧通过原生能力实现。

示例：
```kotlin
override fun created() {
    super.created()
    val ctx = this
    ctx.loadingObservable = true
    val module = acquireModule<FetchDataModule>(FetchDataModule.MODULE_NAME)
    module.fetchData {
        ctx.dataObservable = it
        ctx.loadingObservable = false
    }
}
```
:::tip 提示
* `FetchDataModule`是通过Module机制预先实现的原生扩展API
* Module机制可以配合场景一的Kuikly内建协程使用
:::

### 方式2: KMP多线程和Kuiklyx协程库（回调方式）
#### 场景三
**需要执行耗时任务，对通信开销要求较高，不考虑动态化，跨端逻辑相对简单不需要协程能力**：建议使用KMP多线程自行切换线程，再通过kuiklyx协程库（回调方式）回到Kuikly线程更新UI。

示例：

先在`build.gradle.kts`添加依赖库：
```kotlin
val commonMain by getting {
    dependencies {
        // kuiklyx协程库
        implementation("com.tencent.kuiklyx-open:coroutines:$KUIKLYX_COROUTINES_VERSION")
    }
}
```
:::tip 提示
`KUIKLYX_COROUTINES_VERSION`需要根据项目使用的kotlin版本，替换为对应的版本号
:::

然后在Kuikly页面中使用：
```kotlin
override fun created() {
    super.created()
    val ctx = this
    ctx.loadingObservable = true // 更新loading状态
    // 调用KMP方法
    asyncKmpFetchData { data ->
        KuiklyContextScheduler.runOnKuiklyThread(ctx.pagerId) { cancel ->
            if (cancel) {
                return
            }
            // 回到Kuikly线程，更新响应式字段
            ctx.dataObservable = data
            ctx.loadingObservable = false
        }
    }
}
```

### 方式3：Kotlinx协程库和Kuiklyx协程库（协程方式）

#### 场景四
**需要使用多线程能力，对通信开销要求较高，不考虑动态化，同时需要协程语法提升代码可读性**：建议使用kotlinx协程，再通过kuiklyx协程库（协程方式）回到Kuikly线程更新UI。

示例：

先在`build.gradle.kts`添加依赖库：
```kotlin
val commonMain by getting {
    dependencies {
        // kotlinx协程库
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$KOTLINX_COROUTINES_VERSION")
        // kuiklyx协程库
        implementation("com.tencent.kuiklyx-open:coroutines:$KUIKLYX_COROUTINES_VERSION")
    }
}
```
:::tip 提示
`KOTLINX_COROUTINES_VERSION`和`KUIKLYX_COROUTINES_VERSION`需要根据项目使用的kotlin版本，替换为对应的版本号
:::

然后在Kuikly页面中使用：
```kotlin
override fun created() {
    super.created()
    val ctx = this
    // 使用kotlinx协程库启动协程，通过kuiklyx协程库切换到Kuikly线程
    GlobalScope.launch(Dispatchers.Kuikly[ctx]) {
        ctx.loadingObservable = true // 更新loading状态
        val data = withContext(Dispatchers.IO) { // 在IO线程调用KMP的方法
            kmpFetchData()
        }
        // 回到Kuikly线程，更新响应式字段
        ctx.dataObservable = data
        ctx.loadingObservable = false
    }
}
```
:::tip 提示
* `Dispatchers.Kuikly`和`Pager`相关，需要通过`Pager`上下文获取
* `Dispatchers.IO`是通过KMP方式实现的自定义调度器
* KMP多线程不支持动态化，是因为动态化需要的js目标平台不支持多线程
:::

### 更多方式

上述列举了3种常用方式和对应的场景示例，实际开发中，还可以组合出更多的异步编程方式，此处不再赘述。

## 关于线程安全
* KMP多线程需要开发者自行考虑线程安全问题，可以借助`kotlinx:atomicfu`库提供的原子操作和同步锁能力；
* Kuikly UI的相关类（View、Attr、Event、ObservableProperties、GlobalFunctions等）非线程安全，且只能在Kuikly线程访问。
