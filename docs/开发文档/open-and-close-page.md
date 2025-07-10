# 打开和关闭Kuikly页面

在``Kuikly``中, 你可以通过``Kuikly``内置的``RouterModule``来实现页面的打开和关闭

## 打开新的Kuikly页面

使用``RouterModule.openPage``方法, 可以打开一个新的``Kuikly``页面

```kotlin{24-26}
@Page("firstPage")
internal class FirstPage : Pager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            Button {
                attr {
                    size(200f, 50f)
                    titleAttr {
                        text("点击跳转到第二个页面")
                        fontSize(16f)
                        color(Color.WHITE)
                    }
                    backgroundColor(Color.GRAY)
                }

                event {
                    click {
                        ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage("secondPage", JSONObject().apply {
                            put("fromFirstPage", "heihei")
                        })
                    }
                }
            }
        }
    }

}

@Page("secondPage")
internal class SecondPage : Pager() {

    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Text {
                attr {
                    fontSize(16f)
                    color(Color.BLACK)
                    text(pagerData.fromFirstPage)
                }
            }
        }
    }

}

internal val PageData.fromFirstPage: String
    get() {
        return params.optString("fromFirstPage")
    }
```

在上述代码中，我们在``FirstPage``中添加了一个按钮, 然后监听按钮的点击事件。在按钮被点击时, 我们调用了``RouterModule.openPage``，并传入``pageName``和传递给下一个页面的数据，以此来打开一个新的Kuikly页面

## 关闭当前Kuikly页面

使用``RouterModule.closePage``方法, 可以关闭当前页面

```kotlin
@Page("secondPage")
internal class SecondPage : Pager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            Text {
                attr {
                    fontSize(16f)
                    color(Color.BLACK)
                    text(pagerData.fromFirstPage)
                }
                
                event { 
                    click { 
                        ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                    }
                }
            }
        }
    }

}

internal val PageData.fromFirstPage: String
    get() {
        return params.optString("fromFirstPage")
    }
```

在上述代码中, 我们监听了Text组件的点击事件，然后在点击事件中，我们调用了``RouterModule.closePage``方法来关闭当前页面

## 下一步

学习如何打开和关闭``Kuikly``页面后, 下面我们继续来学习在``Kuikly``中如何[发送事件通知](notify.md)

