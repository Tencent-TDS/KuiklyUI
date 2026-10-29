## ADDED Requirements

### Requirement: 连续同步事件会话期间帧任务 SHALL 等待本帧输入且至多合并一个 vsync

`KRVsyncModule` 在 vsync 回调通过代数校验后，SHALL 把「调用模块回调，再投递清除上一帧未完成标记的任务」作为一次帧任务，由 `KRInputFirstGate` 决定什么时候投递：

- 不处于任何连续同步事件会话中时 SHALL 立即投递；
- 处于会话中且本帧输入已处理完（当前无在途的同步连续事件，且最近一次同步连续事件处理完成时间不早于本次 vsync 时间戳）时 SHALL 立即投递；
- 处于会话中且处于"输入空闲"状态时 SHALL 立即投递；
- 否则 SHALL 暂存，并在本帧同步连续事件处理完成时投递；
- 若下一个 vsync 到达时暂存仍未投递，SHALL 以新 vsync 的帧信息投递该次暂存（合并一个 vsync），SHALL 不再额外丢弃或排队，并 SHALL 进入"输入空闲"状态；"输入空闲"状态在下一次同步连续事件开始时 SHALL 退出。

判断投递时机 SHALL 不使用任何时间阈值。暂存的投递在执行时 SHALL 重新校验模块 running 状态与 generation，反注册或销毁后 SHALL 丢弃。同一时刻至多存在一次还没执行完的帧任务，这个规则不变。该行为始终生效。

#### Scenario: 会话期间帧任务排在输入之后
- **GIVEN** HarmonyOS 上 Compose 页面经 vsync 驱动 `renderFrame`，用户正在进行同步拖动（touch 会话进行中）
- **WHEN** 某帧 vsync 回调先于该帧 touchMove 的同步处理到达
- **THEN** 该帧 `renderFrame` 任务 SHALL 在 touchMove 同步处理与 UI 提交完成之后才进入 context 队列
- **AND** touchMove 的同步处理 SHALL 不因为这一帧的 `renderFrame` 而等待 context 任务锁

#### Scenario: 本帧输入已处理完立即投递
- **GIVEN** 会话进行中
- **WHEN** 某帧 touchMove 的同步处理在该帧 vsync 回调到达前已经完成
- **THEN** 帧任务 SHALL 立即投递，不额外等待

#### Scenario: 手指停顿至多合并一帧
- **GIVEN** 会话进行中，用户手指停住不再产生 touchMove
- **WHEN** vsync N 的帧任务被暂存后 vsync N+1 到达
- **THEN** 暂存的帧任务 SHALL 以 vsync N+1 的帧信息投递
- **AND** 此后的 vsync SHALL 直接投递帧任务，帧节拍 SHALL 不减半
- **AND** 手指再次移动产生 touchMove 后，帧任务 SHALL 重新等待本帧输入

#### Scenario: 停顿期间独立动画
- **GIVEN** 会话进行中，页面存在与手指无关、仍在运行的动画
- **WHEN** 手指停住若干帧
- **THEN** 该动画 SHALL 至多在停顿开始时损失一帧，其余帧正常推进

#### Scenario: 不在会话中行为不变
- **GIVEN** 页面没有进行中的连续同步事件会话
- **WHEN** vsync 回调到达
- **THEN** 帧任务 SHALL 立即投递，与改造前一致

#### Scenario: 暂存期间反注册
- **GIVEN** 某帧帧任务处于暂存状态
- **WHEN** 页面销毁触发 `unRegisterVsync`
- **THEN** 暂存的投递执行时 SHALL 因 generation 不匹配被丢弃，不调用已失效的回调

#### Scenario: Android 与 iOS 不受影响
- **GIVEN** Android 或 iOS 页面经平台既有 vsync 驱动 `renderFrame`
- **WHEN** 用户进行同步拖动
- **THEN** 帧任务投递时机 SHALL 与改造前一致
