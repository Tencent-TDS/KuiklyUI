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

package com.tencent.kuikly.core.base

/**
 * 跨平台特性功能开关
 */
object CrossPlatFeature {
    // 是否启用FastCollection，启用后在js宿主会使用js自己的map list set
    var isUseFastCollection = false
    // 是否忽略isRenderViewForFlatLayer判断容器能否展平
    var isIgnoreRenderViewForFlatLayer = false
}