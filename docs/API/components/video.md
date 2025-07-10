# Video(播放器)

视频播放器组件

[组件使用示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/VideoExamplePage.kt)

## 属性

支持所有[基础属性](basic-attr-event.md#基础属性)，此外还支持：

### src

视频播放源

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| src | 视频源 URL | String |

### playControl

设置播放控制属性（播放、暂停、停止）

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| playControl | 播放控制枚举 | VideoPlayControl |

### resizeModeToCover

设置视频画面拉伸模式为等比例撑满

### resizeModeToContain

设置视频画面拉伸模式为等比例不裁剪，保留黑边

### resizeModeToStretch

设置视频画面拉伸模式为缩放撑满（非等比例，会变形）

### muted

设置是否静音属性

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| muted | 是否静音 | Boolean |

### rate

设置倍速属性（1.0, 1.25, 1.5, 2.0）

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| rate | 倍速值 | Float |


## 事件

支持所有[基础事件](basic-attr-event.md#基础事件)，此外还支持：

### playStateDidChanged

播放状态变化回调，回参为播放状态和扩展信息

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| state | 播放状态枚举 | PlayState |
| extInfo | 扩展信息 | JSONObject |

### playTimeDidChanged

播放时间变化回调（毫秒），回参为当前播放时间和总时间

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| curTime | 当前播放时间 | Int |
| totalTime | 总时间 | Int |

### firstFrameDidDisplay

视频首帧画面显示时回调（一般用该时机隐藏封面）

