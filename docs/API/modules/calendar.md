# CalendarModule

日期处理模块

[模块使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/CalendarModuleExamplePage.kt)

## formatTime方法

格式化时间戳

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| timeMillis | 时间戳（单位毫秒）  | Long |
| format | 格式化模板  | String |

`format`示例："yyyy-MM-dd HH:mm:ss.SSS"

## parseFormattedTime方法

将格式化时间转为时间戳

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| formattedTime | 格式化时间  | String |
| format | 格式化时间模板  | String |

## newCalendarInstance方法

返回时间戳对应的日期（时间戳为0返回当前时间日期）

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| timeMillis | 时间戳  | Long |

该方法回返回日期`Calendar`实例，该实例可调用以下方法：

### set

设置日期

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| field | 日期的某个域（年、月、日、时、分、秒等）  | ICalendar.Field |
| value | 对应域设置的值  | Int |

### add

增加日期

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| field | 日期的某个域（年、月、日、时、分、秒等）  | ICalendar.Field |
| value | 对应域增加的值  | Int |

### get

获取日期某个域的值

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| field | 日期的某个域（年、月、日、时、分、秒等）  | ICalendar.Field |

### timeInMillis

将日期转为时间戳

<br/>

