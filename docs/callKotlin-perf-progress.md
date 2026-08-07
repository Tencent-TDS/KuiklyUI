# callKotlin Interop Performance Optimization

> Goal: optimize Native → Kotlin `callKotlin` interop (large JSON / hot path).
> OHOS device: physical `LNG0223C13000049`. iOS: simulator (see iOS section).

## Progress Log

| Step | Status | Notes |
|------|--------|-------|
| 1. Understand code (render-ohos / core-ksp / core) | DONE | |
| 2. Demo page + OHOS measurement | DONE | `CallKotlinPerfPage` + ohosApp `CallKotlinPerfTestModule` |
| 3. Optimization plan + execute | DONE | A/B/C/D tried |
| 4. Re-test early opts; keep what matters | DONE | Keep A, B, D; drop C |
| 5. Phase distribution + long JSON | DONE | |
| 6. Lazy cJSON* JSONObject proxy | DONE | Flat ~50–70µs even at 3MB if unread |
| 7. iOS port (KSP B + lazy NSDictionary D + DEBUG harness) | DONE | See iOS section |

---

## Keep / Drop verdict (device A/B, 2026-08-03)

Re-measured on `LNG0223C13000049` with and without KSP opt B (`logs/kuikly_ab_with_opts.log` vs `logs/kuikly_ab_no_ksp.log`).

| Opt | What | Device evidence | Verdict |
|-----|------|-----------------|---------|
| **A** | Cache `instanceIdValue_` on `KRRenderCore` | Within same run: `layout_full` 27.8µs vs `layout_cachedArg0` 24.9µs (~**10% / ~3µs**) | **KEEP** — small absolute, cheap, always on hot path |
| **B** | KSP OHOS entry: method-specific `toAny`, no lambda, codegen try/catch | LAYOUT `cpp_callKotlin`: **37µs (with B)** vs **76µs (without B)** ≈ **2×** | **KEEP** — matters for high-frequency small calls |
| **C** | Eager structured Map ARRAY (`__kuikly_map_v1__`) | Superseded by D; never called from `toCValue` after D | **DROP** — removed dead C++/Kotlin marker path |
| **D** | Lazy `NATIVE_JSON` (cJSON* + refcount) | 64KB FIRE string **~7.8ms** vs lazy **~60–70µs**; scales flat when unread | **KEEP** — dominant win for Map/Array events |

Notes:
- FIRE string path is dominated by `toKString` + `JSONObject` parse; A/B do not change that.
- Opt B does not materially change large-string FIRE or lazy Map numbers (noise-level).
- Absolute LAYOUT numbers vary by device thermal/load; relative A/B deltas above are from paired runs.

---

## Call path (OHOS)

```
KRRenderCore::CallKotlinMethod
  └─ instanceIdValue_ (cached)                         // A
  └─ argN->toCValue()                                  // Map → NATIVE_JSON (D)
  └─ callKotlin_(...)
       └─ KSP staticCFunction (method-specific toAny)  // B
            └─ BridgeManager → fireViewEvent
                 └─ TypeUtils → CJsonJSONObject (ohos) / String
                      └─ BridgeManager → fireViewEvent (String|JSONObject)
```

---

## Measurement notes (OHOS)

Kotlin-side phase counters in `core` were removed (too intrusive on Bridge/TypeUtils).
Bench timing is C++-only in ohosApp `CallKotlinPerfTestModule`
(`cpp_toCValue_ns_per` / `cpp_callKotlin_ns_per`). For deeper Kotlin breakdowns use
platform profilers (HiTrace / Perfetto), not framework hooks.

### Lazy cJSON* bridge (kept)

Map/Array `toCValue` passes `NATIVE_JSON` (`KuiklyCJsonOwner*` in `longValue`, refcounted).
Kotlin ohosArm64 `CJsonJSONObject.fromOwner` **retain**s; Cleaner **release**s on GC. C++ dtor also **release**s.

| ~bytes | string (legacy) | lazy cJSON (unread) | speedup |
|------:|----------------:|--------------------:|--------:|
| 1K | ~193µs | ~67µs | ~3× |
| 64K | ~7.8ms | ~69µs | ~110× |
| 3MB | ~326ms | ~48µs | ~6800× |

---

## Optimizations kept in tree (OHOS)

### A. Cache instanceId on `KRRenderCore` (`core-render-ohos`)
- Construct `instanceIdValue_` once in ctor; reuse in `CallKotlinMethod`.

### B. KSP OHOS entry hot path (`core-ksp`)
- Remove per-call `callKotlinClosure` lambda allocation.
- Codegen-time try/catch (use `catchException` flag) instead of runtime branch.
- Method-specific `toAny` arity:
  - `LAYOUT_VIEW` → arg0 only
  - `FIRE_VIEW_EVENT` → arg0–arg3
  - `FIRE_CALLBACK` / `UPDATE` / `DESTROY` → arg0–arg2
  - `CREATE_INSTANCE` → reuse `asString()` as instanceId (no double convert)

### D. Lazy cJSON* bridge (OHOS)
- C++ Map/Array `toCValue` → `NATIVE_JSON` (opaque `shared_ptr<KuiklyCJsonOwner>` handle).
- Kotlin **ohosArm64**: `LazyCJsonMap` into internal `JSONObject(map)`; `TypeUtils` wraps owner → `JSONObject`.
- **commonMain**: `JSONObject` stays `final`; Create/Update/Fire accept `String` or `JSONObject`.
- **CreateInstance**: pass `PageData()` Map (no `toString()`).
- **UpdateInstance / SendEvent**: ETS/NAPI pass Record → Map; legacy string API parses to Map before CallKotlin.

### Dropped: C. Eager structured Map bridge
- Removed `ToStructuredMapLocked` / `ToJsonMapOrArrayLocked` and `__kuikly_map_v1__` Kotlin decode.
- Was ~2.5–3.5× vs string JSON but still paid `toKString` on large leaves; D is strictly better for unread/partial-read Maps.

---

## iOS port (2026-08-04)

iOS Native→Kotlin is ObjC `id` → K/N (not `KRRenderCValue` / cJSON).

| Opt | iOS approach | Notes |
|-----|----------------|-------|
| **A** | Skip | `_instanceId` already retained `NSString*` |
| **B** | `IOSTargetEntryBuilder` (+ multi) | No per-call lambda; codegen try/catch from `catchException` |
| **D** | Lazy `NSDictionary` | Pass collections through ObjC; `toKotlinBridgeArg` at KSP entry wraps `NSDictionary` → `LazyNSDictionaryMap` before `BridgeManager` (OHOS parallel: `toAny`) |

### Call path (iOS)

```
KuiklyRenderCore → ContextHandler.callWithMethod
  └─ All methods: pass NSDictionary/NSArray through (no NSJSONSerialization)
  └─ KuiklyCoreEntry.callKotlinMethod
       └─ KSP entry: argN.toKotlinBridgeArg() → BridgeManager
            └─ Create/Update/Fire* → toBridgeJSONObject (String | JSONObject)
```

### DEBUG harness

- `iosApp/.../CallKotlinPerfTestModule.m` (`#if DEBUG`), class name matches Kotlin `MODULE_NAME`
- Times `hr_dictionaryToJSON` (string mode) vs pass-through dict (map mode) + `callWithMethod`
- Nested lifecycle: `testAppleNestedNSDictionaryLifecycle` (local `NSMutableDictionary` tree)
- Open `CallKotlinPerfPage` via demo router; log tag `CallKotlinPerf`

### Bench numbers (iOS simulator, iPhone 15, 2026-08-04)

Unread payloads — `cpp_total_ns_per` from DEBUG `CallKotlinPerfTestModule`
(string = `hr_dictionaryToJSON` + call; lazy = pass-through `NSDictionary`).

**FireViewEvent**

| ~bytes | string | lazy | speedup |
|------:|-------:|-----:|--------:|
| 1K | ~32µs | ~1.7µs | ~19× |
| 64K | ~1.12ms | ~1.3µs | ~860× |
| 3MB | ~55ms | ~1.2µs | ~45k× |

**UpdateInstance** (same `toBridgeJSONObject` path as Create pageData)

| ~bytes | string | lazy | speedup |
|------:|-------:|-----:|--------:|
| 1K | ~25µs | ~0.88µs | ~29× |
| 64K | ~1.12ms | ~0.83µs | ~1350× |
| 3MB | ~54ms | ~0.82µs | ~66k× |

**FireCallback**

| ~bytes | string | lazy | speedup |
|------:|-------:|-----:|--------:|
| 1K | ~4.3µs | ~0.93µs | ~4.6× |
| 64K | ~121µs | ~0.92µs | ~130× |
| 3MB | ~5.9ms | ~0.90µs | ~6500× |

LAYOUT ≈ **0.52µs**/call. Nested lifecycle: `ok:true`.
CreateInstance is not looped (would allocate pagers); it shares Update’s JSON bridge.
Log: `logs/kuikly_console.log` tag `CallKotlinPerf`.

---

## How to re-run

### OHOS

Open demo page `CallKotlinPerfPage` (router / in-app navigation; Want `--ps pageName` is not wired).
Debug OHOS build only — native `CallKotlinPerfTestModule` is `#ifndef NDEBUG` / CMake Debug-gated.

```bash
hdc -t LNG0223C13000049 shell aa force-stop com.tencent.kuiklyohosdemo
hdc -t LNG0223C13000049 shell aa start -a EntryAbility -b com.tencent.kuiklyohosdemo
# Navigate to CallKotlinPerfPage; hilog tag CallKotlinPerf — BASIC / PHASES_CPP / DONE
```

### iOS

Debug iosApp only — `CallKotlinPerfTestModule` is `#if DEBUG`.

```bash
# Build + launch simulator; open CallKotlinPerfPage via demo router
# Capture: os_log / Xcode console tag CallKotlinPerf — NESTED_LIFECYCLE / BASIC / PHASES_CPP / DONE
```

### Follow-ups (optional)

- Prefer Map/Array at Native call sites instead of pre-serializing JSON strings.
- Cache Kotlin-side pageId string to skip `toKString` on LAYOUT_VIEW (OHOS).
- Measure try/catch-off build to shrink LAYOUT FFI residual.
