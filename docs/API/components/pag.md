# PAG(动画播放)

类Lottie播放动画的组件

[组件用法](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/PAGViewDemoPage.kt)

:::tip 需要在宿主端实现adapter

可参考：

- **安卓**：[PAGViewAdapter.kt](https://github.com/Tencent-TDS/KuiklyUI/blob/main/androidApp/src/main/java/com/tencent/kuikly/android/demo/adapter/PAGViewAdapter.kt)

- **iOS**：如果依赖了pod 'libpag'，无须额外实现。

- **鸿蒙**：[AppKRPAGAdapter.ets](https://github.com/Tencent-TDS/KuiklyUI/blob/main/ohosApp/entry/src/main/ets/kuikly/adapters/AppKRPAGAdapter.ets)

:::
    
## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)，此外还支持：

### src <Badge text="微信小程序实现中" type="warn"/>

设置`PAGView`的源文件路径，支持URL或本地文件路径

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| src | 源文件路径 | String |

### repeatCount <Badge text="微信小程序实现中" type="warn"/>

设置动画重复次数，默认值为 1，表示动画仅播放一次。0 表示动画将无限次播放

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| repeatCount | 动画重复次数 | Int |

### scaleMode <Badge text="微信小程序实现中" type="warn"/>

设置 PAG 内容在视图中的缩放模式，对齐 libpag 的 `PAGScaleMode`，控制动画如何适配容器尺寸。

可选值如下（括号为底层对应的数值，来自 libpag，便于在自定义 adapter 中透传）：

- **NONE (0)**: 不缩放，使用内容原始大小，容器剩余区域将留空。
- **STRETCH (1)**: 拉伸填充容器，不保持宽高比，可能产生拉伸变形。
- **LETTER_BOX (2)**: 等比缩放以完整显示内容，保持宽高比，可能在两侧留黑边（默认）。
- **ZOOM (3)**: 等比缩放以填满容器，保持宽高比，但内容可能被裁剪。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| scaleMode | 缩放模式 | PAGScaleMode |

### autoPlay <Badge text="微信小程序实现中" type="warn"/>

设置是否自动播放，默认值为 true

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| autoPlay | 是否自动播放 | Boolean |

### replaceTextLayerContent <Badge text="微信小程序实现中" type="warn"/>

替换当前 PAG 资源中的文字图层信息，适用于已知目标文字图层名称的场景。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| layerName | 目标图层名称 | String |
| textContent | 替换的文本内容 | String |

### replaceImageLayerContent <Badge text="微信小程序实现中" type="warn"/>

替换当前 PAG 资源中的图像图层信息，适用于已知目标图片图层名称的场景。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| layerName | 目标图层名称 | String |
| imageFilePath | 替换的图片资源文件路径（与 uri 二选一） | String |
| uri | 替换的图片 Assets 资源 uri（与 imageFilePath 二选一） | ImageUri |

### replaceTextByIndex <Badge text="微信小程序实现中" type="warn"/>

根据 `editableIndex` 替换当前 PAG 资源中的可编辑文字图层内容，适用于图层名称不稳定、但可编辑索引固定的场景。

`editableIndex` 的有效范围为 `0` 到 `PAGFile.numTexts - 1`。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| editableIndex | 可编辑文字图层索引 | Int |
| textContent | 替换的文本内容 | String |

### replaceImageByIndex <Badge text="微信小程序实现中" type="warn"/>

根据 `editableIndex` 替换当前 PAG 资源中的可编辑图片图层内容，适用于图层名称不稳定、但可编辑索引固定的场景。

`editableIndex` 的有效范围为 `0` 到 `PAGFile.numImages - 1`。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| editableIndex | 可编辑图片图层索引 | Int |
| imageFilePath | 替换的图片资源文件路径（与 uri 二选一） | String |
| uri | 替换的图片 Assets 资源 uri（与 imageFilePath 二选一） | ImageUri |

## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)，此外还支持：

### loadFailure <Badge text="微信小程序实现中" type="warn"/>

设置加载失败时的事件回调

### animationStart <Badge text="微信小程序实现中" type="warn"/>

设置动画开始时的事件回调

### animationEnd <Badge text="微信小程序实现中" type="warn"/>

设置动画结束时的事件回调

### animationCancel <Badge text="微信小程序实现中" type="warn"/>

设置动画取消时的事件回调

### animationRepeat <Badge text="微信小程序实现中" type="warn"/>

设置动画重复时的事件回调

### click

`PAGView` 的 `click` 事件的 ClickParams 中已支持返回当前点击位置命中的可编辑图层。

`PAGView` 的 click payload 顶层仍遵循基础 click 事件协议，其中 `layers` 字段在 Android / iOS / HarmonyOS 三端统一为 **JSON String**，表示命中的可编辑图层数组。

Kotlin 侧推荐按以下两步解析：

1. 先把顶层 payload 解析为 `JSONObject`
2. 再通过 `JSONArray(json.optString("layers", "[]"))` 解析图层数组

如果你要做“点击 PAG 内可编辑文字或图片后立即替换”，推荐链路是：`click -> 解析 layers -> 读取 editableType 与 editableIndex -> 写入 observable -> 在 attr 中调用 replaceTextByIndex / replaceImageByIndex`。

当前每个命中图层对象都包含 `layerName`、`editableIndex` 和 `editableType`。其中 `editableType` 只会是 `text` 或 `image`，推荐业务优先按 `editableType + editableIndex` 处理；`layerName` 仅作为调试、日志和兼容字段保留。具体示例见 [PAGApiTestPage.kt](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/PAGApiTestPage.kt)。

当点击未命中任何可编辑图层时，`layers` 返回 `"[]"`。

## 方法

### play <Badge text="微信小程序实现中" type="warn"/>

播放动画（`autoPlay` 属性为 true 时不需要手动调用该接口）。

### stop <Badge text="微信小程序实现中" type="warn"/>

停止动画

### setProgress <Badge text="微信小程序实现中" type="warn"/>

设置动画播放进度

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| value | 播放进度，有效值为 0.0 到 1.0 | Float |
