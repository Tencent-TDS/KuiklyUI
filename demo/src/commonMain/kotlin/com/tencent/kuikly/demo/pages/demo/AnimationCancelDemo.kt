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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("AnimationCancelDemo")
internal class AnimationCancelDemo : Pager() {

    var dataList: ObservableList<String> by observableList()
    var viewRef: ViewRef<DivView>? = null
    var touchBeginX = 0f

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
                autoDarkEnable(false)
            }
            View {
                attr {
                    flexDirectionRow()
                    marginTop(100f)
                }
                Text {
                    attr {
                        marginLeft(50f)
                        size(100f, 100f)
                        backgroundColor(Color.RED)
                        text("start")
                    }
                    event {
                        click {
                            // 启动transform动画
                            ctx.viewRef?.view?.animateToAttr(Animation.linear(2f), attrBlock = {
                                transform(Translate(1f, 0f))
                            }, completion = { finish ->
                                KLog.i("AnimationCancelDemo", "启动动画结束: $finish")
                            })
                        }
                    }
                }

                Text {
                    attr {
                        marginLeft(50f)
                        size(100f, 100f)
                        backgroundColor(Color.RED)
                        text("reset")
                    }
                    event {
                        click {
                            // 启动transform动画
                            ctx.viewRef?.view?.animateToAttr(Animation.linear(0f), attrBlock = {
                                transform(Translate(0f, 0f))
                            }, completion = { finish ->
                                KLog.i("AnimationCancelDemo", "reset动画结束: $finish")
                            })
                        }
                    }
                }
            }

            // 被动画对象
            View {
                ref {
                    ctx.viewRef = it
                }
                attr {
                    marginTop(300f)
                    marginLeft(100f)
                    size(100f, 100f)
                    backgroundColor(Color.YELLOW)
                    transform(Translate(0f, 0f))
//                    transform(Translate(1f, 0f))
                }
                event {
                    pan { it ->
                        if (it.state == "start") {
                            ctx.touchBeginX = it.x
                            val deltaX = it.pageX - 100f - ctx.touchBeginX
                            ctx.viewRef?.view?.animateToAttr(Animation.linear(0f), attrBlock = {
                                transform(Translate(deltaX / 100f))
                            }, completion = {
                                KLog.i("AnimationCancelDemo", "0秒取消动画结束: $it")
                            })
                        } else if (it.state == "move") {
                            // 手势驱动动画
                            val deltaX = it.pageX - 100f - ctx.touchBeginX
                            ctx.viewRef?.view?.attr {
                                transform(Translate(deltaX / 100f))
                            }
                        } else if (it.state == "end") {
                            // 手势结束，继续动画
                            ctx.viewRef?.view?.animateToAttr(Animation.linear(2f), attrBlock = {
                                transform(Translate(2f))
                            }, completion = {
                                KLog.i("AnimationCancelDemo", "2秒的结尾结束: $it")
                            })
                        }
                    }
                }
            }
        }
    }
}
