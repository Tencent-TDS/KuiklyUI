# H5 图片域名与路径处理指引

在 H5 环境下，本地开发的图片资源通常使用 `assets://` 协议引用，但在发布到线上时，这些资源往往部署在 CDN 上。Kuikly 提供了 `IImageProcessor` 接口，允许开发者自定义图片路径的转换规则，实现从 `assets://` 到 CDN URL 的动态映射。

## 核心机制

Kuikly 渲染引擎在加载图片资源前，会调用 `IImageProcessor.getImageAssetsSource(src)` 方法。开发者可以通过实现此接口，拦截并修改图片 URL。

## 接入步骤

### 1. 实现自定义 ImageProcessor

创建一个实现 `IImageProcessor` 接口的类（或 object），并在 `getImageAssetsSource` 方法中定义路径转换逻辑。

推荐参考代码：`h5App/src/jsMain/kotlin/processor/CustomImageProcessor.kt`

```kotlin
package com.tencent.kuikly.h5app.processor

import com.tencent.kuikly.core.render.web.processor.IImageProcessor
import com.tencent.kuikly.core.render.web.runtime.web.expand.processor.ImageProcessor as BaseImageProcessor
import org.w3c.dom.HTMLImageElement

object CustomImageProcessor : IImageProcessor {
    // 资源协议前缀
    private const val ASSETS_IMAGE_PREFIX = "assets://"
    // 你的 CDN 根路径
    private const val CDN_BASE_URL = "https://custom.com/assets/"

    /**
     * 图片路径转换核心方法
     * @param src 原始图片路径 (例如 "assets://images/icon.png")
     * @return 转换后的 CDN 路径 (例如 "https://custom.com/assets/images/icon.png")
     */
    override fun getImageAssetsSource(src: String): String {
        if (src.startsWith(ASSETS_IMAGE_PREFIX)) {
             return src.replace(ASSETS_IMAGE_PREFIX, CDN_BASE_URL)
        }
        return src
    }

    // 其他方法通常保持默认行为，委托给 BaseImageProcessor 即可
    override fun isSVGFilterSupported(): Boolean {
        return BaseImageProcessor.isSVGFilterSupported()
    }

    override fun applyTintColor(imageElement: HTMLImageElement, tintColorValue: String, rootWidth: Double) {
        BaseImageProcessor.applyTintColor(imageElement, tintColorValue, rootWidth)
    }
}
```

### 2. 在入口处注册 Processor

在 H5 应用的入口文件 `Main.kt` 中，将你的自定义 Processor 赋值给 `KuiklyProcessor.imageProcessor`。
**注意：必须在应用启动渲染前完成赋值。**

```kotlin
// h5App/src/jsMain/kotlin/Main.kt

import com.tencent.kuikly.h5app.processor.CustomImageProcessor
import com.tencent.kuikly.core.render.web.KuiklyProcessor

fun main() {
    // 1. 注册自定义图片处理器 (配置 CDN 域名)
    KuiklyProcessor.imageProcessor = CustomImageProcessor

    // 2. 正常的应用启动逻辑
    if (KuiklyRouter.handleEntry()) {
        return
    }
    // ...
}
```

## 常见场景

*   **CDN 部署**：将 `assets://` 替换为 `https://cdn.example.com/v1.0/`。
*   **多环境切换**：根据当前域名 (`window.location.hostname`) 动态决定使用测试环境 CDN 还是正式环境 CDN。
*   **WebP 自适应**：在 `getImageAssetsSource` 中判断浏览器支持情况，自动将 `.png` 后缀替换为 `.webp`。
