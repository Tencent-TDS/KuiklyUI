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

#include <deviceinfo.h>
#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_register_font.h>
#include <native_drawing/drawing_shader_effect.h>

#include "libohos_render/expand/components/richtext/KRFontAdapterManager.h"
#include "libohos_render/expand/components/richtext/KRParagraph.h"
#include "libohos_render//foundation/KRCommon.h"
#include "libohos_render/foundation/KRConfig.h"
#include "libohos_render/utils/KRConvertUtil.h"
#include "libohos_render/utils/KRViewUtil.h"


#ifdef __cplusplus
extern "C" {
#endif
// Remove this declaration if compatable api is raised to 14 and above
extern OH_Drawing_FontCollection *OH_Drawing_GetFontCollectionGlobalInstance(void) __attribute__((weak));
#ifdef __cplusplus
};
#endif
constexpr char kRawFilePrefix[] = "rawfile:";

static bool isRawFilePath(const std::string &src) { return src.find(kRawFilePrefix) == 0; }

constexpr int SHADER_EFFECT_DESTROY_API_LEVEL = 19;
static void KRSafeCall_OH_Drawing_ShaderEffectDestroy(OH_Drawing_ShaderEffect *shaderEffect) {
    // OH_Drawing_ShaderEffectDestroy has known issues:
    // 1. Crash on painting phase on devices with api 13、17 (#00 pc 00000000000c0760
    // /system/lib64/lib2d_graphics.z.so(OHOS::Rosen::Drawing::ColorFilter::Serialize()
    // const+8)(16bfef5cc4fcfd7dcea9585dd5c33a22))
    // 2. Works with api 19
    if (OH_GetSdkApiVersion() >= SHADER_EFFECT_DESTROY_API_LEVEL) {
        OH_Drawing_ShaderEffectDestroy(shaderEffect);
    } else {
        // noop
    }
}

class KRFontCollectionManager final {
  public:
    static std::shared_ptr<KRFontCollectionManager> &GetInstance() {
        static std::shared_ptr<KRFontCollectionManager> instance_ = nullptr;
        static std::once_flag flag;
        std::call_once(flag, []() { instance_ = std::make_shared<KRFontCollectionManager>(); });
        return instance_;
    }

    KRFontCollectionManager() {
        if (OH_Drawing_GetFontCollectionGlobalInstance) {
            collection_ = OH_Drawing_GetFontCollectionGlobalInstance();
        } else {
            collection_ = OH_Drawing_CreateSharedFontCollection();
        }
    }

    void LoadCustomFont(const std::string &fontFamily, NativeResourceManager *resMgr) {
        if (fontFamily.empty() || resMgr == nullptr) {
            return;
        }
        auto fontAdapters = KRFontAdapterManager::GetInstance()->AllAdapters();
        if (auto adapter = fontAdapters.find(fontFamily); adapter != fontAdapters.end()) {
            if (registered_.find(fontFamily) == registered_.end()) {
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

                        error = OH_Drawing_RegisterFontBuffer(collection_, fontFamily.c_str(), data.get(), len);
                    } else {
                        error = OH_Drawing_RegisterFont(collection_, fontFamily.c_str(), fontSrc);
                    }
                    if (error == 0) {
                        registered_.emplace(fontFamily);
                    }

                    if (deallocator) {
                        deallocator(fontSrc);
                    }
                } else if (fontBuffer != nullptr && len > 0) {
                    uint32_t error =
                      OH_Drawing_RegisterFontBuffer(collection_, fontFamily.c_str(), (uint8_t *)fontBuffer, len);
                    if (error == 0) {
                        registered_.emplace(fontFamily);
                    }
                    if (deallocator) {
                        deallocator(fontBuffer);
                    }
                }
            }
        }
    }

    OH_Drawing_FontCollection *SharedFontCollection() { return collection_; }

  private:
    OH_Drawing_FontCollection *collection_;
    std::unordered_set<std::string> registered_;
};

static KRAnyValue GetKTValue(const char *key, const KRRenderValue::Map &map0, const KRRenderValue::Map &map1) {
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

template <class Facet> struct deletable_facet : Facet {
    template <class... Args> deletable_facet(Args &&...args) : Facet(std::forward<Args>(args)...) {}
    ~deletable_facet() {}
};

KRParagraph::KRParagraph(KRRenderValue::Array spans, KRRenderValue::Map props, float font_size_scale,
                         float font_weight_scale, float density, NativeResourceManager *resource_manager,
                         std::shared_ptr<kuikly::util::KRLinearGradientParser> gradient)
    : styled_string_(nullptr), spans_(spans), props_(props), font_size_scale_(font_size_scale),
      font_weight_scale_(font_weight_scale), density_(density), resource_manager_(resource_manager),
      linear_gradient_(gradient) {
    // blank
}

KRParagraph::~KRParagraph() {
    if (styled_string_) {
        OH_ArkUI_StyledString_Destroy(styled_string_);
        styled_string_ = nullptr;
    }
    if (typography_) {
        OH_Drawing_DestroyTypography(typography_);
        typography_ = nullptr;
    }
}
std::pair<float, float> KRParagraph::Measure(float max_width_pt) {
    ArkUI_StyledString *styled_string = BuildStyledString(0, 0);
    auto result = Measure(styled_string, max_width_pt, INFINITY);
    measured_width_ = std::get<0>(result);
    measured_height_ = std::get<1>(result);
    if (linear_gradient_) {
        OH_ArkUI_StyledString_Destroy(styled_string);
        styled_string = BuildStyledString(std::get<0>(result), std::get<1>(result));
        styled_string_ = styled_string;
        result = Measure(styled_string, std::get<0>(result), std::get<1>(result));
        measured_width_ = std::get<0>(result);
        measured_height_ = std::get<1>(result);
        typography_ = std::get<2>(result);
        return std::make_pair(std::get<0>(result), std::get<1>(result));
    }

    styled_string_ = styled_string;
    typography_ = std::get<2>(result);
    return std::make_pair(std::get<0>(result), std::get<1>(result));
}

std::tuple<float, float, OH_Drawing_Typography *> KRParagraph::Measure(ArkUI_StyledString *styled_string,
                                                                       float max_width_pt, float max_height_pt) {
    if (styled_string) {
        auto typo = OH_ArkUI_StyledString_CreateTypography(styled_string);
        OH_Drawing_TypographyLayout(typo, max_width_pt * density_);
        float paragraph_height = OH_Drawing_TypographyGetHeight(typo) / density_;
        double maxWidth = max_width_pt * density_;
        float longestLineWidth =
          std::fmax(0, std::fmin(std::ceil(OH_Drawing_TypographyGetLongestLine(typo)), maxWidth));
        float paragraph_width = longestLineWidth / density_;

        return std::make_tuple(paragraph_width, paragraph_height, typo);
    }
    return std::make_tuple(0, 0, nullptr);
}

ArkUI_StyledString *KRParagraph::GetStyledString() {
    assert(styled_string_);
    return styled_string_;
}

OH_Drawing_TypographyStyle *KRParagraph::CreateTypographyStyle() {
    OH_Drawing_TypographyStyle *typography_style = OH_Drawing_CreateTypographyStyle();

    auto numberOfLines = GetKTValue("numberOfLines", props_, props_)->toInt();
    if (numberOfLines == 0) {
        numberOfLines = INT_MAX;
    }
    OH_Drawing_SetTypographyTextMaxLines(typography_style, numberOfLines);
    OH_Drawing_SetTypographyTextDirection(typography_style, text_direction_);
    OH_Drawing_TextAlign text_align = kuikly::util::ConvertToTextAlign(GetKTValue("textAlign", props_, props_)->toString());

    OH_Drawing_SetTypographyTextAlign(typography_style, text_align);
    const std::string lineBreakModeStr = GetKTValue("lineBreakMode", props_, props_)->toString();
    auto lineBreakMode = kuikly::util::ConvertToTextBreakMode(lineBreakModeStr);
    OH_Drawing_SetTypographyTextEllipsisModal(typography_style, lineBreakMode);
    // auto lineSpacing = GetKTValue("lineSpacing", props_, props_)->toFloat() / (fontSize / dpi);  // 行间距比例
    const char *ellipsis = "…";
    if (lineBreakModeStr == "clip") {
        ellipsis = "";
    }
    OH_Drawing_SetTypographyTextEllipsis(typography_style, ellipsis);

    OH_Drawing_WordBreakType workBreak = WORD_BREAK_TYPE_BREAK_WORD;
    if (numberOfLines == 1) {
        workBreak = WORD_BREAK_TYPE_BREAK_ALL;
    }
    OH_Drawing_SetTypographyTextWordBreakType(typography_style, workBreak);

//        if (lineSpacing > 0) {
//            /* Drawing自带的设置SpacingScale的接口段落前后仍有间距
//             * OH_Drawing_SetTypographyTextUseLineStyle(typoStyle, true);
//             * OH_Drawing_SetTypographyTextLineStyleSpacingScale(typoStyle, lineSpacing);
//             * 等待修复，目前使用设置行高+禁用首尾行间距实现，注意同时设置lineHeight和lineSpacing首尾间距也会失效
//             */
//            OH_Drawing_TypographyTextSetHeightBehavior(typography_style, TEXT_HEIGHT_DISABLE_ALL);
//        }
    return typography_style;
}
void KRParagraph::AddSpanToStyledString(const KRRenderValue::Map &spanMap, ArkUI_StyledString *styled_string) {
    double dpi = KRConfig::GetDpi();
    auto fontSize = (GetKTValue("fontSize", spanMap, props_)->toFloat() ?: 15.0) * dpi * font_size_scale_;
    auto text = GetKTValue("value", spanMap, spanMap)->toString();
    if (text.length() == 0) {
        text = GetKTValue("text", spanMap, spanMap)->toString();
    }
    auto fontWeight = kuikly::util::ConvertFontWeight(GetKTValue("fontWeight", spanMap, props_)->toInt(), font_weight_scale_);
    auto colorStr = GetKTValue("color", spanMap, props_)->toString();
    auto fontFamily = GetKTValue("fontFamily", spanMap, props_)->toString();
    auto color = colorStr.length() ? kuikly::util::ConvertToHexColor(colorStr) : 0xff000000;                   // 默认黑色
    auto lineHeight = GetKTValue("lineHeight", spanMap, props_)->toFloat() / (fontSize / dpi);   // 字体比例
    auto lineSpacing = GetKTValue("lineSpacing", spanMap, props_)->toFloat() / (fontSize / dpi); // 行间距比例
    auto textAlign = kuikly::util::ConvertToTextAlign(GetKTValue("textAlign", spanMap, props_)->toString());
    auto textDecoration = kuikly::util::ConvertToTextDecoration(GetKTValue("textDecoration", spanMap, props_)->toString());
    auto fontStyle = kuikly::util::ConvertToFontStyle(GetKTValue("fontStyle", spanMap, props_)->toString());
    auto letterSpacing = GetKTValue("letterSpacing", spanMap, props_)->toDouble();
    auto textShadowStr = GetKTValue("textShadow", spanMap, props_)->toString();
    auto strokeWidth = GetKTValue("strokeWidth", spanMap, props_)->toFloat();
    auto strokeColorStr = GetKTValue("strokeColor", spanMap, props_)->toString();
    auto strokeColor = strokeColorStr.length() ? kuikly::util::ConvertToHexColor(strokeColorStr) : 0xff000000;

    auto placeholderWidth = GetKTValue("placeholderWidth", spanMap, spanMap)->toDouble();
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
        auto shaderEffect = CreateShaderEffect(linear_gradient_);
        if (shaderEffect) {
            OH_Drawing_BrushSetShaderEffect(textForegroundBrush, shaderEffect);
        } else {
            OH_Drawing_BrushSetColor(textForegroundBrush, color);
        }
        OH_Drawing_SetTextStyleForegroundBrush(txtStyle, textForegroundBrush);
        OH_Drawing_BrushDestroy(textForegroundBrush);
        if (shaderEffect) {
            KRSafeCall_OH_Drawing_ShaderEffectDestroy(shaderEffect);
            shaderEffect = nullptr;
        }
        textForegroundBrush = nullptr;
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
    }
    // fontFamily
    if (!fontFamily.empty()) {
        const char *fontFamilyPtr = fontFamily.c_str();
        const char *fontFamilies[] = {fontFamilyPtr};
        OH_Drawing_SetTextStyleFontFamilies(txtStyle, 1, fontFamilies);
        KRFontCollectionManager::GetInstance()->LoadCustomFont(fontFamily, resource_manager_);
    }

    // Push text style
    OH_ArkUI_StyledString_PushTextStyle(styled_string, txtStyle);

    // Add text or span
    if (placeholderWidth != 0) { // 添加占位Span
        auto placeholderHeight = GetKTValue("placeholderHeight", spanMap, spanMap)->toDouble();
        OH_Drawing_PlaceholderSpan inlineView = {
          placeholderWidth * dpi,      placeholderHeight * dpi,
          ALIGNMENT_CENTER_OF_ROW_BOX, // VerticalAlign is 居中
          TEXT_BASELINE_ALPHABETIC,    0,
        };
        // OH_Drawing_TypographyHandlerAddPlaceholder(handler, &inlineView);
        OH_ArkUI_StyledString_AddPlaceholder(styled_string, &inlineView);
        placeholder_index_map_[spanIndex] = placeholder_count;
        placeholder_count++;
        charOffset += 1;
    } else {
        OH_ArkUI_StyledString_AddText(styled_string, text.c_str());
        // OH_Drawing_TypographyHandlerAddText(handler, text.c_str());  // 添加文本

        std::wstring_convert<deletable_facet<std::codecvt<char16_t, char, std::mbstate_t>>, char16_t> conv16;
        std::u16string str16 = conv16.from_bytes(text);
        int codePointCount = str16.size();
        span_offsets_.emplace_back(std::tuple(spanIndex, charOffset, charOffset + codePointCount));
        charOffset += codePointCount;
    }
    ++spanIndex;

    // Pop text style
    OH_ArkUI_StyledString_PopTextStyle(styled_string);
    OH_Drawing_DestroyTextStyle(txtStyle);
}
ArkUI_StyledString *KRParagraph::BuildStyledString(float width, float height) {
    ArkUI_StyledString *styled_string = nullptr;
    charOffset = 0;
    spanIndex = 0;
    placeholder_count = 0;
    span_offsets_.clear();
    placeholder_index_map_.clear();

    // create OH_Drawing_TypographyStyle
    OH_Drawing_TypographyStyle *typography_style = CreateTypographyStyle();

    // Create styled string
    styled_string =
      OH_ArkUI_StyledString_Create(typography_style, KRFontCollectionManager::GetInstance()->SharedFontCollection());

    // add text spans
    std::for_each(spans_.begin(), spans_.end(),
                  [this, styled_string](auto span) { AddSpanToStyledString(span->toMap(), styled_string); });

    // clean up
    OH_Drawing_DestroyTypographyStyle(typography_style);
    return styled_string;
}

std::tuple<float, float, float, float> KRParagraph::SpanRect(int spanIndex) {
    if (typography_ == nullptr) {
        return std::make_tuple(0, 0, 0, 0);
    }
    if (placeholder_index_map_.find(spanIndex) != placeholder_index_map_.end()) {
        auto placeholderIndex = placeholder_index_map_[spanIndex];
        auto placeholderRects = OH_Drawing_TypographyGetRectsForPlaceholders(typography_);
        auto x = OH_Drawing_GetLeftFromTextBox(placeholderRects, placeholderIndex);
        auto y = OH_Drawing_GetTopFromTextBox(placeholderRects, placeholderIndex);
        auto width = OH_Drawing_GetRightFromTextBox(placeholderRects, placeholderIndex) -
                     OH_Drawing_GetLeftFromTextBox(placeholderRects, placeholderIndex);
        auto height = OH_Drawing_GetBottomFromTextBox(placeholderRects, placeholderIndex) -
                      OH_Drawing_GetTopFromTextBox(placeholderRects, placeholderIndex);
        OH_Drawing_TypographyDestroyTextBox(placeholderRects);
        return std::make_tuple(x, y, width, height);
    }
    return std::make_tuple(0, 0, 0, 0);
}


int KRParagraph::SpanIndexAt(float spanX, float spanY) {
    int resultIndex = -1;
    if (typography_ == nullptr) {
        return resultIndex;
    }
    for (int index = 0; index < span_offsets_.size(); ++index) {
        int lastSpanIndex = std::get<0>(span_offsets_[index]);
        int lastSpanBegin = std::get<1>(span_offsets_[index]);
        int lastSpanEnd = std::get<2>(span_offsets_[index]);
        OH_Drawing_TextBox *box = OH_Drawing_TypographyGetRectsForRange(typography_, lastSpanBegin, lastSpanEnd,
                                                                        RECT_HEIGHT_STYLE_MAX, RECT_WIDTH_STYLE_MAX);
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
        OH_Drawing_TypographyDestroyTextBox(box);
        if (resultIndex != -1) {
            break;
        }
    }
    return resultIndex;
}

OH_Drawing_ShaderEffect *KRParagraph::CreateShaderEffect(std::shared_ptr<kuikly::util::KRLinearGradientParser> linearGradient) {
    OH_Drawing_ShaderEffect *shaderEffect = nullptr;
    if (linearGradient && measured_width_ > 0 && measured_height_ > 0) {
        // 获取 colors 和 locations
        const std::vector<uint32_t> &colors = linearGradient->GetColors();
        const std::vector<float> &locations = linearGradient->GetLocations();

        OH_Drawing_Point *startPt =
          linearGradient->GetStartPoint(measured_width_ * density_, measured_height_ * density_);
        OH_Drawing_Point *endPt = linearGradient->GetEndPoint(measured_width_ * density_, measured_height_ * density_);

        shaderEffect = OH_Drawing_ShaderEffectCreateLinearGradient(startPt, endPt, colors.data(), locations.data(),
                                                                   colors.size(), OH_Drawing_TileMode::CLAMP);
        OH_Drawing_PointDestroy(startPt);
        OH_Drawing_PointDestroy(endPt);
    }
    return shaderEffect;
}