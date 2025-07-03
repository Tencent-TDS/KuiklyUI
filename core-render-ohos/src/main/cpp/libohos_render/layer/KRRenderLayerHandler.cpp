/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#include "libohos_render/layer/KRRenderLayerHandler.h"

/**
 * 初始化
 * @param rootView 渲染根容器view
 */
void KRRenderLayerHandler::Init(std::weak_ptr<IKRRenderView> root_view,
                                std::shared_ptr<KRRenderContextParams> &context) {
    context_ = context;
    root_view_ = root_view;
}

/**
 * 创建渲染视图
 * @param tag 视图 ID
 * @param viewName 视图标签名字
 */
void KRRenderLayerHandler::CreateRenderView(int tag, const std::string &view_name) {
    auto strongRoot = root_view_.lock();
    if (strongRoot == nullptr) {
        // noop if the root view has been destroyed
        return;
    }
    auto it = view_registry_.find(tag);
    if (it == view_registry_.end()) {
        auto view = PopViewFromReuseQueue(view_name);
        if (view == nullptr) {
            view = IKRRenderViewExport::CreateView(view_name);
            if(view){
                view->SetRootView(root_view_, context_->InstanceId());
                view->SetViewName(view_name);
                view->SetViewTag(tag);
                view->ToInit();
            }else{
                KR_LOG_ERROR << "Failed to create view with name:"<<view_name<<", tag:"<<tag;
            }
        } else {
            view->SetViewTag(tag);
        }
        if (view != nullptr) {
            view_registry_[tag] = view;
        }
    }
}

/**
 * 删除渲染视图
 * @param tag 视图 ID
 */
void KRRenderLayerHandler::RemoveRenderView(int tag) {
    auto it = view_registry_.find(tag);
    if (it == view_registry_.end()) {
        return;
    }
    auto view = it->second;
    if (!view) {
        return;
    }

    view->ToRemoveFromSuperView();
    view_registry_.erase(it);
    if (view->CanReuse()) {
        PushViewToReuseQueue(view);  // 放入复用队列
    } else {
        // 触摸事件分发子系统涉及多个子系统，存在衔接问题，表现上5.0.0.102版本后比较容易出现节点析构后系统内部会因为事件派发出现crash，
        // 这里暂时做个兜底，延缓两帧再销毁view，后续系统OK后再恢复回来。
        KRContextScheduler::ScheduleTask(false, 32, [view]() {
            KRContextScheduler::ScheduleTaskOnMainThread(false, [view]() { view->ToDestroy(); });
        });
    }
}

/**
 * 父渲染视图插入子渲染视图
 * @param parentTag 父视图 ID
 * @param childTag 子视图 ID
 * @param index 插入的位置
 */
void KRRenderLayerHandler::InsertSubRenderView(int parent_tag, int child_tag, int index) {
    auto isRootViewTag = parent_tag == -1;
    auto &child_view = view_registry_[child_tag];
    if (isRootViewTag) {
        if (auto lock = root_view_.lock()) {
            lock->AddContentView(child_view, index);
        }
    } else {
        auto &parent_view = view_registry_[parent_tag];
        if (parent_view != nullptr && child_view != nullptr) {
            parent_view->ToInsertSubRenderView(child_view, index);
        }
    }
}

/**
 * 设置渲染视图属性
 * @param tag 视图 ID
 * @param propKey 属性 key
 * @param propValue 属性值
 */
void KRRenderLayerHandler::SetProp(int tag, const std::string &prop_key, const KRAnyValue &prop_value) {
    auto &view = view_registry_[tag];
    if (view != nullptr) {
        view->ToSetProp(prop_key, prop_value, nullptr);
    }
}

/**
 * 设置渲染视图事件
 * @param tag 视图 ID
 * @param propKey 属性 key
 * @param propValue 事件
 */
void KRRenderLayerHandler::SetEvent(int tag, const std::string &prop_key, const KRRenderCallback &callback) {
    auto &view = view_registry_[tag];
    if (view != nullptr) {
        view->ToSetProp(prop_key, nullptr, callback);
    }
}

/**
 * 设置 view 对应的 shadow 对象
 * @param tag 视图 ID
 * @param shadow 视图对应的 shadow 对象
 */
void KRRenderLayerHandler::SetShadow(int tag, const std::shared_ptr<IKRRenderShadowExport> &shadow) {
    auto &view = view_registry_[tag];
    if (view != nullptr) {
        view->SetShadow(shadow);
    }
}

/**
 * 渲染视图返回自定义布局尺寸
 * @param tag 视图 ID
 * @param constraintWidth 测量约束宽度
 * @param constraintHeight 测量高度宽度
 * @return 计算得到的尺寸，"${width}|${height}" 格式封装返回
 */
std::string KRRenderLayerHandler::CalculateRenderViewSize(int tag, double constraint_width, double constraint_height) {
    auto &shadow = shadow_registry_[tag];
    if (shadow != nullptr) {
        auto size = shadow->CalculateRenderViewSize(constraint_width, constraint_height);
        return kuikly::util::ConvertSizeToString(size);
    }
    return "0|0";
}

/**
 * 调用渲染视图方法
 * @param tag 视图 ID
 * @param method 视图方法
 * @param params 方法参数
 * @param callback 回调
 */
void KRRenderLayerHandler::CallViewMethod(int tag, const std::string &method, const KRAnyValue &params,
                                          const KRRenderCallback &callback) {
    auto &view = view_registry_[tag];
    if (view != nullptr) {
        view->CallMethod(method, params, callback);
    }
}

KRAnyValue KRRenderLayerHandler::CallModuleMethod(bool sync, const std::string &module_name, const std::string &method,
                                                  const KRAnyValue &params, const KRRenderCallback &callback,
                                                  bool callback_keep_alive) {
    auto module = GetModuleOrCreate(module_name);
    if (module != nullptr) {
        return module->CallMethod(sync, method, params, callback, callback_keep_alive);
    }
    return std::make_shared<KRRenderValue>(nullptr);
}

/**
 * 调用 TDF 通用 Module 方法
 * @param moduleName module 名字
 * @param method module 方法
 * @param params 参数，Json 字符串
 * @param callId, 使用 successCallback Id，如果没有就使用 TDFModulePromise#CALL_ID_NO_CALLBACK
 * @param successCallback 成功回调
 * @param errorCallback 错误回调
 */
KRAnyValue KRRenderLayerHandler::CallTDFModuleMethod(const std::string &module_name, const std::string &method,
                                                     const std::string &params, const std::string &call_id,
                                                     const KRRenderCallback &success_callback,
                                                     KRRenderCallback &error_callback) {
    return std::make_shared<KRRenderValue>();
}

/**
 * 创建 shadow
 * @param tag 视图 ID
 * @param viewName 视图名字
 */
void KRRenderLayerHandler::CreateShadow(int tag, const std::string &view_name) {
    auto it = shadow_registry_.find(tag);
    if (it == shadow_registry_.end()) {
        auto shadow = IKRRenderShadowExport::CreateShadow(view_name);
        if (shadow != nullptr) {
            shadow->SetRootView(root_view_);
            shadow_registry_[tag] = shadow;
        }
    }
}

/**
 * 删除 shadow
 * @param tag 视图 ID
 */
void KRRenderLayerHandler::RemoveShadow(int tag) {
    shadow_registry_.erase(tag);
}

/**
 * 设置 shadow 对象的属性
 * @param tag shadow 对象的 ID
 * @param propKey 属性 key
 * @param propValue 属性值
 */
void KRRenderLayerHandler::SetShadowProp(int tag, const std::string &prop_key, const KRAnyValue &prop_value) {
    auto it = shadow_registry_.find(tag);
    if (it != shadow_registry_.end()) {
        auto shadow = it->second;
        if (shadow != nullptr) {
            shadow->SetProp(prop_key, prop_value);
        }
    }
}

/**
 * 获取指定 ID 的 shadow 对象
 * @param tag shadow 对象的 ID
 * @return 对应 ID 的 shadow 对象，如果不存在则返回 null
 */
std::shared_ptr<IKRRenderShadowExport> KRRenderLayerHandler::Shadow(int tag) {
    return shadow_registry_[tag];
}

/**
 * 调用指定 ID 的 shadow 对象的方法
 * @param tag shadow 对象的 ID
 * @param methodName 方法名
 * @param params 方法参数
 * @return 方法调用的返回值，如果方法不存在则返回 null
 */
KRAnyValue KRRenderLayerHandler::CallShadowMethod(int tag, const std::string &method_name, const std::string &params) {
    auto shadow = Shadow(tag);
    if (shadow != nullptr) {
        return shadow->Call(method_name, params);
    }
    return std::make_shared<KRRenderValue>(nullptr);
}

/**
 * 获取指定名称的 module 实例
 * @param name module 的名称
 * @return 对应名称的 module 实例，如果不存在则返回 null
 */
std::shared_ptr<IKRRenderModuleExport> KRRenderLayerHandler::GetModule(const std::string &name) {
    std::shared_lock lock(module_rw_mutex_);
    if (destroying_) {
        return nullptr;
    }
    return module_registry_[name];
}

/**
 * 获取指定 ID 的渲染视图实例
 * @param tag 渲染视图的 ID
 * @return 对应 ID 的渲染视图实例，如果不存在则返回 null
 */
std::shared_ptr<IKRRenderViewExport> KRRenderLayerHandler::GetRenderView(int tag) {
    return view_registry_[tag];
}

/**
 * 将要销毁时调用
 */
void KRRenderLayerHandler::WillDestroy() {}

/**
 * 销毁时调用，用于清理资源
 */
void KRRenderLayerHandler::OnDestroy() {
    destroying_ = true;
    for (const auto &entry : view_registry_) {
        const std::shared_ptr<IKRRenderViewExport> &value = entry.second;
        if (value) {
            value->ToDestroy();
        }
    }
    // views should be clear, otherwise pending async ops like RemoveRenderView or InsertSubRenderView
    // would still be able to find them and could cause unexpected behaviors
    view_registry_.clear();

    {  // auto lock sub-scope to destroy modules
        std::unique_lock lock(module_rw_mutex_);
        for (const auto &entry : module_registry_) {
            const std::shared_ptr<IKRRenderModuleExport> &value = entry.second;
            if (value) {
                value->OnDestroy();
            }
        }
        module_registry_.clear();
    }

    for (const auto &entry0 : view_reuse_queue_) {
        for (const auto &entry1 : entry0.second) {
            if (entry1) {
                entry1->ToDestroy();
            }
        }
    }
    view_reuse_queue_.clear();
}
/*** private ****/

std::shared_ptr<IKRRenderViewExport> KRRenderLayerHandler::PopViewFromReuseQueue(const std::string &view_name) {
    auto it = view_reuse_queue_.find(view_name);
    if (it != view_reuse_queue_.end()) {
        // 键存在，可以安全地访问元素
        auto &queue = it->second;
        if (!queue.empty()) {
            auto view = queue.back();
            queue.pop_back();
            return view;
        }
    }
    return nullptr;
}

void KRRenderLayerHandler::PushViewToReuseQueue(std::shared_ptr<IKRRenderViewExport> view) {
    auto view_name = view->GetViewName();
    auto &queue = view_reuse_queue_[view_name];
    view->ToReuse();
    queue.push_back(view);
}

std::shared_ptr<IKRRenderModuleExport> KRRenderLayerHandler::GetModuleOrCreate(const std::string &module_name) {
    if (destroying_) {
        return nullptr;
    }

    auto module = GetModule(module_name);
    if (module == nullptr) {
        // 写操作
        std::unique_lock lock(module_rw_mutex_);
        if (destroying_) {
            return nullptr;
        }
        module = module_registry_[module_name];
        if (module == nullptr) {
            module = IKRRenderModuleExport::CreateModule(module_name);
            if (module != nullptr) {
                module->SetRootView(root_view_, context_->InstanceId());
                module->SetModuleName(module_name);
                module_registry_[module_name] = module;
            }
        }
    }
    return module;
}
