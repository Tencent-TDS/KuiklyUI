# DatePicker(日期选择器)

`DatePicker`是基于`Scroller`实现的日期选择器，

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/kit_demo/DeclarativeDemo/ScrollPickerExamplePage.kt)

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)，此外还支持：

### initialDate

设置日期选择器的初始选中日期。如果不设置，默认为当前日期。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| year | 年份 | Int |
| month | 月份 (1-12) | Int |
| day | 日 (1-31) | Int |

也可以直接传入 `Date` 对象：

```kotlin
attr {
    initialDate(2025, 1, 21) // 方式1：直接传入年月日
    // 或者
    initialDate(Date(2025, 1, 21)) // 方式2：传入Date对象
}
```

### initialScrollAnimated

设置初始滚动到指定日期时是否使用动画。

| 值 | 描述 |
| -- | -- |
| `true` | 默认值，首次显示时有弹簧动画滚动到目标日期 |
| `false` | 首次显示时直接定位到目标日期，无动画 |

```kotlin
attr {
    initialDate(2025, 1, 21)
    initialScrollAnimated = false  // 禁用初始滚动动画
}
```

:::tip 动态刷新日期
如需在运行时动态切换日期（如点击"近一月"按钮），可配合 `vbind` 使用：

```kotlin
private var selectedDate: Date by observable(Date(2025, 1, 22))

// 在 body 中
vbind({ selectedDate }) {
    DatePicker {
        attr {
            initialDate(selectedDate)
            // initialScrollAnimated 默认为 true，切换时会有动画效果
        }
    }
}

// 点击按钮时更新日期
selectedDate = Date(2024, 12, 22)  // 触发 DatePicker 重新创建
```
:::

## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)，此外还支持：

### chooseEvent

设置日期选择器的选择事件，当用户选择日期时触发回调，回调传入参数为`DatePickerDate`类型

**DatePickerDate**

| 成员 | 描述 | 类型 |
| -- | -- | -- |
| timeInMillis | 当前选择日期的时间戳，单位毫秒 | Long |
| centerItemIndex | 当前选择日期 | Date |

**Date**

| 成员 | 描述 | 类型 |
| -- | -- | -- |
| year | 当前选择日期的年 | Int |
| month | 当前选择日期的月 | Int |
| day | 当前选择日期的日 | Int |

:::tabs

@tab:active 示例

```kotlin{18-32}
@Page("demo_page")
internal class TestPage : BasePager() {
    private var date: Date by observable(Date(0,0,0))
    private var dateTimestamp : Long by observable(0L)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
                flexDirectionColumn()
            }
            Text {
                attr {
                    text("现在是${ctx.date}, ${ctx.dateTimestamp}")
                }
            }
            DatePicker {
                attr {
                    width(300f)
                    backgroundColor(Color.WHITE)
                    borderRadius(8f)
                }
                event {
                    chooseEvent {
                        it.date?.let {
                            ctx.date = it
                        }
                        ctx.dateTimestamp = it.timeInMillis
                    }
                }
            }
        }
    }
}
```

@tab 效果

<div align="center">
<img src="./img/date_picker.png" style="width: 30%; border: 1px gray solid">
</div>

:::