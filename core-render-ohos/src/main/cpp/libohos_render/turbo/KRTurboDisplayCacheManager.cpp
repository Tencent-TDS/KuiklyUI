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

#include "KRTurboDisplayCacheManager.h"
#include <thread>
#include <sstream>
#include <iomanip>
#include <fstream>
#include <sys/stat.h>
#include <unistd.h>
#include <dirent.h>
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/foundation/thread/KRThread.h"

namespace KuiklyOhos {

std::mutex KRTurboDisplayCacheManager::file_lock_;

// 串行队列

static KRThread* GetCacheSerialQueue() {
    static KRThread* gCacheQueue = new KRThread("TurboCache");
    return gCacheQueue;
}

// 构造函数 & 析构函数

KRTurboDisplayCacheManager::KRTurboDisplayCacheManager(const std::string& cache_dir)
    : cache_dir_(cache_dir) {
    KR_LOG_INFO << "[TurboDisplay-CacheManager] 🏗️ 构造函数, cache_dir=" << cache_dir;
    if (!cache_dir_.empty()) {
        if (!EnsureCacheDirectoryExists()) {
            KR_LOG_ERROR << "[TurboDisplay-CacheManager] ❌ Failed to create cache directory";
        } else {
            KR_LOG_INFO << "[TurboDisplay-CacheManager] ✅ 缓存目录创建成功";
        }
    }
}

KRTurboDisplayCacheManager::~KRTurboDisplayCacheManager() = default;

// ReadCache - 从文件读取缓存

std::shared_ptr<KRTurboDisplayCacheData> KRTurboDisplayCacheManager::ReadCache(const std::string& cache_key) {
#if ENABLE_TURBO_DISPLAY_CACHE
    KR_LOG_INFO << "[TurboDisplay-CacheManager] 📖 ReadCache 开始, key=" << cache_key;
    if (cache_dir_.empty()) {
        KR_LOG_ERROR << "[TurboDisplay-CacheManager] ❌ Cache directory not set";
        return nullptr;
    }
    auto result = NodeWithCacheKey(cache_key);
    if (result) {
        KR_LOG_INFO << "[TurboDisplay-CacheManager] ✅ ReadCache 成功，缓存数据大小=" 
                    << result->GetTurboDisplayNodeData().size() << " bytes";
    } else {
        KR_LOG_INFO << "[TurboDisplay-CacheManager] ⚠️ ReadCache 无缓存数据";
    }
    return result;
#else
    KR_LOG_INFO << "[TurboDisplay-CacheManager] ⚠️ ENABLE_TURBO_DISPLAY_CACHE 未启用";
    return nullptr;
#endif
}

// WriteCache - 写入缓存到文件

void KRTurboDisplayCacheManager::WriteCache(std::shared_ptr<KRTurboDisplayNode> node, const std::string& cache_key) {
#if ENABLE_TURBO_DISPLAY_CACHE
    KR_LOG_INFO << "[TurboDisplay-CacheManager] 📝 WriteCache 开始, key=" << cache_key;
    if (cache_dir_.empty()) {
        KR_LOG_ERROR << "[TurboDisplay-CacheManager] ❌ Cache directory not set";
        return;
    }
    if (!node) {
        KR_LOG_ERROR << "[TurboDisplay-CacheManager] ❌ Node is null";
        return;
    }
    int children_count = node->HadChild() ? node->GetChildren().size() : 0;
    KR_LOG_INFO << "[TurboDisplay-CacheManager] 📊 待写入节点: viewName=" << node->GetViewName() 
                << ", tag=" << node->GetTag() << ", 子节点数=" << children_count;
    CacheWithViewNode(node, cache_key);
#else
    KR_LOG_INFO << "[TurboDisplay-CacheManager] ⚠️ ENABLE_TURBO_DISPLAY_CACHE 未启用，跳过写入";
#endif
}

// RemoveCacheWithKey - 删除缓存文件

void KRTurboDisplayCacheManager::RemoveCacheWithKey(const std::string& cache_key) {
#if ENABLE_TURBO_DISPLAY_CACHE
    file_lock_.lock();
    try {
        std::string file_path = GetFilePath(cache_key);
        if (access(file_path.c_str(), F_OK) == 0) {
            remove(file_path.c_str());
        }
    } catch (...) {
        KR_LOG_ERROR << "[TurboDisplay-Cache] Exception in RemoveCacheWithKey";
    }
    file_lock_.unlock();
#endif
}

// CacheWithViewNode - 异步写入节点

void KRTurboDisplayCacheManager::CacheWithViewNode(std::shared_ptr<KRTurboDisplayNode> view_node, 
                                                    const std::string& cache_key) {
#if ENABLE_TURBO_DISPLAY_CACHE
    // 使用串行队列执行缓存写入
    GetCacheSerialQueue()->DispatchAsync([this, view_node, cache_key]() {
        file_lock_.lock();
        try {
            FormatTagWithCacheTree(view_node);
            WriteToFile(view_node, cache_key);
        } catch (const std::exception& e) {
            KR_LOG_ERROR << "[TurboDisplay-Cache] Exception in cache write: " << e.what();
        } catch (...) {
            KR_LOG_ERROR << "[TurboDisplay-Cache] Unknown exception in cache write";
        }
        file_lock_.unlock();
    });
#endif
}

// CacheWithViewNodeData - 异步写入二进制数据

void KRTurboDisplayCacheManager::CacheWithViewNodeData(const std::vector<uint8_t>& node_data, 
                                                       const std::string& cache_key) {
#if ENABLE_TURBO_DISPLAY_CACHE
    if (node_data.empty()) {
        return;
    }
    
    // 使用串行队列执行缓存写入
    GetCacheSerialQueue()->DispatchAsync([this, node_data, cache_key]() {
        file_lock_.lock();
        try {
            WriteToFile(node_data, cache_key);
        } catch (...) {
            KR_LOG_ERROR << "[TurboDisplay-Cache] Exception in cache data write";
        }
        file_lock_.unlock();
    });
#endif
}

// NodeWithCacheKey - 从文件读取并反序列化

std::shared_ptr<KRTurboDisplayCacheData> KRTurboDisplayCacheManager::NodeWithCacheKey(
    const std::string& cache_key) {
#if ENABLE_TURBO_DISPLAY_CACHE
    std::shared_ptr<KRTurboDisplayCacheData> cache_data = nullptr;
    
    file_lock_.lock();
    try {
        std::string file_path = GetFilePath(cache_key);
        KR_LOG_INFO << "[TurboDisplay-CacheManager] 📂 检查缓存文件: " << file_path;
        
        // 检查文件是否存在
        if (access(file_path.c_str(), F_OK) != 0) {
            KR_LOG_INFO << "[TurboDisplay-CacheManager] ⚠️ 缓存文件不存在";
            file_lock_.unlock();
            return nullptr;
        }
        
        KR_LOG_INFO << "[TurboDisplay-CacheManager] 📖 缓存文件存在，开始读取";
        
        // 读取文件内容
        std::ifstream file(file_path, std::ios::binary | std::ios::ate);
        if (!file.is_open()) {
            KR_LOG_ERROR << "[TurboDisplay-CacheManager] ❌ Failed to open cache file";
            file_lock_.unlock();
            return nullptr;
        }
        
        std::streamsize file_size = file.tellg();
        file.seekg(0, std::ios::beg);
        KR_LOG_INFO << "[TurboDisplay-CacheManager] 📊 文件大小: " << file_size << " bytes";
        
        std::vector<uint8_t> data(file_size);
        if (!file.read(reinterpret_cast<char*>(data.data()), file_size)) {
            KR_LOG_ERROR << "[TurboDisplay-CacheManager] ❌ Failed to read file content";
            file.close();
            file_lock_.unlock();
            return nullptr;
        }
        file.close();
        
        KR_LOG_INFO << "[TurboDisplay-CacheManager] 🔄 开始反序列化节点树";
        // 反序列化为 Node
        auto node = KRTurboDisplayNode::CreateFromByteArray(data);
        if (!node) {
            KR_LOG_ERROR << "[TurboDisplay-CacheManager] ❌ Failed to deserialize node from binary data";
            file_lock_.unlock();
            return nullptr;
        }
        
        int children_count = node->HadChild() ? node->GetChildren().size() : 0;
        KR_LOG_INFO << "[TurboDisplay-CacheManager] ✅ 反序列化成功: viewName=" << node->GetViewName()
                    << ", tag=" << node->GetTag() << ", 子节点数=" << children_count;
        
        cache_data = std::make_shared<KRTurboDisplayCacheData>(node, data);
        
        // 删除原文件（避免缓存问题时一直失败）
        remove(file_path.c_str());
        KR_LOG_INFO << "[TurboDisplay-CacheManager] 🗑️ 已删除原缓存文件（读取后删除策略）";
        
    } catch (...) {
        KR_LOG_ERROR << "[TurboDisplay-CacheManager] ❌ Exception during cache read";
        cache_data = nullptr;
    }
    file_lock_.unlock();
    
    return cache_data;
#else
    return nullptr;
#endif
}

// FormatTagWithCacheTree - 格式化Tag

void KRTurboDisplayCacheManager::FormatTagWithCacheTree(std::shared_ptr<KRTurboDisplayNode> node) {
    if (node == nullptr) return;
    
    // ROOT_VIEW_TAG = -1，不修改
    int tag = node->GetTag();
    if (tag != -1 && tag >= 0) {
        node->SetTag(-(tag + 2));
    }
    
    // 修改 parent_tag
    if (node->GetParentTag().has_value()) {
        int parent_tag = node->GetParentTag().value();
        if (parent_tag != -1 && parent_tag >= 0) {
            node->SetParentTag(-(parent_tag + 2));
        }
    }
    
    // 递归修改子节点
    const auto& children = node->GetChildren();
    for (const auto& child : children) {
        FormatTagWithCacheTree(child);
    }
}

// WriteToFile - 写入二进制数据到文件

void KRTurboDisplayCacheManager::WriteToFile(const std::vector<uint8_t>& node_data, const std::string& cache_key) {
#if ENABLE_TURBO_DISPLAY_CACHE
    try {
        std::string file_path = GetFilePath(cache_key);
        
        // 先删除已存在的文件，确保干净写入
        if (access(file_path.c_str(), F_OK) == 0) {
            remove(file_path.c_str());
        }
        
        // 写入文件
        std::ofstream file(file_path, std::ios::binary | std::ios::trunc);
        if (!file.is_open()) {
            KR_LOG_ERROR << "[TurboDisplay-Cache] Failed to open file for writing: " << file_path;
            return;
        }
        
        file.write(reinterpret_cast<const char*>(node_data.data()), node_data.size());
        file.close();
        
    } catch (const std::exception& e) {
        KR_LOG_ERROR << "[TurboDisplay-Cache] Exception in WriteToFile: " << e.what();
    } catch (...) {
        KR_LOG_ERROR << "[TurboDisplay-Cache] Unknown exception in WriteToFile";
    }
#endif
}

void KRTurboDisplayCacheManager::WriteToFile(std::shared_ptr<KRTurboDisplayNode> view_node, const std::string& cache_key) {
#if ENABLE_TURBO_DISPLAY_CACHE
    try {
        // 序列化为二进制数据
        std::vector<uint8_t> node_data = view_node->ToByteArray();
        // 写入文件
        WriteToFile(node_data, cache_key);
    } catch (...) {
        KR_LOG_ERROR << "[TurboDisplay-Cache] Exception in WriteToFile (node serialization)";
    }
#endif
}

// HasNodeWithCacheKey - 检查缓存文件是否存在

bool KRTurboDisplayCacheManager::HasNodeWithCacheKey(const std::string& cache_key) {
#if ENABLE_TURBO_DISPLAY_CACHE
    file_lock_.lock();
    try {
        std::string file_path = GetFilePath(cache_key);
        bool exists = (access(file_path.c_str(), F_OK) == 0);
        file_lock_.unlock();
        return exists;
    } catch (...) {
        KR_LOG_ERROR << "[TurboDisplay-Cache] Exception in HasNodeWithCacheKey";
        file_lock_.unlock();
        return false;
    }
#else
    return false;
#endif
}

// GetFilePath - 获取完整文件路径

std::string KRTurboDisplayCacheManager::GetFilePath(const std::string& cache_key) {
    if (cache_dir_.empty()) {
        return cache_key;
    }
    
    // 确保目录路径以 / 结尾
    std::string dir = cache_dir_;
    if (dir.back() != '/') {
        dir += '/';
    }
    
    return dir + cache_key;
}

// EnsureCacheDirectoryExists - 确保缓存目录存在

bool KRTurboDisplayCacheManager::EnsureCacheDirectoryExists() {
    if (cache_dir_.empty()) {
        return false;
    }
    
    // 检查目录是否存在
    struct stat st;
    if (stat(cache_dir_.c_str(), &st) == 0) {
        if (S_ISDIR(st.st_mode)) {
            return true;
        } else {
            KR_LOG_ERROR << "[TurboDisplay-Cache] Path exists but is not a directory: " << cache_dir_;
            return false;
        }
    }
    
    // 创建目录（递归创建中间目录）
    std::string path = cache_dir_;
    std::string::size_type pos = 0;
    
    while ((pos = path.find('/', pos + 1)) != std::string::npos) {
        std::string sub_path = path.substr(0, pos);
        if (mkdir(sub_path.c_str(), 0755) != 0 && errno != EEXIST) {
            KR_LOG_ERROR << "[TurboDisplay-Cache] Failed to create directory: " << sub_path;
            return false;
        }
    }
    
    // 创建最后一级目录
    if (mkdir(cache_dir_.c_str(), 0755) != 0 && errno != EEXIST) {
        KR_LOG_ERROR << "[TurboDisplay-Cache] Failed to create cache directory: " << cache_dir_;
        return false;
    }
    
    return true;
}

// CacheKeyWithTurboDisplayKey - 生成缓存键

std::string KRTurboDisplayCacheManager::CacheKeyWithTurboDisplayKey(
    const std::string& turbo_display_key, 
    const std::string& page_url) {
    
    std::string combined = page_url + "_" + turbo_display_key;
    std::string hash = MD5(combined);
    
    return "kuikly_turbo_display_9" + hash + ".data";
}

// MD5 - MD5哈希计算（简化实现，使用std::hash）

std::string KRTurboDisplayCacheManager::MD5(const std::string& input) {
    std::hash<std::string> hasher;
    size_t hash_value = hasher(input);
    
    std::stringstream ss;
    ss << std::hex << std::setfill('0') << std::setw(16) << hash_value;
    
    return ss.str();
}

// RemoveDirectory - 递归删除目录

bool KRTurboDisplayCacheManager::RemoveDirectory(const std::string& dir_path) {
    DIR* dir = opendir(dir_path.c_str());
    if (!dir) {
        KR_LOG_ERROR << "[TurboDisplay-Cache] Failed to open directory: " << dir_path;
        return false;
    }
    
    struct dirent* entry;
    bool success = true;
    
    while ((entry = readdir(dir)) != nullptr) {
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
            continue;
        }
        
        std::string full_path = dir_path;
        if (full_path.back() != '/') {
            full_path += '/';
        }
        full_path += entry->d_name;
        
        struct stat st;
        if (stat(full_path.c_str(), &st) == 0) {
            if (S_ISDIR(st.st_mode)) {
                if (!RemoveDirectory(full_path)) {
                    success = false;
                }
            } else {
                if (remove(full_path.c_str()) != 0) {
                    KR_LOG_ERROR << "[TurboDisplay-Cache] Failed to remove file: " << full_path;
                    success = false;
                }
            }
        }
    }
    
    closedir(dir);
    
    if (rmdir(dir_path.c_str()) != 0) {
        KR_LOG_ERROR << "[TurboDisplay-Cache] Failed to remove directory: " << dir_path;
        success = false;
    }
    
    return success;
}

// RemoveAllCacheFiles - 删除所有缓存文件

void KRTurboDisplayCacheManager::RemoveAllCacheFiles() {
#if ENABLE_TURBO_DISPLAY_CACHE
    std::lock_guard<std::mutex> lock(file_lock_);
    
    try {
        if (cache_dir_.empty()) {
            KR_LOG_ERROR << "[TurboDisplay-Cache] Cache directory not set";
            return;
        }
        
        // 检查目录是否存在
        struct stat st;
        if (stat(cache_dir_.c_str(), &st) != 0) {
            return;
        }
        
        // 删除整个缓存目录
        RemoveDirectory(cache_dir_);
        
        // 重新创建空目录
        EnsureCacheDirectoryExists();
        
    } catch (const std::exception& e) {
        KR_LOG_ERROR << "[TurboDisplay-Cache] Exception in RemoveAllCacheFiles: " << e.what();
    } catch (...) {
        KR_LOG_ERROR << "[TurboDisplay-Cache] Unknown exception in RemoveAllCacheFiles";
    }
#endif
}

// PrintNodeTree - 打印节点树结构

void KRTurboDisplayCacheManager::PrintNodeTree(std::shared_ptr<KRTurboDisplayNode> node,
                                               const std::string& prefix,
                                               int max_depth) {
    if (!node) {
        return;
    }
    PrintNodeTreeRecursive(node, prefix, "", 0, max_depth);
}

void KRTurboDisplayCacheManager::PrintNodeTreeRecursive(std::shared_ptr<KRTurboDisplayNode> node,
                                                        const std::string& prefix,
                                                        const std::string& indent,
                                                        int current_depth,
                                                        int max_depth) {
    if (!node || current_depth >= max_depth) {
        return;
    }
    
    // 递归打印子节点
    const auto& children = node->GetChildren();
    for (size_t i = 0; i < children.size(); ++i) {
        bool is_last = (i == children.size() - 1);
        std::string child_indent = indent;
        if (current_depth > 0) {
            child_indent += is_last ? "   " : "│  ";
        }
        PrintNodeTreeRecursive(children[i], prefix, child_indent, current_depth + 1, max_depth);
    }
}

} // namespace KuiklyOhos
