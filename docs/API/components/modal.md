# Modal(模态)

`Modal`是一个自定义的模态窗口组件，用于在当前页面上显示一个浮动窗口，可以用于显示表单、提示信息、详细信息等场景。当模态窗口显示时，用户无法与背景页面进行交互，只能与模态窗口内的内容进行交互。在创建`Modal`时，可传入一个布尔值参数`inWindow`，表示模态窗口的层级是否为窗口顶级，以及是否和屏幕等大（默认为 `false`，表示和页面一样大）。

[组件使用范例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/ModalViewDemoPage.kt)



## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)

## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)