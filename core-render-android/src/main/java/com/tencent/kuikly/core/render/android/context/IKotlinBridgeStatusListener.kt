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

/**
 * 此接口用于监听 kotlin bridge 状态处于繁忙或者空闲
 */
interface IKotlinBridgeStatusListener {

    /**
     * bridge 状态切换为繁忙
     */
    fun onTransitionBridgeBusy()

    /**
     * bridge 状态切换为空闲
     */
    fun onTransitionBridgeIdle()

}