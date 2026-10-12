Pod::Spec.new do |s|
  s.name         = 'LLVMCompileRT'
  s.version      = '1.0.0'
  s.summary      = 'LLVM Clang Profile Runtime，用于 KuiklyUI demo 的 LLVM PGO 插装链接'
  s.description  = <<-DESC
    提供 libclang_rt.profile_ios.a / libclang_rt.profile_iossim.a（iossim 为 arm64+x86_64 fat），
    仅在 LLVM_PGO_TYPE=LLVMPGO 插装包中引入，供运行时写出 .profraw。
    请先执行 iosApp/scripts/setup_llvm_compile_rt.sh（优先工具链自带同版本库，否则从 compiler-rt 源码编译）。
  DESC
  s.homepage     = 'https://github.com/Tencent-TDS/KuiklyUI'
  s.license      = { :type => 'MIT' }
  s.author       = { 'KuiklyUI' => '' }
  s.platform     = :ios, '14.1'
  s.source       = { :path => '.' }
  s.source_files = 'empty.m'
  s.preserve_paths = [
    'libclang_rt.profile_ios.a',
    'libclang_rt.profile_iossim.a',
  ]
  # path pod 不会拷贝到 Pods/LLVMCompileRT；宿主 SRCROOT=iosApp。
  # 必须带 $(inherited)，否则会覆盖 CocoaPods 注入的 -l/-framework 导致大量 undefined。
  s.user_target_xcconfig = {
    'OTHER_LDFLAGS[sdk=iphoneos*]' => '$(inherited) -force_load $(SRCROOT)/components/LLVMCompileRT/libclang_rt.profile_ios.a',
    'OTHER_LDFLAGS[sdk=iphonesimulator*]' => '$(inherited) -force_load $(SRCROOT)/components/LLVMCompileRT/libclang_rt.profile_iossim.a',
  }
end
