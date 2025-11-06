# 鸿蒙平台开发方式

## so模式
:::tip 注意
1. 鸿蒙平台跨端产物的需要使用鸿蒙SDK的LLVM工具编译生成，官方版的Kotlin工具链并不支持鸿蒙平台，因此我们需要使用定制版的Kotlin工具链。
2. 鸿蒙Kotlin工具链当前两个可用版本：`2.0.21-KBA-004`和`2.0.21-KBA-010`。 `2.0.21-KBA-010` 版本已支持Windows/Linux平台编译，`Kuikly` 鸿蒙跨端产物使用Windows平台编译，请参考 [Windows平台编译配置](#windows平台编译配置) 步骤配置。
:::

### 配置kuikly Ohos编译环境
#### 方式1: Ohos单独配置编译链(推荐)

给ohos单独配置gradle配置项，使用此配置项编译鸿蒙产物

在[编写第一个Kuikly页面](../QuickStart/hello-world.md)中，Android Studio中创建的Kuikly工程中会默认带了鸿蒙产物的gradle设置项，可以参考使用。
(若创建的工程无鸿蒙模版可以升级Andriod Studio Kuikly plugin)
<div>
<img src="./img/ohosgradle.png"  alt="intro_5" width="400">
</div>
在编译产物的时候可以指定此gradle设置，进行鸿蒙产物的编译

`./gradlew -c settings.ohos.gradle.kts :shared:linkOhosArm64`



#### 方式2: 统一编译链形式

由于方式1中ohos工具链并非默认设置，ohosArm64Main中的代码会缺乏相应编译器提示。

可以在工程使用此ohos工具链作为默认设置，按照以下的方式配置


- 在你现有工程中添加maven源：
```gradle
repositories {
    // 定制版kotlin工具链
    maven {
        url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
    }
}
```

- 修改版本号

把工程的Kotlin版本改为**2.0.21-KBA-010**，Kuikly版本改为**KUIKLY_VERSION-2.0.21-ohos**，ksp版本改为**2.0.21-1.0.27**。改完后，执行 ./gradlew bE 和 ./gradlew :shared:d 命令确认依赖项的版本resolve正确。

:::tip 注意
1. 目前鸿蒙编译链有可用版本：`2.0.21-KBA-004`和`2.0.21-KBA-010`。其中 `2.0.21-KBA-010` 已支持Window/Linux平台编译。
2. Kuikly DSL相关SDK -ohos后缀版本支持Android/iOS/Ohos三端。
   Compose DSL 相关SDK -ohos后缀版本当前仅支持ohos，待后续发布跨多端版本。统一编译链使用时，所依赖的Compose SDK，需根据当前编译target不同，判断选择带-ohos后缀版本(ohos target)和不带-ohos后缀版本(Android/iOS target)。
:::

以插件创建的模板工程为例，需要修改的有：

**build.gradle.kts**

```gradle
plugins {
    kotlin("android").version("2.0.21-KBA-010").apply(false)
    kotlin("multiplatform").version("2.0.21-KBA-010").apply(false)
    ...
}
```

**shared/build.gradle.kts**

```gradle
kotlin{
    ...
    sourceSets {
        val commonMain by getting {
            dependencies {
                // KUIKLY_VERSION 用实际使用的Kuikly版本号替换
                implementation("com.tencent.kuikly-open:core:KUIKLY_VERSION-2.0.21-ohos")
                implementation("com.tencent.kuikly-open:core-annotations:KUIKLY_VERSION-2.0.21-ohos")
            }
        }
        ...
    }
}
...
dependencies {
    // KUIKLY_VERSION 用实际使用的Kuikly版本号替换
    compileOnly("com.tencent.kuikly-open:core-ksp:KUIKLY_VERSION-2.0.21-ohos") {
        ...
    }
}
```

- 添加构建目标

在shared/build.gradle中添加ohosArm64构建目标：
```gradle
kotlin {
    ....
    ohosArm64 {
        binaries {
            sharedLib()
        }
    }
}
dependencies {
    compileOnly("com.tencent.kuikly-open:core-ksp:KUIKLY_VERSION-2.0.21-ohos") {
        ....
        add("kspOhosArm64", this)
    }
}
```

### Windows平台编译配置
#### 环境配置
在Windows平台配置，需指定 OHOS SDK 位置，配置环境变量，比如：

变量名: `OHOS_SDK_HOME`

路径: `D:\Program Files\DevEco Studio\sdk\default\openharmony`

#### 模板工程 - Windows编译步骤
`Kuikly` Android Studio Plugin: `KuiklyTemplate`，1.2.0及之前版本生成的模板工程，需做如下配置：

1、将Kotlin编译插件，修改为支持Window平台编译的版本：`2.0.21-KBA-010`。在build.ohos.gradle.kts脚本做如下修改：

```gradle
plugins {
    ....
    kotlin("android").version("2.0.21-KBA-010").apply(false)
    kotlin("multiplatform").version("2.0.21-KBA-010").apply(false)
    ....
}
```

2、编译鸿蒙跨端产物，及Ohos App

由于Window Android Studio上暂未适配sh脚本运行，不能直接通过 `runOhosApp.sh` 联动编译 Ohos APP。Ohos APP 编译需要分两步：

**Step1**: 在Android Studio，通过命令行，编译鸿蒙跨端产物。并将编译后的跨端产物拷贝到Ohos APP对应目录。这一步可以通过gradle脚本自动拷贝，业务自行实现。

产物编译命令：

`./gradlew -c settings.ohos.gradle.kts :shared:linkOhosArm64`

仅编译Debug产物：

`./gradlew -c settings.ohos.gradle.kts :shared:linkDebugSharedOhosArm64`

仅编译Release产物：

`./gradlew -c settings.ohos.gradle.kts :shared:linkReleaseSharedOhosArm64`

拷贝编译产物到Ohos APP对应目录：
<div>
<img src="./img/ohos_app_copy_shared_artifact.png" width="25%">
</div>

**Step2** : 在DevEco 打开Ohos APP，编译鸿蒙APP

#### 业务存量工程 - Windows编译步骤
业务现有工程已经接入了 `Kuikly` 并开发鸿蒙APP，同样参考上述模板工程进行如下配置：

**Step1**: 将Kotlin编译工具链，修改为支持Window平台编译的版本：`2.0.21-KBA-010`

**Step2**: 通过编译命令，编译跨端产物，拷贝到Ohos APP对应目录，启动DevEco，编译鸿蒙APP

#### 源码工程 - Windows编译步骤
KuiklyUI 源码工程已将Kotlin编译插件版本升级为`2.0.21-KBA-010`版本，开发者可以直接通过命令行编译跨端产物，运行鸿蒙APP：

**Step1**: 编译跨端产物，拷贝到Ohos APP对应目录

编译命令

`./gradlew -c .\settings.2.0.ohos.gradle.kts :demo:linkSharedOhosArm64`

仅编译Debug产物：

`./gradlew -c settings.2.0.ohos.gradle.kts :demo:linkSharedDebugSharedOhosArm64`

仅编译Release产物：

`./gradlew -c settings.2.0.ohos.gradle.kts :demo:linkSharedReleaseSharedOhosArm64`

**Step2**: 启动DevEco，编译鸿蒙APP


### 编写Kuikly页面

参照[编写第一个Kuikly页面](../QuickStart/hello-world.md)的指引编写页面。在页面中可以通过以下方式判断鸿蒙平台：
```kotlin
internal class RouterPage : BasePager() {
  override fun body(): ViewBuilder {
    val isOhos = pagerData.platform === "ohos"
    ...
  }
}
```

### 生成so产物和头文件

1. 如果采用上述方式1配置：

命令行执行 `./gradlew -c settings.ohos.gradle.kts :shared:linkOhosArm64` 编译产物

2. 如果采用上述方式2配置：

执行shared module的**linkOhosArm64**任务（或者命令行执行 ./gradlew :shared:linkOhosArm64）

<div>
<img src="./img/ohos_gradle.png" width="50%">
</div>

构建成功后，so产物和头文件在shared/build/bin/ohosArm64/

<div>
<img src="./img/so_dir.png" width="50%">
</div>

### 同步so产物和头文件至鸿蒙宿主工程

#### 方式1：Kuikly Hvigor插件

Kuikly简单封装了一个鸿蒙hvigor插件
插件可以实现在鸿蒙工程运行的时候编译kuiklyOhos产物并拷贝至对应文件夹，实现编译联动

使用方式:
1. ohosProject -> .npmrc
```text
registry=https://registry.npmjs.org/
```

2. ohosProject -> hvigor/hvigor-config.json5
```text
  ...
  "dependencies": {
    ...
    "kuikly-ohos-compile-plugin": "latest"
    ...
  },
  ...
```
3. ohosProject根目录中local.properties配置相应信息

```
# kuiklyCompilePlugin
# REQUIRED Parameters
kuikly.projectPath=Your kuikly project root path
kuikly.moduleName=Your kuikly module name
kuikly.ohosGradleSettings=settings.ohos.gradle

# OPTIONAL Parameters
kuikly.soPath=Your so product path(Relative path to the Ohos project root directory, the default is entry/libs/arm64-v8a)
kuikly.headerPath=Your header product path(Relative path to the Ohos project root directory, the default is entry/src/main/cpp)
kuikly.compilePluginEnabled=Whether the kuikly compile plugin is enabled during build(true or false, the default is true)
```
:::tip 注意
插件会在 `projectPath` 中执行 `./gradlew -c ohosGradleSettings moduleName:linkOhosArm64`

并把编译的产物拷贝到 `kuikly.soPath`、`kuikly.headerPath`

若你的工程结构比较复杂，插件可能无法支持
:::

4. ohosProject -> entry/hvigorfile.ts 启用插件
```text
import { kuiklyCompilePlugin } from 'kuikly-ohos-compile-plugin';
export default {
...

    plugins:[kuiklyCompilePlugin()]         /* Custom plugin to extend the functionality of Hvigor. */

    ...
}
```

在此基础上，如果想要在`Android Studio`运行鸿蒙App，可以参考模版工程添加

`模版工程根目录/.run/ohosApp.run.xml`和`模版工程根目录/ohosApp/runOhosApp.sh`

#### 方式2：自行注册Gradle Task拷贝编译产物
若你的工程结构较为复杂，可以自定义相关Gradle Task实现编译联动的功能