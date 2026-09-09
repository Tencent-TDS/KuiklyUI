## Why

Kuikly 主线构建锁定在 Kotlin 2.1.21，无法享用 Kotlin 2.3 在 K2 编译器上的编译性能改进。本次以**零侵入**方式引入 Kotlin 2.3.10 并行构建分支：2.1.21 主线产物完全不变，新增一套 2.3.10 构建脚本与配套编译器插件源码集，使框架可以在 Kotlin 2.3.10 下完成多端构建，并为后续"编译速度是否真的提升"提供可对比的构建入口。

## What Changes

- 新增 Kotlin 2.3.10 并行构建分支（共 40 个新增文件）：各模块的 `build.2.3.10.gradle.kts` / `build.2.3.10.ohos.gradle.kts`、4 份 `settings.2.3.10*.gradle.kts`（发布 / demo / app / ohos）、`publish/2.3.10_publish.sh`、`publish/compatible/2.3.10.yaml`、`2.3.10_ohos_demo_build.sh`、`verify_2.3.10.sh`。既有 `build.2.1.21.*` 与 `settings.2.1.21.*` **零修改、零删除**。
- `core-gradle-plugin` 采用**版本化方案 A**：新增 `src/main/kotlin_compiler_2_3/` 源码集（`IrTypesExtension`、`KuiklyCommandLineProcessor`、`KuiklyCompilerPluginRegistrar`、`KuiklyGenerationExtension`），与既有 Kotlin 2.1 编译器插件源码集并存，按构建版本选择源码集。
- `buildSrc/src/main/java/KuiklyKotlinBuildVar.kt` 增加 2.3.10 版本映射，使 `2.3.10` 成为可选构建版本。
- KSP 锁定 `2.3.4`，AGP 维持 `8.6`（不升级，保持"能编译的最低版本"），Gradle wrapper 不动。
- 唯一必要的示例源码改动：`demo` 的 `MaterialDemo.kt` 将 `@Composable` 函数内的局部类 `SnackbarVisualsWithError` 提取为顶层类。原因：Kotlin 2.3 的 Kotlin/Native IR 序列化会为局部类生成 top-level 声明记录，link 阶段反序列化时找不到对应 `Idx`，报 `Not found Idx for .../CustomSnackbar|CustomSnackbar(){}[0]`；提取为顶层类后不再产生该记录。已验证 Kotlin 2.1.21 下同样通过。
- `.gitignore` 与 `androidApp/build.gradle.kts` 同步适配 2.3.10 分支。
- 既有源码改动最小化：还原 2 处非必要改动——`core-render-web` 的 `@file:JsExport` 删除、`core` ohosArm64 `DateTime.kt` 的 `getTimeNanos → TimeSource`（后者 `markNow().elapsedNow()` 恒为 0，属错误改写）。

## Non-goals

- **不**升级 AGP / Gradle 到最新版本（保持"能编译的最低版本"）
- **不**删除 `iosX64` / `macosX64` target
- **不**改动或删除任何 2.1.21 / 2.0.21 构建产物与脚本
- **不**升级 Compose Multiplatform（维持 CMP 1.7.3，验证其与 Kotlin 2.3.10 能否共存）
- **不**覆盖鸿蒙定制 Kotlin 2.3（等待定制版本；本期鸿蒙是否切入 2.3.10 未定，属待验证项）
- **不**修改框架运行时行为与业务可见 API

## Capabilities

### New Capabilities

- `kotlin-2-3-10-build-branch`: 以并行版本分支方式提供 Kotlin 2.3.10 构建能力——通过 `build.2.3.10*` / `settings.2.3.10*` 脚本与 `kotlin_compiler_2_3` 编译器插件源码集，使 `core`、`compose`、`core-ksp`、`core-annotations`、`core-render-android`、`core-render-web`、`core-wx`、`ui-tooling`、`demo`、`demo_sub`、`apkbuilder`、`h5App`、`miniApp` 可在 Kotlin 2.3.10 下构建，且不影响 2.1.21 主线。

### Modified Capabilities

- 无（既有 specs 均不涉及构建版本分支行为）。

## Impact

- **受影响平台**：Android、iOS、HarmonyOS、Web、miniApp（2.3.10 分支覆盖；OHOS 实际是否启用 2.3.10 仍属待验证项）。
- **受影响模块**：`core`、`compose`、`core-annotations`、`core-ksp`、`core-gradle-plugin`、`core-render-android`、`core-render-web`（base / h5 / miniapp）、`core-wx`、`ui-tooling`、`demo`、`demo_sub`、`apkbuilder`、`h5App`、`miniApp`、`buildSrc`。
- **不受影响模块**：`core-render-ios`（ObjC 实现，无 Kotlin 编译）。
- **受影响依赖**：Kotlin `2.3.10`、KSP `2.3.4`；AGP `8.6` 与 Gradle wrapper 保持不变。
- **受影响 API**：无业务可见 API 变更。
