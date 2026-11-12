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

package com.tencent.kuikly.core.render.android.expand.component

import android.text.Spanned
import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * mention 两段式删除 + 组合态守卫的纯逻辑单测（Robolectric，JVM 内运行，不依赖真机）。
 * 覆盖：
 *  - parseMentionSpans（JSON 解析）
 *  - interceptMentionBackspace（第一步：光标落在 mention 内 → 选区设整段）
 *  - deleteMentionSelection（第二步：选区覆盖 mention → 真删）
 *  - onKeyDown 的 isInComposition 守卫（组合态退格交给输入法、不触发两段式删除）
 *
 * 注：颜色用 32 位有符号 Int 范围内的小值（颜色不参与断言，仅占位），
 * 避免超出 Int.MAX_VALUE 导致 org.json 截断。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class KRTextFieldViewMentionTest {

    private lateinit var view: KRTextFieldView

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        view = KRTextFieldView(context, null)
    }

    // ---- parseMentionSpans：纯 JSON 解析逻辑 ----

    @Test
    fun parseMentionSpans_empty_returnsNull() {
        assertNull(view.parseMentionSpans(""))
        assertNull(view.parseMentionSpans("[]"))
    }

    @Test
    fun parseMentionSpans_malformed_returnsNull() {
        assertNull(view.parseMentionSpans("not-json"))
    }

    @Test
    fun parseMentionSpans_valid_returnsTriples() {
        val result = view.parseMentionSpans("[[0,3,255],[4,7,128]]")
        assertEquals(
            listOf(Triple(0, 3, 255), Triple(4, 7, 128)),
            result,
        )
    }

    // ---- interceptMentionBackspace：两段式第一步，光标落在 mention 内 → 选区设整段 ----

    @Test
    fun interceptMentionBackspace_noMentionData_returnsFalse() {
        view.setText("hello")
        view.setSelection(5)
        assertFalse(view.interceptMentionBackspace())
    }

    @Test
    fun interceptMentionBackspace_cursorInsideMention_selectsWholeMention() {
        // "@张三" 占 [0,3)，光标在 2 处，退格应把选区设到 [0,3)
        view.setText("@张三")
        view.setMentionSpans("[[0,3,255]]")
        view.setSelection(2)
        assertTrue(view.interceptMentionBackspace())
        assertEquals(0, view.selectionStart)
        assertEquals(3, view.selectionEnd)
    }

    @Test
    fun interceptMentionBackspace_alreadyHasSelection_returnsFalse() {
        // 已有选区视为第二次退格，不再拦截
        view.setText("@张三")
        view.setMentionSpans("[[0,3,255]]")
        view.setSelection(0, 3)
        assertFalse(view.interceptMentionBackspace())
    }

    @Test
    fun interceptMentionBackspace_cursorOutsideMention_returnsFalse() {
        // 有 mention 数据，但光标在普通字符上，不拦截
        view.setText("ab@张三")
        view.setMentionSpans("[[2,5,255]]")
        view.setSelection(1)
        assertFalse(view.interceptMentionBackspace())
    }

    // ---- deleteMentionSelection：两段式第二步，选区覆盖 mention → 真删 ----

    @Test
    fun deleteMentionSelection_selectionCoversMention_deletesIt() {
        view.setText("ab@张三cd")
        view.setMentionSpans("[[2,5,255]]")
        view.setSelection(2, 5)
        assertTrue(view.deleteMentionSelection())
        assertEquals("abcd", view.text.toString())
    }

    @Test
    fun deleteMentionSelection_selectionNotCoveringMention_returnsFalse() {
        view.setText("ab@张三cd")
        view.setMentionSpans("[[2,5,255]]")
        view.setSelection(0, 2)
        assertFalse(view.deleteMentionSelection())
    }

    // ---- onKeyDown + isInComposition 守卫：组合态退格交给输入法，不触发两段式删除 ----

    @Test
    fun onKeyDown_inComposition_doesNotSelectMention() {
        // 组合态（editableText 带 SPAN_COMPOSING）下，即使光标落在 mention 内，
        // 退格也应交给输入法，不把选区设到 mention 整段（即不触发两段式删除）。
        view.setText("@张三")
        view.setMentionSpans("[[0,3,255]]")
        view.setSelection(2)
        view.editableText?.setSpan(Any(), 0, view.editableText!!.length, Spanned.SPAN_COMPOSING)
        view.onKeyDown(KeyEvent.KEYCODE_DEL, KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0))
        // 守卫生效：选区仍折叠，未被设到 mention 整段 [0,3)
        assertTrue(view.selectionStart == view.selectionEnd)
        assertFalse(view.selectionStart == 0 && view.selectionEnd == 3)
    }

    @Test
    fun onKeyDown_notInComposition_selectsMention() {
        // 对照：非组合态下，光标落在 mention 内 → 退格把选区设到整段（原两段式逻辑）。
        view.setText("@张三")
        view.setMentionSpans("[[0,3,255]]")
        view.setSelection(2)
        view.onKeyDown(KeyEvent.KEYCODE_DEL, KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0))
        assertEquals(0, view.selectionStart)
        assertEquals(3, view.selectionEnd)
    }
}
