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

#ifndef CORE_RENDER_OHOS_KRRICHTEXTSHADOW_H
#define CORE_RENDER_OHOS_KRRICHTEXTSHADOW_H

#include <arkui/styled_string.h>
#include <native_drawing/drawing_text_declaration.h>
#include <native_drawing/drawing_text_typography.h>
#include <native_drawing/drawing_text_line.h>
#include <native_drawing/drawing_types.h>
#include <unordered_set>
#include "libohos_render/expand/components/richtext/KRFontAdapterManager.h"
#include "libohos_render/expand/components/richtext/KRParagraph.h"
#include "libohos_render/utils/KRScopedSpinLock.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/export/IKRRenderShadowExport.h"

/**
 * 跨线程安全的 OH_Drawing_Typography 持有句柄。
 *
 * Typography 在 KuiklyUI 的渲染流水线中会被多线程同时持有（context 线程负责
 * 创建/Layout/SpanRect 计算，主线程负责 Paint/SetShadow），并且主线程上的
 * Kotlin 同步回调还可能再触发 context 业务逻辑、形成跨线程交错调用。如果用
 * 裸指针 + 投递 destroy 任务的方式管理，极易出现"release 后还在使用"的悬空
 * 指针问题（例如某条 destroy 任务先于一条尚未消费的 SetShadow 任务执行）。
 *
 * 这里把 OH_Drawing_Typography* 用 std::shared_ptr 包起来：
 *  - 多线程拷贝/释放 shared_ptr 自身是线程安全的（控制块为 atomic）；
 *  - 真正的 OH_Drawing_DestroyTypography 只会在 "最后一个引用消失" 时触发，
 *    保证任何同时持有它的 lambda / 主线程字段在使用期间对象都不会析构；
 *  - 替换 main_thread_typography_ 时，被替换下来的旧 typography 在最后一个
 *    引用归零的瞬间析构，无需依赖任何任务派发顺序。
 */
using KRTypographyHandle = std::shared_ptr<OH_Drawing_Typography>;

inline KRTypographyHandle KRMakeTypographyHandle(OH_Drawing_Typography *raw) {
    if (raw == nullptr) {
        return KRTypographyHandle();
    }
    return KRTypographyHandle(raw, [](OH_Drawing_Typography *p) {
        if (p) {
            OH_Drawing_DestroyTypography(p);
        }
    });
}

class KRFontCollectionWrapper {
public:
    static KRFontCollectionWrapper& GetInstance() {
        static KRFontCollectionWrapper instance;
        return instance;
    }

    // 禁止拷贝和赋值
    KRFontCollectionWrapper(const KRFontCollectionWrapper&) = delete;
    KRFontCollectionWrapper& operator=(const KRFontCollectionWrapper&) = delete;

    /**
     * 获取 FontCollection（用于注册自定义字体和创建 Typography）
     * 使用全局实例（如果可用），否则使用共享实例
     */
    OH_Drawing_FontCollection* GetFontCollection();

    /**
     * 注册自定义字体（如果适用）
     * @param resMgr 资源管理器
     * @param fontFamily 字体名称
     */
    void RegisterCustomFont(NativeResourceManager *resMgr, const std::string &fontFamily);

private:
    KRFontCollectionWrapper();
    ~KRFontCollectionWrapper();

    /**
     * 检查字体是否已注册
     */
    bool IsFontRegistered(const std::string& fontFamily) const;

    /**
     * 标记字体为已注册
     */
    void MarkFontRegistered(const std::string& fontFamily);

    OH_Drawing_FontCollection* fontCollection_ = nullptr;
    bool isGlobalInstance_ = false;  // 标记是否为全局实例（全局实例不需要销毁）
    std::unordered_set<std::string> registered_;
};

class KRRichTextShadow : public IKRRenderShadowExport {
 public:
    KRRichTextShadow() {}
    ~KRRichTextShadow();
    /**
     * 更新 shadow 对象属性时调用
     * @param prop_key 属性名
     * @param prop_value 属性数据
     */
    void SetProp(const std::string &prop_key, const KRAnyValue &prop_value) override;

    /**
     * 调用 shadow 对象方法
     * @param method_name
     * @param params
     * @return
     */
    KRAnyValue Call(const std::string &method_name, const std::string &params) override;

    /**
     * 根据布局约束尺寸计算返回 RenderView 的实际尺寸
     * @param constraint_width
     * @param constraint_height
     * @return
     */
    KRSize CalculateRenderViewSize(double constraint_width, double constraint_height) override;

    /**
     * 完成对某个Span对应TextStyle
     * @param textStyle
     * @param dpi 屏幕密度 即ppi和dp换算比例，如3.0
     */
    virtual void DidBuildTextStyle(OH_Drawing_TextStyle *textStyle, double dpi) {}

    /**
     * 将要SetShadow调用
     * @return
     */
    KRSchedulerTask TaskToMainQueueWhenWillSetShadowToView() override;

    OH_Drawing_Typography *MainThreadTypography() const {
        return main_thread_typography_ ? main_thread_typography_.get() : nullptr;
    }

    /**
     * 拿到一个对当前主线程 typography 的强引用，调用方在使用期间该 typography
     * 不会被 context 线程的 release/replace 释放掉。建议主线程上的所有
     * Layout/Paint/Span 查询都通过这个接口拿到 handle 并保持到使用结束。
     */
    KRTypographyHandle MainThreadTypographyHandle() const {
        return main_thread_typography_;
    }
    const float DrawOffsetY() const {
        return main_thread_drawOffsetY_;
    }
    const float DrawOffsetX() const {
        return main_thread_drawOffsetX_;
    }
    const OH_Drawing_TextAlign TextAlign() const {
        return main_thread_text_align_;
    }
    void ResetTextAlign() {
        main_thread_text_align_ = TEXT_ALIGN_LEFT;
    }

    const KRSize MainMeasureSize() {
        return main_measure_size_;
    }
    
    bool DidExceedMaxLines(){
        return did_exceed_max_lines_;
    }
    
    OH_Drawing_Array *GetTextLines();
    
    /**
     * 替换主线程持有的 typography。被替换下来的旧 typography 会在最后一个
     * 引用消失时自动 destroy（通常发生在本次替换调用栈中，主线程上）。
     */
    void SetMainThreadTypography(KRTypographyHandle typography) {
        if (main_thread_typography_ == typography) {
            return;
        }
        // 赋值会触发旧 shared_ptr 的引用计数 -1。当且仅当没有其他
        // lambda / 字段还持有它时，OH_Drawing_DestroyTypography 才会执行；
        // 否则它会自然延后到最后一个持有者析构时再销毁，从而避免任何
        // "release-after-use" 的窗口。
        main_thread_typography_ = std::move(typography);
        DestroyCachedTextLines();
    }

    std::shared_ptr<KRParagraph> GetParagraph(){
        KRScopedSpinLock lock(&paragraph_lock_);
        return paragraph_;
    }
    
    virtual bool StyledStringEnabled();
 private:
    void DestroyCachedTextLines();
    std::string GetTextContent();
    KRSize CalculateRenderViewSizeWithStyledString(double constraint_width, double constraint_height);

 private:
    KRRenderValue::Map props_;
    KRRenderValue::Array values_;
    OH_Drawing_Array *text_lines_ = nullptr;
    bool did_exceed_max_lines_ = false;
    // 持有 typography 的两个槽位：
    //  - main_thread_typography_:    主线程使用（Paint/SpanIndex 等）；
    //  - context_thread_typography_: context 线程使用（Layout/SpanRect 计算）。
    // 都使用 KRTypographyHandle（shared_ptr）持有，跨线程任何角色都通过
    // 拷贝该 handle 来"延长寿命"，避免任意一端的 reset/replace 释放对方仍
    // 在使用的对象。
    KRTypographyHandle main_thread_typography_;
    KRTypographyHandle context_thread_typography_;
    float context_thread_drawOffsetX_ = 0;
    float context_thread_drawOffsetY_ = 0;
    float main_thread_drawOffsetX_ = 0;
    float main_thread_drawOffsetY_ = 0;
    OH_Drawing_TextAlign context_thread_text_align_ = TEXT_ALIGN_LEFT;
    OH_Drawing_TextAlign main_thread_text_align_ = TEXT_ALIGN_LEFT;

    KRSize context_measure_size_;
    KRSize main_measure_size_;
    std::unordered_map<int, int> placeholder_index_map_;
    std::vector<std::tuple<int, int, int>> span_offsets_;  // span, begin, end
    std::shared_ptr<KRParagraph> paragraph_;
    KRSpinLock paragraph_lock_;
    std::shared_ptr<kuikly::util::KRLinearGradientParser> text_linearGradient_;
    
    void SetParagraph(std::shared_ptr<KRParagraph> paragraph){
        KRScopedSpinLock lock(&paragraph_lock_);
        paragraph_ = paragraph;
    }
    /**
     * 在 context 线程调用：根据当前属性构造一个新的 typography 并赋给
     * context_thread_typography_，返回值依旧返回裸指针（仅供调用栈内立即
     * 使用，生命周期由 context_thread_typography_ 管理）。
     */
    OH_Drawing_Typography *BuildTextTypography(double constraint_width, double constraint_height);

    void ReleaseLastTypography();
    /**
     * 调用获取Span位置方法
     */
    KRAnyValue SpanRect(int spanIndex);

    int SpanIndexAt(float x, float y);

    friend class KRRichTextView;
    friend class KRGradientRichTextShadow;
};

OH_Drawing_TypographyCreate* CreateTypographyHandler(OH_Drawing_TypographyStyle* typoStyle);

#endif  // CORE_RENDER_OHOS_KRRICHTEXTSHADOW_H
