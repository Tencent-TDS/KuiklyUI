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

## 使用Kuikly多线程与协程

Kuikly基于Kotlin实现，可以使用Kotlin的协程（Coroutines）来进行多线程开发，关于协程的详细介绍可参见[官方文档](https://kotlinlang.org/docs/coroutines-overview.html)。要在Kuikly环境下使用Kotlin的协程来实现多线程编程，主要有两步：
1. 接入Kotlin协程库kotlinx.coroutines，以使用协程相关API
2. 接入Kuikly基于kotlinx.coroutines的扩展库kuiklyx.coroutines，用于从其他线程切换到Kuikly线程

:::tip 注意
如果编译成js产物执行（动态化场景），受限于JS引擎的单线程执行方式，无法实现真正的多线程并行执行。
:::


### Kotlin协程库接入
Kotlin官方提供了kotlinx.coroutines协程库，支持多平台，它提供了丰富的API和工具来简化协程的使用。可以在gradle文件中配置依赖以接入kotlinx.coroutines。

#### iOS & Android

官方协程库支持在Android、iOS framework模式使用完备的多线程协程能力：

```gradle
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:VERSION")
}
```
:::tip 注意
结合[官方协程库](https://github.com/Kotlin/kotlinx.coroutines/releases)以及当前Koltin版本确定VERSION字段
:::

#### 鸿蒙

如需在鸿蒙平台上使用协程，可使用基于官方协程库的鸿蒙支持版本：

```gradle
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-KBA-002")
}
```

该扩展协程库位于以下maven源
```gradle
maven("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
```

kotlinx.coroutines库的具体语法和使用方式可参考[官方文档](https://kotlinlang.org/docs/coroutines-guide.html)。


### Kuikly扩展库接入
需要注意的是，对Kuikly的操作必须在Kuikly的Context线程中执行。如果调用Kuikly的API时处于其它线程，需要切换回Kuikly线程再进行调用。为此提供了一个扩展库，封装了切换到Kuikly线程的功能：

#### iOS & Android

```gradle
dependencies {
    implementation("com.tencent.kuiklyx-open:coroutines:1.5.0-2.0.21")
}
```

#### 鸿蒙

如果需要在鸿蒙上使用该切换到Kuikly线程的协程库，需要使用特定版本：

```gradle
dependencies {
    implementation("com.tencent.kuiklyx-open:coroutines:1.5.0-2.0.21-ohos")
}
```

#### 使用方式

该协程库可以和官方协程库配合使用，用于从其他线程切换到Kuikly线程，主要有以下两种使用方式。

1. 协程方式
```kotlin
// 在其他线程执行代码
doSomething()

KuiklyContextScheduler // js模式使用前需要触发KuiklyContextScheduler初始化，其它模式可忽略

// 使用withContext切换线程，ownerPager为pagerId的持有者，可以直接传入当前Pager类
withContext(Dispatchers.Kuikly[ownerPager]) {
    // 在 Kuikly 线程执行代码，如调用module方法、更新UI
    KLog.i("LogModule", "call native log method in kuikly thread")
}
```

2. 回调方式
```kotlin
// 在其他线程执行代码
doSomething()

KuiklyContextScheduler.runOnKuiklyThread(pagerId) { cancel ->
    if (cancel) {
        // pager is destroyed
        return
    }
    // 在 Kuikly 线程执行代码，如调用module方法、更新UI
    KLog.i("LogModule", "call native log method in kuikly thread")
}
```
