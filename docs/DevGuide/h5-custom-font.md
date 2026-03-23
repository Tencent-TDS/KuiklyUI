# H5 自定义字体使用指引

在 H5 环境下，使用自定义字体时，需要在判断到字体加载完成后触发一次文本的重新测量。原因是：H5 自定义字体是异步加载的，Kuikly 初始化时字体还没有加载完成，从而测量出的尺寸是默认字体的尺寸，从而会导致自定义字体的文本显示不全等问题。

## 接入步骤

### 1. 配置自定义字体

配置例子如下，配置在 `h5App/src/jsMain/resources/index.html` 里。

```html
<style>
    @font-face {
      font-family: 'Kanit Medium';
      src: url('./assets/Kanit-Medium.ttf') format('truetype');
    }
</style>
```

### 2. 字体加载完成后触发重新测量的调用

在 H5 应用的入口文件 `Main.kt` 中，判断到字体加载完成后调用 `delegator.fontLoaded()`。

```kotlin
// h5App/src/jsMain/kotlin/Main.kt

fun main() {
    // ...

    // When using custom fonts, fonts are loaded asynchronously, so a re-layout needs to be 
    // triggered after loading completes to re-measure text with the correct font metrics
    document.asDynamic().fonts.load("16px 'Kanit Medium'").then({ _ ->
        delegator.fontLoaded()
    })
}
```
