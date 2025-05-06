
<p align="center">
    <img alt="Kuikly Logo"  src="img/kuikly_logo.svg" />
</p>

English | [简体中文](./README-zh_CN.md) | [Homepage](https://framework.tds.qq.com/)


## Introduction
`Kuikly` is a comprehensive cross-platform solution for UI and logic based on Kotlin multi-platform. It was launched by Tencent's company-level Oteam in the front-end field. It aims to provide a `high-performance, full-platform development framework with unified codebase, ultimate ease of use, and dynamic flexibility`. Currently supported platforms:
- [X] Android
- [X] iOS
- [ ] HarmonyOS(Open-source in May)
- [ ] Web (Open-source in Q2)
- [ ] Mini Programs (Open-source in Q2)

Since its launch, `Kuikly` has gained wide recognition from the business. It has been used by many products such as QQ, QQ Music, QQ Browser, Tencent News, Sogou Input Method, MyApp Hub(Tencent's app store), WeSing, Kugou Music, Kuwo Music, Tencent Self-selected Stock, ima.copilot, Weishi, etc.
## Key Features

- **Cross-platform:** Kotlin-based implementation ensuring consistent operation across multiple platforms - one codebase, five platforms
- **Native performance:** Generates platform-native binaries (.aar/.framework)
- **Native development experience:** Native UI rendering, native toolchain support, Kotlin as primary language
- **Lightweight:** Minimal SDK footprint (AOT mode: ~300KB for Android, ~1.2MB for iOS)
- **Dynamic capability:** Supports compilation into dynamic deliverables
- **Multiple paradigms:** Supports both declarative & reactive programming, with self-developed DSL and Compose DSL (under development).

## Project Structure

```shell
.
├── core                    # Cross-platform module implementing core capabilities like responsive UI, layout algorithms, Bridge communication, etc.
  ├── src
    ├──	commonMain            # Shared cross-platform code, defining cross-platform interfaces
    ├── androidMain           # Android platform implementation (outputs aar)
    ├── jvmMain               # Generic JVM platform code (no Android APIs, outputs jar)
    ├── iosMain               # iOS platform implementation (outputs framework)
├── core-render-android    # Android platform renderer module
├── core-render-ios        # iOS platform renderer module
├── core-annotations       # Annotations module, defining business annotations like @Page
├── core-ksp               # Annotation processing module, generates Core entry files
├── buildSrc               # Build scripts for compilation, packaging, and artifact splitting
├── demo                   # DSL example code
├── androidApp             # Android host shell project
└── iosApp                 # iOS host shell project
```
## System Requirements
- iOS 12.0+
- Android 5.0+
- HarmonyOS Next 5.0.0(12)+
- Kotlin 1.3.10+

## Getting Started

- [Quick Start](https://kuikly.tds.qq.com/%E5%BF%AB%E9%80%9F%E5%BC%80%E5%A7%8B/hello-world.html)
- [Integration Guide](https://kuikly.tds.qq.com/%E5%BF%AB%E9%80%9F%E5%BC%80%E5%A7%8B/overview.html)
- [Component Features](https://kuikly.tds.qq.com/API/%E7%BB%84%E4%BB%B6/override.html)

## Building from Source
### Environment Setup
Refer to [Environment Configuration](https://kuikly.tds.qq.com/%E5%BF%AB%E9%80%9F%E5%BC%80%E5%A7%8B/env-setup.html)
- [Android Studio](https://developer.android.com/studio)

    if your Android Studio Version >= (2024.2.1) Please switch your Gradle JDK Version to JDK17
    (this Version default Gradle JDK is 21, it incompatible with the configuration used by the project)

    Android Studio -> Settings -> Build,Execution,Deployment -> Build Tools -> Gradle -> Gradle JDK
- [XCode](https://developer.apple.com/xcode/) and [cocoapods](https://cocoapods.org/)
- JDK17

### Running Android App
Ensure environment preparation is complete before building:
1. Open `KuiklyUI` root directory in `Android Studio` and sync project
2. Select androidApp configuration, then Run 'androidApp'

### Running iOS App
Ensure environment preparation is complete before building:
1. Navigate to `iosApp` directory
2. Execute `pod install --repo-update`
3. Open `KuiklyUI` root directory in Android Studio and sync project
4. Select iOSApp configuration, then Run 'iOSApp'

   Alternatively, open KuiklyUI/iosApp in Xcode and Run

### Kotlin Version Support
The KuiklyUI directory contains Gradle configurations for various `Kotlin versions`:

Naming convention: `x.x.xx.gradle.kts` (default uses Kotlin 1.7.20)

Test publishing scripts for each version are available as `x.x.xx_test_publish.sh` for building local artifacts.

> Note: Kotlin 1.3.10/1.4.20 require JDK11

After successful build on any platform, you can modify Core, Render, and Demo to experience `Kuikly` development.


## Roadmap
[Roadmap (2025)](https://kuikly.tds.qq.com/%E5%8D%9A%E5%AE%A2/roadmap2025.html)

## Contribution Guidelines
We welcome all developers to submit issues or PRs for `Kuikly`. Please review our [Contribution Guide](CONTRIBUTING.md) before contributing.

## Code of Conduct
All project participants are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). Participation constitutes agreement to these terms.

## FAQs
[`Kuikly` Q&A](https://kuikly.tds.qq.com/QA/kuikly-qa.html)

## Contributors
- Special thanks to the first batch of contributors tom(邱良雄), kam(林锦涛), and watson(金盎), who not only pioneered the incubation and exploration of the Kuikly cross-platform solutions in the frontend field, but also were the first to implement them in the QQ business.
- Thanks to the following core contributors for the continuous construction, maintenance, development and optimization of `Kuikly`:
 <br>tom kam watson rocky jonas ruifan pel layen bird zealot zhenhua vinney xuanxi arnon alexa allens eason

## Stay Connected
Scan the QR codes below to follow our latest updates or contact us for inquiries.
<p align="left">
    <div style="display: inline-block; text-align: center; margin-right: 20px;">
        <div>TDS WeChat Official Account</div>
        <img alt="TDS" src="img/tds_qrcode.jpg" width="200" />
    </div>
    <div style="display: inline-block; text-align: center; margin-right: 20px;">
        <div>TDS Framework WeChat Official Account</div>
        <img alt="TDS Framework WeChat Official Account" src="img/tds_framework_qrcode.jpg" width="200" />
    </div>
    <div style="display: inline-block; text-align: center;">
        <div>Online Support</div>
        <img alt="Online Consult" src="img/consult_qrcode.png" width="200" />
    </div>
</p>