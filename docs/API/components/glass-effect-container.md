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

## 使用场景

### 1. 水平排列的按钮组

```kotlin
GlassEffectContainer {
    attr {
        flexDirectionRow()
        spacing(10f)
        justifyContentCenter()
        alignItemsCenter()
    }
    
    LiquidGlass {
        attr {
            size(80f, 60f)
            borderRadius(30f)
            justifyContentCenter()
            glassEffectInteractive(true)
        }
        event {
            click { /* 处理点击 */ }
        }
        Text {
            attr {
                text("确定")
                alignSelfCenter()
            }
        }
    }
    
    LiquidGlass {
        attr {
            size(80f, 60f)
            borderRadius(30f)
            justifyContentCenter()
            glassEffectInteractive(true)
        }
        event {
            click { /* 处理点击 */ }
        }
        Text {
            attr {
                text("取消")
                alignSelfCenter()
            }
        }
    }
}
```

### 2. 垂直排列的卡片组

```kotlin
GlassEffectContainer {
    attr {
        flexDirectionColumn()
        spacing(15f)
        padding(20f)
    }
    
    LiquidGlass {
        attr {
            height(100f)
            borderRadius(15f)
            padding(15f)
        }
        Text {
            attr {
                text("卡片1")
                fontSize(18f)
                fontWeight600()
            }
        }
    }
    
    LiquidGlass {
        attr {
            height(100f)
            borderRadius(15f)
            padding(15f)
        }
        Text {
            attr {
                text("卡片2")
                fontSize(18f)
                fontWeight600()
            }
        }
    }
}
```

### 3. 工具栏组合

```kotlin
GlassEffectContainer {
    attr {
        flexDirectionRow()
        spacing(8f)
        alignItemsCenter()
        justifyContentSpaceBetween()
        padding(10f)
    }
    
    LiquidGlass {
        attr {
            size(60f, 60f)
            borderRadius(30f)
            justifyContentCenter()
            glassEffectInteractive(true)
        }
        Text {
            attr {
                text("←")
                fontSize(24f)
                alignSelfCenter()
            }
        }
    }
    
    LiquidGlass {
        attr {
            flex(1f)
            height(60f)
            borderRadius(30f)
            justifyContentCenter()
            marginHorizontal(10f)
        }
        Text {
            attr {
                text("搜索框")
                alignSelfCenter()
            }
        }
    }
    
    LiquidGlass {
        attr {
            size(60f, 60f)
            borderRadius(30f)
            justifyContentCenter()
            glassEffectInteractive(true)
        }
        Text {
            attr {
                text("⋯")
                fontSize(24f)
                alignSelfCenter()
            }
        }
    }
}
```

### 4. 不同色调的组合效果

```kotlin
GlassEffectContainer {
    attr {
        flexDirectionRow()
        spacing(12f)
        alignItemsCenter()
    }
    
    LiquidGlass {
        attr {
            size(80f, 80f)
            borderRadius(40f)
            justifyContentCenter()
            glassEffectTintColor(Color.RED)
        }
        Text {
            attr {
                text("红")
                alignSelfCenter()
            }
        }
    }
    
    LiquidGlass {
        attr {
            size(80f, 80f)
            borderRadius(40f)
            justifyContentCenter()
            glassEffectTintColor(Color.GREEN)
        }
        Text {
            attr {
                text("绿")
                alignSelfCenter()
            }
        }
    }
    
    LiquidGlass {
        attr {
            size(80f, 80f)
            borderRadius(40f)
            justifyContentCenter()
            glassEffectTintColor(Color.BLUE)
        }
        Text {
            attr {
                text("蓝")
                alignSelfCenter()
            }
        }
    }
}
```

## 布局特性

### Flexbox 支持

`GlassEffectContainer` 完全支持 Flexbox 布局模式：

```kotlin
GlassEffectContainer {
    attr {
        flexDirectionRow() // 或 flexDirectionColumn()
        justifyContentSpaceBetween()
        alignItemsCenter()
        flexWrap()
        spacing(10f)
    }
    // 子组件会根据Flexbox规则排列
}
```

### 响应式布局

结合 `flex` 属性创建响应式布局：

```kotlin
GlassEffectContainer {
    attr {
        flexDirectionRow()
        spacing(15f)
    }
    
    LiquidGlass {
        attr {
            flex(2f) // 占2份空间
            height(60f)
        }
        // 内容...
    }
    
    LiquidGlass {
        attr {
            flex(1f) // 占1份空间
            height(60f)
        }
        // 内容...
    }
}
```

## 性能优化

### 1. 合理控制子元素数量

```kotlin
// ✅ 推荐：适量子元素
GlassEffectContainer {
    attr {
        spacing(10f)
    }
    // 3-5个LiquidGlass子组件
}

// ❌ 避免：过多子元素影响性能
GlassEffectContainer {
    // 超过10个LiquidGlass子组件
}
```

### 2. 避免嵌套过深

```kotlin
// ✅ 推荐：扁平结构
GlassEffectContainer {
    LiquidGlass { /* 内容 */ }
    LiquidGlass { /* 内容 */ }
}

// ❌ 避免：过深嵌套
GlassEffectContainer {
    GlassEffectContainer {
        GlassEffectContainer {
            // 嵌套过深
        }
    }
}
```

## 兼容性处理

```kotlin
import com.tencent.kuikly.core.utils.PlatformUtils

if (PlatformUtils.isLiquidGlassSupported()) {
    GlassEffectContainer {
        attr {
            spacing(15f)
        }
        // 液态玻璃实现
    }
} else {
    View {
        attr {
            flexDirectionRow() // 保持相同布局
        }
        // 使用普通View作为降级方案
        View {
            attr {
                backgroundColor(Color.GRAY)
                margin(7.5f) // spacing/2 作为margin
            }
        }
        View {
            attr {
                backgroundColor(Color.GRAY)
                margin(7.5f)
            }
        }
    }
}
```

## 相关组件

- [LiquidGlass](./liquid-glass.md) - 液态玻璃容器
- [View](./view.md#glasseffectios) - 基础容器（支持glassEffectIOS属性）
- [iOS 26 液态玻璃概述](./ios26-liquid-glass.md) - 完整使用指南

## 设计建议

1. **间距统一性**：在同一个界面中使用统一的spacing值，保持视觉一致性
2. **色彩搭配**：合理使用 `glassEffectTintColor` 创建层次感，避免色彩冲突
3. **交互反馈**：为需要交互的子元素启用 `glassEffectInteractive`
4. **圆角协调**：子元素的 `borderRadius` 应与整体设计风格协调
5. **内容可读性**：确保液态玻璃效果不影响文本和图标的可读性