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

package com.tencent.kuikly.core.render.android.adapter

/**
 * Created by kam on 2023/3/27.
 */
interface IKRLogAdapter {
    /*
     * 是否支持异步（子线程）打印日志
     * 注：1.对KLog和平台侧日志打印的相对顺序不关注可以返回true，即性能优先
     *    2.无论异步还是同步，KLog接口打印保持相对时序
     */
    val asyncLogEnable: Boolean
    fun i(tag: String, msg: String)
    fun d(tag: String, msg: String)
    fun e(tag: String, msg: String)
}