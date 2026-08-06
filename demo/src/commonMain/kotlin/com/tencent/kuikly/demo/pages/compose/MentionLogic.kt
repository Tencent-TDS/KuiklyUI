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

package com.tencent.kuikly.demo.pages.compose

/**
 * 发布器 @人 功能的「纯逻辑」层——不依赖 Compose / Android，仅操作 String / Int。
 *
 * 从 [MentionPublisherDemo] 抽出，便于在 JVM 上做原生单测（androidUnitTest + kotlin-test），
 * 专抓「UI 看着对、内部区间已错位」类隐蔽回归（off-by-one 等）。
 * 可见性为 internal：模块内可测、不进公开 API。
 */

/** 已知候选名单：name -> userId */
internal val KNOWN_MENTIONS = listOf(
    "张三" to "u_zhangsan",
    "李四" to "u_lisi",
    "王五" to "u_wangwu",
    "Tom" to "u_tom",
)

/**
 * Mention 元数据。约束：text.substring(start, end) == displayName
 */
internal data class Mention(
    val userId: String,
    val displayName: String,   // 例如 "@张三"
    val start: Int,
    val end: Int,              // exclusive
)

/**
 * 正则重扫：在 text 中找出所有已知 @昵称 的出现位置，生成 Mention 列表。
 * 对齐官方思路——每次文本变化后重扫，下标自动正确，无需手动前后移。
 */
internal fun scanMentions(text: String): List<Mention> {
    val result = mutableListOf<Mention>()
    for ((name, userId) in KNOWN_MENTIONS) {
        val token = "@$name"
        var from = 0
        while (true) {
            val pos = text.indexOf(token, from)
            if (pos < 0) break
            result.add(Mention(userId, token, pos, pos + token.length))
            from = pos + token.length
        }
    }
    result.sortBy { it.start }
    return result
}

/**
 * 检测光标前是否有 @ 触发：向回找 @，中间不能有空格；@ 前必须是文本起点或空格；
 * 且 @ 不能落在某个已有 mention 内部（避免对已完成的 @人 重复弹候选）。
 * 返回 @ 的下标，或 null。
 */
internal fun detectMentionTrigger(
    text: String,
    cursor: Int,
    mentions: List<Mention>,
): Int? {
    if (cursor <= 0) return null
    var i = cursor - 1
    while (i >= 0 && text[i] != '@' && !text[i].isWhitespace()) {
        i--
    }
    if (i < 0 || text[i] != '@') return null
    // @ 落在已有 mention 内部则不触发（避免对已完成的 @人 重复弹候选）
    if (mentions.any { it.start <= i && i < it.end }) return null
    return i
}

/**
 * 候选过滤：query 前缀匹配已知名单（对应 UI 里 KNOWN_MENTIONS.filter{startsWith(q)}）。
 */
internal fun filterCandidates(query: String): List<Pair<String, String>> {
    return KNOWN_MENTIONS.filter { it.first.startsWith(query) }
}
