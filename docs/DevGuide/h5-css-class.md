# 使用 `cssClass` 为 DOM 节点附加 CSS 类名

`cssClass` 是 Kuikly 在 H5 平台提供的一个内置能力，用于给渲染后的 DOM 节点附加 CSS class，方便复用宿主侧样式体系，或补充 H5 特有样式。

这个能力适用于 H5 渲染场景，不属于“如何扩展自定义属性”的通用教程范畴，因此单独放在 Web 教程中说明更合适。

## 定义属性

可以先在 Kuikly 侧定义一个扩展属性：

```kotlin
fun Attr.cssClass(value: String) {
    "cssClass" with value
}
```

## 基本用法

可以在 `View`、`Text` 等组件上使用 `cssClass`：

```kotlin
View {
    attr {
        size(pagerData.pageViewWidth, 100f)
        backgroundColor(Color.WHITE)
        cssClass("test-single-class")
    }
}

Text {
    attr {
        text("Single CSS Class: .test-single-class")
        fontSize(16f)
        cssClass("test-text-class")
    }
}
```

## 支持多个 class

多个 class 可以通过空格分隔：

```kotlin
cssClass("test-multi-class-1 test-multi-class-2")
```

H5 宿主侧会按空白字符拆分后逐个添加到 DOM 元素的 `classList` 中。

## 空白处理

前后空白会被自动忽略：

```kotlin
cssClass("  test-padded-class  ")
```

最终生效的 class 名仍然是 `test-padded-class`。

## 动态更新

`cssClass` 支持响应式更新。属性值发生变化后，H5 渲染层会先移除该属性之前写入到 DOM 的 class，再应用新的 class 值。

例如：

```kotlin
var dynamicClassName by observable("test-single-class")

View {
    attr {
        cssClass(dynamicClassName)
    }
}

setTimeout(2000) {
    dynamicClassName = "test-single-class-updated"
}
```

上面的示例在更新后，DOM 上最终保留的是 `test-single-class-updated`，而不是旧值和新值的叠加结果。

## 说明

* `cssClass` 仅在 H5 渲染生效。
* 多个 class 使用空格分隔，前后空白会自动忽略。
* `cssClass` 最终会映射到 DOM 元素的 `classList`。
* 该能力适合补充 H5 特有样式，或接入宿主已有的 CSS 样式体系。

## 示例参考

完整示例可参考：

* `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/CssClassTestPage.kt`
