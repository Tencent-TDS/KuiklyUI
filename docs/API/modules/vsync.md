# VsyncModule

垂直同步（Vsync）信号监听模块，用于注册逐帧回调，适用于需要按帧驱动的动画或自定义绘制场景。

:::warning 使用注意
Vsync 回调为常驻回调（keepCallbackAlive），回调频率与屏幕刷新率一致。回调内应避免耗时操作，使用完毕后务必调用 `unRegisterVsync` 解除注册。
:::

## registerVsync方法

注册 Vsync 回调，每帧触发一次。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| callback | 每帧回调闭包 | () -> Unit |

**示例**

```kotlin
acquireModule<VsyncModule>(VsyncModule.MODULE_NAME).registerVsync {
    // 每帧执行
}
```

## registerVsyncWithFrameInterval方法

注册 Vsync 回调，并将帧间隔（纳秒）以原子方式回传给业务，避免每帧走 JSON 序列化，性能优于 `registerVsync`。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| callback | 每帧回调闭包，参数为帧间隔（纳秒） | (Int) -> Unit |

当端侧回传的帧间隔不在合法区间 `1_000_000 ~ 100_000_000` 纳秒时，会回退为默认值 `16_666_667` 纳秒（约 60fps）。

**示例**

```kotlin
acquireModule<VsyncModule>(VsyncModule.MODULE_NAME).registerVsyncWithFrameInterval { frameIntervalNanos ->
    // frameIntervalNanos 为帧间隔，单位纳秒
}
```

## unRegisterVsync方法

取消注册 Vsync 回调。
