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

#include <native_drawing/drawing_text_declaration.h>
#include <native_drawing/drawing_text_typography.h>
#include <native_drawing/drawing_types.h>
#include <unordered_set>
#include "libohos_render/expand/components/richtext/KRFontAdapterManager.h"
#include "libohos_render/export/IKRRenderShadowExport.h"

struct KRFontCollectionWrapper {
    KRFontCollectionWrapper();
    ~KRFontCollectionWrapper();
    OH_Drawing_FontCollection *fontCollection;
    std::unordered_set<std::string> registered;
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

    void SetMainThreadTypography(OH_Drawing_Typography *typography) {
        main_thread_typography_ = typography;
    }

 private:
    std::string GetTextContent();

 private:
    KRRenderValue::Map props_;
    KRRenderValue::Array values_;
    OH_Drawing_Typography *main_thread_typography_ = nullptr;
    OH_Drawing_Typography *context_thread_typography_ = nullptr;
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
    std::shared_ptr<struct KRFontCollectionWrapper> font_collection_wrapper_;

    OH_Drawing_Typography *BuildTextTypography(double constraint_width, double constraint_height);

    void ReleaseLastTypography();
    /**
     * 调用获取Span位置方法
     */
    KRAnyValue SpanRect(int spanIndex);

    int SpanIndexAt(float x, float y);

    friend class KRRichTextView;
};

void SetCustomFontIfApplicable(NativeResourceManager *resMgr, std::shared_ptr<struct KRFontCollectionWrapper> wrapper,
                               const std::string &fontFamily,
                               const std::unordered_map<std::string, KRFontAdapter> &fontAdapters);

#endif  // CORE_RENDER_OHOS_KRRICHTEXTSHADOW_H
