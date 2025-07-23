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

#ifndef CORE_RENDER_OHOS_IKRRENDERSHADOWEXPORT_H
#define CORE_RENDER_OHOS_IKRRENDERSHADOWEXPORT_H

#include <string>
#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/foundation/KRSize.h"
#include "libohos_render/scheduler/IKRScheduler.h"
#include "libohos_render/view/IKRRenderView.h"

class IKRRenderShadowExport;
using KRShadowCreator = std::function<std::shared_ptr<IKRRenderShadowExport>()>;

class IKRRenderShadowExport : public std::enable_shared_from_this<IKRRenderShadowExport> {
 public:
    /**
     * 更新 shadow 对象属性时调用
     * @param prop_key 属性名
     * @param prop_value 属性数据
     */
    virtual void SetProp(const std::string &prop_key, const KRAnyValue &prop_value) = 0;

    /**
     * 调用 shadow 对象方法
     * @param method_name
     * @param params
     * @return
     */
    virtual KRAnyValue Call(const std::string &method_name, const std::string &params) {
        return std::make_shared<KRRenderValue>(nullptr);
    }

    /**
     * 根据布局约束尺寸计算返回 RenderView 的实际尺寸
     * @param constraint_width
     * @param constraint_height
     * @return
     */
    virtual KRSize CalculateRenderViewSize(double constraint_width, double constraint_height) = 0;

    /**
     * 将要SetShadow调用
     * @return
     */
    virtual KRSchedulerTask TaskToMainQueueWhenWillSetShadowToView() = 0;

    /**
     * 注册View创建器
     * @param creator
     */
    static void RegisterShadowCreator(const std::string &view_name, const KRShadowCreator &creator) {
        GetRegisterShadowCreator()[view_name] = creator;
    }

    static std::unordered_map<std::string, KRShadowCreator> &GetRegisterShadowCreator() {
        static std::unordered_map<std::string, KRShadowCreator> gRegisterShadowCreator;
        return gRegisterShadowCreator;
    }

    /**
     * 指定ViewName生成View
     * @param view_name
     * @return 新的View实例
     */
    static std::shared_ptr<IKRRenderShadowExport> CreateShadow(const std::string &view_name) {
        auto it = GetRegisterShadowCreator().find(view_name);
        if (it != GetRegisterShadowCreator().end()) {
            return it->second();
        }
        return nullptr;
    }

    void SetRootView(std::weak_ptr<IKRRenderView> root_view) {
        root_view_ = root_view;
    }

    const std::weak_ptr<IKRRenderView> GetRootView() {
        return root_view_;
    }

 private:
    std::weak_ptr<IKRRenderView> root_view_;
};

#endif  // CORE_RENDER_OHOS_IKRRENDERSHADOWEXPORT_H
