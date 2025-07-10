# PageList(分页列表)

``PageList``组件是一种分页列表组件, 他的每一个Item的宽度和高度与``PageList``的宽高一样大，并且滑动是以分页进行滑动

[组件使用范例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/kit_demo/DeclarativeDemo/PageListExamplePage.kt)

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)

### pageItemWidth方法

分页组件的Item宽度大小

<div class="table-01">

**pageItemWidth**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| width | item的宽度  | Float |

</div>

### pageItemHeight方法

分页组件的Item高度大小

<div class="table-01">

**pageItemHeight**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| height | item的高度  | Float |

</div>

### pageDirection方法

分页组件Item的排列方向

<div class="table-01">

**pageItemHeight**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| isHorizontal | 是否为横向排列  | Boolean |

</div>

### defaultPageIndex方法

分页组件默认定位到的页数

<div class="table-01">

**defaultPageIndex**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| defaultIndex | 默认定位的页数  | Int |

</div>

### keepItemAlive方法

不可见的分页Item是否要常驻在内存中

<div class="table-01">

**keepItemAlive**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| alive | item是否常驻  | Boolean |

</div>

### offscreenPageLimit方法

设置离屏 page 个数，仅当`keepItemAlive`为`false`时有效，默认`2`

<div class="table-01">

**keepItemAlive**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| value | 离屏 page 个数  | Int |

</div>

## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)

### pageIndexDidChanged

``pageIndexDidChanged``事件为分页组件当前的分页index发生变化的事件，如果组件有设置该事件事件，当分页组件当前的分页index发生变化时，会触发``pageIndexDidChanged``闭包回调。

<div class="table-01">

**pageIndexDidChanged**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| index | 当前分页的index  | Int |

</div>

