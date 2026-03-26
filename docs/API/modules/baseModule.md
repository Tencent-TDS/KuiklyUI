# BaseModule（模块基类）

`BaseModule` 是 Kuikly 框架的模块基类，所有内置 Module 和业务自定义 Module 均继承此类。

## 继承关系

```
BaseModule
├── MemoryCacheModule
├── SharedPreferencesModule
├── RouterModule
├── NetworkModule
├── NotifyModule
├── SnapshotModule
├── CodecModule
├── CalendarModule
└── 业务自定义 Module...
```

## 方法

### viewWithTag()

根据 tag 获取对应的 Native View 实例。

| 参数 | 描述 | 类型 |
| -- | -- | -- |
| tag | View 的索引标识 | Int/NSNumber/number |

| 返回值                              | 描述 |
|----------------------------------| -- |
| View/UIView/KuiklyRenderBaseView | 对应 tag 的 View 实例，不存在则返回 null |


#### 注意事项

- **iOS**: 必须在主线程调用
- **Android**: 在主线程或 UI 线程调用
- **鸿蒙**: 在 UI 线程调用

