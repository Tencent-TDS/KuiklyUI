# 无障碍与语义

Kuikly Compose 通过 Semantics 向无障碍服务与自动化测试暴露节点的语义信息。

:::tip 与自研 DSL 的关系
自研 DSL 的无障碍能力由 `accessibility` / `accessibilityRole` / `testTag` 等属性提供（见[基础属性和事件](../API/components/basic-attr-event.md)）。Compose DSL 使用本篇的语义 Modifier，两者 API 形态不同但作用一致。
:::

## Modifier.semantics

为节点追加语义信息，可选择是否合并子节点语义。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| mergeDescendants <Badge text="非必需" type="warn"/> | 是否将自身与子树合并为一个逻辑实体，常见于按钮、表单等需要整体聚焦的元素 | Boolean |
| properties <Badge text="必需" type="warn"/> | 语义属性设置 | SemanticsPropertyReceiver.() -> Unit |

::::warning 合并语义的影响
`mergeDescendants = true` 时，子树中未标记合并的节点会从语义树中消失，其属性被合并到父节点（例如文本类属性会以逗号拼接）。
::::

## Modifier.clearAndSetSemantics

清除自身与子节点已有的语义，并重新设置。用于把一组细碎的可聚焦元素（如多个小按钮）收敛为一个语义实体。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| properties <Badge text="必需" type="warn"/> | 语义属性设置 | SemanticsPropertyReceiver.() -> Unit |

## Modifier.testTag

设置测试标识，供 Appium 等自动化测试定位节点使用。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| tag <Badge text="必需" type="warn"/> | 测试标识 | String |

## 示例

```kotlin
Text(
    text = "提交",
    modifier = Modifier
        .testTag("submit_button")
        .semantics(mergeDescendants = true) {
            contentDescription = "提交按钮"
        }
)
```

将卡片内的多个小按钮合并为单一语义实体：

```kotlin
Card(
    modifier = Modifier.clearAndSetSemantics {
        contentDescription = "商品卡片，点击查看详情"
        onClick { /* 处理点击 */ }
    }
) {
    // 内部零散的图标与文本
}
```
