Pod::Spec.new do |spec|
  spec.name             = "OpenKuiklyIOSRender"
  spec.version          = "2.5.0"
  spec.summary          = "Kuikly"
  spec.description      = <<-DESC
                        -Kuikly iOS/macOS平台渲染依赖库
                        DESC
  spec.homepage         = "https://github.com/Tencent-TDS/KuiklyUI"
  spec.license          = { :type => "KuiklyUI", :file => "LICENSE" }
  spec.author           = { "Kuikly" => "ruifanyuan@tencent.com" }
  spec.ios.deployment_target = '9.0'
  spec.osx.deployment_target = '10.15'
  spec.source           = { :git => "https://github.com/Tencent-TDS/KuiklyUI.git", :tag => "#{spec.version}" }
  spec.user_target_xcconfig = { 'CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES' => 'YES' }
  spec.requires_arc     = true
  # 通用最小白名单（macOS 用）
  spec.source_files = [
    'core-render-ios/MacSupport/RCTUIKit.{h,m}',
    'core-render-ios/Extension/Vendor/KRDisplayLink.{h,m}',
    'core-render-ios/Extension/Category/NSView+KuiklyCompat.{h,m}',
    'core-render-ios/Extension/Components/Base/KRScrollViewOffsetAnimator.{h,m}'
  ]
  # iOS 保持原有大范围参与
  spec.ios.source_files = 'core-render-ios/**/*.{h,m,c,mm,s,cpp,cc}'
  spec.exclude_files = 'core-render-ios/include/**/*'
  spec.libraries    = "c++"
  # iOS/macOS 平台分别声明需要的系统框架
  spec.ios.frameworks = [
    'UIKit',
    'QuartzCore',
    'CoreGraphics',
    'Foundation',
    'CoreText'
  ]
  spec.osx.frameworks = [
    'AppKit',
    'QuartzCore',
    'CoreGraphics',
    'Foundation',
    'CoreText'
  ]
  # [macOS] 采用最小白名单编译集合，避免未适配源码参与
  spec.osx.source_files = [
    'core-render-ios/MacSupport/RCTUIKit.{h,m}',
    'core-render-ios/Extension/Vendor/KRDisplayLink.{h,m}',
    'core-render-ios/Extension/Category/NSView+KuiklyCompat.{h,m}',
    'core-render-ios/Extension/Components/Base/KRScrollViewOffsetAnimator.{h,m}',
    # 为 KuiklyRenderCallback 引入 typedef [macOS]
    'core-render-ios/Protocol/KuiklyRenderModuleExportProtocol.h',
    # KRConvertUtil 参与，提供类型/方法（UIColor/BezierPath 等） [macOS]
    'core-render-ios/Extension/Category/KRConvertUtil.{h,m}',
    'core-render-ios/Extension/KRComponentDefine.h',
    'core-render-ios/Extension/Category/NSObject+KR.{h,m}',
    # 回收核心：逐步恢复 UIView+CSS（等效/降级） [macOS]
    'core-render-ios/Extension/Category/UIView+CSS.{h,m}',
    # 核心渲染：核心类与 RootView [macOS]
    'core-render-ios/Core/KuiklyRenderCore.{h,m}',
    'core-render-ios/Core/KuiklyContextParam.{h,m}',
    'core-render-ios/View/KuiklyRenderView.{h,m}',
    'core-render-ios/Core/KuiklyRenderUIScheduler.{h,m}',
    'core-render-ios/Thread/KuiklyRenderThreadManager.{h,m}',
    'core-render-ios/Thread/KuiklyRenderThreadLock.{h,m}',
    'core-render-ios/Handler/KuiklyRenderLayerHandler.mm',
    'core-render-ios/Handler/KuiklyRenderLayerHandler.h',
    'core-render-ios/Extension/KuiklyBridgeDelegator.{h,m}',
    # TurboDisplay 回收（先头文件与核心类，依赖最少）
    'core-render-ios/Extension/Modules/KRTurboDisplayModule.{h,m}',
    'core-render-ios/Handler/KuiklyTurboDisplay/KuiklyTurboDisplayRenderLayerHandler.{h,m}',
    'core-render-ios/Handler/KuiklyTurboDisplay/KRTurboDisplayCacheManager.{h,m}',
    'core-render-ios/Handler/KuiklyTurboDisplay/KRTurboDisplayNode.{h,m}',
    'core-render-ios/Handler/KuiklyTurboDisplay/KRTurboDisplayShadow.{h,m}',
    'core-render-ios/Handler/KuiklyTurboDisplay/KRTurboDisplayDiffPatch.{h,m}',
    'core-render-ios/Handler/KuiklyTurboDisplay/KRTurboDisplayNodeMethod.{h,m}',
    'core-render-ios/Handler/KuiklyTurboDisplay/KRTurboDisplayProp.{h,m}',
    # TurboDisplay 依赖的模块
    # 保留 TurboDisplayModule，不在排除列表
    'core-render-ios/Extension/Modules/KRMemoryCacheModule.{h,m}',
    # 组件依赖（KRMemoryCacheModule -> KRImageView）
    'core-render-ios/Extension/Components/KRImageView.{h,m}',
    'core-render-ios/Protocol/KuiklyRenderLayerProtocol.h',
    'core-render-ios/Protocol/KuiklyRenderContextProtocol.h',
    'core-render-ios/Protocol/KuiklyRenderViewExportProtocol.h',
    # 暂不纳入 KuiklyRenderBridge 实现（接口引用保留在被包含文件内）
    'core-render-ios/Extension/BridgeProtocol/KuiklyRenderBridge.h',
    # 回收基础日志模块 [macOS]
    'core-render-ios/Extension/Modules/KRBaseModule.{h,m}',
    'core-render-ios/Extension/Modules/KRLogModule.{h,m}',
    'core-render-ios/Handler/KuiklyRenderFrameworkContextHandler.{h,m}',
    'core-render-ios/Extension/Components/Base/KRMultiDelegateProxy.{h,m}',
    'core-render-ios/Extension/Components/Base/KRWeakObject.{h,m}',
    'core-render-ios/TDFCommon/*.{h,m,mm}',
    'core-render-ios/Extension/Components/Base/KRCacheManager.{h,m}',
    # KRBlurView 供 KRImageView 依赖
    'core-render-ios/Extension/AdvancedComps/KRBlurView.{h,m}',
    # 网络工具依赖日志模块，已排除 Modules，这里也暂不纳入
    # 'core-render-ios/Extension/Vendor/KRHttpRequestTool.{h,m}',
    # 视图分类与 CSS 支撑（依赖广泛）
    'core-render-ios/Extension/Category/UIView+CSS.{h,m}'
  ]
  # macOS 先行编译通过：排除暂未适配的 iOS-only/复杂组件
  spec.osx.exclude_files = [
    'core-render-ios/Extension/Components/KRTextFieldView.{h,m}',
    'core-render-ios/Extension/Components/KRTextAreaView.{h,m}',
    'core-render-ios/Extension/Components/KRScrollView.{h,m}',
    'core-render-ios/Extension/Components/KRScrollView+NestedScroll.{h,m}',
    'core-render-ios/Extension/Components/NestScroll/**/*',
    'core-render-ios/Extension/AdvancedComps/**/*',
    # 保留 TurboDisplay 文件参与编译（已在 source_files 明确列入），不再排除
    # 保留 KRImageView
    'core-render-ios/Extension/Vendor/KRLabel.m',
    'core-render-ios/Extension/Components/KRComposeGesture.m',
    'core-render-ios/Extension/AdvancedComps/KRModalView.m',
    # 依赖 iOS-only 符号或尚未适配的桥接/核心入口
    # 保留桥接实现，参与编译（必要 typedef/接口）
    # 其它在 macOS 下使用大量 UIKit API 的实现，M1 暂时排除
    'core-render-ios/Extension/KuiklyRenderViewControllerBaseDelegator.m',
    # 暂时排除 NSObject+KR.m 的 iOS-only 渲染片段，后续回收
    'core-render-ios/Extension/Category/NSObject+KR.m',
    # 模块先定点排除（显式列出不纳入的模块，其它允许逐步回收）
    'core-render-ios/Extension/Modules/KRCalendarModule.{h,m}',
    'core-render-ios/Extension/Modules/KRCodecModule.{h,m}',
    # 保留 KRFontModule 参与编译（Bridge 头引用）
    'core-render-ios/Extension/Modules/KRNetworkModule.{h,m}',
    'core-render-ios/Extension/Modules/KRNotifyModule.{h,m}',
    'core-render-ios/Extension/Modules/KRReflectionModule.{h,m}',
    'core-render-ios/Extension/Modules/KRRouterModule.{h,m}',
    'core-render-ios/Extension/Modules/KRSharedPreferencesModule.{h,m}',
    'core-render-ios/Extension/Modules/KRSnapshotModule.{h,m}',
    'core-render-ios/Extension/Modules/KRVsyncModule.{h,mm}',
    # 保留 Kuikly* 相关以满足桥接与 Delegator 依赖
  ]
end
