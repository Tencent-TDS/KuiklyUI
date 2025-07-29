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
import com.tencent.kuikly.core.views.*

class ExtRichTextAttr: RichTextAttr() {

    var internalAccessibilitySet = false
    var hasCustomAccessibilitySet = false
    var lastAccessibility = ""

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
        var accessibility = ""
        getSpans().forEach {
            if (it is TextSpan) {
                accessibility += it.getText()
            }
        }
        return accessibility
    }
}

class ExtRichTextEvent: RichTextEvent()

class ExtRichTextView: RichTextView() {

    override fun measure(
        node: FlexNode,
        width: Float,
        height: Float,
        measureOutput: MeasureOutput
    ) {
        willBuildValuesPropValue()
        super.measure(node, width, height, measureOutput)
    }
    fun willBuildValuesPropValue() {
        (getViewAttr() as? ExtRichTextAttr)?.also { extAttr->
            if (!extAttr.hasCustomAccessibilitySet) {
                val accessibility = extAttr.getDefaultAccessibility()
                extAttr.internalAccessibilitySet = true
                extAttr.accessibility(accessibility)
                extAttr.internalAccessibilitySet = false
            }
        }
    }

    override fun createAttr(): RichTextAttr {
        return ExtRichTextAttr()
    }

    override fun createEvent(): RichTextEvent {
        return ExtRichTextEvent()
    }
}