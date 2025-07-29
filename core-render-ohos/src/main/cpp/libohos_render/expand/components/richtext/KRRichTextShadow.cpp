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

#include "libohos_render/expand/components/richtext/KRRichTextShadow.h"

#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_font_collection.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_register_font.h>
#include <native_drawing/drawing_text_declaration.h>
#include <native_drawing/drawing_text_typography.h>

#include <cassert>
#include <codecvt>
#include <unordered_set>

#include "libohos_render/utils/KRConvertUtil.h"
#include "libohos_render/utils/KRStringUtil.h"
#include "libohos_render/utils/KRViewUtil.h"

// utility wrapper to adapt locale-bound facets for wstring/wbuffer convert
template <class Facet> struct deletable_facet : Facet {
    template <class... Args> deletable_facet(Args &&...args) : Facet(std::forward<Args>(args)...) {}
    ~deletable_facet() {}
};

constexpr char kRawFilePrefix[] = "rawfile:";

static bool isRawFilePath(const std::string &src) {
    return src.find(kRawFilePrefix) == 0;
}

KRRichTextShadow::~KRRichTextShadow() {
    if (context_thread_typography_ != nullptr) {
        OH_Drawing_DestroyTypography(context_thread_typography_);
    }
    context_thread_typography_ = nullptr;
}

/**
 * 更新 shadow 对象属性时调用
 * @param prop_key 属性名
 * @param prop_value 属性数据
 */
void KRRichTextShadow::SetProp(const std::string &prop_key, const KRAnyValue &prop_value) {
    if (prop_key == "values") {
        values_ = prop_value->toArray();
        return;
    }
    props_[prop_key] = prop_value;
}

/**
 * 调用 shadow 对象方法
 * @param method_name
 * @param params
 * @return
 */
KRAnyValue KRRichTextShadow::Call(const std::string &method_name, const std::string &params) {
    if (kuikly::util::isEqual(method_name, "spanRect")) {  // 调用获取placeholder span位置方法
        return SpanRect(NewKRRenderValue(params)->toInt());
    }
    return std::make_shared<KRRenderValue>(nullptr);
}

/**
 * 根据布局约束尺寸计算返回 RenderView 的实际尺寸
 * @param constraint_width
 * @param constraint_height
 * @return
 */
KRSize KRRichTextShadow::CalculateRenderViewSize(double constraint_width, double constraint_height) {
    ReleaseLastTypography();
    BuildTextTypography(constraint_width, constraint_height);
    return context_measure_size_;
}

/**
 * 将要SetShadow调用
 * @return
 */
KRSchedulerTask KRRichTextShadow::TaskToMainQueueWhenWillSetShadowToView() {
    auto self = shared_from_this();
    auto typography = context_thread_typography_;
    auto offsetY = context_thread_drawOffsetY_;
    auto offsetX = context_thread_drawOffsetX_;
    auto measure_size = context_measure_size_;
    auto text_align = context_thread_text_align_;
    return [self, typography, offsetY, offsetX, measure_size, text_align] {
        KRRichTextShadow *shadow = reinterpret_cast<KRRichTextShadow *>(self.get());
        shadow->SetMainThreadTypography(typography);
        shadow->main_thread_drawOffsetY_ = offsetY;
        shadow->main_thread_drawOffsetX_ = offsetX;
        shadow->main_thread_text_align_ = text_align;
        shadow->main_measure_size_ = measure_size;
    };
}

static KRAnyValue GetKRValue(const char *key, const KRRenderValue::Map &map0, const KRRenderValue::Map &map1) {
    auto it = map0.find(key);
    if (it != map0.end()) {
        return it->second;
    }
    auto it2 = map1.find(key);
    if (it2 != map1.end()) {
        return it2->second;
    }
    return std::make_shared<KRRenderValue>(nullptr);
}

KRFontCollectionWrapper::KRFontCollectionWrapper() : fontCollection(OH_Drawing_CreateSharedFontCollection()) {
    // blank
}
KRFontCollectionWrapper::~KRFontCollectionWrapper() {
    if (fontCollection) {
        OH_Drawing_DestroyFontCollection(fontCollection);
        fontCollection = nullptr;
    }
}

void SetCustomFontIfApplicable(NativeResourceManager *resMgr, std::shared_ptr<struct KRFontCollectionWrapper> wrapper,
                               const std::string &fontFamily,
                               const std::unordered_map<std::string, KRFontAdapter> &fontAdapters) {
    auto adapter = fontAdapters.find(fontFamily);
    if (adapter != fontAdapters.end() && wrapper->registered.find(fontFamily) == wrapper->registered.end()) {
        char *fontBuffer = nullptr;
        size_t len = 0;
        KRFontDataDeallocator deallocator = nullptr;
        char *fontSrc = adapter->second(fontFamily.c_str(), &fontBuffer, &len, &deallocator);
        if (fontSrc) {
            uint32_t error = 0;
            auto fontStrString = std::string(fontSrc);
            if (isRawFilePath(std::string(fontSrc))) {
                auto newRawPath = fontStrString.substr(strlen(kRawFilePrefix));
                RawFile *rawFile = OH_ResourceManager_OpenRawFile(resMgr, newRawPath.c_str());
                long len = OH_ResourceManager_GetRawFileSize(rawFile);
                std::unique_ptr<uint8_t[]> data = std::make_unique<uint8_t[]>(len);
                int res = OH_ResourceManager_ReadRawFile(rawFile, data.get(), len);
                OH_ResourceManager_CloseRawFile(rawFile);
                error = OH_Drawing_RegisterFontBuffer(wrapper->fontCollection, fontFamily.c_str(), data.get(), len);
            } else {
                error = OH_Drawing_RegisterFont(wrapper->fontCollection, fontFamily.c_str(), fontSrc);
            }
            if (error == 0) {
                wrapper->registered.emplace(fontFamily);
            }

            if (deallocator) {
                deallocator(fontSrc);
            }
        } else if (fontBuffer != nullptr && len > 0) {
            uint32_t error =
                OH_Drawing_RegisterFontBuffer(wrapper->fontCollection, fontFamily.c_str(),
                                              reinterpret_cast<uint8_t *>(fontBuffer), len);
            if (error == 0) {
                wrapper->registered.emplace(fontFamily);
            }
            if (deallocator) {
                deallocator(fontBuffer);
            }
        }
    }
}

std::string KRRichTextShadow::GetTextContent() {
    std::string txt;
    for (auto span : values_) {
        auto spanMap = span->toMap();
        auto text = GetKRValue("value", spanMap, spanMap)->toString();
        if (text.length() == 0) {
            text = GetKRValue("text", spanMap, spanMap)->toString();
        }
        txt.append("<span>" + text + "</span>");
    }
    if (values_.empty() && !props_.empty()) {
        auto text = GetKRValue("value", props_, props_)->toString();
        if (text.length() == 0) {
            text = GetKRValue("text", props_, props_)->toString();
        }
        txt.append("<span>" + text + "</span>");
    }
    return txt;
}

OH_Drawing_Typography *KRRichTextShadow::BuildTextTypography(double constraint_width, double constraint_height) {
    auto rootView = GetRootView().lock();
    if (rootView == nullptr) {
        return nullptr;
    }

    struct RootViewThreadingDispatcher {
        RootViewThreadingDispatcher(std::shared_ptr<IKRRenderView> r) : rootView_(r) {
            // blank
        }
        ~RootViewThreadingDispatcher() {
            if (auto theRootView = rootView_) {
                //
                // We need to dispatch it back to main thread to avoid destructing it on context thread,
                // in case `rootView` variable is the last holding onto the root render view.
                //
                KRMainThread::RunOnMainThread([theRootView] { theRootView.get(); });
            }
        }

        std::shared_ptr<IKRRenderView> rootView_;
    } rootViewThreadingDispatcher(rootView);

    float fontSizeScale = rootView->GetContext()->Config()->GetFontSizeScale();
    float fontWeightScale = rootView->GetContext()->Config()->GetFontWeightScale();

    span_offsets_.clear();
    placeholder_index_map_.clear();
    KRRenderValue::Array spans = values_;
    if (spans.empty()) {
        spans.push_back(std::make_shared<KRRenderValue>(props_));
    }

    auto numberOfLines = GetKRValue("numberOfLines", props_, props_)->toInt();
    const std::string lineBreakModeStr = GetKRValue("lineBreakMode", props_, props_)->toString();
    auto lineBreakMode = kuikly::util::ConvertToTextBreakMode(lineBreakModeStr);
    if (numberOfLines == 0) {
        numberOfLines = 10000;
    }
    auto self = shared_from_this();
    double dpi = KRConfig::GetDpi();
    OH_Drawing_TypographyStyle *typoStyle = nullptr;
    OH_Drawing_TypographyCreate *handler = nullptr;
    bool isFirst = true;
    int spanIndex = 0;
    int placeholder_count = 0;
    OH_Drawing_TextAlign text_align = TEXT_ALIGN_LEFT;
    int charOffset = 0;
    font_collection_wrapper_ = std::make_shared<KRFontCollectionWrapper>();
    auto fontAdapters = KRFontAdapterManager::GetInstance()->AllAdapters();
    for (auto span : spans) {
        auto spanMap = span->toMap();
        auto fontSize = (GetKRValue("fontSize", spanMap, props_)->toFloat() ?: 15.0) * dpi * fontSizeScale;
        auto text = GetKRValue("value", spanMap, spanMap)->toString();
        if (text.length() == 0) {
            text = GetKRValue("text", spanMap, spanMap)->toString();
        }
        auto fontWeight = kuikly::util::ConvertFontWeight(GetKRValue("fontWeight", spanMap, props_)->toInt() * fontWeightScale);
        auto colorStr = GetKRValue("color", spanMap, props_)->toString();
        auto fontFamily = GetKRValue("fontFamily", spanMap, props_)->toString();
        auto color = colorStr.length() ? kuikly::util::ConvertToHexColor(colorStr) : 0xff000000;                    // 默认黑色
        auto lineHeight = GetKRValue("lineHeight", spanMap, props_)->toFloat() / (fontSize / dpi);    // 字体比例
        auto lineSpacing = GetKRValue("lineSpacing", spanMap, props_)->toFloat() / (fontSize / dpi);  // 行间距比例
        auto textAlign = kuikly::util::ConvertToTextAlign(GetKRValue("textAlign", spanMap, props_)->toString());
        auto textDecoration = kuikly::util::ConvertToTextDecoration(GetKRValue("textDecoration", spanMap, props_)->toString());
        auto fontStyle = kuikly::util::ConvertToFontStyle(GetKRValue("fontStyle", spanMap, props_)->toString());
        auto letterSpacing = GetKRValue("letterSpacing", spanMap, props_)->toDouble();
        auto textShadowStr = GetKRValue("textShadow", spanMap, props_)->toString();
        auto strokeWidth = GetKRValue("strokeWidth", spanMap, props_)->toFloat();
        auto strokeColorStr = GetKRValue("strokeColor", spanMap, props_)->toString();
        auto strokeColor = strokeColorStr.length() ? kuikly::util::ConvertToHexColor(strokeColorStr) : 0xff000000;

        if (typoStyle == nullptr) {
            typoStyle = OH_Drawing_CreateTypographyStyle();
            OH_Drawing_SetTypographyTextMaxLines(typoStyle, numberOfLines);
            // 选择从左到右/左对齐、行数限制排版属性设置到排版样式对象中
            OH_Drawing_SetTypographyTextDirection(typoStyle, TEXT_DIRECTION_LTR);
            OH_Drawing_SetTypographyTextAlign(typoStyle, textAlign);
            text_align = textAlign;
            OH_Drawing_SetTypographyTextEllipsisModal(typoStyle, lineBreakMode);
            const char *ellipsis = "…";
            if (lineBreakModeStr == "clip") {
                ellipsis = "";
            }
            OH_Drawing_SetTypographyTextEllipsis(typoStyle, ellipsis);

            OH_Drawing_WordBreakType workBreak = WORD_BREAK_TYPE_BREAK_WORD;
            if (numberOfLines == 1) {
                workBreak = WORD_BREAK_TYPE_BREAK_ALL;
            }
            OH_Drawing_SetTypographyTextWordBreakType(typoStyle, workBreak);

            if (lineSpacing) {
                /* Drawing自带的设置SpacingScale的接口段落前后仍有间距
                 * OH_Drawing_SetTypographyTextUseLineStyle(typoStyle, true);
                 * OH_Drawing_SetTypographyTextLineStyleSpacingScale(typoStyle, lineSpacing);
                 * 等待修复，目前使用设置行高+禁用首尾行间距实现，注意同时设置lineHeight和lineSpacing首尾间距也会失效
                 */
                OH_Drawing_TypographyTextSetHeightBehavior(typoStyle, TEXT_HEIGHT_DISABLE_ALL);
            }

            handler = OH_Drawing_CreateTypographyHandler(typoStyle, font_collection_wrapper_->fontCollection);
        } else {
            isFirst = false;
        }
        auto placeholderWidth = GetKRValue("placeholderWidth", spanMap, spanMap)->toDouble();
        // 创建文本样式对象txtStyle
        OH_Drawing_TextStyle *txtStyle = OH_Drawing_CreateTextStyle();
        OH_Drawing_Pen *textForegroundPen = nullptr;
        OH_Drawing_Brush *textForegroundBrush = OH_Drawing_BrushCreate();
        // 设置文字大小、字重等属性设置到文本样式对象中
        OH_Drawing_SetTextStyleColor(txtStyle, color);
        if (textShadowStr.length()) {
            auto textShadow = OH_Drawing_CreateTextShadow();
            kuikly::util::SetTextShadow(textShadow, textShadowStr);
            OH_Drawing_TextStyleAddShadow(txtStyle, textShadow);
            OH_Drawing_DestroyTextShadow(textShadow);
        }
        if (strokeColorStr.length() && strokeWidth > 0) {
            if (textForegroundPen == nullptr) {
                textForegroundPen = OH_Drawing_PenCreate();
                OH_Drawing_PenSetAntiAlias(textForegroundPen, true);
            }
            OH_Drawing_PenSetColor(textForegroundPen, strokeColor);
            OH_Drawing_PenSetWidth(textForegroundPen, strokeWidth);
        }

        if (textForegroundPen) {
            OH_Drawing_SetTextStyleForegroundPen(txtStyle, textForegroundPen);
        }
        if (textForegroundBrush) {
            OH_Drawing_BrushSetColor(textForegroundBrush, color);
            OH_Drawing_SetTextStyleForegroundBrush(txtStyle, textForegroundBrush);
        }
        OH_Drawing_SetTextStyleFontSize(txtStyle, fontSize);
        OH_Drawing_SetTextStyleFontWeight(txtStyle, fontWeight);
        OH_Drawing_SetTextStyleBaseLine(txtStyle, TEXT_BASELINE_ALPHABETIC);
        OH_Drawing_SetTextStyleDecoration(txtStyle, textDecoration);
        OH_Drawing_SetTextStyleFontStyle(txtStyle, fontStyle);
        if (letterSpacing > 0) {
            OH_Drawing_SetTextStyleLetterSpacing(txtStyle, letterSpacing * dpi);
        }
        if (lineSpacing > 0) {
            OH_Drawing_SetTextStyleFontHeight(txtStyle, lineSpacing + std::max(lineHeight, 1.0));
        } else if (lineHeight > 0) {
            lineHeight = std::max(lineHeight, 1.0);
            OH_Drawing_SetTextStyleFontHeight(txtStyle, lineHeight);
            context_thread_drawOffsetY_ = (fontSize * lineHeight - fontSize) / 4;  // cai系统绘制存在偏移问题，手动校准
        }
        // fontFamily
        if (!fontFamily.empty()) {
            const char *fontFamilyPtr = fontFamily.c_str();
            const char *fontFamilies[] = {fontFamilyPtr};
            OH_Drawing_SetTextStyleFontFamilies(txtStyle, 1, fontFamilies);
            auto rootView = GetRootView();
            auto rootViewLock = rootView.lock();
            auto nativeResMgr = rootViewLock->GetNativeResourceManager();
            SetCustomFontIfApplicable(nativeResMgr, font_collection_wrapper_, fontFamily, fontAdapters);
        }
        OH_Drawing_SetTextStyleFontStyle(txtStyle, FONT_STYLE_NORMAL);
        OH_Drawing_SetTextStyleLocale(txtStyle, "en");
        // 将文本样式对象加入到handler中
        if (!isFirst) {
            OH_Drawing_TypographyHandlerPopTextStyle(handler);
        }
        DidBuildTextStyle(txtStyle, dpi);
        OH_Drawing_TypographyHandlerPushTextStyle(handler, txtStyle);
        if (placeholderWidth != 0) {  // 添加占位Span
            auto placeholderHeight = GetKRValue("placeholderHeight", spanMap, spanMap)->toDouble();
            OH_Drawing_PlaceholderSpan inlineView = {
                placeholderWidth * dpi,      placeholderHeight * dpi,
                ALIGNMENT_CENTER_OF_ROW_BOX,  // VerticalAlign is 居中
                TEXT_BASELINE_ALPHABETIC,    0,
            };
            OH_Drawing_TypographyHandlerAddPlaceholder(handler, &inlineView);
            placeholder_index_map_[spanIndex] = placeholder_count;
            placeholder_count++;
            charOffset += 1;
        } else {
            OH_Drawing_TypographyHandlerAddText(handler, text.c_str());  // 添加文本

            std::wstring_convert<deletable_facet<std::codecvt<char16_t, char, std::mbstate_t>>, char16_t> conv16;
            std::u16string str16 = conv16.from_bytes(text);
            int codePointCount = str16.size();
            span_offsets_.emplace_back(std::tuple(spanIndex, charOffset, charOffset + codePointCount));
            charOffset += codePointCount;
        }
        OH_Drawing_DestroyTextStyle(txtStyle);
        if (textForegroundPen) {
            OH_Drawing_PenDestroy(textForegroundPen);
            textForegroundPen = nullptr;
        }
        if (textForegroundBrush) {
            OH_Drawing_BrushDestroy(textForegroundBrush);
            textForegroundBrush = nullptr;
        }
        spanIndex++;
    }
    // 根据handler对象生成文本排版布局typography
    context_thread_typography_ = OH_Drawing_CreateTypography(handler);
    if (constraint_width == 0) {
        constraint_width = 10000000;  // 无限宽
    }
    double maxWidth = constraint_width * dpi;
    OH_Drawing_TypographyLayout(context_thread_typography_, maxWidth);
    // 获取文本布局结果的宽高
    auto height = OH_Drawing_TypographyGetHeight(context_thread_typography_);
    auto ouput_measure_height_ = height / dpi;
    auto longestLineWidth =
        std::fmax(0, std::fmin(std::ceil(OH_Drawing_TypographyGetLongestLine(context_thread_typography_)), maxWidth));
    context_thread_text_align_ = text_align;
    auto ouput_measure_width_ = (longestLineWidth / dpi);
    if (ouput_measure_width_ < 0.01) {
        KR_LOG_ERROR << "Measure size:" << ouput_measure_width_ << ", " << ouput_measure_height_
                     << ", content bytes:" << GetTextContent().size() << ", in shadow view:" << this;
    }

    context_measure_size_ = KRSize(ouput_measure_width_, ouput_measure_height_);
    if (handler != nullptr) {
        OH_Drawing_DestroyTypographyHandler(handler);
    }
    if (typoStyle != nullptr) {
        OH_Drawing_DestroyTypographyStyle(typoStyle);
    }
    return context_thread_typography_;
}

void KRRichTextShadow::ReleaseLastTypography() {
    OH_Drawing_Typography *typography = context_thread_typography_;
    float drawOffsetY = context_thread_drawOffsetY_;
    float drawOffsetX = context_thread_drawOffsetX_;
    context_thread_typography_ = nullptr;
    context_thread_drawOffsetY_ = 0;
    context_thread_drawOffsetX_ = 0;
    context_thread_text_align_ = TEXT_ALIGN_LEFT;
    context_measure_size_ = KRSize(0, 0);
    std::shared_ptr<struct KRFontCollectionWrapper> collection = std::move(font_collection_wrapper_);
    if (typography != nullptr) {
        if (auto lock = GetRootView().lock()) {
            std::shared_ptr<IKRRenderShadowExport> self = shared_from_this();
            lock->AddTaskToMainQueueWithTask([self, typography, drawOffsetY, drawOffsetX, collection] {
                KRRichTextShadow *shadow = static_cast<KRRichTextShadow *>(self.get());
                if (shadow && shadow->MainThreadTypography() == typography) {
                    shadow->SetMainThreadTypography(nullptr);
                }
                OH_Drawing_DestroyTypography(typography);
            });
        }
    }
}

/**
 * 调用获取Span位置方法
 */
KRAnyValue KRRichTextShadow::SpanRect(int spanIndex) {
    if (placeholder_index_map_.find(spanIndex) != placeholder_index_map_.end()) {
        auto placeholderIndex = placeholder_index_map_[spanIndex];
        auto placeholderRects = OH_Drawing_TypographyGetRectsForPlaceholders(context_thread_typography_);
        auto x = OH_Drawing_GetLeftFromTextBox(placeholderRects, placeholderIndex);
        auto y = OH_Drawing_GetTopFromTextBox(placeholderRects, placeholderIndex);
        auto width = OH_Drawing_GetRightFromTextBox(placeholderRects, placeholderIndex) -
                     OH_Drawing_GetLeftFromTextBox(placeholderRects, placeholderIndex);
        auto height = OH_Drawing_GetBottomFromTextBox(placeholderRects, placeholderIndex) -
                      OH_Drawing_GetTopFromTextBox(placeholderRects, placeholderIndex);
        char buffer[50] = {0};
        auto dpi = KRConfig::GetDpi();
        // %.2f 有解析问题，所以此处取整
        std::snprintf(buffer, sizeof(buffer), "%.0f %.0f %.0f %.0f", x / dpi, y / dpi, width / dpi, height / dpi);
        return NewKRRenderValue(buffer);
    }
    return NewKRRenderValue("0 0 0 0");
}

int KRRichTextShadow::SpanIndexAt(float spanX, float spanY) {
    int resultIndex = -1;
    for (int index = 0; index < span_offsets_.size(); ++index) {
        int lastSpanIndex = std::get<0>(span_offsets_[index]);
        int lastSpanBegin = std::get<1>(span_offsets_[index]);
        int lastSpanEnd = std::get<2>(span_offsets_[index]);
        OH_Drawing_TextBox *box = OH_Drawing_TypographyGetRectsForRange(
            main_thread_typography_, lastSpanBegin, lastSpanEnd, RECT_HEIGHT_STYLE_MAX, RECT_WIDTH_STYLE_MAX);
        int n = OH_Drawing_GetSizeOfTextBox(box);
        auto dpi = KRConfig::GetDpi();
        for (int boxIndex = 0; boxIndex < n; ++boxIndex) {
            float left = OH_Drawing_GetLeftFromTextBox(box, boxIndex) / dpi;
            float right = OH_Drawing_GetRightFromTextBox(box, boxIndex) / dpi;
            float top = OH_Drawing_GetTopFromTextBox(box, boxIndex) / dpi;
            float bottom = OH_Drawing_GetBottomFromTextBox(box, boxIndex) / dpi;
            if (spanX < left || spanX >= right || spanY < top || spanY >= bottom) {
                continue;
            }
            resultIndex = lastSpanIndex;
            break;
        }
        if (resultIndex != -1) {
            break;
        }
    }
    return resultIndex;
}
