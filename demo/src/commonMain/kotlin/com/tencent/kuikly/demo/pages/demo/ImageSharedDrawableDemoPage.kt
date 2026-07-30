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
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 验证 Android 上两个相同 src 的 Image 是否共享 Drawable 实例导致互相影响。
 *
 * 背景：本 PR 在 KRImageView.kt 中修改了：
 *   1. setSrc() 里新增 stopAnimatable() —— 切换 src 前先停止当前 GIF 动画
 *   2. superSetImage() 里新增 stop() + start() —— 确保 GIF 从第一帧开始播放
 *
 * 如果 Android 图片加载库（如 Glide）对相同 URL 返回了同一个 Drawable 实例，
 * 在一个 ImageView 上调用 drawable.stop() 可能会影响另一个持有同一实例的 ImageView。
 *
 * 测试场景：
 *   Case 1: 两个 GIF 使用相同 src，观察是否同时播放、互不干扰
 *   Case 2: 两个 GIF 使用相同 src，切换其中一个的 src，观察另一个 GIF 是否被停止
 *   Case 3: 三个 GIF 使用相同 src 但不同尺寸，观察是否各自正常播放
 */
@Page("image_shared_drawable_demo")
internal class ImageSharedDrawableDemoPage : BasePager() {

    // Case 2: 控制第一个 GIF 的 src 切换
    private var firstGifUseAlt: Boolean by observable(false)
    private var switchCount: Int by observable(0)

    // Case 2 补充: 通过 vif 移除/重建第二个 GIF
    private var showSecondGif: Boolean by observable(true)

    companion object {
        private const val GIF_URL_1 =
            "https://upload.wikimedia.org/wikipedia/commons/2/2c/Rotating_earth_%28large%29.gif"
        private const val GIF_URL_2 =
            "https://upload.wikimedia.org/wikipedia/commons/d/d3/Newtons_cradle_animation_book_2.gif"
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "GIF Shared Drawable Test" } }

            List {
                attr { flex(1f) }

                // ==================== Case 1 ====================
                SectionHeader("Case 1: 两个 GIF 相同 src，观察是否同时播放")
                DescText(
                    "两个 Image 使用完全相同的 GIF URL。\n" +
                    "预期：两个 GIF 都正常播放，互不干扰。\n" +
                    "如果共享 Drawable → 可能只有一个在动，或帧率异常。"
                )
                View {
                    attr {
                        flexDirectionRow()
                        justifyContentSpaceAround()
                        alignItemsCenter()
                        margin(10f)
                    }
                    GifCard("GIF A") {
                        Image {
                            attr {
                                size(140f, 140f)
                                src(GIF_URL_1)
                            }
                        }
                    }
                    GifCard("GIF B (same src)") {
                        Image {
                            attr {
                                size(140f, 140f)
                                src(GIF_URL_1)
                            }
                        }
                    }
                }

                // ==================== Case 2 ====================
                SectionHeader("Case 2: 切换一个 GIF 的 src，观察另一个是否被停止")
                DescText(
                    "GIF A 可切换 src（地球 ↔ 牛顿摆），GIF B 保持地球不变。\n" +
                    "关注：切换 GIF A 时会触发 setSrc() → stopAnimatable()。\n" +
                    "如果共享 Drawable → GIF B 的动画会被 stop() 停掉。\n" +
                    "切换次数: ${ctx.switchCount}"
                )
                View {
                    attr {
                        flexDirectionRow()
                        justifyContentSpaceAround()
                        alignItemsCenter()
                        margin(10f)
                    }
                    GifCard("GIF A (${if (ctx.firstGifUseAlt) "牛顿摆" else "地球"})") {
                        Image {
                            attr {
                                size(140f, 140f)
                                src(if (ctx.firstGifUseAlt) GIF_URL_2 else GIF_URL_1)
                            }
                        }
                    }
                    GifCard("GIF B (地球, 不动)") {
                        Image {
                            attr {
                                size(140f, 140f)
                                src(GIF_URL_1)
                            }
                        }
                    }
                }
                makeButton("切换 GIF A 的 src") {
                    ctx.firstGifUseAlt = !ctx.firstGifUseAlt
                    ctx.switchCount++
                }

                // ==================== Case 3 ====================
                SectionHeader("Case 3: 移除/重建 GIF，观察另一个是否受影响")
                DescText(
                    "通过 vif 完全移除或重建 GIF B（销毁/新建 native view）。\n" +
                    "关注：移除 GIF B 时 onDestroy → stopAnimatable() 是否影响 GIF A。\n" +
                    "如果共享 Drawable → 移除 B 时 A 的动画也会被 stop()。"
                )
                View {
                    attr {
                        flexDirectionRow()
                        justifyContentSpaceAround()
                        alignItemsCenter()
                        margin(10f)
                    }
                    GifCard("GIF A (始终存在)") {
                        Image {
                            attr {
                                size(140f, 140f)
                                src(GIF_URL_1)
                            }
                        }
                    }
                    View {
                        attr {
                            alignItemsCenter()
                        }
                        Text {
                            attr {
                                fontSize(12f)
                                fontWeightBold()
                                color(Color(0xFF333333))
                                marginBottom(4f)
                                text(if (ctx.showSecondGif) "GIF B (存在)" else "GIF B (已移除)")
                            }
                        }
                        vif({ ctx.showSecondGif }) {
                            Image {
                                attr {
                                    size(140f, 140f)
                                    src(GIF_URL_1)
                                }
                            }
                        }
                        velse {
                            View {
                                attr {
                                    size(140f, 140f)
                                    backgroundColor(Color(0xFFEEEEEE))
                                    allCenter()
                                }
                                Text {
                                    attr {
                                        fontSize(13f)
                                        color(Color(0xFF999999))
                                        text("已移除")
                                    }
                                }
                            }
                        }
                    }
                }
                makeButton("移除/重建 GIF B") {
                    ctx.showSecondGif = !ctx.showSecondGif
                }

                // ==================== Case 4 ====================
                SectionHeader("Case 4: 三个相同 src 不同尺寸的 GIF")
                DescText(
                    "三个 GIF 使用相同 src 但尺寸不同 (160/100/60)。\n" +
                    "关注：superSetImage 中 stop()+start() 是否导致某个 GIF 重置帧。\n" +
                    "如果共享 Drawable → 可能出现帧同步或闪烁。"
                )
                View {
                    attr {
                        flexDirectionRow()
                        justifyContentSpaceAround()
                        alignItemsCenter()
                        margin(10f)
                    }
                    GifCard("160x160") {
                        Image {
                            attr {
                                size(160f, 160f)
                                src(GIF_URL_1)
                            }
                        }
                    }
                    GifCard("100x100") {
                        Image {
                            attr {
                                size(100f, 100f)
                                src(GIF_URL_1)
                            }
                        }
                    }
                    GifCard("60x60") {
                        Image {
                            attr {
                                size(60f, 60f)
                                src(GIF_URL_1)
                            }
                        }
                    }
                }

                // Bottom padding
                View {
                    attr { height(40f) }
                }
            }
        }
    }
}

// ========== Helper components ==========

private fun ViewContainer<*, *>.SectionHeader(title: String) {
    View {
        attr {
            backgroundColor(Color(0xFFF0F0F0))
            padding(8f, 16f, 8f, 16f)
            marginTop(16f)
        }
        Text {
            attr {
                fontSize(15f)
                fontWeightBold()
                color(Color(0xFF333333))
                text(title)
            }
        }
    }
}

private fun ViewContainer<*, *>.DescText(content: String) {
    Text {
        attr {
            fontSize(12f)
            color(Color(0xFF888888))
            margin(10f, 16f, 0f, 16f)
            text(content)
        }
    }
}

private fun ViewContainer<*, *>.GifCard(label: String, content: ViewContainer<*, *>.() -> Unit) {
    View {
        attr { alignItemsCenter() }
        Text {
            attr {
                fontSize(12f)
                fontWeightBold()
                color(Color(0xFF333333))
                marginBottom(4f)
                text(label)
            }
        }
        content()
    }
}

private fun ViewContainer<*, *>.makeButton(title: String, onClick: () -> Unit) {
    View {
        attr {
            backgroundColor(Color(0xFF007AFF))
            borderRadius(8f)
            margin(10f, 16f, 0f, 16f)
            padding(10f, 16f, 10f, 16f)
            alignSelfCenter()
        }
        event {
            click { onClick() }
        }
        Text {
            attr {
                fontSize(14f)
                color(Color.WHITE)
                text(title)
            }
        }
    }
}
