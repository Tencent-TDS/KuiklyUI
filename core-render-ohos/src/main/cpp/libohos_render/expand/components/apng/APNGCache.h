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

#ifndef CORE_RENDER_OHOS_APNGCACHE_H
#define CORE_RENDER_OHOS_APNGCACHE_H

#include <arkui/native_type.h>
#include <rawfile/raw_file.h>
#include <rawfile/raw_file_manager.h>
#include <array>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <mutex>
#include <stdexcept>
#include <string>
#include <unordered_map>
#include <vector>
#include "libohos_render/expand/components/apng/ApngParser.h"
#include "libohos_render/expand/modules/log/KRLogModule.h"
#include "libohos_render/foundation/KRRect.h"
#include "libohos_render/foundation/thread/KRGCDQueue.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/KRViewUtil.h"

bool ReadFileToBuffer(const std::string &filePath, std::vector<uint8_t> &buffer) {
    FILE *file = fopen(filePath.c_str(), "rb");
    if (!file) {
        return false;
    }

    fseek(file, 0, SEEK_END);
    std::int32_t fileSize = ftell(file);
    if (fileSize != -1) {
        fseek(file, 0, SEEK_SET);
        buffer.resize(fileSize);
        fread(buffer.data(), 1, fileSize, file);
    }
    fclose(file);

    return true;
}

/**
 * 异步获取 APNG（动画便携式网络图形）文件，并在完成时调用完成回调函数。
 *
 * 该函数从给定的文件路径异步获取 APNG 文件。如果已经有针对相同文件路径的获取请求正在进行中，
 * 则不会启动新的获取，而是将完成回调函数添加到完成列表中，以便在获取完成时调用。这避免了对同一文件的不必要获取。
 *
 * 获取到的 APNG 还将在内存中缓存 1 分钟，以提高后续获取相同文件的性能。
 * 在开始新的获取之前，会检查缓存，如果在缓存中找到相同文件路径的有效 APNG，则返回缓存的 APNG，而不执行获取。
 *
 * 获取 APNG 后，将在主线程上调用完成回调函数并传递获取到的 APNG。如果 APNG 有效（即，它不为 null，它是
 * APNG，并且至少有一帧）， 则将 APNG 传递给完成回调函数。如果 APNG 无效，则将 null 传递给完成回调函数。
 *
 * @param filePath 要获取的 APNG 文件的路径。
 * @param completion 获取完成时要调用的函数。获取到的 APNG 将传递给此函数。
 */

void FetchAPNG(const std::string &filePath, std::function<void(std::shared_ptr<APNG>)> completion) {
    static std::unordered_map<std::string, std::vector<std::function<void(std::shared_ptr<APNG>)>>> pendingRequests;
    static std::unordered_map<std::string, std::shared_ptr<APNG>> apngCache;

    {
        auto it = apngCache.find(filePath);
        if (it != apngCache.end()) {
            // 使用缓存的APNG
            completion(it->second);
            return;
        }
    }

    {
        auto it = pendingRequests.find(filePath);
        if (it != pendingRequests.end()) {
            // 如果已经有一个相同的请求正在进行，将回调函数添加到列表中
            it->second.push_back(completion);
            return;
        } else {
            // 如果没有相同的请求正在进行，创建一个新的回调函数列表
            pendingRequests[filePath] = {completion};
        }
    }
    auto start = std::chrono::steady_clock::now();
    KRGCDQueue::GetInstance().DispatchAsync([filePath, start]() {
        std::vector<uint8_t> buffer;
        bool res = ReadFileToBuffer(filePath, buffer);
        auto end0 = std::chrono::steady_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end0 - start);

        parseAPNG(buffer, [end0, duration, filePath](std::shared_ptr<APNG> apng) {
            auto end1 = std::chrono::steady_clock::now();
            auto duration1 = std::chrono::duration_cast<std::chrono::milliseconds>(end1 - end0);
            bool isValidApng = apng && apng->isAPNG && apng->frames.size();
            KR_LOG_INFO << "ReadFileToBuffer cost time:" << (duration).count() << " parse apng c:" << duration1.count();
            KRMainThread::RunOnMainThread([filePath, apng, isValidApng] {
                auto it = pendingRequests.find(filePath);
                if (it != pendingRequests.end()) {
                    std::vector<std::function<void(std::shared_ptr<APNG>)>> completions = std::move(it->second);
                    pendingRequests.erase(it);

                    if (isValidApng) {
                        apngCache[filePath] = apng;
                        // 设置缓存过期时间为1分钟
                        KRMainThread::RunOnMainThread(
                            [filePath, apng] {
                                KRGCDQueue::GetInstance().DispatchAsync([apng] {
                                    apng->width;  // sub thread release
                                });
                                apngCache.erase(filePath);
                            },
                            10 * 60000);
                    }

                    for (const auto &completion : completions) {
                        if (isValidApng) {
                            // 加载成功
                            completion(apng);
                        } else {
                            // 播放失败
                            completion(nullptr);
                        }
                    }
                }
            });
        });
    });
}

#endif  // CORE_RENDER_OHOS_APNGCACHE_H
