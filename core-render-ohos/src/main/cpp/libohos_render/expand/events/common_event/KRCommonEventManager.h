/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

#ifndef CORE_RENDER_OHOS_KRCOMMONEVENTMANAGER_H
#define CORE_RENDER_OHOS_KRCOMMONEVENTMANAGER_H

#include <map>
#include <mutex>
#include <set>
#include <string>

#include "libohos_render/expand/events/common_event/KRCommonEventName.h"

// 系统 CommonEvent 句柄类型（定义于 BasicServicesKit/oh_commonevent.h），保持全局命名空间
typedef void CommonEvent_Subscriber;
typedef struct CommonEvent_SubscribeInfo CommonEvent_SubscribeInfo;

namespace kuikly {

/**
 * 系统公共事件（CommonEvent）订阅管理器。
 *
 * 设计要点：
 * - 进程内单例，按"事件名 → 已订阅实例集合"登记，实例集合由空变非空才向系统发起订阅，
 *   由非空变空才向系统退订，避免空订阅常驻与重复订阅。
 * - 系统回调所在线程不保证是渲染主线程，统一调度回主线程后再按 instanceId 分发。
 * - 订阅失败（权限/系统限制等）视为系统级问题：记录错误日志后不再重试，也不参与上层行为决策。
 */
class KRCommonEventManager {
 public:
    static KRCommonEventManager &GetInstance();

    KRCommonEventManager(const KRCommonEventManager &) = delete;
    KRCommonEventManager &operator=(const KRCommonEventManager &) = delete;
    KRCommonEventManager(const KRCommonEventManager &&) = delete;
    KRCommonEventManager &operator=(const KRCommonEventManager &&) = delete;

    /**
     * 订阅某系统事件（按实例登记）
     * @param name 事件枚举
     * @param instance_id 渲染实例标识（KRRenderView 的 instanceId）
     */
    void Subscribe(CommonEventName name, const std::string &instance_id);

    /**
     * 退订某系统事件（移除该实例的登记）
     * @param name 事件枚举
     * @param instance_id 渲染实例标识
     */
    void Unsubscribe(CommonEventName name, const std::string &instance_id);

    /**
     * 将事件分发给当前登记的全部实例（要求渲染主线程调用）
     */
    void DispatchEventToInstances(CommonEventName name);

 private:
    KRCommonEventManager() = default;
    ~KRCommonEventManager() = default;

    bool SubscribeSystemEvent(CommonEventName name);
    void UnsubscribeSystemEvent(CommonEventName name);

    std::mutex mutex_;
    // 事件 → 已登记的实例集合
    std::map<CommonEventName, std::set<std::string>> subscribed_instances_;
    // 事件 → 系统订阅句柄（仅订阅成功时存在）
    std::map<CommonEventName, CommonEvent_Subscriber *> system_subscribers_;
    std::map<CommonEventName, CommonEvent_SubscribeInfo *> system_subscribe_infos_;
};

}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRCOMMONEVENTMANAGER_H
