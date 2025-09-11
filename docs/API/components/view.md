# View(容器)

``View``组件作为最基础的UI组件, 可嵌套使用，常用于组织子View如何布局。在Android平台对应``FrameLayout``，在iOS平台对应``UIView``

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/kit_demo/DeclarativeDemo/ViewExamplePage.kt)

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)，还支持以下属性：

### backgroundImage

设置容器背景图片（默认resize为cover）

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| src | 图片源，与[Image组件src](./image.md#src)能力一致 | String |
| imageAttr | 自定义该图片属性，参考[Image组件属性](./image.md#属性) | ImageAttr |

### glassEffectIOS

:::tip 系统要求
iOS 26.0+ 支持。在低版本系统上会自动降级到普通背景效果。
:::

为View组件添加iOS 26+液态玻璃效果。

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| style | 玻璃效果样式（REGULAR/CLEAR） | GlassEffectStyle | REGULAR |
| tintColor | 玻璃效果色调颜色 | Color | 系统默认 |
| interactive | 是否启用交互效果 | Boolean | false |
| enable | 是否启用玻璃效果 | Boolean | true |

**GlassEffectStyle 枚举值：**
- `REGULAR` - 标准液态玻璃效果
- `CLEAR` - 清透液态玻璃效果

**示例：**
```kotlin
View {
    attr {
        glassEffectIOS() // 默认效果
        // 或
        glassEffectIOS(
            style = GlassEffectStyle.CLEAR,
            tintColor = Color.BLUE,
            interactive = true
        )
    }
}
```

:::warning 注意
使用glassEffectIOS时，不能同时设置backgroundColor，否则会覆盖玻璃效果。

液态玻璃效果目前仅支持统一的圆角设置，即四个角的圆角值必须相同。暂不支持为每个角单独设置不同的圆角值。
:::

### glassEffectContainerIOS

:::tip 系统要求
iOS 26.0+ 支持。在低版本系统上会自动降级到普通容器效果。
:::

为View组件添加液态玻璃容器效果，用于创建多个液态玻璃元素之间的融合效果。

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| spacing | 子元素之间的间距 | Float | 0f |

**示例：**
```kotlin
View {
    attr {
        glassEffectContainerIOS(15f) // 15点间距
        flexDirectionRow()
    }
    // 子View会自动应用液态玻璃融合效果
    View {
        attr {
            flex(1f)
            height(60f)
            borderRadius(30f)
        }
    }
    View {
        attr {
            flex(1f)
            height(60f)
            borderRadius(30f)
        }
    }
}
```

:::tip 与glassEffectIOS的区别
- `glassEffectIOS`：为单个View添加液态玻璃效果
- `glassEffectContainerIOS`：为容器添加融合效果，子元素之间会产生视觉融合
:::

## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)

### touchUp、touchDown、touchMove、touchCancel

``touchUp``、``touchDown``、``touchMove``、 ``touchCancel``事件为``View``组件触摸事件触发的回调，触摸事件回调闭包中含有
``TouchParams``类型参数，以此来描述事件相关的信息，``TouchParams``类型中的``touches``属性包含多指触摸信息，为``Touch``类型数组 

<div class="table-01">

**TouchParams**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| x | 触摸点在自身view坐标系下的坐标X  | Float |
| y | 触摸点在自身view坐标系下的坐标Y  | Float |
| pageX | 触摸点在根视图Page下的坐标X  | Float |
| pageY | 触摸点在根视图Page下的坐标Y  | Float |
| pointerId | 触摸点对应的ID, 该属性从1.1.86版本开始支持  | int |
| action | 事件类型, 该属性从1.1.86版本开始支持| String |
| touches | 包含所有多指触摸信息，该属性为Touch类型数组  | List |

:::tip 备注
* 对于1.1.86（不包含）以下版本：触摸抬起和触摸取消都会触发touchUp事件，没有action属性
* 对于1.1.86（包含）及以上版本：
  * 当仅注册touchUp事件时，行为和低版本一致，可以通过action参数来区分事件类型
  * 当同时注册touchUp和touchCancel事件时，触摸抬起会触发touchUp事件，触摸取消会触发touchCancel事件
:::

</div>

<div class="table-02">

**Touch**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| x | 触摸点在自身view坐标系下的坐标X  | Float |
| y | 触摸点在自身view坐标系下的坐标Y  | Float |
| pageX | 触摸点在根视图Page下的坐标X  | Float |
| pageY | 触摸点在根视图Page下的坐标Y  | Float |
| pointerId | 触摸点对应的ID  | int |

</div>

**示例**

```kotlin
@Page("TouchEventTestPage")
internal class TouchEventTestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            List {
                attr {
                    flex(1f)
                }

                View {
                    attr {
                        paddingBottom(100f)
                        paddingTop(100f)
                        backgroundColor(Color.YELLOW)
                    }
                    event {
                        touchUp { touchParams ->
                            val x = touchParams.x
                            val y = touchParams.y
                            val pageX = touchParams.pageX
                            val pageY = touchParams.pageY
                            val action = touchParams.action
                            val pointerId = touchParams.pointerId
                            val touches = touchParams.touches
                            KLog.i("YELLOW", "YELLOW:touchUp")
                        }
                        touchDown { touchParams ->
                            KLog.i("YELLOW", "YELLOW:touchDown")
                        }
                        touchMove { touchParams ->
                            KLog.i("YELLOW", "YELLOW:touchMove")
                        }
                    }
                }
            }

        }
    }
}
```

## 方法

### bringToFront<Badge text="鸿蒙实现中" type="warn"/><Badge text="微信小程序实现中" type="warn"/><Badge text="H5实现中" type="warn"/>

将组件置顶到父组件的最高层级

