/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#ifndef CORE_RENDER_OHOS_IKRSCHEDULER_H
#define CORE_RENDER_OHOS_IKRSCHEDULER_H

#include <functional>

using KRSchedulerTask = std::function<void()>;

class IKRScheduler : public std::enable_shared_from_this<IKRScheduler> {
 public:
    virtual ~IKRScheduler() = default;
};

#endif  // CORE_RENDER_OHOS_IKRSCHEDULER_H
