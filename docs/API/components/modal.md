# Modal(模态)

`Modal`是一个自定义的模态窗口组件，用于在当前页面上显示一个浮动窗口，可以用于显示表单、提示信息、详细信息等场景。当模态窗口显示时，用户无法与背景页面进行交互，只能与模态窗口内的内容进行交互。在创建`Modal`时，可传入一个布尔值参数`inWindow`，表示模态窗口的层级是否为窗口顶级，以及是否和屏幕等大（默认为 `false`，表示和页面一样大）。

[组件使用范例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/ModalViewDemoPage.kt)

::::warning 使用注意
- `inWindow` 为 `true` 时依赖 Native 渲染层能力，端侧版本不满足最低要求时该设置会被忽略，Modal 退化为普通 View 容器。
- `inWindow` 默认为 `false`，此时 Modal 尺寸与页面等大；如需自定义尺寸，请在 `attr` 中自行设置 `absolutePosition` 与 `size`。
::::



## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)

## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)，此外还支持：

### willDismiss

监听系统返回键触发的关闭事件。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| reason | 关闭原因 | ModalDismissReason |

**ModalDismissReason**

| 枚举值 | 值 | 描述 |
| -- | -- | -- |
| BackPressed | 0 | 系统返回键触发 |

```kotlin
Modal {
    event {
        willDismiss { reason ->
            // reason == ModalDismissReason.BackPressed 时表示用户按下了系统返回键
        }
    }
}
```