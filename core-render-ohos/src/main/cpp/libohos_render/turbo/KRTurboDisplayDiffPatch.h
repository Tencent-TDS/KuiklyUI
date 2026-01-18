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

#ifndef KUIKLY_TURBO_DISPLAY_DIFF_PATCH_H
#define KUIKLY_TURBO_DISPLAY_DIFF_PATCH_H

#include <memory>
#include <string>
#include <set>
#include "KRTurboDisplayNode.h"
#include "KRTurboDisplayShadow.h"
#include "../layer/IKRRenderLayer.h"

namespace KuiklyOhos {

class KRTurboDisplayDiffPatch {
public:
    KRTurboDisplayDiffPatch();
    ~KRTurboDisplayDiffPatch();

    void DiffPatchToRenderingWithRenderLayer(IKRRenderLayer* render_layer_handler,
                                             std::shared_ptr<KRTurboDisplayNode> old_node_tree,
                                             std::shared_ptr<KRTurboDisplayNode> new_node_tree);

    bool OnlyUpdateWithTargetNodeTree(std::shared_ptr<KRTurboDisplayNode> target_node_tree,
                                      std::shared_ptr<KRTurboDisplayNode> from_node_tree);
private:
    bool CanReuseNode(std::shared_ptr<KRTurboDisplayNode> old_node,
                     std::shared_ptr<KRTurboDisplayNode> new_node,
                     bool from_update_node);

    void CreateRenderViewWithNode(std::shared_ptr<KRTurboDisplayNode> node,
                                  IKRRenderLayer* render_layer_handler);

    void RemoveRenderViewWithNode(std::shared_ptr<KRTurboDisplayNode> node,
                                  IKRRenderLayer* render_layer_handler);

    void UpdateRenderViewWithCurNode(std::shared_ptr<KRTurboDisplayNode> cur_node,
                                    std::shared_ptr<KRTurboDisplayNode> new_node,
                                    IKRRenderLayer* render_layer_handler,
                                    bool has_parent);

    bool UpdateNodeWithTargetNode(std::shared_ptr<KRTurboDisplayNode> node,
                                  std::shared_ptr<KRTurboDisplayNode> from_node);

    bool UpdatePropWithTargetProp(std::shared_ptr<KRTurboDisplayProp> prop,
                                  std::shared_ptr<KRTurboDisplayProp> from_prop);

    bool IsEqualPropValue(const std::any& old_value, const std::any& new_value);
    bool IsBaseAttrKey(const std::string& prop_key);

    std::vector<std::shared_ptr<KRTurboDisplayNode>> SortScrollIndexWithList(
        const std::vector<std::shared_ptr<KRTurboDisplayNode>>& list);
    
    std::pair<std::shared_ptr<KRTurboDisplayNode>, int> NextNodeForUpdateWithChildren(
        const std::vector<std::shared_ptr<KRTurboDisplayNode>>& from_children,
        int from_index,
        std::shared_ptr<KRTurboDisplayNode> target_node);

    KRAnyValue ConvertAnyToKRAnyValue(const std::any& value);
    
    void SetShadowForViewToRenderLayerWithShadow(
        std::shared_ptr<KRTurboDisplayShadow> shadow,
        std::shared_ptr<KRTurboDisplayNode> node,
        IKRRenderLayer* render_layer_handler);

    std::set<std::string> base_attr_key_set_;
};

} // namespace KuiklyOhos

#endif // KUIKLY_TURBO_DISPLAY_DIFF_PATCH_H
