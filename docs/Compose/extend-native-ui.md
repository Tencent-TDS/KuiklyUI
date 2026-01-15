# 扩展原生 UI 组件

本页说明在「已有 Native UI 能力」的前提下，如何通过 Kuikly DSL 与 `MakeKuiklyComposeNode`，完成从 Native UI 到最终 Compose 组件的完整链路（整体分两步）。

## 步骤一：将 Native UI 封装为 Kuikly DSL 组件

- 在 Kuikly DSL 层实现属性/事件桥接，把原生 View 封装成 Kuikly UI 组件  
- 详细的设计与实现步骤，请参考  [扩展原生View](/DevGuide/expand-native-ui.html)

## 步骤二：将 Kuikly DSL UI 暴露为 Compose 组件

- 在「Kuikly DSL UI 组件已经存在」的前提下，通过 `MakeKuiklyComposeNode` 把 DSL 组件包装成 Compose 可用的 Composable  
- 具体的 API、参数说明与使用示例，请参考：  [扩展 Kuikly DSL UI 组件（Compose）](/Compose/extend-kuikly-dsl-ui.html)

## Demo 示例

- DSL 侧封装示例（Kuikly DSL Video 组件）：[`VideoView.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/core/src/commonMain/kotlin/com/tencent/kuikly/core/views/VideoView.kt)
- Compose 侧封装示例：[`VideoView.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/VideoView.kt)
- 最终使用页面：[`ComposeVideoDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/ComposeVideoDemo.kt)

