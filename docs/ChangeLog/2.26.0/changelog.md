# 2.26.0 变更汇总

> 本版导航：[版本说明](./announcement.md) · **变更汇总** · [破坏性与迁移](./breaking.md)

## 多端

- feat(animation): add opt-in Compose native property animations · [#1633](https://github.com/Tencent-TDS/KuiklyUI/pull/1633)
- fix(core): sliderpager scroll to last item offset overflow problem · [#1681](https://github.com/Tencent-TDS/KuiklyUI/pull/1681)
- fix(core): sliderpager defaultPage not work
- fix(compose): realign lazy content after Android background expansion · [#1642](https://github.com/Tencent-TDS/KuiklyUI/pull/1642)
- fix(compose): stabilize pager scroll session state · [#1629](https://github.com/Tencent-TDS/KuiklyUI/pull/1629)
- feat(core): listView add initContentOffset API · [#1616](https://github.com/Tencent-TDS/KuiklyUI/pull/1616)

## 单端

### Android

- fix(Android): PAG ReplaceImageLayer Image src · [#1688](https://github.com/Tencent-TDS/KuiklyUI/pull/1688)
- feat: Android turboDisplay  pre-prepare · [#1679](https://github.com/Tencent-TDS/KuiklyUI/pull/1679)

### iOS

- fix(ios): resolve crash caused by broken tag chain in cache tree · [#1685](https://github.com/Tencent-TDS/KuiklyUI/pull/1685)
- fix(ios): resolve compilation errors in KRReflectionModule when integrating iOS Render via SPM in release builds · [#1622](https://github.com/Tencent-TDS/KuiklyUI/pull/1622)

### 鸿蒙

- feat(ohos): add ohos accessibility · [#1626](https://github.com/Tencent-TDS/KuiklyUI/pull/1626)
- fix(ohos): add statusCode to network response callback · [#1623](https://github.com/Tencent-TDS/KuiklyUI/pull/1623)

### H5

- fix(h5): make h5 toImage result clearer
- fix(h5): h5 toImage method support image and canvas embedded
- fix(web): setTouchEvent support touch screen windows
- fix(web): fix MINIPROGRAM type of detectDeviceType
- fix(h5App-js): fix cover image position for ImageSpan
- fix(h5App-js): setMultiLineStyle again when measure cache is hit
- fix(h5App-js): protect for serializationObject with null
- fix(h5App-js): add setImageProcessor
- fix(h5App-js): add jsValueToKotlin
- fix(h5App-js): use KuiklyRenderCallback prop for custom view

## 其他

### 文档

- docs(perf): supplement ohos size optimization guidelines · [#1614](https://github.com/Tencent-TDS/KuiklyUI/pull/1614)

### 工程与示例

- fix(h5): fix ToImageExamplePage
- fix(h5App-js): make font-family same for html and body to handle measure and show diff
- chore: update tbdisplay waterfall demo · [#1678](https://github.com/Tencent-TDS/KuiklyUI/pull/1678)
- feat(demo): MyModule Demo 命名优化，补齐 iOS/Android module 实现 · [#1637](https://github.com/Tencent-TDS/KuiklyUI/pull/1637)
- chore: update tbdisplay test demo · [#1617](https://github.com/Tencent-TDS/KuiklyUI/pull/1617)
