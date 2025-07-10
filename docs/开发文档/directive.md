# 语句指令

在``Kuikly``中，我们定义了几个语句指令，以实现语句的响应式更新目的（例如在[UI响应式更新](reactive-update.md)的例子中, 我们使用到的``vfor``语句指令，他接收一个返回可观测容器的闭包，来实现响应式创建``Item``）。

本章节，我们将逐一学习这些语句指令的功能和使用方法。

## 循环语句指令

### vfor

``vfor``语句指令，是一个循环语句，它与kotlin语言的for语句的区别是，``vfor``语句具有UI响应式更新UI的功能。我们在使用vfor时，需要给它传递返回响应式容器的**闭包**,
这样当响应式容器更新时, 会自动触发``vfor``语句内的UI新建。举个例子:

```kotlin{26,27}
@Page("HelloWorldPage")
internal class HelloWorldPage : Pager() {

    private var list by observableList<String>()

    override fun created() {
        super.created()
        // mock data
        for (i in 0 until 10) {
            list.add(i.toString())
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            List {
                attr {
                    flex(1f)
                }

                vfor({ ctx.list }) { item -> // 1
                    Text { // 2
                        attr {
                            text(item)
                            fontSize(20f)
                            fontWeightBold()
                        }
                    }
                }
            }
        }
    }
}
```

在上面例子中, 我们在``List``组件下，使用了``vfor``循环语句指令, 语句中我们传递了一个返回响应式容器的闭包, 即``{ ctx.list }``, 然后在循环内，我们创建了``Text``组件。

这样当运行时，List组件下会带有10个Text item。然后如果我们后面往数据``list``中添加数据, 例如:

```kotlin
list.add("11")
```

此时``KuiklyCore``会监测到list容器内元素有新增, 会自动调用vfor语句下的闭包创建第11个item。值得一提的时，当数据``list``容器元素个数发生变化时，``KuiklyCore``只会
进行增量的更新, 以达到最高的更新性能。

:::tip 注意
* 在使用vfor语句指令时, 我们需要传递带有返回响应式容器的**闭包**, 如vfor({ list })
* 如果需要获取item index，可以使用下文介绍的vforIndex语句指令
:::

### vforIndex

``vforIndex``语句指令是``vfor``的补充，它与``vfor``的区别是，``vforIndex``语句指令的**闭包**除了列表元素外，还会有``index``和``count``两个参数，用于表示当前循环的索引值。举个例子:

```kotlin
vforIndex({ ctx.list }) { item, index, _ ->
    View {
        attr {
            margin(5f)
            backgroundColor(if (index % 2 == 0) Color.GRAY else Color.TRANSPARENT)
            borderRadius(5f)
        }
        Text {
            attr {
                text(item)
            }
        }
    }
}
```

:::tip 注意
* 在数据更新时，只有发生增/删/改的元素会触发UI更新，而数据没有发生变化的元素（即使``index``和``count``已经改变）是不会更新的。因此使用vforIndex时，需要注意更新数据后不能改变存量数据的``index``和``count``的有效性
* 如果仅仅是少量元素由于``index``或``count``的改变需要刷新，有个小技巧可以通过显式调用list的set方法来触发UI更新，例如：
```kotlin
private inline fun <reified T> ObservableList<T>.notifyUpdate(index: Int) {
    // 显式调用响应式对象的set方法，触发更新，可读性差，慎用
    this[index] = this[index]
}

ctx.list.notifyUpdate(ctx.lastCount - 1) // 触发最后一个元素的更新
```
:::

### vforLazy

``Kuikly``在显示列表(List)组件时，会全量创建列表元素的虚拟节点（例如``View{...}``对应的``DivView``、``Text{...}``对应的``TextView``，如果想了解详情，可以阅读Core源码``DeclarativeBaseView``类），用于计算排版信息，再根据列表的滚动位置动态创建和销毁列表元素对应的平台View。
一般来说，虚拟节点的内存开销远小于平台View，因此动态创建和销毁平台View即可在满足大部分场景的性能需求。 但仍有一些极端场景：
1. 元素UI非常复杂，仅虚拟节点就有较大的内存开销
2. 列表元素数量非常多，全量参与计算排版会带来较大的CPU开销
3. 应用场景对内存占用有较苛刻要求

``vforLazy``用于解决上述问题，它与``vfor``的区别是，``vforLazy``会动态增删可见范围外的虚拟节点，而不是全量创建虚拟节点。

:::warning 注意
* 与``vfor``和``vforIndex``不同，``vforLazy``仅可在List组件中使用（且**不包括**List的子类PageList、WaterfallList等）
* 由于``vforLayz``动态增删虚拟节点的特性，会带来额外的性能开销，因此建议仅在符合上述条件的场景下使用。
:::

示例代码：
```kotlin
override fun created() {
    super.created()
    // mock data
    for (i in 0 until 5000) {
        list.add(i.toString())
    }
}

override fun body(): ViewBuilder {
    val ctx = this
    return {
        attr {
            backgroundColor(Color.WHITE)
        }

        List {
            attr {
                flex(1f)
            }

            vforLazy({ ctx.list }) { item, index, count ->
                ...
            }
        }
    }
}
```

## 条件语句指令

在``Kuikly``中，vif, velseif, velse条件语句指令，它与kotlin语言的if, elseif和else语句的区别是，vif, velseif, velse条件语句指令具有响应式更新UI的功能

<br/>

我们在使用条件指令语句时，需要给它传递返回Boolean值的**闭包**, 并且闭包中使用到了响应式字段。

### vif

我们以例子来看vif语句指令的作用

```kotlin
@Page("HelloWorldPage")
internal class HelloWorldPage : Pager() {

    private var uiState by observable(0) // 声明一个代表UI状态的响应式字段

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            View {
                attr {
                    flex(1f)
                }

                event {
                    click {
                        ctx.uiState++ // 点击更新响应式字段
                    }
                }

                vif({ ctx.uiState == 1 }) { // 当满足 ctx.uiState == 1时, 创建Text组件, 否则移除Text组件
                    Text {
                        attr {
                            text("in vif")
                            fontSize(20f)
                            fontWeightBold()
                        }
                    }
                }
            }
        }
    }
}
```

在上述代码中, 一开始``uiState``字段的值为0, 因此在vif语句指令中的条件为false, 所以一开始是不会创建vif语句中包裹的Text组件的；而当我们点击Pager时,
更新响应式字段``uiState``，它的值变为1, 此时``uiState == 1``的条件为true，所以会创建Text组件；当再次点击时，``uiState``的值为2，此时``uiState == 1``条件为false,
此时会移除Text组件。

### velseif

velseif语句指令是与vif语句指令配套使用的, 用于当vif条件语句返回false时，进入velseif语句指令判断。举个例子

```kotlin
@Page("HelloWorldPage")
internal class HelloWorldPage : Pager() {

    private var uiState by observable(0)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            View {
                attr {
                    flex(1f)
                }

                event {
                    click {
                        ctx.uiState++
                    }
                }

                vif({ ctx.uiState == 1 }) {
                    Text {
                        attr {
                            text("in vif")
                            fontSize(20f)
                            fontWeightBold()
                        }
                    }
                }
                velseif( {ctx.uiState == 2 }) {
                    View { 
                        attr { 
                            size(100f, 100f)
                            backgroundColor(Color.GREEN)
                        }
                    }
                }
            }
        }
    }
}
```

在上面的例子中, 我们在vif之后，使用了velseif条件语句指令。当``uiState``值从1变为2时, 此时``uiState == 1``为false，``uiState == 2``为true，
因此会匹配到velseif语句指令, 因此会先删除Text组件，接着创建一个带下为100✖️100的绿色矩形View。

### velse

velse语句指令是与vif语句指令配套使用的, 用于当vif或者velseif条件语句返回false时，兜底进入velse语句。

## 绑定语句指令

### vbind

vbind语句，可类比为kotlin语言的when语句, 它接收一个返回``Any``类型的闭包, 闭包内必须使用到**响应式字段**, 当闭包返回值改变时, ``vbind``指令会将使用到**vbind指令的组件的所有孩子移除**, 然后重新运行vbind下的闭包创建UI，例如:

```kotlin{19,20}
@Page("HelloWorldPage")
internal class HelloWorldPage : Pager() {

    private var uiState by observable(0)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            View {
                attr {
                    flex(1f)
                }

                event {
                    click {
                        ctx.uiState++
                    }
                }

                vbind({ ctx.uiState }) {
                    when (ctx.uiState) {
                        1 -> {
                            Text {
                                attr {
                                    text("in vif")
                                    fontSize(20f)
                                    fontWeightBold()
                                }
                            }
                        }
                        2 -> {
                            View {
                                attr {
                                    size(100f, 100f)
                                    backgroundColor(Color.GREEN)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

在上述代码中, 我们在vbind语句指令中绑定了``{ctx.uiState}``闭包, 因此当闭包内的返回值改变时，即当``uiState``的值发生改变时，vbind指令会先删除**调用vbind指令的组件下的所有子组件**, 然后重新运行``vbind``下的闭包重新创建UI。
当``uistate``的值为1时，创建一个Text组件；当``uiState``的值变为2时, 先删除调用``vbind``指令的组件的所有子组件，即删除Pager下的所有子组件(删除Text组件)，然后重新运行``vbind``的右闭包创建UI，即创建一个View组件。

## 下一步

学习完``Kuikly``的语句指令后，你已经能使用``Kuikly``完成大部分的UI编写功能了。下一步，我们来学习Kuikly中的[组件引用ViewRef](view-ref.md)