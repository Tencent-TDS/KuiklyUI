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
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.PI

@Page("ToImageExamplePage")
internal class ToImageExamplePage : BasePager() {

    companion object {
        private const val TAG = "PerformancePage"
    }

    private var snapshotInfo: String by observable("点击按钮开始截图")
    private var viewRef: ViewRef<DivView>? = null
    private var snapshotResultSrc by observable("")
    private var alternating by observable(false)

    private fun runToImageTest(
        type: DeclarativeBaseView.ImageType,
        sampleSize: Int,
        label: String,
        scale: Float = 1.0f
    ) {
        alternating = !alternating
        viewRef?.view?.toImage(type, sampleSize, scale) {
            val code = it?.optInt("code") ?: -1
            val data = it?.optString("data") ?: ""
            val message = it?.optString("message") ?: ""
            val success = code == 0 && data.isNotEmpty()

            KLog.d(
                TAG,
                "toImage[$label], success: $success, code: $code, sampleSize: $sampleSize, scale: $scale, data: $data, message: $message"
            )

            snapshotInfo = "[$label] code=$code, sampleSize=$sampleSize, scale=$scale, message=$message"
            if (success) {
                snapshotResultSrc = data
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar { attr { title = "ToImage Demo Page" } }

            // Big testing zone
            View {
                attr {
                    margin(12.0f)
                    padding(12.0f)
                    borderRadius(12.0f)
                    border(Border(lineWidth = 1.0f, lineStyle = BorderStyle.SOLID, color = Color(0xFFFB8C00)))
                    backgroundColor(Color(0xFFFFF8E1))
                }

                // Block 1: Snapshot target area
                View {
                    ref { ctx.viewRef = it }
                    attr {
                        padding(12.0f)
                        borderRadius(10.0f)
                        border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color(0xFF90CAF9)))
                        backgroundColor(if (ctx.alternating) Color(0xFFE3F2FD) else Color(0xFFFFFDE7))
                        height(180.0f)
                    }

                    Text {
                        attr {
                            fontSize(16.0f)
                            color(Color(0xFF1565C0))
                            text("待截图区：这里包含文字和图片")
                        }
                    }

                    Text {
                        attr {
                            marginTop(8.0f)
                            fontSize(14.0f)
                            color(Color(0xFF424242))
                            text("点击下方按钮后，会对当前区域进行 toImage 截图并展示结果")
                        }
                    }

                    // A horizontal row groups the sample image and the small canvas
                    // sub-area side by side so the target zone height stays compact.
                    View {
                        attr {
                            marginTop(10.0f)
                            flexDirectionRow()
                        }

                        Image {
                            attr {
                                size(140f, 90f)
                                src("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59ef6918.gif")
                            }
                        }

                        // Small canvas sub-area for verifying canvas-bitmap capture by toImage.
                        // The canvas draws a red-filled rounded rect, a blue circle and a
                        // diagonal green line. If the snapshot fails to include canvas bitmap,
                        // this area will show as an empty rectangle in the result image.
                        View {
                            attr {
                                marginLeft(10.0f)
                                width(140.0f)
                                height(90.0f)
                                borderRadius(6.0f)
                                border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color(0xFF7E57C2)))
                                backgroundColor(Color(0xFFF3E5F5))
                            }

                            Canvas({
                                attr {
                                    absolutePosition(0f, 0f, 0f, 0f)
                                }
                            }) { context, width, height ->
                                // Red rounded-corner rectangle on the left.
                                context.beginPath()
                                context.fillStyle(Color(0xFFE53935))
                                context.moveTo(4f, 4f)
                                context.lineTo(width * 0.35f, 4f)
                                context.lineTo(width * 0.35f, height - 4f)
                                context.lineTo(4f, height - 4f)
                                context.lineTo(4f, 4f)
                                context.fill()

                                // Blue circle in the middle.
                                context.beginPath()
                                context.fillStyle(Color(0xFF1E88E5))
                                val cx = width * 0.55f
                                val cy = height / 2f
                                val r = (height / 2f) - 6f
                                context.arc(cx, cy, r, 0f, (PI * 2f).toFloat(), false)
                                context.fill()

                                // Green diagonal stroke across the whole area.
                                context.beginPath()
                                context.strokeStyle(Color(0xFF43A047))
                                context.lineWidth(2f)
                                context.moveTo(width * 0.65f, height - 6f)
                                context.lineTo(width - 6f, 6f)
                                context.stroke()
                            }
                        }
                    }
                }

                // Block 2: Snapshot buttons
                View {
                    attr {
                        marginTop(12.0f)
                        padding(10.0f)
                        borderRadius(10.0f)
                        border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color(0xFFB0BEC5)))
                        backgroundColor(Color(0xFFF5F5F5))
                    }

                    Text {
                        attr {
                            fontSize(15.0f)
                            color(Color(0xFF37474F))
                            text("截图按钮")
                        }
                    }

                    View {
                        attr {
                            marginTop(10.0f)
                            padding(10.0f)
                            borderRadius(8.0f)
                            allCenter()
                            backgroundColor(Color(0xFF1976D2))
                        }
                        Text {
                            attr {
                                fontSize(14.0f)
                                color(Color.WHITE)
                                text("CACHE_KEY (sampleSize=1)")
                            }
                        }
                        event {
                            click {
                                ctx.runToImageTest(DeclarativeBaseView.ImageType.CACHE_KEY, 1, "CACHE_KEY")
                            }
                        }
                    }

                    View {
                        attr {
                            marginTop(8.0f)
                            padding(10.0f)
                            borderRadius(8.0f)
                            allCenter()
                            backgroundColor(Color(0xFF00897B))
                        }
                        Text {
                            attr {
                                fontSize(14.0f)
                                color(Color.WHITE)
                                text("CACHE_KEY (sampleSize=2)")
                            }
                        }
                        event {
                            click {
                                ctx.runToImageTest(DeclarativeBaseView.ImageType.CACHE_KEY, 2, "CACHE_KEY")
                            }
                        }
                    }

                    View {
                        attr {
                            marginTop(8.0f)
                            padding(10.0f)
                            borderRadius(8.0f)
                            allCenter()
                            backgroundColor(Color(0xFFF57C00))
                        }
                        Text {
                            attr {
                                fontSize(14.0f)
                                color(Color.WHITE)
                                text("DATA_URI (sampleSize=1)")
                            }
                        }
                        event {
                            click {
                                ctx.runToImageTest(DeclarativeBaseView.ImageType.DATA_URI, 1, "DATA_URI")
                            }
                        }
                    }

                    View {
                        attr {
                            marginTop(8.0f)
                            padding(10.0f)
                            borderRadius(8.0f)
                            allCenter()
                            backgroundColor(Color(0xFF8E24AA))
                        }
                        Text {
                            attr {
                                fontSize(14.0f)
                                color(Color.WHITE)
                                text("DATA_URI (sampleSize=2)")
                            }
                        }
                        event {
                            click {
                                ctx.runToImageTest(DeclarativeBaseView.ImageType.DATA_URI, 2, "DATA_URI")
                            }
                        }
                    }

                    // H5-only: caller-requested upscale factor. Default 1.0 keeps behavior
                    // identical to before. Larger values yield a higher-resolution snapshot
                    // (bounded internally by MAX_CANVAS_SIDE).
                    View {
                        attr {
                            marginTop(8.0f)
                            padding(10.0f)
                            borderRadius(8.0f)
                            allCenter()
                            backgroundColor(Color(0xFF3949AB))
                        }
                        Text {
                            attr {
                                fontSize(14.0f)
                                color(Color.WHITE)
                                text("DATA_URI (scale=2.0)")
                            }
                        }
                        event {
                            click {
                                ctx.runToImageTest(
                                    DeclarativeBaseView.ImageType.DATA_URI,
                                    1,
                                    "DATA_URI-scale2",
                                    2.0f
                                )
                            }
                        }
                    }

                    View {
                        attr {
                            marginTop(8.0f)
                            padding(10.0f)
                            borderRadius(8.0f)
                            allCenter()
                            backgroundColor(Color(0xFFD81B60))
                        }
                        Text {
                            attr {
                                fontSize(14.0f)
                                color(Color.WHITE)
                                text("DATA_URI (scale=3.0)")
                            }
                        }
                        event {
                            click {
                                ctx.runToImageTest(
                                    DeclarativeBaseView.ImageType.DATA_URI,
                                    1,
                                    "DATA_URI-scale3",
                                    3.0f
                                )
                            }
                        }
                    }
                }

                // Block 3: Snapshot display area
                View {
                    attr {
                        marginTop(12.0f)
                        padding(10.0f)
                        borderRadius(10.0f)
                        border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color(0xFFFFCC80)))
                        backgroundColor(Color(0xFFFFF3E0))
                        minHeight(180.0f)
                    }

                    Text {
                        attr {
                            fontSize(15.0f)
                            color(Color(0xFFE65100))
                            text("截图展示区")
                        }
                    }

                    Image {
                        attr {
                            marginTop(10.0f)
                            // Match the snapshot target's on-screen aspect ratio so the
                            // result image does not look squashed. Both this Image and
                            // the target View share the same parent width, so using the
                            // same height (180) with flex(1f) keeps proportions aligned.
                            flex(1f)
                            height(180.0f)
                            src(ctx.snapshotResultSrc)
                        }
                    }

                    Text {
                        attr {
                            marginTop(8.0f)
                            fontSize(12.0f)
                            color(Color(0xFF6D4C41))
                            text(ctx.snapshotInfo)
                        }
                    }
                }
            }
        }
    }}
