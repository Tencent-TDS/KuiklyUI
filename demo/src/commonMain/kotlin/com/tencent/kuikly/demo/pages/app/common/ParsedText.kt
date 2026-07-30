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

package com.tencent.kuikly.demo.pages.app.common

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Span

internal class ParsedTextView: ComposeView<ParsedTextViewAttr, ParsedTextViewEvent>() {
    
    override fun createEvent(): ParsedTextViewEvent {
        return ParsedTextViewEvent()
    }

    override fun createAttr(): ParsedTextViewAttr {
        return ParsedTextViewAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            RichText {
                attr {
                    text(ctx.attr.text)
                    color(ctx.attr.color)
                    fontSize(ctx.attr.fontSize)
                }
                // 按正则切割字符串
                var newText = ctx.attr.text
                ctx.attr.parse.forEach { matchText ->
                    val regexPattern = Regex(matchText.pattern)
                    val parts = newText.split(regexPattern).filter { it.isNotEmpty() }
                    var combineText = ""
                    var startIndex = 0
                    var endIndex = 0
                    parts.forEach {
                        endIndex = newText.indexOf(it, startIndex)
                        if (startIndex < endIndex) {
                            combineText = "$combineText%%%%${newText.substring(startIndex, endIndex)}%%%%$it"
                        } else {
                            combineText += it
                        }
                        startIndex = endIndex + it.length
                    }
                    if (startIndex < newText.length) {
                        val substring = newText.substring(startIndex, newText.length)
                        if (regexPattern.matches(substring)) {
                            regexPattern.findAll(substring).iterator().forEach {
                                it.groups[0]?.value?.let { value ->
                                    combineText += "%%%%$value%%%%"
                                }
                            }
                        } else {
                            combineText += substring
                        }
                    }
                    newText = combineText
                }
                val splits = newText.split("%%%%").filter { it.isNotEmpty() }
                splits.forEach { spanStr ->
                    var hasMatch = false
                    ctx.attr.parse.forEach {matchText ->
                        if (matchText.type == ParsedType.CUSTOM) {
                            val customRegExp = Regex(matchText.pattern)
                            if (customRegExp.matches(spanStr)) {
                                hasMatch = true
                                val result  = matchText.rendText?.let {
                                    it.invoke(spanStr, matchText.pattern)
                                }
                                if (result != null) {
                                    Span {
                                        text("${result["display"]}")
                                        color(matchText.color)
                                        fontSize(matchText.fontSize)
                                        click {
                                            matchText.onTap?.invoke(result["display"], result["value"])
                                        }
                                    }
                                } else {
                                    Span {
                                        text(spanStr)
                                        color(matchText.color)
                                        fontSize(matchText.fontSize)
                                        click {
                                            matchText.onTap?.invoke(spanStr, null)
                                        }
                                    }
                                }
                            }
                            return@forEach
                        }
                    }
                    if (!hasMatch) {
                        Span {
                            text(spanStr)
                        }
                    }
                }
            }
        }
    }
}

internal class ParsedTextViewAttr : ComposeAttr() {
    var text: String = ""
    var parse = mutableListOf<MatchText>()
    var color by observable(Color.BLACK)
    var fontSize = 15.0f

    fun text(text: String): ParsedTextViewAttr {
        this.text = text
        return this
    }

    fun matchText(init: MatchText.() -> Unit): ParsedTextViewAttr {
        val matchText = MatchText()
        matchText.init()
        parse.add(matchText)
        return this
    }

    fun color(color: Color): ParsedTextViewAttr {
        this.color = color
        return this
    }

    fun fontSize(fontSize: Float): ParsedTextViewAttr {
        this.fontSize = fontSize
        return this
    }

}

internal class ParsedTextViewEvent : ComposeEvent()

internal fun ViewContainer<*, *>.ParsedText(init: ParsedTextView.() -> Unit) {
    addChild(ParsedTextView(), init)
}

enum class ParsedType {
    EMAIL, PHONE, URL, CUSTOM
}

typealias RenderTextFun = (String, String) -> Map<String, String>
typealias TapEventFun = (String?, String?) -> Unit

class MatchText {
    var type: ParsedType = ParsedType.CUSTOM
    lateinit var pattern: String
    var rendText: RenderTextFun? = null
    var onTap: TapEventFun? = null
    var color: Color = Color.BLACK
    var fontSize: Float = 15.0f
}