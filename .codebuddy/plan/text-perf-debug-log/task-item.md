# 实施计划

## 概述

本计划旨在为 `JankPageDemo.kt` 添加调试日志，用于排查 Text 组件在滑动过程中是否存在多次重组和多次布局的问题。

---

- [ ] 1. 创建带日志的 DebugText 可复用组件
   - 在 `JankPageDemo.kt` 文件中创建一个 `DebugText` Composable 函数
   - 该组件内部包装 Material3 的 `Text` 组件
   - 接收 `uniqueId` 参数用于标识组件实例
   - 使用 `SideEffect` 在每次重组时打印日志，格式为：`[TextPerfDebug][Recompose] id={uniqueId} text={textContent}`
   - 使用 `onTextLayout` 回调在布局完成时打印日志，格式为：`[TextPerfDebug][Layout] id={uniqueId} size=(w:{width}, h:{height})`
   - _需求：1.1-1.5, 2.1-2.5, 3.1-3.5, 4.1-4.3, 5.1-5.3_

- [ ] 2. 替换 RankingItemView2 中的 Title Text 组件
   - 将 `RankingItemView2` 函数中的 Title `Text` 替换为 `DebugText`
   - 设置 `uniqueId` 为 `{carIndex}_{itemIndex}_title` 格式
   - 保持原有的样式属性（fontSize、color、maxLines 等）不变
   - _需求：1.3, 2.3, 3.3_

- [ ] 3. 替换 RankingItemView2 中的 Tag Text 组件
   - 将 `RankingItemView2` 函数中的 Tag `Text` 替换为 `DebugText`
   - 设置 `uniqueId` 为 `{carIndex}_{itemIndex}_tag` 格式
   - 保持原有的样式属性不变
   - _需求：1.3, 2.3, 3.3_

- [ ] 4. 替换 RankingItemView2 中的 Views Text 组件
   - 将 `RankingItemView2` 函数中的 Views 数量 `Text` 替换为 `DebugText`
   - 设置 `uniqueId` 为 `{carIndex}_{itemIndex}_views` 格式
   - 保持原有的样式属性不变
   - _需求：1.3, 2.3, 3.3_

- [ ] 5. 替换 RankingItemView2 中的 Rule Text 组件
   - 将 `RankingItemView2` 函数中的 Rule `Text` 替换为 `DebugText`
   - 设置 `uniqueId` 为 `{carIndex}_{itemIndex}_rule` 格式
   - 保持原有的样式属性不变
   - _需求：1.3, 2.3, 3.3_

- [ ] 6. 替换 RankingItemView2 中的排名数字 Text 组件
   - 将 `RankingItemView2` 函数中显示排名数字的 `Text` 替换为 `DebugText`
   - 设置 `uniqueId` 为 `{carIndex}_{itemIndex}_rank` 格式
   - 保持原有的样式属性不变
   - _需求：1.3, 2.3, 3.3_

- [ ] 7. 验证日志输出并进行滑动测试
   - 运行 Demo 应用
   - 滑动 LazyColumn 和 LazyRow
   - 使用 `[TextPerfDebug]` 前缀过滤日志
   - 检查是否存在同一 uniqueId 多次打印 `[Recompose]` 或 `[Layout]` 日志的情况
   - _需求：1.1, 2.1, 3.4, 3.5_
