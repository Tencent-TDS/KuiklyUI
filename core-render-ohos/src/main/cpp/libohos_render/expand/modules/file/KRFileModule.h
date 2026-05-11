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

#pragma once

#include "libohos_render/export/IKRRenderModuleExport.h"

namespace kuikly {
namespace module {

/**
 * 文件读写 Module，提供 App files 目录的文件写入能力。
 * 主要用于 RecompositionProfiler 导出 JSON 报告供 AI 分析。
 *
 * 写入目录：filesDir/KuiklyProfiler/
 * HarmonyOS 文件路径：/data/storage/el2/base/haps/entry/files/KuiklyProfiler/
 * 读取命令：hdc file recv /data/storage/el2/base/haps/entry/files/KuiklyProfiler/<filename> /tmp/
 */
class KRFileModule : public IKRRenderModuleExport {
 public:
    static const char MODULE_NAME[];

    KRAnyValue CallMethod(bool sync, const std::string &method, KRAnyValue params,
                          const KRRenderCallback &callback) override;
    void OnDestroy() override;

 private:
    static const char METHOD_WRITE_FILE[];
    static const char METHOD_APPEND_FILE[];
    static const char METHOD_GET_FILES_DIR[];

    std::string GetProfilerDir();
    void WriteFile(const KRAnyValue &params, const KRRenderCallback &callback);
    void AppendFile(const KRAnyValue &params, const KRRenderCallback &callback);
    void GetFilesDir(const KRRenderCallback &callback);
};

}  // namespace module
}  // namespace kuikly
