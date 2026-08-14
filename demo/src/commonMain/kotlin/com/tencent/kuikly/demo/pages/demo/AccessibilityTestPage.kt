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
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 无障碍能力全覆盖测试页面。
 *
 * 用例矩阵（19 个）：
 *   A 组 · accessibility 文本
 *     A1 CAPI 组件设 text，无 role
 *     A2 CAPI 组件 text 随状态变化（响应式）
 *     A3 ArkTS 转发组件设 text
 *     A4 空串 text（等价 reset）
 *   B 组 · accessibilityRole
 *     B1 BUTTON  B2 TEXT  B3 IMAGE  B4 CHECKBOX
 *     B5 SEARCH（鸿蒙降级为 TEXT_INPUT）
 *     B6 NONE   （鸿蒙走 MODE_DISABLED，剔除无障碍树）
 *     B7 NONE ↔ BUTTON 切换（观察 MODE 是否清除）
 *     B8 ArkTS 组件 role=BUTTON
 *   C 组 · accessibilityInfo
 *     C1 clickable=true, longClickable=false
 *     C2 clickable=true, longClickable=true
 *     C3 全 false（等价 reset）
 *   D 组 · accessibilityAnnounce
 *     D1 CAPI view 触发 announce
 *     D2 ArkTS view 触发 announce
 *     D3 长文本 announce（100+ 字符）
 *     D4 快速连续 announce（时序）
 *   E 组 · accessibilityFocus
 *     E1 焦点跳到 CAPI view（顶部）
 *     E2 焦点跳到 ArkTS view
 *     E3 焦点跳到当前视口之外的 view（底部）
 *
 * 使用方式：真机开启屏幕朗读，逐条聚焦每个用例卡片，观察读屏内容是否与
 * "预期" 标签一致。D/E 组由底部悬浮触发条触发。
 */
@Page("AccessibilityTestPage")
internal class AccessibilityTestPage : BasePager() {

    // 用于展示最近一次 announce/focus 触发结果的状态条
    private var lastActionLog by observable("尚未触发")

    // A2 用：让 accessibility 文本随点击变化
    private var a2Counter by observable(0)

    // B7 用：NONE ↔ BUTTON 切换
    private var b7RoleIsNone by observable(true)

    // 触发目标 refs（E 组用）
    private var e1TopRef: ViewRef<DivView>? = null
    private var e2ArkTsRef: ViewRef<MyDemoCustomView>? = null
    private var e3BottomRef: ViewRef<DivView>? = null

    // D2 用：ArkTS view 上触发 announce
    private var d2ArkTsRef: ViewRef<MyDemoCustomView>? = null

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color(0xFFF5F5F5)) }
            NavBar { attr { title = "AccessibilityTestPage" } }

            // ===== 顶部状态条：非无障碍焦点目标，仅辅助人工观察 =====
            View {
                attr {
                    height(36f)
                    backgroundColor(Color(0xFFEEEEEE))
                    padding(left = 12f, right = 12f)
                    justifyContentCenter()
                }
                Text {
                    attr {
                        text("状态: ${ctx.lastActionLog}")
                        fontSize(12f)
                        color(Color(0xFF666666))
                    }
                }
            }

            Scroller {
                attr {
                    flex(1f)
                }

                // ===== A 组：accessibility 文本 =====
                sectionHeader("A. accessibility 文本")

                // A1
                caseCard(
                    title = "A1 · CAPI View + accessibility",
                    expect = "读屏播报：\"这是A1卡片\"",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFD1E7DD))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("这是A1卡片")
                        }
                        Text { attr { text("A1"); fontSize(16f) } }
                    }
                }

                // A2 · 响应式
                caseCard(
                    title = "A2 · text 随状态更新",
                    expect = "点击卡片使计数+1，再次聚焦读屏应播报新数值",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFD1E7DD))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("A2计数当前为${ctx.a2Counter}")
                        }
                        event { click { ctx.a2Counter += 1 } }
                        Text {
                            attr {
                                text("A2 · 点我+1（现值：${ctx.a2Counter}）")
                                fontSize(16f)
                            }
                        }
                    }
                }

                // A3 · ArkTS 转发组件
                caseCard(
                    title = "A3 · ArkTS 组件 accessibility",
                    expect = "读屏播报：\"这是A3的ArkTS组件\"（依赖业务方 @Component 已应用 .accessibilityText）",
                ) {
                    MyDemoCustom {
                        attr {
                            size(width = 260f, height = 80f)
                            backgroundColor(Color.YELLOW)
                            message("A3")
                            accessibility("这是A3的ArkTS组件")
                        }
                    }
                }

                // A4 · 空串
                caseCard(
                    title = "A4 · 空串 accessibility（等价 reset）",
                    expect = "读屏应按默认行为播报（一般播报子 Text 的文本 \"A4\"），不再朗读自定义文本",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFD1E7DD))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("")
                        }
                        Text { attr { text("A4"); fontSize(16f) } }
                    }
                }

                // ===== B 组：accessibilityRole =====
                sectionHeader("B. accessibilityRole 角色映射")

                caseCard(
                    title = "B1 · BUTTON",
                    expect = "播报 \"我是按钮，按钮\"（角色 \"按钮\"）",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFCFE2FF))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("我是按钮")
                            accessibilityRole(AccessibilityRole.BUTTON)
                        }
                        Text { attr { text("B1"); fontSize(16f) } }
                    }
                }

                caseCard(
                    title = "B2 · TEXT",
                    expect = "播报 \"我是文本，文本\"（角色 \"文本\"）",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFCFE2FF))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("我是文本")
                            accessibilityRole(AccessibilityRole.TEXT)
                        }
                        Text { attr { text("B2"); fontSize(16f) } }
                    }
                }

                caseCard(
                    title = "B3 · IMAGE",
                    expect = "播报 \"图标示例，图片\"（角色 \"图片\"）",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFCFE2FF))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("图标示例")
                            accessibilityRole(AccessibilityRole.IMAGE)
                        }
                        Text { attr { text("B3"); fontSize(16f) } }
                    }
                }

                caseCard(
                    title = "B4 · CHECKBOX",
                    expect = "播报 \"是否同意，复选框，未勾选\"（或类似角色描述）",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFCFE2FF))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("是否同意")
                            accessibilityRole(AccessibilityRole.CHECKBOX)
                        }
                        Text { attr { text("B4"); fontSize(16f) } }
                    }
                }

                caseCard(
                    title = "B5 · SEARCH（鸿蒙降级 TEXT_INPUT）",
                    expect = "鸿蒙播报为 \"文本输入框\" / \"编辑框\"；安卓/iOS 为 \"搜索框\"",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFFFE69C))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("搜索关键字")
                            accessibilityRole(AccessibilityRole.SEARCH)
                        }
                        Text { attr { text("B5"); fontSize(16f) } }
                    }
                }

                caseCard(
                    title = "B6 · NONE（本节点剔除，子节点独立）",
                    expect = "外层粉色卡片本身不会作为焦点单元被聚焦；但内部子 Text 仍会独立聚焦并朗读其文本（NONE 仅剔除本节点，不递归到子节点，三端一致）",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFF8D7DA))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("这段不应被朗读")
                            accessibilityRole(AccessibilityRole.NONE)
                        }
                        Text { attr { text("B6（外层不聚焦，本 Text 仍会聚焦）"); fontSize(16f) } }
                    }
                }

                caseCard(
                    title = "B7 · NONE ↔ BUTTON 切换（验证 MODE 重置）",
                    expect = "切换到 BUTTON 时：蓝色测试节点可被无障碍聚焦并播报为按钮；切回 NONE 时：蓝色节点不可聚焦（子 Text 仍可聚焦，参见 B6）。用于验证 MODE 从 DISABLED 正确复位。",
                ) {
                    // 被测试节点：无 click 事件，仅参与角色切换观察。
                    // 不在 NONE 节点上挂 click——那本身就与"不参与无障碍"矛盾。
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFCFE2FF))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("B7测试节点，当前角色是${if (ctx.b7RoleIsNone) "NONE" else "BUTTON"}")
                            accessibilityRole(
                                if (ctx.b7RoleIsNone) AccessibilityRole.NONE else AccessibilityRole.BUTTON
                            )
                        }
                        Text {
                            attr {
                                text("被测节点（当前：${if (ctx.b7RoleIsNone) "NONE" else "BUTTON"}）")
                                fontSize(16f)
                            }
                        }
                    }
                    // 独立的切换开关：始终是 BUTTON，方便读屏用户也能双击切换。
                    View {
                        attr {
                            size(width = 260f, height = 40f)
                            margin(top = 8f)
                            backgroundColor(Color(0xFFFFC107))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("切换 B7 被测节点的角色")
                            accessibilityRole(AccessibilityRole.BUTTON)
                            accessibilityInfo(clickable = true, longClickable = false)
                        }
                        event { click { ctx.b7RoleIsNone = !ctx.b7RoleIsNone } }
                        Text {
                            attr {
                                text("↕ 点此切换角色")
                                fontSize(14f)
                            }
                        }
                    }
                }

                caseCard(
                    title = "B8 · ArkTS 组件 role=BUTTON（API < 18 时 role 不生效）",
                    expect = "本项目 compatibleSdkVersion=5.0.0(12)，ArkTS `.accessibilityRole()` 修饰器要求 API 18+，不启用。\n" +
                        "读屏预期播报 accessibility 文案\"我是ArkTS按钮\"，但**不会**额外播报\"按钮\"角色提示。\n" +
                        "如需 role 语义，业务方应直接把词写进 accessibility 文案，如 accessibility(\"我是提交按钮\")。",
                ) {
                    MyDemoCustom {
                        attr {
                            size(width = 260f, height = 80f)
                            backgroundColor(Color.YELLOW)
                            message("B8")
                            // accessibilityRole(BUTTON) 在 ArkTS 转发组件上暂不生效（API 18+ 才支持）；
                            // 保留调用不影响运行，仅 CAPI 组件路径下会真正写 NODE_ACCESSIBILITY_ROLE。
                            accessibility("我是ArkTS按钮")
                            accessibilityRole(AccessibilityRole.BUTTON)
                        }
                    }
                }

                // ===== C 组：accessibilityInfo =====
                sectionHeader("C. accessibilityInfo 可交互动作")

                caseCard(
                    title = "C1 · clickable=true, longClickable=false",
                    expect = "读屏播报 \"双击激活\" 类提示；不应有 \"双击并长按\" 提示",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFD1E7DD))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("可点击卡片")
                            accessibilityInfo(clickable = true, longClickable = false)
                        }
                        Text { attr { text("C1"); fontSize(16f) } }
                    }
                }

                caseCard(
                    title = "C2 · clickable=true, longClickable=true",
                    expect = "读屏应同时播报 \"双击激活\" 与 \"长按\" 类提示",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFD1E7DD))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("可点击可长按")
                            accessibilityInfo(clickable = true, longClickable = true)
                        }
                        Text { attr { text("C2"); fontSize(16f) } }
                    }
                }

                caseCard(
                    title = "C3 · 全 false（等价 reset）",
                    expect = "无 \"双击激活\" / \"长按\" 提示，仅播报文本",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFD1E7DD))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("无动作提示")
                            accessibilityInfo(clickable = false, longClickable = false)
                        }
                        Text { attr { text("C3"); fontSize(16f) } }
                    }
                }

                // ===== D 组：accessibilityAnnounce =====
                sectionHeader("D. accessibilityAnnounce 主动播报")

                caseCard(
                    title = "D1 · CAPI view 触发 announce（短文本）",
                    expect = "点击卡片后读屏播报 \"D1: 你好世界\"",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFE2D9F3))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("D1 触发按钮")
                            accessibilityRole(AccessibilityRole.BUTTON)
                            accessibilityInfo(clickable = true, longClickable = false)
                        }
                        event {
                            click {
                                ctx.lastActionLog = "D1 announce: 你好世界"
                                // announce 是 view 上的方法；对当前 View 引用较麻烦，改用 pager 顶层任一 view
                                // 这里通过点击卡片自身，但需要 ViewRef。用 ref 拿引用：
                                // 由于此处无法直接拿 ViewRef，改在闭包外用 ref 已存的 e1TopRef 触发。
                                ctx.e1TopRef?.view?.accessibilityAnnounce("D1: 你好世界")
                            }
                        }
                        Text { attr { text("D1 · 点击触发 announce"); fontSize(16f) } }
                    }
                }

                caseCard(
                    title = "D2 · ArkTS view 触发 announce",
                    expect = "点击卡片后读屏播报 \"D2: 来自ArkTS的问候\"",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFE2D9F3))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("D2 触发按钮")
                            accessibilityRole(AccessibilityRole.BUTTON)
                        }
                        event {
                            click {
                                ctx.lastActionLog = "D2 announce: 来自ArkTS的问候"
                                ctx.d2ArkTsRef?.view?.accessibilityAnnounce("D2: 来自ArkTS的问候")
                            }
                        }
                        Text { attr { text("D2 · 点击（走 ArkTS view）"); fontSize(16f) } }
                    }
                }

                // 隐藏的 ArkTS view 承载 D2 的 announce 调用；也用于 E2 focus 目标
                MyDemoCustom {
                    ref { ctx.d2ArkTsRef = it; ctx.e2ArkTsRef = it }
                    attr {
                        size(width = 260f, height = 80f)
                        backgroundColor(Color.YELLOW)
                        message("D2/E2 目标")
                        accessibility("D2和E2的ArkTS目标")
                        accessibilityRole(AccessibilityRole.BUTTON)
                    }
                }

                caseCard(
                    title = "D3 · 长文本 announce（100+ 字符）",
                    expect = "读屏完整播报整段长文本，不截断",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFE2D9F3))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("D3 长文本触发")
                            accessibilityRole(AccessibilityRole.BUTTON)
                        }
                        event {
                            click {
                                val longMsg =
                                    "这是一段用于验证长文本朗读的样本文字，" +
                                        "包含逗号、句号和中英文混合 hello world，" +
                                        "同时故意超过一百个字符以覆盖边界场景，" +
                                        "验证屏幕朗读器不会静默截断或提前结束。"
                                ctx.lastActionLog = "D3 announce (${longMsg.length} chars)"
                                ctx.e1TopRef?.view?.accessibilityAnnounce(longMsg)
                            }
                        }
                        Text { attr { text("D3 · 点击触发长文本"); fontSize(16f) } }
                    }
                }

                caseCard(
                    title = "D4 · 快速连续 announce（时序）",
                    expect = "点击后连续触发3次 announce；实际播报可能被后者覆盖，只朗读最后一条",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFE2D9F3))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("D4 连续触发")
                            accessibilityRole(AccessibilityRole.BUTTON)
                        }
                        event {
                            click {
                                ctx.lastActionLog = "D4 announce x3"
                                ctx.e1TopRef?.view?.accessibilityAnnounce("D4 消息一")
                                ctx.e1TopRef?.view?.accessibilityAnnounce("D4 消息二")
                                ctx.e1TopRef?.view?.accessibilityAnnounce("D4 消息三")
                            }
                        }
                        Text { attr { text("D4 · 点击连续 announce x3"); fontSize(16f) } }
                    }
                }

                // ===== E 组：accessibilityFocus =====
                sectionHeader("E. accessibilityFocus 焦点跳转")

                // E1 目标：本身在页面顶部（Scroller 首位置），受 ref 引用；同时被 D 组当作 announce 载体
                View {
                    ref { ctx.e1TopRef = it }
                    attr {
                        size(width = 260f, height = 48f)
                        margin(left = 20f, right = 20f, bottom = 12f)
                        backgroundColor(Color(0xFFFFC1B6))
                        justifyContentCenter()
                        padding(left = 12f)
                        accessibility("E1 顶部目标视图")
                        accessibilityRole(AccessibilityRole.BUTTON)
                    }
                    Text { attr { text("E1 · 顶部目标（focus 目标）"); fontSize(16f) } }
                }

                caseCard(
                    title = "E1 · focus 到本页顶部（CAPI view）",
                    expect = "点击后读屏焦点框跳到上方的橙色 \"E1 顶部目标\"",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFFFF3CD))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("E1 触发按钮")
                            accessibilityRole(AccessibilityRole.BUTTON)
                        }
                        event {
                            click {
                                ctx.lastActionLog = "E1 focus → 顶部 CAPI view"
                                ctx.e1TopRef?.view?.accessibilityFocus()
                            }
                        }
                        Text { attr { text("E1 · 点击 focus 到顶部"); fontSize(16f) } }
                    }
                }

                caseCard(
                    title = "E2 · focus 到 ArkTS view",
                    expect = "点击后读屏焦点框跳到 D2/E2 的黄色 ArkTS 目标",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFFFF3CD))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("E2 触发按钮")
                            accessibilityRole(AccessibilityRole.BUTTON)
                        }
                        event {
                            click {
                                ctx.lastActionLog = "E2 focus → ArkTS view"
                                ctx.e2ArkTsRef?.view?.accessibilityFocus()
                            }
                        }
                        Text { attr { text("E2 · 点击 focus 到 ArkTS 目标"); fontSize(16f) } }
                    }
                }

                caseCard(
                    title = "E3 · focus 到视口之外（底部视图）",
                    expect = "点击后读屏焦点框跳到页面底部的绿色 \"E3 底部目标\"（可能伴随自动滚动）",
                ) {
                    View {
                        attr {
                            size(width = 260f, height = 48f)
                            backgroundColor(Color(0xFFFFF3CD))
                            justifyContentCenter()
                            padding(left = 12f)
                            accessibility("E3 触发按钮")
                            accessibilityRole(AccessibilityRole.BUTTON)
                        }
                        event {
                            click {
                                ctx.lastActionLog = "E3 focus → 底部视图"
                                ctx.e3BottomRef?.view?.accessibilityFocus()
                            }
                        }
                        Text { attr { text("E3 · 点击 focus 到底部"); fontSize(16f) } }
                    }
                }

                // E3 目标（底部）
                View {
                    ref { ctx.e3BottomRef = it }
                    attr {
                        size(width = 260f, height = 48f)
                        margin(left = 20f, right = 20f, top = 12f, bottom = 24f)
                        backgroundColor(Color(0xFFB6E3C6))
                        justifyContentCenter()
                        padding(left = 12f)
                        accessibility("E3 底部目标视图")
                        accessibilityRole(AccessibilityRole.BUTTON)
                    }
                    Text { attr { text("E3 · 底部目标（focus 目标）"); fontSize(16f) } }
                }

                // 底部占位，避免最后一条卡片贴到屏幕边缘
                View {
                    attr {
                        height(40f)
                    }
                }
            }
        }
    }
}

/**
 * section 标题条。
 */
private fun ViewContainer<*, *>.sectionHeader(title: String) {
    View {
        attr {
            height(36f)
            padding(left = 20f, right = 20f)
            justifyContentCenter()
            backgroundColor(Color(0xFFDDDDDD))
        }
        Text {
            attr {
                text(title)
                fontSize(14f)
                color(Color(0xFF333333))
            }
        }
    }
}

/**
 * 单个用例卡片：标题 + 预期 + 内容。
 * 卡片本身设 accessibilityRole(NONE)，避免抢占子内容的无障碍焦点。
 */
private fun ViewContainer<*, *>.caseCard(
    title: String,
    expect: String,
    content: ViewContainer<*, *>.() -> Unit,
) {
    View {
        attr {
            margin(left = 20f, right = 20f, top = 12f)
            padding(top = 8f, bottom = 8f, left = 12f, right = 12f)
            backgroundColor(Color.WHITE)
            // 卡片本身不参与无障碍聚焦，让内部的用例 view 单独被聚焦。
            accessibilityRole(AccessibilityRole.NONE)
        }
        Text {
            attr {
                text(title)
                fontSize(14f)
                color(Color(0xFF111111))
                marginBottom(4f)
            }
        }
        Text {
            attr {
                text("预期: $expect")
                fontSize(12f)
                color(Color(0xFF666666))
                marginBottom(8f)
            }
        }
        this.content()
    }
}
