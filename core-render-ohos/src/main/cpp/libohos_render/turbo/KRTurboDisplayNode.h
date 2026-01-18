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

#ifndef KUIKLY_TURBO_DISPLAY_NODE_H
#define KUIKLY_TURBO_DISPLAY_NODE_H

#include <string>
#include <vector>
#include <memory>
#include <optional>
#include <any>
#include "KRTurboDisplayProp.h"
#include "KRTurboDisplayNodeMethod.h"
#include "../export/IKRRenderShadowExport.h"
#include "../foundation/KRCommon.h"  // 引入 KRRenderCallback 定义

namespace KuiklyOhos {

// 前向声明
class KRTurboDisplayShadow;

// TurboDisplay节点树
class KRTurboDisplayNode : public std::enable_shared_from_this<KRTurboDisplayNode> {
public:
    KRTurboDisplayNode(int tag, const std::string& view_name);
    ~KRTurboDisplayNode();

    // 基础 Getter/Setter
    int GetTag() const { return tag_; }
    void SetTag(int tag) { tag_ = tag; }
    
    std::string GetViewName() const { return view_name_; }
    void SetViewName(const std::string& name) { view_name_ = name; }

    std::optional<int> GetParentTag() const { return parent_tag_; }
    void SetParentTag(std::optional<int> parent_tag) { parent_tag_ = parent_tag; }

    const std::vector<std::shared_ptr<KRTurboDisplayNode>>& GetChildren() const { return children_; }
    void SetChildren(const std::vector<std::shared_ptr<KRTurboDisplayNode>>& children) { children_ = children; }
    
    const std::vector<std::shared_ptr<KRTurboDisplayProp>>& GetProps() const { return props_; }
    const std::vector<std::shared_ptr<KRTurboDisplayNodeMethod>>& GetCallMethods() const { return call_methods_; }

    // addViewMethodDisable - 是否禁用ViewMethod添加，用于控制只保存一帧
    bool GetAddViewMethodDisable() const { return add_view_method_disable_; }
    void SetAddViewMethodDisable(bool disable) { add_view_method_disable_ = disable; }
    
    // 计算属性
    
    // renderShadow - 真实shadow对象（内存字段，不参与序列化）
    std::shared_ptr<IKRRenderShadowExport> GetRenderShadow() const { return render_shadow_; }
    void SetRenderShadow(std::shared_ptr<IKRRenderShadowExport> shadow) { render_shadow_ = shadow; }
    
    // scrollIndex - 滚动索引（从props中提取）
    int GetScrollIndex() const;
    
    // renderFrame - 渲染帧（从props中提取PROP_TYPE_FRAME）
    struct RenderFrame {
        float x, y, width, height;
        RenderFrame() : x(0), y(0), width(0), height(0) {}
        RenderFrame(float x_, float y_, float w_, float h_) : x(x_), y(y_), width(w_), height(h_) {}
    };
    RenderFrame GetRenderFrame() const;

    // 树操作
    void InsertSubNode(std::shared_ptr<KRTurboDisplayNode> sub_node, int index);
    void RemoveFromParentNode(std::shared_ptr<KRTurboDisplayNode> parent_node);
    
    // hasChild/HadChild - 判断是否有子节点
    bool HadChild() const { return !children_.empty(); }
    bool HasChild() const { return !children_.empty(); }

    // 属性管理
    
    // setProp - 设置属性（自动判断类型）
    void SetProp(const std::string& prop_key, const std::any& prop_value);
    
    // setProp - 设置属性（指定类型）
    void SetProp(const std::string& prop_key, const std::any& prop_value, KRTurboDisplayPropType prop_type);
    
    // setFrame - 设置Frame属性
    void SetFrame(float x, float y, float width, float height);
    
    // setShadow - 设置Shadow属性
    void SetShadow(std::shared_ptr<KRTurboDisplayShadow> shadow);
    
    // getProp/propWithKey - 获取指定key的属性
    std::shared_ptr<KRTurboDisplayProp> GetProp(const std::string& key);

    // 方法管理
    
    // addViewMethod - 添加View方法调用
    void AddViewMethod(const std::string& method, const std::string& params, const KRRenderCallback& callback = nullptr);
    
    // addModuleMethod - 添加Module方法调用
    void AddModuleMethod(const std::string& module_name, const std::string& method, const std::string& params, const KRRenderCallback& callback = nullptr);

    // 深拷贝
    std::shared_ptr<KRTurboDisplayNode> DeepCopy() const;

    // 序列化/反序列化
    std::vector<uint8_t> ToByteArray() const;
    static std::shared_ptr<KRTurboDisplayNode> CreateFromByteArray(const std::vector<uint8_t>& data);

    // 常量定义
    static const std::string SCROLL_INDEX;   // "scrollIndex"
    static const std::string SHADOW_KEY;     // "shadow"
    static const std::string FRAME_KEY;      // "frame"
    static const std::string INSERT_KEY;     // "insert"

private:
    void AddNodeMethod(KRTurboDisplayMethodType type, const std::string& name, 
                      const std::string& method, const std::string& params, const KRRenderCallback& callback = nullptr);

    // 序列化辅助函数
    static void WriteInt(std::vector<uint8_t>& bytes, int value);
    static void WriteString(std::vector<uint8_t>& bytes, const std::string& str);
    static void WriteBool(std::vector<uint8_t>& bytes, bool value);
    static void WriteByteArray(std::vector<uint8_t>& bytes, const std::vector<uint8_t>& data);
    
    static int ReadInt(const std::vector<uint8_t>& data, size_t& offset);
    static std::string ReadString(const std::vector<uint8_t>& data, size_t& offset);
    static bool ReadBool(const std::vector<uint8_t>& data, size_t& offset);
    static std::vector<uint8_t> ReadByteArray(const std::vector<uint8_t>& data, size_t& offset);

    int tag_;
    std::string view_name_;
    std::optional<int> parent_tag_;
    std::vector<std::shared_ptr<KRTurboDisplayNode>> children_;
    std::vector<std::shared_ptr<KRTurboDisplayProp>> props_;
    std::vector<std::shared_ptr<KRTurboDisplayNodeMethod>> call_methods_;
    std::shared_ptr<IKRRenderShadowExport> render_shadow_;  // 真实shadow对象
    bool add_view_method_disable_ = false;
};

} // namespace KuiklyOhos

#endif // KUIKLY_TURBO_DISPLAY_NODE_H
