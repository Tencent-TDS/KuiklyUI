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

static std::map<void *, std::weak_ptr<KRModalView>> gModalMap;

bool KRModalView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                          const KRRenderCallback event_call_back) {
    if (prop_key == "onWillDismiss") {
        on_will_dismiss_callback_ = event_call_back;
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
            ArkUI_NativeDialogHandle dialogNode = kuikly::util::GetDialogNodeApi()->create();
            strongSelf->dialogNode_ = dialogNode;
            kuikly::util::GetDialogNodeApi()->enableCustomStyle(dialogNode, true);
            kuikly::util::GetDialogNodeApi()->enableCustomAnimation(dialogNode, true);
            kuikly::util::GetDialogNodeApi()->setModalMode(dialogNode, false);
            kuikly::util::GetDialogNodeApi()->setAutoCancel(dialogNode, false);
            kuikly::util::GetDialogNodeApi()->setContentAlignment(dialogNode, ARKUI_ALIGNMENT_TOP_START, 0, 0);
            kuikly::util::GetDialogNodeApi()->setContent(dialogNode, strongSelf->GetNode());
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
            kuikly::util::GetDialogNodeApi()->show(dialogNode, false);
        },
        16);
}

void KRModalView::OnDestroy() {
    KRView::OnDestroy();
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

void KRModalView::FireDismissEvent(int32_t reason) {
    if (on_will_dismiss_callback_ != nullptr) {
        KRRenderValueMap map;
        map["reason"] = NewKRRenderValue(reason);
        on_will_dismiss_callback_(NewKRRenderValue(map));
    }
}
