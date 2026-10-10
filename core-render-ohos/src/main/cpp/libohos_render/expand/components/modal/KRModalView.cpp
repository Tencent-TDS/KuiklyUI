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

#include "libohos_render/expand/components/modal/KRModalView.h"

#include "libohos_render/utils/KRRenderLoger.h"

static std::map<void *, std::weak_ptr<KRModalView>> gModalMap;

// 弱声明：仅用于诊断日志，低版本系统未导出时取 nullptr
extern "C" int32_t OH_ArkUI_NodeUtils_GetLayoutSize(ArkUI_NodeHandle node, ArkUI_IntSize *size)
    __attribute__((weak));

bool KRModalView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                          const KRRenderCallback event_call_back) {
    if (prop_key == "onWillDismiss") {
        on_will_dismiss_callback_ = event_call_back;
    }
    if (prop_key == kuikly::util::kPropNameModalCustomStyleOhos) {
        custom_style_ = prop_value->toBool();
    }
    if (prop_key == kuikly::util::kPropNameModalAlignCenterOhos) {
        align_center_ = prop_value->toBool();
    }
    // 上游诊断：确认尺寸类属性是否到达 Modal（frame 为 0 时用它判断是不是下发链路的问题）
    if (prop_key == "frame" || prop_key == "width" || prop_key == "height") {
        KR_LOG_INFO << "ImmLightTest: modal size prop=" << prop_key
                    << ", frameAfter=" << GetFrame().width << "x" << GetFrame().height;
    }
    return KRView::SetProp(prop_key, prop_value, event_call_back);
}

bool KRModalView::ReuseEnable() {
    return false;
}

void KRModalView::DidMoveToParentView() {
    KRView::DidMoveToParentView();
    this->ToRemoveFromSuperView();

    std::weak_ptr<KRModalView> weakSelf = std::dynamic_pointer_cast<KRModalView>(shared_from_this());
    void *dialogContext = reinterpret_cast<void *>(GetViewTag());
    gModalMap[dialogContext] = weakSelf;
    KRMainThread::RunOnMainThread(
        [node = GetNode(), weakSelf, dialogContext] {
            auto strongSelf = weakSelf.lock();
            if (strongSelf == nullptr) {
                return;
            }
            // 无条件诊断日志：确认延期任务执行时基类是否已记录材质配置
            KR_LOG_INFO << "ImmLightTest: modal dialog creating, hasMaterial="
                        << strongSelf->has_system_material_ << ", customStyle=" << strongSelf->custom_style_
                        << ", dialogApiAvailable=" << kuikly::util::IsDialogMaterialApiAvailable();
            ArkUI_NativeDialogHandle dialogNode = kuikly::util::GetDialogNodeApi()->create();
            strongSelf->dialogNode_ = dialogNode;
            kuikly::util::GetDialogNodeApi()->enableCustomStyle(dialogNode, strongSelf->custom_style_);
            kuikly::util::GetDialogNodeApi()->enableCustomAnimation(dialogNode, true);
            kuikly::util::GetDialogNodeApi()->setModalMode(dialogNode, false);
            kuikly::util::GetDialogNodeApi()->setAutoCancel(dialogNode, false);
            kuikly::util::GetDialogNodeApi()->setContentAlignment(
                dialogNode, strongSelf->align_center_ ? ARKUI_ALIGNMENT_CENTER : ARKUI_ALIGNMENT_TOP_START, 0, 0);
            // 内容节点已被 ToRemoveFromSuperView 移出页面树，Kuikly 的 flex 引擎不再为它
            // 计算 frame，setContent 时拿到的是 0x0，dialog 会塌陷成一条、内容全被裁掉。
            // 这里用最后一次已知的 frame 显式补一次尺寸。
            {
                KRRect &frame = strongSelf->GetFrame();
                KR_LOG_INFO << "ImmLightTest: modal content frame=" << frame.width << "x" << frame.height;
                if (frame.width > 0 && frame.height > 0) {
                    kuikly::util::UpdateNodeSize(strongSelf->GetNode(), frame.width, frame.height);
                }
            }
            kuikly::util::GetDialogNodeApi()->setContent(dialogNode, strongSelf->GetNode());
            {
                ArkUI_IntSize node_size{0, 0};
                if (OH_ArkUI_NodeUtils_GetLayoutSize != nullptr) {
                    OH_ArkUI_NodeUtils_GetLayoutSize(strongSelf->GetNode(), &node_size);
                }
                KR_LOG_INFO << "ImmLightTest: modal content node size=" << node_size.width << "x"
                            << node_size.height;
            }
            // 弹窗类接口在官方白名单内、可全页面生效，走 CustomDialog 专用通道
            strongSelf->OnSystemMaterialChanged();
            if (kuikly::util::GetDialogNodeApi()->registerOnWillDismissWithUserData) {
                kuikly::util::GetDialogNodeApi()->registerOnWillDismissWithUserData(
                    strongSelf->dialogNode_, dialogContext, [](ArkUI_DialogDismissEvent *event) {
                        if (event) {
                            void *ctx = OH_ArkUI_DialogDismissEvent_GetUserData(event);
                            int32_t reason = OH_ArkUI_DialogDismissEvent_GetDismissReason(event);
                            OH_ArkUI_DialogDismissEvent_SetShouldBlockDismiss(event, true);
                            auto it = gModalMap.find(ctx);
                            if (it != gModalMap.end()) {
                                if (auto strongModalView = it->second.lock()) {
                                    strongModalView->FireDismissEvent(reason);
                                }
                            }
                        }
                    });
            }
            int32_t show_ret = kuikly::util::GetDialogNodeApi()->show(dialogNode, false);
            KR_LOG_INFO << "ImmLightTest: dialog show ret=" << show_ret;
            // 部分实现会在 show 时重建背板，这里再应用一次以规避属性被重置
            strongSelf->OnSystemMaterialChanged();
        },
        16);
}

void KRModalView::OnDestroy() {
    KRView::OnDestroy();
    if (dialog_material_ != nullptr) {
        kuikly::util::DestroySystemMaterial(dialog_material_);
        dialog_material_ = nullptr;
    }
    if (dialogNode_ != nullptr) {
        kuikly::util::GetDialogNodeApi()->registerOnWillDismiss(dialogNode_, nullptr);
        kuikly::util::GetDialogNodeApi()->removeContent(dialogNode_);
        kuikly::util::GetDialogNodeApi()->close(dialogNode_);
        kuikly::util::GetDialogNodeApi()->dispose(dialogNode_);
    }
    auto it = gModalMap.find(reinterpret_cast<void *>(GetViewTag()));
    if (it != gModalMap.end()) {
        gModalMap.erase(it);
    }

    dialogNode_ = nullptr;
}

void KRModalView::OnSystemMaterialChanged() {
    if (dialogNode_ == nullptr) {
        // 弹窗尚未创建：DidMoveToParentView 建好 dialog 后会再次调用本方法
        return;
    }
    if (dialog_material_ != nullptr) {
        kuikly::util::DestroySystemMaterial(dialog_material_);
        dialog_material_ = nullptr;
    }
    if (!has_system_material_) {
        KR_LOG_INFO << "ImmLightTest: modal material cleared";
        return;
    }
    KR_LOG_INFO << "ImmLightTest: modal apply, customStyle=" << custom_style_
                << ", dialogApiAvailable=" << kuikly::util::IsDialogMaterialApiAvailable();
    dialog_material_ = kuikly::util::CreateSystemMaterial(system_material_options_);
    if (dialog_material_ != nullptr) {
        kuikly::util::ApplySystemMaterialToDialog(dialogNode_, dialog_material_);
    }
}

void KRModalView::FireDismissEvent(int32_t reason) {
    if (on_will_dismiss_callback_ != nullptr) {
        KRRenderValueMap map;
        map["reason"] = NewKRRenderValue(reason);
        on_will_dismiss_callback_(NewKRRenderValue(map));
    }
}
