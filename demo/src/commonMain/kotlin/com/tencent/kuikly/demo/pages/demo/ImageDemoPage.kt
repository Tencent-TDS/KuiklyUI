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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.Utils
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

@Page("image_demo")
internal class ImageDemoPage: BasePager() {

    var localImagePath: String by observable("")
    var tintColor: Color? by observable(Color.YELLOW)

    override fun created() {
        val imageUrl = "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg"
        Utils.bridgeModule(this).getLocalImagePath(imageUrl) {data ->
            localImagePath = data?.optString("localPath", "") ?: ""
        }
        KLog.i("34343", "safeAreaInsets:" +pageData.safeAreaInsets.toString())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "Image Example" } }
            List {
                attr { flex(1f) }
                ViewExampleSectionHeader { attr { title = "Assets Image" } }
                View {
                    attr {
                        allCenter()
                        margin(20f)
                    }

                    Image {
                        attr {
                            resizeContain()
                            tintColor(Color.RED)
                            accessibilityRole(AccessibilityRole.BUTTON)
                            size(pagerData.pageViewWidth * 0.6f, 100f)
                            src(ImageUri.pageAssets("withAlphaTest.png"))
                        }
                    }
                    Image {
                        attr {
                            resizeContain()
                            tintColor(Color.RED)
                            accessibilityRole(AccessibilityRole.BUTTON)
                            size(pagerData.pageViewWidth * 0.6f, 100f)
                            src(ImageUri.pageAssets("panda.png"))
                        }
                    }
                    Image {
                        attr {
                            resizeContain()
                            tintColor(Color.BLUE)
                            size(pagerData.pageViewWidth * 0.6f, 100f)
                            src(ImageUri.pageAssets("1/penguin1.png"))
                        }
                    }
                    Image {
                        attr {
                            resizeContain()
                            tintColor(ctx.tintColor)
                            size(pagerData.pageViewWidth * 0.6f, 100f)
                            src(ImageUri.commonAssets("penguin2.png"))
                            blurRadius(25F)
                        }
                        setTimeout(2000) {
                            ctx.tintColor = null
                        }
                    }

                    Image {
                        attr {
                            resizeContain()
                            tintColor(Color.RED)
                            size(pagerData.pageViewWidth * 0.6f, 100f)
                            src(ImageUri.pageAssets("penguin3.png"))
                        }
                    }

                }
                ViewExampleSectionHeader { attr { title = "Network Image" } }
                View {
                    attr {
                        allCenter()
                        margin(20f)
                    }

                    Image {
                        attr {
                            resizeContain()
                            tintColor(Color.RED)
                            size(pagerData.pageViewWidth * 0.6f, 100f)
                            src("https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/Dfnp7Q9F.png")
                        }
                        event {
                            loadSuccess { loadSuccessParams ->
                                val src = loadSuccessParams.src
                            }
                            loadFailure { loadFailureParams ->
                                val src = loadFailureParams.src
                                val errorCode = loadFailureParams.errorCode
                            }
                        }
                    }

                }
                ViewExampleSectionHeader { attr { title = "File Image" } }
                View {
                    attr {
                        allCenter()
                        margin(20f)
                    }

                    Image {
                        attr {
                            resizeContain()
                            size(pagerData.pageViewWidth * 0.6f, 100f)
                            src(ImageUri.file(ctx.localImagePath))
                        }
                    }

                }
                ViewExampleSectionHeader { attr { title = "NinePath Image" } }
                View {
                    attr {
                        allCenter()
                        margin(20f)
                    }

                    Image {
                        attr {
                            resizeContain()
                            size(pagerData.pageViewWidth * 0.6f, 50f)
                            src("https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/gjCqDSbr.png", true)
                        }
                    }

                }

                ViewExampleSectionHeader { attr { title = "Custom Cap Inset Image" } }
                View {
                    attr {
                        allCenter()
                        margin(20f)
                    }

                    Image {
                        attr {
                            resizeStretch()
                            size(pagerData.pageViewWidth * 0.6f, 50f)
                            src("https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/gjCqDSbr.png")
                            capInsets(12f, 25f, 12f, 12f)
                        }
                    }

                }
            }

        }
    }
}