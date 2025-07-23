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

#ifndef CORE_RENDER_OHOS_IKRRENDERLAYER_H
#define CORE_RENDER_OHOS_IKRRENDERLAYER_H

#include <any>
#include <functional>
#include <map>
#include <memory>
#include <string>
#include "libohos_render/context/KRRenderContextParams.h"
#include "libohos_render/export/IKRRenderModuleExport.h"
#include "libohos_render/export/IKRRenderShadowExport.h"
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/view/IKRRenderView.h"

class IKRRenderLayer {
 public:
    /**
     * 初始化i
     * @param rootView 渲染根容器view
     */
    virtual void Init(std::weak_ptr<IKRRenderView> root_view, std::shared_ptr<KRRenderContextParams> &context) = 0;

    /**
     * 创建渲染视图
     * @param tag 视图 ID
     * @param viewName 视图标签名字
     */
    virtual void CreateRenderView(int tag, const std::string &view_name) = 0;

    /**
     * 删除渲染视图
     * @param tag 视图 ID
     */
    virtual void RemoveRenderView(int tag) = 0;

    /**
     * 父渲染视图插入子渲染视图
     * @param parentTag 父视图 ID
     * @param childTag 子视图 ID
     * @param index 插入的位置
     */
    virtual void InsertSubRenderView(int parent_tag, int child_tag, int index) = 0;

    /**
     * 设置渲染视图属性
     * @param tag 视图 ID
     * @param propKey 属性 key
     * @param propValue 属性值
     */
    virtual void SetProp(int tag, const std::string &prop_key, const KRAnyValue &prop_value) = 0;

    /**
     * 设置渲染视图事件
     * @param tag 视图 ID
     * @param propKey 属性 key
     * @param propValue 事件
     */
    virtual void SetEvent(int tag, const std::string &prop_key, const KRRenderCallback &callback) = 0;

    /**
     * 设置 view 对应的 shadow 对象
     * @param tag 视图 ID
     * @param shadow 视图对应的 shadow 对象
     */
    virtual void SetShadow(int tag, const std::shared_ptr<IKRRenderShadowExport> &shadow) = 0;

    /**
     * 渲染视图返回自定义布局尺寸
     * @param tag 视图 ID
     * @param constraintWidth 测量约束宽度
     * @param constraintHeight 测量高度宽度
     * @return 计算得到的尺寸，"${width}|${height}" 格式封装返回
     */
    virtual std::string CalculateRenderViewSize(int tag, double constraint_width, double constraint_height) = 0;

    /**
     * 调用渲染视图方法
     * @param tag 视图 ID
     * @param method 视图方法
     * @param params 方法参数
     * @param callback 回调
     *
     */
    virtual void CallViewMethod(int tag, const std::string &method, const KRAnyValue &params,
                                const KRRenderCallback &callback) = 0;

    /**
     * 调用 module 方法
     * @param sync 是否同步调用
     * @param moduleName module 名字
     * @param method module 方法
     * @param params 参数
     * @param callback 回调
     * @param callback_keep_alive callback 是否 keep alive
     */
    virtual KRAnyValue CallModuleMethod(bool sync, const std::string &module_name, const std::string &method,
                                        const KRAnyValue &params, const KRRenderCallback &callback,
                                        bool callback_keep_alive) = 0;

    /**
     * 调用 TDF 通用 Module 方法
     * @param moduleName module 名字
     * @param method module 方法
     * @param params 参数，Json 字符串
     * @param callId, 使用 successCallback Id，如果没有就使用 TDFModulePromise#CALL_ID_NO_CALLBACK
     * @param successCallback 成功回调
     * @param errorCallback 错误回调
     */
    virtual KRAnyValue CallTDFModuleMethod(const std::string &module_name, const std::string &method,
                                           const std::string &params, const std::string &call_id,
                                           const KRRenderCallback &success_callback,
                                           KRRenderCallback &error_callback) = 0;

    /**
     * 创建 shadow
     * @param tag 视图 ID
     * @param viewName 视图名字
     */
    virtual void CreateShadow(int tag, const std::string &view_name) = 0;

    /**
     * 删除 shadow
     * @param tag 视图 ID
     */
    virtual void RemoveShadow(int tag) = 0;

    /**
     * 设置 shadow 对象的属性
     * @param tag shadow 对象的 ID
     * @param propKey 属性 key
     * @param propValue 属性值
     */
    virtual void SetShadowProp(int tag, const std::string &prop_key, const KRAnyValue &prop_value) = 0;

    /**
     * 获取指定 ID 的 shadow 对象
     * @param tag shadow 对象的 ID
     * @return 对应 ID 的 shadow 对象，如果不存在则返回 null
     */
    virtual std::shared_ptr<IKRRenderShadowExport> Shadow(int tag) = 0;

    /**
     * 调用指定 ID 的 shadow 对象的方法
     * @param tag shadow 对象的 ID
     * @param methodName 方法名
     * @param params 方法参数
     * @return 方法调用的返回值，如果方法不存在则返回 null
     */
    virtual KRAnyValue CallShadowMethod(int tag, const std::string &method_name, const std::string &params) = 0;

    /**
     * 获取指定名称的 module 实例
     * @param name module 的名称
     * @return 对应名称的 module 实例，如果不存在则返回 null
     */
    virtual std::shared_ptr<IKRRenderModuleExport> GetModule(const std::string &name) = 0;

    /**
     * * 获取指定名称的 module 实例
     * @param name module 的名称
     * @return 对应名称的 module 实例，如果不存在则新建一个
     */
    virtual std::shared_ptr<IKRRenderModuleExport> GetModuleOrCreate(const std::string &name) = 0;

    /**
     * 获取指定 ID 的渲染视图实例
     * @param tag 渲染视图的 ID
     * @return 对应 ID 的渲染视图实例，如果不存在则返回 null
     */
    virtual std::shared_ptr<IKRRenderViewExport> GetRenderView(int tag) = 0;

    /**
     * 将要销毁时调用
     */
    virtual void WillDestroy() = 0;

    /**
     * 销毁时调用，用于清理资源
     */
    virtual void OnDestroy() = 0;
};
#endif  // CORE_RENDER_OHOS_IKRRENDERLAYER_H
