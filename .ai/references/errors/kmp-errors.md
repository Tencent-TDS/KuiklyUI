# KMP 通用错误

> 以下场景读取本文件：新增 `expect/actual` 后编译报错、某个平台缺少 actual 实现、iOS sourceSet 名称写错（`iosMain` vs `appleMain`）。

---

### 1. expect/actual 平台覆盖不全

新增 `expect` 声明后，必须为全部 5 个平台提供 `actual`，否则编译报错：

| 平台 | 正确 sourceSet | 常见错误 |
|------|--------------|---------|
| Android | `androidMain` | — |
| iOS / macOS | `appleMain` | ❌ 写成 `iosMain` |
| Web / 小程序 | `jsMain` | — |
| 纯 JVM | `jvmMain` | — |
| HarmonyOS | `ohosArm64Main` | — |

```kotlin
// commonMain
expect fun currentTimeMs(): Long

// androidMain ✅
actual fun currentTimeMs(): Long = System.currentTimeMillis()

// appleMain ✅（不是 iosMain）
actual fun currentTimeMs(): Long =
    (NSDate.date().timeIntervalSince1970() * 1000).toLong()
```
