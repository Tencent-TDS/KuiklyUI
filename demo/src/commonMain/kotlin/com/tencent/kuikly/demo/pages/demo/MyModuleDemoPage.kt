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
import com.tencent.kuikly.core.module.AnyCallbackFn
import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Kotlin 侧 MyModule，对应鸿蒙侧的 KRMyModule
 */
internal class MyModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    /**
     * syncJsonCall: 传递 JSON 字符串，同步获取返回值（返回 JSON 中的 data 字段）
     */
    fun syncJsonCall(data: String): String {
        val json = JSONObject()
        json.put("data", data)
        return syncToNativeMethod("syncJsonCall", json, null)
    }

    /**
     * syncByteArrayCall: 传递 ByteArray，同步获取交换字节后的 ByteArray
     */
    fun syncByteArrayCall(bytes: ByteArray): Any? {
        return syncToNativeMethod("syncByteArrayCall", arrayOf<Any>(bytes), null)
    }

    /**
     * asyncJsonCallback: 传递 JSON 字符串，通过 callback 异步获取结果
     */
    fun asyncJsonCallback(data: String, callbackFn: CallbackFn) {
        val json = JSONObject()
        json.put("data", data)
        toNative(
            false,
            "asyncJsonCallback",
            json.toString(),
            callbackFn,
            false
        )
    }

    /**
     * asyncArrayCallback: 传递基本类型数组，通过 callback 异步获取结果
     */
    fun asyncArrayCallback(content: String, callbackFn: AnyCallbackFn) {
        asyncToNativeMethod("asyncArrayCallback", arrayOf<Any>(content), callbackFn)
    }

    /**
     * asyncMixedArrayCallback: 无参数，通过 callback 异步获取数组结果 [ByteArray, String, String]
     */
    fun asyncMixedArrayCallback(callbackFn: AnyCallbackFn) {
        asyncToNativeMethod("asyncMixedArrayCallback", arrayOf<Any>(""), callbackFn)
    }

    /**
     * retDouble: 无参数，同步返回 Native 侧的 Double 值（3.14）。
     * 注意：同步 JSON 通路(syncToNativeMethod JSONObject 版本)的回传值恒为 String，
     * 因此此处返回 String("3.14")，由 Demo 页直接展示。
     */
    fun retDouble(): String {
        return syncToNativeMethod("retDouble", null, null)
    }

    /**
     * retInt: 无参数，同步返回 Native 侧的 Int 值（314）。
     * 注意：同步 JSON 通路(syncToNativeMethod JSONObject 版本)的回传值恒为 String，
     * 因此此处返回 String("314")，由 Demo 页直接展示。
     */
    fun retInt(): String {
        return syncToNativeMethod("retInt", null, null)
    }

    companion object {
        const val MODULE_NAME = "KRMyModule"
    }
}

/**
 * MyModule Demo 页面，演示 KRMyModule 各方法的调用
 */
@Page("MyModuleDemoPage")
internal class MyModuleDemoPage : BasePager() {

    private var syncJsonCallResult by observable("未调用")
    private var syncByteArrayCallResult by observable("未调用")
    private var asyncJsonCallbackResult by observable("未调用")
    private var asyncArrayCallbackResult by observable("未调用")
    private var asyncMixedArrayCallbackResult by observable("未调用")
    private var retDoubleResult by observable("未调用")
    private var retIntResult by observable("未调用")

    override fun createExternalModules(): Map<String, Module>? {
        val modules = super.createExternalModules()?.toMutableMap() ?: hashMapOf()
        modules[MyModule.MODULE_NAME] = MyModule()
        return modules
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            NavBar {
                attr {
                    title = "MyModule Demo"
                }
            }
            Scroller {
                attr {
                    flex(1f)
                    margin(15f)
                }

                // syncJsonCall 演示
                Text {
                    attr {
                        fontSize(16f)
fontWeightBold()
                        marginTop(10f)
                        text("syncJsonCall - 同步调用(JSON传输)")
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        color(Color(0xFF666666L))
                        marginTop(5f)
                        text("传入JSON字符串，同步返回data字段值")
                    }
                }
                Button {
                    attr {
                        marginTop(8f)
                        height(40f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF3c6cbdL))
                        testTag("call_syncJsonCall")
                        titleAttr {
                            text("调用 syncJsonCall")
                            fontSize(14f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            val module = ctx.acquireModule<MyModule>(MyModule.MODULE_NAME)
                            ctx.syncJsonCallResult = module.syncJsonCall("Hello from Kotlin!")
                        }
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        marginTop(5f)
                        testTag("result_syncJsonCall")
                        text("结果: ${ctx.syncJsonCallResult}")
                    }
                }

                // 分隔线
                View {
                    attr {
                        height(1f)
                        marginTop(15f)
                        backgroundColor(Color(0xFFEEEEEEL))
                    }
                }

                // syncByteArrayCall 演示
                Text {
                    attr {
                        fontSize(16f)
fontWeightBold()
                        marginTop(15f)
                        text("syncByteArrayCall - 同步调用(ByteArray传输)")
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        color(Color(0xFF666666L))
                        marginTop(5f)
                        text("传入ByteArray[1,2,3,4]，Native侧交换字节后返回")
                    }
                }
                Button {
                    attr {
                        marginTop(8f)
                        height(40f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF3c6cbdL))
                        testTag("call_syncByteArrayCall")
                        titleAttr {
                            text("调用 syncByteArrayCall")
                            fontSize(14f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            val module = ctx.acquireModule<MyModule>(MyModule.MODULE_NAME)
                            val inputBytes = byteArrayOf(1, 2, 3, 4)
                            val result = module.syncByteArrayCall(inputBytes)
                            if (result is ByteArray) {
                                ctx.syncByteArrayCallResult = "ByteArray[${result.joinToString(",")}]"
                            } else {
                                ctx.syncByteArrayCallResult = "返回类型: ${result?.let { it::class.simpleName }}, 值: $result"
                            }
                        }
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        marginTop(5f)
                        testTag("result_syncByteArrayCall")
                        text("结果: ${ctx.syncByteArrayCallResult}")
                    }
                }

                // 分隔线
                View {
                    attr {
                        height(1f)
                        marginTop(15f)
                        backgroundColor(Color(0xFFEEEEEEL))
                    }
                }

                // asyncJsonCallback 演示
                Text {
                    attr {
                        fontSize(16f)
fontWeightBold()
                        marginTop(15f)
                        text("asyncJsonCallback - 异步回调(JSON传输)")
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        color(Color(0xFF666666L))
                        marginTop(5f)
                        text("传入JSON字符串，通过callback回调结果")
                    }
                }
                Button {
                    attr {
                        marginTop(8f)
                        height(40f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF3c6cbdL))
                        testTag("call_asyncJsonCallback")
                        titleAttr {
                            text("调用 asyncJsonCallback")
                            fontSize(14f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            val module = ctx.acquireModule<MyModule>(MyModule.MODULE_NAME)
                            module.asyncJsonCallback("Kuikly回调测试") { jsonObj ->
                                ctx.asyncJsonCallbackResult = jsonObj?.toString() ?: "null"
                            }
                        }
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        marginTop(5f)
                        testTag("result_asyncJsonCallback")
                        text("结果: ${ctx.asyncJsonCallbackResult}")
                    }
                }

                // 分隔线
                View {
                    attr {
                        height(1f)
                        marginTop(15f)
                        backgroundColor(Color(0xFFEEEEEEL))
                    }
                }

                // asyncArrayCallback 演示
                Text {
                    attr {
                        fontSize(16f)
fontWeightBold()
                        marginTop(15f)
                        text("asyncArrayCallback - 异步回调(数组传输)")
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        color(Color(0xFF666666L))
                        marginTop(5f)
                        text("传入字符串数组，通过callback回调第一个元素")
                    }
                }
                Button {
                    attr {
                        marginTop(8f)
                        height(40f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF3c6cbdL))
                        testTag("call_asyncArrayCallback")
                        titleAttr {
                            text("调用 asyncArrayCallback")
                            fontSize(14f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            val module = ctx.acquireModule<MyModule>(MyModule.MODULE_NAME)
                            module.asyncArrayCallback("asyncArrayCallback的参数") { result ->
                                ctx.asyncArrayCallbackResult = result?.toString() ?: "null"
                            }
                        }
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        marginTop(5f)
                        testTag("result_asyncArrayCallback")
                        text("结果: ${ctx.asyncArrayCallbackResult}")
                    }
                }

                // 分隔线
                View {
                    attr {
                        height(1f)
                        marginTop(15f)
                        backgroundColor(Color(0xFFEEEEEEL))
                    }
                }

                // asyncMixedArrayCallback 演示
                Text {
                    attr {
                        fontSize(16f)
fontWeightBold()
                        marginTop(15f)
                        text("asyncMixedArrayCallback - 异步回调(混合数组)")
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        color(Color(0xFF666666L))
                        marginTop(5f)
                        text("无参数，回调包含ByteArray和String的数组")
                    }
                }
                Button {
                    attr {
                        marginTop(8f)
                        height(40f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF3c6cbdL))
                        testTag("call_asyncMixedArrayCallback")
                        titleAttr {
                            text("调用 asyncMixedArrayCallback")
                            fontSize(14f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            val module = ctx.acquireModule<MyModule>(MyModule.MODULE_NAME)
                            module.asyncMixedArrayCallback { result ->
                                if (result is Array<*>) {
                                    val sb = StringBuilder("Array[")
                                    result.forEachIndexed { index, item ->
                                        if (index > 0) {
                                            sb.append(", ")
                                        }
                                        when (item) {
                                            is ByteArray -> sb.append("ByteArray[${item.joinToString(",")}]")
                                            else -> sb.append(item.toString())
                                        }
                                    }
                                    sb.append("]")
                                    ctx.asyncMixedArrayCallbackResult = sb.toString()
                                } else {
                                    ctx.asyncMixedArrayCallbackResult = "返回类型: ${result?.let { it::class.simpleName }}, 值: $result"
                                }
                            }
                        }
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        marginTop(5f)
                        testTag("result_asyncMixedArrayCallback")
                        text("结果: ${ctx.asyncMixedArrayCallbackResult}")
                    }
                }

                // 分隔线
                View {
                    attr {
                        height(1f)
                        marginTop(15f)
                        backgroundColor(Color(0xFFEEEEEEL))
                    }
                }

                // retDouble 演示
                Text {
                    attr {
                        fontSize(16f)
fontWeightBold()
                        marginTop(15f)
                        text("retDouble - 同步调用(返回Double)")
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        color(Color(0xFF666666L))
                        marginTop(5f)
                        text("无参数，Native侧同步返回 Double 值 3.14")
                    }
                }
                Button {
                    attr {
                        marginTop(8f)
                        height(40f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF3c6cbdL))
                        testTag("call_retDouble")
                        titleAttr {
                            text("调用 retDouble")
                            fontSize(14f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            val module = ctx.acquireModule<MyModule>(MyModule.MODULE_NAME)
                            ctx.retDoubleResult = module.retDouble().toString()
                        }
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        marginTop(5f)
                        testTag("result_retDouble")
                        text("结果: ${ctx.retDoubleResult}")
                    }
                }

                // 分隔线
                View {
                    attr {
                        height(1f)
                        marginTop(15f)
                        backgroundColor(Color(0xFFEEEEEEL))
                    }
                }

                // retInt 演示
                Text {
                    attr {
                        fontSize(16f)
fontWeightBold()
                        marginTop(15f)
                        text("retInt - 同步调用(返回Int)")
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        color(Color(0xFF666666L))
                        marginTop(5f)
                        text("无参数，Native侧同步返回 Int 值 314")
                    }
                }
                Button {
                    attr {
                        marginTop(8f)
                        height(40f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF3c6cbdL))
                        testTag("call_retInt")
                        titleAttr {
                            text("调用 retInt")
                            fontSize(14f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            val module = ctx.acquireModule<MyModule>(MyModule.MODULE_NAME)
                            ctx.retIntResult = module.retInt().toString()
                        }
                    }
                }
                Text {
                    attr {
                        fontSize(14f)
                        marginTop(5f)
                        testTag("result_retInt")
                        text("结果: ${ctx.retIntResult}")
                    }
                }

                // 底部间距
                View {
                    attr {
                        height(30f)
                    }
                }
            }
        }
    }
}
