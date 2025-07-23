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

#ifndef CORE_RENDER_OHOS_KRRENDEREXECUTEMODE_H
#define CORE_RENDER_OHOS_KRRENDEREXECUTEMODE_H

#include <functional>
class KRRenderExecuteMode;
using KRRenderExecuteModeCreator = std::function<std::shared_ptr<KRRenderExecuteMode>()>;

class KRRenderExecuteMode {
 public:
    explicit KRRenderExecuteMode(int mode);
    virtual bool IsContextSyncInit() = 0;  // ContextHandler是否同步初始化
    virtual int ModeToCoreValue() = 0;     //  render mode映射到Core的对应值
    int GetMode();

    //  注册自定义ExecuteMode创建器
    static void RegisterExecuteModeCreator(const int &mode, const KRRenderExecuteModeCreator &creator) {
        GetExecuteModeCreatorRegister()[mode] = creator;
    }
    static std::unordered_map<int, KRRenderExecuteModeCreator> &GetExecuteModeCreatorRegister() {
        static std::unordered_map<int, KRRenderExecuteModeCreator> gRegisterExecuteModeCreator;
        return gRegisterExecuteModeCreator;
    }

 private:
    int mode_ = 0;  //  运行模式
};

#endif  // CORE_RENDER_OHOS_KRRENDEREXECUTEMODE_H
