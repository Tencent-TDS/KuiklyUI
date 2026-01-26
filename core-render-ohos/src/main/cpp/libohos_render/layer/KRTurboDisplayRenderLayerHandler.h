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

#ifndef CORE_RENDER_OHOS_KRTURBODISPLAYRENDERLAYERHANDLER_H
#define CORE_RENDER_OHOS_KRTURBODISPLAYRENDERLAYERHANDLER_H

#include <memory>
#include <string>
#include <unordered_map>
#include <vector>
#include "libohos_render/context/KRRenderContextParams.h"
#include "libohos_render/layer/IKRRenderLayer.h"
#include "libohos_render/layer/KRRenderLayerHandler.h"

// Forward declarations for TurboDisplay data structures (must be in KuiklyOhos namespace)
namespace KuiklyOhos {
    class KRTurboDisplayNode;
    class KRTurboDisplayShadow;
    struct KRTurboDisplayCacheData;
    class KRTurboDisplayCacheManager;
    class KRTurboDisplayDiffPatch;
}

// Forward declaration for UIScheduler
class KRUIScheduler;

/**
 * TurboDisplay 渲染层处理器
 * 实现双渲染策略：缓存首屏 + 差量更新
 */
class KRTurboDisplayRenderLayerHandler : public IKRRenderLayer {
 public:
    KRTurboDisplayRenderLayerHandler();
    ~KRTurboDisplayRenderLayerHandler() = default;

    /**
     * 设置 UIScheduler
     * @param ui_scheduler UI调度器
     */
    void SetUIScheduler(std::shared_ptr<KRUIScheduler> ui_scheduler) {
        ui_scheduler_ = ui_scheduler;
    }

    /**
     * 初始化
     * @param root_view 渲染根容器view
     * @param context 上下文参数
     */
    void Init(std::weak_ptr<IKRRenderView> root_view, std::shared_ptr<KRRenderContextParams> &context) override;

    /**
     * 初始化完成后调用
     * 用于读取缓存、注册 UIScheduler 回调等操作
     */
    void DidInit() override;

    /**
     * 创建渲染视图
     * @param tag 视图 ID
     * @param view_name 视图标签名字
     */
    void CreateRenderView(int tag, const std::string &view_name) override;

    /**
     * 删除渲染视图
     * @param tag 视图 ID
     */
    void RemoveRenderView(int tag) override;

    /**
     * 父渲染视图插入子渲染视图
     * @param parent_tag 父视图 ID
     * @param child_tag 子视图 ID
     * @param index 插入的位置
     */
    void InsertSubRenderView(int parent_tag, int child_tag, int index) override;

    /**
     * 设置渲染视图属性
     * @param tag 视图 ID
     * @param prop_key 属性 key
     * @param prop_value 属性值
     */
    void SetProp(int tag, const std::string &prop_key, const KRAnyValue &prop_value) override;

    /**
     * 设置渲染视图事件
     * @param tag 视图 ID
     * @param prop_key 属性 key
     * @param callback 事件回调
     */
    void SetEvent(int tag, const std::string &prop_key, const KRRenderCallback &callback) override;

    /**
     * 设置 view 对应的 shadow 对象
     * @param tag 视图 ID
     * @param shadow 视图对应的 shadow 对象
     */
    void SetShadow(int tag, const std::shared_ptr<IKRRenderShadowExport> &shadow) override;

    /**
     * 设置渲染视图的 frame
     * @param tag 视图 ID
     * @param x frame x 坐标
     * @param y frame y 坐标
     * @param width frame 宽度
     * @param height frame 高度
     */
    void SetRenderViewFrame(int tag, double x, double y, double width, double height);

    /**
     * 渲染视图返回自定义布局尺寸
     * @param tag 视图 ID
     * @param constraint_width 测量约束宽度
     * @param constraint_height 测量约束高度
     * @return 计算得到的尺寸，"${width}|${height}" 格式封装返回
     */
    std::string CalculateRenderViewSize(int tag, double constraint_width, double constraint_height) override;

    /**
     * 调用渲染视图方法
     * @param tag 视图 ID
     * @param method 视图方法
     * @param params 方法参数
     * @param callback 回调
     */
    void CallViewMethod(int tag, const std::string &method, const KRAnyValue &params,
                        const KRRenderCallback &callback) override;

    /**
     * 调用 module 方法
     * @param sync 是否同步调用
     * @param module_name module 名字
     * @param method module 方法
     * @param params 参数
     * @param callback 回调
     * @param callback_keep_alive callback是否keep alive
     */
    KRAnyValue CallModuleMethod(bool sync, const std::string &module_name, const std::string &method,
                                const KRAnyValue &params, const KRRenderCallback &callback,
                                bool callback_keep_alive) override;

    /**
     * 调用 TDF 通用 Module 方法
     * @param module_name module 名字
     * @param method module 方法
     * @param params 参数，Json 字符串
     * @param call_id 使用 successCallback Id
     * @param success_callback 成功回调
     * @param error_callback 错误回调
     */
    KRAnyValue CallTDFModuleMethod(const std::string &module_name, const std::string &method, const std::string &params,
                                   const std::string &call_id, const KRRenderCallback &success_callback,
                                   KRRenderCallback &error_callback) override;

    /**
     * 创建 shadow
     * @param tag 视图 ID
     * @param view_name 视图名字
     */
    void CreateShadow(int tag, const std::string &view_name) override;

    /**
     * 删除 shadow
     * @param tag 视图 ID
     */
    void RemoveShadow(int tag) override;

    /**
     * 设置 shadow 对象的属性
     * @param tag shadow 对象的 ID
     * @param prop_key 属性 key
     * @param prop_value 属性值
     */
    void SetShadowProp(int tag, const std::string &prop_key, const KRAnyValue &prop_value) override;

    /**
     * 获取指定 ID 的 shadow 对象
     * @param tag shadow 对象的 ID
     * @return 对应 ID 的 shadow 对象，如果不存在则返回 null
     */
    std::shared_ptr<IKRRenderShadowExport> Shadow(int tag) override;

    /**
     * 调用指定 ID 的 shadow 对象的方法
     * @param tag shadow 对象的 ID
     * @param method_name 方法名
     * @param params 方法参数
     * @return 方法调用的返回值，如果方法不存在则返回 null
     */
    KRAnyValue CallShadowMethod(int tag, const std::string &method_name, const std::string &params) override;

    /**
     * 获取指定名称的 module 实例
     * @param name module 的名称
     * @return 对应名称的 module 实例，如果不存在则返回 null
     */
    std::shared_ptr<IKRRenderModuleExport> GetModule(const std::string &name) const override;

    /**
     * 获取指定名称的 module 实例，如果不存在则创建
     * @param name module 的名称
     * @return 对应名称的 module 实例
     */
    std::shared_ptr<IKRRenderModuleExport> GetModuleOrCreate(const std::string &name) override;

    /**
     * 获取指定 ID 的渲染视图实例
     * @param tag 渲染视图的 ID
     * @return 对应 ID 的渲染视图实例，如果不存在则返回 null
     */
    std::shared_ptr<IKRRenderViewExport> GetRenderView(int tag) override;
    
    /**
     * 获取指定 ID 的渲染视图实例
     * @param tag 渲染视图的 ID
     * @return 对应 ID 的渲染视图实例，如果不存在则返回 null
     */
    void updateViewTagWithCurTag(int oldTag, int newTag) override;

    /**
     * 将要销毁时调用
     */
    void WillDestroy() override;

    /**
     * 销毁时调用，用于清理资源
     */
    void OnDestroy() override;

    /**
     * 收到手势响应时调用
     * 用于禁用自动缓存更新，由业务主动控制
     */
    void DidHitTest();
    
 private:
    /**
     * UI调度器（对齐 Android/iOS）
     */
    std::shared_ptr<KRUIScheduler> ui_scheduler_;

    /**
     * 内部持有的普通渲染器
     */
    std::shared_ptr<KRRenderLayerHandler> render_layer_handler_;

    /**
     * 真实节点树（记录完整的渲染指令）
     */
    std::unordered_map<int, std::shared_ptr<KuiklyOhos::KRTurboDisplayNode>> real_node_map_;

    /**
     * 真实 Shadow 树
     */
    std::unordered_map<int, std::shared_ptr<KuiklyOhos::KRTurboDisplayShadow>> real_shadow_map_;

    /**
     * 真实渲染树根节点
     */
    std::shared_ptr<KuiklyOhos::KRTurboDisplayNode> real_root_node_;

    /**
     * 懒渲染标记（是否正在使用 TurboDisplay 缓存首屏）
     */
    bool lazy_rendering_ = false;

    /**
     * TurboDisplay 缓存数据
     */
    std::shared_ptr<KuiklyOhos::KRTurboDisplayCacheData> turbo_cache_data_;

    /**
     * 下次 TurboDisplay 首屏（用于保存新的缓存）
     */
    std::shared_ptr<KuiklyOhos::KRTurboDisplayNode> next_turbo_root_node_;

    /**
     * 是否需要更新下次首屏缓存
     */
    bool need_update_next_turbo_root_node_ = false;

    /**
     * 关闭自动更新 TurboDisplay（收到用户手势后）
     */
    bool close_auto_update_turbo_ = false;

    /**
     * 是否已缓存首屏
     */
    bool did_cache_turbo_ = false;

    /**
     * 下一个RunLoop需要同步主队列任务（对齐 Android/iOS）
     */
    bool need_sync_main_queue_on_next_runloop_ = false;

    /**
     * 下一个RunLoop要执行的主队列任务列表（对齐 Android/iOS）
     */
    std::vector<std::function<void()>> next_loop_task_on_main_queue_;

    /**
     * 上下文参数
     */
    std::shared_ptr<KRRenderContextParams> context_;

    /**
     * TurboDisplay 缓存管理器
     */
    std::shared_ptr<KuiklyOhos::KRTurboDisplayCacheManager> cache_manager_;

    /**
     * Diff Patch 算法实现
     */
    std::shared_ptr<KuiklyOhos::KRTurboDisplayDiffPatch> diff_patch_;

    /**
     * TurboDisplay 缓存 Key
     */
    std::string turbo_cache_key_;
    
    /**
     * 根视图弱引用
     */
    std::weak_ptr<IKRRenderView> root_view_;

    /**
     * 首屏耗时统计：页面初始化开始时间（Init 开始）
     */
    std::chrono::steady_clock::time_point init_start_time_;

    /**
     * 首屏耗时统计：缓存渲染完成时间
     */
    std::chrono::steady_clock::time_point cache_render_complete_time_;

    /**
     * 首屏耗时统计：DiffPatch 完成时间
     */
    std::chrono::steady_clock::time_point diff_patch_complete_time_;

    /**
     * 渲染 TurboDisplay 缓存首屏到渲染器
     */
    void RenderTurboDisplayCache();

    /**
     * Diff 两棵树并 Patch 差量到渲染器
     */
    void DiffPatchToRenderLayer();

    /**
     * 标记需要更新下次首屏缓存
     */
    void SetNeedUpdateNextTurboRoot();

    /**
     * 更新下次首屏缓存（如果需要）
     */
    void UpdateNextTurboRootIfNeed();

    /**
     * 回写 TurboDisplay 缓存（兜底机制）
     */
    void RewriteTurboCacheIfNeed();

    /**
     * 添加任务到下一个RunLoop统一执行（对齐 Android/iOS）
     * @param task 要执行的任务
     */
    void AddTaskOnNextLoopMainQueue(std::function<void()> task);

    /**
     * 根节点 Tag（与 Android/iOS 保持一致）
     */
    static constexpr int ROOT_VIEW_TAG = -1;
    static constexpr const char* ROOT_VIEW_NAME = "RootView";
};

#endif  // CORE_RENDER_OHOS_KRTURBODISPLAYRENDERLAYERHANDLER_H
