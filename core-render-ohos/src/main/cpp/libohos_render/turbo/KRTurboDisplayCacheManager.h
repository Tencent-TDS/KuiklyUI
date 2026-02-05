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

#ifndef KUIKLY_TURBO_DISPLAY_CACHE_MANAGER_H
#define KUIKLY_TURBO_DISPLAY_CACHE_MANAGER_H

// TurboDisplay缓存功能开关：0禁用，1启用
#define ENABLE_TURBO_DISPLAY_CACHE 1

#include <string>
#include <memory>
#include <mutex>
#include "KRTurboDisplayNode.h"
#include "KRTurboDisplayCacheData.h"

namespace KuiklyOhos {

// TurboDisplay缓存管理器，负责节点树的持久化缓存，提升二次加载性能
class KRTurboDisplayCacheManager {
public:
    // 构造函数，cache_dir为缓存文件存储目录
    explicit KRTurboDisplayCacheManager(const std::string& cache_dir = "");
    ~KRTurboDisplayCacheManager();

    // 读取缓存，获取后内部自动删除原文件
    std::shared_ptr<KRTurboDisplayCacheData> ReadCache(const std::string& cache_key);

    // 写入缓存，异步写入到串行队列
    void WriteCache(std::shared_ptr<KRTurboDisplayNode> node, const std::string& cache_key);

    // 删除指定缓存
    void RemoveCacheWithKey(const std::string& cache_key);

    // 生成缓存键，格式：kuikly_turbo_display_9{MD5}.data
    static std::string CacheKeyWithTurboDisplayKey(const std::string& turbo_display_key, 
                                                    const std::string& page_url);

    // 写入Node的二进制数据，用于回写已序列化的数据
    void CacheWithViewNodeData(const std::vector<uint8_t>& node_data, const std::string& cache_key);

    // 检查缓存文件是否存在
    bool HasNodeWithCacheKey(const std::string& cache_key);
    
    // 读取节点
    std::shared_ptr<KRTurboDisplayCacheData> NodeWithCacheKey(const std::string& cache_key);

    // 删除所有缓存文件，删除后重新创建目录
    void RemoveAllCacheFiles();

private:
    // 递归删除目录
    bool RemoveDirectory(const std::string& dir_path);
    // 缓存ViewNode，在串行队列执行：格式化Tag→序列化→写入文件
    void CacheWithViewNode(std::shared_ptr<KRTurboDisplayNode> view_node, 
                           const std::string& cache_key);

    // 格式化缓存树的Tag，将Tag转为负数(-Tag-2)避免与真实渲染节点冲突
    void FormatTagWithCacheTree(std::shared_ptr<KRTurboDisplayNode> node);
    
    // 写入二进制数据到文件
    void WriteToFile(const std::vector<uint8_t>& node_data, const std::string& cache_key);

    // 写入节点到文件（重载版本）
    void WriteToFile(std::shared_ptr<KRTurboDisplayNode> view_node, const std::string& cache_key);

    // 获取完整文件路径
    std::string GetFilePath(const std::string& cache_key);

    // 确保缓存目录存在
    bool EnsureCacheDirectoryExists();

    // MD5哈希计算
    static std::string MD5(const std::string& input);

    // 打印节点树结构（用于调试）
    static void PrintNodeTree(std::shared_ptr<KRTurboDisplayNode> node, 
                             const std::string& prefix = "[TurboDisplay-Cache]",
                             int max_depth = 10);

private:
    // 递归打印节点树
    static void PrintNodeTreeRecursive(std::shared_ptr<KRTurboDisplayNode> node,
                                      const std::string& prefix,
                                      const std::string& indent,
                                      int current_depth,
                                      int max_depth);

    std::string cache_dir_;  // 缓存文件存储目录
    static std::mutex file_lock_;  // 静态文件锁
};

} // namespace KuiklyOhos

#endif // KUIKLY_TURBO_DISPLAY_CACHE_MANAGER_H
