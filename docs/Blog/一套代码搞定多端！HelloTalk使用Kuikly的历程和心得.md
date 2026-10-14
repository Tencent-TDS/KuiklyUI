# 一套代码搞定多端！HelloTalk使用Kuikly的历程和心得

作者：Kuikly

发布时间：2025-10-31

原文链接：https://mp.weixin.qq.com/s/TIpqVoa0ILH4a_V7ir384A

本文原作者：Wind

[原文](https://mp.weixin.qq.com/s?__biz=MzYzOTA4Mzg3OQ==&mid=2247483666&idx=1&sn=0f31edd74ce793275ea267f7930fd9a8&scene=21#wechat_redirect)发布于公众号“Hellotalk技术”

自开源以来，Kuikly跨平台框架迅速获得了开发者社区的青睐，众多团队已成功落地项目并积累了宝贵经验。本期分享的HelloTalk实践文章，从技术选型、基建落地、原生组件扩展，到多语言、模块化架构乃至AI提效，系统性地呈现了Kuikly在复杂业务中的全链路实践。无论你的团队正在评估还是已深度使用Kuikly，这篇文章都将提供有益参考。

### 为什么选择Kuikly

年初的时候，我们一直在思考怎么提升开发效率。相同的页面要开发两次，相同的逻辑要实现两遍，这不仅消耗人力，还容易导致两端表现不一致。作为iOS和Android双端并重的产品，我们希望通过跨平台方案来减少重复工作，统一页面和业务逻辑。

在调研了几个方案后，我们选择了Kuikly，主要看重这几点：

## 技术架构的契合度

Kuikly基于**Kotlin MultiPlatform（KMP）**构建，这和我们的技术栈比较契合。团队对Kotlin已经有一定的了解，上手不会太难。而且KMP的逻辑跨平台能力可以让我们在保持原生性能的同时实现代码复用。

## 原生UI渲染策略

Kuikly不像Flutter或React Native那样自己绘制UI，而是**复用平台的原生UI组件**。这样带来的好处是：

1. 页面的表现和交互完全遵循平台特性

2. 原生组件的性能优势能够保留

3. 可以无缝集成现有的原生组件和第三方库

4. 包体积增量较小，对App体积影响可控

### Kuikly带来的实际收益

经过一段时间的实践，我们确实体会到了Kuikly带来的好处：

**开发效率的提升一份代码，多端运行**是最直观的收益。对于HelloTalk这样iOS和Android双端并重的产品来说：

- **消除重复工作**：相同的页面和业务逻辑只需要用Kotlin写一次

- **保证双端一致性**：统一的代码天然保证了两端表现一致，避免了需求理解偏差

- **降低沟通成本**：不再需要频繁地跨端联调和对齐

- **加快迭代速度**：需求变更时只需改一处代码

## 轻量且高性能

Kuikly基于KMP和原生组件，不需要内置庞大的渲染引擎：

- **轻量**：包体积增量可控

- **高性能**：直接使用系统原生UI组件渲染，性能表现和纯原生基本一致

- **可动态化**：支持动态下发页面配置，具备热更新能力

## 完整的跨端能力

Kuikly不仅仅是UI跨平台，还提供了完善的基建能力，真正实现了页面和业务逻辑的统一：

- **UI跨端**：页面布局和交互逻辑统一编写

- **逻辑跨端**：业务逻辑可以在CommonMain中编写，彻底消除重复实现

## 高度的可扩展性

这是我们感受最深的一点。Kuikly不是一个封闭的黑盒，而是一个开放的框架：

- **组件可扩展**：可以轻松扩展自定义的跨端组件

- **原生能力可桥接**：通过DeclarativeBaseView等机制，可以方便地将原生能力封装为Kuikly组件

- **灵活的模块化**：支持多模块架构，便于团队协作

### 接入过程的挑战

虽然Kuikly带来了很多好处，但实际接入时也遇到了一些挑战，这里客观地说明一下。

## 前期基础建设的投入

在使用Kuikly开发需求时，我们发现现有的Kuikly工程缺少一些必要的基础组件，同时HelloTalk项目本身也有一些复杂的业务规则。这导致前期需要花较长时间做基础建设：

## 基础组件的补充和开发

Kuikly虽然提供了一套基础组件,但针对HelloTalk的具体场景，我们需要：

1. 补充一些常用但还没有实现的组件

2. 将Kuikly的组件体系和HelloTalk现有的设计规范对齐

3. 针对特殊场景开发定制化的组件（如多语言显示、特殊交互等）

## 原生能力的封装和桥接

HelloTalk作为一个成熟的产品，积累了大量的原生能力和工具类。要在Kuikly中使用这些能力，需要：

1. 将现有的原生UI组件封装为Kuikly可用的跨端组件

2. 将原生的工具类、网络库等基础设施桥接到Kuikly环境

3. 统一处理iOS和Android两端的实现差异

## 复杂业务规则的适配

HelloTalk经过多年迭代，积累了很多复杂的业务规则，迁移到Kuikly时需要特别处理：

1. 多语言和国际化：包括RTL布局、特殊语言的显示优化等

2. 性能优化规则：长列表、大图加载等场景的优化策略

3. 兼容性处理：确保Kuikly页面与原生页面的交互和跳转无缝衔接

## 必要的投入

虽然前期投入了较多时间在基础建设上，但这些工作是必要的：

1. **为后续开发铺平道路**：完善的基础组件库和工具链，能让后续的需求开发更高效

2. **建立最佳实践**：在摸索过程中积累的经验，形成了适合团队的开发规范

3. **提升复用价值**：封装好的组件和工具可以在多个页面中复用，投入产出比会越来越高

4. **真正发挥Kuikly价值**：只有基础设施完善了，才能真正发挥Kuikly跨端开发的效率优势

### 开发页面展示

以下是我们使用Kuikly开发并已上线的部分页面截图：

## 页面截图

![](https://mmecoa.qpic.cn/mmecoa_png/tnZGrhTk4dftfNDpz8XR3OlLqDsvXZFayIib1PsXWAmS7BqRo0LwdQW3vLC0zFlSJVSK0LTJtPmhB1AW5G0nxuA/640?wx_fmt=png&from=appmsg)

![](https://mmecoa.qpic.cn/mmecoa_png/tnZGrhTk4dftfNDpz8XR3OlLqDsvXZFaJcGFIKgR1Yb0FciakP2BdLDqEvbpDuaribKHxDibxpxMcAt3z4zHLJibCA/640?wx_fmt=png&from=appmsg)

### 开发实践

## Pager的学习和使用

Pager是接入Kuikly后第一个需要学习的概念。作为直接展示给用户的页面容器，理解Pager的使用是开发Kuikly页面的基础。

## Pager是什么

在Kuikly框架中，Pager是页面的入口类，类似于Android的Activity和iOS的ViewController。它承载整个页面的UI，负责管理页面的生命周期、状态和渲染。

根据[官方文档](https://kuikly.tds.qq.com/DevGuide/pager.html)，当原生容器跳转到Kuikly页面时，KuiklyCore会根据传递的pageName找到对应的Pager创建闭包，创建Pager实例。

## Pager的基本使用

一个基础的Pager需要做三件事：

## 1. 使用@Page注解注册

每个Pager类都需要用@Page注解标记，并指定一个唯一的路由名称：

| Kotlin @Page("HelloWorld") internal class HelloWorldPage : Pager() { // ... } |
| --- |

这个路由名称是原生代码打开该页面时使用的标识。

## 2. 实现body方法

body方法是必须实现的，它返回页面的UI结构描述：

| Kotlin override fun body(): ViewBuilder { return { attr { allCenter() } Text { attr { text("Hello Kuikly") fontSize(14f) } } } } |
| --- |

## 3. 获取页面参数PagerData

原生端打开Kuikly页面时可以传递参数，这些参数存储在PagerData中。根据[官方文档](https://kuikly.tds.qq.com/DevGuide/page-data.html)，我们可以在生命周期方法中通过pagerData获取：

| Kotlin override fun created() { super.created() // 获取原生传递的参数 val userId = pagerData.params.optString("userId") } |
| --- |

## Pager的生命周期

理解Pager的生命周期对于正确处理页面逻辑很重要。根据[官方文档](https://kuikly.tds.qq.com/DevGuide/pager-lifecycle.html)，Pager有以下生命周期方法：

| Kotlin @Page("ExamplePage") class ExamplePager : BasePager() { // 1. 页面创建 - 在body()之前调用 override fun created() { super.created() // 必须先调用父类方法 // 适合做初始化和数据拉取 fetchData() } // 2. 页面可见 override fun pageDidAppear() { super.pageDidAppear() // 页面在屏幕上完全显示时调用 // 适合做页面埋点、开始动画等 } // 3. 页面不可见 override fun pageDidDisappear() { super.pageDidDisappear() // 页面从屏幕上消失时调用 // 适合暂停动画、停止播放等 } // 4. 页面即将销毁 override fun pageWillDestroy() { super.pageWillDestroy() // 适合做资源释放、清理工作 // 注意：此时无法调用Module的Native方法 } } |
| --- |

## 几个注意事项：

1. 重写生命周期方法时，必须先调用super父类方法

2. created()在body()之前执行，适合做数据准备

3. pageWillDestroy()中无法通过Module调用Native方法

4. PagerData需要在created()或之后访问，在全局变量初始化时访问会报错

## 响应式状态管理

Kuikly的一大特点是响应式UI更新。当状态变量改变时，UI会自动重新渲染，用observable就可以实现：

| Kotlin // 定义响应式状态 private var count by observable(0) // UI中使用状态 override fun body(): ViewBuilder { val ctx = this return { Text { attr { text("点击次数: ${ctx.count}") } } Button { attr { text("点击我") } event { onClick { ctx.count++ // 修改状态会自动触发UI更新 } } } } } |
| --- |

## 一些体会

在学习和使用Pager的过程中，我们有这些体会：

1. 从简单的静态页面入手，逐步添加交互和数据

2. 正确使用生命周期方法能避免很多问题

3. 充分利用BasePager提供的便捷方法，避免重复造轮子

4. 保持代码结构清晰，让代码易于维护

5. 掌握observable的使用是写好Kuikly页面的关键

### 扩展原生View

在掌握了Pager的基本使用后，我们很快遇到了一个实际问题：如何在Kuikly中使用原生的业务组件？这就需要用到DeclarativeBaseView，它是连接Kuikly和原生组件的桥梁。

## 为什么需要扩展原生View

在开发过程中，我们发现Kuikly提供的基础组件（如Image、Text等）虽然能满足一般需求，但无法直接适配我们App已有的业务规范。

以头像组件为例：HelloTalk对头像有一套完整的显示规范，包括特定的圆角样式、边框处理、不同尺寸的规格定义、默认占位图的统一管理、图片加载和缓存策略等。

如果直接使用Kuikly的Image组件，我们需要在每个地方重复配置这些规则，不仅繁琐，还容易出现不一致。所以我们需要将原生端已有的头像组件（HTAvatarView）封装成Kuikly可用的组件。

这就是DeclarativeBaseView的作用：**将原生组件封装成Kuikly的声明式组件，让它能像内置组件一样在Kuikly DSL中使用。DeclarativeBaseView的基本使用**

封装一个原生组件需要在两端实现：

## Kotlin端 - 定义声明式组件

在commonMain中定义继承自DeclarativeBaseView的类：

| Kotlin // base/src/commonMain/kotlin/com/hellotalk/kuikly/base/widget/avatar/NativeAvatarView.kt class NativeAvatarView : DeclarativeBaseView () { override fun createAttr(): NativeAvatarViewAttr = NativeAvatarViewAttr() override fun createEvent(): NativeAvatarViewEvent = NativeAvatarViewEvent() // 这个名字必须和原生端注册的名字一致 override fun viewName(): String = "NativeAvatarView" } // 定义属性类 class NativeAvatarViewAttr : Attr() { // 设置头像大小 fun avatarSize(avatarSize: Int): NativeAvatarViewAttr { "avatarSize" with avatarSize return this } // 设置头像地址 fun avatarUrl(avatarUrl: String): NativeAvatarViewAttr { "avatarUrl" with avatarUrl return this } } // 定义事件类（如果需要） class NativeAvatarViewEvent : Event() // 提供DSL扩展函数 fun ViewContainer<*, *>.NativeAvatarView(init: NativeAvatarView.() -> Unit) { addChild(NativeAvatarView(), init) } |
| --- |

## 几个关键点：

1. DeclarativeBaseView需要两个泛型参数：属性类和事件类

2. viewName()返回的字符串用于关联原生端的实现

3. 属性通过"key" with value的方式设置，会传递给原生端

4. DSL扩展函数让组件可以在body()中直接使用

## 原生端 - 实现和注册组件Android端实现：

| Kotlin // androidApp/src/main/java/com/hellotalk/kuikly/view/avatar/NativeAvatarView.kt class NativeAvatarView(context: Context) : AppCompatImageView(context), IKuiklyRenderViewExport { // 实现这个接口 // 处理从Kuikly传递过来的属性 override fun setProp(propKey: String, propValue: Any): Boolean { when (propKey) { "avatarSize" -> { updateLayoutParams { width = propValue as? Int ?: 0 height = propValue as? Int ?: 0 } } "avatarUrl" -> { val avatarUrl = (propValue as? String).orEmpty() // 使用App的图片加载逻辑 setImageResource(R.drawable.ic_avatar_default) } else -> return super.setProp(propKey, propValue) } return true } } |
| --- |

## 注册到Kuikly：

| Kotlin // androidApp/src/main/java/com/hellotalk/kuikly/HTKuiklyRenderViewBaseDelegator.kt override fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) { super.registerExternalRenderView(kuiklyRenderExport) with(kuiklyRenderExport) { renderViewExport( "NativeAvatarView", // 必须和viewName()返回值一致 renderViewExportCreator = { context -> NativeAvatarView(context) } ) } } |
| --- |

## iOS端实现：

| Objective-C // iosApp/iosApp/KuiklyExpand/view/NativeAvatarView.h #import #import "KuiklyRenderViewExportProtocol.h" @interface NativeAvatarView : UIImageView @end |
| --- |

| Objective-C // iosApp/iosApp/KuiklyExpand/view/NativeAvatarView.m @implementation NativeAvatarView @synthesize hr_rootView; #pragma mark - Initialization - (instancetype)initWithFrame:(CGRect)frame { self = [super initWithFrame:frame]; if (self) { self.contentMode = UIViewContentModeScaleAspectFill; self.clipsToBounds = YES; [self setPlaceholderImage]; } return self; } #pragma mark - KuiklyRenderViewExportProtocol - (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue { if ([propKey isEqualToString:@"avatarSize"]) { CGFloat size = [propValue floatValue]; [self setAvatarSize:size]; } else if ([propKey isEqualToString:@"avatarUrl"]) { NSString *url = [propValue description]; [self setAvatarUrl:url]; } // 处理基础CSS属性（必须） KUIKLY_SET_CSS_COMMON_PROP; } - (void)hrv_prepareForReuse { // 组件复用时的清理工作 [self sd_cancelCurrentImageLoad]; [self setPlaceholderImage]; KUIKLY_RESET_CSS_COMMON_PROP; } #pragma mark - Property Setters - (void)setAvatarSize:(CGFloat)avatarSize { // 更新尺寸 [self updateSizeConstraints:avatarSize]; // 更新圆角 self.layer.cornerRadius = avatarSize / 2.0; self.layer.masksToBounds = YES; } - (void)setAvatarUrl:(NSString *)avatarUrl { if (!avatarUrl \|\| avatarUrl.length == 0) { [self setPlaceholderImage]; return; } // 使用SDWebImage加载图片 [self sd_setImageWithURL:[NSURL URLWithString:avatarUrl] placeholderImage:[self placeholderImage] options:SDWebImageRetryFailed completed:^(UIImage *image, NSError *error, SDImageCacheType cacheType, NSURL *imageURL) { if (error) { [self setPlaceholderImage]; } }]; } @end |
| --- |

## iOS端组件注册：

iOS端的组件注册比较简单，Kuikly会通过类名自动发现和注册实现了KuiklyRenderViewExportProtocol协议的组件，无需手动注册。只要确保：

1. 类实现了KuiklyRenderViewExportProtocol协议

2. 类名与Kotlin端viewName()返回值一致

3. 文件被正确编译到项目中

## Android与iOS端实现对比：

| 对比项 | Android端 | iOS端 |
| --- | --- | --- |
| 实现接口/协议 | IKuiklyRenderViewExport | KuiklyRenderViewExportProtocol |
| 属性设置方法 | setProp(propKey, propValue) | hrv_setPropWithKey:propValue: |
| 组件复用清理 | 通常在 onDetachedFromWindow() | hrv_prepareForReuse |
| 注册方式 | 需要手动注册到 registerExternalRenderView | 自动发现，无需手动注册 |
| 必须调用的宏 | 无 | KUIKLY_SET_CSS_COMMON_PROP |
| 语言 | Kotlin | Objective-C |

## 在Pager中使用

封装完成后，就可以像使用内置组件一样使用了：

| Kotlin @Page("user_profile") class UserProfilePager : BasePager() { override fun body(): ViewBuilder { return { View { attr { flex(1f) } // 使用封装的头像组件 NativeAvatarView { attr { width(100f) height(100f) avatarSize(100) avatarUrl(" https://example.com/avatar.jpg") } } } } } } |
| --- |

### Shadow的概念和使用

在封装某些组件时，我们还会遇到**布局测量**的问题，这时就需要用到`Shadow`。

## Shadow是什么

Shadow是Kuikly中用于**布局计算的代理类**，它的作用是：

1. 在布局计算阶段，将测量请求转发给原生端

2. 原生端根据实际内容计算出准确的尺寸

3. 将计算结果返回给Kuikly的布局引擎

## 为什么需要Shadow

以文本组件为例，文本的实际高度取决于文本内容的长度、字体大小和行高、容器的宽度约束、文本的换行规则等。这些复杂的计算在原生端已经有成熟的实现，通过Shadow机制，我们可以直接利用原生的测量能力，而不需要在Kuikly端重新实现。

## Shadow的基本使用

以HTTextView为例，它使用Shadow来实现准确的文本测量：

| Kotlin open class HTTextView : DeclarativeBaseView (), MeasureFunction { var shadow: HTTextShadow? = null // 初始化时创建Shadow override fun willInit() { super.willInit() shadow = HTTextShadow(pagerId, nativeRef, viewName()) } // 设置属性时同步到Shadow override fun didSetProp(propKey: String, propValue: Any) { if (isShadowProp(propKey)) { shadow?.setProp(propKey, propValue) flexNode.markDirty() // 标记需要重新布局 } } // 实现测量函数 override fun measure( node: FlexNode, width: Float, height: Float, measureOutput: MeasureOutput ) { // 通过Shadow计算尺寸 val size = shadow?.calculateRenderViewSize(width, height) measureOutput.width = size?.width ?: 0f measureOutput.height = size?.height ?: 0f } } |
| --- |

## Shadow的核心方法：

1. calculateRenderViewSize(width, height) - 请求原生端计算尺寸

2. setProp(key, value) - 同步属性到Shadow，用于测量计算

3. markDirty() - 标记需要重新计算

## 原生端的配合

在注册组件时，可以提供对应的Shadow创建器：

| Kotlin renderViewExport( "KRHTTextView", renderViewExportCreator = { context -> KRHTTextView(context) }, shadowExportCreator = { // 提供Shadow创建器 KRHTTextShadow() } ) |
| --- |

原生端实现calculateRenderViewSize方法来响应测量请求：

| Kotlin override fun calculateRenderViewSize(width: Float, height: Float): String { // 根据当前属性和约束计算实际尺寸 val measuredWidth = // ... 测量逻辑 val measuredHeight = // ... 测量逻辑 return "$measuredWidth\|$measuredHeight" } |
| --- |

### 多语言的适配方式

## 多语言适配实现

我们的App已经在原生端建立了完善的多语言管理体系。在接入Kuikly后，为了兼容宿主App的多语言系统，避免在Kuikly端重复定义和维护大量的多语言文案，我们采用了**Module桥接**的方式：Kuikly端只维护一个多语言Key的列表配置（`KRStrings`），实际的多语言文案获取则通过`LocalizedStringModule`桥接到原生端，直接复用原生的多语言资源。

## 多语言系统架构

整体数据流如下：

| Plain Text Kuikly层 → LocalizedStringModule(桥接) → 原生端KRLocalizedStringModule → 宿主App多语言资源 |
| --- |

核心组件包括：

1. **KRStrings** - Kuikly端维护的多语言Key列表配置

2. **LocalizedStringModule** - Kuikly侧的桥接Module

3. **KRLocalizedStringModule** - 原生侧的Module实现，负责从宿主App多语言资源中获取文案

4. **localizable()扩展函数** - 便捷的调用方式

## Kuikly端的Key列表配置

在Kuikly端，我们只需要维护一个多语言Key的列表配置：

| Kotlin // base/src/commonMain/kotlin/com/hellotalk/kuikly/base/res/strings/KRStrings.kt object KRStrings { const val REPORT_BUG = "Report Bug" const val PLEASE_ENABLE_PHOTO_ALBUM_ACCESS = "Please enable photo album access" const val LINE_BREAK = "Line Break" const val RESTORE_PURCHASES = "Restore Purchases" // ... 更多多语言Key } |
| --- |

这些Key对应着原生端多语言资源中的Key，值通常设置为英文，作为找不到对应翻译时的兜底显示。我们不需要在Kuikly端维护各语言的翻译文案，所有翻译都复用原生端已有的资源。

## LocalizedStringModule - Kuikly侧的实现

LocalizedStringModule是Kuikly侧的Module，负责将多语言请求桥接到原生端：

| Kotlin // base/src/commonMain/kotlin/com/hellotalk/kuikly/base/res/strings/LocalizedStringModule.kt class LocalizedStringModule : Module() { companion object { const val MODULE_NAME = "KRLocalizedStringModule" private const val METHOD_GET_STRING = "getString" } override fun moduleName(): String = MODULE_NAME fun getString(name: String, vararg args: Any?): String { val data = JSONObject().apply { put("key", name) if (args.isNotEmpty()) { put("params", JSONArray().apply { for (arg in args) { put(arg) } }) } } val result = syncToNativeMethod(METHOD_GET_STRING, data, null) return result } } |
| --- |

这个Module通过syncToNativeMethod同步调用原生方法，支持在文案中使用占位符进行参数替换（如：Hello %s）。

## 便捷的使用方式

为了简化调用，我们提供了String.localizable()扩展函数：

| Kotlin // base/src/commonMain/kotlin/com/hellotalk/kuikly/base/pager/api/BasePagerExt.kt fun String.localizable(): String { return HTPagerManager.instance.acquireModule (LocalizedStringModule.MODULE_NAME) ?.getString(this) ?: this } |
| --- |

这样在Pager中就可以很方便地使用多语言：

| Kotlin @Page("example_page") class ExamplePager : BasePager() { override fun body(): ViewBuilder { return { View { attr { flex(1f) } HTText { attr { text(KRStrings.REPORT_BUG.localizable()) fontSize(16f) } } Button { attr { text(KRStrings.RESTORE_PURCHASES.localizable()) } event { onClick { // 处理点击 } } } } } } } |
| --- |

## 原生端实现Android端：

| Kotlin // androidApp/src/main/java/com/hellotalk/kuikly/module/KRLocalizedStringModule.kt class KRLocalizedStringModule : KuiklyRenderBaseModule() { companion object { const val MODULE_NAME = "KRLocalizedStringModule" } override fun call(method: String, params: Any?, callback: KuiklyRenderCallback?): Any? { return when (method) { "getString" -> getString(params) else -> super.call(method, params, callback) } } private fun getString(params: Any?): String? { val json = params?.toString()?.let { JSONObject(it) } ?: return null val name = json.optString("key") // 通过StringResMap将key映射到R.string资源ID val resId = StringResMap.getStringResId(name) ?: return name // 支持参数化文案 val formatArgsJson = json.optJSONArray("params") return if (formatArgsJson != null && formatArgsJson.length() > 0) { val args = Array(formatArgsJson.length()) { i -> formatArgsJson.get(i).toString() } KRApplication.application.getString(resId, *args) } else { KRApplication.application.getString(resId) } } } |
| --- |

## iOS端：

| Objective-C // iosApp/iosApp/KuiklyExpand/Modules/KRLocalizedStringModule.m @implementation KRLocalizedStringModule - (id)getString:(NSDictionary *)args { NSString *jsonStr = args[@"param"]; NSDictionary *param = [SwiftKuiklyTool toDictionary:jsonStr]; NSString *key = param[@"key"]; // 使用NSLocalizedString获取本地化字符串 NSString *format = NSLocalizedString(key, nil); if (![format isKindOfClass:[NSString class]]) return key ?: @""; // 处理参数替换 NSArray *params = param[@"params"]; return [SwiftKuiklyTool textString:format args:params]; } @end |
| --- |

## Module的注册

在BasePager中，我们会自动注册LocalizedStringModule，这样所有继承自BasePager的页面都可以直接使用多语言功能：

| Kotlin // base/src/commonMain/kotlin/com/hellotalk/kuikly/base/pager/api/BasePager.kt override fun createExternalModules(): Map ? { val externalModules = hashMapOf () // ... 其他模块 externalModules[LocalizedStringModule.MODULE_NAME] = LocalizedStringModule() // ... return externalModules } |
| --- |

通过这套基于Module桥接的方案，我们成功地让Kuikly页面复用了宿主App的多语言资源。

### 阿拉伯语的适配实现

## 为什么需要特殊适配

HelloTalk作为一个面向全球用户的跨文化交流平台，支持包括阿拉伯语在内的多种语言是产品的基础能力。阿拉伯语是一种**从右到左（RTL）**书写的语言，这和我们常见的从左到右（LTR）的书写方向完全相反

在实际使用Kuikly官方提供的Text和RichText组件时，我们发现它们对阿拉伯语的显示存在一些问题：

1. **混合文本处理不当**：当文本中同时包含阿拉伯语、数字和拉丁字母时，显示顺序和对齐方式不符合预期

2. **对齐方式不智能**：纯阿拉伯语文本应该右对齐，但组件没有自动识别并调整

3. **双向文本算法**：iOS和Android系统对双向文本（Bidi）的处理策略不同，需要统一处理逻辑

这些问题会严重影响阿拉伯语用户的使用体验，所以我们决定重写这两个组件。

## 重写HTTextView和HTRichTextView的核心差异

我们基于Kuikly的DeclarativeBaseView机制，重新实现了HTTextView和HTRichTextView，在保持与官方组件API兼容的前提下，针对阿拉伯语做了专门的优化。

## 主要实现差异包括：1. 智能文本方向检测

我们实现了自动检测文本内容的能力，判断文本是否包含RTL字符：

| Kotlin // Kuikly端添加RTL支持开关 const val RTL_SUPPORT = "rtlSupport" // 使用方式 HTText { attr { text("مرحبا بك") // 阿拉伯语"欢迎" rtlSupport(true) // 启用RTL支持 } } |
| --- |

在原生端，我们会检测文本内容是否包含RTL字符、是否包含数字、是否包含拉丁字母。

## 2. 混合文本的特殊处理

对于混合文本（同时包含RTL字符和LTR字符），我们采用了特殊的策略：

- **纯RTL文本**：使用RTL书写方向，右对齐

- **混合文本**：使用自然（Natural）书写方向，让系统的双向文本算法自动处理

- **纯LTR文本**：使用LTR书写方向，左对齐

## 3. 智能对齐方式

我们实现了智能对齐逻辑：

| Plain Text // iOS端示例逻辑 if (isPrimaryRTLText(text)) { alignment = NSTextAlignmentRight; // 纯RTL文本右对齐 } else if (containsRTLText(text) && containsLTRText(text)) { alignment = NSTextAlignmentNatural; // 混合文本使用自然对齐 } else { alignment = NSTextAlignmentLeft; // LTR文本左对齐 } |
| --- |

这样可以根据文本内容自动选择合适的对齐方式，不需要业务代码手动判断。

## 4. 段落样式优化

针对RTL文本，我们优化了段落样式的设置：

- **baseWritingDirection**：根据文本内容智能设置段落的基础书写方向

- **首行缩进**：RTL文本的首行缩进需要从右侧开始

- **行间距和字间距**：保持与LTR文本一致的视觉效果

## 5. 跨平台一致性

由于iOS和Android对RTL文本的处理机制不同，我们在两端分别实现了相同的检测和处理逻辑，确保相同的阿拉伯语文本在两个平台显示一致。

## RTL布局支持

除了文本组件，我们还扩展了通用的布局属性，提供了RTL自适应的布局能力：

| Kotlin // 使用Start/End语义，自动适配RTL fun Attr.marginStart(start: Float) // LTR时=marginLeft, RTL时=marginRight fun Attr.marginEnd(end: Float) // LTR时=marginRight, RTL时=marginLeft fun Attr.paddingStart(start: Float) fun Attr.paddingEnd(end: Float) // 使用示例 View { attr { marginStart(16f) // 在阿拉伯语环境下自动变为右边距 paddingStart(8f) } } |
| --- |

这些扩展方法根据CommonResConfig.isRtl标志自动映射到正确的方向，业务代码无需手写RTL分支判断。

## 实现效果

通过重写这两个组件，我们实现了：

1. **自动识别**：无需手动判断文本语言，组件自动识别并处理

2. **显示正确**：阿拉伯语文本在各种场景下都能正确显示

3. **混合文本支持**：包含多种语言的文本也能正确渲染

4. **API兼容**：与官方组件保持API兼容，业务代码只需替换组件名即可

这套方案不仅解决了阿拉伯语的显示问题，也为支持其他RTL语言（如希伯来语、波斯语等）打下了基础。

### 多模块的搭建

随着Kuikly开发的深入，我们开发的页面和组件越来越多，如果所有代码都放在一个模块中，会导致编译时间变长、代码管理混乱、团队协作困难。为了更好地组织代码和提高开发效率，我们采用了Kuikly的多模块架构。

## 为什么需要多模块

在使用多模块之前，我们面临这些问题：

1. **编译时间长**：所有代码在一个模块中，每次修改都需要重新编译整个模块

2. **代码耦合**：不同业务的代码混在一起，难以维护和复用

3. **团队协作**：多人同时开发容易产生代码冲突

4. **职责不清**：基础能力、通用组件、业务页面混杂在一起

Kuikly提供的多模块机制可以解决这些问题，它允许我们将代码按职责划分到不同的模块中，各模块独立编译，互不干扰。

## 项目的模块结构

我们的项目采用了以下模块结构：

| Plain Text HTKuiklyProject/ ├── androidApp/ - Android宿主应用 ├── iosApp/ - iOS宿主应用 ├── base/ - 基础模块（基础组件和工具） └── shared/ - 业务模块（业务页面） |
| --- |

其中base模块负责基础能力层，shared模块负责业务逻辑层，两个原生宿主负责框架初始化和原生能力注册。

## 多模块配置

要让Kuikly支持多模块，需要在每个模块的build.gradle.kts中进行配置。

## base模块配置

| Kotlin // base/build.gradle.kts plugins { kotlin("multiplatform") kotlin("native.cocoapods") id("com.android.library") id("com.google.devtools.ksp") // KSP插件，用于代码生成 } // KSP配置 ksp { arg("moduleId", "base") // 模块唯一ID arg("isMainModule", "false") // 是否为主模块 arg("enableMultiModule", "true") // 启用多模块支持 } dependencies { // 添加KSP处理器依赖 compileOnly("com.tencent.kuikly-open:core-ksp:${Version.getKuiklyVersion()}") { add("kspAndroid", this) add("kspIosArm64", this) add("kspIosX64", this) add("kspIosSimulatorArm64", this) } } |
| --- |

## 配置说明：

1. moduleId：模块的唯一标识符，用于区分不同的模块

2. isMainModule：标识是否为主模块。通常只有一个模块设置为true

3. enableMultiModule：启用多模块支持，让KSP能够正确处理跨模块的页面注册

## shared模块配置

| Kotlin // shared/build.gradle.kts ksp { arg("moduleId", "shared") arg("isMainModule", "true") // shared作为主模块 arg("enableMultiModule", "true") } dependencies { // 依赖base模块 implementation(project(":base")) // 添加KSP处理器 compileOnly("com.tencent.kuikly-open:core-ksp:${Version.getKuiklyVersion()}") { add("kspAndroid", this) add("kspIosArm64", this) add("kspIosX64", this) add("kspIosSimulatorArm64", this) } } |
| --- |

## 多模块的工作原理

Kuikly的多模块机制基于KSP（Kotlin Symbol Processing）实现：

1. **编译时处理**：KSP在编译时扫描各模块中的`@Page`注解

2. **代码生成**：为每个模块生成页面注册代码

3. **模块聚合**：主模块（shared）会聚合所有子模块（base）的页面注册信息

4. **统一路由**：运行时，所有模块的页面都可以通过路由名称访问

这样的设计使得每个模块可以独立开发和编译，模块之间通过依赖关系组织，页面路由全局统一。

## 模块间的依赖关系

| Plain Text androidApp ──┐ ├──> shared ──> base ──> kuikly-core iosApp ──────┘ |
| --- |

依赖方向是单向的，base模块不依赖shared模块，保证了基础层的独立性和可复用性。

## settings.gradle.kts配置

在项目根目录的settings.gradle.kts中，需要声明所有模块：

| Kotlin rootProject.name = "HTKuiklyProject" include(":androidApp") include(":shared") include(":base") |
| --- |

## 多模块带来的好处

通过多模块架构，我们获得了这些收益：

1. **编译速度提升**：修改某个模块时，只需重新编译该模块，其他模块不受影响

2. **职责清晰**：基础能力和业务逻辑分离，分工明确

3. **便于复用**：base模块可以在多个项目中复用

4. **团队协作**：不同团队可以负责不同模块，减少代码冲突

通过以上配置，我们成功地将Kuikly项目拆分为多个模块，每个模块独立编译和管理，大大提高了开发效率。

### 通过AI开发Kuikly

在使用Kuikly开发过程中，我们探索出了一套基于AI的开发协作模式。为了规范化和标准化开发流程，我们构建了一套完整的AI辅助开发系统，大大提升了开发效率和代码质量。

## AI辅助开发系统概览

我们在项目的ai-prompts/目录下建立了一套完整的AI协作系统，包含：

| Plain Text ai-prompts/ ├── 00-role-definition.md # AI角色定义 ├── 01-development-workflow.md # 开发工作流程 ├── layer-configs/ # 分层级开发配置 ├── knowledge-base/ # 知识库 └── templates/ # 代码模板 |
| --- |

这套系统的核心思想是：**让AI深度理解Kuikly的技术栈和项目规范，按照标准化的工作流程协助开发，避免重复造轮子，确保代码质量和一致性。核心文件说明角色定义（00-role-definition.md）**

这个文件定义了AI作为Kuikly开发专家应该具备的能力和遵循的原则：

## 核心技术能力：

1. Kuikly框架：Pager系统、组件体系、模块系统、状态管理

2. KMP（Kotlin Multiplatform）：跨平台开发、代码组织

3. Android开发：View系统、Compose、自定义View

4. iOS开发：UIKit、SwiftUI、自定义View

## 开发原则：

1. **组件优先原则**：优先使用已有组件，避免重复造轮子

2. **渐进式开发原则**：先分析方案，再选择组件，最后实现代码

3. **规范至上原则**：严格遵循项目代码规范与最佳实践

4. **文档同步原则**：代码与文档同步更新

5. **质量保证原则**：代码质量优先于开发速度

## 工作流程（01-development-workflow.md）

定义了标准的六阶段开发流程：

| Plain Text 阶段1: 理解需求 ↓ 阶段2: 分析方案 ↓ 阶段3: 查找组件 ⚠️ 必需步骤，不可跳过 ↓ 阶段4: 设计架构 ↓ 阶段5: 编写代码 ↓ 阶段6: 生成文档 |
| --- |

这个流程的关键是**阶段3（查找组件）是强制步骤**，必须先查询知识库确认没有现成组件可用，才能开始编写新代码。这样可以最大程度地复用现有代码，避免重复开发。

## 分层级配置（layer-configs/）

根据代码的复杂度和抽象层级，我们定义了五个开发层级：

- **L1 - 业务代码层**：具体业务页面（Pager），直接使用组件实现业务需求

- **L2 - 业务组件层**：可复用的业务级组件，封装常见业务场景

- **L3 - 基础组件层**：通用基础组件，不涉及具体业务

- **L4 - 跨平台组件层**：KMP共享组件，实现跨平台复用

- **L5 - Native View层**：原生自定义视图，需要Android/iOS分别实现

每个层级都有对应的配置文件，明确了该层级的特点、开发规范、组件使用指南等。

## 知识库（knowledge-base/）

知识库是整个系统的核心，包含了Kuikly开发所需的所有技术文档：

## 目录结构：

| Plain Text knowledge-base/ ├── quick-reference/ # 快速参考 │ ├── components-quick.md # 组件速查表 │ ├── modules-quick.md # 模块速查表 │ ├── patterns-quick.md # 常用模式 │ └── ... ├── components/ # 组件详细文档 │ ├── button.md │ ├── text.md │ ├── list.md │ ├── ht-components/ # 自定义组件 │ └── ... ├── modules/ # 模块详细文档 │ ├── router.md │ ├── network.md │ ├── ht-modules/ # 自定义模块 │ └── ... ├── architecture/ # 架构文档 │ └── ... └── guidelines/ # 开发指南 ├── pager-development-guide.md ├── component-usage-guidelines.md └── ... |
| --- |

## 知识库的作用：

1. **快速参考（quick-reference/）**：供AI快速查询的精简版文档，包含组件速查表、模块速查表、常用模式等，让AI在1-2秒内就能找到需要的组件

2. **详细文档**：完整的API文档、使用示例、注意事项，供深入了解组件细节时使用

3. **开发指南**：Pager开发、组件使用、暗黑模式、RTL、多语言等主题的系统性指南

## 知识库的价值：

- **避免重复造轮子**：AI可以快速查询已有组件，避免重复开发

- **确保代码一致性**：所有开发都基于同一套文档和规范

- **提高开发效率**：不需要每次都去翻阅源码或官方文档

- **降低学习成本**：新接手的开发者可以快速上手

## 代码模板（templates/）

提供了标准的代码结构模板：

1. 业务页面模板

2. 业务组件模板

3. 基础组件模板

4. Native View模板

使用模板可以确保代码结构的一致性，减少低级错误。

## 实际使用方式

当我们需要开发一个新功能时，会给AI提供以下提示词：

| Markdown # Kuikly 开发任务## 角色配置 请加载以下文件作为你的工作指南： - ai-prompts/00-role-definition.md - ai-prompts/01-development-workflow.md - ai-prompts/layer-configs/L1-business-logic.md ## 开发需求 [详细描述需求] ## 功能需求- 功能点1 - 功能点2 ## UI要求 [UI描述或设计图] ## 技术要求 [特殊技术要求] 请按照标准工作流程完成开发。 |
| --- |

AI会按照六阶段流程，先理解需求、分析方案、查找可用组件，然后再设计架构、编写代码、生成文档。整个过程规范、高效、可追溯。

## 推荐结语

Kuikly跨端框架正受到越来越多开发者的关注，并已在众多业务中成功落地。希望HelloTalk的实践经验能为各位开发者带来帮助和参考。同时，也欢迎更多开发者积极参与分享，共同推动技术社区的成长。
若你刚接触Kuikly，可通过以下途径快速了解与接入：

- 进入腾讯端服务联盟（[tds.qq.com](https://tds.qq.com/?utm_source=artical6)）主页访问Kuikly官网（[framework.tds.qq.com](https://framework.tds.qq.com/?utm_source=artical6)）。

- 通过以下Kuikly文档和仓库，快速体验：

[官方文档](https://kuikly.tds.qq.com/Introduction/arch.html?utm_source=artical6)

[Github仓库](https://github.com/Tencent-TDS/KuiklyUI)

- 关注和联系他们

![图片](https://mmbiz.qpic.cn/mmbiz_png/ibrUhU3BKx7CXXo8ibGPEGJpictVPLCDf3jN8EjP2hUEJkt4aLZU5CAZ41mTqvdoOTkXvwSkSzTZcKGfL8GsslQhQ/640?wx_fmt=png&from=appmsg&randomid=mus016ue&wxfrom=5&wx_lazy=1&tp=webp#imgIndex=21)
