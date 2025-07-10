# Switch(开关)

风格类iOS的开关组件，支持属性定制自定义开关样式

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/SwitchExamplePager.kt)

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)，此外还支持：

### isOn

设置开关

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| on | 开关是否打开 | Boolean |

### onColor

设置开关打开时的高亮色（背景色）

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| color | 开关打开时的高亮色 | Color |

### unOnColor

设置开关关闭时的背景色

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| color | 开关关闭时的背景色 | Color |

### thumbColor

设置圆型滑块颜色（关闭和开启同一个颜色）

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| color | 圆型滑块颜色| Color |

### thumbMargin

圆块与开关的贴边边距，默认为2f

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| margin | 圆块与开关的贴边边距| Float |

## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)，此外还支持：

### switchOnChanged

开关变化时的回调，回调会传入开关状态参数：

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| on | 开关状态（是否打开）| Boolean |

:::tabs

@tab:active 示例

```kotlin{8-17}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }
            Switch {
                attr {
                    isOn(true)
                }
                event {
                    switchOnChanged {
                        KLog.i("Switch", "switchOnChanged $it")
                    }
                }
            }
        }
    }
}
```

@tab 效果

<div align="center">
<img src="./img/switch.png" style="width: 30%; border: 1px gray solid">
</div>

:::