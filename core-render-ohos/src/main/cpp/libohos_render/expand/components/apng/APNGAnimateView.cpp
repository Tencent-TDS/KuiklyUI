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

#include "libohos_render/expand/components/apng/APNGAnimateView.h"

#include "libohos_render/expand/components/apng/APNGCache.h"
#include "libohos_render/foundation/thread/KRGCDQueue.h"
#include "libohos_render/utils/KRRenderLoger.h"
/**
 * 实例初始化构造器
 * @param filePath 设置资源文件路径
 * @param autoPlay 是否加载完自动播放
 */
void APNGAnimateView::Init(std::string &filePath, ArkUI_NodeHandle parentNode, bool autoPlay, KRRect parentFrame) {
    parent_node_ = parentNode;
    image_node_ = kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_IMAGE);
    kuikly::util::GetNodeApi()->addChild(parentNode, image_node_);
    kuikly::util::SetArkUIIMageResizeMode(image_node_, ARKUI_OBJECT_FIT_CONTAIN);
    SetFrame(parentFrame);
    SetAutoPlay(autoPlay);
    // 放入线程池执行IO操作
    std::shared_ptr<APNGAnimateView> self = shared_from_this();
    auto start = std::chrono::steady_clock::now();
    FetchAPNG(filePath, [self, start](std::shared_ptr<APNG> apng) {
        if (apng) {
            // 加载成功
            self->LoadSuccess(apng);
            auto end = std::chrono::steady_clock::now();
            auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
            KR_LOG_INFO << "LoadSuccess png cost time: " << duration.count();

        } else {
            self->LoadFailure();
        }
    });
}

void APNGAnimateView::Destroy() {
    Stop();
    if (parent_node_) {
        kuikly::util::GetNodeApi()->removeChild(parent_node_, image_node_);
        kuikly::util::GetNodeApi()->disposeNode(image_node_);
        image_node_ = nullptr;
        parent_node_ = nullptr;
    }
}

APNGAnimateView::~APNGAnimateView() {
    Destroy();
    KRGCDQueue::GetInstance().DispatchAsync([apng = apng_] {
        apng->width;  // sub thread gc
    });
}

void APNGAnimateView::SetAutoPlay(bool auto_play) {
    if (auto_play_ != auto_play) {
        auto_play_ = auto_play;
        SyncAutoPlayIfNeed();
    }
}

void APNGAnimateView::SetRepeatCount(int count) {
    repeat_count_ = count;
}

void APNGAnimateView::SyncAutoPlayIfNeed() {
    if (apng_) {
        if (auto_play_) {
            Play();
        } else {
            Stop();
        }
    }
}

void APNGAnimateView::Play() {
    auto_play_ = true;
    if (apng_) {
        if (play_timeout_flag_ == -1) {
            PlayNextFrame();
        }
    }
}

void APNGAnimateView::Stop() {
    auto_play_ = false;
    play_timeout_flag_ = -1;
    current_frame_index_ = -1;
    did_play_loop_count_ = 0;
}

void APNGAnimateView::SetFrame(KRRect parent_frame) {
    frame_ = parent_frame;
    kuikly::util::UpdateNodeFrame(image_node_, KRRect(0, 0, parent_frame.width, parent_frame.height));
}

void APNGAnimateView::LoadSuccess(std::shared_ptr<APNG> apng) {
    // 开始播放
    apng_ = apng;
    SyncAutoPlayIfNeed();
    if (animation_start_callback_) {
        animation_start_callback_();
    }
}

// 加载失败
void APNGAnimateView::LoadFailure() {
    if (load_failure_callback_) {
        load_failure_callback_();
    }
}

void APNGAnimateView::PlayNextFrame() {
    if (apng_->frames.size() == 0) {
        return;
    }
    bool need_play_next_frame = true;

    std::shared_ptr<APNGDrawable> apngDrawable = apng_->GetDrawable(current_frame_index_ + 1);
    if (apngDrawable == nullptr) {
        if (!(apng_->IsParsing())) {  // 说明最后一个了
            did_play_loop_count_ += 1;
            if (did_play_loop_count_ < repeat_count_) {
                current_frame_index_ = 0;
                apngDrawable = apng_->GetDrawable(current_frame_index_);
            } else {
                need_play_next_frame = false;
            }
        } else {  // 还在解析 延迟等待
            need_play_next_frame = true;
        }
    } else {
        current_frame_index_ += 1;
    }
    if (apngDrawable && apngDrawable->isLast && did_play_loop_count_ + 1 >= repeat_count_) {
        need_play_next_frame = false;
    }

    if (!need_play_next_frame) {
        if (animation_end_callback_) {
            animation_end_callback_();
        }
        // 播放结束
        return;
    }
    UpdateCurrentFrameToRender(apngDrawable);
    int delay = 16;
    if (apngDrawable) {
        delay = apngDrawable->nextFrameDelay / speed_rate_;
    }
    delay = std::max(delay, 16);
    play_timeout_flag_ += 1;
    auto flag = play_timeout_flag_;
    std::shared_ptr<APNGAnimateView> self = shared_from_this();
    KRMainThread::RunOnMainThread(
        [flag, self] {
            if (self->play_timeout_flag_ != flag) {
                return;
            }
            self->PlayNextFrame();
        },
        delay);
}

void APNGAnimateView::UpdateCurrentFrameToRender(std::shared_ptr<APNGDrawable> apngDrawable) {
    if (apngDrawable && apngDrawable->drawable) {
        kuikly::util::SetArkUIImageSrc(image_node_, apngDrawable->drawable);
    }
}

void APNGAnimateView::RegisterLoadFailure(std::function<void()> callback) {
    load_failure_callback_ = callback;
}

void APNGAnimateView::RegisterAnimationStart(std::function<void()> callback) {
    animation_start_callback_ = callback;
}

void APNGAnimateView::RegisterAnimationEnd(std::function<void()> callback) {
    animation_end_callback_ = callback;
}
