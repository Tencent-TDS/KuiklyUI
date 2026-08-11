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

### 可选：启用高刷新率

若宿主需要在支持 ProMotion 的 iPhone 上以系统允许的高刷新率运行 Kuikly Compose，请在**宿主 App**的 `Info.plist` 中添加：

```xml
<key>CADisableMinimumFrameDurationOnPhone</key>
<true/>
```

该配置由宿主 App 生效，Pod 内部无法代为设置。启用后，Kuikly 会跟随系统的动态刷新率（例如 60Hz 与 120Hz 之间切换）；不支持高刷的设备保持原有行为。



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
