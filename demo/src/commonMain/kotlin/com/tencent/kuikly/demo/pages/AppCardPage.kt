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
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 卡片式瀑布流卡片页面（独立 @Page）
 *
 * 该页面作为独立的 Kuikly Page，被 Native 侧的 KuiklyBaseView 直接加载。
 * Native 侧（iOS/Android/HarmonyOS）创建 KuiklyBaseView 时传入 pageName="AppCardPage"
 * 和 pageData，本页面通过 pagerData.params 获取卡片数据。
 *
 * 架构关系（以 iOS 为例）：
 *   NativeAppWaterfallViewController (Native VC)
 *     └─ UICollectionView (瀑布流)
 *          └─ AppWaterfallCell
 *               └─ KuiklyBaseView(pageName="AppCardPage", pageData={param:{...}})
 *                    └─ 本页面 (AppCardPage)
 */
@Page("AppCardPage")
internal class AppCardPage : BasePager() {

    // 预定义渐变色组合（封面背景色）
    private val gradientPairs = listOf(
        Pair(Color(0xFFFF6B6B), Color(0xFFFF8E53)),  // 珊瑚橙
        Pair(Color(0xFF667eea), Color(0xFF764ba2)),  // 紫蓝
        Pair(Color(0xFF43e97b), Color(0xFF38f9d7)),  // 翠绿
        Pair(Color(0xFFfa709a), Color(0xFFfee140)),  // 粉金
        Pair(Color(0xFF4facfe), Color(0xFF00f2fe)),  // 天蓝
        Pair(Color(0xFFa18cd1), Color(0xFFfbc2eb)),  // 薰衣草
        Pair(Color(0xFFffecd2), Color(0xFFfcb69f)),  // 桃粉
        Pair(Color(0xFF89f7fe), Color(0xFF66a6ff)),  // 冰蓝
        Pair(Color(0xFFf093fb), Color(0xFFf5576c)),  // 玫红
        Pair(Color(0xFF5ee7df), Color(0xFFb490ca)),  // 青紫
    )

    override fun body(): ViewBuilder {
        val params = pagerData.params
        val imageUrl = params.optString("imageUrl", "")
        val imageHeight = params.optDouble("imageHeight", 200.0).toFloat()
        val title = params.optString("title", "")
        val nickname = params.optString("nickname", "")
        val avatarUrl = params.optString("avatarUrl", "")
        val likeCount = params.optInt("likeCount", 0)
        val tag = params.optString("tag", "")

        val colorIndex = params.optInt("colorIndex", 0)
        val desc = params.optString("desc", "")
        val ctx = this

        val gradient = gradientPairs[colorIndex % gradientPairs.size]

        return {
            attr {
                backgroundColor(Color.WHITE)
                borderRadius(8f)
            }

            // 封面图区域：渐变背景 + 图片叠加
            View {
                attr {
                    width(pagerData.pageViewWidth)
                    height(imageHeight)
                    backgroundLinearGradient(
                        Direction.TO_BOTTOM_RIGHT,
                        ColorStop(gradient.first, 0f),
                        ColorStop(gradient.second, 1f)
                    )
                    allCenter()
                    borderRadius(8f, 8f, 0f, 0f)
                }

                // 封面图片（叠加在渐变背景上，如果图片加载成功则覆盖渐变色）
                if (imageUrl.isNotEmpty()) {
                    Image {
                        attr {
                            absolutePositionAllZero()
                            resizeCover()
                            borderRadius(8f, 8f, 0f, 0f)
                            src(imageUrl)
                        }
                    }
                }


            }

            // 标签
            if (tag.isNotEmpty()) {
                View {
                    attr {
                        flexDirectionRow()
                        marginTop(8f)
                        marginLeft(8f)
                        marginRight(8f)
                    }

                    View {
                        attr {
                            backgroundColor(Color(0xFFFFF0F0))
                            borderRadius(4f)
                            paddingLeft(6f)
                            paddingRight(6f)
                            paddingTop(2f)
                            paddingBottom(2f)
                        }

                        Text {
                            attr {
                                fontSize(10f)
                                color(Color(0xFFFF6B6B))
                                text(tag)
                            }
                        }
                    }
                }
            }

            // 标题
            Text {
                attr {
                    marginTop(if (tag.isNotEmpty()) 6f else 8f)
                    marginLeft(8f)
                    marginRight(8f)
                    fontSize(14f)
                    fontWeightBold()
                    color(Color(0xFF333333))
                    lines(2)
                    text(title)
                }
            }

            // 描述（如果有）
            if (desc.isNotEmpty()) {
                Text {
                    attr {
                        marginTop(4f)
                        marginLeft(8f)
                        marginRight(8f)
                        fontSize(11f)
                        color(Color(0xFF999999))
                        lines(2)
                        text(desc)
                    }
                }
            }

            // 底部：头像 + 昵称 + 点赞
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                    marginTop(8f)
                    marginLeft(8f)
                    marginRight(8f)
                    marginBottom(10f)
                }

                // 头像（用渐变色圆形兜底）
                View {
                    attr {
                        size(20f, 20f)
                        borderRadius(10f)
                        backgroundLinearGradient(
                            Direction.TO_BOTTOM_RIGHT,
                            ColorStop(gradient.first, 0f),
                            ColorStop(gradient.second, 1f)
                        )
                        allCenter()
                    }

                    // 头像图片
                    if (avatarUrl.isNotEmpty()) {
                        Image {
                            attr {
                                absolutePositionAllZero()
                                borderRadius(10f)
                                resizeCover()
                                src(avatarUrl)
                            }
                        }
                    }

                    // 昵称首字作为兜底
                    Text {
                        attr {
                            fontSize(10f)
                            color(Color.WHITE)
                            fontWeightBold()
                            text(if (nickname.isNotEmpty()) nickname.substring(0, 1) else "U")
                        }
                    }
                }

                // 昵称
                Text {
                    attr {
                        flex(1f)
                        marginLeft(4f)
                        fontSize(11f)
                        color(Color(0xFF999999))
                        text(nickname)
                    }
                }

                // 点赞图标
                Text {
                    attr {
                        fontSize(11f)
                        color(Color(0xFFFF6B6B))
                        text("♥")
                    }
                }

                // 点赞数
                Text {
                    attr {
                        marginLeft(2f)
                        fontSize(11f)
                        color(Color(0xFF999999))
                        text(ctx.formatCount(likeCount))
                    }
                }
            }
        }
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 10000 -> "${count / 10000}.${(count % 10000) / 1000}w"
            count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}k"
            else -> "$count"
        }
    }
}
