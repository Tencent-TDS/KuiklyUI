package com.tencent.kuikly.demo.pages.waterfall_app.models

import com.tencent.kuikly.core.base.BaseObject
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.reactive.handler.observable

/**
 * 瀑布流卡片数据模型
 */
internal class WaterFallItem : BaseObject() {
    var title: String by observable("")
    var content: String by observable("")
    var userNick: String by observable("")
    var userAvatar: String by observable("")
    var imageUrl: String by observable("")
    var likeNum: String by observable("0")
    var height: Float by observable(0f)
    var bgColor: Color by observable(Color.WHITE)
    var imageWidth: Float by observable(0f)
    var imageHeight: Float by observable(0f)
}

/**
 * 消息数据模型
 */
internal class MessageItem : BaseObject() {
    var userName: String by observable("")
    var userAvatar: String by observable("")
    var lastMessage: String by observable("")
    var time: String by observable("")
    var unreadCount: Int by observable(0)
    var isOnline: Boolean by observable(false)
}