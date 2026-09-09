# Design — adapt-kotlin-2-3-10

## 目标约束

在"能充分支持 Kotlin 2.3"的前提下，**框架修改程度尽可能小**。任何改动都要能回答：是不是非改不可？能不能同时兼容 2.1.21？

## 决策 1：并行版本分支，而非原地升级

**选择**：新增一套 `2.3.10` 构建脚本，与既有 `2.1.21` 并存，不修改、不删除任何 2.1.21 脚本。

**理由**：
- Kuikly 已有多版本脚本先例（`build.1.3.10` ~ `build.2.1.21`、`build.2.0.21` 等），版本化脚本是项目既有范式，新增一套成本可控。
- 原地升级会让 2.1.21 的所有使用者立即暴露在 2.3 风险下，回滚成本高。
- 并行分支使"编译速度对比"具备可比基线（同一份代码，只切 Kotlin 版本）。

**代价**：构建脚本数量翻倍，后续每个模块新增 target 需同步两处。

## 决策 2：core-gradle-plugin 版本化 —— 方案 A（新增独立源码集）

**候选**：
- A：新增 `src/main/kotlin_compiler_2_3/` 源码集，按构建版本选择
- B：单源码集 + 条件编译（`if` 分支）
- C：抽取公共接口 + 版本化实现

**选择 A**。理由：Kotlin 2.3 的编译器插件 API（`IrTypesExtension`、`CommandLineProcessor`、`CompilerPluginRegistrar`、`GenerationExtension`）与 2.1 存在签名差异，单源码集条件编译会让一个文件里塞满版本分支，可读性和编译安全性都差；方案 C 抽象成本高于收益。方案 A 的物理隔离使两个版本各自独立演进、互不影响。

**落地**：新增 4 个文件到 `src/main/kotlin_compiler_2_3/com/tencent/kuikly/gradle/compiler/`。

## 决策 3：KSP 锁 2.3.4，AGP / Gradle 不升级

- KSP `2.3.4` 是与 Kotlin 2.3.10 对齐的版本；升到 `2.3.11` 会牵连 AGP 升级，成本量级差异巨大（AGP 升级会波及所有 Android 模块的构建行为与 demo 宿主）。
- AGP 维持 `8.6`、Gradle wrapper 不动，遵循"保持能编译的最低版本"。

## 决策 4：鸿蒙（OHOS）保持混合态

Kotlin 2.3 的 OHOS 定制版本尚未就绪。本期策略：**2.3.10 分支覆盖 Android / iOS / JS，OHOS 是否切入 2.3.10 列为待验证项**。`build.2.3.10.ohos.gradle.kts` 与 `2.3.10_ohos_demo_build.sh` 已先行铺好，待定制版本就绪后启用。

## 决策 5：唯一必要的源码改动 —— 局部类提取

`demo` 的 `MaterialDemo.kt` 中 `@Composable fun CustomSnackbar()` 内的局部类 `SnackbarVisualsWithError`，在 Kotlin 2.3 下会触发 Kotlin/Native IR 序列化问题：IR 会为局部类生成 top-level 声明记录，link 阶段反序列化时找不到对应 `Idx`，报 `Not found Idx for .../CustomSnackbar|CustomSnackbar(){}[0]`。提取为顶层类后不再产生该记录，iOS / 鸿蒙 link 均可通过，且已验证 2.1.21 下同样通过——属双版本兼容的最小改动。

## 决策 6：改动最小化 —— 还原 2 处非必要改动

Spike 阶段引入的两处源码改动经复核**非必需**，已还原：

1. `core-render-web` 删除 `@file:JsExport`：双版本兼容验证通过后无必要保留。
2. `core` ohosArm64 `DateTime.kt` 把 `getTimeNanos()` 改为 `TimeSource.Monotonic.markNow().elapsedNow()`：该写法在 `markNow()` 后立即 `elapsedNow()`，返回值恒为 ~0，属**错误改写**；且 `getTimeNanos` 在 2.3 下仅 deprecated（警告，不阻断编译），无需改动。

## 风险与未决项

| 风险 | 说明 | 状态 |
|---|---|---|
| FileReplacer yaml 替换不完整 | `publish/compatible/2.3.10.yaml` 仅 2/3 生效：多行块 `\|-` 语法不支持、`gradle-wrapper.properties` 路径报未找到 | 待修 |
| 腾讯镜像缺失 Kotlin 2.3.x | 若镜像未同步，依赖拉取会失败 | 待确认 |
| `DateTime.kt` 还原 | 若 2.3.10 实际编译 `ohosArm64` 且 `getTimeNanos` 在 2.3 是移除而非 deprecated，会编译失败 | 待验证 |
| 编译速度收益 | 上 2.3 的核心诉求，但尚无 2.1.21 vs 2.3.10 的对比数据 | 未测 |
