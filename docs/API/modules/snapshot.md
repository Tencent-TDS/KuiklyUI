# SnapshotModule

生成当前pager的快照，用于下次打开该pager的首屏未出现时以该快照作为首屏，实现首屏0白屏体验 <Badge text="仅iOS" type="error"/> 

:::tip 注意
该模块是用于解决js模式首屏慢的问题，如果你的页面是使用framework执行模式的话, 无需使用该模块
:::

## snapshotPager方法

将当前Pager生成快照缓存到本地, 下次打开页面时, 以该快照作为首屏

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| snapshotKey <Badge text="必需" type="warn"/> | 快照的唯一key，用户端侧使用该快照的key，建议一般该key以（版本号+页面名+夜间模式）作为key  | String |
