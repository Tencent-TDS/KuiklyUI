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

package com.tencent.kuikly.demo.pages.base

/**
 * 配置 Kotlin/Native GC 参数。
 * 应在 Kotlin 初始化时调用一次（通过 @EagerInitialization 自动触发）。
 */
expect fun configureGC()

/**
 * 暂停 GC。内部会控制调用频率，避免过于频繁的 suspend/resume 切换。
 * 连续调用多次 suspendGC 只会在首次生效。
 *
 * 注意：当前实现未做线程同步（如加锁），因为 suspendGC/resumeGC 仅在 Kuikly 主线程中调用，
 * 不存在多线程竞争问题。如果后续需要在多线程环境中使用，需要增加同步机制。
 */
expect fun suspendGC()

/**
 * 恢复 GC。与 suspendGC 配对使用。
 *
 * 注意：同 suspendGC，仅在 Kuikly 主线程中调用，无需线程同步。
 */
expect fun resumeGC()
