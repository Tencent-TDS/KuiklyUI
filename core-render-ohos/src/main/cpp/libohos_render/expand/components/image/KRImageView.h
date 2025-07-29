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

#ifndef CORE_RENDER_OHOS_KRIMAGEVIEW_H
#define CORE_RENDER_OHOS_KRIMAGEVIEW_H

#include "libohos_render/expand/components/image/KRImageLoadOption.h"
#include "libohos_render/export/IKRRenderViewExport.h"

using namespace std::string_view_literals;
constexpr std::string_view KR_ASSET_PREFIX = "assets://"sv;

class KRImageView : public IKRRenderViewExport {
 public:
    KRImageView() = default;
    KRImageView(const KRImageView &) = delete;
    KRImageView(KRImageView &&) = delete;
    KRImageView &operator=(const KRImageView &) = delete;
    KRImageView &operator=(KRImageView &&) = delete;

    ArkUI_NodeHandle CreateNode() override;
    void DidInit() override;
    bool ReuseEnable() override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    bool ResetProp(const std::string &prop_key) override;
    void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;
    void OnDestroy() override;

 private:
    bool SetImageSrc(const KRAnyValue &value);
    bool SetResizeMode(const KRAnyValue &value);
    bool SetDragEnable(const KRAnyValue &value);
    bool SetBlurRadius(const KRAnyValue &value);
    bool SetTintColor(const KRAnyValue &value);
    bool SetCapInsets(const KRAnyValue &value);
    bool SetDotNineImage(const KRAnyValue &value);
    bool SetMaskLinearGradient(const KRAnyValue &value);
    void ResetMaskLinearGradientNode();
    bool RegisterLoadSuccessCallback(const KRRenderCallback &event_callback);
    bool RegisterLoadResolutionCallback(const KRRenderCallback &event_callback);
    bool RegisterLoadFailureCallback(const KRRenderCallback &event_callback);
    void FireOnImageCompleteEvent(ArkUI_NodeEvent *event);
    void FireOnImageErrorEvent(ArkUI_NodeEvent *event);
    std::shared_ptr<KRImageLoadOption> ToImageLoadOption(const std::string &src);
    void LoadFromSrc(const std::string image_src);
    void LoadFromNetwork(const std::shared_ptr<KRImageLoadOption> image_option);
    void LoadFromBase64(const std::shared_ptr<KRImageLoadOption> image_option);
    void LoadFromFile(const std::shared_ptr<KRImageLoadOption> image_option);
    void LoadFromResourceMedia(const std::shared_ptr<KRImageLoadOption> image_option);
    void LoadFromAssets(const std::shared_ptr<KRImageLoadOption> image_option);

 private:
    std::string image_src_;
    std::shared_ptr<KRImageLoadOption> image_option_ = nullptr;
    KRRenderCallback load_success_callback_ = nullptr;
    KRRenderCallback load_resolution_callback_ = nullptr;
    KRRenderCallback load_failure_callback_ = nullptr;
    bool had_register_on_complete_event_ = false;
    bool had_register_on_error_event_ = false;
    bool is_dot_nine_image_ = false;
    ArkUI_NodeHandle mask_linear_gradient_node_ = nullptr;
};

#endif  // CORE_RENDER_OHOS_KRIMAGEVIEW_H
