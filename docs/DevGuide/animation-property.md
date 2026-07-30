# 可动画属性

在``Kuikly``支持对以下属性做动画

1. transform动画: 可以对组件进行**位移**、**旋转**和**缩放**动画
2. opacity动画: 可以对组件的**透明度**进行动画
3. backgroundColor动画: 可以对组件的**背景颜色**进行动画
4. frame动画: 可以对组件的**位置和大小**进行动画

在编写``Kuikly``动画时，我们一般会先对组件的属性设置**一个起始态值**，然后通过**响应式字段**来控制组件动画的**终止态值**，至于**动画时间和插值器类型我**们通过``animate``方法来配置。

举个例子，我们先来看如何使用Kuikly进行opacity动画

## opacity动画

::: tabs

@tab:active 示例

```kotlin{4,17-22,31}
@Page("1")
internal class TestPage : BasePager() {

    private var opacityAnmationFlag by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            View {
                attr {
                    size(150f, 150f)
                    backgroundColor(Color.GREEN)
                    if (ctx.opacityAnmationFlag) {
                        opacity(0f)
                    } else {
                        opacity(1f)
                    }
                    animate(Animation.linear(0.5f), ctx.opacityAnmationFlag)
                }
            }
        }
    }

    override fun created() {
        super.created()
        setTimeout(500) {
            opacityAnmationFlag = true
        }
    }

}
```

@tab 效果

<div align="center">
<video src="https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/tMt8jrhE.mp4" style="height:500px; " controls="controls" autoplay="autoplay" loop="loop"></video>
</div>

:::

在上述代码中, 我们先定义了一个定义了响应式变量``opacityAnmationFlag``来控制动画的起始和终止状态，接着我们在``View``组件的``attr{}``内，根据``opacityAnmationFlag``设置动画的起始和终止值，最后我们调用了``animate``方法来配置动画曲线以及动画时间。

<br/>

在运行时，一开始``opacityAnmationFlag``的值为false，所以组件一开始的``opacity``属性是1f, 接着，我们在``created``方法内，延时500ms后，将``opacityAnmationFlag``设置为true，会触发组件``attr{}``重新运行, 让View的opacity属性以动画的形式从起始值1f变化到0f。

### attr中的animation方法

每个组件的``attr{}``内，都含有``animate``方法，此方法用于描述组件的动画时间，动画曲线类型。它接收两个参数

* animation: 类型为Animation, 用于表示动画的曲线以及动画持续时间。Animation类默认提供了以下动画曲线类型
  * Animation.linear: 线性曲线
  * Animation.easeIn: 先慢后快曲线
  * Animation.easeOut: 先快后慢曲线
  * Animation.easeInOut: 开始和结尾慢, 中间快曲线
  * Animation.springLinear: 弹簧式线性曲线
  * Animation.springEaseIn: 弹簧式先慢后快曲线
  * Animation.springEaseOut: 弹簧式先快后慢曲线
  * Animation.springEaseInOut: 弹簧式开始和结尾慢, 中间快曲线
* value: 需传入一个控制动画状态的响应式变量, 在上面的例子为``opacityAnmationFlag``, 该字段用于标识响应式字段关联的动画曲线和动画时间以及控制动画的状态

## backgroundColor动画

学习了``opacity``动画后, 接下来我们来看如何对组件的背景颜色进行动画

::: tabs

@tab:active 示例

```kotlin{4,16-21,30}
@Page("1")
internal class TestPage : BasePager() {

    private var backgroundColorAnmationFlag by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            View {
                attr {
                    size(150f, 150f)
                    if (ctx.backgroundColorAnmationFlag) {
                        backgroundColor(Color.GREEN)
                    } else {
                        backgroundColor(Color.RED)
                    }
                    animate(Animation.linear(0.5f), ctx.backgroundColorAnmationFlag)
                }
            }
        }
    }

    override fun created() {
        super.created()
        setTimeout(500) {
            backgroundColorAnmationFlag = true
        }
    }

}
```

@tab 效果

<div align="center">
<video src="https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/32UqW3ZZ.mp4" style="height:500px; " controls="controls" autoplay="autoplay" loop="loop"></video>
</div>

:::

在上述代码中, 跟opacity动画类似, 使用了``backgroundColorAnmationFlag``控制背景颜色的起始和终止值，并绑定到一个时间为0.5s的线性动画描述。当``backgroundColorAnmationFlag``设置为true时, 背景颜色以动画的形式从红色变为绿色

## transform动画

transform动画支持对组件进行位移、缩放和旋转属性作动画。

### transform位移动画

::: tabs

@tab:active 示例

```kotlin{4,16-18,20,29}
@Page("1")
internal class TestPage : BasePager() {

    private var translateAnimationFlag by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            View {
                attr {
                    size(150f, 150f)
                    if (ctx.translateAnimationFlag) {
                        transform(Translate(0.5f, 0.5f))
                    }
                    backgroundColor(Color.GREEN)
                    animate(Animation.easeIn(0.5f), ctx.translateAnimationFlag)
                }
            }
        }
    }

    override fun created() {
        super.created()
        setTimeout(500) {
            translateAnimationFlag = true
        }
    }

}
```

@tab 效果

<div align="center">
<video src="https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/8zJ2coHo.mp4" style="height:500px; " controls="controls" autoplay="autoplay" loop="loop"></video>
</div>

:::

在上述代码中，我们使用``translateAnimationFlag``来控制组件``transform(Translate)``的起始(默认值)和终止值, 然后将**Animation.easeIn(0.5f)绑定到``translateAnimationFlag``变量**, 最后在创建页面，延迟500ms，将``translateAnimationFlag``设置为true, 从而触发``translate``动画

### scale缩放动画

::: tabs


@tab:active 示例

```kotlin{4,19-24,32}
@Page("2")
internal class TestPage : BasePager() {

    private var scaleAnimationFlag by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this

        return {
            attr {
                allCenter()
            }

            View {
                attr {
                    size(100f, 100f)
                    backgroundColor(Color.GREEN)

                    if (ctx.scaleAnimationFlag) {
                        transform(Scale(0.5f, 0.5f))
                    } else {
                        transform(Scale(1f, 1f))
                    }
                    animate(Animation.linear(0.5f), ctx.scaleAnimationFlag)
                }
            }
        }
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
        scaleAnimationFlag = true // 启动动画
    }
}
```

@tab 效果

<div align="center">
<video src="https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/qpX0h1sV.mp4" style="height:500px; " controls="controls" autoplay="autoplay" loop="loop"></video>
</div>

:::

在上述代码中, 我们使用``transform(Scale(x, y)``方法来设置组件的缩放属性, ``Scale(x, y)``需要传入相对于组件自身大小的百分比, 取值为[0~max]。比如缩放为原来大小的0.5倍时，需传入``Scale0.5f, 0.5f)``。
接着我们定义了响应式变量``scaleAnimationFlag``来控制**scale动画**的动画起始和终止值, 并绑定到一个0.5s的线性动画曲线上, 最后在页面回调``viewDidLayout``时, 将**scaleAnimationFlag**设置为true来启动动画


### rotate旋转动画

::: tabs

@tab:active 示例

```kotlin{4,19-24,32}
@Page("2")
internal class TestPage : BasePager() {

    private var rotateAnimationFlag by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this

        return {
            attr {
                allCenter()
            }

            View {
                attr {
                    size(100f, 100f)
                    backgroundColor(Color.GREEN)

                    if (ctx.rotateAnimationFlag) {
                        transform(Rotate(20f))
                    } else {
                        transform(Rotate(0f))
                    }
                    animate(Animation.linear(0.5f), ctx.rotateAnimationFlag)
                }
            }
        }
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
        rotateAnimationFlag = true // 启动动画
    }
}
```

@tab 效果

<div align="center">
<video src="https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/GpcSswQn.mp4" style="height:500px; " controls="controls" autoplay="autoplay" loop="loop"></video>
</div>

:::

在上述代码中, 我们调用``transform(Rotate(angle))``来设置组件的旋转属性, ``Rotate(angle)``需传入旋转的角度, 负值为逆时针旋转，正值为顺时针旋转，取值范围为[-360, 360]之间。
接着我们定义了响应式变量``rotateAnimationFlag``来控制旋转动画的初始和结束值, 并绑定到一个0.5f的线性动画曲线上, 最后在页面回调``viewDidLayout``时, 将**rotateAnimationFlag**设置为true来启动动画。

:::tip 注意
transform动画默认是以组件的中心点作为轴心来做动画, 你可以在transform方法传入Anchor来控制transform的中心点
:::

## frame动画

``frame动画``是指对组件的位置(x, y)和大小(width, height)进行动画, 例如:

::: tabs

@tab:active 示例

```kotlin{4,16,18,26}
@Page("2")
internal class TestPage : BasePager() {

    private var frameHeight by observable(100f)

    override fun body(): ViewBuilder {
        val ctx = this

        return {
            attr {
                allCenter()
            }

            View {
                attr {
                    size(100f, ctx.frameHeight)
                    backgroundColor(Color.GREEN)
                    animate(Animation.linear(0.5f), ctx.frameHeight)
                }
            }
        }
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
        frameHeight = 200f // 启动动画
    }
}
```

@tab 效果

<div align="center">
<video src="https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/4bF2NNUA.mp4" style="height:500px; " controls="controls" autoplay="autoplay" loop="loop"></video>
</div>

:::

上面的代码是使用``frame``动画对组件的高度进行动画, 首先定义了响应式变量``frameHeight = 100f``, 接着我们将组件的高度设置为``frameHeight``, 并绑定到一个0.5s的线性动画曲线，最后在``viewDidLayout``回调中，将``frameHeight``设置为200f，此时你可以看到组件会做一个高度从100f变换到200f的动画。
