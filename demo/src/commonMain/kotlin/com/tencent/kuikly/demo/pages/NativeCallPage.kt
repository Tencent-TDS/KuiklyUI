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

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.manager.IBridgeCallObserver
import com.tencent.kuikly.core.manager.NativeMethod
import com.tencent.kuikly.core.views.Text

@Page("NativeCallPage")
internal class NativeCallPage : BasePager() {
    init {
        BridgeManager.addCallObserver(object : IBridgeCallObserver {
            override fun onCallNative(methodId: Int, vararg args: Any?) {
                if (methodId == NativeMethod.CALL_MODULE_METHOD && args.getOrNull(1) == "KRLogModule") {
                    return
                }
                KLog.i("NativeCall", "callNative: $methodId, $args")
            }

            override fun onCallKotlin(methodId: Int, vararg args: Any?) {
                KLog.i("NativeCall", "callKotlin: $methodId, $args")
            }
        })
    }

    override fun onDestroyPager() {
        BridgeManager.removeCallObserver()
    }
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }
            Text {
                attr {
                    fontSize(24f)
                    text("hello world")
                }
            }
        }
    }
}
