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

package com.tencent.kuikly.demo.pages.base.extension

import com.tencent.kuikly.core.base.attr.IStyleAttr
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.layout.MeasureOutput
import com.tencent.kuikly.core.views.TextAttr
import com.tencent.kuikly.core.views.TextEvent
import com.tencent.kuikly.core.views.TextView

class ExtTextAttr: TextAttr() {

    var internalAccessibilitySet = false
    internal var hasCustomAccessibilitySet = false
    internal var lastAccessibility = ""
    internal var internalText = ""

    override fun accessibility(accessibility: String): IStyleAttr {
        if (!internalAccessibilitySet) {
            hasCustomAccessibilitySet = true
        }
        if (lastAccessibility == accessibility) {
            return this
        }
        lastAccessibility = accessibility
        return super.accessibility(accessibility)
    }

    fun getDefaultAccessibility(): String {
       return internalText
    }

    override fun value(value: String): TextAttr {
        return text(value)
    }

    override fun text(text: String): TextAttr {
        internalText = text
        return super.text(text)
    }

}

class ExtTextEvent: TextEvent()

class ExtTextView: TextView() {

    override fun measure(
        node: FlexNode,
        width: Float,
        height: Float,
        measureOutput: MeasureOutput
    ) {
        val extAttr = getViewAttr() as? ExtTextAttr
        extAttr?.also {
            if (!it.hasCustomAccessibilitySet) {
                it.internalAccessibilitySet = true
                it.accessibility(it.getDefaultAccessibility())
                it.internalAccessibilitySet = false
            }
        }
        super.measure(node, width, height, measureOutput)
    }

    override fun createAttr(): TextAttr {
        return ExtTextAttr()
    }

    override fun createEvent(): TextEvent {
        return ExtTextEvent()
    }

}
