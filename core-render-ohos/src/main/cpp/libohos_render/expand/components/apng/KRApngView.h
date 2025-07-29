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

#ifndef CORE_RENDER_OHOS_KRAPNGVIEW_H
#define CORE_RENDER_OHOS_KRAPNGVIEW_H
#include <cstdint>
#include "libohos_render/export/IKRRenderViewExport.h"

class APNGAnimateView;
class KRApngView : public IKRRenderViewExport {
 public:
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    ArkUI_NodeHandle CreateNode() override;

    void DidMoveToParentView() override;
    void SetRenderViewFrame(const KRRect &frame) override;
    void OnDestroy() override;

 private:
    std::shared_ptr<APNGAnimateView> apng_view_;
    std::string file_path_;
    bool did_mounted_ = false;
    std::string src_;
    bool auto_play_ = true;
    uint32_t repeat_count_ = INT32_MAX;
    KRRenderCallback load_failure_callback_ = nullptr;
    KRRenderCallback animation_start_callback_ = nullptr;
    KRRenderCallback animation_end_callback_ = nullptr;
    void SetSrc(std::string &src);
    void SetAutoPlay(bool auto_play);
    void SetRepeatCount(int count);
    void LoadFile(std::string &file_path);
    void CreateAnimatedViewIfNeed();
    void FireLoadFailure();
    void FireAnimationStart();
    void FireAnimationEnd();
};

#endif  // CORE_RENDER_OHOS_KRAPNGVIEW_H
