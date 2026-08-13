# Kuikly安装包优化指引

## Kotlin Native符号内部化

不需要被外部引用的类和对象，增加internal修饰，这样做的收益有两方面：
1. 在iOS环境中，这样做避免避免编译器为这些类生成桥接对象，减少包大小以及内存的使用
2. 在LTO的DCE优化环节中，有助于编译器对无人引用的死代码进行移除

优化措施：
1. 通常可以通过脚本对非internal的类和文件进行统一的修改
2. 腾讯内部项目也可以使用[Kuikly Shrinker插件](https://raftx.woa.com/kuikly/detail/717)自动对项目符号进行可见性调整。


## 编译选项优化

:::tip 注意
**通常多数的业务在鸿蒙上将Kotlin Native产物编译为动态库，而在iOS上则编译为静态framework，所以在iOS上宿主的编译选项也会影响最终链接产物的大小，可结合iOS苹果官方以及业界的安装包优化措施进行整体优化，本指引重点关注Kotlin Native产物大小。**
:::

可以把这类优化分成两档：
1. **无明显性能副作用、建议优先启用的选项**：适合先作为默认优化基线。
2. **更激进的极致压缩选项**：体积收益更高，但更容易带来性能回退，需要专项验证。

### 优先启用的无明显性能副作用选项

以下组合已经在 Kuikly Demo 的 OHOS Release 冷启动场景做过 A/B 实测，可优先作为默认配置：
1. 启用 `--pack-dyn-relocs=relr`
2. 启用 `--gc-sections`
3. 启用 `-ffunction-sections`
4. 启用 `-fdata-sections`
5. 对 C/C++ 动态库额外启用 `--hash-style=gnu`
6. 对 C/C++ 动态库额外启用 `-fvisibility=hidden`、`-fvisibility-inlines-hidden`

其中需要特别注意：
- **Kotlin Native 侧不要照搬 `-fvisibility=hidden`**：该参数对 KN 已生成 IR 的收益很有限，且 KN 动态库通常仍会保留少量必要导出符号。
- **C/C++ 侧启用 `-fvisibility=hidden` 时，必须为公开 API 和 KN 桥接符号补齐导出宏**，否则会出现动态库加载失败或运行时找不到符号的问题。
- **这组参数主要适用于 Release / 非 Debug 构建**，不建议影响日常调试配置。

### 推荐配置示例

#### Kotlin Native 动态库（`libshared.so`）

```kotlin
kotlin {
    ohosArm64 {
        binaries.sharedLib("shared") {
            freeCompilerArgs += "-Xadd-light-debug=enable"
            linkerOpts += "--build-id=sha1"
            if (buildType == org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.RELEASE) {
                val CLANG_OPT_FLAGS = "-O3 -ffunction-sections -fdata-sections"
                val CLANG_FLAGS = "clangOptFlags.ohos_arm64=$CLANG_OPT_FLAGS;clangDebugFlags.ohos_arm64=$CLANG_OPT_FLAGS"
                freeCompilerArgs += "-Xoverride-konan-properties=$CLANG_FLAGS"
                linkerOpts += "--pack-dyn-relocs=relr"
                linkerOpts += "--gc-sections"
                linkerOpts += "--hash-style=gnu"
            }
        }
    }
}
```

这组配置的关键点：
- 保留 KN 默认的 `-O3`，**不使用 `-Os` / `-Oz`**。
- 通过 `-Xoverride-konan-properties` 注入 `-ffunction-sections`、`-fdata-sections`。
- 通过 `linkerOpts` 注入 `relr`、`gc-sections`、`hash-style=gnu`。
- KN 侧如果要继续减符号，优先通过 `internal` 修饰和 Shrinker 处理，而不是依赖 `-fvisibility=hidden`。

#### C/C++ 动态库（如 `libkuikly.so`）

```cmake
target_compile_options(kuikly PRIVATE
        -ffunction-sections
        -fdata-sections
        -fvisibility=hidden
        -fvisibility-inlines-hidden
)

target_link_options(kuikly PRIVATE
        -Wl,--gc-sections
        -Wl,--pack-dyn-relocs=relr
        -Wl,--hash-style=gnu
)
```

如果开启了 `-fvisibility=hidden`，建议统一通过导出宏管理公开接口，例如：

```cpp
#if defined(__GNUC__)
#define KUIKLY_EXPORT __attribute__((visibility("default")))
#else
#define KUIKLY_EXPORT
#endif
```

对外暴露的 C API、给 KN 调用的桥接函数，以及必须被其他动态库消费的符号，都需要显式标记为默认可见。

### 实测收益参考

以下数据来自 Kuikly Demo 在 OHOS 真机 Release 冷启动首页场景的 A/B 对比，A 为清空优化配置，C 为启用上述最终配置：

| 指标 | A 基线 | C 最终 | 变化 |
|------|--------|--------|------|
| `libshared.so`（HAP / stripped） | 48.51 MiB | 38.41 MiB | **-20.8%** |
| `libkuikly.so`（HAP / stripped） | 2.17 MiB | 1.49 MiB | **-31.3%** |
| `libkuikly.so` dynsym 符号数 | 4525 | 1032 | **-77.2%** |
| Total PSS | 109115 kB | 97452 kB | **-10.7%** |
| `.so` PSS | 35524 kB | 24949 kB | **-29.8%** |
| native heap PSS | 32617 kB | 30194 kB | **-7.4%** |

从结果上看，这组参数不仅能显著降低 HAP 中动态库体积，也能带来较明显的 `.so` 映射内存下降，适合作为 OHOS 的默认安装包优化基线。

### 更激进的极致压缩选项

经验证以下选项对于 Kotlin Native 产物的减少也有较明显帮助（如在鸿蒙上 Kuikly Demo 产物大小下降 40%，有的业务下降 50%），但**这些选项更容易对性能产生影响**，使用前请做好专项验证：
1. 使用 `-Os`（`-Oz` 效果更佳，但对性能影响通常更大）
2. 启用 `-mllvm -enable-machine-outliner=always` 提取重复指令

示例：

```kotlin
kotlin {
    targets.all {
        compilations.all {
            kotlinOptions {
                // ... 省略其他选项 ...
                val CLANG_OPT_FLAGS = "-Os -mllvm -enable-machine-outliner=always -ffunction-sections"
                val CLANG_FLAGS = "clangOptFlags.ios_arm64=$CLANG_OPT_FLAGS;clangDebugFlags.ios_arm64=$CLANG_OPT_FLAGS;clangOptFlags.ohos_arm64=$CLANG_OPT_FLAGS;clangDebugFlags.ohos_arm64=$CLANG_OPT_FLAGS"
                freeCompilerArgs += "-Xoverride-konan-properties=$CLANG_FLAGS"
            }
        }
    }
    ohosArm64 {
        binaries.sharedLib("shared") {
            // ... 省略其他选项 ...
            freeCompilerArgs += "-Xadd-light-debug=enable"
            linkerOpts += "--pack-dyn-relocs=relr"
            linkerOpts += "--gc-sections"
        }
    }
}
```

如果业务对启动性能、滚动流畅度或关键链路延迟较为敏感，建议先落地前文的“无明显性能副作用选项”，再按模块逐步评估 `-Os`、`-Oz`、machine outliner 等更激进的优化组合。

