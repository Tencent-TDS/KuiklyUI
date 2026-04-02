# KuiklyUI

KuiklyUI 是腾讯开源的 **Kotlin Multiplatform 跨端 UI 框架**，支持 Android、iOS、HarmonyOS (OHOS)、Web(H5)、小程序 五个平台。提供两种 DSL 写法：自研 DSL（基于 `Pager`/`body()`）和 Compose DSL（基于 `ComposeContainer`/`setContent{}`）。

## 模块速查

**跨平台共享模块**（Kotlin/KMP，编译验证用 Android target 作为快速检查）

| 目录 | 职责 | 编译验证 |
|------|------|---------|
| `core/` | 核心引擎：Pager、响应式、布局、Bridge | `./gradlew :core:compileDebugKotlinAndroid` |
| `compose/` | Compose DSL 层 | `./gradlew :compose:compileDebugKotlinAndroid` |
| `core-annotations/` | 注解定义（@Page 等） | 随 core 构建 |
| `core-ksp/` | KSP 注解处理器 | 随 core 构建 |
| `demo/` | 示例代码（自研 DSL + Compose DSL） | 各平台 App 构建 |
| `docs/` | 对外官网文档，不是 AI 知识库 | — |
| `settings.gradle.kts` | 主构建入口（默认 Kotlin 2.1.21）；根目录多个 `build.X.Y.Z.gradle.kts` 对应不同 Kotlin 版本，由此文件选择 | — |

**平台 App 模块**（触发对应 render 层编译）

| 目录 | 平台 | 编译验证 |
|------|------|---------|
| `androidApp/` | Android | `./gradlew :androidApp:assembleDebug` |
| `iosApp/` | iOS | `cd iosApp && pod install --repo-update && cd .. && xcodebuild -workspace iosApp/iosApp.xcworkspace -scheme iosApp -configuration Debug build` |
| `ohosApp/` | HarmonyOS | Mac: `./2.0_ohos_demo_build.sh`；Windows: `2.0_ohos_demo_build.bat` |
| `h5App/` + `miniApp/` | Web / 小程序 | `./gradlew :h5App:jsBrowserDevelopmentWebpack` |

> 需要修改上表未列出的目录（如 `buildSrc/`、`core-render-*`、`core-ksp/` 内部结构）、确认代码应放哪个模块、理解模块间依赖关系时：见 [.ai/architecture/AGENTS.md](.ai/architecture/AGENTS.md)

## 依赖关系与模块边界

**边界规则**：
- `core/` 和 `compose/` 是纯 KMP 模块，**禁止**依赖任何 `core-render-*`
- 各 `core-render-*` 只能依赖平台原生 API，**禁止**依赖 `core/` 或 `compose/` 的接口
- `demo/` 只依赖 `core/` 和 `compose/`，不直接依赖 render 层

## 关键约束

**两种 DSL 不可混用**：开始任务前先判断当前需求或 bug 属于自研 DSL 还是 Compose DSL 范畴，无法判断时主动询问开发者，不要混用。

## 知识库索引

> **⚠️ 重要**：项目已建立完整的 AI 知识库，**开始任何编码任务前请先查表，找到对应文档后优先阅读**。
> 主动读取知识库中任意文档时，可先只读**前 5 行**的场景说明，判断是否与当前任务相关，再决定是否读取完整内容。

| 你遇到的情况 | 读取文档 |
|------------|---------|
| 自研 DSL：修改 `core/` 模块、或使用 `Pager`/`body()`/`vfor`/`vif`/`observable` | [.ai/self-dsl/AGENTS.md](.ai/self-dsl/AGENTS.md) |
| Compose DSL：修改 `compose/` 模块、或使用 `ComposeContainer`/`setContent`/`@Composable`/`Modifier` | [.ai/compose-dsl/AGENTS.md](.ai/compose-dsl/AGENTS.md) |
| 编码规范、包名规则、命名规范、expect/actual、日志 | [.ai/coding-standards/AGENTS.md](.ai/coding-standards/AGENTS.md) |
| 遇到疑难 bug、框架内部机制问题、复杂技术专题 | [.ai/references/AGENTS.md](.ai/references/AGENTS.md) |
