# APNG(动画播放)

播放APNG图片动画的组件

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/APNGExamplePage.kt)

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)，此外还支持：

### src

设置`APNG`的源文件路径，支持URL或本地文件路径

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| src | 源文件路径 | String |

:::tabs

@tab:active 示例

```kotlin{8-18}
@Page("demo_page")
internal class TestPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                allCenter()
            }
            APNG {
                attr {
                    size(200f, 200f)
                    src("https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/5vcy152h.png?test=4")
                }
                event {
                    animationStart {
                        KLog.i("APNG", "start play animation")
                    }
                }
            }
        }
    }
}
```

@tab 效果

<div align="center">
<img src="./img/apng.gif" style="width: 30%; border: 1px gray solid">
</div>

:::

### repeatCount

设置动画重复次数，默认值为 0，表示动画将无限次播放

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| repeatCount | 动画重复次数 | Int |

### autoPlay

设置是否自动播放，默认值为 true

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| play | 是否自动播放 | Boolean |

## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)，此外还支持：

### loadFailure

设置加载失败时的事件回调

### animationStart

设置动画开始时的事件回调

### animationEnd

设置动画结束时的事件回调

## 适配器

### iOS

在开源版本中，移除了`APNG`组件对第三方库`QQAPNGView`的集成适配，需要业务自行实现APNG适配器。

#### 重新实现APNG适配器

对于之前直接pod安装`QQAPNGView`第三方库使用`APNG`组件的业务，需要实现并注册`APNGImageViewProtocol`，比如：

```objc
// KRAPNGViewHandler.h
#import <SDWebImage/SDWebImage.h>
#import "KRAPNGView.h"

@interface KRAPNGViewHandler : SDAnimatedImageView<APNGImageViewProtocol>

@end
```

```objc
// KRAPNGViewHandler.m
#import "KRAPNGViewHandler.h"

@implementation KRAPNGViewHandler

+ (void)load {
    [KRAPNGView registerAPNGViewCreator:^id<APNGImageViewProtocol> _Nonnull(CGRect frame) {
        KRAPNGViewHandler *apngView = [[KRAPNGViewHandler alloc] initWithFrame:frame];
        return apngView;
    }];
}
...
```

详细使用示例可参考[KRAPNGViewHandler](https://github.com/Tencent-TDS/KuiklyUI/blob/main/iosApp/iosApp/KuiklyRenderExpand/Handlers/KRAPNGViewHandler.m)

#### 修改已有APNG适配器

对于之前实现过`APNG`适配器的业务，需要对适配器进行一些修改：

- 适配器协议名：`QQAPNGImageViewProtocol` --> `APNGImageViewProtocol`
- 动画播放回调协议名：`QQAPNGViewPlayDelegate` --> `APNGViewPlayDelegate`
- 开始播放动画方法：`startAnimating` --> `startAPNGAnimating`
- 结束播放动画方法：`stopAnimating` --> `stopAPNGAnimating`

