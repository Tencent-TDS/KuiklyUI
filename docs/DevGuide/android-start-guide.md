# Kuikly页面启动性能分析指引

## **1. 背景**

部分接入Kuikly的业务希望进一步优化启动耗时，以及进行相关性能分析。针对这类启动性能问题本文将介绍一些初步分析及排查的思路。

## **2. 分析概述**

经过我们内部测试验证以及已有接入业务线上使用来看，只要正常使用，不存在框架问题导致影响启动性能情况。

如果出现相关耗时增长，很大概率是因为业务逻辑的不正常使用(如：数据读取加载阻塞UI创建、UI层级过于复杂且一次加载过多等）

## **3. 典型问题及其解决方案**

:::warning 注意
原则上，性能应在真机上用Release包进行验证，在进行对比分析前应做好确认！
:::


### 通用场景问题

Kuikly内置模式性能接近原生性能，如果业务逻辑实现合理，通常性能都是能满足需求的，但我们发现有些业务在使用过程中有许多使用不当的情况，而在动态化模式运行的时候更容易凸显出来。

- Pager或ComposeView生命周期函数调用被阻塞

  Pager生命周期调用中包括`created`，`pageDidAppear`，`viewWillLoad`，`viewDidLoad`等详情见[Pager生命周期](pager-lifecycle.md)以及[ComposeView生命周期](compose-view-lifecycle.md)，此外在页面创建的过程中`willInit`，`initModule`，`didInit`，`body`等也会被执行。为了保证启动的高性能，如果业务有这些函数的override实现，请确保其轻量快速。

    * 案例
      业务在`created`中调用了`loadIntialData`，而`loadInitialData`会等待所有请求完成才继续往下走，导致Pager的创建被阻塞。
        ```kotlin
        import com.tencent.kuikly.core.coroutines.GlobalScope

        override fun created() {
            super.created()
            // ...省略其他代码...
            loadInitialData()
        }

        private fun loadInitialData() {
            // ...省略其他代码...
            GlobalScope.launch {
                try {
                    val adsJob = async { requestADSSwitch() }
                    val recentListJob = async { requestRecentList() }
                    val activityJob = async { requestActivityList() }
                    val results = listOf(adsJob, recentListJob, activityJob).awaitAll()

                    // ...省略其他代码...
                }
            }
        }
        ```
    * 解决方案

      造成加载慢的原因是上述的GlobalScope.launch会立即执行，发起3个请求后awaitAll等待结果，因此核心修改是要避免awaitAll，这里以改造为异步回调方式避免阻塞加载生命周期。

        ```kotlin
        import com.tencent.kuikly.core.coroutines.GlobalScope

        
        override fun created() {
            super.created()
            // ...省略其他代码...
            loadInitialData()
        }

        private fun loadInitialData() {
            // ...省略其他代码...
            requestADSSwitch{
                // do something in completion callback
            };
            requestRecentList{
                // do something in completion callback
            }
            requestActivityList{
                // do something in completion callback
            }
            // ...省略其他代码...
        }
        ```
- 首屏异步拉取过多数据
  拉取的数据量过大，超过业务首屏实际需要的时候，由于数据下载、下载、传输、解析等各个环节都会有耗时的增加，所以它对业务首屏的影响是比较大的。

    * 案例

      业务在首屏启动的时候通过网络加载了300K左右的JSON数据，包含了第2、3屏的内容，且没有本地数据缓存，整体延迟较为明显。

    * 解决方案

        1. 优化数据请求逻辑，首屏仅拉取本身需要的数据，如有需要，可在首屏加载成功后再预加载2、3屏数据。
        2. 实现首屏数据本地缓存，避免首屏的显示强依赖网络，可先用本地缓存展示，拉到远端首评数据后再更新。

- 过度的日志输出

  在页面创建生命周期回调或者在首屏数据的处理过程中，适度的日志输出有一定的必要，但要控制频率，且避免输出过大的日志内容。

    * 案例

      业务在下载到网络数据的时候，对数据整体进行了打印。
        ```kotlin
        acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).requestPost(
            "https://example.com/example_service",
            JSONObject().apply { put("key", "value") }
        ) { data, success, errorMsg, response ->
            // ...省略...
            KLog.i("ExampleTag", data.toString())
        }
        ```

    * 解决方案

      对于远程返回的数据，数据量大小往往是不可控的，因此避免对数据进行整体打印。
        ```kotlin
        acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).requestPost(
            "https://example.com/example_service",
            JSONObject().apply { put("key", "value") }
        ) { data, success, errorMsg, response ->
            // ...省略...
            val str = data.toString();
            // 最多打印100个字符，适度调整长度。另外从隐私角度看，正式包中也许不宜输出这些数据，
            // 因此改为debug log，另外可考虑移除这个log打印。
            KLog.d("ExampleTag", if(str.length > 100) str.substring(0, 100) else str)
        }
        ```
- 同步Module调用过于耗时

  Kuikly Module调用支持同步方法，即调用此类方法的时候会等待同步返回，如果调用耗时较长，会导致Kuikly线程被卡住。

    * 案例

      业务在`created`中调用了自有的一个Module获取数据，而这个接口是一个耗时接口，导致了Pager的创建被卡住
        ```kotlin
        val data = acquireModule<BizDataModule>(BizDataModule.MODULE_NAME).getData()
        ```

    * 解决方案

        1. 优化`BizDataModule`的`getData()`调用，降低耗时
        2. 如果耗时无法降低，则将`getData()`调用变更为异步方式回调数据，调整业务逻辑适配异步实现

            ```kotlin
            val data = acquireModule<BizDataModule>(BizDataModule.MODULE_NAME).getData{
                processData(it)
            }
            ```

- attr block中放置过多业务逻辑问题

  attr block仅用于进行observable以及各种属性的更新/绑定，在observable有更新的时候会被执行，而如果业务实现上放了许多计算逻辑在attr中执行，则会导致这些逻辑同样被执行，应注意避免。

    * 案例

      业务在`created`中调用了自有的一个Module获取数据，而这个接口是一个耗时接口，导致了Pager的创建被卡住
        ```kotlin
        View {
                attr {
                    padding(myPadding)
                    val m = calculateMargin()
                    margin(m)
                    val r = calulateBorderRadius()
                    borderRadius(r)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color(0xFFFB8C00)))
                    allCenter()
                    val h = calulateHeight()
                    height(h)
                }
        }
        ```

    * 解决方案

      将计算逻辑从attr中移出，会变化的用observable，不变的则用普通变量
        ```kotlin
        val myMargin = calculateMargin() // 假设这个不会变化，可以初始化计算一次，用普通val成员即可
        var myBorderRadius by observable(0f) // 这个会根据需要进行改版，在需要变化的地方执行更新
        var myHeight by observable(0f) // 这个会根据需要进行改版，在需要变化的地方执行更新

        // call this when needed
        fun updateBorderRadius(){
            myBorderRadius = calulateBorderRadius()
        }

        fun updateHeight(){
            myHeight = calulateHeight()
        }

        View {
                attr {
                    padding(myPadding)
                    margin(myMargin)
                    borderRadius(myBorderRadius)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color(0xFFFB8C00)))
                    allCenter()
                    height(myHeight)
                }
        }
        ```

### JS动态化场景

动态化模式以JS方式运行，相比原生的有一定性能差距，在较为复杂的信息流应用场景测试对比中，动态化和Native的对比只差20%左右，如果业务实现得当，这个比例应该是接近的，如果偏差较大，应检查实现细节。

- JSON数据解析慢

    * 案例

      在Kuikly中业务可以这样进行JSON字符串的解析，在普通内置模式时候没有问题，但在JS动态化模式的时候往往容易有性能问题。
        ```kotlin
        val jsonStr = ... // a valid json string
        val jsonObj = JSONObject(jsonStr)
        val value = jsonObj.optString("key", "")
        ```
    * 解决方案

      上述片段在JS动态化中低效的原因是，JSONObject默认是用Kotlin实现的，而在JS引擎中，内置的JS解析能力才是最高效的，因此可以通过标志位让Kuikly优先使用JS引擎内置的JS解析能力。
      :::tip 注意
      **2.7.0以及更新版本中本选项默认为true，无需手动设置，使用低版本的业务建议升级至最新版本**
      :::
        ```kotlin
        // 在解析前将标志位设置为true，可尽早统一设置，至少要在解析前进行设置。
        JSON.useNativeMethod = true
        val jsonStr = ... // a valid json string
        val jsonObj = JSONObject(jsonStr)
        val value = jsonObj.optString("key", "")
        ```
- Range比较慢

    * 案例

      业务在scroll的时候通过IntRange的intersect进行了是否重叠判断，但发现滚动起来很慢。
        ```kotlin
        scroll {
            var index = 0
            val offsetX = it.offsetX
            val pageListWidth = it.viewWidth
            ctx.galleryList.forEach { item ->
                val itemLeft = index * pageListWidth
                val itemRange = itemLeft.toInt() .. (itemLeft + pageListWidth).toInt()
                val listRange = (offsetX).toInt() .. (offsetX + pageListWidth).toInt()
                val overlap = itemRange.intersect(listRange)
                val visiblePercentage = overlap.count() * 1f / pageListWidth
                item.transformScale1 = 0.85f + (1 - 0.85f) * visiblePercentage
                index++
            }
        }
        ```

    * 解决方案

      慢的原因是IntRange实际上会转为对象集合，当区间越大，集合中的Int类型对象越多，集合的重叠比较判断就会变得很慢，优化方案则是修改为数值比较代替集合重叠判断。
        ```kotlin
        scroll {
            var index = 0
            val offsetX = it.offsetX
            val pageListWidth = it.viewWidth
            ctx.galleryList.forEach { item ->
                val itemLeft = index * pageListWidth
                var lower: Int = max((itemLeft).toInt(), (offsetX ).toInt())
                var upper: Int = min((itemLeft+ pageListWidth).toInt(), (offsetX  + pageListWidth).toInt())
                var count = if(upper - lower >= 0) upper - lower + 1 else 0; // 计算两个区间重合区域
                val visiblePercentage = count * 1f / pageListWidth.toInt() // 重合区域除以宽度就是可见比例
                item.transformScale1 = 0.85f + (1 - 0.85f) * visiblePercentage
                index++
            }
        }

        ```

- 集合操作慢问题

  Kotlin的集合类是在语言层面自行实现的，而JS引擎有内置的List、Map、Set支持，因此为了最佳性能，最好要适配为使用JS引擎内置的集合能力。
  为此，Kuikly从1.9.0开始内置了对JS集合能力的支持，所以如果业务需要使用JS动态化模式，建议升级使用2.x，至少应升级到1.9.0以上。



## **4. 排查工具**

### Performance API  [启动耗时指标说明](https://kuikly.tds.qq.com/API/modules/performance.html#指标1-启动耗时)

记录页面启动到首帧渲染完成，各个阶段的耗时，可以使用进行初步的耗时记录

### 关键函数打点

对于一些觉得耗时的操作，可以进行打点耗时记录影响。

注：此处打点耗时建议 println(当前时间戳) ，避免相关Log异步后输出非实际的时间戳

### Android Profile工具

对于一些页面启动后或完整的流程记录，可以使用 `Profile` 工具进一步分析

`Androdi Studio`上提供了可以`trace`的工具，可以在页面打开前启动，并在内容显示后结束

`HRContenxtQueueHandlerThread`为`Kuikly`线程，可以看到在启动过程执行了什么任务，是否有任务影响耗时。


<div align="center">
<img src="./img/android_start_guide1.png" style="width: 60%; border: 1px gray solid">
</div>
<div align="center">
<img src="./img/android_start_guide2.png" style="width: 60%; border: 1px gray solid">
</div>

如果业务过程比较复杂，`Trace`过程太久导致文件过大,可能出现`Android Studio`打不开的情况，这类情况可以采取以下措施：

使用代码进行`Trace`文件记录，`Debug.startMethodTracing(fileName, bufferSize)` `Debug.stopMethodTracing()` 在代码需要关注的时间节点前后使用，最终`trace`文件会默认生成对应的目录，文件过大可以使用一些第三方工具进行打开，如：https://ui.perfetto.dev/
<div>
<img src="./img/android_start_guide3.png" style="width: 50%; border: 1px gray solid">
</div>

![Trace4](./img/android_start_guide4.png)

对于复杂的流程，建议先初步打点观察，后再对一些关键步骤进行`trace`的进一步分析

在Kuikly代码内，可以通过 `expect` 函数的接口并在 `AndroidMain` 实现 `trace` 函数的记录

```kotlin
// CommonMain
expect fun debugStart(fileName: String)
expect fun debugStop()

// androidMain

import android.os.Debug

actual fun debugStart(fileName: String) {
    val bufferSize = 1000 * 1024 * 1024; // 适当调大buffer
    Debug.startMethodTracing(fileName, bufferSize);
}

actual fun debugStop() {
    Debug.stopMethodTracing()
}

// 使用
debugStart("xxx")

debugStop()
```

### iOS Profile工具

在iOS平台上，如果希望页面代码耗时有详细的了解，可以利用instrument工具进行分析。

用Xcode打开iosApp.xcworkspace工程后，选中target（如iosApp）和设备后，通过`Command-I (⌘I) `快捷键或者`Product`-`Profile`菜单执行Profiling操作，并在随后的Instrument面板中选择`Time Profiler`；
随即Profiler启动，页面启动完或执行完业务期望的操作后，可点击工具栏按钮停止Profiler。

这时在Instruments中就记录好了callstack以及耗时信息，默认符号未还原，所以在开始分析前，还需要通过选中业务帧，点击右键，在出现的菜单中点击`Lcate dSYM...`关联符号。
<div>
<img src="./img/ios-time-profiler1.png" style="width: 50%; border: 1px gray solid">
</div>

关联符号后在函数耗时基础上可看到完整的符号,及其对应的文件、行号信息
<div>
<img src="./img/ios-time-profiler2.png" style="width: 50%; border: 1px gray solid">
</div>



### 鸿蒙 Profile工具

在鸿蒙平台上，如果希望页面代码耗时有详细的了解，可以利用Profiler工具进行分析。

用鸿蒙IDE加载工程跑起来后，点选底部的`Profiler`面板，选择设备、App进程，选中`Time`或者`Launch` Profiler，点击Create，然后点击小三角形启动Profiler。

<div>
<img src="./img/ohos-time-profiler1.png" style="width: 50%; border: 1px gray solid">
</div>

启动页面或者执行完预期中的操作后，点击停止按钮。
这时选中Callstack泳道即可浏览堆栈调用以及耗时情况，并可以看到完整的符号信息。
双击堆栈行可自动打开对应的Kotlin源文件。
<div>
<img src="./img/ios-time-profiler2.png" style="width: 50%; border: 1px gray solid">
</div>


## **5. 实际业务用例**

用例A：

业务部分代码块：
```
val data = initHomeData()
performTaskWhenRenderViewDidLoad {           
	saveCache(data)
	firstLoadEnd = true
	preloadSecondPage()
}
```

背景：在首屏加载过程中，会请求数据，并在数据到来后，希望使用 `performTaskWhenRenderViewDidLoad` 异步缓存数据，但发现过程中会白屏阻塞一段时间。

分析：页面对 `trace` 分析发现，发现缓存数据的操作并没有按照预期在下一个时间片执行， 并且缓存存储采用的是同步方法，数据量也比较大，缓存数据过程阻塞了相关UI的创建。

原因：使用`performTaskWhenRenderViewDidLoad` 是在 `renderView` 还没创建的时候才有效，但在使用过程中`renderView`已经创建，所以并没有按照预期在下个时间片执行。

解决方案：

- 使用 `addNextTickTask` 或者在相关的 View 的回调在进行缓存存储，避免影响视图的创建和加载。


用例B：

业务B发现数据回包，更新`observable`过程耗时比较长，由于业务复杂，不确定此过程的相关操作，并期望有相关优化空间

```
println(t1)

observableA = newValue

println(t2)
```

做法：

对该`observable`的前后进行`trace`操作

```
debugStart("updateValue")

observableA = newValue

debugStop()
```

原因：发现过程更新`observable`会触发一个新的`View`创建，进一步的该`View`使用`Scroll`用于`PageList`的内容，所有`Item`都一并上屏渲染计算

![Trace5](./img/android_start_guide5.png)

解决方案：

由于`Scroll`会全部加载渲染，所有`Item`都会创建渲染，导致了耗时增加，此处使用 `List/PageList` 控制数量可以有明显的改善。