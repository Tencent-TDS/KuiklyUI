Pod::Spec.new do |spec|
  spec.name             = "OpenKuiklyIOSRender"
  spec.version          = "2.11.0"
  spec.summary          = "Kuikly"
  spec.description      = <<-DESC
                        -Kuikly iOS/macOS平台渲染依赖库
                        DESC
  spec.homepage         = "https://github.com/Tencent-TDS/KuiklyUI"
  spec.license          = { :type => "KuiklyUI", :file => "LICENSE" }
  spec.author           = "Kuikly Team"
  spec.ios.deployment_target = '12.0'
  spec.osx.deployment_target = '10.13'
  spec.source           = { :git => "https://github.com/Tencent-TDS/KuiklyUI.git", :tag => "#{spec.version}" }
  spec.user_target_xcconfig = { 'CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES' => 'YES' }
  spec.requires_arc     = true
  spec.source_files = 'core-render-ios/**/*.{h,m,c,mm,s,cpp,cc}'
  spec.exclude_files = 'core-render-ios/include/**/*'
  spec.libraries    = "c++"
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

end
