# macOS工程接入

:::warning Alpha版本说明
macOS平台目前处于Alpha阶段，API可能会有变动，建议充分验证后使用。
:::

:::tip 注意
在此之前请确保已经完成**KMP侧 Kuikly**的接入，如还未完成，请移步[KMP跨端工程接入](./common.md)
:::

macOS平台的接入方式与iOS基本一致，Kuikly复用了iOS渲染器（`OpenKuiklyIOSRender`）来支持macOS平台。

## 接入说明

macOS接入流程与iOS相同，请参考[iOS工程接入](./iOS.md)文档完成以下步骤：

1. **添加Kuikly macOS渲染器依赖** - 通过CocoaPods或SPM集成`OpenKuiklyIOSRender`
2. **实现Kuikly承载容器** - 创建`KuiklyRenderViewController`作为页面容器
3. **实现Kuikly适配器** - 包括图片加载适配器、页面路由适配器等
4. **链接Kuikly业务代码** - 集成编译好的`.framework`产物

## 与iOS的差异

### 平台配置

在Podfile中需要将平台设置为macOS：

```ruby
platform :osx, '10.13'
```

### 系统要求

- macOS 10.13+
- Xcode 14.0+

## 示例工程

可参考源码工程中的`macApp`模块，了解macOS平台的完整接入示例。

## 已知限制

由于macOS平台处于Alpha阶段，部分iOS组件在macOS上可能存在兼容性问题，如遇问题请提交Issue反馈。

