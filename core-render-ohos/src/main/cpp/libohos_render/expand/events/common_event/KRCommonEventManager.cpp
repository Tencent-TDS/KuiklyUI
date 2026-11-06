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

#include "libohos_render/expand/events/common_event/KRCommonEventManager.h"

#include <vector>

#include "BasicServicesKit/oh_commonevent.h"
#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/manager/KRRenderManager.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/view/KRRenderView.h"

namespace kuikly {

namespace {
constexpr char kCommonEventLogTag[] = "KRCommonEvent";

/**
 * 系统公共事件回调（线程不保证为渲染主线程）
 */
void OnSystemCommonEventReceived(const CommonEvent_RcvData *data) {
    if (data == nullptr) {
        return;
    }
    const char *event = OH_CommonEvent_GetEventFromRcvData(data);
    if (event == nullptr) {
        return;
    }
    CommonEventName name;
    if (!CommonEventNameFromEventString(event, name)) {
        KR_LOG_INFO_WITH_TAG(kCommonEventLogTag) << "ignore unregistered common event: " << event;
        return;
    }
    // 公共事件回调线程既不是主线程也不是 kuikly worker 线程（实测为 FFRT worker），
    // 因此必须用 KRMainThread::RunOnMainThread 投递；KRContextScheduler::ScheduleTaskOnMainThread
    // 只接受主线程/worker 线程调用，第三方线程调用会断言失败。
    KRMainThread::RunOnMainThread([name]() {
        KRCommonEventManager::GetInstance().DispatchEventToInstances(name);
    });
}
}  // namespace

KRCommonEventManager &KRCommonEventManager::GetInstance() {
    static KRCommonEventManager instance;
    return instance;
}

void KRCommonEventManager::Subscribe(CommonEventName name, const std::string &instance_id) {
    if (instance_id.empty()) {
        return;
    }
    bool need_system_subscribe = false;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        subscribed_instances_[name].insert(instance_id);
        // 系统订阅尚未生效（首个订阅者，或此前订阅失败需要重试）
        need_system_subscribe = system_subscribers_.find(name) == system_subscribers_.end();
    }
    if (need_system_subscribe) {
        SubscribeSystemEvent(name);
    }
}

void KRCommonEventManager::Unsubscribe(CommonEventName name, const std::string &instance_id) {
    if (instance_id.empty()) {
        return;
    }
    bool need_system_unsubscribe = false;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        auto it = subscribed_instances_.find(name);
        if (it == subscribed_instances_.end()) {
            return;
        }
        it->second.erase(instance_id);
        if (it->second.empty()) {
            subscribed_instances_.erase(it);
            need_system_unsubscribe = true;
        }
    }
    if (need_system_unsubscribe) {
        UnsubscribeSystemEvent(name);
    }
}

bool KRCommonEventManager::SubscribeSystemEvent(CommonEventName name) {
    const char *event_name = CommonEventNameToEventString(name);
    if (event_name == nullptr || event_name[0] == '\0') {
        return false;
    }
    {
        std::lock_guard<std::mutex> lock(mutex_);
        if (system_subscribers_.find(name) != system_subscribers_.end()) {
            return true;
        }
    }

    const char *events[] = {event_name};
    CommonEvent_SubscribeInfo *info = OH_CommonEvent_CreateSubscribeInfo(events, 1);
    if (info == nullptr) {
        KR_LOG_ERROR_WITH_TAG(kCommonEventLogTag) << "create subscribe info failed: " << event_name;
        return false;
    }
    CommonEvent_Subscriber *subscriber = OH_CommonEvent_CreateSubscriber(info, OnSystemCommonEventReceived);
    if (subscriber == nullptr) {
        KR_LOG_ERROR_WITH_TAG(kCommonEventLogTag) << "create subscriber failed: " << event_name;
        OH_CommonEvent_DestroySubscribeInfo(info);
        return false;
    }
    const CommonEvent_ErrCode ret = OH_CommonEvent_Subscribe(subscriber);
    if (ret != COMMONEVENT_ERR_OK) {
        KR_LOG_ERROR_WITH_TAG(kCommonEventLogTag)
            << "subscribe failed, event=" << event_name << ", ret=" << static_cast<int>(ret);
        OH_CommonEvent_DestroySubscriber(subscriber);
        OH_CommonEvent_DestroySubscribeInfo(info);
        return false;
    }
    {
        std::lock_guard<std::mutex> lock(mutex_);
        system_subscribers_[name] = subscriber;
        system_subscribe_infos_[name] = info;
    }
    KR_LOG_INFO_WITH_TAG(kCommonEventLogTag) << "subscribe success, event=" << event_name;
    return true;
}

void KRCommonEventManager::UnsubscribeSystemEvent(CommonEventName name) {
    CommonEvent_Subscriber *subscriber = nullptr;
    CommonEvent_SubscribeInfo *info = nullptr;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        auto subscriber_it = system_subscribers_.find(name);
        if (subscriber_it == system_subscribers_.end()) {
            return;
        }
        subscriber = subscriber_it->second;
        system_subscribers_.erase(subscriber_it);
        auto info_it = system_subscribe_infos_.find(name);
        if (info_it != system_subscribe_infos_.end()) {
            info = info_it->second;
            system_subscribe_infos_.erase(info_it);
        }
    }
    if (subscriber != nullptr) {
        const CommonEvent_ErrCode ret = OH_CommonEvent_UnSubscribe(subscriber);
        if (ret != COMMONEVENT_ERR_OK) {
            KR_LOG_ERROR_WITH_TAG(kCommonEventLogTag)
                << "unsubscribe failed, event=" << CommonEventNameToEventString(name)
                << ", ret=" << static_cast<int>(ret);
        }
        OH_CommonEvent_DestroySubscriber(subscriber);
    }
    if (info != nullptr) {
        OH_CommonEvent_DestroySubscribeInfo(info);
    }
    KR_LOG_INFO_WITH_TAG(kCommonEventLogTag)
        << "unsubscribe finish, event=" << CommonEventNameToEventString(name);
}

void KRCommonEventManager::DispatchEventToInstances(CommonEventName name) {
    std::vector<std::string> instance_ids;
    {
        std::lock_guard<std::mutex> lock(mutex_);
        auto it = subscribed_instances_.find(name);
        if (it == subscribed_instances_.end()) {
            return;
        }
        instance_ids.assign(it->second.begin(), it->second.end());
    }
    size_t dispatched = 0;
    for (const auto &instance_id : instance_ids) {
        auto render_view = KRRenderManager::GetInstance().GetRenderView(instance_id);
        if (render_view == nullptr) {
            // 实例尚未创建完成或已销毁，忽略
            continue;
        }
        render_view->OnCommonEvent(name);
        dispatched++;
    }
    KR_LOG_INFO_WITH_TAG(kCommonEventLogTag)
        << "dispatch event=" << CommonEventNameToEventString(name) << ", registered=" << instance_ids.size()
        << ", dispatched=" << dispatched;
}

}  // namespace kuikly
