# 鸿蒙KN Crash监控和上报指引

## 1.编译选项设置


在build.gradke.kts中为鸿蒙设置`add-light-debug=enable`选项，使鸿蒙KN产物中保留调试信息。

```kotlin
kotlin {
    ohosArm64 {
        binaries.sharedLib("shared") {
            // ... 省略其他选项 ...
            freeCompilerArgs += "-Xadd-light-debug=enable"
        }
    }
}
```

Kuikly默认会在调用Kotlin逻辑时为用户catch异常，但这只是历史默认行为，目前强烈建议通过把catchException设置为false禁用此逻辑。catchException标志位仅在2.14.0+中生效，请注意升级版本。

```kotlin
ksp {
    arg("catchException", "false")
}
```

## 2.注册Kotlin UnhandledExceptionHook并对接上报

在Kotlin中注册UnhandledExceptionHook，在初始化阶段尽早调用，并对接Crash监控服务进行上报。
UnhandledExceptionHook的注册方法参考以下代码：

``` kotlin
object ExceptionHookManager {
    @OptIn(ExperimentalNativeApi::class)
    fun registerUnhandledExceptionHook(reportOperation: (Array<JSValue>) -> Unit) {
        var old: ReportUnhandledExceptionHook? = null
        old = setUnhandledExceptionHook {
            val errorName = it::class.qualifiedName ?: "unknown"
            val message = it.message ?: "unknown"
            val callstack = it.getStackTraceForReport()

            // 
            // 对接crash监控服务进行上报
            //
            
            old?.invoke(it)
            terminateWithUnhandledException(it)
            // 注：有的业务用throw it代替terminateWithUnhandledException(it)，
            // 虽然在当前版本行为也符合预期，但还是推荐按官方文档建议使用terminateWithUnhandledException(it)
        }
    }
}
```
