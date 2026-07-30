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
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.APNG
import com.tencent.kuikly.core.views.APNGVView
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("APNGExamplePage")
internal class APNGExamplePage: BasePager() {

    lateinit var apngViewRef: ViewRef<APNGVView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {

            NavBar {
                attr {
                    title = "APNGExamplePage"
                }

            }
            APNG {
                ref {
                    ctx.apngViewRef = it
                }
                setTimeout(timeout = 3000) {
                    ctx.apngViewRef.view?.play()
                }

                attr {
                    size(200f, 200f)
                    marginTop(200f)
                    autoPlay(true)
                    repeatCount(4)
                    src("https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/5vcy152h.png?test=4")
                }
//                setTimeout(timeout = 3000) {
//                    ctx.autoPlay = true
//                }
                event {
                    animationStart {
                        KLog.i("22", "start play animation")
                    }
                    animationEnd {
                        KLog.i("22", "animation end")
                    }

                }
            }

        }
    }

}
