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

#ifndef CORE_RENDER_OHOS_APNGANIMATEVIEW_H
#define CORE_RENDER_OHOS_APNGANIMATEVIEW_H

#include <arkui/native_type.h>
#include <array>
#include <cstdint>
#include <cstring>
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>
#include "libohos_render/expand/components/apng/APNGStructs.h"
#include "libohos_render/foundation/KRRect.h"
#include "libohos_render/foundation/thread/KRGCDQueue.h"
#include "libohos_render/utils/KRViewUtil.h"
/**
 * @class APNGAnimateView
 * @brief 用于显示和播放 APNG 动画的视图类
 *
 * 该类提供了 APNG 动画的加载、播放、暂停、停止等功能，并支持设置自动播放、循环次数等属性。
 * 通过注册回调函数，可以在加载失败、动画开始和动画结束时执行自定义操作。
 */
class APNGAnimateView : public std::enable_shared_from_this<APNGAnimateView> {
 public:
    /**
     * 实例初始化构造器
     * @param filePath 设置资源文件路径
     * @param parentNode 父节点句柄（参考：）
     * @param autoPlay 是否加载完自动播放
     * @param parentFrame 父节点布局大小
     */
    void Init(std::string &filePath, ArkUI_NodeHandle parentNode, bool autoPlay, KRRect parentFrame);

    /**
     * 销毁视图及其资源
     */
    void Destroy();

    /**
     * 析构函数
     */
    ~APNGAnimateView();

    /**
     * 设置是否自动播放
     * @param auto_play 自动播放标志
     */
    void SetAutoPlay(bool auto_play);

    /**
     * 设置动画循环次数
     * @param count 循环次数
     */
    void SetRepeatCount(int count);

    /**
     * 如果需要，同步自动播放状态
     */
    void SyncAutoPlayIfNeed();

    /**
     * 开始播放动画
     */
    void Play();

    /**
     * 停止播放动画
     */
    void Stop();

    /**
     * 设置视图布局大小
     * @param parent_frame 视图布局大小
     */
    void SetFrame(KRRect parent_frame);

    /**
     * 注册加载失败回调
     * @param callback 加载失败回调函数
     */
    void RegisterLoadFailure(std::function<void()> callback);

    /**
     * 注册动画开始回调
     * @param callback 动画开始回调函数
     */
    void RegisterAnimationStart(std::function<void()> callback);

    /**
     * 注册动画结束回调
     * @param callback 动画结束回调函数
     */
    void RegisterAnimationEnd(std::function<void()> callback);

 private:
    ArkUI_NodeHandle parent_node_ = nullptr;  // 父节点句柄
    ArkUI_NodeHandle image_node_ = nullptr;   // 图片节点句柄
    std::shared_ptr<APNG> apng_ = nullptr;    // APNG 动画对象
    bool auto_play_ = true;                   // 是否自动播放
    int32_t current_frame_index_ = -1;        // 当前帧索引
    int32_t play_timeout_flag_ = -1;          // 播放超时标志
    uint32_t repeat_count_ = INT32_MAX;       // 循环播放次数，默认无限循环
    uint32_t did_play_loop_count_ = 0;        // 已播放的循环次数
    float speed_rate_ = 1;                    // 播放速率
    KRRect frame_;                            // 视图布局大小

    std::function<void()> load_failure_callback_ = nullptr;     // 加载失败回调函数
    std::function<void()> animation_start_callback_ = nullptr;  // 动画开始回调函数
    std::function<void()> animation_end_callback_ = nullptr;    // 动画结束回调函数

    /**
     * 加载成功处理
     * @param apng 加载成功的 APNG 对象
     */
    void LoadSuccess(std::shared_ptr<APNG> apng);

    /**
     * 加载失败处理
     */
    void LoadFailure();

    /**
     * 播放下一帧
     */
    void PlayNextFrame();

    /**
     * 更新当前帧以进行渲染
     * @param apngDrawable APNG 可绘制对象
     */
    void UpdateCurrentFrameToRender(std::shared_ptr<APNGDrawable> apngDrawable);
};

#endif  // CORE_RENDER_OHOS_APNGANIMATEVIEW_H
