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

#include <assert.h>
#include "DefaultRenderNativeContextHandler.h"

extern CallKotlin callKotlin_;

void DefaultRenderNativeContextHandler::CallKotlinMethod(const KuiklyRenderContextMethod &method,
                                                         const std::shared_ptr<KRRenderValue> &arg0,
                                                         const std::shared_ptr<KRRenderValue> &arg1,
                                                         const std::shared_ptr<KRRenderValue> &arg2,
                                                         const std::shared_ptr<KRRenderValue> &arg3,
                                                         const std::shared_ptr<KRRenderValue> &arg4,
                                                         const std::shared_ptr<KRRenderValue> &arg5) {
    if(callKotlin_ == nullptr){
        __assert_fail("Tips: make sure initKuikly() has been called!", __FILE__, __LINE__, __func__);
    }
    callKotlin_(static_cast<int>(method), arg0->toCValue(), arg1->toCValue(), arg2->toCValue(), arg3->toCValue(),
                arg4->toCValue(), arg5->toCValue());
}
