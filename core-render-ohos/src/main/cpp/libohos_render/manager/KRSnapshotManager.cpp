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

#include "libohos_render/manager/KRSnapshotManager.h"

#include <arkui/drawable_descriptor.h>
#include <arkui/native_interface.h>
#include <arkui/native_node.h>
#include <arkui/native_node_napi.h>
#include <multimedia/image_framework/image_packer_mdk.h>
#include <multimedia/image_framework/image_pixel_map_mdk.h>
#include <unistd.h>

#include "libohos_render/expand/components/view/KRView.h"
#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/foundation/ark_ts.h"
#include "libohos_render/scheduler/KRContextScheduler.h"
#include "libohos_render/utils/KRBase64Util.h"

constexpr static int TIME_TO_REMOVE_SNAPSHOT_DRAWABLE_MS = 100;  // remove the cached drawable after 6 frames
constexpr static int MAX_DELAY_DURATION_MS = 1000;

static void DisposeItem(struct KRSnapshotItem *item) {
    if (item) {
        if (item->drawableDescriptor) {
            OH_ArkUI_DrawableDescriptor_Dispose(item->drawableDescriptor);
            item->drawableDescriptor = nullptr;
        }
        item->uri = "";
    }
}

KRSnapshotManager::~KRSnapshotManager() {
    std::for_each(drawableDescriptorCache_.begin(), drawableDescriptorCache_.end(),
                  [](auto desc) { DisposeItem(&desc.second); });
    drawableDescriptorCache_.clear();
}

void KRSnapshotManager::CacheSnapshot(ArkUI_DrawableDescriptor *descriptor, const std::string &key) {
    auto item = drawableDescriptorCache_.find(key);
    if (item != drawableDescriptorCache_.end()) {
        DisposeItem(&item->second);
    }

    if (descriptor) {
        struct KRSnapshotItem item;
        item.drawableDescriptor = descriptor;
        drawableDescriptorCache_[key] = item;
    } else {
        drawableDescriptorCache_.erase(key);
    }
}

void KRSnapshotManager::UpdateSnapshot(const std::string &uri, const std::string &key) {
    auto item = drawableDescriptorCache_.find(key);
    if (item != drawableDescriptorCache_.end()) {
        if (item->second.drawableDescriptor) {
            OH_ArkUI_DrawableDescriptor_Dispose(item->second.drawableDescriptor);
            item->second.drawableDescriptor = nullptr;
        }
        item->second.uri = uri;
    }
}

void KRSnapshotManager::SetCachedSnapshotToNode(ArkUI_NodeHandle node, const std::string &key) {
    auto item = drawableDescriptorCache_.find(key);
    if (item != drawableDescriptorCache_.end()) {
        if (item->second.uri.length() > 0) {
            // when uri become valid, the cached drawable descriptor must had been disposed
            kuikly::util::SetArkUIImageSrc(node, item->second.uri);
            return;
        }
        if (item->second.drawableDescriptor) {
            // FIXME: using drawable would cause memory leak.
            // Looks lie the internal pixelmap is retained somewhere by the system internally
            kuikly::util::SetArkUIImageSrc(node, item->second.drawableDescriptor);
            return;
        }
    }
}

struct KRSnapshotManager::ResultData KRSnapshotManager::ProcessSnapshotResultWithDataType(
    napi_env env, napi_value pixelMap, const std::string &path, const std::string &pathUri,
    ArkUI_DrawableDescriptor *drawableDescriptorPtr, std::weak_ptr<IKRRenderViewExport> weak_view) {
    struct ResultData resultData;
    NativePixelMap *nativePixelMap = OH_PixelMap_InitNativePixelMap(env, pixelMap);
    OhosPixelMapInfos info;
    OH_PixelMap_GetImageInfo(nativePixelMap, &info);
    struct ImagePacker_Opts_ opts;
    opts.format = "image/png";
    opts.quality = 80;

    size_t size = info.width * info.height * 4;
    uint8_t *outData = reinterpret_cast<uint8_t *>(malloc(size));

    napi_value packer;
    OH_ImagePacker_Create(env, &packer);
    ImagePacker_Native *imagePacker = OH_ImagePacker_InitNative(env, packer);
    int err = OH_ImagePacker_PackToData(imagePacker, pixelMap, &opts, outData, &size);
    OH_ImagePacker_Release(imagePacker);
    if (err == 0) {
        std::string_view imageData((const char *)outData, size);
        std::string base64Data = KRBase64Util::Encode(imageData);
        std::stringstream base64ImageSS;
        base64ImageSS << "data:" << opts.format << ";base64," << base64Data;
        resultData.data = base64ImageSS.str();
        resultData.code = 0;
    }
    free(outData);
    return resultData;
}

void KRSnapshotManager::RemoveCachedDrawableAfterDelay(int delayMS, const std::string &key, const std::string &path,
                                                       const std::string &pathUri,
                                                       std::weak_ptr<IKRRenderViewExport> weak_view) {
    KRContextScheduler::ScheduleTask(false, delayMS, [delayMS, weak_view, path, pathUri, key]() {
        if (access(path.c_str(), F_OK) == 0) {
            KRContextScheduler::ScheduleTaskOnMainThread(false, [delayMS, weak_view, pathUri, key] {
                if (auto strong_view = weak_view.lock()) {
                    if (auto strong_root = strong_view->GetRootView().lock()) {
                        auto snapshotManager = strong_root->GetSnapshotManager();
                        snapshotManager->UpdateSnapshot(pathUri, key);
                    }
                }
            });
        } else if (delayMS < MAX_DELAY_DURATION_MS) {
            auto strongView = weak_view.lock();
            if (strongView == nullptr) {
                return;
            }
            if (auto root = strongView->GetRootView().lock()) {
                auto snapshotManager = root->GetSnapshotManager();
                snapshotManager->RemoveCachedDrawableAfterDelay(delayMS * 2, key, path, pathUri, weak_view);
            }
        }
    });
}

struct KRSnapshotManager::ResultData KRSnapshotManager::ProcessSnapshotResultWithCacheKeyType(
    napi_env env, napi_value pixelMap, const std::string &path, const std::string &pathUri,
    ArkUI_DrawableDescriptor *drawableDescriptorPtr, std::weak_ptr<IKRRenderViewExport> weak_view) {
    struct ResultData resultData;
    std::stringstream kss;
    kss << "data:image_Md5_Pixelmap" << drawableDescriptorPtr;
    std::string key = kss.str();
    if (auto strong_view = weak_view.lock()) {
        if (auto strong_root = strong_view->GetRootView().lock()) {
            auto snapshotManager = strong_root->GetSnapshotManager();
            snapshotManager->CacheSnapshot(drawableDescriptorPtr, key);
            // users would typically use the result immediately,
            // keep the drawable for a while, and remove it after the disk copy is ready
            snapshotManager->RemoveCachedDrawableAfterDelay(TIME_TO_REMOVE_SNAPSHOT_DRAWABLE_MS, key, path, pathUri,
                                                            weak_view);
        }
    }
    resultData.data = key;
    resultData.code = 0;

    return resultData;
}

struct KRSnapshotManager::ResultData KRSnapshotManager::ProcessSnapshotResultWithFileType(
    napi_env env, napi_value pixelMap, const std::string &path, const std::string &pathUri,
    ArkUI_DrawableDescriptor *drawableDescriptorPtr, std::weak_ptr<IKRRenderViewExport> weak_view) {
    struct ResultData resultData;
    resultData.code = 0;
    if (resultData.code == 0) {
        resultData.data = pathUri;
    } else {
        resultData.message = "ERROR: FAILED TO SAVE SNAPSHOT";
    }

    return resultData;
}

void KRSnapshotManager::TakeSnapshot(const std::string &instance_id, const std::string &method_name,
                                     const std::string &nodeId, const KRAnyValue &params, const KRRenderCallback &cb,
                                     std::weak_ptr<IKRRenderViewExport> weak_view) {
    KRContextScheduler::ScheduleTaskOnMainThread(false, [instance_id, method_name, nodeId, params, cb, weak_view] {
        auto module_name = NewKRRenderValue("KRSnapshotModule");
        KRRenderValueMap valueMap;
        valueMap["node_id"] = NewKRRenderValue(nodeId);
        valueMap["params"] = params;
        KRRenderCallback callback = cb;
        KRRenderCallback toImageCb = [params, callback, weak_view](KRAnyValue result) {
            bool isNapiValue = result->isNapiValue();
            auto strongView = weak_view.lock();
            if (isNapiValue && strongView) {
                auto paramsMap = params->toMap();
                std::string type = paramsMap["type"]->toString();
                NapiValue napiValue = result->toNapiValue();
                auto env = napiValue.env;
                ArkTS arkTs(napiValue.env);

                napi_value snapshotData = arkTs.GetArrayElement(napiValue.value, 0);
                napi_value drawableDescriptor = arkTs.GetObjectProperty(snapshotData, "drawableDescriptor");
                KRSnapshotManager::ResultData resultData;

                if (arkTs.IsNull(drawableDescriptor) || arkTs.IsUndefined(drawableDescriptor)) {
                    resultData.data = arkTs.GetString(arkTs.GetObjectProperty(snapshotData, "message"));
                    resultData.code = -1;
                } else {
                    napi_value pixelMap = arkTs.GetObjectProperty(snapshotData, "pixelMap");

                    ArkUI_DrawableDescriptor *drawableDescriptorPtr = nullptr;
                    OH_ArkUI_GetDrawableDescriptorFromNapiValue(env, drawableDescriptor, &drawableDescriptorPtr);
                    std::string pathStr;
                    std::string pathURI;
                    if (auto root = strongView->GetRootView().lock()) {
                        auto snapshotManager = root->GetSnapshotManager();
                        if (type == "dataUri") {
                            resultData = snapshotManager->ProcessSnapshotResultWithDataType(
                                env, pixelMap, "", "", drawableDescriptorPtr, weak_view);
                        } else {
                            napi_value path = arkTs.GetObjectProperty(snapshotData, "path");
                            pathStr = arkTs.GetString(path);
                            napi_value uri = arkTs.GetObjectProperty(snapshotData, "pathURI");
                            pathURI = arkTs.GetString(uri);
                            if (type == "cacheKey") {
                                resultData = snapshotManager->ProcessSnapshotResultWithCacheKeyType(
                                    env, pixelMap, pathStr, pathURI, drawableDescriptorPtr, weak_view);
                            } else if (type == "file") {
                                resultData = snapshotManager->ProcessSnapshotResultWithFileType(
                                    env, pixelMap, pathStr, pathURI, drawableDescriptorPtr, weak_view);
                            }
                        }
                    }
                }
                KRRenderValue::Map resultMap;
                resultMap["code"] = std::make_shared<KRRenderValue>(resultData.code);
                if (resultData.code == 0) {
                    resultMap["data"] = std::make_shared<KRRenderValue>(resultData.data);
                } else {
                    resultMap["message"] = std::make_shared<KRRenderValue>(resultData.message);
                }
                callback(std::make_shared<KRRenderValue>(resultMap));
            } else {
                KRRenderValue::Map resultMap;
                resultMap["code"] = std::make_shared<KRRenderValue>(-1);
                resultMap["message"] = std::make_shared<KRRenderValue>("invalid result from arkts");
                callback(std::make_shared<KRRenderValue>(resultMap));
            }
        };
        KRArkTSManager::GetInstance().CallArkTSMethod(
            instance_id, KRNativeCallArkTSMethod::CallModuleMethod, module_name, NewKRRenderValue(method_name),
            NewKRRenderValue(valueMap), nullptr, nullptr, toImageCb, false, nullptr, true);
    });
}
