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

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.BackPressCallback
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button

/**
 * 该页面用于测试返回键处理
 *
 * 提供“进入编辑态”按钮，点击后进行编辑态。并启用对BackPress的监听。
 * 监听一次BackPress后退出编辑态状态，
 *
 */
@Page("BackPressHandlerPager")
internal class BackPressHandlerPager : BasePager() {

    private var inEditMode by observable(false)

    private val singleShotBack = object : BackPressCallback() {
        override fun handleOnBackPressed() {
            // 退出编辑态并移除自身，消费一次返回
            inEditMode = false
            getBackPressHandler().removeCallback(this)
        }
    }
    private val persistentBack = object : BackPressCallback() {
        override fun handleOnBackPressed() {
            // 持久消费返回，不自动移除
        }
    }
    
    override fun body(): ViewBuilder {
        val ctx = this

        return {
            View {
                attr {
                    width(ctx.pageData.pageViewWidth)
                    height(ctx.pageData.pageViewHeight)
                }
                View {
                    attr {
                        marginTop(150.0f)
                        marginLeft(16.0f)
                        marginRight(16.0f)
                    }
                    Button {
                        attr {
                            height(100f)
                            backgroundColor(Color.GREEN)
                            titleAttr {
                                text(if (ctx.inEditMode) "BackPress拦截生效中，触发BackPress可返回 " else "BackPress拦截未生效，启用单次拦截BackPress")
                            }
                        }

                        event {
                            click {
                                ctx.inEditMode = !ctx.inEditMode
                                if (ctx.inEditMode) {
                                    // 进入编辑态时，注册一次性回调
                                    ctx.getBackPressHandler().addCallback(ctx.singleShotBack)
                                } else {
                                    ctx.getBackPressHandler().removeCallback(ctx.singleShotBack)
                                }
                            }
                        }
                    }
                    Button {
                        attr {
                            height(100f)
                            backgroundColor(Color.YELLOW)
                            titleAttr {
                                text("点击开启BackPress持久拦截，连续BackPress没有任何效果")
                            }
                        }

                        event {
                            click {
                                if (ctx.getBackPressHandler().containsCallback(ctx.persistentBack)) {
                                    ctx.getBackPressHandler().removeCallback(ctx.persistentBack)
                                } else {
                                    ctx.getBackPressHandler().addCallback(ctx.persistentBack)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}