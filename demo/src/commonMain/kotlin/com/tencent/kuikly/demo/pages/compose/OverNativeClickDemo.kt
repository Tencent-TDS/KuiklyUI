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

package com.tencent.kuikly.demo.pages.compose

import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.setEvent
import com.tencent.kuikly.compose.extension.setProp
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.event.EventName
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.demo.pages.base.BridgeModule
import com.tencent.kuikly.demo.pages.base.TDFTestModule

@Page("OverNativeClickDemo")
internal class OverNativeClickDemo : ComposeContainer() {

    fun toast(msg: String) {
        getModule<BridgeModule>(BridgeModule.MODULE_NAME)?.toast(msg)
    }

    override fun willInit() {
        super.willInit()

        setContent {
            ComposeNavigationBar {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Color.Green.copy(0.3f),
                                ),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .padding(start = 20.dp)
                                    .size(50.dp, 150.dp)
                                    .background(Color.Red.copy(alpha = .75f))
                                    .clickable {
                                        toast("ComposeSceneMediator.result = ComposeClick ")
                                    }
                                    .setEvent(EventName.CLICK.value) {},
                        ) {
                            Text("Compose点击，原生不触发", color = Color.White)
                        }

                        Box(
                            modifier =
                                Modifier
                                    .size(50.dp, 150.dp)
                                    .background(Color.Blue.copy(alpha = .75f))
                                    .clickable {
                                        println("ComposeSceneMediator.result = ComposeClick ")
                                    }
                                    .setProp("stop-propagation-ohos", 1)
                                    .setEvent(EventName.TOUCH_DOWN.value) {}
                                    .setEvent(EventName.TOUCH_MOVE.value) {}
                                    .setEvent(EventName.TOUCH_UP.value) {}
                                    .setEvent(EventName.TOUCH_CANCEL.value) {},
                        ) {
                            Text("ComposeTouch，原生不触发", color = Color.White)
                        }

                    }

                    Box(
                        modifier =
                            Modifier
                                .padding(start = 20.dp)
                                .align(Alignment.Center)
                                .size(50.dp, 150.dp)
                                .background(Color.Red.copy(alpha = .75f))
                                .clickable {
                                    toast("ComposeSceneMediator.result = ComposeClick ")
                                }
//                                .setEvent(EventName.CLICK.value) {},
                    ) {
                        Text("Compose点击，原生不触发", color = Color.White)
                    }
                }
            }
        }
    }

    override fun createExternalModules(): Map<String, Module>? {
        val externalModules = hashMapOf<String, Module>()
        externalModules[BridgeModule.MODULE_NAME] = BridgeModule()
        return externalModules
    }
}
