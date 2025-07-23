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

package com.tencent.kuikly.core.render.android.expand.component.list

import android.content.Context
import android.view.View
import com.tencent.kuikly.core.render.android.expand.component.KRView

class KRRecyclerContentView(context: Context) : KRView(context) {

    init {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        setFocusable(false)
    }

    var addChildCallback: ((View) -> Unit)? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
            runAddChildLazyTask()
            addChildLazyTasks.clear()
        }

    private val addChildLazyTasks = mutableListOf<View>()

    override fun addView(child: View, index: Int) {
        super.addView(child, index)
        if (addChildCallback == null) {
            addChildLazyTasks.add(child)
        } else {
            addChildCallback?.invoke(child)
        }
    }

    private fun runAddChildLazyTask() {
        addChildLazyTasks.forEach {
            addChildCallback?.invoke(it)
        }
    }

    override val reusable: Boolean
        get() = false

    companion object {
        const val VIEW_NAME = "KRScrollContentView"
    }
}