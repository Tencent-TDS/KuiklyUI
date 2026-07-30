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

#ifndef CORE_RENDER_OHOS_APNGSTRUCTS_H
#define CORE_RENDER_OHOS_APNGSTRUCTS_H

#include <arkui/drawable_descriptor.h>
#include <hilog/log.h>
#include <linux/kd.h>
#include <multimedia/image_framework/image/image_common.h>
#include <multimedia/image_framework/image/image_packer_native.h>
#include <multimedia/image_framework/image/image_receiver_native.h>
#include <multimedia/image_framework/image/image_source_native.h>
#include <multimedia/image_framework/image/pixelmap_native.h>
#include <multimedia/image_framework/image_pixel_map_mdk.h>
#include <native_drawing/drawing_canvas.h>
#include <array>
#include <cstdint>
#include <cstring>
#include <execution>  // For std::execution::par
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>
#include "libohos_render/expand/components/apng/APNGUtil.h"
#include "libohos_render/utils/KRRenderLoger.h"

class Frame {
 public:
    int left = 0;
    int top = 0;
    int width = 0;
    int height = 0;
    int delay = 0;
    int disposeOp = 0;
    int blendOp = 0;
    std::vector<std::vector<uint8_t>> dataParts;
    std::vector<uint8_t> data;
    std::string base64Image;

    void SetImageBuffer(std::vector<std::vector<uint8_t>> &image_buffer);
};

class APNGDrawable {
 public:
    int nextFrameDelay = 0;  // 如果为-1, 则没有下一个drawable
    bool isLast = false;
    OH_PixelmapNative *pixelmap = nullptr;
    ArkUI_DrawableDescriptor *drawable = nullptr;
};

class APNG {
 public:
    bool isAPNG = true;
    int width = 0;
    int height = 0;
    int numPlays = 0;
    int playTime = 0;
    std::vector<std::shared_ptr<Frame>> frames;
    std::vector<std::shared_ptr<APNGDrawable>> animateDrawables;  // 播放帧
    ~APNG();

    void WillParseFrame() {
        isParsing.store(true);
    }
    bool DidAddFrame(std::shared_ptr<Frame> frame, int index);
    void DidEndParserFrame() {
        isParsing.store(false);
        lazyBeforeNextFrameTask = nullptr;
    }

    bool IsParsing() {
        return isParsing.load();
    }

    std::shared_ptr<APNGDrawable> GetDrawable(int index);

 private:
    std::atomic_bool isParsing = true;
    std::function<void()> lazyBeforeNextFrameTask = nullptr;
    std::vector<uint8_t> curBitmapBuffer;
    std::vector<uint8_t> previousBuffer;
    std::mutex drawableMutex;

    bool hasFirstFullFrame = false;

    void HandleFrameBlendOp(std::shared_ptr<Frame> frame, int index);

    void HandlePostFrameDisposeOp(std::shared_ptr<Frame> frame);
    //
    void AddDrawable(std::shared_ptr<APNGDrawable> drawable, int curFrameIndex);
};

enum APNGEvent { LOAD_FAILURE, ANIMATION_START, ANIMATION_END };

#endif  // CORE_RENDER_OHOS_APNGSTRUCTS_H
