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

#ifndef CORE_RENDER_OHOS_KRSNAPSHOTMANAGER_H
#define CORE_RENDER_OHOS_KRSNAPSHOTMANAGER_H
#include <arkui/drawable_descriptor.h>
#include <string>
#include <unordered_map>
#include "libohos_render/expand/components/view/KRView.h"
#include "libohos_render/foundation/KRCommon.h"

struct KRSnapshotItem {
    KRSnapshotItem() : drawableDescriptor(nullptr) {}
    ArkUI_DrawableDescriptor *drawableDescriptor;
    std::string uri;
};

class KRSnapshotManager {
 public:
    ~KRSnapshotManager();

    void SetCachedSnapshotToNode(ArkUI_NodeHandle node, const std::string &key);
    void TakeSnapshot(const std::string &instance_id, const std::string &method_name, const std::string &nodeId,
                      const KRAnyValue &params, const KRRenderCallback &cb,
                      std::weak_ptr<IKRRenderViewExport> weak_view);

 private:
    struct ResultData {
        int code;
        std::string data;
        std::string message;
    };

    struct ResultData ProcessSnapshotResultWithDataType(napi_env env, napi_value pixelMap, const std::string &path,
                                                        const std::string &pathUri,
                                                        ArkUI_DrawableDescriptor *drawableDescriptorPtr,
                                                        std::weak_ptr<IKRRenderViewExport> weak_view);
    struct ResultData ProcessSnapshotResultWithCacheKeyType(napi_env env, napi_value pixelMap, const std::string &path,
                                                            const std::string &pathUri,
                                                            ArkUI_DrawableDescriptor *drawableDescriptorPtr,
                                                            std::weak_ptr<IKRRenderViewExport> weak_view);
    struct ResultData ProcessSnapshotResultWithFileType(napi_env env, napi_value pixelMap, const std::string &path,
                                                        const std::string &pathUri,
                                                        ArkUI_DrawableDescriptor *drawableDescriptorPtr,
                                                        std::weak_ptr<IKRRenderViewExport> weak_view);

    void CacheSnapshot(ArkUI_DrawableDescriptor *descriptor, const std::string &key);
    void UpdateSnapshot(const std::string &uri, const std::string &key);
    void RemoveCachedDrawableAfterDelay(int delayMS, const std::string &key, const std::string &path,
                                        const std::string &pathUri, std::weak_ptr<IKRRenderViewExport> weak_view);

    std::unordered_map<std::string, struct KRSnapshotItem> drawableDescriptorCache_;
};

#endif  // CORE_RENDER_OHOS_KRSNAPSHOTMANAGER_H
