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
 * Created by kam on 2023/4/21.
 */
interface IKRThreadAdapter {
    fun executeOnSubThread(task: () -> Unit)

    /**
     * increase Kuikly Thread StackSize to avoid StackOverflow Exception under Compose Scene
     *
     * @return <=0 mean use System default, it all depend platform setting, usually
     *             system default is 1024 * 1024
     *         >0 mean you custom the stackSize
     */
    fun stackSize(): Long = 0
}