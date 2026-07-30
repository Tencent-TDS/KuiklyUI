# Button(按钮)

按钮

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/kit_demo/DeclarativeDemo/ButtonExamplePage.kt)


## 属性

除了支持所有[基础属性](basic-attr-event.md#基础属性)，还支持以下属性：

### titleAttr方法

设置文本属性

<div class="table-01">

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| init | 文本属性 | TextAttr |

</div>

### imageAttr方法

设置图片属性

<div class="table-01">

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| init | 图片属性 | ImageAttr |

</div>

### highlightBackgroundColor方法

设置按下态颜色

<div class="table-01">

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| color | 颜色 | Color |

</div>

### flexDirection方法

设置图标和文本的排列方向

<div class="table-01">

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| flexDirection | 排列方向 | enum |

</div>

### glassEffectIOS

:::tip 系统要求
iOS 26.0+ 支持。在低版本系统上会自动降级到普通背景效果。
:::

为Button组件添加iOS 26+液态玻璃效果。

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| enable | 是否启用液态玻璃效果 | Boolean | true |
| interactive | 是否启用交互效果 | Boolean | true |
| tintColor | 玻璃效果色调颜色 | Color | 系统默认 |
| style | 玻璃效果样式（REGULAR/CLEAR） | GlassEffectStyle | REGULAR |

**GlassEffectStyle 枚举值：**
- `REGULAR` - 标准液态玻璃效果
- `CLEAR` - 清透液态玻璃效果

**示例：**
```kotlin
Button {
    attr {
        glassEffectIOS() // 默认效果
        // 或自定义效果
        glassEffectIOS(
            style = GlassEffectStyle.CLEAR,
            tintColor = Color.BLUE,
            interactive = true
        )
    }
    titleAttr {
        text("液态玻璃按钮")
    }
}
```

:::warning 注意
使用glassEffectIOS时，不能同时设置backgroundColor，否则会覆盖玻璃效果。
:::

### glassEffectContainerIOS

:::tip 系统要求
iOS 26.0+ 支持。在低版本系统上会自动降级到普通容器效果。
:::

为Button组件添加液态玻璃容器效果，用于创建多个液态玻璃元素之间的融合效果。

| 参数 | 描述 | 类型 | 默认值 |
| -- | -- | -- | -- |
| spacing | 子元素之间的间距 | Float | 0f |

**示例：**
```kotlin
View {
    attr {
        glassEffectContainerIOS(10f) // 10点间距
        flexDirectionRow()
    }
    
    Button {
        attr {
            flex(1f)
            glassEffectIOS()
            backgroundColor(Color.TRANSPARENT)
        }
        titleAttr {
            text("左按钮")
        }
    }
    
    Button {
        attr {
            flex(1f)
            glassEffectIOS()
            backgroundColor(Color.TRANSPARENT)
        }
        titleAttr {
            text("右按钮")
        }
    }
}
```

:::tip 兼容性处理
```kotlin
Button {
    attr {
        if (PlatformUtils.isLiquidGlassSupported()) {
            glassEffectIOS()
            backgroundColor(Color.TRANSPARENT)
        } else {
            backgroundColor(Color(0x80000000)) // 半透明黑色降级
        }
    }
    titleAttr {
        text("自适应按钮")
    }
}
```
:::

## 事件

支持所有[基础属性](basic-attr-event.md#基础属性)