# Tasks — adapt-kotlin-2-3-10

> 落地 commit：`edf9a9c8d`（分支 `feature/valo_kotlin_2.3_support_latest`，基线 `origin/main_internal@9bc6cf5d0`）
> 44 files changed, 3772 insertions(+), 13 deletions(-)　删除：0

## 0. 进度总览

```
✅ 1  版本决策与前置验证（镜像已确认含 2.3.10 正式版）
✅ 2  并行构建分支建设
✅ 3  编译器插件版本化（yaml 替换实测 3/3 生效，无需修复）
✅ 4  源码适配
✅ 5  改动最小化（还原已落地，回归验证通过）
✅ 6  分支治理
✅ 7  待验证项：全部完成（Android ✅ ｜ iOS ✅ ｜ OHOS 已定混合态 ✅ ｜ 编译速度 ✅ ｜ demo 宿主 ✅）
```

## 1. 版本决策与前置验证

- [x] 1.1 主线 Kotlin 版本定为 **2.3.10**，全局统一
- [x] 1.2 KSP 锁 **2.3.4**，AGP 维持 **8.6**，Gradle wrapper 不动
  > ⚠️ **本条已被 §11 取代**：为「2.1.21 共存」而定，当时 wrapper 保持 7.6.3、靠 FileReplacer 临时切 8.9。
  > 方向改为「默认 2.3.10」后，wrapper 已**固化为 8.9**（见 §11.2），本条仅作历史记录保留。
- [x] 1.3 **确认腾讯镜像是否同步 Kotlin 2.3.x** —— 已确认：`mirrors.tencent.com/nexus/repository/maven-public/` 含 `2.3.10` 正式版（及 2.3.20 / 2.3.21），依赖拉取无忧

## 2. 并行构建分支建设

- [x] 2.1 新增各模块 `build.2.3.10.gradle.kts` / `build.2.3.10.ohos.gradle.kts`
- [x] 2.2 新增 `settings.2.3.10.gradle.kts`（发布）、`.demo` / `.app` / `.ohos` 三份验证用 settings
- [x] 2.3 新增 `publish/2.3.10_publish.sh` 与 `publish/compatible/2.3.10.yaml`
- [x] 2.4 新增 `verify_2.3.10.sh`、`2.3.10_ohos_demo_build.sh`
- [x] 2.5 `buildSrc/src/main/java/KuiklyKotlinBuildVar.kt` 增加 2.3.10 版本映射
- [x] 2.6 确认既有 `build.2.1.21.*` / `settings.2.1.21.*` 零修改、零删除

## 3. 编译器插件版本化（方案 A）

- [x] 3.1 新增 `core-gradle-plugin/src/main/kotlin_compiler_2_3/` 源码集（`IrTypesExtension`、`KuiklyCommandLineProcessor`、`KuiklyCompilerPluginRegistrar`、`KuiklyGenerationExtension`）
- [x] 3.2 2.3.10 构建走 `kotlin_compiler_2_3` 源码集
- [x] 3.3 **修复 FileReplacer 的 yaml 替换** —— 实测 `Files updated: 3 / 3, Total changes: 4`，全部生效（此前 "2/3" 系在错误工作目录执行所致，非代码缺陷，无需修复）

## 4. 源码适配

- [x] 4.1 `demo` 的 `MaterialDemo.kt`：将 `@Composable` 内的局部类 `SnackbarVisualsWithError` 提取为顶层类（Kotlin 2.3 K/N IR 序列化导致 link 阶段 `Not found Idx`）
- [x] 4.2 验证该改动在 Kotlin 2.1.21 下同样通过（双版本兼容）

## 5. 改动最小化（还原）

- [x] 5.1 ~~还原 `core-render-web` 的 `@file:JsExport` 删除~~ → **结论已修正，方向相反**：
  原判断"该改动非必需、予以还原"是**误判**——当时只验证了 2.1.21，未跑 2.3 的 JS 编译。实测 2.3.10 下：

  ```
  e: KuiklyRenderViewDelegatorDelegate.kt:16:1
     Declaration of such kind (typealias) cannot be exported to JavaScript
  ```

  Kotlin 2.3 对 `@file:JsExport` 更严格，文件内的 `typealias` 不允许导出 → **2.3.10 必须删除 `@file:JsExport`**。
  现已**恢复该删除**，并双版本实测：2.3.10 ✅ RC=0、2.1.21（应用同一改动）✅ RC=0，确认无回归。
- [x] 5.2 还原 `core` ohosArm64 `DateTime.kt` 的 `getTimeNanos → TimeSource` 改写（原写法恒为 0，属错误改写）
- [x] 5.3 **还原后回归验证** —— 已跑 `./verify_2.3.10.sh framework`：**BUILD SUCCESSFUL in 33s**（Kotlin 2.3.10 / AGP 8.6.0，60 tasks，仅 deprecation warning）。注：该轮为 Android target，`ohosArm64` 仍需 §7.4 覆盖

## 6. 分支治理

- [x] 6.1 适配 commit cherry-pick 到 `origin/main_internal` 最新 `9bc6cf5d0`（零冲突）
- [x] 6.2 收敛为单个 commit `edf9a9c8d`
- [x] 6.3 清理 spike 遗留文件（`LeakReproPage.kt` 等）
- [x] 6.4 工作区确认干净、无 `*.spike*` / `spikeKspTest` 残留

## 7. 待验证项（阻塞归档）

- [x] 7.1 **2.3.10 配置阶段验证** —— 还原 2 处后重跑通过（见 §5.3）
- [x] 7.2 **Android 编译冒烟**（2.3.10）—— `core` / `compose` / `core-annotations` / `core-ksp` / `core-wx` / `ui-tooling` / `core-gradle-plugin` 的 `compileDebugKotlinAndroid` 全部通过
- [x] 7.3 **iOS 编译冒烟**（2.3.10）—— `:core:compileKotlinIosArm64` **BUILD SUCCESSFUL in 17s**（K/N 链路打通）
- [x] 7.4 **OHOS 是否启用 2.3.10** —— 已核实 `core/build.2.3.10.gradle.kts` **未声明 `ohosArm64` target**（仅 `androidTarget` / `iosArm64` / `iosX64` / `iosSimulatorArm64` / `macosX64` / `js(IR)`），故 OHOS 保持 2.0.21 混合态，2.3.10 不覆盖鸿蒙。连带结论：`DateTime.kt`（位于 `ohosArm64Main`）的还原不会影响 2.3.10 构建，§5.3 的风险点自动消解。
- [x] 7.5 **OHOS 定制 Kotlin 2.3 跟进** —— 不阻塞本期：按混合态处理，等定制版本就绪后再单独起 change 接入
- [x] 7.6 **编译速度对比度量** —— 任务 `:core:compileKotlinIosArm64 --rerun --no-build-cache`，交替 3 轮以消除 daemon 冷热偏差：

  | 轮次 | 2.3.10 | 2.1.21 |
  |------|--------|--------|
  | run1 | 7s | 17s |
  | run2 | 5s | 26s |
  | run3 | 6s | 48s |
  | **中位** | **6s** | **26s** |

  **结论（仅限纯 Kotlin 编译）：2.3.10 约快 4 倍，且更稳定**（2.1.21 波动 17→26→48s，呈恶化趋势）。

  ⚠️ **但全量构建的结论相反** —— 见 §7.9。解读本条数据时必须连同 §7.9 一起看，不可单独引用"快 4 倍"。

  **测量局限（归档时需保留）**：仅覆盖单个 K/N 任务，未覆盖 Android / JS / 全量 clean build；两端 Gradle 版本不同（2.3.10 用 8.9，2.1.21 用 7.6.3），提速中 Gradle 贡献未拆分归因；2.1.21 样本波动大，建议后续以全量 clean build 复核。

- [x] 7.7 **demo 宿主编译冒烟** —— `./verify_2.3.10.sh`（默认模式，`settings.2.3.10.app.gradle.kts`）：**BUILD SUCCESSFUL in 40s**，88 actionable tasks，含 `:demo:compileDebugKotlinAndroid`
- [x] 7.8 **JS(IR) 编译冒烟** —— `:core:compileKotlinJs` **BUILD SUCCESSFUL in 5s**（H5 / 小程序链路的 Kotlin 侧可编译）
- [x] 7.9 **全量编译速度对比（端到端）** —— `clean` + 7 个框架模块 `compileDebugKotlinAndroid` / `compileKotlin`，`--no-build-cache`：

  | 版本 | 墙钟耗时 | tasks |
  |------|---------|-------|
  | 2.3.10 | **34s** | 104（68 executed） |
  | 2.1.21 | **30s** | 108（72 executed） |

  **结论：端到端全量构建 2.3.10 反而慢约 13%（34s vs 30s），与 §7.6 的单任务结论相反。**

  **归因分析**：全量构建不只是 Kotlin 编译，还包含 Gradle 配置 + AGP 任务（资源 / manifest / 打包）。2.3.10 分支被迫使用 Gradle 8.9 + AGP 8.6，而 2.1.21 分支用 Gradle 7.6.3 + AGP 7.4.2——**Kotlin 编译器省下的时间，被 Gradle / AGP 升级带来的额外开销抵消了**。

  **因此：本次适配"编译提速"的收益，目前只能确认存在于纯 Kotlin 编译环节，尚未在端到端全量构建中体现。** 是否值得升级，需要结合 Kotlin 2.3 的其他收益（新语言特性、后续版本支持）综合判断，不能仅凭"提速"立项。

- [x] 7.10 **KMP 产物级编译速度对比（iOS framework / Android APK）** —— 为避免 Gradle 8.9 与 7.6.3 互相污染本地状态，采用**两个独立项目隔离**：

  | | 2.1.21 | 2.3.10 |
  |---|---|---|
  | 项目 | `/Users/xubin/KuiklyProjetcts/KuiklyUI-Mirror` | `/Users/xubin/clientDemo/KuiklyUI-Mirror` |
  | Gradle | 7.6.3（原生） | 8.9（FileReplacer 切换） |
  | settings | 默认（`build.2.1.21.gradle.kts`） | `settings.2.3.10.app.gradle.kts` |
  | 环境变量 | `env -u` 剔除 AGP/Kotlin 变量 | `KUIKLY_AGP_VERSION=8.6.0` `KUIKLY_KOTLIN_VERSION=2.3.10` |

  统一条件：`clean` 后 `+ --no-build-cache`，任务 `:demo:linkPodDebugFrameworkIosArm64` / `:androidApp:assembleDebug`。

  | 产物 | 2.3.10 | 2.1.21 | 差异 |
  |------|--------|--------|------|
  | **iOS framework**（Kotlin→Native→framework） | **103s** | **110s** | 2.3.10 快 ~6% |
  | **Android APK**（Kotlin→JVM→DEX→APK） | **45s** | **67s** | 2.3.10 快 ~33% |

  **结论：在真实 KMP 产物构建上，2.3.10 两端均快于 2.1.21**（Android 端优势明显，iOS 端优势较小、接近噪声）。

  **与 §7.9 口径差异说明**：§7.9 测的是"框架模块 `compileDebugKotlinAndroid` 全量"（2.3.10 略慢 13%），本条测的是"完整产物"。两者不矛盾——**口径不同结论不同**：纯 Kotlin 编译 2.3 快，但 Gradle/AGP 升级有额外开销；纳入完整打包链路后，2.3.10 的整体耗时反而更优。

  **测量局限**：跨两个项目（不同 clone），机器状态与依赖缓存存在细微差异；iOS 端 6% 差距接近噪声，建议多轮复核。

## 8. 归档前置检查

- [x] 8.1 上述 §7 全部验证完成并回填结论
- [x] 8.2 更新本 tasks.md 勾选状态
- [x] 8.3 执行 `openspec archive adapt-kotlin-2-3-10`

## 11. 默认版本升级（本轮：由「2.1.21 共存」改为「默认 2.3.10」）

> 方向调整：原方案让 2.3.10 与 2.1.21 并行共存（靠 `-c settings.2.3.10.*` 切换）。
> 现目标改为**合入后默认版本即 2.3.10**，不再维护与 2.1.21 的共存切换，配置文件回归单一版本。

- [x] 11.1 **版本指针切换** —— `settings.gradle.kts` 的 `buildFileName` 由 `build.2.1.21.gradle.kts` → `build.2.3.10.gradle.kts`。
  这是切换默认的真正开关：`core` / `core-annotations` / `core-ksp` / `core-wx` / `core-render-android` / `compose` 六个模块**没有默认 `build.gradle.kts`**，只能靠该指针定位构建文件
- [x] 11.2 **Gradle wrapper 固化 8.9**（7.6.3 → 8.9）。此前为兼容 2.1.21 而保持 7.6.3、靠 FileReplacer 临时切换的做法废弃
- [x] 11.3 **`gradle.properties` 固化** —— 注释掉 `android.disableAutomaticComponentCreation`（AGP 8 已移除该属性）
- [x] 11.4 **`getKspArguments.kt` 固化** —— 写入 `@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")` 且 `getAdditionalArguments()` 改为无参调用（Kotlin 2.3 起 `KaptExtension.getAdditionalArguments` 变为 internal 无参）
- [x] 11.5 **修复 2.3.10 破坏性变更（构建脚本 DSL）**

  | 位置 | 2.3.10 报错 | 修法 |
  |---|---|---|
  | `apkbuilder/build.gradle.kts` | `jvmTarget: String` 已移除 | `compilerOptions { jvmTarget.set(JVM_1_8) }` |
  | `demo` / `demo_sub` | `moduleName: String?`、`outputFileName` 已移除 | `outputModuleName.set()` / `mainOutputFileName.set()` |
  | `core-render-web/base\|h5\|miniapp` | 同上 JS DSL | 同上 |
  | `core-render-web/miniapp` | `kotlinOptions { freeCompilerArgs += }` 已移除 | `compilerOptions.configure { freeCompilerArgs.addAll() }` |

  说明：这些文件是**默认** `build.gradle.kts`（不跟随版本指针），此前指向 2.1.21 时不会加载，切换默认后才暴露
- [x] 11.6 **原生/JS 配置同步** —— `core.podspec` / `core_annotations.podspec` 版本标识 → `2.0.0-2.3.10-SNAPSHOT`；`yarn.lock` 切到 `yarn.2.3.10.lock`
- [x] 11.7 **发布链路清理** —— `publish/2.3.10_publish.sh` 移除已固化的 FileReplacer 替换/还原调用；`verify_2.3.10.sh` 简化为直接用默认配置（不再需要 `-c` / 环境变量 / 替换）
- [x] 11.8 **CI 新增 2.3.10 模板** —— `job_publish_2.3.10.yaml`、`step_set_build_env_2.3.10.yaml`（JDK 17，Gradle 8.9 要求；用 `/usr/libexec/java_home -v 17` 动态定位）
- [x] 11.9 **外网仓库同步** —— `KuiklyUI` 的 `feature/kotlin-2.3.10` 分支同样改动（指针 / wrapper 8.9 / gradle.properties / demo 与 core-render-web × 3 的 JS DSL / podspec 版本标识 / 脚本与文档清理），提交 `73c5ada2`。

  外网实测（clean + `--no-build-cache` 冷构建）：

  | 任务 | 耗时 |
  |---|---|
  | `:core:compileKotlinIosArm64`（K/N 单任务） | 18s |
  | `:core:compileDebugKotlinAndroid`（JVM 单任务） | 4s |
  | 框架全量（9 个编译任务） | 19s |
  | `:androidApp:assembleDebug` → APK | 47s |
  | `:demo:linkPodDebugFrameworkIosArm64` → framework | 104s |
  | `:h5App:jsBrowserProductionWebpack` → JS bundle | 36s |
  | `:core-render-web:{base,h5,miniapp}:compileKotlinJs` | 3s |

  产物均实际落盘（APK 27.9MB、framework 已生成）。数据与内网测量吻合（iOS 103s/104s、APK 45s/47s），结论稳定。

### 11.10 默认 2.3.10 下的验证结果

| 验证项 | 结果 |
|---|---|
| 框架 8 模块 Kotlin 编译 | BUILD SUCCESSFUL |
| Android APK `assembleDebug` | BUILD SUCCESSFUL（26s） |
| iOS framework `linkPodDebugFrameworkIosArm64` | BUILD SUCCESSFUL（1m39s） |
| JS（base / h5 / miniapp）`compileKotlinJs` | BUILD SUCCESSFUL |
| **Android 运行时**（模拟器 Android14） | 路由页 + `TextExamplePage` 正常渲染（FontSize/FontWeight/Color/Shadow/Stroke 全部正常） |
| **iOS 运行时**（iPhone 16 Pro 模拟器） | 路由页正常渲染 |

> 运行时验证补齐了 §9 中"真机/运行时验证未做"的空缺。

### 11.11 遗留风险

- **CI 需 JDK 17**：现有 `step_set_build_env.yaml` 把 `org.gradle.java.home` 写死为 JDK 11，Gradle 8.9 无法运行。已新增 JDK 17 专用步骤，但**需流水线镜像确实安装 JDK 17**，否则 2.3.10 发布 job 会失败
- **历史版本不可用**：wrapper 升到 8.9 后，本分支上 2.1.21 及更早版本无法构建（AGP 不兼容）——这是"不共存"目标的预期代价
- **外网落后内网**：外网 `feature/kotlin-2.3.10` 缺少 `ui-tooling` / `core-gradle-plugin` 等模块的 2.3.10 资产同步，本轮只做了与默认构建直接相关的改动

## 9. 未能完成的验证（如实记录）

- **全量 clean build 速度对比**：未做。当前速度数据来自单任务 `:core:compileKotlinIosArm64`；两端 Gradle 大版本不同，全量构建的归因需要额外时间，留待后续。
- **JS / Web 运行时验证**：仅做到 `:core:compileKotlinJs` 编译通过（§7.8），H5 / 小程序的**运行时行为**未经 2.3.10 实测。
- **真机 / 运行时验证**：~~未做。本期仅验证编译通过，未验证 2.3.10 产物的运行时行为。~~
  → **已补齐（见 §11.10）**：Android 模拟器与 iOS 模拟器均实测通过，路由页与 `TextExamplePage` 渲染正常。
  仍**未覆盖**：真机（非模拟器）、H5 / 小程序的 JS **运行时**行为。
- **发布流程实跑**：未做。`publish/2.3.10_publish.sh` 未实际发布（避免污染制品库）。

## 10. 新发现的适配缺口（未解决，阻塞 JS 产物链路）

- [x] 10.1 **JS 编译链路已打通；产物计时受阻于执行环境（非适配问题）** —— `:h5App:jsBrowserProductionWebpack` 曾失败：

  ```
  Execution failed for task ':kotlinStoreYarnLock'.
  > Lock file was changed. Run the `kotlinUpgradeYarnLock` task to actualize lock file
  ```

  **根因（已修正）**：最初报 `kotlinStoreYarnLock` 失败（lock 校验）；升级 lock 后暴露**真正的编译错误**——`core-render-web:base` 的 `typealias` 不能被导出（详见 §5.1）。移除 `@file:JsExport` 后 **JS 编译已通过（RC=0）**。

  **残留项**：`kotlinNpmInstall` 仍失败，真实原因是执行环境的批量删除保护（`SAFE_DELETE_BULK_CONFIRM_REQUIRED`，累计 500 文件阈值）拦截了 `node_modules` 重建，**与 Kotlin 2.3 适配无关**，需在普通终端完成 P5 计时。

  **待决策**：`yarn.lock` 是共享文件（不版本化），与 `gradle-wrapper.properties` 面临同样的"两版本互斥"问题。候选方案：
  - 像 gradle-wrapper 一样交由 `FileReplacer` 在 2.3.10 构建时临时切换 lock 文件
  - 或为 2.3.10 单独维护一份 lock（如 `yarn.2.3.10.lock`）
  - 或执行 `kotlinUpgradeYarnLock` 更新共享 lock（**会影响 2.1.21 分支，需评估**）

  **影响面**：H5 / 小程序的 JS 产物在 2.3.10 下当前无法构建；不影响 Android / iOS。
