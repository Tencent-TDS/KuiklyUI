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

package com.tencent.kuikly.core.render.android.context

import com.tencent.kuikly.core.render.android.css.ktx.fifthArg
import com.tencent.kuikly.core.render.android.css.ktx.firstArg
import com.tencent.kuikly.core.render.android.css.ktx.fourthArg
import com.tencent.kuikly.core.render.android.css.ktx.isMainThread
import com.tencent.kuikly.core.render.android.css.ktx.secondArg
import com.tencent.kuikly.core.render.android.css.ktx.sixthArg
import com.tencent.kuikly.core.render.android.css.ktx.thirdArg
import com.tencent.kuikly.core.render.android.css.ktx.toJSONArray
import com.tencent.kuikly.core.render.android.css.ktx.toJSONObject
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.exception.IKuiklyRenderExceptionListener
import org.json.JSONObject

/**
 * KuiklyRender执行模式基类
 */
abstract class KuiklyRenderCommonContextHandler : IKuiklyRenderContextHandler {

    /**
     * KTV侧调用Native的回调
     */
    protected var callNativeCallback: KuiklyRenderNativeMethodCallback? = null

    /**
     * Kotlin Bridge 状态回调
     */
    @Volatile
    private var bridgeStatusListener: IKotlinBridgeStatusListener? = null

    /**
     * 渲染异常监听
     */
    private var exceptionListener : IKuiklyRenderExceptionListener? = null

    @Suppress("UNCHECKED_CAST")
    override fun call(method: KuiklyRenderContextMethod, args: List<Any?>) {
        assert(!isMainThread())

        // 1.list中如果有对象or数组的话，转为json string
        var argsList = mutableListOf<Any?>()
        for (i in args.indices) {
            argsList.add(args[i]?.toKotlinObject())
        }

        // 2.确保argsList的长度为6
        argsList = if (argsList.size >= CALL_ARGS_COUNT) {
            argsList.subList(0, CALL_ARGS_COUNT)
        } else {
            val appendArgCount = CALL_ARGS_COUNT - argsList.size
            for (index in 0 until appendArgCount) {
                argsList.add(null)
            }
            argsList
        }

        // 3.to KTV Environment
        bridgeStatusListener?.onTransitionBridgeBusy()
        callKotlinMethod(
            method.ordinal,
            argsList.firstArg(),
            argsList.secondArg(),
            argsList.thirdArg(),
            argsList.fourthArg(),
            argsList.fifthArg(),
            argsList.sixthArg()
        )
        bridgeStatusListener?.onTransitionBridgeIdle()
    }

    override fun registerCallNative(callback: KuiklyRenderNativeMethodCallback) {
        this.callNativeCallback = callback
    }

    /**
     * 调用KTV侧的方法
     *
     * <p>这里的参数列表定义成arg0 ... arg5看起来不太合理，但是也是考虑到kmm的js在iOS上的接口定义问题做出的折中
     *
     * <p>arg0 ... arg5参数的含义，在本类中不感知。由外部与KTV Core两者根据[methodId]来感知
     * @param methodId [KuiklyRenderContextMethod]枚举定义的方法
     * @param arg0 参数1
     * @param arg1 参数2
     * @param arg2 参数3
     * @param arg3 参数4
     * @param arg4 参数5
     * @param arg5 参数6
     */
    abstract fun callKotlinMethod(
        methodId: Int,
        arg0: Any?,
        arg1: Any?,
        arg2: Any?,
        arg3: Any?,
        arg4: Any?,
        arg5: Any?
    )

    override fun setBridgeStatusListener(listener: IKotlinBridgeStatusListener) {
        this.bridgeStatusListener = listener
    }

    override fun setRenderExceptionListener(listener: IKuiklyRenderExceptionListener?) {
        exceptionListener = listener
    }

    protected fun notifyException(throwable: Throwable, errorReason: ErrorReason) {
        exceptionListener?.onRenderException(throwable, errorReason)
    }

    override fun destroy() {
        exceptionListener = null
        callNativeCallback = null
    }

    fun Any.toKotlinObject(): Any {
        when(this) {
            is Map<*, *> -> {
                return (this as Map<String, Any>).toJSONObject().toString()
            }
            is List<*> -> {
                return (this as List<Any>).toJSONArray().toString()
            }
            is JSONObject -> {
                return this.toString()
            }
            is Array<*> -> {
                if (this.hasByteArrayElement()) { // 对齐iOS，array中有二进制参数才变成array去透传，否则就序列化为json字符串
                    return this
                }
                return (this.toList() as List<Any>).toJSONArray().toString()
            }
            else -> {
                return this
            }
        }
        return this
    }

    fun Array<*>.hasByteArrayElement(): Boolean {
        for (ele in this) {
            if (ele is ByteArray) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val CALL_ARGS_COUNT = 6 // 参数最大数量
    }
}
