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

## Swift 兼容性说明

Kuikly 的 iOS 侧是基于 Objective-C 运行时的，但可以兼容 Swift 类文件。只需要在 Swift 类上添加 `@objc` 和 `@objcMembers` 注解向 Objective-C 运行时披露该类即可。

### 示例代码

```swift
import UIKit

@objc
@objcMembers
class MySwiftViewController: NSObject {
    
    // ...
}
```
。
