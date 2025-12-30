# KMP组件鸿蒙适配指引
在KuiklyBase基建文档中，提供了[KMP模块鸿蒙Kotlin/Native适配](https://kuikly.tds.qq.com/DevGuide/kuiklybase-ohos-kn.html)的核心思路。但在具体操作上，缺少相关的案例指引。
<br>[KuiklyMMKV](https://github.com/walkman707/KuiklyMMKV)是社区开发者在携程开源的[mmkv-kotlin](https://github.com/ctripcorp/mmkv-kotlin)组件基础上，增加了鸿蒙平台的适配。本文档将在上述KMP模块鸿蒙Kotlin/Native适配指引基础上，
以此KMP组件作为适配案例，进一步介绍KMP组件的适配流程，以供参考。
按照Kuikly文档介绍，KMP组件鸿蒙适配主要分为三个步骤：
* 使用定制化Kotlin版本
* 配置鸿蒙平台Target
* 开发KMP鸿蒙平台代码(ohosArm64Main实现)

下面我们按照这三个步骤，介绍KMP组件的适配过程。

## 适配前的准备
在适配之前，我们先创建一个Kuikly模板工程，以更方便的进行Demo测试。如果其他KMP工程，不需要在Kuikly模板工程中调试，则可忽略当前步骤。
<br>使用Android Studio新建 Kuikly 模板工程。 File -> New -> New Project -> Kuikly Project Template
![新建Kuikly工程](../QuickStart/img/new_kuikly_project.png)

创建好模板工程后，可以将[mmkv-kotlin](https://github.com/ctripcorp/mmkv-kotlin)组件的mmkv-kotlin模块拷贝到模板工程，并改名为mmkvKotlin。具体拷贝过程，及拷贝后修改gradle模块配置，这里略过。

## KotlinMMKV模块编译工具链选择
参考鸿蒙平台开发方式文档，模块的编译工具链配置有两种方式：
* Ohos单独配置编译链方式：Android、iOS采用build.gradle.kts进行构建配置，Ohos采用build.ohos.gradle.kts进行构建配置。
* 统一编译工具链方案：Android、iOS、Ohos构建均使用一个build.gradle.kts进行构建配置。
Ohos单独配置编译链方式，主要是为了解决多端对Kotlin版本要求不一致问题，比如业务由于特殊原因，安卓要求使用较低的Kotlin版本，但适配了Ohos的定制Kotlin不支持低版本，这时只能采用Ohos单独编译链方式。这个方式存在的问题是由于ohos工具链并非默认设置，ohosArm64Main中的代码会缺乏相应编译器提示，同时多个构建脚本也增加了脚本复杂度。

因此，业务可根据需要选择不同的编译方式。本次KotlinMMKV模块适配，采用统一编译链方式来介绍。

注意：如果业务希望单独编译链方式适配，只需要：
* 在mmkvKotlin模块下增加build.ohos.gradle.kts，这个脚本只有ohosArm64构建目标
* 在根目录settings.ohos.gradle.kts，指定mmkvKotlin模块的构建脚本为：build.ohos.gradle.kts
```shell
val buildFileName = "build.ohos.gradle.kts"
rootProject.buildFileName = buildFileName
project(":mmkvKotlin").buildFileName = buildFileName

```

## 使用定制化Kotlin版本
适配鸿蒙，必须使用KuiklyBase定制的Kotlin版本。需要将Kotlin插件源修改为该定制版本。比如在项目根目录的build.gradle.kts、build.ohos.gradle.kts添加如下软件源。

```shell
    kotlin("android").version("2.0.21-KBA-010").apply(false)
    kotlin("multiplatform").version("2.0.21-KBA-010").apply(false)
```

## 配置鸿蒙平台Target
在mmkvKotlin KMP模块的build.gradle.kts中加入鸿蒙平台ohosArm64编译目标。
```shell
    ohosArm64 {
    
     }

```

## ohosArm64Main实现
mmkvKotlin 模块鸿蒙适配是基于cinterop，使用Kotlin语法与MMKV Native So，即libmmkv.so进行交互。
### mmkv C接口封装
在ohosApp下创建mmkv_c_wrapper模块：
* 首先在该模块添加对@tencent/mmkv库依赖。
* 其次，创建 mmkv_c_wrapper.h，并定义和实现mmkv库的C接口。
具体参考/ohosApp/mmkv_c_wrapper模块实现

### 配置 .def 文件
在mmkvKotlin模块的 src 目录新建一个 nativeInterop/cinterop 目录，该目录是默认配置目录。目录下新建interop.def，并配置.def文件。
```shell
package = com.tencent.mmkv
headers = mmkv_c_wrapper.h
staticLibraries = libmmkv_c_wrapper.so
```
### 配置 build.gradle.kts 文件
配置mmkvKotlin模块 Gradle 构建文件，以在ohosArm64构建过程中包含 cinterop相关文件。在cinterop中配置.def文件位置、includeDir位置、以及so Path。
```shell
    ohosArm64 {
        val main by compilations.getting

//        val devopsFlag = providers.environmentVariable("PIPELINE_ID").orNull != null
        val devopsFlag = true
        // mmkvKotlin模块ohosArm64Main依赖的mmkv so头文件路径和so路径
        val includeDir = "${rootProject.rootDir}/ohosApp/mmkv_c_wrapper/src/main/cpp/include/"
        val soLibDir = if(devopsFlag) {
            "${rootProject.rootDir}/shared/src/libs/arm64-v8a"
        } else {
            "${rootProject.rootDir}/ohosApp/mmkv_c_wrapper/build/default/intermediates/libs/default/arm64-v8a"
        }
        // 需要在mmkvKotlin里调来自外部C的时候加这个
        val interop by main.cinterops.creating {
            definitionFile = file("src/nativeInterop/cinterop/interop.def")
            includeDirs(includeDir)
            extraOpts("-libraryPath", soLibDir)
        }
        compilations.forEach {
            it.kotlinOptions.freeCompilerArgs += when {
                HostManager.hostIsMac -> listOf("-linker-options", "-lmmkv_c_wrapper -L${soLibDir}")
                else -> throw RuntimeException("暂不支持")
            }
            // 抑制 NativeApi 提示
            it.compilerOptions.options.optIn.addAll(
                "kotlinx.cinterop.ExperimentalForeignApi",
                "kotlin.experimental.ExperimentalNativeApi",
            )
        }
    }
```
### 实现ohosArm64Main
在mmkvKotlin CommonMain的Creator.kt、Util.kt等类，定义了expect 预期声明，则各平台Main需要使用actual 声明各平台的实际实现。
<br>在ohosArm64Main/MMKVImpl.kt类中，通过调用cinterop暴露的Kotlin语法的mmkv C接口，创建mmkv实例。
```shell
    // MMKVImpl.kt
    private val mmkvHandle: Long by lazy {
        mmapId?.let {
            mmkv_c_mmkv_with_id(it, mode.rawValue, cryptKey, rootPath, 0)
        } ?: mmkv_c_default_mmkv(
            mode.rawValue,
            cryptKey
        )
    }

```

## 测试验证
在适配好mmkvKotlin后，即可对接到模版工程shared模块进行验证。

### 添加对mmkvKotlin依赖
```shell
// shared：build.ohos.gradle.kts
        val commonMain by getting {
            dependencies {
                implementation("com.tencent.kuikly-open:core:${Version.getKuiklyOhosVersion()}")
                implementation("com.tencent.kuikly-open:core-annotations:${Version.getKuiklyOhosVersion()}")
                implementation(projects.mmkvKotlin)  // 源码编译
            }
        }

```

### shared模块链接mmkv_c_wrapper.so
在shared模块配置对mmkv_c_wrapper so的链接，不配置会出现找不到符号问题。
```shell
// shared：build.ohos.gradle.kts
    ohosArm64 {
        ...
        val main by compilations.getting
        compilations.forEach {
            it.kotlinOptions.freeCompilerArgs += when {
                HostManager.hostIsMac -> listOf("-linker-options", "-lmmkv_c_wrapper -L${projectDir}/src/libs/arm64-v8a/")
                else -> throw RuntimeException("暂不支持")
            }
        }
        ...
    }

```
### 编写测试页面
参考源码，在shared模块定义HelloWorldPage，并编写测试页面

### 在ohosApp初始化mmkv库
```shell
//  AbilityState.ets
  onCreate(): void {
    super.onCreate()
    // Napi.initKuikly();

    //直接调用鸿蒙版 MMKV 初始化，和 kmp 共用同一个
    MMKV.initialize(this.context.getApplicationContext())

    MMKV.defaultMMKV().encodeString("test", "abcdef")
    hilog.info(0x0000, 'mmkv', MMKV.defaultMMKV().decodeString("test"))
  }

```

## 组件发布
参考mmkvKotlin README.md说明
