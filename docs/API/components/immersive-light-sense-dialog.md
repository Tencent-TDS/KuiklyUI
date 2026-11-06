# ImmersiveLightSenseDialogOhos(沉浸光感弹窗)

:::tip 系统和版本要求

- **系统版本**：HarmonyOS 6.1.0+

- **Kuikly版本**：2.6.0+

:::

沉浸光感弹窗：**内容即 Kuikly 子视图**，背板（尺寸/圆角/底色）由属性映射，背板材质（玻璃）由 `immersiveMaterial()` 开启。

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/ImmersiveLightSenseDialogDemoPage.kt)

## 基础用法

```kotlin
ImmersiveLightSenseDialogOhos {
    attr {
        width(300f); height(220f)
        borderRadius(24f)
        backgroundColor(Color(0x22000000))          // 可选：材质层之上叠底色
        immersiveMaterial(style = ImmersiveStyle.REGULAR, applyShadow = true)
    }
    event {
        onDismiss { /* 关闭回调 */ }
        willDismiss { /* 即将关闭（可拦截） */ }
    }
    // 内容根：flex 撑满，不写死尺寸
    View {
        attr { flex(1f); alignSelfStretch() }
        Text { attr { text("弹窗内容") } }            // 文字用深色（材质不自动反色）
    }
}
```

## 属性映射

| 通用属性 | 映射到 |
| -- | -- |
| `width` / `height` | 弹窗背板尺寸 |
| `borderRadius` | 背板圆角 |
| `backgroundColor` | 背板底色（叠加在材质层之上） |
| `immersiveMaterial` | 背板沉浸光感材质，见[总览](immersive-light-sense.md#immersivematerial) |

## 事件

| 事件 | 说明 |
| -- | -- |
| onDismiss | 弹窗关闭 |
| willDismiss | 即将关闭（点外部/返回键，可拦截） |

## 注意事项

- **border（描边）无效**：背板描边归材质管，官方未开放；需要描边在内容层自绘（`flex(1f) + alignSelfStretch() + borderRadius + border`）；
- 内容文字颜色需业务按背景自选（材质不自动反色）；
- 应用级 MaterialState 为 `DISABLE` 时所有材质不生效，弹窗回退普通样式。
