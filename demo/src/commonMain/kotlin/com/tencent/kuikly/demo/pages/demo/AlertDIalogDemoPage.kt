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
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.AlertDialog

/*
 * 提示对话框组件，UI风格对齐iOS UIAlertController风格, 并支持自定义弹窗UI
 */
@Page("AlertDialogDemo")
internal class AlertDialogPage : Pager() {
    private var showAlert by observable(true)  // 定义响应式变量

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            AlertDialog {
                attr {
                    showAlert(ctx.showAlert)  // 控制Alert是否显示，不显示时不占用布局(必须设置该属性)
                    title("我是Alert标题")
                    message("alert内容")
                    actionButtons("取消", "确定")
                    inWindow(true)
                }
                event {
                    clickActionButton { index ->
                        // 根据index进行确认点击了哪一个button处理对应事件(index值和actionButtons传入button的下标一致)
                        ctx.showAlert = false  // 关闭Alert弹框
                    }
                    willDismiss {
                        // 按下系统返回按键或右滑返回时触发
                        ctx.showAlert = false  // 关闭Alert弹框
                    }
                }
            }
        }
    }
}