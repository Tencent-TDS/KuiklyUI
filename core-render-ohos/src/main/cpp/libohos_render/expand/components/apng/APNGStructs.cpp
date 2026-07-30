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

#include "libohos_render/expand/components/apng/APNGStructs.h"

#include <cstddef>
#include <cstdint>

void Frame::SetImageBuffer(std::vector<std::vector<uint8_t>> &image_buffer) {
    if (width == 0) {
        return;
    }
    // 预先计算所需的总内存大小
    size_t totalSize = 0;
    for (const auto &arr : image_buffer) {
        totalSize += arr.size();
    }
    // 为 buffer 预分配内存
    std::vector<uint8_t> &buffer = data;
    buffer.reserve(totalSize);
    // 合并 image_buffer 中的所有数组
    for (const auto &arr : image_buffer) {
        std::copy(arr.begin(), arr.end(), std::back_inserter(buffer));
    }
}

static OH_PixelmapNative *CreatePixelMap(std::shared_ptr<Frame> frame) {
    // 创建ImageSource实例
    OH_ImageSourceNative *source = nullptr;

    auto &buffer = frame->data;
    Image_ErrorCode errCode = OH_ImageSourceNative_CreateFromData(buffer.data(), buffer.size(), &source);
    if (errCode != IMAGE_SUCCESS) {
        KR_LOG_ERROR << "ImageSourceNativeCTest sourceTest OH_ImageSourceNative_CreateFromData failed, errCode: "
                     << errCode;
        return nullptr;
    }

    OH_DecodingOptions *ops = nullptr;
    OH_DecodingOptions_Create(&ops);
    // 设置为AUTO会根据图片资源格式解码，如果图片资源为HDR资源则会解码为HDR的pixelmap。
    OH_DecodingOptions_SetDesiredDynamicRange(ops, IMAGE_DYNAMIC_RANGE_AUTO);
    OH_DecodingOptions_SetPixelFormat(ops, PIXEL_FORMAT_RGBA_8888);
    OH_PixelmapNative *resPixMap = nullptr;
    // ops参数支持传入nullptr, 当不需要设置解码参数时，不用创建
    errCode = OH_ImageSourceNative_CreatePixelmap(source, ops, &resPixMap);
    OH_DecodingOptions_Release(ops);

    if (errCode != IMAGE_SUCCESS) {
        KR_LOG_ERROR << "ImageSourceNativeCTest sourceTest OH_ImageSourceNative_CreatePixelmap failed, errCode: "
                     << errCode;
        return nullptr;
    }
    return resPixMap;
}

static bool PixelmapToBitmapBuffer(OH_PixelmapNative *pixelmap, size_t bufferSize, std::vector<uint8_t> &buffer) {
    buffer.clear();
    buffer.reserve(bufferSize);
    Image_ErrorCode errCode = OH_PixelmapNative_ReadPixels(pixelmap, buffer.data(), &bufferSize);
    if (errCode != IMAGE_SUCCESS) {
        KR_LOG_ERROR << "ImageSourceNativeCTest sourceTest PixelMapToBitmapBuffer failed, errCode: " << errCode;
        buffer.clear();
        return false;
    }
    return true;
}

static OH_PixelmapNative *BitmapBufferToPixelmap(size_t bufferSize, int width, int height,
                                                 std::vector<uint8_t> &buffer) {
    OH_PixelmapNative *resPixMap = nullptr;
    OH_Pixelmap_InitializationOptions *createOpts;
    OH_PixelmapInitializationOptions_Create(&createOpts);
    OH_PixelmapInitializationOptions_SetWidth(createOpts, width);
    OH_PixelmapInitializationOptions_SetHeight(createOpts, height);
    OH_PixelmapInitializationOptions_SetPixelFormat(createOpts, PIXEL_FORMAT_RGBA_8888);
    // OH_PixelmapInitializationOptions_SetAlphaType(createOpts, PIXELMAP_ALPHA_TYPE_);

    Image_ErrorCode errCode = OH_PixelmapNative_CreateEmptyPixelmap(createOpts, &resPixMap);

    if (errCode != IMAGE_SUCCESS) {
        KR_LOG_ERROR << "BitmapBufferToPixelmap sourceTest OH_PixelmapNative_CreateEmptyPixelmap failed, errCode: "
                     << errCode;
        return nullptr;
    }

    errCode = OH_PixelmapNative_WritePixels(resPixMap, buffer.data(), bufferSize);
    if (errCode != IMAGE_SUCCESS) {
        KR_LOG_ERROR << "BitmapBufferToPixelmap BitmapBufferToPixelmap sourceTest OH_PixelmapNative_WritePixels "
                        "failed, errCode: "
                     << errCode;
        return nullptr;
    }

    return resPixMap;
}

APNG::~APNG() {
    std::unique_lock<std::mutex> lock(drawableMutex);
    for (const auto &drawable : animateDrawables) {
        if (drawable && drawable->pixelmap) {
            OH_PixelmapNative_Release(drawable->pixelmap);
        }
        if (drawable && drawable->drawable) {
            OH_ArkUI_DrawableDescriptor_Dispose(drawable->drawable);
        }
        drawable->drawable = nullptr;
        drawable->pixelmap = nullptr;
    }
    animateDrawables.clear();
}

bool APNG::DidAddFrame(std::shared_ptr<Frame> frame, int index) {
    if (frame->width == 0 || frame->height == 0) {
        return false;
    }
    bool didHandled = false;
    // 首个完整帧
    if (frame->width == width && frame->height == height && !hasFirstFullFrame) {
        auto drawable = std::make_shared<APNGDrawable>();
        drawable->pixelmap = CreatePixelMap(frame);
        if (drawable->pixelmap) {
            hasFirstFullFrame = true;
            drawable->drawable = OH_ArkUI_DrawableDescriptor_CreateFromPixelMap(drawable->pixelmap);
            didHandled = true;
            size_t bufferSize = width * height * 4;
            AddDrawable(drawable, index);
            lazyBeforeNextFrameTask = [needApplyPixelmap = drawable->pixelmap, frame, bufferSize, this]() {
                PixelmapToBitmapBuffer(needApplyPixelmap, bufferSize, this->curBitmapBuffer);
                HandlePostFrameDisposeOp(frame);
            };
        }
    } else if (hasFirstFullFrame) {  // 非完整帧依赖合成
        size_t bufferSize = width * height * 4;
        if (this->lazyBeforeNextFrameTask) {
            this->lazyBeforeNextFrameTask();
            this->lazyBeforeNextFrameTask = nullptr;
        }
        if (frame->disposeOp == 2) {  // 保存上一帧到缓冲区
            previousBuffer = curBitmapBuffer;
        }
        HandleFrameBlendOp(frame, index);
        HandlePostFrameDisposeOp(frame);
        didHandled = true;
    }
    return didHandled;
}

void APNG::HandleFrameBlendOp(std::shared_ptr<Frame> frame, int index) {
    // 合成// 根据 blendOp 合成帧
    auto &drawingBuffer = this->curBitmapBuffer;
    auto framePixelmap = CreatePixelMap(frame);
    auto frameBuffer = std::vector<uint8_t>();
    int frameBufferSize = frame->width * frame->height * 4;
    bool frameDecodeSuccess = PixelmapToBitmapBuffer(framePixelmap, frameBufferSize, frameBuffer);
    if (!frameDecodeSuccess) {
        return;
    }
    for (int y = 0; y < frame->height; ++y) {
        for (int x = 0; x < frame->width; ++x) {
            int srcIdx = (y * frame->width + x) * 4;
            int dstIdx = ((y + frame->top) * width + (x + frame->left)) * 4;

            if (frame->blendOp == 0) {  // 覆盖操作
                drawingBuffer[dstIdx + 0] = frameBuffer[srcIdx + 0];
                drawingBuffer[dstIdx + 1] = frameBuffer[srcIdx + 1];
                drawingBuffer[dstIdx + 2] = frameBuffer[srcIdx + 2];
                drawingBuffer[dstIdx + 3] = frameBuffer[srcIdx + 3];
            } else if (frame->blendOp == 1) {
                // 在这里执行混合操作，例如使用 alpha 通道作为权重计算新的颜色值
                uint8_t srcR = frameBuffer[srcIdx + 0];
                uint8_t srcG = frameBuffer[srcIdx + 1];
                uint8_t srcB = frameBuffer[srcIdx + 2];
                uint8_t srcA = frameBuffer[srcIdx + 3];

                uint8_t dstR = drawingBuffer[dstIdx + 0];
                uint8_t dstG = drawingBuffer[dstIdx + 1];
                uint8_t dstB = drawingBuffer[dstIdx + 2];
                uint8_t dstA = drawingBuffer[dstIdx + 3];

                float srcAlpha = srcA / 255.0f;
                float dstAlpha = dstA / 255.0f;
                float outAlpha = srcAlpha + dstAlpha * (1 - srcAlpha);

                if (outAlpha == 0) {
                    drawingBuffer[dstIdx + 0] = 0;
                    drawingBuffer[dstIdx + 1] = 0;
                    drawingBuffer[dstIdx + 2] = 0;
                    drawingBuffer[dstIdx + 3] = 0;
                } else {
                    drawingBuffer[dstIdx + 0] =
                        static_cast<uint8_t>((srcR * srcAlpha + dstR * dstAlpha * (1 - srcAlpha)) / outAlpha);
                    drawingBuffer[dstIdx + 1] =
                        static_cast<uint8_t>((srcG * srcAlpha + dstG * dstAlpha * (1 - srcAlpha)) / outAlpha);
                    drawingBuffer[dstIdx + 2] =
                        static_cast<uint8_t>((srcB * srcAlpha + dstB * dstAlpha * (1 - srcAlpha)) / outAlpha);
                    drawingBuffer[dstIdx + 3] = static_cast<uint8_t>(outAlpha * 255);
                }
            }
        }
    }

    auto pixelmap = BitmapBufferToPixelmap(width * height * 4, width, height, this->curBitmapBuffer);
    if (pixelmap) {
        auto drawable = std::make_shared<APNGDrawable>();
        drawable->pixelmap = pixelmap;
        drawable->drawable = OH_ArkUI_DrawableDescriptor_CreateFromPixelMap(drawable->pixelmap);
        AddDrawable(drawable, index);
    }
}

void APNG::HandlePostFrameDisposeOp(std::shared_ptr<Frame> frame) {
    if (frame->disposeOp == 1) {  // 清理当前区域
        auto &drawingBuffer = this->curBitmapBuffer;
        // 清除当前帧区域
        for (int y = 0; y < frame->height; ++y) {
            for (int x = 0; x < frame->width; ++x) {
                int dstIdx = ((y + frame->top) * width + (x + frame->left)) * 4;
                drawingBuffer[dstIdx + 0] = 0;
                drawingBuffer[dstIdx + 1] = 0;
                drawingBuffer[dstIdx + 2] = 0;
                drawingBuffer[dstIdx + 3] = 0;
            }
        }
    } else if (frame->disposeOp == 2) {
        previousBuffer = this->curBitmapBuffer;
    }
}

void APNG::AddDrawable(std::shared_ptr<APNGDrawable> drawable, int curFrameIndex) {
    std::unique_lock<std::mutex> lock(drawableMutex);
    if (curFrameIndex + 1 < this->frames.size()) {
        drawable->isLast = false;
        drawable->nextFrameDelay = this->frames[curFrameIndex + 1]->delay;
    } else {
        drawable->nextFrameDelay = this->frames[this->frames.size() - 1]->delay;
        drawable->isLast = true;
    }
    animateDrawables.push_back(drawable);
}

std::shared_ptr<APNGDrawable> APNG::GetDrawable(int index) {
    std::unique_lock<std::mutex> lock(drawableMutex);
    if (index < animateDrawables.size()) {
        return animateDrawables[index];
    }
    return nullptr;
}
