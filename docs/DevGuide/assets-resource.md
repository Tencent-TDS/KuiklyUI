# Assets 资源与图片
自 1.1 版本起，Kuikly 支持业务使用 assets 资源。默认模式下（aar模式和framework模式），assets 资源将被打包到对应的编译产物中。动态化模式下，assets 资源将会被打包进成动态化产物中。

## 添加 Assets 资源

assets 资源存放在业务的 Kuikly 模块（shared）下的 `commonMain/assets` 目录（如没有该目录，请新建一个）：

```shell
├── androidApp
├── iosApp
├── shared
  ├── build.gradle.kts
  ├── shared.podspec
  └── src
      └── commonMain
          ├── assets 	# 存放 assets 图片
          └── kotlin
```

由于动态化场景产物可根据页面进行分页，业务在添加 assets 资源时需要区分该资源 assets 是公共资源还是页面资源。公共资源存放在`common`目录，页面资源放在`$pageName`目录：

```shell
├── shared
  ├── build.gradle.kts
  ├── shared.podspec
  └── src
      └── commonMain
          ├── assets 		# 存放 assets 图片
            ├── common
         			└── pic1.png   	
            ├── page1		# 存放 page1 页面的图片
            	└── pic2.png   	
            └── page2   # 存放 page2 页面的图片      
            	└── pic3.png   	
```

## 打包 Assets 资源

### 1. 内置打包

业务首次使用`assets`资源时，需要修改下编译配置：

#### Android

修改`shared/build.gradle.kts`

```kotlin
...
android {
	...
	sourceSets {
		named("main") {
			...
			// 添加 android assets 资源路径
			assets.srcDirs("src/commonMain/assets")
		}
	}
}
...
```

#### iOS

修改`shared/shared.podspec`

```shell
...
	// 添加 ios assets 资源路径
	spec.resources = ['src/commonMain/assets/**']
...
```

#### 鸿蒙
鸿蒙会将业务代码编译为so文件，不支持`assets`资源内置打包，需要将资源拷贝到鸿蒙工程的`resfile`目录中，例如：
```
shared/src/commonMain/assets/common/* -> entry/src/main/resources/resfile/common/*
```

### 2. 动态化打包

动态化资源打包不需要增加配置，assets 资源会被一起打进产物包，如下：

```shell
# image_demo js 产物解压
├── assets
│   ├── common
│   │   └── penguin2.png
│   └── image_demo
│       ├── 1
│       │   └── penguin1.png
│       └── panda.png
├── config.json
└── image_demo.js
```

image_demo 页面打包时，`common`和`image_demo`目录下的资源将会一并进行打包

## 加载 Assets 图片

### 1. 图片适配器添加 Assets 图片加载

为保持 Kuikly 框架的轻量，框架不内置图片加载器，因此业务需要在图片适配器中添加 `assets` 图片加载支持，如有使用动态化场景，则还需添加`file`图片加载支持

（1）Android

```kotlin
class KRImageAdapter(val context: Context) : IKRImageAdapter {
  override fun fetchDrawable(
        imageLoadOption: HRImageLoadOption,
        callback: (drawable: Drawable?) -> Unit,
    ) {
        if (imageLoadOption.isBase64()) {
       			...
        } else if (imageLoadOption.isWebUrl()) {
					// 实现加载 Assets 资源逻辑
        } else if (imageLoadOption.isAssets()) {	
          // 实现加载 File 资源逻辑，动态化场景使用
        } else if (imageLoadOption.isFile()) {		
          	...
        }
    		...
    }
}
```

> 可参考：[KRImageAdapter.kt](https://github.com/Tencent-TDS/KuiklyUI/blob/main/androidApp/src/main/java/com/tencent/kuikly/android/demo/adapter/KRImageAdapter.kt)

（2）iOS

* framework产物模式，默认会从mainBundle加载图片，无需适配
* 动态化产物模式，需要传递图片资源的本地路径给到sdk，需要实现KuiklyRenderViewControllerDelegatorDelegate的该接口

```oc
@protocol KuiklyRenderViewControllerDelegatorDelegate

/*
 * @breif assetsPathUrl assert资源在动态化产物模式下，由于是后下载，故通过该路径来传递assert资源的路径。注意该目录下，应该有common目录，和对应的pagename目录
 */
- (NSURL *)assetsPathUrl;
```

### 2. 使用 Assets 图片

使用公共 assets 图片

```kotlin
Image {
  attr {
  	// 使用 common 目录下的相对路径
	  src(ImageUri.commonAssets("penguin2.png"))
  }  
}
```

使用页面 assets 图片

```kotlin
Image {
  attr {
  	// 使用 common 目录下的相对路径
  	src(ImageUri.pageAssets("panda.png"))
  }  
}
```

> 可参考：[ImageDemoPage.kt](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/ImageDemoPage.kt)
