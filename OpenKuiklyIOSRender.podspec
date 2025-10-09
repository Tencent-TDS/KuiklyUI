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
    # 回收核心：逐步恢复 UIView+CSS（等效/降级） [macOS]
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
    'core-render-ios/Handler/KuiklyTurboDisplay/**/*',
    'core-render-ios/Extension/Components/KRImageView.m',
    'core-render-ios/Extension/Vendor/KRLabel.m',
    'core-render-ios/Extension/Components/KRComposeGesture.m',
    'core-render-ios/Extension/AdvancedComps/KRModalView.m',
    # 依赖 iOS-only 符号或尚未适配的桥接/核心入口
    'core-render-ios/Extension/BridgeProtocol/KuiklyRenderBridge.m',
    'core-render-ios/Core/KuiklyRenderCore.m',
    # 其它在 macOS 下使用大量 UIKit API 的实现，M1 暂时排除
    'core-render-ios/Extension/Category/NSObject+KR.m',
    'core-render-ios/View/KuiklyRenderView.*',
    'core-render-ios/Extension/KuiklyRenderViewControllerBaseDelegator.m',
    'core-render-ios/Extension/Modules/KRMemoryCacheModule.m',
    'core-render-ios/Extension/Modules/KRSnapshotModule.m',
    # 临时移除模块与大入口，待 M2/M3 回收
    'core-render-ios/Extension/Modules/**/*',
    'core-render-ios/Extension/Kuikly*.*',
    # 仍大量依赖 UIKit 的分类
    # 线程/Handler 模块依赖未适配日志模块
    'core-render-ios/Thread/**/*',
    'core-render-ios/Handler/**/*'
  ]
end
