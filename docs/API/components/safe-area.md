# SafeArea(安全区域容器)

自动为内容应用安全区内边距的容器组件，用于避开状态栏、导航栏、Home 指示条以及屏幕圆角、刘海等物理遮挡区域。

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)与[布局属性](basic-attr-event.md#布局属性)，无自有属性。

## 事件

支持所有[基础事件](basic-attr-event.md#事件)，无自有事件。

## 说明

- SafeArea 可当作 View 容器使用，唯一差异是自动为内容增加了安全区内边距：`padding(top = safeAreaInsets.top, left = safeAreaInsets.left, bottom = safeAreaInsets.bottom, right = safeAreaInsets.right)`。
- 安全区边距**包含顶部状态栏高度区域**。
- 未显式设置高度时，组件会自动设置 `flex(1f)` 撑满父容器；需要固定高度时请显式设置 `height()`。
- 建议用 SafeArea 包裹页面顶级视图，并设置与应用设计匹配的背景色。

## 示例

```kotlin{9-18}
@Page("safe_area_demo")
internal class SafeAreaDemoPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            SafeArea {
                attr {
                    flex(1f)
                    backgroundColor(Color.WHITE)
                }
                Text {
                    attr {
                        text("内容不会被状态栏与 Home 指示条遮挡")
                        fontSize(16f)
                    }
                }
            }
        }
    }
}
```
