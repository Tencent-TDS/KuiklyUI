## Context

### 现状：JS 上 packed Long 的代价

KuiklyUI 的 compose fork（基于 JetBrains Compose Multiplatform 1.5.14.1）在 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/` 下有 14 个采用 "packed Long" 模式的 `@JvmInline value class`：

- **2×Float 打包（8 个）**：Offset、Size、CornerRadius、DpOffset、DpSize、Velocity、ScaleFactor、TransformOrigin
- **2×Int 打包（3 个）**：IntOffset、IntSize、TextRange
- **特殊位打包（3 个）**：TextUnit (Float + 2bit type)、Constraints (4 维 bit 打包)、HitTestResult.DistanceAndInLayer (文件内 private)

底层通过 `compose/src/commonMain/kotlin/com/tencent/kuikly/compose/ui/util/InlineClassHelper.kt` 的 `packFloats`/`unpackFloat1/2`/`packInts`/`unpackInt1/2` 把两个 32-bit 值塞进一个 64-bit Long。

在 JVM/Native 上这是零分配零开销的理想方案。但 Kotlin/JS IR 把 `Long` 实现为 `class { low: Int, high: Int }` + 软件模拟的 64 位运算函数（Long_init_0、times、shl、and、or、shr、toInt、equals 等）。POC 编译产物实测：

```js
// 每次 Offset(x, y) 构造，在 JS 上变成
toLong(i).shl_bg8if3_k$(32).or_v7fvkl_k$(toLong(i).and_4spn93_k$(new Long(-1, 0)))
// 每次读 .x 时
$this.shr_9fl3wl_k$(32).toInt_1tsl84_k$()
```

每次 pack 至少 3 次 `new Long(...)` + 若干 Long 方法调用。高频路径（PointerEvent 每次构造、Lazy 布局 placeable 循环、DragGesture delta 累加、AnimateAsState 插值、NodeCoordinator 坐标变换）每帧触发数十到数百次，导致 GC 持续抖动、CPU 占用显著。

用户已通过 Chrome DevTools Performance profile 确认 Long 相关函数是 h5App 热点路径上的主要开销。

### 为什么别的路走不通

| 路径 | 是否可行 | 原因 |
|---|---|---|
| 升 Kotlin 2.2 用多字段 value class | **否** | KEEP 里从未有 multi-field value class proposal；2.2 release notes 确认无此特性；roadmap 未承诺时间 |
| BigInt 替代 Long | **否** | 项目目标是 JSC（微信小程序）等引擎，BigInt 支持弱 |
| `expect value class` + 各平台 actual 不同字段类型 | **否** | POC 实测：Kotlin 2.1.21 要求 expect 与 actual 的 value class 主构造参数类型必须完全一致，不接受 Long/Double 混用 |
| jsMain 下 override InlineClassHelper 的 pack/unpack 实现（但 packedValue 字段仍是 Long） | **否** | Offset.packedValue 字段是 Long 意味着 JVM `packFloats` 返回 Long，JS 端也必须返回能赋给 Long 字段的 Long 值。JS 上无法绕开 Long 分配。 |

唯一可行路径是**把 packedValue 的底层类型本身做成 expect**，让 JS 端能换成 Double/Int。

### POC 验证结论（Kotlin 2.1.21）

在 `/tmp/kmp-valueclass-poc` 写了最小 KMP 项目验证：
1. ✅ `expect class PackedFloats` + `actual typealias PackedFloats = Long`（JVM）/ `= Double`（JS）**编译通过**（Beta warning，不阻塞）
2. ✅ common 层 `@kotlin.jvm.JvmInline value class CommonOffset(val packedValue: PackedFloats)` 编译通过
3. ✅ 生成的 JS 代码 `packFloats(v1, v2) { f32[0]=v1; f32[1]=v2; return f64[0]; }` — **零 Long 参与**
4. ✅ value class 构造/字段访问被 erase（`_CommonOffset___init__` 返回 packedValue 自身），Offset 对象本身零分配
5. ✅ `CommonOffset__equals_impl` 对 Double 字段直接走 number 比较，不走 Long.equals

## Goals / Non-Goals

### Goals

- 在 Kotlin 2.1.21 约束下，让 JS 端 Offset / Size / IntOffset 等高频 value class 的 pack/unpack/bitEquals 操作**彻底不引用 Long**。
- JVM / iOS / HarmonyOS / macOS 上生成代码与改造前**字节等价**，零性能回归。
- common 层的公共 API（构造、.x/.y、+/-/\*/÷、copy、Unspecified、equals）签名完全不变，对 compose 外部使用方零感知。
- 引入一套**可复用**的抽象（PackedFloats、PackedInts、PackedTextUnit），为后续其他 value class 的类似改造铺路。
- 改动局限于 compose/ 模块，不触及 core-render-\*、core、demo。

### Non-Goals

- **不改 `Constraints`**：其 bit 布局复杂（4 维度、变长 bit 宽度），第一期收益/风险比不合适。
- **不改 `core-render-*`**：已经用独立 Float 跨 bridge，与 packed 类型无耦合。
- **不升级 Kotlin 版本**。
- **不对外承诺 compose 模块二进制兼容**：内部字段类型变化会破坏 ABI；kuikly-compose 不是独立发布的 library。
- **不做 Compose 上游 rebase 友好性设计**：改动聚焦本 fork。

### 适用 DSL

本次改动**只影响 Compose DSL**（compose/ 模块）。自研 DSL（core/）不涉及 Offset 等 packed 类型。

### NativeBridge 影响

**无**。packed 类型从未跨 NativeBridge。core-render-web（h5App / miniApp）与 compose 之间的坐标/尺寸传递用的是独立 Float 参数（`updateOffsetMap(offsetX: Float, offsetY: Float, ...)`、`setContentOffset(params: String)`），本次改造完全封闭在 compose 模块 Kotlin 层内部。

## Decisions

### D1：用 `expect class` + `actual typealias` 把 packedValue 底层类型平台化

**选择**：`commonMain` 只声明 `expect class PackedFloats` 占位类型，不携带任何成员；`packFloats` 等函数在 common 层作为 `expect fun` 声明，以 PackedFloats 为返回/参数类型。各平台 `actual typealias` 到原生类型：JVM/iOS/HarmonyOS/macOS 一律 `Long`，JS 是 `Double`。

**替代方案与 pros/cons**：

| 方案 | 结果 |
|---|---|
| (A) `expect class` + 各平台字段不同的 `actual value class` | Kotlin 2.1.21 不支持（POC 实测报错 `actual constructor has no corresponding expected declaration`） |
| (B) 把 Offset 整体下沉到各平台 source set（same-name 类） | 210 个 common 文件 import 这些类型，下沉代价巨大、不现实 |
| (C) JS 上不改 packedValue 类型，只换 pack 函数实现 | Long 字段依然在，JS 侧仍要 new Long，无效 |
| (D) 把 Offset 改为普通 class `{x: Float, y: Float}` | 每次构造都分配对象；JVM 侧破坏现有零分配；全局回归 |
| (E) **采用的方案**：`expect class + actual typealias` | POC 编译通过、生成代码零 Long、JVM 端 typealias 完全无开销 |

**Rationale**：E 方案利用了 Kotlin 一个重要但低调的特性——`actual typealias` 可以把 `expect class` 映射到**已存在的任意类型包括原生 primitive**。JVM 上 `PackedFloats = Long` 编译后就是 Long，完全没有间接层；JS 上 `PackedFloats = Double` 同理。common 层的 `value class Offset(val packedValue: PackedFloats)` 在每个平台 inline 展开后等价于 "直接拿这个原生类型当 field"。

**代价**：
- Kotlin 官方把 `expect class + actual typealias` 标记为 Beta（KT-61573），会产生编译 warning。解决：compose/build 里加 `-Xexpect-actual-classes` compiler arg 消 warning。
- 实际语言特性稳定了多年，主流项目 (Kotlinx libs、androidx.compose 自身) 都在用。

### D2：packed 类型分 PackedFloats / PackedInts / PackedTextUnit 三类

**选择**：不用单一类型，按语义分三个独立的 `expect class`。

**为什么不统一成一个**：
- **JS 端能选的 actual 类型不同**：PackedFloats 必须用 Double（两个 Float 通过 Float32Array/Float64Array 视图做 bit 互转）；PackedInts 则可以用 Double 或 JS 用两个 Int 编码（例如 `high * 0x100000000 + low`，因 JS number 可精确到 53-bit 整数）；PackedTextUnit 是 Float + type 小标签，用 Double（高 32 位存 float，低 32 位存 type+padding）最方便。
- **JVM 端实际用同一个 `Long`** 作为 actual target 没问题，但 common 层要的是类型安全：`Offset(packed: PackedFloats)` 和 `IntOffset(packed: PackedInts)` 在 common 层必须不能相互误用。
- 未来若想针对某一类做特殊优化（比如 JS 上 PackedInts 改成 BigInt64），独立类型更容易演进。

### D3：NaN 哨兵比较改为 `bitEquals` expect fun

**问题**：现状代码 `isUnspecified: Boolean get() = packedValue == Offset.Unspecified.packedValue`（`Offset.kt:252,258` 等）依赖 `Long.equals` 做 bit 级比较。Offset.Unspecified 是 `Offset(NaN, NaN)`，打包成特定 Long 值，Long 的 equals 走位比较，`NaN Long == NaN Long` 返回 true。

但 JS 上 PackedFloats = Double，NaN 的 Double 做 `==` 会返回 **false**（IEEE 754 NaN 语义），导致 isUnspecified 永假。

**选择**：在 common 层提供 `expect fun PackedFloats.bitEquals(other: PackedFloats): Boolean`，所有 `packedValue == X.packedValue` 的地方改为 `packedValue.bitEquals(X.packedValue)`。各平台 actual：
- JVM / Native：直接 `this == other`（Long equals）
- JS：通过 Int32Array 视图比 32-bit 两段 `Int32View[0] == other[0] && Int32View[1] == other[1]`，保证 bit 级比较忽略 NaN 语义

同理需要 `PackedInts.bitEquals`（JS 上如果 Int 编码用了会丢失符号位等边界，需要 bit-exact 比较）。

### D4：hashCode 处理

value class 的 hashCode 默认走 backing field 的 hashCode。Long.hashCode 在 JS 上是 Long 方法调用。改成 Double/Int 后：
- PackedFloats = Double：`Double.hashCode()` 在 JS 上走 `getNumberHashCode`，比 Long 便宜。
- PackedInts = Int：`Int.hashCode() == this`，更便宜。

**决定**：不单独处理，复用 Kotlin 自动生成的 value class hashCode 即可。

### D5：Unspecified 常量的定义

现状：`val Unspecified = Offset(Float.NaN, Float.NaN)`。

**问题**：`Offset(Float, Float)` 顶级构造函数 `fun Offset(x: Float, y: Float) = Offset(packFloats(x, y))` 在 JS 上每次调用会经过共享 Float32Array 视图，多次初始化读写。Unspecified 是个常量，每次读不应重算。

**决定**：Unspecified 依然按 `Offset(NaN, NaN)` 写，但靠 `val`/`@Stable` + companion object 的**延迟初始化一次性**特性，单次初始化成本可忽略。无需额外优化。

如果后续 profile 显示 Unspecified 多次构造成问题（不太可能），再引入 `internal val UnspecifiedPackedValue: PackedFloats` 的常量即可。

### D6：JS actual 的 Float32Array/Float64Array 视图共享

**选择**：在 `jsMain` 的 `InlineClassHelper.js.kt` 里放一对 top-level 单例 `f32 = Float32Array(2)`、`f64 = Float64Array(f32.buffer)`。所有 packFloats/unpackFloat1/2 调用共享这两个视图。

**线程安全**：Kotlin/JS IR 产物是单线程执行（即便在 Web Worker 里，每个 Worker 也是独立 JS realm，各自持一份视图）。无竞争问题。

**性能**：共享视图避免每次 pack 都 `new Float32Array(2)`。POC 实测生成代码：
```js
get_f32()[0] = v1; get_f32()[1] = v2; return get_f64()[0];
```
三条 TypedArray 操作 + 一个 number 返回，比 Long 模拟便宜得多。

### D7：PackedInts 的 JS actual 选型

两个候选：
- **(a) Double actual**：`typealias PackedInts = Double`。用 Int32Array 视图拼接 2 个 32-bit Int 到一个 Double。和 PackedFloats 对称、一致。
- **(b) Int actual + 位压缩**：要求两个 Int 都在 16-bit 范围才能塞一个 Int（JS number 位运算是 32-bit）。IntOffset 的 x/y 理论上是 Int32 全范围，不够用。

**决定**：第一期用 **(a) Double actual**，实现统一、类型足够、性能差异可忽略。

### D8：TextUnit 的打包

TextUnit 原设计：高 32 位 Float 值 + 低 2 位 type flag（Unspecified/Sp/Em）。总体也是 Long。

**决定**：独立 `expect class PackedTextUnit`，JVM = Long、JS = Double（与 PackedFloats 同策略）。本期若时间紧可以放到第 3 期；但 TextUnit 在 Compose text 渲染路径上也不算罕见。

### D9：不改 Constraints 的决策

Constraints 的 Long 编码：2 bit 压缩类型选择子 + 4×30/18 bit 的 min/max width/height。bit 布局的 pack/unpack 函数大量位运算（shl、or、and、shr），JS 下要么一起用 Double + Int 视图重写（工作量大），要么接受 Long 开销（局部回归）。

**决定**：第一期完全不动 Constraints.kt。它的 packedValue 字段保持 Long。Layout pass 每次走 Constraints 也是热点，但一个值会被大量 readOnly 访问而非频繁构造，Long 的构造次数比 Offset 低。后续专题评估。

## Risks / Trade-offs

| 风险 | 影响 | 缓解 |
|---|---|---|
| `expect class + actual typealias` 是 Beta (KT-61573) | 未来 Kotlin 升级可能调整语法 | 加 `-Xexpect-actual-classes` 编译标志；该特性实际稳定多年，生态广泛使用；真有 breaking 改动时 typealias 换成真正 class 即可 |
| NaN 语义在 JS 上变化导致 isUnspecified/isSpecified 错判 | 隐蔽的正确性 bug，可能导致 UI 元素错位、手势事件丢失 | 所有 `packedValue ==` 比较强制替换为 `bitEquals`；新增 unit 测试覆盖 Offset.Unspecified、Size.Unspecified、IntOffset.Unspecified 的 isSpecified/isUnspecified 在四个 target 上的行为 |
| `-Xexpect-actual-classes` 在 multi-target 编译下关闭的 warning 里可能掩盖真正的 actual 不匹配 | 调试困难 | 改造期间先不加这个 flag，带 warning 一起看；全部改完后再加 |
| 共享 Float32Array 视图在 suspend/reentrant 场景下的覆盖 | pack 进行中被打断，f32 写了 v1 但 v2 被另一次 pack 覆盖 | Kotlin/JS 单线程执行 + pack 函数是纯同步、无 suspend 点，不会被打断；但为保险，pack 函数全部 `@PublishedApi internal inline`，单次连续读写；unpack 同理 |
| Compose 源码 diff 扩大，未来想 rebase 上游增加成本 | merge 冲突 | diff 主要集中在 `packedValue: Long` → `packedValue: PackedFloats` 这一行 + InlineClassHelper 抽象；提取为明确 patch 便于维护；proposal 已声明 kuikly-compose 不是库级产出，可接受 |
| bitEquals 函数调用 vs 原生 `==` 的额外间接 | 每次哨兵比较多一次函数 entry | JVM 下 inline 函数零开销；JS 下也是简单函数调用，比 Long.equals 便宜 |
| 14 个 value class 中有的是业务代码/三方代码直接引用 packedValue 字段 | 编译失败 | packedValue 字段是 `internal`，跨模块不可访问；compose 内部访问点已在调研阶段枚举（约 30 处），集中在 InlineClassHelper 调用和 Unspecified 比较，全部纳入改造范围 |
| TextUnit 打包第一期改不改的决策反复 | 返工 | 提议**第一期先改 Float 系列 8 个 + Int 系列 3 个共 11 个**；TextUnit 放第二期单独跑 |
| 某个下游 JS 构建（H5 / 小程序）运行时差异导致 Float32Array 视图行为不一致 | 正确性问题 | 在 h5App 和 miniApp 各跑一次基础 demo（Offset 相关的 Column/Row/LazyColumn/拖拽）烟雾测试；必要时加 assertion |

## Migration Plan

1. **P0（基础设施）**：新增 `PackedValue.kt` + 改 `InlineClassHelper.kt`，添加 expect/actual，不修改任何 value class。跑通 6 target 编译。
2. **P1（Offset 单点打通）**：只改 `Offset.kt` 的 packedValue 类型和 Unspecified 比较。跑通编译 + h5App 手势/滚动 demo，用 Chrome DevTools 对比改前改后 profile。
3. **P2（Float 系列推广）**：如果 P1 收益符合预期，把其他 7 个 Float 系列 value class（Size、CornerRadius、DpOffset、DpSize、Velocity、ScaleFactor、TransformOrigin）一次性改完。
4. **P3（Int 系列）**：IntOffset、IntSize、TextRange。
5. **P4（可选 TextUnit）**：如果时间/收益允许，改 TextUnit。
6. **Rollback**：每个 P 阶段独立 commit，如果某阶段引入回归，revert 到上一阶段。P0 单独作为基础设施 commit，可保留不 revert。

## Open Questions

1. 是否需要立刻 enable `-Xexpect-actual-classes` 编译 flag 消 Beta warning？
   - **建议**：改造期间不加，全部改完验证无问题后在最后一个 P 阶段统一加。
2. 是否需要给 `bitEquals` 做 inline 标记？
   - **建议**：common 层声明 `expect fun`（不能 inline expect），JVM/Native actual 用普通 fun（JIT 自会 inline），JS actual 用普通 fun。如果 JS 下 profile 显示 bitEquals 调用开销显著，再改为 jsMain 侧的 inline。
3. 第一期验证用什么 profile 方法？
   - **建议**：h5App 自带 demo 选一个典型场景（LazyColumn 快速滑动），Chrome DevTools Performance 录 5 秒，看 "Bottom Up" 视图里 `Long_` 前缀函数的总耗时 + "Memory" 的 young-gen allocation rate。改前改后对比。
4. 要不要添加针对 packed 类型的 microbenchmark？
   - **建议**：不做独立 benchmark（项目无既有 bench 基础设施）。依赖真实 demo 场景的 profile 数据。
