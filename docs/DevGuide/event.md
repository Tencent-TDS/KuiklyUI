# 组件事件event

在``Kuikly``中, 我们可以在组件的声明式API内, 调用``event{}``闭包来监听组件的事件。每个组件都支持**单击**、**双击**和**长按**等基础事件。
组件支持的基础事件列表, 可移步[组件基础事件](../API/components/basic-attr-event.md#基础事件)查阅。

## 例子

下面以单击事件为例, 来看如何监听组件事件

```kotlin{18-22}
@Page("HelloWorldPage")
internal class HelloWorldPage : Pager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            Text {
                attr {
                    text("Hello Kuikly")
                    fontSize(20f)
                    fontWeightBold()
                }
                
                event { 
                    click { clickParams -> 
                        // 单击事件处理
                    }
                }
            }
        }
    }
}
```

在上述代码中, 我们在组件的``event{}``内调用``click``方法来监听``Text``组件的单击事件。

## 下一步

学习完如何监听组件的事件后, 下一步我们来学习Kuikly的[UI响应式更新](reactive-update.md)