## ADDED Requirements

### Requirement: PackedFloats 类型占位

compose/commonMain 层 SHALL 定义 `expect class PackedFloats`，作为"两个 Float 合并后的打包值"的平台无关类型占位。

- `PackedFloats` 类型在 common 层不暴露任何成员（无字段、无方法、无构造），仅作为类型标签。
- 各平台 MUST 通过 `actual typealias` 把它映射到本平台最优的原生类型。
- Kotlin/JVM、Kotlin/Native、HarmonyOS target 的 actual MUST 等于 `Long`，以保持现有零分配零开销语义。
- Kotlin/JS target 的 actual MUST 等于 `Double`，以绕开 Kotlin/JS 的 Long 软件模拟开销。

#### Scenario: Android target 的 PackedFloats actual

- **GIVEN** 在 compose/src/androidMain（或共享 jvmLike 层级）
- **WHEN** 编译 `actual typealias PackedFloats = Long`
- **THEN** compose 模块 :compileDebugKotlinAndroid 任务 SHALL 编译通过
- **AND** 反编译 .class 文件观察 Offset.packedValue 字段 SHALL 为 primitive long

#### Scenario: iOS/macOS/HarmonyOS target 的 PackedFloats actual

- **GIVEN** 在 compose/src/appleMain、nativeMain、ohosArm64Main
- **WHEN** 编译 `actual typealias PackedFloats = Long`
- **THEN** 各 target 的 Kotlin/Native 编译任务 SHALL 编译通过

#### Scenario: JS target 的 PackedFloats actual

- **GIVEN** 在 compose/src/jsMain
- **WHEN** 编译 `actual typealias PackedFloats = Double`
- **THEN** compose 模块的 :compileKotlinJs 任务 SHALL 编译通过
- **AND** h5App 的 :jsBrowserDevelopmentWebpack 任务 SHALL 编译通过

### Requirement: PackedInts 类型占位

compose/commonMain 层 SHALL 定义 `expect class PackedInts`，作为"两个 Int 合并后的打包值"的平台无关类型占位。

- JVM/Native/HarmonyOS target 的 actual MUST 等于 `Long`。
- JS target 的 actual MUST 等于 `Double`，以覆盖 Int32 全范围（单个 JS number 的位运算范围是 32bit，无法安全承载 2×32bit，但 IEEE 754 double 的 53bit 尾数足够表示 2×32bit 正整数组合）。

#### Scenario: JVM/Native target 的 PackedInts actual

- **GIVEN** 在 compose/src/androidMain、appleMain、nativeMain、ohosArm64Main
- **WHEN** 编译 `actual typealias PackedInts = Long`
- **THEN** 编译 SHALL 通过

#### Scenario: JS target 的 PackedInts actual

- **GIVEN** 在 compose/src/jsMain
- **WHEN** 编译 `actual typealias PackedInts = Double`
- **THEN** 编译 SHALL 通过，且在 53bit 尾数内可安全承载任意 `(a: Int, b: Int)` 组合

### Requirement: PackedTextUnit 类型占位

compose/commonMain 层 SHALL 定义 `expect class PackedTextUnit`，用于 TextUnit 的 Float + 2bit type 打包。

- JVM/Native/HarmonyOS target 的 actual MUST 等于 `Long`。
- JS target 的 actual MUST 等于 `Double`，高 32 位存 Float（单位数值）bit 表示，低 32 位存 type flag 和 padding。

#### Scenario: 各 target 的 PackedTextUnit actual

- **GIVEN** 四个 target 下 compose/src/{androidMain,appleMain,nativeMain,ohosArm64Main}/ 与 jsMain/
- **WHEN** 分别声明 `actual typealias PackedTextUnit = Long` 与 `= Double`
- **THEN** 所有 target 编译通过

> **Note**：PackedTextUnit 在本次 change 的第一期可能延后实施；若第一期暂不改造，spec 仍登记但 tasks 中标记为 P4。

### Requirement: pack/unpack API 抽象

compose/commonMain/ui/util/InlineClassHelper.kt SHALL 把 pack/unpack 函数声明为 expect fun，由各平台提供 actual：

- `expect fun packFloats(v1: Float, v2: Float): PackedFloats`
- `expect fun unpackFloat1(p: PackedFloats): Float`
- `expect fun unpackFloat2(p: PackedFloats): Float`
- `expect fun packInts(v1: Int, v2: Int): PackedInts`
- `expect fun unpackInt1(p: PackedInts): Int`
- `expect fun unpackInt2(p: PackedInts): Int`

API 签名（名称、参数名、返回类型抽象名）在各 target 上 MUST 完全一致，确保 common 代码透明调用。

#### Scenario: JVM actual 实现语义

- **GIVEN** `actual fun packFloats(v1: Float, v2: Float): Long`
- **WHEN** 调用 `packFloats(x, y)` 随后 `unpackFloat1(result)` 和 `unpackFloat2(result)`
- **THEN** 返回值 SHALL 分别 bit-exact 等于 x、y（含 NaN、±Infinity 边界）

#### Scenario: JS actual 实现语义

- **GIVEN** `actual fun packFloats(v1: Float, v2: Float): Double`，内部使用共享 Float32Array/Float64Array 视图
- **WHEN** 调用 `packFloats(x, y)` 随后 `unpackFloat1(result)` 和 `unpackFloat2(result)`
- **THEN** 返回值 SHALL 分别 bit-exact 等于 x、y（含 NaN、±Infinity）
- **AND** 生成的 JS 代码 SHALL 不包含任何 Kotlin Long 类型操作（无 `Long_init_*`、`*.shl_*`、`*.and_*`、`*.or_*`、`*.shr_*`、`*.toLong_*`）

#### Scenario: Int 打包边界

- **GIVEN** `packInts(Int.MIN_VALUE, Int.MAX_VALUE)`
- **WHEN** 在所有 target 上分别调用 `unpackInt1` 和 `unpackInt2`
- **THEN** SHALL 分别还原为 `Int.MIN_VALUE` 和 `Int.MAX_VALUE`

### Requirement: bitEquals 哨兵比较 API

compose/commonMain SHALL 提供 `expect fun PackedFloats.bitEquals(other: PackedFloats): Boolean` 和 `expect fun PackedInts.bitEquals(other: PackedInts): Boolean`，用于替代原有的 `packedValue == other.packedValue` 哨兵比较。

- 语义：bit 级比较两个打包值是否完全相等，**忽略 IEEE 754 NaN 非自等语义**。
- JVM/Native actual MAY 直接使用 `this == other`（Long equals 本身就是 bit 比较）。
- JS actual MUST 使用 Int32 视图做 2 段 32-bit 比较，以规避 `Double.NaN !== Double.NaN` 问题。

#### Scenario: Unspecified 哨兵在所有平台一致返回 true

- **GIVEN** `Offset.Unspecified.packedValue` 是 `packFloats(NaN, NaN)` 的结果
- **WHEN** 在四个平台上调用 `Offset.Unspecified.packedValue.bitEquals(Offset.Unspecified.packedValue)`
- **THEN** 结果 SHALL 全部为 `true`

#### Scenario: Unspecified 与非 Unspecified 值的比较

- **GIVEN** `val unspec = Offset.Unspecified.packedValue`，`val zero = Offset.Zero.packedValue`
- **WHEN** 调用 `unspec.bitEquals(zero)`
- **THEN** 结果 SHALL 为 `false`

#### Scenario: 普通值的 bit 比较

- **GIVEN** `val a = packFloats(1.5f, 2.5f)`，`val b = packFloats(1.5f, 2.5f)`，`val c = packFloats(1.5f, 2.5001f)`
- **WHEN** `a.bitEquals(b)` 与 `a.bitEquals(c)`
- **THEN** 前者 SHALL 为 `true`，后者 SHALL 为 `false`

### Requirement: Offset / Size / IntOffset 等公共 API 行为保持不变

以下 value class 的 **对外公共 API**（构造、字段访问、运算符、copy、companion 常量、toString、equals、hashCode）在改造前后 MUST 保持完全一致的行为：

- Float 打包：Offset、Size、CornerRadius、DpOffset、DpSize、Velocity、ScaleFactor、TransformOrigin
- Int 打包：IntOffset、IntSize、TextRange

仅内部 `internal val packedValue` 字段的声明类型从 `Long` 改为 `PackedFloats`/`PackedInts`。

#### Scenario: Offset 公共 API 一致性

- **GIVEN** 改造后的 Offset 类
- **WHEN** 调用 `val o = Offset(1.5f, 2.5f)`，访问 `o.x`、`o.y`，执行 `o + Offset(0.5f, 0.5f)`，调用 `o.copy(x = 3f)`
- **THEN** 各调用 SHALL 返回与改造前语义完全一致的结果
- **AND** `Offset.Zero`、`Offset.Infinite`、`Offset.Unspecified` 的访问行为 SHALL 不变
- **AND** `o.isSpecified`、`o.isUnspecified` 在四个 target 上 SHALL 返回一致的 Boolean 值

#### Scenario: IntOffset 公共 API 一致性

- **GIVEN** 改造后的 IntOffset 类
- **WHEN** 调用 `val io = IntOffset(-100, 200)`，访问 `io.x`、`io.y`，执行 `io + IntOffset(1, 1)`
- **THEN** 结果 SHALL 与改造前一致

### Requirement: JS target 零 Long 验证

改造完成后，compose 模块在 JS target 产出的 js 代码中，与被改造 value class 相关的 pack/unpack/bitEquals 函数体 SHALL 不包含任何 Kotlin Long 类型的运行时调用。

#### Scenario: 生成代码检查 Offset 相关路径无 Long

- **GIVEN** 改造完成后执行 `./gradlew :compose:compileKotlinJs` 和 `./gradlew :h5App:jsBrowserDevelopmentWebpack`
- **WHEN** 检查生成的 `*.js` 产物中 `packFloats`、`unpackFloat1`、`unpackFloat2`、`Offset__plus_impl`、`_Offset___get_x__impl`、`_Offset___get_y__impl` 等函数体
- **THEN** 函数体内 SHALL 不出现 `new Long(`、`Long_init_`、`.shl_`、`.and_`、`.or_`、`.shr_`、`.toLong_`、`Long$Companion_getInstance` 这类 Kotlin/JS Long 运行时符号

### Requirement: JVM/Native target 零性能回归

改造完成后，compose 模块在 JVM 与 Native target 上的生成代码 SHALL 与改造前字节等价或仅在符号命名上不同，不得引入额外的装箱、间接调用、或 GC 分配。

#### Scenario: JVM 字节码等价性

- **GIVEN** 改造前后 compose/:jar 在 Android target 下的构建产物
- **WHEN** 对 `Offset$Companion.getZero-F1C5BW0()`、`Offset.plus-MK-Hz9U`、`Offset.getX-impl` 等关键方法做字节码反编译对比
- **THEN** 字节码主体 SHALL 仅在名称/签名上随字段类型别名变化而等价调整，不得多出装箱/额外方法调用

### Requirement: 不改造范围

本 change SHALL NOT 修改以下文件或类的 packedValue 实现：

- `ui/unit/Constraints.kt`（Constraints）——bit 布局复杂，本期排除。
- `ui/node/HitTestResult.kt` 的 `DistanceAndInLayer`（文件内 private inline）——本期评估后决定，默认不改。
- `core-render-*` 全部模块。
- `core/` 全部模块。
- `demo/`。

#### Scenario: Constraints 保持现状

- **GIVEN** 改造完成后
- **WHEN** 查看 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/unit/Constraints.kt`
- **THEN** 其 `value class Constraints(val value: Long)` 声明 SHALL 保持不变
