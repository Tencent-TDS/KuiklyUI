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

package com.tencent.kuikly.core.module

class NewTestModule: Module() {
    override fun moduleName(): String {
        return MODULE_NAME
    }

    fun fun1(a1: HashMap<Any, Any>,
             a2: List<Any>?,
             a3: Int,
             a4: String,
             a5: Float?,
             a6: Double,
             a7: HashSet<Any>,
             a8: Int) {
        // set 类型不支持
        val params = listOf<Any?>(a1, a2, a3, a4, a5, a6, a7, a8)
        toTDFNative(
            false,
            "fun1",
            params,
            null,
            null,
            SYNC_CALL
            )
    }

    fun fun2(a1: String,
             a2: TDFModuleCallbackFn,
             a3: TDFModuleCallbackFn) {
        val params = listOf<Any?>(a1)
        toTDFNative(
            false,
            "fun2",
            params,
            a2,
            a3,
            SYNC_CALL
        )
    }

    fun fun3(a1: Boolean,
             a2: Double,
             a3: Float,
             a4: Int,
             a5: Int) {
        val params = listOf<Any?>(a1, a2, a3, a4, a5)
        toTDFNative(
            false,
            "fun3",
            params,
            null,
            null,
            SYNC_CALL
        )
    }

//    fun fun4(a3: Short,
//             a4: Long,
//             a5: ULong) {
//        val params = listOf<Any?>(a3, a4, a5, a6)
//        toTdfNative(
//            false,
//            "fun4",
//            params,
//            null,
//            null,
//            SYNC_CALL
//        )
//    }

    companion object {
        const val MODULE_NAME = "NewTestModule"
        const val SYNC_CALL = false
    }
}