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

package com.tencent.kuikly.demo.pages.app.model

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.demo.pages.base.ktx.map

data class AppFeedModel(
    val appId: String,
    val categoryId: String,
    val content: String,
    var userInfo: AppUserInfo,
    val picUrl: List<String>,
    val forwardContent: String?,
    val forwardNick: String?,
    val forwardUserId: String?,
    val forwardPicUrl: List<String>?,
    val forwardAppId: String?,
    val forwardVideoUrl: String?,
    val containForward: Boolean,
    val videoUrl: String,
    val tail: String,
    val createTime: Int,
    val likeStatus: Int,
    val forwardNum: Int,
    val likeNum: Int,
    val commentNum: Int
) {
    companion object {

        private const val KEY_FEED = "feed"
        private const val KEY_USER = "user"
        private const val KEY_FEED_APP_ID = "appId"
        private const val KEY_FEED_CATEGORY_ID = "categoryId"
        private const val KEY_FEED_CONTENT = "content"
        private const val KEY_FEED_PIC_URL = "picurl"

        private const val KEY_FEED_FORWARD_CONTENT = "zfContent"
        private const val KEY_FEED_FORWARD_NICK = "zfNick"
        private const val KEY_FEED_FORWARD_USER_ID = "zfUserId"
        private const val KEY_FEED_FORWARD_APP_ID = "zfAppId"
        private const val KEY_FEED_CONTAIN_FORWARD = "containZf"
        private const val KEY_FEED_FORWARD_PIC_URL = "zfPicurl"
        private const val KEY_FEED_FORWARD_VIDEO_URL = "zfVedioUrl"

        private const val KEY_FEED_VIDEO_URL = "vediourl"
        private const val KEY_FEED_CREATE_TAIL = "tail"
        private const val KEY_FEED_CREATE_TIME = "createtime"
        private const val KEY_FEED_LIKE_STATUS = "zanStatus"
        private const val KEY_FEED_FORWARD_NUM = "zhuanfaNum"
        private const val KEY_FEED_LIKE_NUM = "likeNum"
        private const val KEY_FEED_COMMENT_NUM = "commentNum"

        private fun parsePictureUrl(urlStr: String) : String {
            return urlStr
                .removePrefix("[")
                .removeSuffix("]")
                .trim()
                .trim('\'')
        }

        fun fromJson(jsonObject: JSONObject): AppFeedModel? {
            val feedObj = jsonObject.optJSONObject(KEY_FEED)
            val userObj = jsonObject.optJSONObject(KEY_USER)
            if (feedObj == null || userObj == null) {
                return null
            }

            return AppFeedModel(
                appId = feedObj.optString(KEY_FEED_APP_ID, ""),
                categoryId = feedObj.optString(KEY_FEED_CATEGORY_ID, ""),
                content = feedObj.optString(KEY_FEED_CONTENT, ""),
                picUrl = feedObj.optJSONArray(KEY_FEED_PIC_URL)?.map<String, String> {
                    return@map parsePictureUrl(it)
                } ?: listOf(),
                userInfo = AppUserInfo.fromJson(userObj),
                forwardContent = feedObj.optString(KEY_FEED_FORWARD_CONTENT),
                forwardNick = feedObj.optString(KEY_FEED_FORWARD_NICK),
                forwardUserId = feedObj.optString(KEY_FEED_FORWARD_USER_ID),
                forwardAppId = feedObj.optString(KEY_FEED_FORWARD_APP_ID),
                containForward = feedObj.optBoolean(KEY_FEED_CONTAIN_FORWARD),
                forwardPicUrl = feedObj.optJSONArray(KEY_FEED_FORWARD_PIC_URL)?.map<String, String> {
                    return@map parsePictureUrl(it)
                },
                forwardVideoUrl = "",
                videoUrl = feedObj.optString(KEY_FEED_VIDEO_URL),
                tail = feedObj.optString(KEY_FEED_CREATE_TAIL),
                createTime = feedObj.optInt(KEY_FEED_CREATE_TIME),
                likeStatus = 0,
                forwardNum = feedObj.optInt(KEY_FEED_FORWARD_NUM),
                likeNum = feedObj.optInt(KEY_FEED_LIKE_NUM),
                commentNum = feedObj.optInt(KEY_FEED_COMMENT_NUM)
            )
        }

    }
}
