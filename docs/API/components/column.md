# Column(垂直布局容器)

垂直排列子元素的容器组件，等价于设置了 `flexDirectionColumn()` 的 View 容器。

## 构造参数

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| align <Badge text="非必需" type="warn"/> | 子元素在水平方向（交叉轴）上的对齐方式，默认 `FlexAlign.STRETCH` | FlexAlign |

`align` 可选取值：`FlexAlign.FLEX_START`、`FlexAlign.CENTER`、`FlexAlign.FLEX_END`、`FlexAlign.STRETCH`。

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)与[布局属性](basic-attr-event.md#布局属性)，无自有属性。

## 事件

支持所有[基础事件](basic-attr-event.md#事件)，无自有事件。

## 说明

- `Column` 在初始化时自动设置 `flexDirectionColumn()` 与 `alignItems(align)`，因此无需再手动设置主轴方向。
- 主轴（垂直方向）上的对齐通过 `justifyContent()` 系列方法设置，详见[布局属性](basic-attr-event.md#布局属性)。

## 示例

```kotlin{5-20}
Column(align = FlexAlign.CENTER) {
    attr {
        size(pagerData.pageViewWidth, 200f)
        backgroundColor(Color.GRAY)
    }
    Text {
        attr {
            text("第一行")
        }
    }
    Text {
        attr {
            text("第二行")
        }
    }
}
```
