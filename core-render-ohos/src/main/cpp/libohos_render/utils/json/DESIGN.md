<!--
 Tencent is pleased to support the open source community by making KuiklyUI available.
 Copyright (C) 2026 Tencent. All rights reserved.
 Licensed under the License of KuiklyUI; see https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
-->

# KRJSON 设计说明

> **当前编码**：指针低位 tag，见仓库根目录 [KRJSON-pointer-tagging.md](../../../../../../../KRJSON-pointer-tagging.md)（含盒布局、`HeapAllocator`、small-slab）。  
> 下文 Scheme A（低字节 tag + 指针左移 8）为历史实现，仅作对照。

`KRJSON` 是 OHOS render 层的 JSON 值类型。它是一个**标签化的 8 字节值**（`typedef uint64_t KRJSON`），
同一脉络于 NSNumber tagged pointer / JSC `JSValue` / ArkCompiler `JSTaggedValue`：一个字要么直接编码一个立即值，
要么编码一个指向引用计数堆对象的指针。对外只暴露 **C API**（`api/include/Kuikly/KRJSON.h`，接入 Kotlin/Native cinterop）。

---

## 通用约定

- `KRJSON` 是 POD `uint64_t`：**按值拷贝不改变引用计数**。共享所有权必须显式 `KRJSONRetain`，释放用 `KRJSONRelease`。
- 立即值（标量）无堆分配，`Retain`/`Release` 为 **no-op**；堆对象带原子 `rc`（`std::atomic<int32_t>`，初值 1）。
- 访问器：标量按值返回；`KRJSONGetString` 只对 `kTagString`（UTF-8）返回借用的 NUL 结尾 `const char*`。`kTagU16String` 必须走 `KRJSONGetStringUTF16`（借用 `const uint16_t*`）。Dump / C++ `stringValue()` 若需要 UTF-8 文本，在调用方把 UTF-16 转成 UTF-8，不写回盒子。object key **一律 UTF-16**；`ObjectPut` / `KRJSONParse` 的 UTF-8 key 在边界 `UTF8ToUTF16` 后写入。ArkTS stringify 用 `ParseUTF16`：叶子是 `kTagU16String`。Kotlin / NAPI 对象用 `NewObject` / `NewObjectUTF16`（二者等价）。叶子字符串：`KRJSONParse` / `Make(std::string)` 仍是 UTF-8 盒；二进制 `rectData` 也走 UTF-8。`MakeUTF16` 仅用于 C ABI `const char*`。其余链路在源头产出 `u16string` 再 `Make()`。
- 容器访问（`KRJSONArrayGet`/`KRJSONObjectGet`）返回 **borrowed** 值；如需延长寿命，调用方自行 `KRJSONRetain`。
- 堆对象析构需**手动**递归 `Release` 子 cell（`vector<KRJSON>` / map value 是 POD，不会自动释放）。
- 盒级堆分配一律走 `HeapAllocator`（默认 `DefaultHeapAllocator`）：`Allocate<alignof(T)>(bytes)`；小对象（≤128B 且对齐 ≤16）走分配器内部 slab，其余 `posix_memalign`。`std::vector` / `u16string` 缓冲仍用标准库 `new`。

---

## Scheme A（历史）— 低字节 tag

```
bits[0..7]   = 存储 tag（kTag*）
bits[8..63]  = 立即值 payload  或  48 位指针（堆类型：ptr = v >> 8，构造 v = (ptr<<8)|tag）
```

| tag | 类型 | 存储 |
|-----|------|------|
| kTagNull(0)   | null | 立即值（v==0） |
| kTagBool(1)   | bool | 立即值，bit8 = 0/1 |
| kTagInt(2)    | 整数 | 立即值，56 位有符号整数（±2^55；含 ms 时间戳） |
| kTagDouble(3) | double | 堆 `NumberBox`（double 需 64 位，无法内联） |
| kTagInt64(4)  | 整数 | 堆 `NumberBox`（超 56 位的 int64） |
| kTagUint64(5) | 无符号 | 堆 `NumberBox`（> int64 max） |
| kTagString(6) | string | 堆 `StringBox`：UTF-8 `[rc][len bytes][bytes][NUL]`；`GetType` = `KRJSON_STRING`(6) |
| kTagArray(7)  | array | 堆 `ArrayBox { rc; vector<KRJSON> }` |
| kTagObject(8) | object | 堆 `ObjectBox { rc; vector<pair<string,KRJSON>> }`（保序，线性查找；对小对象比 hash map 更快、更省分配） |
| kTagBytes(9)  | bytes | 堆 `BytesBox`（一次尾部分配，`[rc][len][bytes]`，不可变） |
| kTagInt32(10) | 整数 | 立即值；`GetType` 折叠为 `KRJSON_INT`(2) |
| kTagFloat(11) | float | 堆 `NumberBox`；`GetType` = `KRJSON_FLOAT`(11) |
| kTagLong(12)  | long | 堆 `NumberBox`；`GetType` = `KRJSON_LONG`(12) |
| kTagU16String(13) | string | 堆 `U16StringBox`：UTF-16 `[rc][unit_count][units][0]`；`GetType` = `KRJSON_U16STRING`(13) |

**判别值/指针**：`type = v & 0xFF`；`type < kFirstHeapTag(=3)` → 立即值（就地解码，整数用算术右移 8 位 sign-extend）；
否则 → 指针 `(HeapBox*)(v >> 8)`（逻辑右移）。`kTagInvalid=0xFF` 为错误哨兵，`AsBox` 返回 null → retain/release 安全 no-op。

- **优点**：可移植、无地址空间假设；天然 56 位内联整数；string 走堆 → 借用 `const char*` 语义干净。
- **代价**：**double 入堆**（每个 1 次分配）；短字符串不内联（未来可加，但会破坏借用指针语义）。
- object 用**扁平 `vector<pair>`** → 保留插入顺序（`Dump` 可精确往返）；小对象线性查找比 hash map 更快、分配更少。大对象/高频查找场景如需 O(1) 可换 hash（会牺牲上述优势）。

---

## Scheme B（备将来）— NaN-boxing（JSC `JSValue` / ArkCompiler `JSTaggedValue` 家族）

把非 double 编码进 IEEE-754 double 的 NaN 空间；**double 原生内联、零分配**（数值密集 JSON 的关键优势）。

```
kNumberTag    = 0xFFFF000000000000   // 高 16 位
kDoubleOffset = 0x0001000000000000   // 2^48
int32 : kNumberTag | (uint32)i                        // 高16 全 1
double: bitcast<u64>(purifyNaN(d)) + kDoubleOffset    // 编码；解码减 offset
ptr   : 原样（高16 = 0，低 48 位地址）                  // aarch64 用户态 48 位 VA
特殊  : null=0x02, false=0x04, true=0x06（高16=0，<0x1000，永不等于真实 box 指针）
```

判别：
```
IsInt32(v)  : (v & kNumberTag) == kNumberTag          // 高16 == 0xFFFF
IsDouble(v) : (v & kNumberTag) != 0 && !IsInt32(v)    // 高16 ∈ [0x0001, 0xFFF1]
IsSpecial(v): v==null||v==false||v==true
IsPointer(v): (v & kNumberTag)==0 && v >= 0x1000      // 真实堆 box
```

### 核心：为什么“任意 64 位”的 double 不会与指针/int 混淆
一个 double 的原始位型可为任意 64 位（`+0.0=0x0…0` 撞 null 指针，微小 subnormal 撞堆指针，负 NaN `0xFFFF…` 撞 int tag）。
NaN-boxing 用**两个机制**消除歧义，缺一不可：

1. **NaN 净化**：只有 NaN 才有“任意高熵”位型（`0x7FF0…01..`、`0xFFF8..`）。把所有 NaN 收敛为单一规范值
   `0x7FF8000000000000`。净化后 double 位型被限制在 `0x0000…0000 .. 0x7FF8…`(正,含规范NaN) 与
   `0x8000…0000 .. 0xFFF0…0000`(负,含 -Inf)。
2. **加偏移 2^48**：编码时 `+2^48`：
   - 最小 `+0.0=0x0…0` → `0x0001000000000000`（高16=0x0001 ≠ 0x0000）→ **绝不像指针**；
   - 最大(净化后) `-Inf=0xFFF0000000000000` → `0xFFF1000000000000`（高16=0xFFF1 ≠ 0xFFFF）→ **绝不像 int**；
   - 会绕回撞进指针区的负 NaN（`0xFFFF…`+offset→`0x0000…`）已被步骤 1 删除。

⇒ 所有 double 编码后高 16 ∈ [0x0001, 0xFFF1]，与指针(0x0000)、int(0xFFFF) 三段严格不相交。

- **优点**：double 零分配（数值密集 JSON 最快）；OHOS/ArkCompiler 同平台已验证。
- **代价**：实现更复杂；依赖 48 位 VA（**52 位 LVA / MTE 会破坏“高16=0”前提**，需 `static_assert` + 文档标注）；短字符串内联位数更紧；仅小端。
- **参考**：OpenHarmony `arkcompiler_ets_runtime` → `ecmascript/js_tagged_value.h`；ARM ARM DDI 0487「Address tagging」；Linux `Documentation/arm64/tagged-pointers.rst`。

---

## 从 Scheme A 迁移到 Scheme B 的注意点
- `KRJSON` 公开类型不变（仍是 `uint64_t`）；仅内部编码/判别 + `Box.cpp` 的 encode/decode/accessor 需改写。
- C API 签名不变（`KRJSON.h`），故 cinterop / Kotlin 侧无需改动。
- double 从堆迁为内联后，`NumberBox` 只剩“超范围整数”用途；`Dump`/accessor 分支相应调整。

---

## 性能现状（真机 release，RelWithDebInfo -O2，aarch64）

1000 对象 ×200 次 / int-array 20000 元素；对象为**扁平 vector**：

| profile | cJSON | Scheme A DOM | RapidJSON SAX |
|---|---|---|---|
| obj-0double | 基准 | **1.13x** | 4.34x |
| obj-1double | 基准 | **1.03x** | 3.09x |
| obj-4double | 基准 | **1.14x** | 4.15x |
| int-array | 基准 | **4.77x** | 9.27x |

结论：实测 workload 上 Scheme A DOM 已全面快于 cJSON，SAX 更是 3–9x。边界：object 线性扫描，**超大对象 + 高频按 key 查找**时理论上会被 hash map 反超（当前 render 小对象场景领先）。

---

## 将来可能的优化（尚未采用，仅记录）

### 已落地：分配器内 small-slab
`DefaultHeapAllocator` 已对 ≤128 字节、对齐 ≤16 的请求做 256 槽 slab + TLS 缓存（见 pointer-tagging.md §11）。覆盖短 UTF-16（JsonPlatform p90/p95 约 32 unit）、短 UTF-8 与 `NumberBox`。**不改所有权**。array/object 的 vector 缓冲仍走 malloc。

### arena / bump 分配（潜在的最大 DOM 提速）
现状大对象与容器缓冲仍是独立系统分配。arena 预分配大块、指针 bump、**整块一次性释放**（yyjson/simdjson/RapidJSON pool 的做法），可把每次解析的大量 malloc 再压下去。

**关键冲突**：arena 只能整体回收，与"每节点独立引用计数（某节点 rc 归零即单独释放）"矛盾。故落地需在下列路线间取舍：
- **(a) 定长/小对象 slab**：已由 `DefaultHeapAllocator` 承担；可再加更多 size-class。
- **(b) 容器内联变长 + arena**：array/object 子元素内联进变长节点，整树从一个 arena 分配；彻底消除逐节点/逐缓冲 malloc；改动中等。
- **(c) yyjson 式不可变冻结文档（最快）**：解析后把整棵树压进一块连续 arena，返回**文档级** refcount 句柄、节点借用；释放 = O(1) 释放 arena。代价：C API 所有权语义从"每节点可独立持有"变为"retain 保活整个文档"——**方向性变更，需单独确认**。

### 非原子 refcount（已评估，**不采用**）
把 `rc` 从 `std::atomic<int32_t>` 换成普通 `int32_t`（编译开关 `KRJSON_SINGLE_THREADED`）可省未竞争原子 RMW 的屏障开销（估计对象 DOM ~个位数–15%）。**当前不采用**：render 跨线程、且需对齐 `KRRenderValue`（shared_ptr）的线程安全语义；非原子会在跨线程共享时数据竞争 → 泄漏/double-free。仅在未来确有"单线程独占所有权"的明确场景时再作为可选开关引入。
