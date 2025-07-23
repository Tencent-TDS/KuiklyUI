Pod::Spec.new do |spec|
  spec.name             = "OpenKuiklyIOSRender"
  spec.version          = "2.2.0"
  spec.summary          = "Kuikly"
  spec.description      = <<-DESC
                        -Kuikly iOS平台渲染依赖库
                        DESC
  spec.homepage         = "https://github.com/Tencent-TDS/KuiklyUI"
  spec.license          = { :type => "KuiklyUI", :file => "LICENSE" }
  spec.author           = { "Kuikly" => "ruifanyuan@tencent.com" }
  spec.ios.deployment_target = '9.0'
  spec.source           = { :git => "https://github.com/Tencent-TDS/KuiklyUI.git", :tag => "#{spec.version}" }
  spec.user_target_xcconfig = { 'CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES' => 'YES' }
  spec.requires_arc     = true
  spec.source_files = 'core-render-ios/**/*.{h,m,c,mm,s,cpp,cc}'
  spec.libraries    = "c++"
end
