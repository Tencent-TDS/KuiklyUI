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

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.demo.pages.waterfall_app.models.MessageItem
import com.tencent.kuikly.demo.pages.waterfall_app.models.WaterFallItem
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 瀑布流数据提供者 - 支持异步数据加载
 * 从磁盘文件异步加载数据，模拟网络请求场景
 */
internal object WaterfallDataProvider {
    
    private const val TAG = "WaterfallDataProvider"
    
    /**
     * 异步加载瀑布流数据
     * 使用Kuikly内建协程方式，将回调式API转换为挂起函数
     * 
     * @param page 页码，从1开始
     * @param pageSize 每页数据量
     * @return 瀑布流数据加载结果
     */
    suspend fun loadWaterfallData(page: Int = 1, pageSize: Int = 20): Result<List<Map<String, Any>>> {
        return suspendCoroutine { continuation ->
            try {
                KLog.i(TAG, "开始异步加载瀑布流数据 - 页码: $page, 每页: $pageSize")
                
                // 模拟异步数据加载
                loadWaterfallDataAsync(page, pageSize) { result ->
                    if (result.isSuccess) {
                        val dataItems = result.getOrNull() ?: emptyList()
                        val mapData = dataItems.map { item ->
                            mapOf(
                                "content" to item.content,
                                "userNick" to item.userNick,
                                "userAvatar" to item.userAvatar,
                                "likeNum" to item.likeNum,
                                "imageUrl" to item.imageUrl,
                                "imageWidth" to item.imageWidth.toFloat(),
                                "imageHeight" to item.imageHeight.toFloat()
                            )
                        }
                        KLog.i(TAG, "瀑布流数据加载成功 - 返回 ${mapData.size} 条数据")
                        continuation.resume(Result.success(mapData))
                    } else {
                        val error = result.exceptionOrNull() ?: Exception("未知错误")
                        KLog.e(TAG, "瀑布流数据加载失败: ${error.message}")
                        continuation.resume(Result.failure(error))
                    }
                }
            } catch (e: Exception) {
                KLog.e(TAG, "瀑布流数据加载异常: ${e.message}")
                continuation.resumeWithException(e)
            }
        }
    }
    
    /**
     * 内部回调式异步加载方法
     * 模拟真实的异步数据加载场景
     */
    private fun loadWaterfallDataAsync(
        page: Int, 
        pageSize: Int, 
        callback: (Result<List<DataLoader.WaterfallDataItem>>) -> Unit
    ) {
        // 模拟异步操作，实际项目中这里可能是网络请求或数据库查询
        try {
            // 使用DataLoader同步方法，在实际项目中这里应该是真正的异步操作
            val result = DataLoader.loadWaterfallDataSync(page, pageSize)
            callback(result)
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }
    
    /**
     * 异步创建瀑布流数据项
     * 使用Kuikly内建协程，调用挂起函数进行数据处理
     * 
     * @param page 页码，从1开始
     * @param pageSize 每页数据量，默认20条
     * @return 瀑布流数据项列表的加载结果
     */
    suspend fun createWaterfallItems(page: Int = 1, pageSize: Int = 20): Result<List<WaterFallItem>> {
        return try {
            KLog.i(TAG, "开始创建瀑布流数据项 - 页码: $page, 每页: $pageSize")
            
            // 调用挂起函数异步加载数据
            val dataResult = loadWaterfallData(page, pageSize)
            
            if (dataResult.isSuccess) {
                val mockData = dataResult.getOrNull() ?: emptyList()
                val waterfallItems = mockData.map { data ->
                    WaterFallItem().apply {
                        content = data["content"] as String
                        userNick = data["userNick"] as String
                        userAvatar = data["userAvatar"] as String
                        likeNum = data["likeNum"] as String
                        imageUrl = data["imageUrl"] as String
                        imageWidth = data["imageWidth"] as Float
                        imageHeight = data["imageHeight"] as Float
                        bgColor = Color.WHITE
                    }
                }
                KLog.i(TAG, "瀑布流数据项创建成功 - 创建 ${waterfallItems.size} 个数据项")
                Result.success(waterfallItems)
            } else {
                val error = dataResult.exceptionOrNull() ?: Exception("数据加载失败")
                KLog.e(TAG, "瀑布流数据项创建失败: ${error.message}")
                Result.failure(error)
            }
        } catch (e: Exception) {
            KLog.e(TAG, "瀑布流数据项创建异常: ${e.message}")
            Result.failure(e)
        }
    }

    
    /**
     * 异步加载消息数据
     * 使用Kuikly内建协程，将回调式API转换为挂起函数
     * 
     * @return 消息数据列表的加载结果
     */
    suspend fun loadMessageData(): Result<List<MessageItem>> {
        return suspendCoroutine { continuation ->
            try {
                KLog.i(TAG, "开始异步加载消息数据")
                
                // 模拟异步数据加载
                loadMessageDataAsync { result ->
                    if (result.isSuccess) {
                        val dataItems = result.getOrNull() ?: emptyList()
                        val messageItems = dataItems.map { item ->
                            MessageItem().apply {
                                userName = item.userName
                                userAvatar = item.userAvatar
                                lastMessage = item.lastMessage
                                time = item.time
                                unreadCount = item.unreadCount
                                isOnline = item.isOnline
                            }
                        }
                        KLog.i(TAG, "消息数据加载成功 - 返回 ${messageItems.size} 条消息")
                        continuation.resume(Result.success(messageItems))
                    } else {
                        val error = result.exceptionOrNull() ?: Exception("未知错误")
                        KLog.e(TAG, "消息数据加载失败: ${error.message}")
                        continuation.resume(Result.failure(error))
                    }
                }
            } catch (e: Exception) {
                KLog.e(TAG, "消息数据加载异常: ${e.message}")
                continuation.resumeWithException(e)
            }
        }
    }
    
    /**
     * 内部回调式消息数据加载方法
     */
    private fun loadMessageDataAsync(callback: (Result<List<DataLoader.MessageDataItem>>) -> Unit) {
        try {
            val result = DataLoader.loadMessageDataSync()
            callback(result)
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }

    


}