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

### 必需：Kotlin/Native runtime 的线程进入顺序

若某个 Kotlin/Native framework 可能**先**由 Kuikly 的 Context 线程进入，请先在**主线程**调用它的任意一个导出入口，再让它进入 Context 线程。

Kotlin/Native 为 CFRunLoop 创建的 finalizer processor 假定主线程已注册 runtime。Kotlin 2.1.21 下，主线程未注册而 Context 线程先进入 runtime 时，主线程随后处理真实 finalizer 回调可发生 `SIGSEGV / EXC_BAD_ACCESS`（故障地址 `0xf8`）：

```text
RunLoopFinalizerProcessor<...>::process
kotlin::objc_support::RunLoopSource::perform
CFRunLoop source callback
```

这是 Kotlin/Native 运行时缺陷（上游 [KT-84331](https://youtrack.jetbrains.com/issue/KT-84331)），据 #1829 的模拟器矩阵，Kotlin 2.4.0 下 8/8 真实回调均正常进入并返回；本仓库当前发布流水线固定在 Kotlin 2.1.21（`publish/2.1.21_publish.sh`）。

自 2.23.3（#1566）起，`KuiklyRenderThreadManager` 的 sync / async 调用**都**投递到独立的 Context `NSThread`，主线程不再经由同步调用进入 runtime，因此该顺序要求对两者都成立。旧版本同步调用在主线程执行只是当时的实现巧合，应用不应依赖。

**注意避免死锁**：不要实现成「主线程同步等待 Context 线程」或「Context 线程同步等待主线程」。`KREnsureContextThreadStarted` 启动 Context 线程后会以 `dispatch_semaphore_wait(..., DISPATCH_TIME_FOREVER)` 等待就绪信号；若在该窗口内再从 Context 线程 `dispatch_sync` 回主线程，而主线程正阻塞于该信号，即会死锁。稳妥做法是在 Context 线程启动之前、直接在主线程上完成一次 runtime 进入。



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
