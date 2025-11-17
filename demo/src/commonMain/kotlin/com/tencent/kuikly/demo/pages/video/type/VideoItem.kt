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


package com.tencent.kuikly.demo.pages.video.type

import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.CodecModule

class VideoItem (
    var videoId: String = "",
    var nick: String = "",                              // 账号名
    var avatar:String = "",                              // 头像
    var description:String = "",                       // 视频描述
    var videoUrl: String = "",                             // 视频url
    var likeNum: Int = 0,
    var retweetNum: Int = 0,
    var likeStatus: Boolean = false,
    var searchContent: String = "",
    var imgUrl: String = "",
    var commentNum: Int = 0,
    var duration: Int = 0,                            // 视频持续时间
//    var commentList: MutableList<CommentItem>       // 注意到评论区以及回复列表在界面上都是可以新增内容的

    var videoHeight: Int = 0,
    var videoWidth: Int = 0,
    var collectNum: Int = 0
)

fun initFirstVideoItem(entryPageUrl: String): VideoItem {

        return VideoItem(
        videoId = "123",
        nick = "晨间漫步者",
        description = "一辆AE86亮着车灯在镜头前飘逸",
        videoUrl = "https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/fe544048.mp4",
        imgUrl = "https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/2e3488a2.png",
        duration = 0,
        avatar = "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d852717b.jpg"
    )
}

fun parseVideoTransferInfo(transferInfo: String): Pair<Int, Int> {
    val dimensions = transferInfo.split("&").associateBy(
        { it.split("=")[0] }, // Key: videoHeight or videoWidth
        { it.split("=")[1].toInt() } // Value: height or width as Int
    )
    val videoHeight = dimensions["videoHeight"] ?: 0
    val videoWidth = dimensions["videoWidth"] ?: 0
    return Pair(videoHeight, videoWidth)
}