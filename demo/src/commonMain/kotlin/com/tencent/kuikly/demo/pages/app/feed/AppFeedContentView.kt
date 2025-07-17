/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.kuikly.demo.pages.app.feed

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.app.common.ParsedText
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager

internal class AppFeedContentView: ComposeView<AppFeedContentViewAttr, AppFeedContentViewEvent>() {

    companion object {
        private const val MAX_SHOW_TEXT_LENGTH = 150
    }

    private var content by observable("")
    private var colorScheme by observable(ThemeManager.colorScheme)
    private lateinit var eventCallbackRef: CallbackRef

    override fun viewDestroyed() {
        super.viewDestroyed()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(ThemeManager.SKIN_CHANGED_EVENT, eventCallbackRef)
    }


    override fun createEvent(): AppFeedContentViewEvent {
        return AppFeedContentViewEvent()
    }

    override fun createAttr(): AppFeedContentViewAttr {
        return AppFeedContentViewAttr()
    }

    override fun created() {
        super.created()
        initContent()
        eventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(ThemeManager.SKIN_CHANGED_EVENT) { _ ->
                colorScheme = ThemeManager.colorScheme
            }
    }

    private fun initContent() {
        content = if (attr.content.length > MAX_SHOW_TEXT_LENGTH) {
            attr.content.substring(0, MAX_SHOW_TEXT_LENGTH - 2) + "... 全文"
        } else {
            attr.content
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
//                    alignItemsCenter()
                    margin(top = 5.0f, left = 15.0f, right = 15.0f, bottom = 5.0f)
                }
                ParsedText {
                    attr {
                        text(ctx.content)
                        color(ctx.colorScheme.feedContentText)
                        fontSize(15.0f)

                        matchText {
                            pattern = "\\[(@[^:]+):([^\\]]+)\\]"
                            color = ctx.colorScheme.feedContentQuoteText
                            fontSize = 15.0f
                            rendText = fun(str, pattern): Map<String, String> {
                                val result = mutableMapOf<String, String>()
                                val regExp = Regex(pattern)
                                val matchResult = regExp.find(str)
                                if (matchResult != null) {
                                    result["display"] = matchResult.groups[1]?.value ?: ""
                                    result["value"] = matchResult.groups[2]?.value ?: ""
                                }
                                return result
                            }
                            onTap = fun(content, contentId) {
                                // todo: 跳转
                            }
                        }

                        matchText {
                            pattern = "#.*?#"
                            color = ctx.colorScheme.feedContentQuoteText
                            fontSize = 15.0f
                            rendText = fun(str, pattern): Map<String, String> {
                                val result = mutableMapOf<String, String>()
                                val idStr = str.substring(str.indexOf(":") + 1, str.lastIndexOf("#"))
                                val showStr = str.substring(str.indexOf("#"), str.lastIndexOf("#") + 1).replace(
                                    ":$idStr", "")
                                result["display"] = showStr
                                result["value"] = idStr
                                return result
                            }
                            onTap = fun(content, contentId) {
                                // todo: 跳转
                            }
                        }

                        matchText {
                            pattern = "(\\\\[/).*?(\\\\])"
                            color = ctx.colorScheme.feedContentText
                            fontSize = 15.0f
                            rendText = fun(str, pattern): Map<String, String> {
                                val result = mutableMapOf<String, String>()
                                var mEmoji2 = ""
                                val regExp = Regex("(\\\\[/)|(\\\\])")
                                val mEmoji = str.replace(regExp, "")
                                val mEmojiNew = mEmoji.toInt()
                                mEmoji2 = StringBuilder().append(mEmojiNew.toChar()).toString()
                                result["display"] = mEmoji2
                                return result
                            }
                        }

                        matchText {
                            pattern = "全文"
                            fontSize = 15.0f
                            color = ctx.colorScheme.feedContentQuoteText
                            rendText = fun(str, pattern): Map<String, String> {
                                val result = mutableMapOf<String, String>()
                                result["display"] = "全文"
                                result["value"] = "全文"
                                return result
                            }
                            onTap = fun(content, contentId) {
                                // todo: 展开全文
                            }
                        }

                    }

                }
            }
        }
    }
}

internal class AppFeedContentViewAttr : ComposeAttr() {
    var content: String = ""
        set(value) {
            field = if (value.length > 150) {
                value.substring(0, 148) + " ... 全文"
            } else {
                value
            }
        }
}

internal class AppFeedContentViewEvent : ComposeEvent()

internal fun ViewContainer<*, *>.AppFeedContent(init: AppFeedContentView.() -> Unit) {
    addChild(AppFeedContentView(), init)
}