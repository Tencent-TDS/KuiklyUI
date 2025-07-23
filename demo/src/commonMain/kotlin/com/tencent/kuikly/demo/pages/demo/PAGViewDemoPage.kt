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
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.PAG
import com.tencent.kuikly.core.views.PAGView
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("PAGViewDemoPage")
internal class PAGViewDemoPage: BasePager() {

    var autoPlay : Boolean by observable(true)
    lateinit var pagViewRef: ViewRef<PAGView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {

                NavBar {
                    attr {
                        title = "PAGExamplePage"
                    }

                }
                PAG {
                    ref {
                        ctx.pagViewRef = it
                    }
//                    setTimeout(timeout = 1000) {
//                       ctx.autoPlay = true
//                        setTimeout(timeout = 3000) {
//                            ctx.pagViewRef.view?.stop()
//                            setTimeout(timeout = 6000) {
//                                ctx.pagViewRef.view?.play()
//                            }
//                        }
//                    }

                    attr {
                        backgroundColor(Color.GRAY)
                        marginTop(100f)
                        size(50f, 50f)
                        repeatCount(3)
                        autoPlay(ctx.autoPlay)
                        src("https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/1pwxlc62.pag?test=15")
                    }

                    event {
                        animationStart {
                            KLog.d(TAG, "animationStart")
                        }

                        animationEnd {
                            KLog.d(TAG, "animationEnd")

                        }

                    }
                }
            }
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()

    }

    companion object {
        private const val TAG = "PAGViewDemoPage"
    }
}