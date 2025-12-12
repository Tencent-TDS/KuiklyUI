# H5 SPA 实现指引

Kuikly Web SDK提供的是一个单页渲染生命周期的管理，每打开一个新页面需要加载整个页面，如果想把跨端层实现的多个页面通过hash路由来管理，实现SPA，H5App Demo中的Kuikly Router 组件提供了参考思路：

KuiklyRouter 组件基于浏览器的 History API 实现，支持页面导航、状态缓存、滚动位置恢复以及页面生命周期管理。

## 核心特性

*   **无缝切换**：支持通过 URL 参数动态开启或关闭 SPA 模式。
*   **页面导航**：提供标准的 `push`、`replace`、`back` 导航接口。
*   **页面缓存**：自动缓存已加载的页面实例（DOM 及状态），返回时秒开。
*   **滚动恢复**：自动记录并恢复页面的滚动位置（Scroll Restoration）。
*   **生命周期管理**：自动分发 `pause` (隐藏) 和 `resume` (恢复) 事件，与原生生命周期保持一致。

## 接入步骤

### 1. 修改入口文件 (Main.kt)

在 H5 应用的入口函数 `main()` 中，优先调用 `KuiklyRouter.handleEntry()`。如果该方法返回 `true`，则表示 Router 已接管页面渲染，无需后续处理。

```kotlin
// h5App/src/jsMain/kotlin/Main.kt

import com.tencent.kuikly.h5app.manager.KuiklyRouter

fun main() {
    // 1. 尝试让 Router 接管入口
    // 如果 URL 包含 use_spa=1 或默认开启了 SPA，这里会初始化 Router 并返回 true
    if (KuiklyRouter.handleEntry()) {
        return
    }

    // 2. 普通多页模式（降级处理）
    console.log("##### Kuikly H5 Normal Mode #####")
    val delegator = KuiklyRouter.createDelegator(window.location.href)
    
    // ... 其他常规初始化代码
}
```

### 2. 启用 SPA 模式

有两种方式可以激活 SPA 模式：

*   **URL 参数激活（推荐调试用）**：
    在页面 URL 中添加参数 `use_spa=1`。
    示例：`http://localhost:8080/index.html?use_spa=1&page_name=home`

*   **全局默认激活**：
    修改 `KuiklyRouter` 中的 `ENABLE_BY_DEFAULT` 常量。
    ```kotlin
    // KuiklyRouter.kt
    private const val ENABLE_BY_DEFAULT = true // 改为 true 默认开启
    ```

## API 使用指南

KuiklyRouter 提供了简便的 Kotlin API 进行页面跳转。

### 页面导航

*   **打开新页面 (Push)**
    ```kotlin
    KuiklyRouter.push("http://your-domain.com/path?param=value")
    ```
    *   会将新状态推入 History 栈。
    *   当前页面会被隐藏（`pause`），新页面被创建或恢复。

*   **替换当前页 (Replace)**
    ```kotlin
    KuiklyRouter.replace("http://your-domain.com/new-path")
    ```
    *   替换当前 History 记录。
    *   销毁当前页面，加载新页面。

*   **返回 (Back)**
    ```kotlin
    KuiklyRouter.back()
    ```
    *   触发浏览器后退行为。

### 路由回调与劫持

Router 会自动劫持 `KRRouterModule` 的全局导航事件，因此你也可以使用通用的路由模块接口，底层会自动转发给 KuiklyRouter：

```kotlin
// 业务层通用调用（会自动适配 SPA）
KRRouterModule.openUrl("...") // 等同于 KuiklyRouter.push
KRRouterModule.closePage()    // 等同于 KuiklyRouter.back
```

## 原理说明

1.  **容器管理**：
    *   Router 默认在 `id="root"` 的容器中管理页面。
    *   每个页面对应一个 `PageInfo`，包含 `delegator`（视图代理）和 `HTMLElement`（DOM 节点）。

2.  **页面缓存 (`pageCache`)**：
    *   使用 `Map<String, PageInfo>` 缓存页面。
    *   跳转新页面时，旧页面不会被销毁，而是 `display: none` 并触发 `pause`。
    *   返回旧页面时，恢复 `display: block` 并触发 `resume`。

3.  **滚动恢复**：
    *   Router 禁用了浏览器的默认滚动恢复 (`history.scrollRestoration = 'manual'`)。
    *   在页面切换前，自动记录当前页面的滚动位置 (`y` 坐标)。
    *   页面切回时，延迟一小段时间自动滚动到记录的位置。

