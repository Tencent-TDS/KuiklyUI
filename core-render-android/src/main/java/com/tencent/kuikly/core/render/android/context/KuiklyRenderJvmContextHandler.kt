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

import com.tencent.kuikly.core.IKuiklyCoreEntry
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.render.android.css.ktx.isMainThread
import com.tencent.kuikly.core.render.android.exception.ErrorReason

/**
 * 渲染流程在JVM环境执行的处理器
 */
class KuiklyRenderJvmContextHandler : KuiklyRenderCommonContextHandler(), IKuiklyCoreEntry.Delegate {

    private val kuiklyCoreEntry = newKuiklyCoreEntryInstance()

    init {
        kuiklyCoreEntry.delegate = this
    }

    override fun init(contextCode: String) {}

    override fun callKotlinMethod(
        methodId: Int,
        arg0: Any?,
        arg1: Any?,
        arg2: Any?,
        arg3: Any?,
        arg4: Any?,
        arg5: Any?
    ) {
        try {
            kuiklyCoreEntry.callKotlinMethod(methodId, arg0, arg1, arg2, arg3, arg4, arg5)
        } catch (t: Throwable) {
            // 这里catch的异常类型是故意设置成Throwable的，因为callKotlinMethod运行的是KTV业务代码
            // 因此需要catch顶层的类型异常，保证能catch到业务异常.
            // 在catch到异常后, debug包下抛出异常, release模式下打印error日志并且做上报
            // 为啥不用Thread.UncaughtExceptionHandler来捕获线程异常：
            // 使用UncaughtExceptionHandler来捕获的话，当异常发生时，KTV线程已经挂掉了，因此所有KTV页面都使用不了
            // 使用try-catch的话，能保证KTV线程一直存活，KTV页面之间的异常不会影响到彼此
            notifyException(t, ErrorReason.CALL_KOTLIN)
        }
    }

    override fun callNative(
        methodId: Int,
        arg0: Any?,
        arg1: Any?,
        arg2: Any?,
        arg3: Any?,
        arg4: Any?,
        arg5: Any?
    ): Any? {
        assert(!isMainThread())
        try {
            val result = callNativeCallback?.invoke(
                KuiklyRenderNativeMethod.fromInt(methodId), listOf(
                    arg0,
                    arg1,
                    arg2,
                    arg3,
                    arg4,
                    arg5
                )
            )
            return result?.toKotlinObject()
        } catch (t: Throwable) {
            // 这里catch的异常类型是故意设置成Throwable的，因为callKotlinMethod运行的是KTV业务代码
            // 因此需要catch顶层的类型异常，保证能catch到业务异常.
            // 在catch到异常后, debug包下抛出异常, release模式下打印error日志并且做上报
            // 为啥不用Thread.UncaughtExceptionHandler来捕获线程异常：
            // 使用UncaughtExceptionHandler来捕获的话，当异常发生时，KTV线程已经挂掉了，因此所有KTV页面都使用不了
            // 使用try-catch的话，能保证KTV线程一直存活，KTV页面之间的异常不会影响到彼此
            notifyException(t, ErrorReason.CALL_NATIVE)
        }
        return null
    }

    companion object {

        private val kuiklyClass = Class.forName("com.tencent.kuikly.core.android.KuiklyCoreEntry")

        fun newKuiklyCoreEntryInstance(): IKuiklyCoreEntry {
            return kuiklyClass.newInstance() as IKuiklyCoreEntry
        }

        fun isPageExist(pageName: String): Boolean {
            newKuiklyCoreEntryInstance().triggerRegisterPages()
            return BridgeManager.isPageExist(pageName)
        }
    }
}
