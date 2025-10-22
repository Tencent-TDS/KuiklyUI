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

package com.tencent.kuikly.demo.pages.waterfall_app.pages

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.AlertDialog
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.waterfall_app.components.MessageItemView
import com.tencent.kuikly.demo.pages.waterfall_app.data.WaterfallDataProvider
import com.tencent.kuikly.demo.pages.waterfall_app.models.MessageItem
// 移除kotlinx.coroutines的直接导入，使用Kuikly内建协程

/**
 * 瀑布流消息页面
 */
internal class WaterfallMessagePage : ComposeView<WaterfallMessagePageAttr, WaterfallMessagePageEvent>() {
    
    private var messageList: ObservableList<MessageItem> by observableList<MessageItem>()
    private var selectedTabIndex: Int by observable(0) // 0: 聊天, 1: 赞和收藏, 2: 新增关注
    private val tabTitles = listOf("聊天", "赞和收藏", "新增关注")
    private var searchKeyword: String by observable("")
    private var showDeleteDialog: Boolean by observable(false)
    private var messageToDelete: MessageItem? = null
    private var isLoading: Boolean by observable(true)
    private var loadError: String? by observable(null)
    
    override fun createEvent(): WaterfallMessagePageEvent {
        return WaterfallMessagePageEvent()
    }

    override fun createAttr(): WaterfallMessagePageAttr {
        return WaterfallMessagePageAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flex(1f)
                backgroundColor(Color(0xFFF5F5F5))
                flexDirectionColumn()
            }

            // 分类标签栏
            View {
                attr {
                    height(60f)
                    backgroundColor(Color.WHITE)
                    flexDirectionRow()
                    alignItemsCenter()
                    justifyContentCenter() // 水平居中对齐
                    paddingTop(15f)
                }

                for (i in ctx.tabTitles.indices) {
                    View {
                        attr {
                            flexDirectionColumn() // 改为垂直布局
                            alignItemsCenter() // 水平居中
                            justifyContentCenter() // 垂直居中
                            marginLeft(10f) // 添加左间距
                            marginRight(10f) // 添加右间距
                        }

                        Text {
                            attr {
                                text(ctx.tabTitles[i])
                                fontSize(16f)
                                color(if (i == ctx.selectedTabIndex) Color(0xFFFF2442) else Color(0xFF666666))
                                if (i == ctx.selectedTabIndex) fontWeightBold()
                            }
                        }

                        // 选中指示器
                        View {
                            attr {
                                width(20f)
                                height(2f)
                                backgroundColor(if (i == ctx.selectedTabIndex) Color(0xFFFF2442) else Color.TRANSPARENT)
                                borderRadius(1f)
                                marginTop(5f)
                            }
                        }

                        event {
                            click {
                                ctx.selectedTabIndex = i
                                KLog.i("WaterfallMessagePage", "切换到标签: ${ctx.tabTitles[i]}")
                                ctx.event.onTabChanged?.invoke(i)
                            }
                        }
                    }
                }
            }

            // 分隔线
            View {
                attr {
                    height(1f)
                    backgroundColor(Color(0xFFEEEEEE))
                }
            }

            // 消息列表
            Scroller {
                attr {
                    flex(1f)
                    backgroundColor(Color.WHITE)
                }

                View {
                    attr {
                        flexDirectionColumn()
                    }

                    // 根据选中的标签显示不同内容
                    when (ctx.selectedTabIndex) {
                        0 -> {
                            // 聊天列表
                            if (ctx.isLoading) {
                                // 加载中状态
                                View {
                                    attr {
                                        height(200f)
                                        justifyContentCenter()
                                        alignItemsCenter()
                                    }
                                    Text {
                                        attr {
                                            text("正在加载消息...")
                                            fontSize(16f)
                                            color(Color(0xFF999999))
                                        }
                                    }
                                }
                            } else if (ctx.loadError != null) {
                                // 加载错误状态
                                View {
                                    attr {
                                        height(200f)
                                        justifyContentCenter()
                                        alignItemsCenter()
                                        flexDirectionColumn()
                                    }
                                    Text {
                                        attr {
                                            text("加载失败: ${ctx.loadError}")
                                            fontSize(16f)
                                            color(Color(0xFFFF4444))
                                            marginBottom(10f)
                                        }
                                    }
                                    View {
                                        attr {
                                            backgroundColor(Color(0xFFFF2442))
                                            borderRadius(5f)
                                        }
                                        Text {
                                            attr {
                                                text("重试")
                                                fontSize(14f)
                                                color(Color.WHITE)
                                            }
                                        }
                                        event {
                                            click {
                                                KLog.i("WaterfallMessagePage", "用户点击重试按钮")
                                                ctx.initMessageData()
                                            }
                                        }
                                    }
                                }
                            } else {
                                val filteredMessages = if (ctx.searchKeyword.isEmpty()) {
                                    ctx.messageList
                                } else {
                                    ctx.messageList.filter { message ->
                                        message.userName.contains(ctx.searchKeyword, ignoreCase = true) ||
                                        message.lastMessage.contains(ctx.searchKeyword, ignoreCase = true)
                                    }
                                }

                                if (filteredMessages.isEmpty()) {
                                    View {
                                        attr {
                                            height(200f)
                                            justifyContentCenter()
                                            alignItemsCenter()
                                        }
                                        Text {
                                            attr {
                                                text(if (ctx.searchKeyword.isEmpty()) "暂无聊天消息" else "未找到相关消息")
                                                fontSize(16f)
                                                color(Color(0xFF999999))
                                            }
                                        }
                                    }
                                } else {
                                    filteredMessages.forEachIndexed { index, message ->
                                        MessageItemView(message) {
                                            event {
                                                click {
                                                    KLog.i("WaterfallMessagePage", "点击了消息: ${message.userName}")
                                                    // 这里可以跳转到具体的聊天页面
                                                    ctx.event.onMessageClick?.invoke(message)
                                                }
                                                longPress {
                                                    KLog.i("WaterfallMessagePage", "长按了消息: ${message.userName}")
                                                    // 显示删除选项
                                                    ctx.showDeleteDialog(message)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // 赞和收藏页面
                            View {
                                attr {
                                    height(200f)
                                    justifyContentCenter()
                                    alignItemsCenter()
                                }
                                Text {
                                    attr {
                                        text("暂无赞和收藏消息")
                                        fontSize(16f)
                                        color(Color(0xFF999999))
                                    }
                                }
                            }
                        }
                        2 -> {
                            // 新增关注页面
                            View {
                                attr {
                                    height(200f)
                                    justifyContentCenter()
                                    alignItemsCenter()
                                }
                                Text {
                                    attr {
                                        text("暂无新增关注消息")
                                        fontSize(16f)
                                        color(Color(0xFF999999))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 删除确认对话框
            AlertDialog {
                attr {
                    showAlert(ctx.showDeleteDialog)
                    title("删除对话")
                    message("确定要删除与「${ctx.messageToDelete?.userName ?: ""}」的对话吗？删除后将无法恢复。")
                    actionButtons("取消", "删除")
                    inWindow(true)
                }
                event {
                    clickActionButton { index ->
                        when (index) {
                            0 -> {
                                // 取消按钮
                                KLog.i("WaterfallMessagePage", "用户取消删除操作")
                                ctx.showDeleteDialog = false
                                ctx.messageToDelete = null
                            }
                            1 -> {
                                // 删除按钮
                                ctx.messageToDelete?.let { message ->
                                    ctx.deleteMessage(message)
                                    KLog.i("WaterfallMessagePage", "用户确认删除与 ${message.userName} 的对话")
                                }
                                ctx.showDeleteDialog = false
                                ctx.messageToDelete = null
                            }
                        }
                    }
                    willDismiss {
                        // 系统返回键或右滑返回时触发
                        ctx.showDeleteDialog = false
                        ctx.messageToDelete = null
                    }
                }
            }

        }
    }

    override fun created() {
        super.created()
        // 初始化消息数据
        initMessageData()
    }

    private fun initMessageData() {
        isLoading = true
        loadError = null
        
        val ctx = this
        // 使用Kuikly内建协程，通过getPager().lifecycleScope启动协程
        getPager().lifecycleScope.launch {
            try {
                KLog.i("WaterfallMessagePage", "开始异步加载消息数据")
                // 调用挂起函数loadMessageData
                val result = WaterfallDataProvider.loadMessageData()
                
                if (result.isSuccess) {
                    val messages = result.getOrNull() ?: emptyList()
                    // 更新响应式字段（在Kuikly线程中执行）
                    ctx.messageList.clear()
                    ctx.messageList.addAll(messages)
                    KLog.i("WaterfallMessagePage", "消息数据加载成功，共 ${messages.size} 条消息")
                } else {
                    val error = result.exceptionOrNull()
                    ctx.loadError = error?.message ?: "未知错误"
                    KLog.e("WaterfallMessagePage", "消息数据加载失败: ${ctx.loadError}")
                }
            } catch (e: Exception) {
                ctx.loadError = e.message ?: "加载异常"
                KLog.e("WaterfallMessagePage", "消息数据加载异常: ${ctx.loadError}")
            } finally {
                ctx.isLoading = false
            }
        }
    }

    private fun showDeleteDialog(messageItem: MessageItem) {
        KLog.i("WaterfallMessagePage", "准备显示删除确认对话框: ${messageItem.userName}")
        messageToDelete = messageItem
        showDeleteDialog = true
    }

    private fun deleteMessage(messageItem: MessageItem) {
        messageList.remove(messageItem)
        KLog.i("WaterfallMessagePage", "已删除与 ${messageItem.userName} 的对话")
    }

    fun searchMessages(keyword: String) {
        searchKeyword = keyword
    }
}

internal class WaterfallMessagePageAttr : ComposeAttr()

internal class WaterfallMessagePageEvent : ComposeEvent() {
    var onMessageClick: ((MessageItem) -> Unit)? = null
    var onTabChanged: ((Int) -> Unit)? = null
}

internal fun ViewContainer<*, *>.WaterfallMessagePage(init: WaterfallMessagePage.() -> Unit) {
    addChild(WaterfallMessagePage(), init)
}