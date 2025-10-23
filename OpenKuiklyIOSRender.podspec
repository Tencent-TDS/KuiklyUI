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
  spec.source_files = 'core-render-ios/**/*.{h,m,c,mm,s,cpp,cc}'
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

  # macOS 先行编译通过：排除暂未适配的 iOS-only/复杂组件
  spec.osx.exclude_files = [
    'core-render-ios/Extension/Components/KRTextFieldView.{h,m}',
    'core-render-ios/Extension/Components/KRTextAreaView.{h,m}',
    'core-render-ios/Extension/Components/KRScrollView.{h,m}',
    'core-render-ios/Extension/Components/KRListView.{h,m}',
    'core-render-ios/Extension/Components/KRScrollView+NestedScroll.{h,m}',
    'core-render-ios/Extension/Components/NestScroll/**/*',
    'core-render-ios/Extension/Components/KRView+Compose.{h,m}',
    'core-render-ios/Extension/AdvancedComps/**/*',
    
    # 其他UI组件
    'core-render-ios/Extension/Vendor/KRLabel.m',
    'core-render-ios/Extension/Components/KRComposeGesture.m',
    'core-render-ios/Extension/AdvancedComps/KRModalView.m',

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
  ]
end
