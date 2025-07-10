# 组件引用ViewRef

在编写``Kuikly``页面时，你可能会有调用组件的公开方法的诉求, 此时你可以声明一个``ViewRef``变量，然后在声明组件时，调用``ref{}``闭包来初始化``ViewRef``。
有了``ViewRef``以后，你就可以调用组件的内部方法。举个例子: 我想调用List组件内部的setContentOffset方法

```kotlin
@Page("HelloWorldPage")
internal class HelloWorldPage : Pager() {
    
    lateinit var listViewRef: ViewRef<ListView<*, *>> // 1.声明ListView类型的ViewRef变量

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            
            event { 
                click { 
                    // 3.点击的时候, 调用List组件内的setContentOffset方法
                    ctx.listViewRef.view?.setContentOffset(0f, 100f)
                }
            }

            List { 
                ref { 
                    ctx.listViewRef = it // 2.在ref{}闭包内初始化ViewRef
                }
                attr { 
                    flex(1f)
                }
            }
        }
    }
```

在上述代码中，我们首先定义了一个类型为``ViewRef``变量，然后再List组件的``ref{}``闭包内初始化``ViewRef``, 最后在点击的时候，通过``ViewRef``拿到ListView，接着调用其setContentOffset方法

## 下一步

学习完``Kuikly``中的``ViewRef``后，我们接下来学习``Kuikly``中的[组合组件ComposeView](compose-view.md)