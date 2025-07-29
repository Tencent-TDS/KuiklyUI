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

#ifndef CORE_RENDER_OHOS_KRRENDEREXECUTEMODEWRAPPER_H
#define CORE_RENDER_OHOS_KRRENDEREXECUTEMODEWRAPPER_H

#include "libohos_render/context/IKRRenderNativeContextHandler.h"
#include "libohos_render/context/KRRenderExecuteMode.h"

class KRRenderExecuteModeWrapper {
 public:
    KRRenderExecuteModeWrapper(const int mode, const KRRenderExecuteModeCreator &mode_creator,
                               const KRRenderContextHandlerCreator &context_creator);
    int GetMode();
    KRRenderExecuteModeCreator GetExecuteModeCreator();
    KRRenderContextHandlerCreator GetContextHandlerCreator();

 private:
    int mode_ = 0;
    KRRenderExecuteModeCreator execute_mode_creator_;
    KRRenderContextHandlerCreator context_handler_creator_;
};

#endif  // CORE_RENDER_OHOS_KRRENDEREXECUTEMODEWRAPPER_H
