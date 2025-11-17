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


package com.tencent.kuikly.demo.pages.video.feed

import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

data class CommentItem (
    var commentId: String,
    var avatar: String,                     // 评论者头像
    var nick: String,                       // 评论者昵称
    var commentContent: String,             // 评论内容
    var likeNum: Int,                       // 评论被点赞数据
    var likeStatus: Boolean,                // 是否点赞
    var location: String,                   // 评论者ip
    var time: String                        // 评论时间
){
    companion object {
        private const val KEY_COMMENT = "coment"
        private const val KEY_COMMENT_ID = "commentId"
        private const val KEY_AVATER = "avatar"
        private const val KEY_NICK = "nick"
        private const val KEY_COMMENTCONTENT = "commentContent"
        private const val KEY_LIKENUM = "likeNum"
        private const val KEY_LIKESTATUS = "likeStatus"
        private const val KEY_LOCATION = "location"
        private const val KEY_TIME = "time"

        fun fromJson(jsonObject: JSONObject): CommentItem? {
            KLog.i("commentItemJsonOBJ", jsonObject.toString())
            return CommentItem(
                commentId = jsonObject.optString(KEY_COMMENT_ID,""),
                avatar = jsonObject.optString(KEY_AVATER, ""),
                nick = jsonObject.optString(KEY_NICK, ""),
                commentContent = jsonObject.optString(KEY_COMMENTCONTENT,""),
                likeNum = jsonObject.optInt(KEY_LIKENUM, 0),
                likeStatus = jsonObject.optBoolean(KEY_LIKESTATUS,false),
                location = jsonObject.optString(KEY_LOCATION, ""),
                time = jsonObject.optString(KEY_TIME, "")
            )
        }

        fun constructSingle(jsonObject: JSONObject): CommentItem? {
            return CommentItem(
                commentId = jsonObject.optString(KEY_COMMENT_ID,""),
                avatar = jsonObject.optString(KEY_AVATER, ""),
                nick = jsonObject.optString(KEY_NICK, ""),
                commentContent = jsonObject.optString(KEY_COMMENTCONTENT,""),
                likeNum = jsonObject.optInt(KEY_LIKENUM, 0),
                likeStatus = jsonObject.optBoolean(KEY_LIKESTATUS, false),
                location = jsonObject.optString(KEY_LOCATION, ""),
                time = jsonObject.optString(KEY_TIME, "")
            )
        }

        fun toJson(item: CommentItem): JSONObject? {
            val jsonObject = JSONObject()
            jsonObject.put(KEY_COMMENT_ID, item.commentId)
            jsonObject.put(KEY_AVATER, item.avatar)
            jsonObject.put(KEY_NICK, item.nick)
            jsonObject.put(KEY_COMMENTCONTENT, item.commentContent)
            jsonObject.put(KEY_LIKENUM, item.likeNum)
            jsonObject.put(KEY_LIKESTATUS, item.likeStatus)
            jsonObject.put(KEY_LOCATION, item.location)
            jsonObject.put(KEY_TIME, item.time)
            return jsonObject
        }
    }
}