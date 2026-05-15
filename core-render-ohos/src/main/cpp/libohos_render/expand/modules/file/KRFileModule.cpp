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

#include "KRFileModule.h"

#include <fcntl.h>
#include <sys/stat.h>
#include <cstdio>
#include <cstring>
#include <string>
#include <thread>

#include "libohos_render/utils/KRJSONObject.h"

namespace kuikly {
namespace module {

const char KRFileModule::MODULE_NAME[]       = "KRFileModule";
const char KRFileModule::METHOD_WRITE_FILE[] = "writeFile";
const char KRFileModule::METHOD_APPEND_FILE[]  = "appendFile";
const char KRFileModule::METHOD_GET_FILES_DIR[] = "getFilesDir";

// ---------------------------------------------------------------------------
// 工具：确保目录存在
// ---------------------------------------------------------------------------
static void MkdirIfNeeded(const std::string &dir) {
    struct stat st{};
    if (stat(dir.c_str(), &st) != 0) {
        mkdir(dir.c_str(), 0755);
    }
}

// ---------------------------------------------------------------------------
// 工具：构建回调结果 map
// ---------------------------------------------------------------------------
static KRAnyValue MakeResult(const std::string &key, const std::string &value) {
    KRRenderValueMap result;
    result[key] = KRRenderValue::Make(value);
    return KRRenderValue::Make(result);
}

// ---------------------------------------------------------------------------
// 获取 profiler 写入目录（filesDir/KuiklyProfiler/）
// ---------------------------------------------------------------------------
std::string KRFileModule::GetProfilerDir() {
    auto root = GetRootView().lock();
    if (!root) {
        return "";
    }
    const std::string filesDir = root->GetContext()->Config()->GetFilesDir();
    if (filesDir.empty()) {
        return "";
    }
    std::string profilerDir = filesDir + "/KuiklyProfiler";
    MkdirIfNeeded(profilerDir);
    return profilerDir;
}

// ---------------------------------------------------------------------------
// writeFile：覆盖写，在后台线程执行
// ---------------------------------------------------------------------------
void KRFileModule::WriteFile(const KRAnyValue &params, const KRRenderCallback &callback) {
    auto jsonObj = util::JSONObject::Parse(params->toString());
    const std::string filename = jsonObj->GetString("filename");
    const std::string content  = jsonObj->GetString("content");

    if (filename.empty() || content.empty()) {
        if (callback) callback(MakeResult("error", "missing filename or content"));
        return;
    }

    const std::string dir = GetProfilerDir();
    if (dir.empty()) {
        if (callback) callback(MakeResult("error", "context unavailable"));
        return;
    }

    const std::string filePath = dir + "/" + filename;

    std::thread([filePath, content, callback]() {
        FILE *fp = fopen(filePath.c_str(), "w");
        if (!fp) {
            if (callback) callback(MakeResult("error", "fopen failed"));
            return;
        }
        fwrite(content.c_str(), 1, content.size(), fp);
        fclose(fp);
        if (callback) callback(MakeResult("path", filePath));
    }).detach();
}

// ---------------------------------------------------------------------------
// appendFile：追加写，末尾加换行（适合 JSONL），在后台线程执行
// ---------------------------------------------------------------------------
void KRFileModule::AppendFile(const KRAnyValue &params, const KRRenderCallback &callback) {
    auto jsonObj = util::JSONObject::Parse(params->toString());
    const std::string filename = jsonObj->GetString("filename");
    const std::string content  = jsonObj->GetString("content");

    if (filename.empty() || content.empty()) {
        if (callback) callback(MakeResult("error", "missing filename or content"));
        return;
    }

    const std::string dir = GetProfilerDir();
    if (dir.empty()) {
        if (callback) callback(MakeResult("error", "context unavailable"));
        return;
    }

    const std::string filePath = dir + "/" + filename;

    std::thread([filePath, content, callback]() {
        FILE *fp = fopen(filePath.c_str(), "a");
        if (!fp) {
            if (callback) callback(MakeResult("error", "fopen failed"));
            return;
        }
        // 追加内容 + 换行，适合 JSONL 格式
        fwrite(content.c_str(), 1, content.size(), fp);
        fwrite("\n", 1, 1, fp);
        fclose(fp);
        if (callback) callback(MakeResult("path", filePath));
    }).detach();
}

// ---------------------------------------------------------------------------
// getFilesDir：返回 profiler 目录路径
// ---------------------------------------------------------------------------
void KRFileModule::GetFilesDir(const KRRenderCallback &callback) {
    if (!callback) return;
    const std::string dir = GetProfilerDir();
    if (dir.empty()) {
        callback(MakeResult("error", "context unavailable"));
    } else {
        callback(MakeResult("path", dir));
    }
}

// ---------------------------------------------------------------------------
// CallMethod 分发
// ---------------------------------------------------------------------------
KRAnyValue KRFileModule::CallMethod(bool sync, const std::string &method,
                                    KRAnyValue params,
                                    const KRRenderCallback &callback) {
    if (method == METHOD_WRITE_FILE) {
        WriteFile(params, callback);
    } else if (method == METHOD_APPEND_FILE) {
        AppendFile(params, callback);
    } else if (method == METHOD_GET_FILES_DIR) {
        GetFilesDir(callback);
    }
    return KREmptyValue();
}

void KRFileModule::OnDestroy() {}

}  // namespace module
}  // namespace kuikly
