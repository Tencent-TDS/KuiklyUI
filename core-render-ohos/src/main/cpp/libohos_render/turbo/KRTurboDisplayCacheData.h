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

#ifndef KUIKLY_TURBO_DISPLAY_CACHE_DATA_H
#define KUIKLY_TURBO_DISPLAY_CACHE_DATA_H

#include <memory>
#include <vector>
#include "KRTurboDisplayNode.h"

namespace KuiklyOhos {

// TurboDisplay缓存数据封装类，保存节点树和二进制数据，支持快速回写和Diff/Patch操作
class KRTurboDisplayCacheData {
public:
    // 构造函数：node为节点树，node_data为原始二进制数据
    KRTurboDisplayCacheData(std::shared_ptr<KRTurboDisplayNode> node,
                            const std::vector<uint8_t>& node_data);
    

    ~KRTurboDisplayCacheData();

    // 获取TurboDisplay节点树
    std::shared_ptr<KRTurboDisplayNode> GetTurboDisplayNode() const { return turbo_display_node_; }
    
    // 获取序列化后的二进制数据
    std::vector<uint8_t> GetTurboDisplayNodeData() const { return turbo_display_node_data_; }

private:
    std::shared_ptr<KRTurboDisplayNode> turbo_display_node_;  // TurboDisplay节点树
    std::vector<uint8_t> turbo_display_node_data_;            // 序列化后的二进制数据
};

} // namespace KuiklyOhos

#endif // KUIKLY_TURBO_DISPLAY_CACHE_DATA_H
