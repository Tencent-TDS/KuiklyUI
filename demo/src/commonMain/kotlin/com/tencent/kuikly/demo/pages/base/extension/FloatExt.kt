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

package com.tencent.kuikly.demo.pages.base.extension

import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.manager.PagerManager

// 转化为375宽度坐标的比例大小
internal val Float.to375: Float
    get() = this * (PagerManager.getPager(BridgeManager.currentPageId).pageData.pageViewWidth / 375f)

// 转化为812高度坐标的比例大小
internal val Float.to812: Float
    get() = this * (PagerManager.getPager(BridgeManager.currentPageId).pageData.pageViewHeight / 812f)