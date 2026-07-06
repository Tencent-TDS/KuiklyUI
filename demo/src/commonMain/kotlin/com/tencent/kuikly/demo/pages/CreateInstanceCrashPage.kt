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

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 诊断用页面：在 Pager 构造阶段（K/N 侧 createInstance 调用同步执行时）
 * 主动抛出未捕获异常，用于验证 Native 侧的诊断日志链是否生效：
 *   1) [callKotlin_] ... method=1 type=<T>              (DefaultRenderNativeContextHandler)
 *   2) [KRThread.DirectRunOnCurThread.borrow] uncaught non-std exception (type=<T>)
 *      (KRThreadFatalGuard)
 *
 * 用法：把宿主启动的 pageName 切到 "CreateInstanceCrashPage" 即可复现。
 * 复现后应立即回滚到正常页面，勿在生产版本发布此页。
 */
@Page("CreateInstanceCrashPage")
internal class CreateInstanceCrashPage : BasePager() {

    init {
        // 这里位于 Pager 子类构造过程中，
        // 也就是 KRRenderCore::DidInit -> CallKotlinMethod(CreateInstance=1)
        // 同步派发到 K/N 后立刻会走到的路径。
        throw Exception("intentional crash in CreateInstanceCrashPage.init for diagnostics")
    }

    override fun body(): ViewBuilder {
        return {
            // unreachable：init 已抛出，body 不会被执行
        }
    }
}
