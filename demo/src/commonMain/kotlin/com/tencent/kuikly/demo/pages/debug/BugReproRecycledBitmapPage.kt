/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.demo.pages.debug

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.MemoryCacheModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * KRMemoryCacheModule 销毁时 recycle 宿主图片库 bitmap 的复现页。
 *
 * cacheImage 拿到的是宿主 adapter（Glide）内存缓存里的 bitmap。
 * 打开本页 → 返回 → 再打开：若上一页销毁时 recycle 了该 bitmap，第二次绘制即崩溃。
 */
@Page("BugReproRecycledBitmapPage")
internal class BugReproRecycledBitmapPage : BasePager() {

    private var cacheKey by observable("")

    override fun created() {
        super.created()
        acquireModule<MemoryCacheModule>(MemoryCacheModule.MODULE_NAME)
            .cacheImage(ImageUri.commonAssets("sample.png").toUrl(""), false) {
                if (it.state == "Complete" && it.errorCode == 0) {
                    cacheKey = it.cacheKey
                }
            }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "Recycled Bitmap"
                }
            }
            Text {
                attr {
                    margin(12f)
                    fontSize(13f)
                    color(Color(0xFF666666L))
                    text("步骤：返回后再次打开本页。修复前第二次打开会崩溃：Canvas: trying to use a recycled bitmap")
                }
            }
            vif({ ctx.cacheKey.isNotEmpty() }) {
                Image {
                    attr {
                        marginLeft(12f)
                        size(132f, 132f)
                        src(ctx.cacheKey)
                    }
                }
            }
        }
    }
}
