# Blur(高斯模糊)

高斯模糊（毛玻璃）组件，盖住其他view可进行动态高斯模糊布局位置下方的视图

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/kit_demo/DeclarativeDemo/BlurExamplePage.kt)

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)，此外还支持：

### blurRadius

高斯模糊半径，最大为12.5f（默认：10f）

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| radius | 高斯模糊半径 | Float |

### targetBlurViewNativeRefs

想要模糊的View的nativeRef列表，可提高在Android平台上的模糊性能，**建议设置**。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| refs | 想要模糊的View的nativeRef列表 | `List<Int>` |

```kotlin
@Page("demo_page")
internal class TestPage : BasePager() {
    private lateinit var imageRef: ViewRef<ImageView>
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    size(200f, 100f)
                }
                // 背景图
                Image {
                    ref { ref -> ctx.imageRef = ref }
                    attr {
                        absolutePositionAllZero()
                        src("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png")
                    }
                }
                Blur {
                    attr {
                        height(50f)
                        blurRadius(3f)
                        // 指定要模糊的View的nativeRef，提高模糊性能
                        targetBlurViewNativeRefs(listOf(ctx.imageRef.nativeRef))
                    }
                }
            }
        }
    }
}
```

### blurOtherLayer

是否模糊其他单独的layer。目前只有 Android 使用到，用于开启模糊 TextureView。

**注意**: 如果设置了 `targetBlurViewNativeRefs` 属性的话，此属性无效。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| blur | 是否模糊其他单独的layer | Boolean |

:::tabs

@tab:active 示例

```kotlin{15-27}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }
            Image {
                attr {
                    absolutePosition(0f,0f,0f,0f)
                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                    src("https://picsum.photos/id/221/1500/2500")
                }
            }
            Blur {
                attr {
                    size(pagerData.pageViewWidth, 100f)
                    blurRadius(10f)
                }
            }
            Blur {
                attr {
                    marginTop(100f)
                    size(pagerData.pageViewWidth, 100f)
                    blurRadius(1f)
                }
            }
        }
    }
}
```

@tab 效果

<div align="center">
<img src="./img/blur.png" style="width: 30%; border: 1px gray solid">
</div>

:::

## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)