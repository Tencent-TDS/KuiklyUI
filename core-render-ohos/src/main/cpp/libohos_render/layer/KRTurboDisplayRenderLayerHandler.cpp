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

#include "libohos_render/layer/KRTurboDisplayRenderLayerHandler.h"
#include "libohos_render/scheduler/KRContextScheduler.h"
#include "libohos_render/scheduler/KRUIScheduler.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/turbo/KRTurboDisplayNode.h"
#include "libohos_render/turbo/KRTurboDisplayShadow.h"
#include "libohos_render/turbo/KRTurboDisplayCacheData.h"
#include "libohos_render/turbo/KRTurboDisplayCacheManager.h"
#include "libohos_render/turbo/KRTurboDisplayDiffPatch.h"
#include <chrono>
#include <iomanip>

// 辅助函数：将 KRAnyValue 转换为 std::any（声明在命名空间外）
std::any ConvertKRAnyValueToAny(const KRAnyValue& kr_value);

using namespace KuiklyOhos;

// MARK: - KRTurboDisplayRenderLayerHandler 实现

KRTurboDisplayRenderLayerHandler::KRTurboDisplayRenderLayerHandler() {
    KR_LOG_INFO << "[TurboDisplay-Handler] 🏗️ KRTurboDisplayRenderLayerHandler 构造函数执行";
    render_layer_handler_ = std::make_shared<KRRenderLayerHandler>();
    
    real_root_node_ = std::make_shared<KRTurboDisplayNode>(ROOT_VIEW_TAG, ROOT_VIEW_NAME);
    real_node_map_[ROOT_VIEW_TAG] = real_root_node_;  // 将 root 节点也加入 map，保持引用一致
    
    diff_patch_ = std::make_shared<KRTurboDisplayDiffPatch>();
    KR_LOG_INFO << "[TurboDisplay-Handler] ✅ 构造完成，root_tag=" << ROOT_VIEW_TAG;
}

void KRTurboDisplayRenderLayerHandler::Init(std::weak_ptr<IKRRenderView> root_view,
                                            std::shared_ptr<KRRenderContextParams> &context) {
    KR_LOG_INFO << "[TurboDisplay-Handler] 🚀 Init 开始";
    context_ = context;
    root_view_ = root_view;
    
    // 初始化内部普通渲染器
    render_layer_handler_->Init(root_view, context);
    
    // 初始化缓存管理器
    std::string files_dir;
    if (context && context->Config()) {
        files_dir = context->Config()->GetFilesDir();
    }
    
    // 在 files_dir 下创建 TurboDisplay 子目录
    std::string turbo_cache_dir;
    if (!files_dir.empty()) {
        if (files_dir.back() != '/') {
            files_dir += '/';
        }
        turbo_cache_dir = files_dir + "TurboDisplay";
        cache_manager_ = std::make_shared<KRTurboDisplayCacheManager>(turbo_cache_dir);
        KR_LOG_INFO << "[TurboDisplay-Handler] 📁 缓存目录: " << turbo_cache_dir;
    } else {
        cache_manager_ = std::make_shared<KRTurboDisplayCacheManager>("");
        KR_LOG_INFO << "[TurboDisplay-Handler] ⚠️ files_dir 为空，缓存功能受限";
    }
    
    // 生成 TurboDisplay 缓存 Key
    std::string page_name = context ? context->PageName() : "";
    std::string turbo_display_key = context ? context->TurboDisplayKey() : "";
    
    KR_LOG_INFO << "[TurboDisplay-Handler] 📄 page_name=" << page_name << ", turbo_display_key=" << turbo_display_key;
    
    // 设置缓存 key
    if (!turbo_display_key.empty() && !page_name.empty()) {
        turbo_cache_key_ = KRTurboDisplayCacheManager::CacheKeyWithTurboDisplayKey(
            turbo_display_key, 
            page_name
        );
    } else {
        turbo_cache_key_ = KRTurboDisplayCacheManager::CacheKeyWithTurboDisplayKey(
            page_name, 
            page_name
        );
    }
    KR_LOG_INFO << "[TurboDisplay-Handler] 🔑 缓存 key: " << turbo_cache_key_;
    KR_LOG_INFO << "[TurboDisplay-Handler] ✅ Init 完成";
}

// DidInit() 在 Init() 之后调用，用于读取缓存和注册 UIScheduler 回调
void KRTurboDisplayRenderLayerHandler::DidInit() {
    KR_LOG_INFO << "[TurboDisplay-DidInit] 🚀 DidInit 开始执行";
    
    // 首屏耗时统计：记录初始化开始时间
    init_start_time_ = std::chrono::steady_clock::now();
    std::string page_name = context_ ? context_->PageName() : "";
    
    KR_LOG_INFO << "[TurboDisplay-DidInit] 📖 开始读取缓存, key=" << turbo_cache_key_;
    auto read_begin_time = std::chrono::steady_clock::now();
    // 读取首屏缓存
    turbo_cache_data_ = cache_manager_->ReadCache(turbo_cache_key_);
    
    auto read_end_time = std::chrono::steady_clock::now();
    auto read_turbo_file_cost_time = std::chrono::duration_cast<std::chrono::milliseconds>(
        read_end_time - read_begin_time
    ).count();
    
    // 判断是否启用懒渲染，核心就是判断是否有缓存
    if (turbo_cache_data_ && turbo_cache_data_->GetTurboDisplayNode()) {
        if (!turbo_cache_data_->GetTurboDisplayNode()->GetViewName().empty()) {
            lazy_rendering_ = true;
            
            KR_LOG_INFO << "[TurboDisplay-DidInit] ✅ 缓存命中！启用 TurboDisplay 懒渲染模式";
            KR_LOG_INFO << "[TurboDisplay-DidInit] 📊 缓存数据大小: " << turbo_cache_data_->GetTurboDisplayNodeData().size() << " bytes";
            KR_LOG_INFO << "[TurboDisplay-DidInit] ⏱️ 缓存读取耗时: " << read_turbo_file_cost_time << " ms";
            
            render_layer_handler_->CallModuleMethod(
                true,  // sync
                "KRTurboDisplayModule",
                "setFirstScreenTurboDisplay",
                std::make_shared<KRRenderValue>(true),  // params: true
                nullptr,  // callback
                false     // callback_keep_alive
            );
            KR_LOG_INFO << "[TurboDisplay-DidInit] 📤 已通知 Kotlin 侧启用 TurboDisplay";
        } 
    } else {
        KR_LOG_INFO << "[TurboDisplay-DidInit] ⚠️ 缓存未命中，使用普通渲染模式（首次加载或缓存失效）";
    }

    // 使用 UIScheduler 的 PerformWhenViewDidLoad
    if (ui_scheduler_) {
        KR_LOG_INFO << "[TurboDisplay-DidInit] 📝 注册 PerformWhenViewDidLoad 回调（等待首帧后执行 DiffPatch）";
        // 首帧之后去diff两棵树patch差量渲染指令更新到渲染器,延迟执行的
        ui_scheduler_->PerformWhenViewDidLoad([this]() {
            KR_LOG_INFO << "[TurboDisplay-ViewDidLoad] 🎯 首帧完成，开始执行 DiffPatchToRenderLayer";
            DiffPatchToRenderLayer();
        });
    } else {
        KR_LOG_INFO << "[TurboDisplay-DidInit] ⚠️ ui_scheduler_ 为空，无法注册 ViewDidLoad 回调";
    }
    
    // LAZY模式：先渲染缓存，然后标记 viewDidLoad
    if (lazy_rendering_) {
        KR_LOG_INFO << "[TurboDisplay-DidInit] 🎨 LAZY模式：开始渲染缓存首屏";
        // 标记首帧
        if (ui_scheduler_) {
            ui_scheduler_->MarkViewDidLoad();
            KR_LOG_INFO << "[TurboDisplay-DidInit] ✅ 已调用 MarkViewDidLoad()";
        }
        // 渲染TurboDisplay首屏
        auto render_begin_time = std::chrono::steady_clock::now();
        
        RenderTurboDisplayCache();
        
        if (turbo_cache_data_ && turbo_cache_data_->GetTurboDisplayNode()) {
            auto& children = turbo_cache_data_->GetTurboDisplayNode()->GetChildren();
            KR_LOG_INFO << "[TurboDisplay-DidInit] 📊 缓存树子节点数量: " << children.size();
            if (!children.empty()) {
                auto first_child_tag = children[0]->GetTag();
                auto view = render_layer_handler_->GetRenderView(first_child_tag);
                // 根View 执行布局
                if (view) {
                    kuikly::util::GetNodeApi()->markDirty(view->GetNode(), NODE_NEED_LAYOUT);
                    KR_LOG_INFO << "[TurboDisplay-DidInit] 🔄 已标记首个子节点需要布局, tag=" << first_child_tag;
                }
            }
        }
        
        auto render_end_time = std::chrono::steady_clock::now();
        auto render_cost_time = std::chrono::duration_cast<std::chrono::milliseconds>(
            render_end_time - render_begin_time
        ).count();
        KR_LOG_INFO << "[TurboDisplay-DidInit] ⏱️ 缓存首屏渲染耗时: " << render_cost_time << " ms";
    } else {
        KR_LOG_INFO << "[TurboDisplay-DidInit] 📝 非LAZY模式，等待正常渲染流程";
    }
    
    KR_LOG_INFO << "[TurboDisplay-DidInit] ✅ DidInit 执行完成";
}

void KRTurboDisplayRenderLayerHandler::CreateRenderView(int tag, const std::string &view_name) {
    KR_LOG_DEBUG << "[TurboDisplay-Create] 📦 CreateRenderView tag=" << tag << ", viewName=" << view_name 
                 << ", lazy=" << (lazy_rendering_ ? "true" : "false");
    
    // 1. 基于渲染指令更新真实树
    std::shared_ptr<KRTurboDisplayNode> node;
    node = std::make_shared<KRTurboDisplayNode>(tag, view_name);
    real_node_map_[tag] = node;
   
    SetNeedUpdateNextTurboRoot();    // 标记需要执行diff-DOM
    
    if (node) {
        AddTaskOnNextLoopMainQueue([node]() {
            node->SetAddViewMethodDisable(true);  // 存储首帧的Methods
        });
    }
    
    // 2. 根据懒渲染状态决定是否响应渲染指令并执行
    if (!lazy_rendering_) {
        render_layer_handler_->CreateRenderView(tag, view_name);
    } else {
        KR_LOG_DEBUG << "[TurboDisplay-Create] ⏸️ LAZY模式，暂不执行实际创建";
    }
}

void KRTurboDisplayRenderLayerHandler::RemoveRenderView(int tag) {
    // 1. 基于渲染指令更新真实树
    auto it = real_node_map_.find(tag);
    if (it != real_node_map_.end()) {
        auto node = it->second;
        // 确保 parent_tag 有值且父节点存在
        if (node->GetParentTag().has_value()) {
            auto parent_it = real_node_map_.find(node->GetParentTag().value());
            if (parent_it != real_node_map_.end() && parent_it->second) {
                node->RemoveFromParentNode(parent_it->second);
            }
        }
        real_node_map_.erase(it);
    } 
    
    // 2.根据懒渲染状态决定是否立即渲染
    if (!lazy_rendering_) {
        render_layer_handler_->RemoveRenderView(tag);
    }
}

void KRTurboDisplayRenderLayerHandler::InsertSubRenderView(int parent_tag, int child_tag, int index) {
    // 1.更新真实树
    auto parent_it = real_node_map_.find(parent_tag);
    auto child_it = real_node_map_.find(child_tag);
    if (parent_it != real_node_map_.end() && child_it != real_node_map_.end()) {
        auto parent_node = parent_it->second;
        auto child_node = child_it->second;
        
        parent_node->InsertSubNode(child_node, index);
        SetNeedUpdateNextTurboRoot();       // 标记需要执行diff-DOM
    }
    
    // 2.根据懒渲染状态决定是否立即渲染
    if (!lazy_rendering_) {
        render_layer_handler_->InsertSubRenderView(parent_tag, child_tag, index);
    }
}


void KRTurboDisplayRenderLayerHandler::SetProp(int tag, const std::string &prop_key, const KRAnyValue &prop_value) {
    // 获取 viewName 用于日志
    std::string view_name = "unknown";
    auto it = real_node_map_.find(tag);
    if (it != real_node_map_.end()) {
        view_name = it->second->GetViewName();
    }
    
    // 获取 prop_value 的字符串表示
    std::string value_str = "null";
    if (prop_value) {
        if (prop_value->isString()) {
            value_str = prop_value->toString();
            if (value_str.length() > 50) {
                value_str = value_str.substr(0, 50) + "...";
            }
        } else if (prop_value->isInt()) {
            value_str = std::to_string(prop_value->toInt());
        } else if (prop_value->isDouble()) {
            value_str = std::to_string(prop_value->toDouble());
        } else if (prop_value->isBool()) {
            value_str = prop_value->toBool() ? "true" : "false";
        } else if (prop_value->isMap()) {
            value_str = "[Map]";
        } else if (prop_value->isArray()) {
            value_str = "[Array]";
        }
    }
    
    // 1.更新真实树 - 将 KRAnyValue 转换为 std::any
    if (it != real_node_map_.end()) {
        auto node = it->second;
        // 当前的Prop只包括Attr，不包括Event
        std::any any_value = ConvertKRAnyValueToAny(prop_value);
        node->SetProp(prop_key, any_value);
        SetNeedUpdateNextTurboRoot();
    }
    
    // 2.根据懒渲染状态决定是否立即执行
    if (!lazy_rendering_) {
        render_layer_handler_->SetProp(tag, prop_key, prop_value);
    }
}

// 辅助函数：将 KRAnyValue 转换为 std::any
std::any ConvertKRAnyValueToAny(const KRAnyValue& kr_value) {
    if (!kr_value || kr_value->isNull()) {
        return std::any();
    }
    
    if (kr_value->isBool()) {
        return std::any(kr_value->toBool());
    }
    if (kr_value->isInt()) {
        return std::any(kr_value->toInt());
    }
    if (kr_value->isLong()) {
        return std::any(kr_value->toLong());
    }
    if (kr_value->isFloat()) {
        return std::any(kr_value->toFloat());
    }
    if (kr_value->isDouble()) {
        return std::any(kr_value->toDouble());
    }
    if (kr_value->isString()) {
        return std::any(kr_value->toString());
    }
    if (kr_value->isMap()) {
        return std::any(kr_value->toMap());
    }
    if (kr_value->isArray()) {
        return std::any(kr_value->toArray());
    }
    
    return std::any();
}


void KRTurboDisplayRenderLayerHandler::SetEvent(int tag, const std::string &prop_key, const KRRenderCallback &callback) {
    // 获取 viewName 用于日志
    std::string view_name = "unknown";
    auto it = real_node_map_.find(tag);
    if (it != real_node_map_.end()) {
        view_name = it->second->GetViewName();
    }
    
    // setNeedLayout 是虚拟事件，仅用于驱动 loop，不存入 props（对齐 iOS）
    if (prop_key == "setNeedLayout") {
        // 直接注册事件，但不存入 props
        render_layer_handler_->SetEvent(tag, prop_key, callback);
        return;
    }
    
    // 1.更新真实树，将 callback 存储到节点的 props 中
    if (it != real_node_map_.end()) {
        auto node = it->second;
        // 将 callback 包装为 shared_ptr 存储，对齐 LazyEventIfNeed 
        auto callback_ptr = std::make_shared<KRRenderCallback>(callback);
        node->SetProp(prop_key, std::any(callback_ptr), PROP_TYPE_EVENT);
    }
    SetNeedUpdateNextTurboRoot();
    
    // Event 必须立即注册（无论 lazy 与否）
    // 原因：
    // 1. Event callback 是 JS 函数，无法序列化到缓存
    // 2. DiffPatch 不会恢复 Event（缓存中没有）
    // 3. Kotlin 不会二次调用 SetEvent（没有响应式触发条件）
    // 4. 如果延迟注册，Event 会永久丢失
    render_layer_handler_->SetEvent(tag, prop_key, callback);
}

void KRTurboDisplayRenderLayerHandler::SetShadow(int tag, const std::shared_ptr<IKRRenderShadowExport> &shadow) {
    // 获取 viewName 用于日志
    std::string view_name = "unknown";
    auto it = real_node_map_.find(tag);
    if (it != real_node_map_.end()) {
        view_name = it->second->GetViewName();
    }
    
    // 1.标记缓存待刷新
    SetNeedUpdateNextTurboRoot();
    
    // 2.根据懒渲染状态决定是否立即渲染
    if (!lazy_rendering_) {
        render_layer_handler_->SetShadow(tag, shadow);
    }
}


std::string KRTurboDisplayRenderLayerHandler::CalculateRenderViewSize(int tag, double constraint_width,
                                                                      double constraint_height) {
    // 获取 viewName 用于日志
    std::string view_name = "unknown";
    auto it = real_node_map_.find(tag);
    if (it != real_node_map_.end()) {
        view_name = it->second->GetViewName();
    }
    
    auto shadow = real_shadow_map_[tag];
    shadow->CalculateWithConstraintSize(constraint_width, constraint_height);
    return render_layer_handler_->CalculateRenderViewSize(tag, constraint_width, constraint_height);
}

void KRTurboDisplayRenderLayerHandler::CallViewMethod(int tag, const std::string &method, const KRAnyValue &params,
                                                      const KRRenderCallback &callback) {
    // 获取 viewName 用于日志
    std::string view_name = "unknown";
    auto it = real_node_map_.find(tag);
    if (it != real_node_map_.end()) {
        view_name = it->second->GetViewName();
    }
    
    // 获取 params 的字符串表示
    std::string params_str = "null";
    if (params) {
        if (params->isString()) {
            params_str = params->toString();
            if (params_str.length() > 50) {
                params_str = params_str.substr(0, 50) + "...";
            }
        } else if (params->isMap()) {
            params_str = "[Map]";
        } else {
            params_str = "[Other]";
        }
    }
    
    
    if (it != real_node_map_.end()) {
        auto node = it->second;
        
        // 检查是否禁用 addViewMethod
        if (!node->GetAddViewMethodDisable()) {
            // 将 KRAnyValue 转换为 JSON 字符串
            std::string params_json;
            if (params) {
                if (params->isString()) {
                    params_json = params->toString();
                } else if (params->isMap()) {
                    // TODO: 如果需要支持 Map，可以实现序列化
                    params_json = "{}";
                } else {
                    params_json = "";
                }
            }
            
            // 记录 ViewMethod 到节点
            node->AddViewMethod(method, params_json, callback);
        }
    }
    
    // 根据懒渲染状态决定是否立即渲染
    if (!lazy_rendering_) {
        render_layer_handler_->CallViewMethod(tag, method, params, callback);
    }
}

KRAnyValue KRTurboDisplayRenderLayerHandler::CallModuleMethod(bool sync, const std::string &module_name,
                                                              const std::string &method, 
                                                              const KRAnyValue &params,
                                                              const KRRenderCallback &callback,
                                                              bool callback_keep_alive) {
    // 获取 params 的字符串表示
    std::string params_str = "null";
    if (params) {
        if (params->isString()) {
            params_str = params->toString();
            if (params_str.length() > 50) {
                params_str = params_str.substr(0, 50) + "...";
            }
        } else if (params->isMap()) {
            params_str = "[Map]";
        } else if (params->isBool()) {
            params_str = params->toBool() ? "true" : "false";
        } else {
            params_str = "[Other]";
        }
    }
    
    if (module_name == "KRMemoryCacheModule") {
        if (real_root_node_ && params) {
            std::string params_json;
            if (params->isString()) {
                params_json = params->toString();
            }
            real_root_node_->AddModuleMethod(module_name, method, params_json, callback);
        }
    }
    
    // Module 方法始终调用
    return render_layer_handler_->CallModuleMethod(sync, module_name, method, params, callback, callback_keep_alive);
}

KRAnyValue KRTurboDisplayRenderLayerHandler::CallTDFModuleMethod(const std::string &module_name,
                                                                 const std::string &method, const std::string &params,
                                                                 const std::string &call_id,
                                                                 const KRRenderCallback &success_callback,
                                                                 KRRenderCallback &error_callback) {
    return render_layer_handler_->CallTDFModuleMethod(module_name, method, params, call_id, success_callback,
                                                      error_callback);
}

void KRTurboDisplayRenderLayerHandler::CreateShadow(int tag, const std::string &view_name) {
    // 记录到真实 Shadow 树
    auto shadow = std::make_shared<KRTurboDisplayShadow>(tag, view_name);
    real_shadow_map_[tag] = shadow;
    
    // 始终创建真实的 Shadow
    render_layer_handler_->CreateShadow(tag, view_name);
}

void KRTurboDisplayRenderLayerHandler::RemoveShadow(int tag) {
    real_shadow_map_.erase(tag);
    render_layer_handler_->RemoveShadow(tag);
}

void KRTurboDisplayRenderLayerHandler::SetShadowProp(int tag, const std::string &prop_key, const KRAnyValue &prop_value) {
    // 获取 prop_value 的字符串表示
    std::string value_str = "null";
    if (prop_value) {
        if (prop_value->isString()) {
            value_str = prop_value->toString();
            if (value_str.length() > 50) {
                value_str = value_str.substr(0, 50) + "...";
            }
        } else if (prop_value->isInt()) {
            value_str = std::to_string(prop_value->toInt());
        } else if (prop_value->isDouble()) {
            value_str = std::to_string(prop_value->toDouble());
        } else if (prop_value->isBool()) {
            value_str = prop_value->toBool() ? "true" : "false";
        } else {
            value_str = "[Other]";
        }
    }
    
    // 更新真实 Shadow 树
    auto it = real_shadow_map_.find(tag);
    if (it != real_shadow_map_.end()) {
        auto shadow = it->second;
        // 将 KRAnyValue 转换为 std::any 存储
        std::any any_value = ConvertKRAnyValueToAny(prop_value);
        shadow->SetProp(prop_key, any_value);
        
    }
    
    render_layer_handler_->SetShadowProp(tag, prop_key, prop_value);
}

KRAnyValue KRTurboDisplayRenderLayerHandler::CallShadowMethod(int tag, const std::string &method_name,
                                                              const std::string &params) {
    auto it = real_shadow_map_.find(tag);
    if (it != real_shadow_map_.end()) {
        auto shadow = it->second;
        // 将 KRAnyValue 转换为 std::any 存储
        shadow->AddMethodWithName(method_name, params);
    }
    return render_layer_handler_->CallShadowMethod(tag, method_name, params);
}
// 已核对
std::shared_ptr<IKRRenderShadowExport> KRTurboDisplayRenderLayerHandler::Shadow(int tag) {
    // 获取tag对应的shadow
    auto shadow = render_layer_handler_->Shadow(tag);
    
    // 如果有TurboDisplay 模式 的shadow
    auto shadow_turbo = real_shadow_map_.find(tag);
    if (shadow_turbo != real_shadow_map_.end() && shadow) {
        // 深拷贝 TurboDisplay shadow
        auto view_shadow = shadow_turbo->second->DeepCopy();
        
        if (ui_scheduler_) {
            ui_scheduler_->AddTaskToMainQueueWithTask([this, tag, view_shadow, shadow]() {
                auto node_it = real_node_map_.find(tag);
                if (node_it != real_node_map_.end()) {
                    // 记录 Shadow 数据到 Node（用于缓存）
                    node_it->second->SetShadow(view_shadow);
                    // 关联真实的 Shadow 对象（用于运行时）
                    node_it->second->SetRenderShadow(shadow);
                }
            });
        }
    }
    
    return shadow;
}
// 已核对 moduleWithName
std::shared_ptr<IKRRenderModuleExport> KRTurboDisplayRenderLayerHandler::GetModule(const std::string &name) const {
    return render_layer_handler_->GetModule(name);
}
// ***没有此方法，但是先设置了
std::shared_ptr<IKRRenderModuleExport> KRTurboDisplayRenderLayerHandler::GetModuleOrCreate(const std::string &name) {
    return render_layer_handler_->GetModuleOrCreate(name);
}
// 对应 viewWithTag
std::shared_ptr<IKRRenderViewExport> KRTurboDisplayRenderLayerHandler::GetRenderView(int tag) {
    return render_layer_handler_->GetRenderView(tag);
}
// ***没有此方法，但是先设置了
void KRTurboDisplayRenderLayerHandler::updateViewTagWithCurTag(int oldTag, int newTag) {
    render_layer_handler_->updateViewTagWithCurTag(oldTag, newTag);
}


// 写入缓存的场景2：对象销毁时执行缓存
void KRTurboDisplayRenderLayerHandler::WillDestroy() {
    KR_LOG_INFO << "[TurboDisplay-Destroy] 🔴 WillDestroy 开始";
    // 销毁前更新缓存
    if (!next_turbo_root_node_) {
        KR_LOG_INFO << "[TurboDisplay-Destroy] 📝 next_turbo_root_node_ 为空，尝试 RewriteTurboCacheIfNeed";
        RewriteTurboCacheIfNeed();
    }
    UpdateNextTurboRootIfNeed();
    render_layer_handler_->WillDestroy();
    KR_LOG_INFO << "[TurboDisplay-Destroy] ✅ WillDestroy 完成";
}

void KRTurboDisplayRenderLayerHandler::OnDestroy() {
    KR_LOG_INFO << "[TurboDisplay-Destroy] 🔴 OnDestroy 开始"
                << ", real_node_map_ 节点数: " << real_node_map_.size()
                << ", real_shadow_map_ 节点数: " << real_shadow_map_.size();
    
    // 清理资源
    real_node_map_.clear();
    real_shadow_map_.clear();
    real_root_node_.reset();
    next_turbo_root_node_.reset();
    turbo_cache_data_.reset();
    
    render_layer_handler_->OnDestroy();
    
    KR_LOG_INFO << "[TurboDisplay-Destroy] ✅ OnDestroy 完成，资源已清理";
}

// 收到手势响应时调用
void KRTurboDisplayRenderLayerHandler::DidHitTest() {
    // 收到手势，不再自动更新
    if (next_turbo_root_node_) {
        UpdateNextTurboRootIfNeed();
        
        close_auto_update_turbo_ = true;
        next_turbo_root_node_.reset();
        
    } 
}


// #pragma mark - TurboDisplay rendering
// 已核对，对应的是renderTurboDisplayNodeToRenderLayerWithNode
void KRTurboDisplayRenderLayerHandler::RenderTurboDisplayCache() {
    KR_LOG_INFO << "[TurboDisplay-Render] 🎨 RenderTurboDisplayCache 开始";
    if (!turbo_cache_data_ || !turbo_cache_data_->GetTurboDisplayNode()) {
        KR_LOG_INFO << "[TurboDisplay-Render] ⚠️ 无缓存数据，跳过渲染";
        return;
    }
    
    auto cache_node = turbo_cache_data_->GetTurboDisplayNode();
    int children_count = cache_node->HadChild() ? cache_node->GetChildren().size() : 0;
    KR_LOG_INFO << "[TurboDisplay-Render] 📊 缓存根节点: viewName=" << cache_node->GetViewName() 
                << ", tag=" << cache_node->GetTag()
                << ", 子节点数=" << children_count;
    
    diff_patch_->DiffPatchToRenderingWithRenderLayer(
        render_layer_handler_.get(), 
        nullptr, 
        turbo_cache_data_->GetTurboDisplayNode()
    );
    KR_LOG_INFO << "[TurboDisplay-Render] ✅ RenderTurboDisplayCache 完成";
}

void KRTurboDisplayRenderLayerHandler::DiffPatchToRenderLayer() {
    KR_LOG_INFO << "[TurboDisplay-DiffPatch] 🔄 DiffPatchToRenderLayer 开始";
    auto diff_patch_start = std::chrono::steady_clock::now();
    
    // 1. 无缓存 构建目标树
    if (real_root_node_ && !next_turbo_root_node_) {
        KR_LOG_INFO << "[TurboDisplay-DiffPatch] 📝 无缓存，创建目标树（DeepCopy real_root_node_）";
        next_turbo_root_node_ = real_root_node_->DeepCopy();
        SetNeedUpdateNextTurboRoot();
    } 
    
    // 1. 有缓存，懒加载，快速加载首屏
    if (turbo_cache_data_ && turbo_cache_data_->GetTurboDisplayNode() && real_root_node_) {
        int cache_children = turbo_cache_data_->GetTurboDisplayNode()->HadChild() 
            ? turbo_cache_data_->GetTurboDisplayNode()->GetChildren().size() : 0;
        int real_children = real_root_node_->HadChild() ? real_root_node_->GetChildren().size() : 0;
        
        KR_LOG_INFO << "[TurboDisplay-DiffPatch] 📊 执行 Diff/Patch："
                    << "\n  - 缓存树子节点数: " << cache_children
                    << "\n  - 真实树子节点数: " << real_children;
        
        // Diff 缓存树和真实树，Patch 差量到渲染器 首先已经显示出来了，然后更新一下是否有变化
        diff_patch_->DiffPatchToRenderingWithRenderLayer(
            render_layer_handler_.get(), 
            turbo_cache_data_->GetTurboDisplayNode(), 
            real_root_node_
        );
        KR_LOG_INFO << "[TurboDisplay-DiffPatch] ✅ Diff/Patch 执行完成";
    } else {
        KR_LOG_INFO << "[TurboDisplay-DiffPatch] ⚠️ 无缓存数据，跳过 Diff/Patch"
                    << " (turbo_cache_data_=" << (turbo_cache_data_ ? "有" : "无")
                    << ", real_root_node_=" << (real_root_node_ ? "有" : "无") << ")";
    }
    
    auto diff_patch_end = std::chrono::steady_clock::now();
    auto diff_patch_cost = std::chrono::duration_cast<std::chrono::milliseconds>(
        diff_patch_end - diff_patch_start
    ).count();
    KR_LOG_INFO << "[TurboDisplay-DiffPatch] ⏱️ Diff/Patch 总耗时: " << diff_patch_cost << " ms";
    
    lazy_rendering_ = false;
    KR_LOG_INFO << "[TurboDisplay-DiffPatch] 🔄 lazy_rendering_ 设为 false";
    RewriteTurboCacheIfNeed();
    turbo_cache_data_ = nullptr;
    KR_LOG_INFO << "[TurboDisplay-DiffPatch] ✅ DiffPatchToRenderLayer 完成";
}
// 已核对
void KRTurboDisplayRenderLayerHandler::SetNeedUpdateNextTurboRoot() {
    if (!need_update_next_turbo_root_node_) {
        need_update_next_turbo_root_node_ = true;
        
        // 限频：500ms 内最多更新一次
        KRMainThread::RunOnMainThread([this]() {
            UpdateNextTurboRootIfNeed();
        }, 500);
    } 
}

void KRTurboDisplayRenderLayerHandler::UpdateNextTurboRootIfNeed() {
    KR_LOG_INFO << "[TurboDisplay-CACHE] 🔄 UpdateNextTurboRootIfNeed 开始"
                << ", need_update:" << (need_update_next_turbo_root_node_ ? "true" : "false")
                << ", close_auto_update:" << (close_auto_update_turbo_ ? "true" : "false")
                << ", real_root_node_存在:" << (real_root_node_ ? "true" : "false")
                << ", next_turbo_root_node_存在:" << (next_turbo_root_node_ ? "true" : "false");
    
    if (!need_update_next_turbo_root_node_) {
        KR_LOG_INFO << "[TurboDisplay-CACHE] UpdateNextTurboRootIfNeed - 无需更新，跳过";
        return;
    }
    need_update_next_turbo_root_node_ = false;
//    assert()
    // 如果关闭了自动更新，则不执行
    if (close_auto_update_turbo_) {
        return;
    }
    
    if (real_root_node_ && next_turbo_root_node_) {
        // 打印两棵树的子节点数量
        int real_children_count = real_root_node_->HadChild() ? real_root_node_->GetChildren().size() : 0;
        int next_children_count = next_turbo_root_node_->HadChild() ? next_turbo_root_node_->GetChildren().size() : 0;
        
        // valo-Debug代码
        KR_LOG_INFO << "[TurboDisplay-CACHE] 🔍 Diff 前树对比："
                    << "\n  - real_root_node_ 子节点数: " << real_children_count
                    << "\n  - next_turbo_root_node_ 子节点数: " << next_children_count;
        
        auto start_time = std::chrono::steady_clock::now();
        bool did_updated = diff_patch_->OnlyUpdateWithTargetNodeTree(next_turbo_root_node_, real_root_node_);
        auto end_time = std::chrono::steady_clock::now();
        auto diff_cost = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time).count();
        
        // 保存缓存条件：有更新 或者 (首次启动且未缓存)
        if (did_updated) {
            auto copy_start = std::chrono::steady_clock::now();
            cache_manager_->WriteCache(next_turbo_root_node_->DeepCopy(), turbo_cache_key_);
            auto copy_end = std::chrono::steady_clock::now();
            auto copy_cost = std::chrono::duration_cast<std::chrono::milliseconds>(copy_end - copy_start).count();
        } else if (!turbo_cache_data_) {
            KR_LOG_INFO << "[TurboDisplay-CACHE] ⚠️当前页面无TB缓存，强制执行一次";
            cache_manager_->WriteCache(real_root_node_->DeepCopy(), turbo_cache_key_);
        }
    } 
}
// 已核对
void KRTurboDisplayRenderLayerHandler::RewriteTurboCacheIfNeed() {
    auto turbo_cache_node_data = turbo_cache_data_ ? turbo_cache_data_->GetTurboDisplayNodeData() : std::vector<uint8_t>();
    
    if (!turbo_cache_node_data.empty()) {
        // 检查文件是否已存在（对齐 iOS 的 hasNodeWithCacheKey 检查）
        bool has_cache = cache_manager_->HasNodeWithCacheKey(turbo_cache_key_);
        
        if (!has_cache) {
            KR_LOG_INFO << "[TurboDisplay-CACHE] ⚠️rewrite执行一次";
            cache_manager_->CacheWithViewNodeData(turbo_cache_node_data, turbo_cache_key_);
        } 
    }
}



// 添加任务到下一个RunLoop统一执行（对齐 Android/iOS）
void KRTurboDisplayRenderLayerHandler::AddTaskOnNextLoopMainQueue(std::function<void()> task) {
    if (!need_sync_main_queue_on_next_runloop_) {
        need_sync_main_queue_on_next_runloop_ = true;
        
        if (next_loop_task_on_main_queue_.empty()) {
            next_loop_task_on_main_queue_.clear();
        }
        
        next_loop_task_on_main_queue_.push_back(task);
        
        // 通过 UIScheduler 调度到主线程的下一个RunLoop执行
        if (ui_scheduler_) {
            ui_scheduler_->AddTaskToMainQueueWithTask([this]() {
                need_sync_main_queue_on_next_runloop_ = false;
                
                // 取出任务队列并清空
                auto queue = std::move(next_loop_task_on_main_queue_);
                next_loop_task_on_main_queue_.clear();
                
                // 执行所有任务
                for (const auto& task_item : queue) {
                    if (task_item) {
                        task_item();
                    }
                }
            });
        } else {
            // 降级方案：如果没有 UIScheduler，使用 ScheduleTaskOnMainThread
            KRContextScheduler::ScheduleTaskOnMainThread(false, [this]() {
                need_sync_main_queue_on_next_runloop_ = false;
                
                auto queue = std::move(next_loop_task_on_main_queue_);
                next_loop_task_on_main_queue_.clear();
                
                for (const auto& task_item : queue) {
                    if (task_item) {
                        task_item();
                    }
                }
            });
        }
    } else {
        // 如果已经调度过，直接添加到任务队列
        next_loop_task_on_main_queue_.push_back(task);
    }
}
