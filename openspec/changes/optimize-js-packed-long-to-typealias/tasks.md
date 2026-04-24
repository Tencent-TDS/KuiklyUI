## 1. 基础设施（P0）— compose 模块

**实施调整说明**：落地时发现 common 层已有 8+ 个文件（SpringSimulation、SliderRange、SpanRange、DistanceAndInLayer、colorspace/\*）直接把 `packFloats`/`packInts` 返回值存成 `Long` 字段。为保持 P0 阶段的改动最小可渐进，**保留原 `packFloats`/`unpackFloat1/2`/`packInts`/`unpackInt1/2` 函数不动（Long-returning）**，另外新增 `packFloatsP`/`unpackFloat1P`/`unpackFloat2P`/`packIntsP`/`unpackInt1P`/`unpackInt2P` 作为 expect fun（返回 `PackedFloats`/`PackedInts`），加上 `packedFloatsBitEquals`/`packedIntsBitEquals`。P1+ 阶段让受影响的 value class 逐个从老函数切到新函数。

- [x] 1.1 创建 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/util/PackedValue.kt`，定义 `expect class PackedFloats`、`expect class PackedInts`、`expect class PackedTextUnit`
- [x] 1.2 改造 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/util/InlineClassHelper.kt`：原有 `packFloats`/`unpackFloat1/2`/`packInts`/`unpackInt1/2` 保留；新增 `expect fun packFloatsP`/`unpackFloat1P`/`unpackFloat2P`/`packIntsP`/`unpackInt1P`/`unpackInt2P` 返回/参数类型使用 `PackedFloats`/`PackedInts`；新增 top-level `expect fun packedFloatsBitEquals(a,b)` 和 `packedIntsBitEquals(a,b)`（top-level 而非扩展，避免 JVM 端 typealias 展开后签名冲突）
- [x] 1.3 在 `compose/src/androidMain/kotlin/com/tencent/kuikly/compose/ui/util/` 新增 `PackedValue.android.kt`（actual typealias Long）和 `InlineClassHelper.android.kt`（actual fun 全部 Long 位运算实现）
- [x] 1.4 由于 appleMain 在项目中只存 ios 专属代码、nativeMain 承担所有 native 共享 actual，本任务合并到 1.5
- [x] 1.5 在 `compose/src/nativeMain/kotlin/com/tencent/kuikly/compose/ui/util/` 新增 `PackedValue.native.kt` 和 `InlineClassHelper.native.kt`（覆盖 iosX64/iosArm64/iosSimulatorArm64/macosX64/macosArm64/ohosArm64 所有 native target）
- [x] 1.6 ohosArm64 从 nativeMain 自动继承，无需独立文件
- [x] 1.7 在 `compose/src/jsMain/kotlin/com/tencent/kuikly/compose/ui/util/` 新增 `PackedValue.js.kt`（actual typealias Double）和 `InlineClassHelper.js.kt`（使用共享 Float32Array+Float64Array+Int32Array 视图实现零 Long 版本的 pack/unpack/bitEquals）
- [x] 1.8 运行 `ANDROID_HOME=~/Library/Android/sdk ./gradlew :compose:compileDebugKotlinAndroid` — BUILD SUCCESSFUL
- [x] 1.9 运行 `./gradlew :compose:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL（选一个 apple target 代表，其他继承自 nativeMain actual 相同）
- [x] 1.10 运行 `./gradlew :compose:compileKotlinJs` — BUILD SUCCESSFUL
- [ ] 1.11 此阶段独立 commit：`refactor(compose): introduce PackedFloats/PackedInts expect types with platform-specific actuals`

## 2. Offset 单点打通（P1）— 验证方案可行

- [ ] 2.1 修改 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/geometry/Offset.kt`：把 `value class Offset internal constructor(internal val packedValue: Long)` 改为 `value class Offset internal constructor(internal val packedValue: PackedFloats)`
- [ ] 2.2 在同文件内，把所有 `packedValue == Unspecified.packedValue` 比较改为 `packedValue.bitEquals(Unspecified.packedValue)`（约 2 处：x getter、y getter 的 check 逻辑）
- [ ] 2.3 修改 `Offset.kt` 底部 `val Offset.isSpecified` / `val Offset.isUnspecified` 扩展属性（`Offset.kt:252,258`），使用 `bitEquals` 替换 `==`
- [ ] 2.4 搜索 `compose/src/commonMain/` 下其他对 `Offset.packedValue` 的直接比较（`grep -rn "\.packedValue ==\|\.packedValue !=" compose/src/commonMain/kotlin/` 找 Offset 相关点），全部改为 `bitEquals`
- [ ] 2.5 运行 `./gradlew :compose:compileDebugKotlinAndroid :compose:compileKotlinJs` 确认双 target 编译通过
- [ ] 2.6 生成 h5App debug 产物：`./gradlew :h5App:jsBrowserDevelopmentWebpack`，在生成的 js 文件中 grep 确认 `packFloats`/`Offset__plus_impl`/`_Offset___get_x__impl` 函数体不再包含 `Long_init_`、`.shl_`、`.and_`、`.or_`、`.shr_`、`new Long(` 符号
- [ ] 2.7 启动 h5App（`./gradlew :h5App:jsBrowserDevelopmentRun`），手动操作一个包含 Column/Row/LazyColumn/Button 点击/拖拽的 demo 页面，肉眼验证布局、点击、滚动行为正确，Offset.Unspecified 相关代码路径不崩溃
- [ ] 2.8 用 Chrome DevTools Performance 录制 5 秒 LazyColumn 快速滑动场景，对比改前（P0 commit）与改后的火焰图，确认 `Long_init_*` 等符号在 Offset 相关调用栈中消失，截图两组 profile 数据
- [ ] 2.9 在 iOS 模拟器上跑一次 iosApp（Offset 相关 demo 页面），肉眼验证 JVM 侧语义等价（使用 .agents/skills/kuikly-app-runner 工具链）
- [ ] 2.10 此阶段独立 commit：`refactor(compose): migrate Offset.packedValue to PackedFloats`

## 3. Float 系列推广（P2）— Size / CornerRadius / 等 7 个

- [ ] 3.1 修改 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/geometry/Size.kt`：packedValue 类型 Long → PackedFloats，Unspecified 哨兵比较改 bitEquals
- [ ] 3.2 修改 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/geometry/CornerRadius.kt`：同上
- [ ] 3.3 修改 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/unit/Dp.kt` 中的 `value class DpOffset` 和 `value class DpSize`：同上
- [ ] 3.4 修改 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/unit/Velocity.kt`：同上
- [ ] 3.5 修改 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/layout/ScaleFactor.kt`：同上
- [ ] 3.6 修改 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/graphics/TransformOrigin.kt`：同上
- [ ] 3.7 搜索整个 compose/commonMain 下对这 7 个类型的 `.packedValue ==` 或 `.packedValue !=` 的引用点，全部改 `bitEquals`
- [ ] 3.8 运行 `./gradlew :compose:compileDebugKotlinAndroid :compose:compileKotlinJs` 验证编译
- [ ] 3.9 运行 `./gradlew :h5App:jsBrowserDevelopmentWebpack`，grep 确认 Size/CornerRadius 等相关函数也无 Long 符号
- [ ] 3.10 在 h5App 跑一个含尺寸/Dp/Velocity 相关特性的 demo 页面（建议 Scrollable + 动画页面），肉眼 & 行为验证
- [ ] 3.11 iOS 模拟器回归验证
- [ ] 3.12 此阶段独立 commit：`refactor(compose): migrate Size/CornerRadius/Dp/Velocity/ScaleFactor/TransformOrigin packedValue to PackedFloats`

## 4. Int 系列推广（P3）— IntOffset / IntSize / TextRange

- [ ] 4.1 修改 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/unit/IntOffset.kt`：packedValue 类型 Long → PackedInts，Unspecified（若有）/其他哨兵比较改 bitEquals
- [ ] 4.2 修改 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/unit/IntSize.kt`：同上
- [ ] 4.3 修改 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/text/TextRange.kt`：同上
- [ ] 4.4 搜索 compose/commonMain 下对这 3 个类型的 `.packedValue ==/!=` 全部改 bitEquals
- [ ] 4.5 在 jsMain 的 InlineClassHelper.js.kt 中实现 `actual fun packInts` 与 `actual fun unpackInt1/2`：使用 `Int32Array(2)` + `Float64Array(buffer)` 视图。验证 Int.MIN_VALUE 和 Int.MAX_VALUE 的 round-trip 正确性（在 P0 tasks.md 1.7 时先预留，此处确认已实现）
- [ ] 4.6 新增 KMP commonTest（如 compose 暂无 commonTest 基础设施则改为 jsTest + jvmTest 各一个 round-trip 单测），覆盖 `packInts(MIN, MAX)` → unpack 还原等价
- [ ] 4.7 运行编译验证（Android + JS + iOS）
- [ ] 4.8 生成 h5App 产物 grep 确认 IntOffset 相关函数无 Long
- [ ] 4.9 h5App 跑 LazyColumn（item 大量使用 IntOffset.place）滑动 demo，iOS 模拟器跑同样页面
- [ ] 4.10 此阶段独立 commit：`refactor(compose): migrate IntOffset/IntSize/TextRange packedValue to PackedInts`

## 5. TextUnit（P4，可选）

- [ ] 5.1 评估收益：用 Chrome DevTools 抓 text 渲染密集页面 profile，确认 TextUnit 相关 Long 符号占比
- [ ] 5.2 如果 5.1 显示收益足够，修改 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/unit/TextUnit.kt`：packedValue Long → PackedTextUnit
- [ ] 5.3 实现 JS actual：Double 高 32 位存 float bit，低 32 位存 type+padding。注意 pack/unpack 函数（可能叫 pack 之外的名字，如 `packFloatAndByte`）也要 expect/actual
- [ ] 5.4 编译与 grep 验证
- [ ] 5.5 text 相关 demo（文本样式页）冒烟测试
- [ ] 5.6 此阶段独立 commit：`refactor(compose): migrate TextUnit packedValue to PackedTextUnit`

## 6. 收尾

- [ ] 6.1 在 `compose/build.2.1.21.gradle.kts` 的 `freeCompilerArgs` 中追加 `-Xexpect-actual-classes` 消除 Beta warning
- [ ] 6.2 验证所有 target 编译无 warning（或只剩无关 warning）
- [ ] 6.3 完整跑一遍 `./gradlew :compose:build`（covers androidTest 等）
- [ ] 6.4 运行 `openspec validate optimize-js-packed-long-to-typealias`
- [ ] 6.5 更新 AGENTS.md 或 .ai/ 中的 compose 开发指南，备注"Offset/Size 等打包类型在 JS 上使用 Double 存储"（如果存在相关文档段落）
- [ ] 6.6 最终 commit：`chore(compose): enable -Xexpect-actual-classes and finalize packed-value migration`
