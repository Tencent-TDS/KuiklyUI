# Kuikly安装包优化指引

## Kotlin Native符号内部化

不需要被外部引用的类和对象，增加internal修饰，这样做的收益有两方面：
1. 在iOS环境中，这样做避免避免编译器为这些类生成桥接对象，减少包大小以及内存的使用
2. 在LTO的DCE优化环节中，有助于编译器对无人引用的死代码进行移除

优化措施：
1. 通常可以通过脚本对非internal的类和文件进行统一的修改
2. 腾讯内部项目也可以使用[Kuikly Shrinker插件](https://raftx.woa.com/kuikly/detail/717)自动对项目符号进行可见性调整。

    :::tip 注意
    **Kuikly Shrinker插件当前仅仅支持iOS平台，鸿蒙平台支持中。**
    :::

## 编译选项优化

:::tip 注意
**通常多数的业务在鸿蒙上将Kotlin Native产物编译为动态库，而在iOS上则编译为静态framework，所以在iOS上宿主的编译选项也会影响最终链接产物的大小，可结合iOS苹果官方以及业界的安装包优化措施进行整体优化，本指引重点关注Kotlin Native产物大小。**
:::

经验证以下选项对于Kotlin Native产物的减少有较为明显的帮助（如在鸿蒙上Kuikly Demo产物大小下降40%，有的业务下降50%），部分选项通常对性能有所影响，程度大小因业务而异，使用前请做好验证。
1. 启用--pack-dyn-relocs=relr
2. 启用gc-sections，function-sections，data-sections
3. 使用Os选项（Oz效果更佳，但对性能影响偏大一些）
4. 启用-mllvm -enable-machine-outiner=always 提取重复指令，这个对性能影响偏大一些，使用时要多加关注
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
        binaries.sharedLib("shared"){
            // ... 省略其他选项 ...
            freeCompilerArgs += "-Xadd-light-debug=enable"
            linkerOpts += "--pack-dyn-relocs=relr"
            linkerOpts += "--gc-sections"
        }
    }
}
```

