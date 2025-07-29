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

#include "KRRenderExecuteModeWrapper.h"

KRRenderExecuteModeWrapper::KRRenderExecuteModeWrapper(const int mode, const KRRenderExecuteModeCreator &mode_creator,
                                                       const KRRenderContextHandlerCreator &context_creator) {
    mode_ = mode;
    execute_mode_creator_ = mode_creator;
    context_handler_creator_ = context_creator;
}
int KRRenderExecuteModeWrapper::GetMode() {
    return mode_;
}
KRRenderExecuteModeCreator KRRenderExecuteModeWrapper::GetExecuteModeCreator() {
    return execute_mode_creator_;
}

KRRenderContextHandlerCreator KRRenderExecuteModeWrapper::GetContextHandlerCreator() {
    return context_handler_creator_;
}