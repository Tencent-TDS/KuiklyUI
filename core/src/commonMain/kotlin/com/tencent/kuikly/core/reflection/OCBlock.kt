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

package com.tencent.kuikly.core.reflection

/*
 * 调用OC方法中若有闭包，请使用以下类来创建闭包参数传入call方法参数中
 */
// 空参数OC 闭包使用
class OCBlock0(val block: () -> Unit)

// 单个参数Native闭包使用
class OCBlock1(val block: (arg1: NativeObject<*>?) -> Unit)

// 2个参数Native闭包
class OCBlock2(val block: (arg1: NativeObject<*>?, arg2: NativeObject<*>?) -> Unit)

// 3个参数Native闭包使用
class OCBlock3(val block: (arg1: NativeObject<*>?, arg2: NativeObject<*>?, arg3: NativeObject<*>?) -> Unit)

// 4个参数Native闭包使用
class OCBlock4(val block: (arg1: NativeObject<*>?, arg2: NativeObject<*>?, arg3: NativeObject<*>?, arg4: NativeObject<*>?) -> Unit)

// 5个参数Native闭包使用
class OCBlock5(
    val block: (
        arg1: NativeObject<*>?,
        arg2: NativeObject<*>?,
        arg3: NativeObject<*>?,
        arg4: NativeObject<*>?,
        arg5: NativeObject<*>?
    ) -> Unit
)