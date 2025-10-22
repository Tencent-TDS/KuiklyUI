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

package com.tencent.kuikly.demo.pages.waterfall_app.data

import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 从本地资源文件加载数据，模拟真实的网络请求场景
 */
internal object DataLoader {

    private const val TAG = "DataLoader"

    /**
     * 瀑布流数据项
     */
    data class WaterfallDataItem(
        val content: String,
        val userNick: String,
        val userAvatar: String,
        val likeNum: String,
        val imageUrl: String,
        val imageWidth: Double,
        val imageHeight: Double
    ) {
        companion object {
            /**
             * 从JSONObject创建WaterfallDataItem
             */
            fun fromJSONObject(jsonObject: JSONObject): WaterfallDataItem {
                return WaterfallDataItem(
                    content = jsonObject.optString("content"),
                    userNick = jsonObject.optString("userNick"),
                    userAvatar = jsonObject.optString("userAvatar"),
                    likeNum = jsonObject.optString("likeNum"),
                    imageUrl = jsonObject.optString("imageUrl"),
                    imageWidth = jsonObject.optDouble("imageWidth"),
                    imageHeight = jsonObject.optDouble("imageHeight")
                )
            }
        }
    }

    /**
     * 消息数据项
     */
    data class MessageDataItem(
        val userName: String,
        val userAvatar: String,
        val lastMessage: String,
        val time: String,
        val unreadCount: Int,
        val isOnline: Boolean
    ) {
        companion object {
            /**
             * 从JSONObject创建MessageDataItem
             */
            fun fromJSONObject(jsonObject: JSONObject): MessageDataItem {
                return MessageDataItem(
                    userName = jsonObject.optString("userName"),
                    userAvatar = jsonObject.optString("userAvatar"),
                    lastMessage = jsonObject.optString("lastMessage"),
                    time = jsonObject.optString("time"),
                    unreadCount = jsonObject.optInt("unreadCount"),
                    isOnline = jsonObject.optBoolean("isOnline")
                )
            }
        }
    }



    /**
     * 从资源文件加载内容
     * 这是一个平台相关的实现，需要在各平台实现具体的资源加载逻辑
     */
    private fun loadResourceFile(path: String): String {
        // 这里需要根据不同平台实现资源文件加载
        // 在实际项目中，可以使用 expect/actual 机制来实现平台相关的代码
        return try {
            // 模拟从资源文件读取内容
            // 在真实项目中，这里应该是平台相关的资源加载代码
            when (path) {
                "data/waterfall_data.json" -> getWaterfallDataJson()
                "data/message_data.json" -> getMessageDataJson()
                else -> throw IllegalArgumentException("未知的资源文件: $path")
            }
        } catch (e: Exception) {
            KLog.e(TAG, "资源文件加载失败: $path, 错误: ${e.message}")
            throw e
        }
    }

    /**
     * 同步加载瀑布流数据（内部使用）
     * 供WaterfallDataProvider的挂起函数调用
     */
    fun loadWaterfallDataSync(page: Int = 1, pageSize: Int = 20): Result<List<WaterfallDataItem>> {
        return try {
            KLog.i(TAG, "同步加载瀑布流数据 - 页码: $page, 每页: $pageSize")

            // 模拟网络请求失败（5% 概率）
            if ((1..100).random() <= 5) {
                throw Exception("网络请求失败")
            }

            // 从资源文件加载数据，使用JSONArray解析
            val jsonContent = loadResourceFile("data/waterfall_data.json")
            val jsonArray = JSONArray(jsonContent)

            // 将JSONArray转换为数据列表
            val allData = mutableListOf<WaterfallDataItem>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.optJSONObject(i)
                if (jsonObject != null) {
                    allData.add(WaterfallDataItem.fromJSONObject(jsonObject))
                }
            }

            // 分页处理
            val startIndex = (page - 1) * pageSize

            val pageData = if (startIndex < allData.size) {
                // 如果请求的页码超出数据范围，循环使用数据
                val result = mutableListOf<WaterfallDataItem>()
                for (i in 0 until pageSize) {
                    val dataIndex = (startIndex + i) % allData.size
                    result.add(allData[dataIndex])
                }
                result
            } else {
                emptyList()
            }

            KLog.i(TAG, "瀑布流数据同步加载成功 - 返回 ${pageData.size} 条数据")
            Result.success(pageData)

        } catch (e: Exception) {
            KLog.e(TAG, "瀑布流数据同步加载失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 同步加载消息数据（内部使用）
     * 供WaterfallDataProvider的挂起函数调用
     */
    fun loadMessageDataSync(): Result<List<MessageDataItem>> {
        return try {
            KLog.i(TAG, "同步加载消息数据")

            // 模拟网络请求失败（3% 概率）
            if ((1..100).random() <= 3) {
                throw Exception("消息数据加载失败")
            }

            // 从资源文件加载数据，使用JSONArray解析
            val jsonContent = loadResourceFile("data/message_data.json")
            val jsonArray = JSONArray(jsonContent)

            // 将JSONArray转换为数据列表
            val messageData = mutableListOf<MessageDataItem>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.optJSONObject(i)
                if (jsonObject != null) {
                    messageData.add(MessageDataItem.fromJSONObject(jsonObject))
                }
            }

            KLog.i(TAG, "消息数据同步加载成功 - 返回 ${messageData.size} 条数据")
            Result.success(messageData)

        } catch (e: Exception) {
            KLog.e(TAG, "消息数据同步加载失败: ${e.message}")
            Result.failure(e)
        }
    }



    /**
     * 获取瀑布流数据的JSON字符串
     * 在实际项目中，这应该从资源文件中读取
     */
    private fun getWaterfallDataJson(): String {
        return """[
  {
    "content": "清晨的阳光洒在窗台上，一杯咖啡，一本书，一段静谧的时光。生活不需要太多的喧嚣，简单才是最真实的幸福。",
    "userNick": "晨间漫步者",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8d0813ca.png",
    "likeNum": "400",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59591ba6.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 1200.0
  },
  {
    "content": "我们这代人最擅长的，就是把『我想你』翻译成『你看月亮了吗』。",
    "userNick": "文字失语症",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/45ad086d.png",
    "likeNum": "5300",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8ae4eef2.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 600.0
  },
  {
    "content": "#夏日避暑好去处#分享今日打卡的冷门咖啡馆，空调超足人还少！",
    "userNick": "快乐小番茄",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/3ecf791d.png",
    "likeNum": "256",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/bee80ae7.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 1000.0
  },
  {
    "content": "#科技新品# 苹果发布了最新款 iPhone 15，A17 芯片性能提升显著，摄像头系统也进行了全面升级。",
    "userNick": "数码先知",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d77dc0ad.png",
    "likeNum": "800",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/4acfc4a5.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 800.0
  },
  {
    "content": "#周末去哪玩#发现一个绝美露营地，星空太震撼了！",
    "userNick": "旅行小青蛙",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9bd34fff.png",
    "likeNum": "320",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/0b72a27c.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 1200.0
  },
  {
    "content": "#咖啡豆评测#埃塞俄比亚耶加雪菲，柑橘风味明显！",
    "userNick": "咖啡研究所",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8a01b17c.png",
    "likeNum": "180",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/610f6fc3.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 600.0
  },
  {
    "content": "我家主子今天又双叒叕拆家了...",
    "userNick": "喵星人日记",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/844aa82b.png",
    "likeNum": "800",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/126148bf.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 800.0
  },
  {
    "content": "#iPhone15预测# 全系C口+钛金属边框，果粉冲吗？",
    "userNick": "数码先知",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/891fc305.png",
    "likeNum": "1050",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/cadabbca.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 1000.0
  },
  {
    "content": "苏州街边偶遇的蟹黄面，一碗浇了8只蟹的膏黄！",
    "userNick": "碳水教父",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9bd34fff.png",
    "likeNum": "420",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/f36214ee.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 600.0
  },
  {
    "content": "#封神票房破20亿# 乌尔善的选角眼光太毒了！",
    "userNick": "院线雷达",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/ce73f60a.jpg",
    "likeNum": "3800",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/5cdb0696.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 1200.0
  },
  {
    "content": "当北方人第一次见到会飞的蜷螢时的反应...",
    "userNick": "迷惑行为大赏",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/7d986b3a.png",
    "likeNum": "15000",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8d510f14.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 800.0
  },
  {
    "content": "领导说『年轻人要多学习』的真实意思：下班后免费加班三小时。",
    "userNick": "反卷战士",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/007634a8.png",
    "likeNum": "2200",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/6f1a911f.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 600.0
  },
  {
    "content": "建议把『禁止电动车进电梯』标语换成『电动车进电梯会爆炸』，恐惧比道德更有效。",
    "userNick": "人间观察员",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/b2fc4f8d.jpg",
    "likeNum": "6800",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d502c511.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 1000.0
  },
  {
    "content": "#世界那么大# 这次去了冰岛，极光下的蓝湖温泉简直美到窒息！",
    "userNick": "世界旅人",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/fadde619.jpg",
    "likeNum": "700",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/be55b9a0.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 1200.0
  },
  {
    "content": "冷知识：用过期牛奶擦皮鞋，皮质会变得更亮。别问我是怎么知道的。",
    "userNick": "生活黑客",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/38d02bd9.png",
    "likeNum": "950",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/e8681bbc.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 600.0
  },
  {
    "content": "有时候觉得，生活就像一杯茶，苦涩中带着回甘。忙碌的日子里，不妨停下来，泡一杯茶，静静地感受当下的美好。",
    "userNick": "半盏清茶",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/a595874c.png",
    "likeNum": "280",
    "imageUrl": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c7edf17c.jpeg",
    "imageWidth": 800.0,
    "imageHeight": 1000.0
  }
]"""
    }



    /**
     * 获取消息数据的JSON字符串
     * 在实际项目中，这应该从资源文件中读取
     */
    private fun getMessageDataJson(): String {
        return """[
  {
    "userName": "APP官方",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8d0813ca.png",
    "lastMessage": "欢迎来到APP！快来发布你的第一篇内容吧~",
    "time": "刚刚",
    "unreadCount": 1,
    "isOnline": true
  },
  {
    "userName": "旅行小青蛙",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9bd34fff.png",
    "lastMessage": "那个露营地真的太美了！下次一起去吧",
    "time": "5分钟前",
    "unreadCount": 2,
    "isOnline": true
  },
  {
    "userName": "咖啡研究所",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8a01b17c.png",
    "lastMessage": "新到了一批埃塞俄比亚豆子，要试试吗？",
    "time": "1小时前",
    "unreadCount": 0,
    "isOnline": false
  },
  {
    "userName": "数码先知",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d77dc0ad.png",
    "lastMessage": "iPhone 15的评测视频出来了，记得看哦",
    "time": "2小时前",
    "unreadCount": 0,
    "isOnline": true
  },
  {
    "userName": "美食探索家",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/45ad086d.png",
    "lastMessage": "今天发现了一家超棒的川菜馆！",
    "time": "昨天",
    "unreadCount": 0,
    "isOnline": false
  },
  {
    "userName": "健身达人",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/3ecf791d.png",
    "lastMessage": "一起去健身房吗？我办了新卡",
    "time": "昨天",
    "unreadCount": 1,
    "isOnline": true
  },
  {
    "userName": "摄影师小王",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/844aa82b.png",
    "lastMessage": "你的照片拍得真好，能教教我吗？",
    "time": "2天前",
    "unreadCount": 0,
    "isOnline": false
  },
  {
    "userName": "读书分享",
    "userAvatar": "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/891fc305.png",
    "lastMessage": "推荐一本好书《人类简史》",
    "time": "3天前",
    "unreadCount": 0,
    "isOnline": false
  }
]"""
    }
}