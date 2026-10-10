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

#ifndef CORE_RENDER_OHOS_KRMODALVIEW_H
#define CORE_RENDER_OHOS_KRMODALVIEW_H

#include "libohos_render/expand/components/view/KRView.h"
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/utils/KRSystemMaterialUtil.h"
class KRModalView : public KRView {
 public:
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    bool ReuseEnable() override;
    void DidMoveToParentView() override;
    void OnDestroy() override;
    // 材质配置变化时同步到 NativeDialog 背板（dialog 已创建时立即生效）
    void OnSystemMaterialChanged() override;
    void FireDismissEvent(int32_t reason);

 private:
    ArkUI_NativeDialogHandle dialogNode_ = nullptr;
    KRRenderCallback on_will_dismiss_callback_ = nullptr;
    // 弹窗沉浸光感：customStyle 开关 + 应用到 NativeDialog 的材质对象。
    // 材质配置本身由基类 IKRRenderViewExport 持有（has_system_material_ /
    // system_material_options_），因为属性在基类就被消费，子类 SetProp 收不到。
    bool custom_style_ = true;  // 对应 enableCustomStyle，默认 true 保持历史行为
    bool align_center_ = false; // 是否将 dialog 内容相对屏幕居中（默认 TOP_START 保持历史行为）
    void *dialog_material_ = nullptr;
};

#endif  // CORE_RENDER_OHOS_KRMODALVIEW_H
