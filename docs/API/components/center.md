# Center(居中布局容器)

使子元素在水平与垂直方向上都居中的容器组件，等价于同时设置了 `justifyContentCenter()` 与 `alignItemsCenter()` 的 View 容器。

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)与[布局属性](basic-attr-event.md#布局属性)，无自有属性。

## 事件

支持所有[基础事件](basic-attr-event.md#事件)，无自有事件。

## 说明

- `Center` 无构造参数，子节点统一居中，不再区分主轴与交叉轴。
- 如需单独控制某个方向的对齐，请使用 [Row](row.md) 或 [Column](column.md)。

## 示例

```kotlin{5-13}
Center {
    attr {
        size(pagerData.pageViewWidth, 100f)
        backgroundColor(Color.GRAY)
    }
    Text {
        attr {
            text("水平垂直皆居中")
        }
    }
}
```
