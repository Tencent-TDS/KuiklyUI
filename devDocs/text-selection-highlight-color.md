# TODO：鸿蒙端富文本选区高亮色未对接 selectionColor

> 临时备忘。相关上下文见 #1793（iOS / Android 已修，鸿蒙未修）。
> 触发时机：鸿蒙端需要支持自定义选区高亮色时。

## 现状

上层 `selectionColor` 属性（Kotlin 侧 `DivView.selectionColor`）序列化为：

```json
{"background": 0x66RRGGBB, "cursor": 0xFFRRGGBB}
```

三端落地情况：

| 端 | 取值处 | 是否生效 |
|----|--------|----------|
| Android | 容器 `KRView` 解析 → `KRTextSelector.selectionPaint`，由 `KRRichTextView` 绘制时使用 | 生效 |
| iOS | 容器 `KRView+TextSelection` 解析 → `KRTextSelectionHelper.selectionColor` 下发到每个 `KRLabel` | 生效 |
| 鸿蒙 | 无链路，`KRRichTextView.cpp` 选区画笔写死 `0x33007DFF` | 不生效 |

## 问题

`expand/components/richtext/KRRichTextView.cpp` 绘制选区底色：

```cpp
OH_Drawing_BrushSetColor(backgroundBrush, 0x33007DFF);  // 硬编码
```

选区矩形 `selection_rects_` 由 `KRRichTextView` 自己持有，但属性落在**容器** `KRView` 上，两者之间没有色值通路：

- `selectable` 属性与 `selectable_option_` 都在容器 `KRView`（`expand/components/view/KRView.cpp`）
- 触发选区时 `KRView.cpp` 调 `UpdateSelection(ancestor_view, p0, p1, type)`，经 `export/IKRRenderViewExport.cpp` 递归下发到子视图
- `UpdateSelection` 的签名不带颜色，`KRRichTextView` 拿到选区后无从得知容器配置的颜色

## 建议改法

沿已有输入控件的解析范式接：

1. 容器 `KRView` 解析 `selectionColor`（JSON 取 `background`），用 `kuikly::util::ClampSelectionColorAlpha`（`utils/KRViewUtil.h`，上限 `kSelectionColorMaxAlpha = 0x66`，与 Android / iOS 一致）落成 `uint32_t`；解析分支可参照 `expand/components/input/KRTextEditorFieldView.cpp` 的 `kSelectionColor`
2. 打通容器 → 子 `KRRichTextView` 的通路：给 `IKRRenderViewExport` 增加选区色 setter（默认空实现，`KRRichTextView` 覆写），在 `UpdateSelection` 递归时逐个下发；或把色值并入 `UpdateSelection` 参数
3. `KRRichTextView` 保存 `selection_color_`，绘制时替换硬编码值，未配置时回退 `0x33007DFF`

第 2 步会动到 `export/IKRRenderViewExport.h` 这个共享基类，影响面需评估。

## 改动锚点

- `expand/components/richtext/KRRichTextView.cpp`：选区画笔颜色
- `expand/components/richtext/KRRichTextView.h`：`selection_color_` 成员
- `expand/components/view/KRView.cpp`：`selectionColor` 属性解析与下发
- `export/IKRRenderViewExport.h` / `.cpp`：容器到子视图的色值通路

## 验证建议

- 鸿蒙真机 / 模拟器：demo `TextSelectionExamplePage` 长按选中，选区应显示配置色而非华为系统蓝
- 回归：未配置 `selectionColor` 时仍为 `0x33007DFF`
- 三端对齐：同一色值下选区视觉一致（文字应在选区上层，见 #1793 的 Android 修复）

## 决议

- Android / iOS 已在 #1793 修复
- 鸿蒙需要改跨组件接口，且当前开发环境无 HarmonyOS NDK，无法本地编译与真机验证，留待有鸿蒙构建环境时处理
