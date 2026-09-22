 /*
  * Tencent is pleased to support the open source community by making KuiklyUI
  * available.
  * Copyright (C) 2025 Tencent. All rights reserved.
  * Licensed under the License of KuiklyUI;
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

 package com.tencent.kuikly.demo.pages.demo.TBDemoTest

 import com.tencent.kuikly.core.annotations.Page
 import com.tencent.kuikly.core.base.Color
 import com.tencent.kuikly.core.base.ViewBuilder
 import com.tencent.kuikly.core.directives.vfor
 import com.tencent.kuikly.core.directives.vif
 import com.tencent.kuikly.core.directives.velse
 import com.tencent.kuikly.core.log.KLog
 import com.tencent.kuikly.core.module.CalendarModule
 import com.tencent.kuikly.core.module.TurboDisplayModule
 import com.tencent.kuikly.core.pager.IPagerEventObserver
 import com.tencent.kuikly.core.pager.Pager
 import com.tencent.kuikly.core.reactive.collection.ObservableList
 import com.tencent.kuikly.core.reactive.handler.observable
 import com.tencent.kuikly.core.reactive.handler.observableList
 import com.tencent.kuikly.core.timer.setTimeout
 import com.tencent.kuikly.core.views.List
 import com.tencent.kuikly.core.views.Text
 import com.tencent.kuikly.core.views.View
 import com.tencent.kuikly.demo.pages.base.BasePager
 import com.tencent.kuikly.demo.pages.demo.base.NavBar

 /**
  * TurboDisplay 测试页面 - 挂起 Diff（Config 静态挂起 + executeTurboDisplayDiff 触发）
  *
  * 页面状态机（参考业务 ChatDetailPage 的 loading/成功态模式）：
  * - 无缓存首进：DataList 为空 -> 渲染 Loading 态 -> 数据回来 -> 成功态布局
  * - 有缓存二进：缓存直出上次成功态；Kotlin 首帧跳过 Loading 直接渲染成功态布局
  *   （DataList 此时为空，结构占位）-> 数据回来 -> executeTurboDisplayDiff -> diff 接管
  *
  * 关键设计：有缓存时首帧渲染"成功态结构"而非 Loading，使真实树结构与缓存树结构对齐，
  * 数据回来后仅列表内容变化，diff 全部为属性级更新，无结构跳变。
  */
 @Page("TBDeferDiffTestPage")
 internal class TBDeferDiffTestPage : BasePager(), IPagerEventObserver {

     companion object {
         private const val TAG = "TBDeferDiffTestPage"
     }

     /** 列表数据（初始为空，数据下载完成后填充） */
     private var dataList: ObservableList<ListItemData> by observableList()

     /** 本次是否为 TB 缓存首屏（决定首帧渲染成功态布局还是 Loading 态） */
     private var isTurboDisplayMode: Boolean by observable(false)

     /** 真实视图是否已接管（TakeOverFinish 事件驱动，状态条肉眼验证 diff 完成时刻） */
     private var realViewTakenOver: Boolean by observable(false)

     /** 生命周期事件序号与起始时间戳（KLog 时序观测） */
     private var eventSeq = 0
     private var createTimestamp = 0L

     override fun created() {
         super.created()

         // 订阅TurboDisplay首屏渲染层生命周期事件（Native经sendWithEvent发送，onPagerEvent接收）
         addPagerEventObserver(this)

         // 缓存命中时首帧跳过Loading、直接渲染成功态布局（见body的vif分支）
         isTurboDisplayMode = acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME).isTurboDisplay()
         KLog.i(TAG, "isTurboDisplay: $isTurboDisplayMode")

         // 发起数据请求（模拟3s网络延迟）
         setTimeout(3000) {
             for (i in 0 until 1000) {
                 dataList.add(ListItemData(i, "Item $i"))
             }
             // *** 新增：数据下载完成且渲染指令发出后（layout完成），触发挂起的diff执行
             addTaskWhenPagerUpdateLayoutFinish {
                 realViewTakenOver = true
                 acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME).executeTurboDisplayDiff()
                 KLog.i(TAG, "executeTurboDisplayDiff triggered")
             }
         }
     }

     override fun body(): ViewBuilder {
         val ctx = this
         return {
             attr {
                 backgroundColor(Color(0xFFF5F6F8))
                 flexDirectionColumn()
             }

             NavBar {
                 attr {
                     title = "TB挂起Diff"
                 }
             }

             // 挂起场景状态指示：缓存视图展示中 -> 真实视图已接管，TakeOverFinish事件驱动，diff完成时刻肉眼可见
             vif({ ctx.isTurboDisplayMode }) {
                 Text {
                     attr {
                         margin(16f, 8f, 16f, 0f)
                         fontSize(12f)
                         color(if (ctx.realViewTakenOver) Color(0xFF1B5E20) else Color(0xFFE65100))
                         text(if (ctx.realViewTakenOver) "● 真实视图已接管（diff 完成）" else "● 挂起中：缓存视图展示中（等待 executeTurboDisplayDiff）")
                     }
                 }
                 // 手动执行Diff（幂等测试）：可重复点击，重复通知由Handler侧diffSuspended幂等拒绝；
                 // 边界：数据未回时点击 = 提前execute = 空真实树diff（白屏边界复现，属预期行为）
                 View {
                     attr {
                         margin(16f, 4f, 16f, 8f)
                         padding(10f, 6f)
                         borderRadius(8f)
                         backgroundColor(Color(0xFF0D47A1))
                         allCenter()
                     }
                     event {
                         click {
                             ctx.acquireModule<TurboDisplayModule>(TurboDisplayModule.MODULE_NAME).executeTurboDisplayDiff()
                             KLog.i(TAG, "manual executeTurboDisplayDiff clicked (idempotency test)")
                         }
                     }
                     Text {
                         attr {
                             fontSize(13f)
                             color(Color.WHITE)
                             text("手动执行Diff（幂等测试，可重复点击）")
                         }
                     }
                 }
             }

             // 首帧分叉：无缓存 -> Loading态；有缓存 -> 成功态布局（空数据结构占位，缓存直出补齐视觉）
             vif({ !ctx.isTurboDisplayMode && ctx.dataList.isEmpty() }) {
                 View {
                     attr {
                         flex(1f)
                         allCenter()
                     }
                     Text {
                         attr {
                             text("Loading...")
                             fontSize(14f)
                             color(Color(0xFFBBBBBB))
                         }
                     }
                 }
             }
             velse {
                 // 成功态布局：页面标题区 + List主体（与缓存树结构对齐）
                 Text {
                     attr {
                         margin(16f, 10f)
                         fontSize(18f)
                         fontWeightBold()
                         color(Color(0xFF333333))
                         text("聊天详情")
                     }
                 }
                 // 页面主体：List懒加载（1000条仅构建可视+预加载区，滚动按需创建）
                 List {
                     attr {
                         flex(1f)
                         firstContentLoadMaxIndex(20)
                     }
                     vfor({ ctx.dataList }) { item ->
                         View {
                             attr {
                                 height(60f)
                                 margin(12f, 5f)
                                 borderRadius(12f)
                                 backgroundColor(Color.WHITE)
                                 flexDirectionRow()
                                 alignItemsCenter()
                             }
                             // 左侧随机色块：缓存态与数据态颜色必然不同，diff接管时刻肉眼可见
                             View {
                                 attr {
                                     size(40f, 40f)
                                     marginLeft(14f)
                                     borderRadius(20f)
                                     backgroundColor(ctx.getColorByIndex(item.id))
                                 }
                             }
                             Text {
                                 attr {
                                     marginLeft(14f)
                                     flex(1f)
                                     text(item.title)
                                     fontSize(16f)
                                     color(Color(0xFF333333))
                                 }
                             }
                             Text {
                                 attr {
                                     text(">")
                                     fontSize(14f)
                                     color(Color(0xFFCCCCCC))
                                     marginRight(14f)
                                 }
                             }
                         }
                     }
                 }
             }
         }
     }

     /** TurboDisplay首屏渲染层生命周期事件接收（验证跨端事件链路），KLog带序号+相对耗时便于时序观测 */
     override fun onPagerEvent(pagerEvent: String, eventData: com.tencent.kuikly.core.nvi.serialization.json.JSONObject) {
         // 仅对TB生命周期事件编号打印，其他Pager事件（pageFirstFramePaint/viewDidAppear等）直接忽略，避免序号跳号误导
         if (!pagerEvent.startsWith("onInitLayer")) {
             return
         }
         val seq = ++eventSeq
         when (pagerEvent) {
             Pager.PAGER_EVENT_INIT_LAYER_READ_CACHE_START ->
                 KLog.i(TAG, "[TB event #$seq + onInitLayerReadCacheStart")
             Pager.PAGER_EVENT_INIT_LAYER_READ_CACHE_FINISH ->
                 KLog.i(TAG, "[TB event #$seq + onInitLayerReadCacheFinish: succ=${eventData.optBoolean("succ")}, bytes=${eventData.optLong("bytes")}, children=${eventData.optLong("children")}")
             Pager.PAGER_EVENT_INIT_LAYER_RENDER_CACHE_START ->
                 KLog.i(TAG, "[TB event #$seq + onInitLayerRenderCacheStart")
             Pager.PAGER_EVENT_INIT_LAYER_RENDER_CACHE_FINISH ->
                 KLog.i(TAG, "[TB event #$seq + onInitLayerRenderCacheFinish: succ=${eventData.optBoolean("succ")}")
             Pager.PAGER_EVENT_INIT_LAYER_REAL_VIEW_TAKE_OVER_START ->
                 KLog.i(TAG, "[TB event #$seq + onInitLayerRealViewTakeOverStart")
             Pager.PAGER_EVENT_INIT_LAYER_REAL_VIEW_TAKE_OVER_FINISH -> {
                 KLog.i(TAG, "[TB event #$seq + onInitLayerRealViewTakeOverFinish: succ=${eventData.optBoolean("succ")}")
             }
         }
     }

     /**
      * 列表项数据类
      */
     data class ListItemData(
         val id: Int,
         val title: String
     )

     // 取色：按 index 确定性取色 + 页面级随机偏移，
     // 使本次启动的配色与上次缓存下来的配色不同（diff 接管时刻配色整体刷新，肉眼可见）；
     // 偏移在单次页面生命周期内固定，避免每次重组都变色
     private val listColorScheme = listOf(
         Color(0xFFB71C1C), Color(0xFFE65100), Color(0xFF1B5E20),
         Color(0xFF0D47A1), Color(0xFF4A148C)
     )

     /** 页面级取色偏移（每次启动随机，单次生命周期内固定） */
     private val colorOffset = kotlin.random.Random.nextInt(listColorScheme.size)

     private fun getColorByIndex(index: Int): Color {
         return listColorScheme[(index + colorOffset) % listColorScheme.size]
     }
 }
