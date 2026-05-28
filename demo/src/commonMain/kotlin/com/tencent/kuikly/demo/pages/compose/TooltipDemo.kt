package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.PlainTooltip
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TooltipAnchorPosition
import com.tencent.kuikly.compose.material3.TooltipBox
import com.tencent.kuikly.compose.material3.TooltipDefaults
import com.tencent.kuikly.compose.material3.rememberTooltipPositionProvider
import com.tencent.kuikly.compose.material3.rememberTooltipState
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.PointerIcon
import com.tencent.kuikly.compose.ui.input.pointer.pointerHoverIcon
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("TooltipDemo")
class TooltipDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("Tooltip 悬停提示") {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(40.dp))

                    /**
                     * 场景 1：二维码 Tooltip（Freeze-on-Hover 增强 Tooltip 组件在桌面端支持鼠标可从Hover区域移动到Tooltip 的区域）
                     *
                     * 能力说明：
                     * 开启 enableFreezeOnHover 后，鼠标从 Anchor 移入 Tooltip 弹窗区域时提示保持显示，
                     * 只有当鼠标同时离开 Anchor 和弹窗超过 freezeDelayMillis 后才会自动消失，适合需要
                     * 用户与弹窗内容交互（如扫码、复制文字）的场景。
                     *
                     * 关键属性：
                     * - enableFreezeOnHover：是否启用鼠标冻结能力，二维码/可交互弹窗建议设为 true。
                     * - freezeDelayMillis：Anchor 与弹窗之间的过渡缓冲时长（默认 150ms），鼠标在间隔中移动不会触发关闭。
                     */
                    TooltipBox(
                        positionProvider = rememberTooltipPositionProvider(
                            positioning = TooltipAnchorPosition.Left,
                            spacingBetweenTooltipAndAnchor = 12.dp
                        ),
                        tooltip = {
                            PlainTooltip(
                                shape = RoundedCornerShape(12.dp),
                                containerColor = Color.White,
                                contentColor = Color(0xFF333333),
                                shadowElevation = 8.dp,
                                maxWidth = 300.dp,                  // 限制最大宽度
                                modifier = Modifier.width(300.dp)   // 限制宽度,和 maxWidth 组合使用
                                    .background(Color(0xFFF5F5F5))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("使用 Workshop app 扫码体验", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Image(
                                        painter = rememberAsyncImagePainter("https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=KuiklyUI"),
                                        contentDescription = "QR Code",
                                        modifier = Modifier.size(140.dp)
                                    )
                                }
                            }
                        },
                        state = rememberTooltipState(),
                        enableFreezeOnHover = true,
                        freezeDelayMillis = TooltipDefaults.FreezeOnHoverDelay,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFF333333), RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("QR", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("扫码体验", fontSize = 15.sp, color = Color(0xFF333333))
                        }
                    }

                }
            }
        }
    }
}
