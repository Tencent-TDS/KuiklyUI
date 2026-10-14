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

#ifndef CORE_RENDER_OHOS_DEFAULTRENDERNATIVECONTEXTHANDLER_H
#define CORE_RENDER_OHOS_DEFAULTRENDERNATIVECONTEXTHANDLER_H

#include "libohos_render/context/IKRRenderNativeContextHandler.h"
class DefaultRenderNativeContextHandler : public IKRRenderNativeContextHandler {
 public:
    void CallKotlinMethod(const KuiklyRenderContextMethod &method, const KRAnyValue &arg0, const KRAnyValue &arg1,
                          const KRAnyValue &arg2, const KRAnyValue &arg3, const KRAnyValue &arg4,
                          const KRAnyValue &arg5) override;
};

#endif  // CORE_RENDER_OHOS_DEFAULTRENDERNATIVECONTEXTHANDLER_H
