# TransitionView(转场动画容器)

等价 View 的内容视图转场过渡组件，常用于弹窗的入场与出场动画。使用方式与 View 完全一致，同样需要自行设置布局，区别是内容附带转场过渡动画。

## 构造参数

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| type <Badge text="必需" type="warn"/> | 转场动画类型 | TransitionType |

### TransitionType

| 取值 | 说明 |
| -- | -- |
| `NONE` | 无转场 |
| `DIRECTION_FROM_BOTTOM` | 从底部入场过渡 |
| `DIRECTION_FROM_CENTER` | 从中间入场过渡（缩放） |
| `DIRECTION_FROM_RIGHT` | 从右入场过渡 & 从右出场过渡 |
| `DIRECTION_FROM_LEFT` | 从左入场过渡 & 从左出场过渡 |
| `FADE_IN_OUT` | 淡入入场过渡 & 淡出出场过渡 |
| `CUSTOM` | 自定义转场动画，务必同时设置 `customBeginAnimationAttr` 与 `customEndAnimationAttr` |

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)与[布局属性](basic-attr-event.md#布局属性)，此外还支持：

### transitionAppear

控制当前以入场还是退场动画进行，默认为 `true`（入场）。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| enterOrExit | 入场或退场 | Boolean |

### customBeginAnimationAttr

自定义动画起始状态的属性设置。使用该方法时 `type` 需设置为 `TransitionType.CUSTOM`。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| beginAttr | 起始动画属性设置闭包 | Attr.() -> Unit |

### customEndAnimationAttr

自定义动画终止状态的属性设置。使用该方法时 `type` 需设置为 `TransitionType.CUSTOM`。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| endAttr | 终止动画属性设置闭包 | Attr.() -> Unit |

### customAnimation

配置动画参数，设置后优先使用该配置。默认使用 `Animation.springEaseInOut(0.35f, 0.9f, 1f)`。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| animation | 动画配置 | Animation |

::::warning 使用限制
`transitionAppear`、`customBeginAnimationAttr`、`customEndAnimationAttr`、`customAnimation` 均**不支持二次修改**，请在首次布局时完成设置。
::::

## 事件

支持所有[基础事件](basic-attr-event.md#事件)，此外还支持：

### transitionFinish

转场动画结束回调。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| transitionAppear | 本次结束的是入场(true)还是退场(false)动画 | Boolean |

## 示例

```kotlin{6-24}
Modal {
    attr {
        size(pagerData.pageViewWidth, pagerData.pageViewHeight)
    }
    TransitionView(TransitionType.DIRECTION_FROM_BOTTOM) {
        attr {
            size(pagerData.pageViewWidth, 300f)
            backgroundColor(Color.WHITE)
        }
        event {
            transitionFinish { isAppear ->
                // 转场动画结束
            }
        }
        Text {
            attr {
                text("从底部弹出的内容")
            }
        }
    }
}
```
