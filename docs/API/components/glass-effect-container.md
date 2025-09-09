# GlassEffectContainer(液态玻璃容器组合)

:::tip 系统要求
iOS 26.0+ 支持。在低版本系统上会自动降级到普通容器效果。
:::

`GlassEffectContainer` 是专门用于组织和管理多个液态玻璃元素的容器组件，用于实现多个液态玻璃元素相互靠近时的融合效果。

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/LiquidGlassDemoPage.kt)

## 基础用法

```kotlin
import com.tencent.kuikly.core.views.GlassEffectContainer
import com.tencent.kuikly.core.views.LiquidGlass

GlassEffectContainer {
    attr {
        spacing(15f)
        flexDirectionRow()
    }
    
    LiquidGlass {
        attr {
            flex(1f)
            height(60f)
            borderRadius(30f)
        }
        Text {
            attr {
                text("左侧")
                alignSelfCenter()
            }
        }
    }
    
    LiquidGlass {
        attr {
            flex(1f)
            height(60f)
            borderRadius(30f)
        }
        Text {
            attr {
                text("右侧")
                alignSelfCenter()
            }
        }
    }
}
```

## 属性

`GlassEffectContainer` 支持所有[基础属性](basic-attr-event.md#基础属性)，还支持以下专有属性：

### spacing

设置子元素之间的间距。

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| spacing | 间距大小（单位：点） | Float | 0f |

**示例**

```kotlin
GlassEffectContainer {
    attr {
        spacing(20f) // 子元素间距20点
    }
    // 子组件...
}
```

:::tip 注意
spacing 值必须为非负数，否则会抛出异常。
:::

## 事件

`GlassEffectContainer` 支持所有[基础事件](basic-attr-event.md#基础事件)。

## 相关组件

- [LiquidGlass](./liquid-glass.md) - 液态玻璃容器
- [View](./view.md#glasseffectios) - 基础容器（支持glassEffectIOS属性）
- [iOS 液态玻璃概述](./ios26-liquid-glass.md) - 完整使用指南
