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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @人发布器纯逻辑 JVM 原生单测（androidUnitTest + kotlin-test，不依赖 Android/Robolectric）。
 *
 * 与黑盒 E2E（TC1–TC10）互补：E2E 防「功能被改到用户能感知」，本层防「逻辑悄悄算错但界面没崩」。
 * 重点防线是 [tc_u6_self_consistent_substring]——off-by-one 类回归的常驻断言
 * （对应黑盒 TC9，但这里零设备、秒级、零 flaky）。
 */
class MentionLogicTest {

    // ===== scanMentions：高亮区间 offset 映射 =====

    @Test
    fun tc_u1_single_mention_interval() {
        // "@张三" -> 区间 [0,3]（end exclusive = start + token.length）
        val m = scanMentions("@张三 ")
        assertEquals(1, m.size)
        assertEquals("@张三", m[0].displayName)
        assertEquals(0, m[0].start)
        assertEquals(3, m[0].end)
    }

    @Test
    fun tc_u2_multi_mention_no_drift() {
        // 两个 @人 共存，区间不漂移
        val m = scanMentions("@张三 @李四 ")
        assertEquals(2, m.size)
        assertEquals(listOf(0 to 3, 4 to 7), m.map { it.start to it.end })
    }

    @Test
    fun tc_u3_mention_not_at_start() {
        // @人 嵌在文本中间，偏移仍正确
        val m = scanMentions("说点啥@张三 后面")
        assertEquals(1, m.size)
        assertEquals(3, m[0].start)
        assertEquals(6, m[0].end)
    }

    @Test
    fun tc_u4_partial_query_not_a_mention() {
        // 部分输入 "@张" 不构成完整 mention（token 是 "@张三"）
        assertTrue(scanMentions("@张").isEmpty())
    }

    @Test
    fun tc_u5_repeated_mention() {
        // 同一个 @人 出现多次，都应被扫出
        val m = scanMentions("@张三@张三")
        assertEquals(2, m.size)
        assertEquals(0, m[0].start)
        assertEquals(3, m[1].start)
    }

    @Test
    fun tc_u6_self_consistent_substring() {
        // 自洽：对每个 mention，text.substring(start,end)==displayName。
        // off-by-one（end 少 1）类回归会在此变红——黑盒 TC9 的纯逻辑版。
        val text = "@张三 @李四 "
        scanMentions(text).forEach { mention ->
            assertEquals(mention.displayName, text.substring(mention.start, mention.end))
        }
    }

    // ===== detectMentionTrigger：@ 触发检测 =====

    @Test
    fun tc_u7_trigger_at_at() {
        // 光标紧跟 "@" 后，命中触发，返回 @ 下标
        assertEquals(1, detectMentionTrigger("a@", 2, emptyList()))
    }

    @Test
    fun tc_u8_no_trigger_without_at() {
        // 光标前无 @，不触发
        assertNull(detectMentionTrigger("abc", 3, emptyList()))
    }

    @Test
    fun tc_u9_whitespace_breaks_trigger() {
        // @ 与光标之间有空格，触发链断开
        assertNull(detectMentionTrigger("@ abc", 5, emptyList()))
    }

    @Test
    fun tc_u10_suppress_inside_existing_mention() {
        // @ 落在已完成 mention 内部，不重复触发（避免对 @张三 重复弹候选）
        val mentions = scanMentions("@张三")
        assertNull(detectMentionTrigger("@张三", 2, mentions))
    }

    // ===== filterCandidates：候选前缀过滤 =====

    @Test
    fun tc_u11_prefix_filter() {
        // query="张" -> 只匹配张三
        val c = filterCandidates("张")
        assertEquals(listOf("张三"), c.map { it.first })
    }

    @Test
    fun tc_u12_empty_query_returns_all() {
        // query="" -> 全量候选
        val c = filterCandidates("")
        assertEquals(KNOWN_MENTIONS.size, c.size)
    }
}
