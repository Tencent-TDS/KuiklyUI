# 替换内置组件实现

宿主工程可以在**不改动业务代码**的前提下，替换 Kuikly 内置组件的实现。典型场景是对内置组件做统一增强（如统一埋点、统一样式、统一无障碍处理）。

## 注册方式

| 方法 | 说明 |
| -- | -- |
| `Pager.registerViewCreator(viewClassName, viewCreator)` | 注册指定内置组件的创建器 |

::::warning 注册时机
`registerViewCreator` **必须在 `Pager#body()` 之前调用**，推荐在 `willInit()` 或 `created()` 中注册。晚于 `body()` 注册不会生效。
::::

**示例**

```kotlin
@Page("demo_page")
internal class DemoPage : Pager() {

    override fun willInit() {
        super.willInit()
        registerViewCreator(ViewConst.TYPE_TEXT_CLASS_NAME) {
            // 返回自定义的 TextView 实现
            CustomTextView()
        }
    }

    override fun body(): ViewBuilder {
        return {
            Text {
                attr { text("这段文本由自定义实现渲染") }
            }
        }
    }
}
```

## 可替换的组件

仅有以下 4 个内置组件支持替换，常量取自 `ViewConst`：

| 常量 | 值 | 对应组件 |
| -- | -- | -- |
| `ViewConst.TYPE_VIEW_CLASS_NAME` | `"DivView"` | `View {}` |
| `ViewConst.TYPE_TEXT_CLASS_NAME` | `"TextView"` | `Text {}` |
| `ViewConst.TYPE_IMAGE_CLASS_NAME` | `"ImageView"` | `Image {}` |
| `ViewConst.TYPE_RICH_TEXT_CLASS_NAME` | `"RichTextView"` | `RichText {}` |

::::danger 注意区分
`ViewConst` 中还有大量形如 `TYPE_VIEW = "KRView"`、`TYPE_LIST = "KRListView"` 的常量，它们是**映射到端侧 Native 组件的类名**，不能用于 `registerViewCreator`。只有上表中的 4 个 `TYPE_XXX_CLASS_NAME` 可用于注册。
::::

## 生效方式

组件 DSL 函数在创建实例时会先尝试从注册表取自定义实现，取不到时回退到框架默认实现：

```kotlin
// 伪代码，说明内部流程
fun ViewContainer<*, *>.Text(init: TextView.() -> Unit) {
    val view = createViewFromRegister(ViewConst.TYPE_TEXT_CLASS_NAME) as? TextView
        ?: TextView()
    addChild(view, init)
}
```

因此注册是**按页面实例生效**的：在某个 Pager 中注册，只影响该页面；需要全局生效时，请在基类 Pager 中统一注册。
