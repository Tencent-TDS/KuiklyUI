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

#include "KRTurboDisplayDiffPatch.h"
#include <algorithm>
#include <any>
#include <cstring>
#include <sstream>
#include "libohos_render/utils/KRRenderLoger.h"
#include "KRTurboDisplayShadow.h"

namespace KuiklyOhos {

const int ROOT_VIEW_TAG = -1;
const std::string RECYCLER_CONTENT_VIEW_NAME = "KRScrollerContentView";

KRTurboDisplayDiffPatch::KRTurboDisplayDiffPatch() {
    base_attr_key_set_ = {
        "backgroundColor", "opacity", "borderRadius", "borderWidth",
        "borderColor", "shadowColor", "shadowOffset", "shadowOpacity",
        "shadowRadius", "transform", "overflow", "visibility", 
        "backgroundImage", "frame", "zIndex",
        "boxShadow", "border", "animation", "accessibility",
        "accessibilityRole", "touchEnable", "useShadowPath",
        "wrapperBoxShadowView", "autoDarkEnable", "shouldRasterize",
        "lazyAnimationKey", "scrollIndex", "turboDisplayAutoUpdateEnable",
        "textDecoration", "color", "fontSize", "fontWeight", "text",
        "placeholder", "placeholderColor", "src", "dotNineImage",
        "click", "doubleClick", "longPress", "pan", "pinch",
        "animationCompletion", "textDidChange", "onChange", "onFocus",
        "onBlur", "onScroll", "onDragBegin", "onDragEnd", "onScrollEnd",
        "onWillDismiss", "onTouchStart", "onTouchMove", "onTouchEnd",
        "onTouchCancel", "capture"
    };
}

KRTurboDisplayDiffPatch::~KRTurboDisplayDiffPatch() = default;

void KRTurboDisplayDiffPatch::DiffPatchToRenderingWithRenderLayer(
    IKRRenderLayer* render_layer_handler,
    std::shared_ptr<KRTurboDisplayNode> old_node_tree,
    std::shared_ptr<KRTurboDisplayNode> new_node_tree) {
    
    KR_LOG_INFO << "[TurboDisplay-DiffPatch] 🔄 DiffPatchToRenderingWithRenderLayer 开始"
                << "\n  - old_node_tree: " << (old_node_tree ? old_node_tree->GetViewName() : "null")
                << "\n  - new_node_tree: " << (new_node_tree ? new_node_tree->GetViewName() : "null");
    
    if (CanReuseNode(old_node_tree, new_node_tree, false)) {
        KR_LOG_INFO << "[TurboDisplay-DiffPatch] ✅ CanReuseNode=true，执行增量更新";
        UpdateRenderViewWithCurNode(old_node_tree, new_node_tree, render_layer_handler, true);
        auto old_children = old_node_tree ? old_node_tree->GetChildren() : std::vector<std::shared_ptr<KRTurboDisplayNode>>();
        auto new_children = new_node_tree ? new_node_tree->GetChildren() : std::vector<std::shared_ptr<KRTurboDisplayNode>>();
        if (old_node_tree && old_node_tree->GetViewName() == RECYCLER_CONTENT_VIEW_NAME) {
            old_children = SortScrollIndexWithList(old_children);
            new_children = SortScrollIndexWithList(new_children);
        }
        size_t max_size = std::max(old_children.size(), new_children.size());
        KR_LOG_INFO << "[TurboDisplay-DiffPatch] 📊 子节点对比: old=" << old_children.size() 
                    << ", new=" << new_children.size() << ", max=" << max_size;
        for (size_t i = 0; i < max_size; i++) {
            auto old_node = i < old_children.size() ? old_children[i] : nullptr;
            auto new_node = i < new_children.size() ? new_children[i] : nullptr;
            DiffPatchToRenderingWithRenderLayer(render_layer_handler, old_node, new_node);
        }
    } else {
        KR_LOG_INFO << "[TurboDisplay-DiffPatch] ⚠️ CanReuseNode=false，执行全量重建";
        RemoveRenderViewWithNode(old_node_tree, render_layer_handler);
        CreateRenderViewWithNode(new_node_tree, render_layer_handler);
    }
}

bool KRTurboDisplayDiffPatch::CanReuseNode(std::shared_ptr<KRTurboDisplayNode> old_node,
                                          std::shared_ptr<KRTurboDisplayNode> new_node,
                                          bool from_update_node) {
    if (!old_node || !new_node) {
        return false;
    }
    if (old_node->GetViewName() != new_node->GetViewName()) {
        return false;
    }
    const auto& old_props = old_node->GetProps();
    const auto& new_props = new_node->GetProps();
    if (old_props.size() != new_props.size()) {
        return false;
    }

    for (size_t i = 0; i < new_props.size(); i++) {
        auto old_prop = old_props[i];
        auto new_prop = new_props[i];
        if (old_prop->GetPropKey() != new_prop->GetPropKey()) {
            return false;
        }
        if (old_prop->GetPropType() != new_prop->GetPropType()) {
            return false;
        }
        if (old_prop->GetPropType() == PROP_TYPE_ATTR || old_prop->GetPropType() == PROP_TYPE_EVENT) {
            if (old_prop->GetPropType() == PROP_TYPE_EVENT) {
                continue;
            }
            if (from_update_node && old_prop->GetPropKey() == "turboDisplayAutoUpdateEnable") {
                const auto& prop_value = new_prop->GetPropValue();
                if (prop_value.has_value()) {
                    try {
                        int int_value = 0;
                        if (prop_value.type() == typeid(int)) {
                            int_value = std::any_cast<int>(prop_value);
                        } else if (prop_value.type() == typeid(bool)) {
                            int_value = std::any_cast<bool>(prop_value) ? 1 : 0;
                        } else if (prop_value.type() == typeid(double)) {
                            int_value = static_cast<int>(std::any_cast<double>(prop_value));
                        } else if (prop_value.type() == typeid(float)) {
                            int_value = static_cast<int>(std::any_cast<float>(prop_value));
                        }
                        if (int_value == 0) {
                            return false;
                        }
                    } catch (const std::bad_any_cast&) {}
                }
            }
            bool is_base_attr = IsBaseAttrKey(old_prop->GetPropKey());
            if (!from_update_node && !is_base_attr) {
                if (!IsEqualPropValue(old_prop->GetPropValue(), new_prop->GetPropValue())) {
                    return false;
                }
            }
        }
    }

    const auto& old_methods = old_node->GetCallMethods();
    const auto& new_methods = new_node->GetCallMethods();
    std::vector<std::shared_ptr<KRTurboDisplayNodeMethod>> old_view_methods;
    std::vector<std::shared_ptr<KRTurboDisplayNodeMethod>> new_view_methods;
    for (const auto& method : old_methods) {
        if (method->GetType() == METHOD_TYPE_VIEW) {
            old_view_methods.push_back(method);
        }
    }
    for (const auto& method : new_methods) {
        if (method->GetType() == METHOD_TYPE_VIEW) {
            new_view_methods.push_back(method);
        }
    }
    if (old_view_methods.size() != new_view_methods.size()) {
        return false;
    }
    for (size_t i = 0; i < new_view_methods.size(); i++) {
        if (old_view_methods[i]->GetMethod() != new_view_methods[i]->GetMethod()) {
            return false;
        }
        if (old_view_methods[i]->GetParams() != new_view_methods[i]->GetParams()) {
            return false;
        }
    }
    return true;
}

void KRTurboDisplayDiffPatch::CreateRenderViewWithNode(
    std::shared_ptr<KRTurboDisplayNode> node,
    IKRRenderLayer* render_layer_handler) {
    
    if (!node) {
        return;
    }

    KR_LOG_DEBUG << "[TurboDisplay-DiffPatch] 📦 CreateRenderViewWithNode: viewName=" << node->GetViewName() 
                 << ", tag=" << node->GetTag();

    if (node->GetTag() == ROOT_VIEW_TAG) {
        KR_LOG_DEBUG << "[TurboDisplay-DiffPatch] 🔧 ROOT节点，执行ModuleMethod";
        for (const auto& method : node->GetCallMethods()) {
            if (method->GetType() == METHOD_TYPE_MODULE) {
                auto params = std::make_shared<KRRenderValue>(method->GetParams());
                render_layer_handler->CallModuleMethod(
                    true,
                    method->GetName(), 
                    method->GetMethod(), 
                    params,
                    method->GetCallback(),
                    true
                );
            }
        }
    } else {
        render_layer_handler->CreateRenderView(node->GetTag(), node->GetViewName());
    }

    UpdateRenderViewWithCurNode(nullptr, node, render_layer_handler, false);

    if (node->HadChild()) {
        int current_tag = node->GetTag();
        KR_LOG_DEBUG << "[TurboDisplay-DiffPatch] 📦 递归创建子节点, 父tag=" << current_tag 
                     << ", 子节点数=" << node->GetChildren().size();
        for (const auto& sub_node : node->GetChildren()) {
            CreateRenderViewWithNode(sub_node, render_layer_handler);
        }
    }
}

void KRTurboDisplayDiffPatch::RemoveRenderViewWithNode(
    std::shared_ptr<KRTurboDisplayNode> node,
    IKRRenderLayer* render_layer_handler) {
    
    if (!node || !render_layer_handler) {
        return;
    }
    
    render_layer_handler->RemoveRenderView(node->GetTag());

    if (node->HadChild()) {
        for (const auto& sub_node : node->GetChildren()) {
            RemoveRenderViewWithNode(sub_node, render_layer_handler);
        }
    }
}

void KRTurboDisplayDiffPatch::UpdateRenderViewWithCurNode(
    std::shared_ptr<KRTurboDisplayNode> cur_node,
    std::shared_ptr<KRTurboDisplayNode> new_node,
    IKRRenderLayer* render_layer_handler,
    bool has_parent) {
    
    if (!render_layer_handler) {
        return;
    }

    if (cur_node && new_node && cur_node->GetTag() != new_node->GetTag()) {
        render_layer_handler->updateViewTagWithCurTag(cur_node->GetTag(), new_node->GetTag());
        cur_node->SetTag(new_node->GetTag());
    }

    const auto& cur_props = cur_node ? cur_node->GetProps() : std::vector<std::shared_ptr<KRTurboDisplayProp>>();
    const auto& new_props = new_node ? new_node->GetProps() : std::vector<std::shared_ptr<KRTurboDisplayProp>>();
    
    size_t max_size = std::max(cur_props.size(), new_props.size());
    for (size_t i = 0; i < max_size; i++) {
        auto cur_prop = i < cur_props.size() ? cur_props[i] : nullptr;
        auto new_prop = i < new_props.size() ? new_props[i] : nullptr;
        
        if (!new_prop) {
            continue;
        }
        
        if (new_prop->GetPropType() == PROP_TYPE_ATTR) {
            if (!cur_prop || !IsEqualPropValue(cur_prop->GetPropValue(), new_prop->GetPropValue())) {
                auto prop_value = ConvertAnyToKRAnyValue(new_prop->GetPropValue());
                render_layer_handler->SetProp(new_node->GetTag(), new_prop->GetPropKey(), prop_value);
            }
        } else if (new_prop->GetPropType() == PROP_TYPE_EVENT) {
            if (cur_prop) {
                const auto& prop_value = new_prop->GetPropValue();
                if (prop_value.has_value() && prop_value.type() == typeid(std::shared_ptr<KRRenderCallback>)) {
                    auto callback_ptr = std::any_cast<std::shared_ptr<KRRenderCallback>>(prop_value);
                    if (callback_ptr) {
                        cur_prop->PerformLazyEventToCallback(*callback_ptr);
                    }
                } else if (prop_value.has_value() && prop_value.type() == typeid(KRRenderCallback)) {
                    auto callback = std::any_cast<KRRenderCallback>(prop_value);
                    if (callback) {
                        cur_prop->PerformLazyEventToCallback(callback);
                    }
                }
            } else {
                new_prop->LazyEventIfNeed();
            }
            
            const auto& prop_value = new_prop->GetPropValue();
            if (prop_value.has_value() && prop_value.type() == typeid(std::shared_ptr<KRRenderCallback>)) {
                auto callback_ptr = std::any_cast<std::shared_ptr<KRRenderCallback>>(prop_value);
                if (callback_ptr) {
                    render_layer_handler->SetEvent(new_node->GetTag(), new_prop->GetPropKey(), *callback_ptr);
                }
            } else if (prop_value.has_value() && prop_value.type() == typeid(KRRenderCallback)) {
                auto callback = std::any_cast<KRRenderCallback>(prop_value);
                if (callback) {
                    render_layer_handler->SetEvent(new_node->GetTag(), new_prop->GetPropKey(), callback);
                }
            } 
            
        } else if (new_prop->GetPropType() == PROP_TYPE_FRAME) {
            if (cur_prop && IsEqualPropValue(cur_prop->GetPropValue(), new_prop->GetPropValue())) {
            } else {
                auto prop_value = ConvertAnyToKRAnyValue(new_prop->GetPropValue());
                render_layer_handler->SetProp(new_node->GetTag(), new_prop->GetPropKey(), prop_value);
            }
        } else if (new_prop->GetPropType() == PROP_TYPE_SHADOW) {
            if (new_node->GetRenderShadow()) {
                render_layer_handler->SetShadow(new_node->GetTag(), new_node->GetRenderShadow());
            } else {
                const auto& prop_value = new_prop->GetPropValue();
                if (prop_value.has_value() && prop_value.type() == typeid(std::shared_ptr<KRTurboDisplayShadow>)) {
                    auto shadow = std::any_cast<std::shared_ptr<KRTurboDisplayShadow>>(prop_value);
                    if (shadow) {
                        SetShadowForViewToRenderLayerWithShadow(shadow, new_node, render_layer_handler);
                    }
                }
            }
        } else if (new_prop->GetPropType() == PROP_TYPE_INSERT) {
            if (!has_parent) {
                const auto& prop_value = new_prop->GetPropValue();
                if (prop_value.has_value()) {
                    try {
                        int index = 0;
                        if (prop_value.type() == typeid(int)) {
                            index = std::any_cast<int>(prop_value);
                        } else if (prop_value.type() == typeid(double)) {
                            index = static_cast<int>(std::any_cast<double>(prop_value));
                        } else if (prop_value.type() == typeid(float)) {
                            index = static_cast<int>(std::any_cast<float>(prop_value));
                        }
                        render_layer_handler->InsertSubRenderView(new_node->GetParentTag().value(), new_node->GetTag(), index);
                    } catch (const std::bad_any_cast& e) {
                        KR_LOG_ERROR << "[TurboDisplay-DiffPatch] Failed to cast insert index: " << e.what();
                    }
                }
            }
        }
    }

    std::vector<std::shared_ptr<KRTurboDisplayNodeMethod>> new_node_view_methods;
    std::vector<std::shared_ptr<KRTurboDisplayNodeMethod>> cur_node_view_methods;
    
    if (new_node) {
        for (const auto& method : new_node->GetCallMethods()) {
            if (method->GetType() == METHOD_TYPE_VIEW) {
                new_node_view_methods.push_back(method);
            }
        }
    }
    
    if (cur_node) {
        for (const auto& method : cur_node->GetCallMethods()) {
            if (method->GetType() == METHOD_TYPE_VIEW) {
                cur_node_view_methods.push_back(method);
            }
        }
    }
    
    size_t from_index = 0;
    for (from_index = 0; from_index < new_node_view_methods.size(); from_index++) {
        auto new_method = new_node_view_methods[from_index];
        if (from_index >= cur_node_view_methods.size()) {
            break;
        }
        auto cur_method = from_index < cur_node_view_methods.size() ? cur_node_view_methods[from_index] : nullptr;
        if (new_method->GetMethod() != cur_method->GetMethod() ||
            !IsEqualPropValue(std::any(new_method->GetParams()), std::any(cur_method->GetParams()))) {
            break;
        }
    }
    
    for (size_t i = from_index; i < new_node_view_methods.size(); i++) {
        auto method = new_node_view_methods[i];
        auto params = std::make_shared<KRRenderValue>(method->GetParams());
        render_layer_handler->CallViewMethod(
            new_node->GetTag(),
            method->GetMethod(),
            params,
            method->GetCallback()
        );
    }
}

std::pair<std::shared_ptr<KRTurboDisplayNode>, int> KRTurboDisplayDiffPatch::NextNodeForUpdateWithChildren(
    const std::vector<std::shared_ptr<KRTurboDisplayNode>>& from_children,
    int from_index,
    std::shared_ptr<KRTurboDisplayNode> target_node) {

    int target_tag = target_node->GetTag();
    for (int i = from_index; i < static_cast<int>(from_children.size()); i++) {
        auto next_target_node = from_children[i];
        if (next_target_node->GetTag() == target_tag) {
            return std::make_pair(next_target_node, i);
        }
    }
    if (from_index < static_cast<int>(from_children.size())) {
        return std::make_pair(from_children[from_index], from_index);
    }
    return std::make_pair(nullptr, from_index);
}

bool KRTurboDisplayDiffPatch::OnlyUpdateWithTargetNodeTree(
    std::shared_ptr<KRTurboDisplayNode> target_node_tree,
    std::shared_ptr<KRTurboDisplayNode> from_node_tree) {
    
    bool has_update = false;

    if (CanReuseNode(target_node_tree, from_node_tree, true)) {
        if (UpdateNodeWithTargetNode(target_node_tree, from_node_tree)) {
            has_update = true;
        }

        if (target_node_tree->HadChild() && from_node_tree->HadChild()) {
            auto a_children = target_node_tree->GetChildren();
            auto b_children = from_node_tree->GetChildren();

            if (target_node_tree->GetViewName() == RECYCLER_CONTENT_VIEW_NAME) {
                a_children = SortScrollIndexWithList(a_children);
                b_children = SortScrollIndexWithList(b_children);
                
                if (!a_children.empty() && b_children.size() >= a_children.size()) {
                    auto frame_prop = from_node_tree->GetProp(KRTurboDisplayNode::FRAME_KEY);
                    if (frame_prop && frame_prop->GetPropValue().has_value()) {
                        struct Frame { float x, y, width, height; };
                        auto frame = std::any_cast<Frame>(frame_prop->GetPropValue());
                        if (frame.height > frame.width) {
                            std::vector<std::shared_ptr<KRTurboDisplayNode>> target_children(
                                b_children.begin(), b_children.begin() + a_children.size());
                            for (auto &node : target_children) {
                                node->SetParentTag(target_node_tree->GetTag());
                            }
                            target_node_tree->SetChildren(target_children);
                            return true;
                        }
                    }
                }
            }
            
            int from_index = 0;
            for (size_t i = 0; i < a_children.size(); i++) {
                auto next_target_node = a_children[i];
                auto next_from_node = NextNodeForUpdateWithChildren(b_children, from_index, next_target_node);
                from_index = next_from_node.second;
                if (next_from_node.first) {
                    if (OnlyUpdateWithTargetNodeTree(next_target_node, next_from_node.first)) {
                        has_update = true;
                    }
                }
                from_index++;
            }
        }
    }

    return has_update;
}

bool KRTurboDisplayDiffPatch::UpdateNodeWithTargetNode(
    std::shared_ptr<KRTurboDisplayNode> node,
    std::shared_ptr<KRTurboDisplayNode> from_node) {
    
    bool has_update = false;

    if (node->GetViewName() != from_node->GetViewName()) {
        node->SetViewName(from_node->GetViewName());
        has_update = true;
    }

    const auto& node_props = node->GetProps();
    const auto& from_props = from_node->GetProps();
    
    if (node_props.size() == from_props.size()) {
        for (size_t i = 0; i < node_props.size(); i++) {
            if (UpdatePropWithTargetProp(node_props[i], from_props[i])) {
                has_update = true;
            }
        }
    }

    return has_update;
}

bool KRTurboDisplayDiffPatch::UpdatePropWithTargetProp(
    std::shared_ptr<KRTurboDisplayProp> prop,
    std::shared_ptr<KRTurboDisplayProp> from_prop) {
    
    bool has_update = false;
    
    KR_LOG_INFO << "[TurboDisplay-Diff] 比较属性: " << prop->GetPropKey()
                << ", type: " << static_cast<int>(prop->GetPropType())
                << ", 是否相等: " << (IsEqualPropValue(prop->GetPropValue(), from_prop->GetPropValue()) ? "true" : "false");

    if (prop->GetPropType() != from_prop->GetPropType()) {
        prop->SetPropType(from_prop->GetPropType());
        has_update = true;
    }
    
    if (prop->GetPropKey() != from_prop->GetPropKey()) {
        prop->SetPropKey(from_prop->GetPropKey());
        has_update = true;
    }

    if (!IsEqualPropValue(prop->GetPropValue(), from_prop->GetPropValue())) {
        prop->SetPropValue(from_prop->GetPropValue());
        has_update = true;
    }

    return has_update;
}

bool KRTurboDisplayDiffPatch::IsEqualPropValue(const std::any& old_value, const std::any& new_value) {
    if (!old_value.has_value() && !new_value.has_value()) {
        return true;
    }
    if (!old_value.has_value() || !new_value.has_value()) {
        return false;
    }

    bool old_is_callback = (old_value.type() == typeid(KRRenderCallback) || 
                            old_value.type() == typeid(std::shared_ptr<KRRenderCallback>));
    bool new_is_callback = (new_value.type() == typeid(KRRenderCallback) || 
                            new_value.type() == typeid(std::shared_ptr<KRRenderCallback>));
    if (old_is_callback || new_is_callback) {
        return true;
    }

    if (old_value.type() == typeid(KRAnyValue) && new_value.type() == typeid(KRAnyValue)) {
        try {
            auto old_any_value = std::any_cast<KRAnyValue>(old_value);
            auto new_any_value = std::any_cast<KRAnyValue>(new_value);
            if (!old_any_value && !new_any_value) return true;
            if (!old_any_value || !new_any_value) return false;
            if (old_any_value->isNull() && new_any_value->isNull()) return true;
            if (old_any_value->isBool() && new_any_value->isBool()) {
                return old_any_value->toBool() == new_any_value->toBool();
            }
            if ((old_any_value->isInt() || old_any_value->isLong() || old_any_value->isFloat() || old_any_value->isDouble()) &&
                (new_any_value->isInt() || new_any_value->isLong() || new_any_value->isFloat() || new_any_value->isDouble())) {
                return old_any_value->toDouble() == new_any_value->toDouble();
            }
            if (old_any_value->isString() && new_any_value->isString()) {
                return old_any_value->toString() == new_any_value->toString();
            }
            if ((old_any_value->isMap() || old_any_value->isArray()) && 
                (new_any_value->isMap() || new_any_value->isArray())) {
                return old_any_value->toString() == new_any_value->toString();
            }
            return false;
        } catch (const std::bad_any_cast&) {
            return false;
        }
    }

    if (old_value.type() != new_value.type()) {
        auto try_compare_numbers = [&]() -> std::pair<bool, bool> {
            double old_num = 0, new_num = 0;
            bool old_is_num = false, new_is_num = false;
            try {
                if (old_value.type() == typeid(int)) { old_num = std::any_cast<int>(old_value); old_is_num = true; }
                else if (old_value.type() == typeid(int32_t)) { old_num = std::any_cast<int32_t>(old_value); old_is_num = true; }
                else if (old_value.type() == typeid(int64_t)) { old_num = static_cast<double>(std::any_cast<int64_t>(old_value)); old_is_num = true; }
                else if (old_value.type() == typeid(float)) { old_num = std::any_cast<float>(old_value); old_is_num = true; }
                else if (old_value.type() == typeid(double)) { old_num = std::any_cast<double>(old_value); old_is_num = true; }
                if (new_value.type() == typeid(int)) { new_num = std::any_cast<int>(new_value); new_is_num = true; }
                else if (new_value.type() == typeid(int32_t)) { new_num = std::any_cast<int32_t>(new_value); new_is_num = true; }
                else if (new_value.type() == typeid(int64_t)) { new_num = static_cast<double>(std::any_cast<int64_t>(new_value)); new_is_num = true; }
                else if (new_value.type() == typeid(float)) { new_num = std::any_cast<float>(new_value); new_is_num = true; }
                else if (new_value.type() == typeid(double)) { new_num = std::any_cast<double>(new_value); new_is_num = true; }
            } catch (...) {}
            if (old_is_num && new_is_num) {
                return {true, old_num == new_num};
            }
            return {false, false};
        };
        auto [is_both_num, nums_equal] = try_compare_numbers();
        if (is_both_num) {
            return nums_equal;
        }
        return false;
    }

    try {
        if (old_value.type() == typeid(std::string)) {
            return std::any_cast<std::string>(old_value) == std::any_cast<std::string>(new_value);
        }
        if (old_value.type() == typeid(int)) {
            return std::any_cast<int>(old_value) == std::any_cast<int>(new_value);
        }
        if (old_value.type() == typeid(int32_t)) {
            return std::any_cast<int32_t>(old_value) == std::any_cast<int32_t>(new_value);
        }
        if (old_value.type() == typeid(int64_t)) {
            return std::any_cast<int64_t>(old_value) == std::any_cast<int64_t>(new_value);
        }
        if (old_value.type() == typeid(long)) {
            return std::any_cast<long>(old_value) == std::any_cast<long>(new_value);
        }
        if (old_value.type() == typeid(float)) {
            return std::any_cast<float>(old_value) == std::any_cast<float>(new_value);
        }
        if (old_value.type() == typeid(double)) {
            return std::any_cast<double>(old_value) == std::any_cast<double>(new_value);
        }
        if (old_value.type() == typeid(bool)) {
            return std::any_cast<bool>(old_value) == std::any_cast<bool>(new_value);
        }
        if (old_value.type() == typeid(char)) {
            return std::any_cast<char>(old_value) == std::any_cast<char>(new_value);
        }
        if (old_value.type() == typeid(const char*)) {
            const char* old_str = std::any_cast<const char*>(old_value);
            const char* new_str = std::any_cast<const char*>(new_value);
            if (old_str == nullptr || new_str == nullptr) {
                return old_str == new_str;
            }
            return std::strcmp(old_str, new_str) == 0;
        }
        struct Frame { float x, y, width, height; };
        if (old_value.type() == typeid(Frame)) {
            auto old_frame = std::any_cast<Frame>(old_value);
            auto new_frame = std::any_cast<Frame>(new_value);
            return old_frame.x == new_frame.x && 
                   old_frame.y == new_frame.y && 
                   old_frame.width == new_frame.width && 
                   old_frame.height == new_frame.height;
        }
    } catch (const std::bad_any_cast&) {
        return false;
    }
    return false;
}

bool KRTurboDisplayDiffPatch::IsBaseAttrKey(const std::string& prop_key) {
    return base_attr_key_set_.find(prop_key) != base_attr_key_set_.end();
}

std::vector<std::shared_ptr<KRTurboDisplayNode>> KRTurboDisplayDiffPatch::SortScrollIndexWithList(
    const std::vector<std::shared_ptr<KRTurboDisplayNode>>& list) {
    
    auto sorted_list = list;
    std::sort(sorted_list.begin(), sorted_list.end(), 
        [](const std::shared_ptr<KRTurboDisplayNode>& a, const std::shared_ptr<KRTurboDisplayNode>& b) {
            int scroll_index_a = 0;
            int scroll_index_b = 0;
            auto prop_a = a->GetProp(KRTurboDisplayNode::SCROLL_INDEX);
            if (prop_a && prop_a->GetPropValue().has_value()) {
                try {
                    scroll_index_a = std::any_cast<int>(prop_a->GetPropValue());
                } catch (...) {}
            }
            auto prop_b = b->GetProp(KRTurboDisplayNode::SCROLL_INDEX);
            if (prop_b && prop_b->GetPropValue().has_value()) {
                try {
                    scroll_index_b = std::any_cast<int>(prop_b->GetPropValue());
                } catch (...) {}
            }
            if (scroll_index_a < scroll_index_b) {
                return true;
            } else if (scroll_index_a > scroll_index_b) {
                return false;
            }
            float index_a = 0.0f;
            float index_b = 0.0f;
            auto frame_prop_a = a->GetProp(KRTurboDisplayNode::FRAME_KEY);
            if (frame_prop_a && frame_prop_a->GetPropValue().has_value()) {
                try {
                    struct Frame { float x, y, width, height; };
                    auto frame = std::any_cast<Frame>(frame_prop_a->GetPropValue());
                    index_a = frame.x + frame.y;
                } catch (...) {}
            }
            auto frame_prop_b = b->GetProp(KRTurboDisplayNode::FRAME_KEY);
            if (frame_prop_b && frame_prop_b->GetPropValue().has_value()) {
                try {
                    struct Frame { float x, y, width, height; };
                    auto frame = std::any_cast<Frame>(frame_prop_b->GetPropValue());
                    index_b = frame.x + frame.y;
                } catch (...) {}
            }
            return index_a < index_b;
        });
    return sorted_list;
}

KRAnyValue KRTurboDisplayDiffPatch::ConvertAnyToKRAnyValue(const std::any& value) {
    if (!value.has_value()) {
        return std::make_shared<KRRenderValue>(nullptr);
    }
    try {
        if (value.type() == typeid(KRRenderCallback) || 
            value.type() == typeid(std::shared_ptr<KRRenderCallback>)) {
            return std::make_shared<KRRenderValue>(nullptr);
        }
        if (value.type() == typeid(bool)) {
            return std::make_shared<KRRenderValue>(std::any_cast<bool>(value));
        }
        if (value.type() == typeid(int)) {
            return std::make_shared<KRRenderValue>(std::any_cast<int>(value));
        }
        if (value.type() == typeid(int32_t)) {
            return std::make_shared<KRRenderValue>(std::any_cast<int32_t>(value));
        }
        if (value.type() == typeid(int64_t)) {
            return std::make_shared<KRRenderValue>(std::any_cast<int64_t>(value));
        }
        if (value.type() == typeid(float)) {
            return std::make_shared<KRRenderValue>(std::any_cast<float>(value));
        }
        if (value.type() == typeid(double)) {
            return std::make_shared<KRRenderValue>(std::any_cast<double>(value));
        }
        if (value.type() == typeid(std::string)) {
            return std::make_shared<KRRenderValue>(std::any_cast<std::string>(value));
        }
        if (value.type() == typeid(const char*)) {
            return std::make_shared<KRRenderValue>(std::any_cast<const char*>(value));
        }
        struct Frame { float x, y, width, height; };
        if (value.type() == typeid(Frame)) {
            auto frame = std::any_cast<Frame>(value);
            std::ostringstream oss;
            oss << "{{" << frame.x << ", " << frame.y << "}, {" << frame.width << ", " << frame.height << "}}";
            return std::make_shared<KRRenderValue>(oss.str());
        }
        if (value.type() == typeid(KRRenderValueMap)) {
            return std::make_shared<KRRenderValue>(std::any_cast<KRRenderValueMap>(value));
        }
        if (value.type() == typeid(KRRenderValueArray)) {
            return std::make_shared<KRRenderValue>(std::any_cast<KRRenderValueArray>(value));
        }
        if (value.type() == typeid(KRAnyValue)) {
            return std::any_cast<KRAnyValue>(value);
        }
    } catch (const std::bad_any_cast& e) {
        KR_LOG_ERROR << "[TurboDisplay-DiffPatch] ConvertAnyToKRAnyValue failed: " << e.what();
    }
    return std::make_shared<KRRenderValue>(nullptr);
}

void KRTurboDisplayDiffPatch::SetShadowForViewToRenderLayerWithShadow(
    std::shared_ptr<KRTurboDisplayShadow> shadow,
    std::shared_ptr<KRTurboDisplayNode> node,
    IKRRenderLayer* render_layer_handler) {
    
    if (!shadow || !node || !render_layer_handler) {
        return;
    }
    auto real_shadow = IKRRenderShadowExport::CreateShadow(shadow->GetViewName());
    if (!real_shadow) {
        KR_LOG_ERROR << "[TurboDisplay-DiffPatch] Failed to create shadow: " << shadow->GetViewName();
        return;
    }
    for (const auto& prop : shadow->GetProps()) {
        if (prop && prop->GetPropValue().has_value()) {
            auto prop_value = ConvertAnyToKRAnyValue(prop->GetPropValue());
            real_shadow->SetProp(prop->GetPropKey(), prop_value);
        }
    }
    real_shadow->CalculateRenderViewSize(shadow->GetConstraintWidth(), shadow->GetConstraintHeight());
    auto task = real_shadow->TaskToMainQueueWhenWillSetShadowToView();
    if (task) {
        task();
    }
    render_layer_handler->SetShadow(node->GetTag(), real_shadow);
}

} // namespace KuiklyOhos
