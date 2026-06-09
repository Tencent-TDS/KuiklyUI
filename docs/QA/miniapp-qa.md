# 微信小程序接入常见问题

## PageList 嵌套 WaterfallList 不显示

**现象**：PageList 中嵌套 WaterfallList，数据正常（scroll-view 的 cn 中包含 custom-wrapper 节点），但瀑布流列表区域空白，无任何内容渲染。

**根本原因**：`custom-wrapper.json` 中的 `usingComponents` 为空 `{}`，导致小程序框架无法识别嵌套的 `<custom-wrapper>` 组件。由于组件未在自身的 `usingComponents` 中声明自引用，`tmpl_3_custom-wrapper` 模板渲染嵌套标签时无法实例化该组件，`attached` 生命周期不触发，子组件的 `setData` 永远无法执行。

**对比**：

| 工程 | custom-wrapper.json | 结果 |
|------|---------------------|------|
| 正常工程 | `"usingComponents":{"comp":"./comp","custom-wrapper":"./custom-wrapper"}` | ✅ 正常 |
| 问题工程 | `"usingComponents":{}` | ❌ 不显示 |

**修复**：将 `custom-wrapper.json` 的 `usingComponents` 改为包含 `comp` 和 `custom-wrapper` 的自引用声明：

```json
{
  "usingComponents": {
    "comp": "./comp",
    "custom-wrapper": "./custom-wrapper"
  }
}
```
