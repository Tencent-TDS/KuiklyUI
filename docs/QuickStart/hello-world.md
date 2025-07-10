# 编写第一个Kuikly页面

:::tip 注意
在此之前请确保已经完成**Kuikly**环境搭建，如还未搭建环境，请移步[环境搭建文档](env-setup.md)
:::

## 新建Kuikly工程

1. 使用``Android Studio``新建``Kuikly``工程。 **File -> New -> New Project -> Kuikly Project Template**

![新建Kuikly工程](./img/new_kuikly_project.png)

## 工程自动构建
若环境搭建无误，工程将自动构建，如遇以下问题，可根据解决方案进行处理并手动触发sync：

### Gradle安装依赖失败
![](./img/gradle_version_error.png)
如果遇到Gradle版本不匹配情况，请将Gradle版本切换成7.x(如7.5.1)。  **File -> Project Structure -> Project -> Gradle Version**

如果Gradle版本低于7.4.1会出现报错，需要在根目录 setting.gradle.kts 添加 `enableFeaturePreview("VERSION_CATALOGS")`

### 找不到pod程序
![](./img/no_pod_error.png)
参照[环境搭建文档](env-setup.md)，确保iOS运行环境安装无误或者已注释相关构建逻辑([忽略iOS编译打包](env-setup.md#忽略ios编译打包))

### 没有启动配置
![](./img/no_configuration.png)
工程构建完以后没有提供预设的启动配置，一般是由于之前出现异常导致的，解决异常后重新sync即可

## 运行androidApp

1. 接着运行``androidApp``

![运行AndroidApp](./img/android_app_run.png)

2. 当手机出现**以下页面**时，说明已经成功运行``androidApp``

<div align="center">
<img src="./img/kuikly_android_app_demo.jpg" width="30%"  alt="运行成功" style="border: 1px solid black;">
</div>

## 运行iosApp

1. 如果是首次运行iOS的话，需要先进入iosApp目录，执行`pod install --repo-update`。
2. 将运行的scheme改为iosApp

![](./img/run_config_step.png)
![](./img/iosApp_scheme.png)

3. 运行``iosApp``

![运行iosApp](./img/ios_app_run.png)

4. 当手机出现下面界面时，说明已运行成功

<div align="center">
<img src="../DevGuide/img/kuikly_ios_app_demo.png" width="30%"  alt="运行成功" style="border: 1px solid black;">
</div>

:::tip 注意
插件自动创建的工程iosApp在编译时会执行KMP脚本，如果遇到脚本读写文件权限报错，需要在``Xcode -> Build Setting``中将``User Script Sandboxing``设置为``No``
:::

## 运行鸿蒙App
:::tip 注意
1. 当前kuikly鸿蒙跨端产物仅支持Mac编译，Windows可以使用编译好的跨端产物运行Ohos宿主APP 
2. 若想通过插件生成Ohos工程，需要更新插件至1.1.0版本以上

:::
1. 如果是首次运行Ohos的话，需要用鸿蒙DevEco-Studio打开ohosApp目录
    >若初次打开鸿蒙IDEsync出错，打开ohosApp目录下的.npmrc文件，右上角点击sync重新sync即可

2. 在运行之前App之前，需要执行签名操作
    File -> Project Structure -> Signing Configs
    <div align="center">
        <img src="./img/ohos_ide_sign.png" style="width: 100%;">
    </div>
3. 运行``ohosApp``

    可以选择在 DevEco-Studio 内运行 或 Android Studio 内运行

   ![DevEco运行OhosApp](./img/ohos_run1.png)

   ![AndroidStudio运行OhosApp](./img/ohos_run2.png)
    > 若 Android Studio 无ohosApp选项 重新打开Android Studio即可
4. 当手机出现下面界面时，说明已运行成功ohosApp
    <div align="center">
        <img src="./img/ohos_run_success.jpg" width="30%"  alt="运行成功" style="border: 1px solid black;">
    </div>

## 开始编写代码

我们来看如何使用``Kuikly``编写经典``HelloWorld``

1. 在**share/src/commonMain/kotlin/xxx.xxx.xxx/** 目录下新建 **pages** 目录并创建``HelloWorldPage``类
2. 接着我们让``HelloWorldPage``继承``Pager``, 并重写``body``方法

```kotlin
internal class HelloWorldPage : Pager() {

    override fun body(): ViewBuilder {
    }
}
```

3. 在``body``方法中，我们添加一个居中的Text组件

```kotlin
internal class HelloWorldPage : Pager() {

    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Text {
                attr {
                    text("Hello Kuikly")
                    fontSize(14f)
                }
            }
        }
    }
}
```

4. 接着我们在``HelloWorldPage``类加上``@Page``注解，指定``Page``的名字，供``Kuikly``在运行时创建该``Page``

```kotlin
@Page("HelloWorld")
internal class HelloWorldPage : Pager() {
    ...
}
```

5. 完整的代码如下:

```kotlin
@Page("HelloWorld")
internal class HelloWorldPage : Pager() {

    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }

            Text {
                attr {
                    text("Hello Kuikly")
                    fontSize(14f)
                }
            }
        }
    }
}
```

6. 我们以Android平台作为效果演示，运行androidApp, 在**Kuikly路由页面**输入我们加在``HelloWorldPage``上的注解名字**HelloWorld**，最后点击跳转

<div align="center">
<img src="./img/hello_world_jump.jpg" width="30%" style="border: 1px solid black">
</div>

7. 最后的运行效果:

<div align="center">
<img src="./img/hello_world.png" style="width: 30%; border: 1px gray solid">
</div>

## 更多示例
``Kuikly``源码工程，根目录下demo工程模块，提供了丰富的可体验Demo供日常体验和开发参考。
demo工程模块需要编译源码方可进行体验。 可参考Readme 源码编译构建步骤，编译源码中提供的各平台APP Demo进行体验。
### APP原型Demo
<div align="center">
<img src="./img/app_prototype_demo.png" width="30%" style="border: 1px solid black">
</div>

### 组件Demo
同时，demo工程模块，也提供了框架各个组件的样式和属性的使用Demo。
<div align="center">
<img src="./img/hello_world_example_demo.png" width="30%" style="border: 1px solid black">
</div>

## 下一步

* 如果你的应用还没接入Kuikly的话，建议先移步[Kuikly接入](./overview.md)，查看如何将``Kuikly``接入您的应用。
* 如果您的应用已经接入Kuikly，那么建议您继续浏览Kuikly教程，从理解[Kuikly中的Pager概念](../DevGuide/pager.md)开始