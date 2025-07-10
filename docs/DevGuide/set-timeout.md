# 执行延时任务

在下一帧执行任务或者执行延时任务是一项非常常见的诉求，在``Android``中，你可以使用Handler来执行延时任务；在``iOS``中，你可以使用``GCD``执行延时任务。
<br/>
而在``Kuikly``中，你可以使用``setTimeout``方法来执行延时任务。举个例子

```kotlin{12-14}
@Page("HelloWorldPage")
internal class HelloWorldPage : Pager() {

    lateinit var listViewRef: ViewRef<ListView<*, *>> // 1.声明ListView类型的ViewRef变量

    override fun body(): ViewBuilder {
        ...
    }

    override fun created() {
        super.created()
        setTimeout(2 * 1000) {
            // 延迟2s执行任务
        }
    }
}
```

## 下一步

接下来, 我们来学习在``Kuikly``中如何[使用Module调用Native方法](module.md)