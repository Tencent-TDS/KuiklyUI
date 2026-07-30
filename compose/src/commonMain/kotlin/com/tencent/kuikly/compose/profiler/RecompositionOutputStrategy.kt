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

package com.tencent.kuikly.compose.profiler

/**
 * 重组分析数据的输出策略接口。
 *
 * 实现此接口以自定义重组数据的输出方式（如日志、UI overlay、JSON 等）。
 * 多个策略可同时注册，互不干扰。
 */
interface RecompositionOutputStrategy {

    /**
     * 当一个重组帧完成时调用。
     * 包含该帧内的所有追踪事件。
     *
     * @param events 本帧内的事件列表（从 FrameStart 到 FrameEnd）
     */
    fun onFrameComplete(events: List<RecompositionEvent>)

    /**
     * 当分析报告就绪时调用（可选实现）。
     * 默认空实现，策略可按需覆盖。
     *
     * @param report 分析报告
     */
    fun onReportReady(report: RecompositionReport) {}

    /**
     * 当 Profiler 被重置时调用（可选实现）。
     * 策略应在此清空自身累积数据。
     */
    fun onReset() {}
}
