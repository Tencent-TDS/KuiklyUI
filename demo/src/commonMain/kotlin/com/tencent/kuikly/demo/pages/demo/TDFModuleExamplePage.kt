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

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.module.NewTestModule
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.TDFTestModule
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("TDFModuleExample")
internal class TDFModuleExamplePage : BasePager() {

    companion object {
        private const val TAG = "TDFModuleExamplePage"
    }

    override fun createExternalModules(): Map<String, Module>? {
        val modules = super.createExternalModules() as HashMap
        modules[NewTestModule.MODULE_NAME] = NewTestModule()
        return modules
    }

    fun testNewTestModule() {
        val module = getPager().acquireModule<NewTestModule>(NewTestModule.MODULE_NAME)
        val result1 = module.fun1(
            hashMapOf("a" to 1, "b" to 2),
//                            listOf(1, 2, 3, 5),
            null,
            123,
            "gqweo",
            null,
            123.13415,
            hashSetOf(12, 123, 232),
            8
        )
        val result2 = module.fun2("qwoeigh", {
            KLog.d(TAG, "sync call succ: ${it.toString()}")
        }, {
            KLog.d(TAG, "sync call error: ${it}")
        })
        val result3 = module.fun3(true, 124.2135215, 12345.214f, 15, 23)
        KLog.d("xxxx", "result: $result2")
    }
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar { attr { title = "TDF Module Example" } }
            View {
                attr {
                    padding(5.0f)
                    margin(10.0f)
                    borderRadius(12.0f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color(0xFFFB8C00)))
                    allCenter()
                    height(50.0f)
                }
                Text {
                    attr {
                        fontSize(18.0f)
                        color(Color(0xFFFB8C00))
                        text("sync call")
                    }
                }
                event {
                    click {
                        val value = PagerManager.getCurrentPager().acquireModule<TDFTestModule>(
                            TDFTestModule.MODULE_NAME).syncCall("", 1, 2.0, false, 3.0f, listOf("1", 2, 3), mapOf("a" to 1, "b" to null, "c" to listOf<String>("iuwe")))
                        KLog.d(TAG, "sync call: $value")
                    }
                }
            }

            View {
                attr {
                    padding(5.0f)
                    margin(10.0f)
                    borderRadius(12.0f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color(0xFFFB8C00)))
                    allCenter()
                    height(50.0f)
                }
                Text {
                    attr {
                        fontSize(18.0f)
                        color(Color(0xFFFB8C00))
                        text("sync call with return value")
                    }
                }
                event {
                    click {
                        val value = PagerManager.getCurrentPager().acquireModule<TDFTestModule>(
                            TDFTestModule.MODULE_NAME).syncCallWithReturnValue("", 1, 2.0, false, 3.0f, listOf("1", 2, 3), mapOf("a" to 1, "b" to null))
                        KLog.d(TAG, "sync call with return value: $value")
                    }
                }
            }

            View {
                attr {
                    padding(5.0f)
                    margin(10.0f)
                    borderRadius(12.0f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color(0xFFFB8C00)))
                    allCenter()
                    height(50.0f)
                }
                Text {
                    attr {
                        fontSize(18.0f)
                        color(Color(0xFFFB8C00))
                        text("async call success")
                    }
                }
                event {
                    click {
                        PagerManager.getCurrentPager().acquireModule<TDFTestModule>(
                            TDFTestModule.MODULE_NAME).asyncCall(true)
                    }
                }
            }

            View {
                attr {
                    padding(5.0f)
                    margin(10.0f)
                    borderRadius(12.0f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color(0xFFFB8C00)))
                    allCenter()
                    height(50.0f)
                }
                Text {
                    attr {
                        fontSize(18.0f)
                        color(Color(0xFFFB8C00))
                        text("async call error")
                    }
                }
                event {
                    click {
                        PagerManager.getCurrentPager().acquireModule<TDFTestModule>(
                            TDFTestModule.MODULE_NAME).asyncCall(false)
                    }
                }
            }

        }
    }
}