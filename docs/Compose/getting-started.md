# 快速开始

本页目标：**5 分钟内在工程中跑起第一个 Kuikly Compose 页面**。

## 前置条件

在开始之前，请确保您已完成 [环境搭建](../QuickStart/env-setup.html) 中介绍的所有环境配置。

## 创建Compose工程
Kuikly 提供了预配置的模板工程，这是开始使用 Kuikly Compose 最简单的方式：

1. 创建工程，并选择Kuikly project template （需要完成环境搭建中的ide插件安装）
![](./img/create1.png)
2. 填好基本信息后，下一步，注意DSL选择Compose，最后点击Finish完成工程创建
![](./img/create2.png)
3. 执行gradle sync 同步项目（File > Sync Project with Gradle Files）
4. 运行项目，查看示例页面

## 第一个 Compose 页面

Kuikly 中的页面一般继承自 `ComposeContainer`（一个基于 Kuikly Core 的跨端页面容器），并在其中通过 `setContent` 设置 `@Composable` 内容。

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Modifier

@Page("helloWorld")
class HelloComposePage : ComposeContainer() {

   override fun willInit() {
      super.willInit()
      setContent {
          HelloComposeScreen()
      }
   }
}

@Composable
private fun HelloComposeScreen() {
    var count by remember { mutableStateOf(0) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Hello Kuikly Compose, count = $count")
        Button(onClick = { count++ }) {
            Text(text = "Click me")
        }
    }
}
```

**关键步骤说明**：

1. **创建页面类**
   - 继承 `ComposeContainer`
   - 使用 `@Page` 注解定义页面名称
   - 在 `willInit()` 中调用 `setContent {}` 设置 UI

2. **实现 Compose UI**
   - 在 `setContent` 中使用 Compose DSL 编写 UI 代码
   - 可以使用标准的 Compose 组件和修饰符

3. **运行和测试**
   - 使用 Android Studio 运行项目
   - 在模拟器或真机上查看效果

**注意**：

- `HelloComposePage` 继承自 Kuikly 的 `ComposeContainer`，而不是 Android Activity
- `setContent {}` 的用法与 Jetpack Compose 基本一致
- 页面生命周期、路由跳转等能力来自 Kuikly Core，与自研 DSL 页面保持一致

## 包名导入须知

为了实现跨平台并避免与 Android 原生 Compose 冲突，请遵循以下导入规则：

- **Runtime 层（`androidx.compose.runtime.*`）**：
  直接使用官方包。Kuikly 复用了官方的运行时能力（如 `remember`, `mutableStateOf`）。

- **非 Runtime 层（`com.tencent.kuikly.compose.*`）**：
  **必须使用 Kuikly 的包名**。这包括 UI 基础（`ui`）、组件库（`material3`、`foundation`）以及动画（`animation`）等所有上层能力，都经过了适配以支持跨平台渲染。

## 下一步

- 继续阅读：[入门指南](./how-to-read.md)（文档阅读顺序与推荐路径）
