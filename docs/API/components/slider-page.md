# SliderPage(轮播图)

用于展示一组图片或内容的视图组件，支持自动轮播和手动切换页面

[组件使用范例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/SliderPageViewDemoPage.kt)

## 属性

除了支持所有[基础属性](basic-attr-event.md#基础属性)，还支持以下属性：

### defaultPageIndex

初始显示的页面索引，默认为 0

| 变量名              | 描述                                                     | 类型    |
|------------------| -------------------------------------------------------- | ------- |
| defaultPageIndex | 初始显示的页面索引                            | Int     |

### isHorizontal

是否为水平方向展示, 默认为 `true`

| 变量名                 | 描述                                                   | 类型    |
| ---------------------- | ------------------------------------------------------ | ------- |
| isHorizontal           | 是否为水平方向展示                      | Boolean |

### pageItemWidth

页面项宽度，默认为 `view` 宽度

| 变量名                 | 描述                                       | 类型    |
| ---------------------- | ------------------------------------------ | ------- |
| pageItemWidth          | 页面项宽度                          | Float   |

### pageItemHeight

页面项高度，默认为 `view` 高度

| 变量名                 | 描述    | 类型    |
| ---------------------- |-------| ------- |
| pageItemHeight         | 页面项高度 | Float   |

### loopPlayIntervalTimeMs

轮播时间间隔（毫秒单位），若等于 0 则不轮播，默认为 `0`

| 变量名                 | 描述                                                      | 类型    |
| ---------------------- | --------------------------------------------------------- | ------- |
| loopPlayIntervalTimeMs | 轮播时间间隔（毫秒单位） | Int     |

### itemCount

页面项数量，默认为 0

| 变量名                 | 描述                                               | 类型    |
| ---------------------- | -------------------------------------------------- | ------- |
| itemCount              | 页面项数量                                    | Int     |

### initSliderItems

设置Slider的dataList，以及creator

| 参数                     | 描述            | 类型    |
|------------------------|---------------| ------- |
| dataList		      | 		数据源		       |  List<``T``>|
| creator		      | 		创建item的闭包		 |  SliderItemCreator<``T``>|


## 事件

### pageIndexDidChanged

监听页面切换事件，回调参数为 `Int`，表示当前页面的索引

| 事件名              | 描述                                                                  | 类型                           |
| ------------------- |---------------------------------------------------------------------| ------------------------------ |
| handler | 事件回调闭包, 回包回调的参数param的类型为``JSONObject``, 其中包含``index``的key, 代表当前分页索引 | (handler：parma: Any?) -> Unit |

## 方法

### startLoopPlayIfNeed

`()` 启动循环播放。

### stopLoopPlayIfNeed

`()`停止循环播放

### autoLoopPlay

`()`设置自动循环播放