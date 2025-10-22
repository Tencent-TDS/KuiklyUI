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