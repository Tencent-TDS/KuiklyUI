# iOS平台开发方式

## framework模式

1. 在你的iOS宿主工程中的podFile文件添加本地``Kuikly``存放业务代码的module路径，这里以shared为例

```ruby
...
pod 'shared', :path => '/Users/XXX/workspace/TestKuikly/shared' # 本地存放Kuikly业务代码工程路径
end
```

2. 执行以下命令安装依赖

```shell
pod install --repo-update
```

3. 最后先在Android Studio编写业务代码, 然后切换到Xcode中点击运行即可



## 开发语言选择
Kuikly iOS 支持业务使用 Objective-C 或 Swift 开发，例如开发自定义 Module 和 View。

若使用 Swift，需在类上添加 @objc 和 @objcMembers 注解，供 Kuikly iOS Render 识别并调用。


### 示例代码

```swift
import Foundation

@objc
@objcMembers
class KRMyLogModule: KRBaseModule {
    func log(_ content: String) {
        print("Log: \(content)")
    }
}
```
