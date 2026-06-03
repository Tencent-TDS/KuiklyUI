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


#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_font_collection.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_pixel_map.h>
#include <native_drawing/drawing_register_font.h>
#include <native_drawing/drawing_rect.h>
#include <native_drawing/drawing_sampling_options.h>
#include <native_drawing/drawing_shader_effect.h>
#include <native_drawing/drawing_text_declaration.h>
#include <native_drawing/drawing_text_typography.h>
#include <multimedia/image_framework/image/image_source_native.h>
#include <multimedia/image_framework/image/pixelmap_native.h>

#include <codecvt>
#include <thread>
#include <unordered_set>

#include "libohos_render/api/src/KRTextPostProcessor.h"
#include "libohos_render/expand/components/richtext/KRCustomEmojiPixmapCache.h"
#include "libohos_render/expand/components/richtext/KRParagraph.h"
#include "libohos_render/expand/components/richtext/KRRichTextShadow.h"
#include "libohos_render/foundation/thread/KRMainThread.h"
#include "libohos_render/utils/KRConvertUtil.h"
#include "libohos_render/utils/KRLinearGradientParser.h"
#include "libohos_render/utils/KRStringUtil.h"
#include "libohos_render/utils/KRViewUtil.h"

static bool KR_TEXT_RENDER_V2_ENABLED = false;
#ifdef __cplusplus
extern "C" {
#endif
// Remove this declaration if compatable api is raised to 14 and above
extern OH_Drawing_FontCollection* OH_Drawing_GetFontCollectionGlobalInstance(void) __attribute__((weak));
extern OH_Drawing_Array* OH_Drawing_TypographyGetTextLines(OH_Drawing_Typography* typography) __attribute__((weak));
extern void OH_Drawing_DestroyTextLines(OH_Drawing_Array* lines) __attribute__((weak));
// 垂直对齐接口的弱符号声明（系统 API 20+ 提供，低版本系统该符号为 nullptr）
extern void OH_Drawing_SetTypographyVerticalAlignment(OH_Drawing_TypographyStyle* style,
                                                      OH_Drawing_TextVerticalAlignment alignment) __attribute__((weak));

void KREnableTextRenderV2(){
    KR_TEXT_RENDER_V2_ENABLED = true;
}
#ifdef __cplusplus
};
#endif

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
    DestroyCachedTextLines();
    // 不再手动调 OH_Drawing_DestroyTypography：shared_ptr 的 deleter 会在
    // 最后一个引用释放时自动销毁对象，避免与同时还持有该 typography 的
    // 任务 lambda 冲突。如果还有另一端持有它，销毁会自然延后到那个持有者
    // 释放时。
    main_thread_typography_.reset();
    context_thread_typography_.reset();
    // pixmap 缓存已迁移至 KRCustomEmojiPixmapCache（进程级单例），不随 shadow 销毁
    // 而释放——同一张 emoji 在多个 RichText / 多页面间复用，由 LRU(128) 控制容量。
    // 销毁仅需清理本 shadow 持有的轻量状态。
    {
        std::lock_guard<std::mutex> lk(image_loaded_callback_mutex_);
        image_loaded_callback_ = nullptr;
    }
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
    } else if(method_name == "isLineBreakMargin"){
        return NewKRRenderValue(did_exceed_max_lines_ && OH_Drawing_DestroyTextLines? "1" : "0");
    }
    return KRRenderValue::Make(nullptr);
}

/**
 * 根据布局约束尺寸计算返回 RenderView 的实际尺寸
 * @param constraint_width
 * @param constraint_height
 * @return
 */
KRSize KRRichTextShadow::CalculateRenderViewSize(double constraint_width, double constraint_height) {
    if(StyledStringEnabled()){
        KRSize sz = CalculateRenderViewSizeWithStyledString(constraint_width, constraint_height);
        return sz;
    }else{
        SetParagraph(nullptr);
    }
    ReleaseLastTypography();
    BuildTextTypography(constraint_width, constraint_height);
    return context_measure_size_;
}

KRSize KRRichTextShadow::CalculateRenderViewSizeWithStyledString(double constraint_width, double constraint_height) {
    auto rootView = GetRootView().lock();
    if (rootView == nullptr) {
        return KRSize(0,0);
    }

    struct RootViewThreadingDispatcher{
        RootViewThreadingDispatcher(std::shared_ptr<IKRRenderView> r): rootView_(r){
            // blank
        }
        ~RootViewThreadingDispatcher(){
            if(auto theRootView = rootView_) {
                //
                // We need to dispatch it back to main thread to avoid destructing it on context thread,
                // in case `rootView` variable is the last holding onto the root render view.
                //
                KRMainThread::RunOnMainThread([theRootView]{
                    theRootView.get();
                });
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
        spans.push_back(KRRenderValue::Make(props_));
    }
    
    auto nativeResMgr = rootView->GetNativeResourceManager();
    std::shared_ptr<KRParagraph> paragraph = std::make_shared<KRParagraph>(spans, props_, fontSizeScale, fontWeightScale, KRConfig::GetDpi(), nativeResMgr, text_linearGradient_);
    auto [width, height] = paragraph->Measure(constraint_width);
    SetParagraph(paragraph);
    return KRSize(width, height);
}

bool KRRichTextShadow::StyledStringEnabled(){
    // 决策 6C：含 image span（PostProcessor 拆段产生的内置图片占位）时，必须走
    // V1 OnForegroundDraw 路径才能插入图片绘制。这里把判定收敛在 shadow 端，
    // view 端只需读取一个布尔结果。
    if (HasImageSpans()) {
        return false;
    }
    return KR_TEXT_RENDER_V2_ENABLED;
}


/**
 * 将要SetShadow调用
 * @return
 */
KRSchedulerTask KRRichTextShadow::TaskToMainQueueWhenWillSetShadowToView() {
    auto self = shared_from_this();
    // 拷贝一份 shared_ptr，保证 lambda 在主线程执行期间该 typography 不会被
    // context 线程后续的 ReleaseLastTypography()/重新 BuildTextTypography()
    // 释放掉。
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
    return KRRenderValue::Make(nullptr);
}

KRFontCollectionWrapper::KRFontCollectionWrapper() {
    // 优先使用全局实例（API >= 14），否则创建共享实例
    if (OH_Drawing_GetFontCollectionGlobalInstance) {
        fontCollection_ = OH_Drawing_GetFontCollectionGlobalInstance();
        isGlobalInstance_ = true;
    } else {
        fontCollection_ = OH_Drawing_CreateSharedFontCollection();
        isGlobalInstance_ = false;
    }
}

KRFontCollectionWrapper::~KRFontCollectionWrapper() {
    // 只有非全局实例需要销毁
    if (fontCollection_ && !isGlobalInstance_) {
        OH_Drawing_DestroyFontCollection(fontCollection_);
    }
    fontCollection_ = nullptr;
}

OH_Drawing_FontCollection* KRFontCollectionWrapper::GetFontCollection() {
    return fontCollection_;
}

bool KRFontCollectionWrapper::IsFontRegistered(const std::string& fontFamily) const {
    return registered_.find(fontFamily) != registered_.end();
}

void KRFontCollectionWrapper::MarkFontRegistered(const std::string& fontFamily) {
    registered_.emplace(fontFamily);
}

void KRFontCollectionWrapper::RegisterCustomFont(NativeResourceManager *resMgr,
                                                  const std::string &fontFamily) {
    auto fontAdapters = KRFontAdapterManager::GetInstance()->AllAdapters();
    auto adapter = fontAdapters.find(fontFamily);
    if (adapter != fontAdapters.end() && !IsFontRegistered(fontFamily)) {
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
                error = OH_Drawing_RegisterFontBuffer(fontCollection_, fontFamily.c_str(), data.get(), len);
            } else {
                error = OH_Drawing_RegisterFont(fontCollection_, fontFamily.c_str(), fontSrc);
            }
            if (error == 0) {
                MarkFontRegistered(fontFamily);
            }

            if (deallocator) {
                deallocator(fontSrc);
            }
        } else if (fontBuffer != nullptr && len > 0) {
            uint32_t error =
                OH_Drawing_RegisterFontBuffer(fontCollection_, fontFamily.c_str(),
                                              reinterpret_cast<uint8_t *>(fontBuffer), len);
            if (error == 0) {
                MarkFontRegistered(fontFamily);
            }
            if (deallocator) {
                deallocator(fontBuffer);
            }
        }
    }
}

OH_Drawing_TypographyCreate* CreateTypographyHandler(OH_Drawing_TypographyStyle* typoStyle) {
    auto &wrapper = KRFontCollectionWrapper::GetInstance();
    return OH_Drawing_CreateTypographyHandler(typoStyle, wrapper.GetFontCollection());
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
    image_draw_records_.clear();
    KRRenderValue::Array spans = values_;
    if (spans.empty()) {
        spans.push_back(KRRenderValue::Make(props_));
    }

    // ===== Phase 2: PostProcessor 拆段 =====
    // 仅对"纯文本 span"（没有 placeholderWidth 且无内置 image src 标记）调用一次
    // RunTextPostProcessor(processor_name, text, segs)：
    //   * processor_name 取自 props_["textPostProcessor"]（与 iOS / Android 跨端语义对齐：
    //     业务通过 `Text { textPostProcessor("input") }` / `Text { textPostProcessor("richtext") }`
    //     等显式声明 name；OHOS 侧不假设默认 name），缺省（业务未声明）时跳过 adapter，
    //     与原始路径完全等价、零开销。
    //   * 若 adapter 返回非空 segs：把该 span **就地替换**为多个 span——每个 text seg 复制
    //     原 span 全部样式属性 + 改写 text 字段；每个 image seg 复制原 span 全部样式属性
    //     + 写入 placeholderWidth/Height（让现有循环走占位分支）+ 内置字段
    //     `__kr_image_src__`（让本函数后续把它登记到 image_draw_records_）。
    //   * 未注册 adapter / 返回空：保持原 span 不变。
    // 业务声明的 ImageSpan（spanPropsMap 自带 placeholderWidth）跳过——它们走原有
    // "PlaceholderSpan + 父节点 ImageView" 链路，不需要本机制接管图片绘制。
    {
        std::string processor_name = GetKRValue("textPostProcessor", props_, props_)->toString();
        if (!processor_name.empty()) {
            KRRenderValue::Array expanded;
            expanded.reserve(spans.size());
            for (const auto &span : spans) {
                auto m = span->toMap();
                // 已声明 image span（业务自己写 ImageSpan { src(...) }）：跳过 PostProcessor
                auto declared_ph_w = GetKRValue("placeholderWidth", m, m)->toDouble();
                // 内置 image span（前一轮 SetProp 已展开过 / 嵌套场景）：避免重复展开
                auto already_image_src = GetKRValue(kuikly::richtext::kInternalImageSrcKey, m, m)->toString();
                if (declared_ph_w != 0 || !already_image_src.empty()) {
                    expanded.push_back(span);
                    continue;
                }
                auto raw_text = GetKRValue("value", m, m)->toString();
                if (raw_text.empty()) {
                    raw_text = GetKRValue("text", m, m)->toString();
                }
                if (raw_text.empty()) {
                    expanded.push_back(span);
                    continue;
                }
                std::vector<kuikly::text::KRTextPostProcessSpan> segs;
                if (!kuikly::text::RunTextPostProcessor(processor_name, raw_text, segs)) {
                    expanded.push_back(span);
                    continue;
                }
                // 命中 adapter：按 segs 顺序生成多个 span。每个新 span 在原 span 的 map 基础上
                // 改写 value/text/placeholderWidth/placeholderHeight/__kr_image_src__ 等字段，
                // 其它样式（fontSize / color / fontWeight / textAlign / lineHeight ...）原样继承。
                for (const auto &seg : segs) {
                    auto new_map = m;  // copy
                    if (seg.type == kuikly::text::KRTextPostProcessSpan::Type::kText) {
                        new_map["value"] = NewKRRenderValue(seg.text_or_src);
                        new_map["text"] = NewKRRenderValue(seg.text_or_src);
                        new_map.erase("placeholderWidth");
                        new_map.erase("placeholderHeight");
                        new_map.erase(kuikly::richtext::kInternalImageSrcKey);
                    } else {
                        // image seg：用占位分支，dpi 缩放在循环内统一处理；
                        // 业务给的 width/height 单位是 vp（与 ImageSpan / TextEditor 路径一致），
                        // 缺省时按当前 fontSize（vp）取方形。
                        float w = seg.width > 0 ? seg.width : 0.0f;
                        float h = seg.height > 0 ? seg.height : (w > 0 ? w : 0.0f);
                        if (w <= 0) {
                            // 取该 span 的 fontSize（vp）作为兜底——保证 emoji 与文字同高。
                            float fs_vp = GetKRValue("fontSize", m, props_)->toFloat();
                            if (fs_vp <= 0) {
                                fs_vp = 16.0f;  // 与 ImageView span 的兜底口径一致
                            }
                            w = fs_vp;
                            h = fs_vp;
                        }
                        new_map["value"] = NewKRRenderValue(std::string(""));
                        new_map["text"] = NewKRRenderValue(std::string(""));
                        new_map["placeholderWidth"] = NewKRRenderValue(static_cast<double>(w));
                        new_map["placeholderHeight"] = NewKRRenderValue(static_cast<double>(h));
                        new_map[kuikly::richtext::kInternalImageSrcKey] = NewKRRenderValue(seg.text_or_src);
                    }
                    expanded.push_back(KRRenderValue::Make(new_map));
                }
            }
            spans = std::move(expanded);
        }
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
    std::string text_content;
    for (auto span : spans) {
        auto spanMap = span->toMap();
        auto fontSize = (GetKRValue("fontSize", spanMap, props_)->toFloat() ?: 15.0) * dpi * fontSizeScale;
        auto text = GetKRValue("value", spanMap, spanMap)->toString();
        if (text.length() == 0) {
            text = GetKRValue("text", spanMap, spanMap)->toString();
        }
        auto fontWeight = kuikly::util::ConvertFontWeight(GetKRValue("fontWeight", spanMap, props_)->toInt(), fontWeightScale);
        // 解析基于Span的多个渐变色属性
        auto colorStr = GetKRValue("color", spanMap, props_)->toString();
        auto backgroundImage = GetKRValue("backgroundImage", spanMap, props_)->toString();
        OH_Drawing_ShaderEffect *colorShaderEffect = nullptr;
        auto linearGradient = std::make_shared<kuikly::util::KRLinearGradientParser>();
        bool hasBackgroundImage = linearGradient->ParseFromCssLinearGradient(backgroundImage);      // 当前是否存在渐变色待解析

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

        // 颜色设置，优先判断是否存在渐变色待加载
        if (hasBackgroundImage) {
            // 获取 colors 和 locations
            const std::vector<uint32_t> &colors = linearGradient->GetColors();
            const std::vector<float> &locations = linearGradient->GetLocations();
    
            // 创建 C 风格数组
            unsigned int colorsArray[colors.size()];
            float stopsArray[locations.size()];
    
            // 填充数组
            for (size_t i = 0; i < colors.size(); ++i) {
                colorsArray[i] = colors[i];
            }
            for (size_t i = 0; i < locations.size(); ++i) {
                if (i == locations.size() - 1) {
                    stopsArray[i] = 1.0;
                } else {
                    stopsArray[i] = locations[i];
                }
            }
            // 估算文本宽高
            auto calculateSize = CalculateRenderViewSizeWithStyledString(constraint_width, constraint_height);
            // 开始点
            OH_Drawing_Point *startPt = linearGradient->GetStartPoint(calculateSize.width * dpi, calculateSize.height * dpi);
            // 结束点
            OH_Drawing_Point *endPt = linearGradient->GetEndPoint(calculateSize.width * dpi, calculateSize.height * dpi);
            // 创建线性渐变着色器效果
             colorShaderEffect = OH_Drawing_ShaderEffectCreateLinearGradient(startPt, endPt, colorsArray, stopsArray, colors.size(), OH_Drawing_TileMode::CLAMP);
        }

        
        // 基于画刷设置着色器效果
        if (textForegroundBrush) {
            if (hasBackgroundImage) {
                OH_Drawing_BrushSetShaderEffect(textForegroundBrush, colorShaderEffect);
            } else {
                OH_Drawing_BrushSetColor(textForegroundBrush, color);
            }
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
            // 低版本系统（不支持 OH_Drawing_SetTypographyVerticalAlignment）的 work around：
            // cai 系统绘制存在偏移问题，手动校准 drawOffsetY_ 实现垂直居中
            // 高版本系统通过 OH_Drawing_SetTypographyVerticalAlignment 设置垂直居中，无需此校准
            if (&OH_Drawing_SetTypographyVerticalAlignment == nullptr) {
                context_thread_drawOffsetY_ = (fontSize * lineHeight - fontSize) / 4;
            }
        }
        // fontFamily
        if (!fontFamily.empty()) {
            const char *fontFamilyPtr = fontFamily.c_str();
            const char *fontFamilies[] = {fontFamilyPtr};
            OH_Drawing_SetTextStyleFontFamilies(txtStyle, 1, fontFamilies);
            auto rootView = GetRootView();
            auto rootViewLock = rootView.lock();
            auto nativeResMgr = rootViewLock->GetNativeResourceManager();
            KRFontCollectionWrapper::GetInstance().RegisterCustomFont(nativeResMgr, fontFamily);
        }

        // 需要在fontFamily设置后设置
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
            // 设置文本垂直居中：API 20+ 系统支持 OH_Drawing_SetTypographyVerticalAlignment
            // 弱符号检查：地址为 nullptr 表示当前系统不提供该接口，将回退到基线 work around
            if (&OH_Drawing_SetTypographyVerticalAlignment != nullptr) {
                OH_Drawing_SetTypographyVerticalAlignment(typoStyle, TEXT_VERTICAL_ALIGNMENT_CENTER);
            }
            handler = CreateTypographyHandler(typoStyle);
        } else {
            isFirst = false;
        }

        OH_Drawing_SetTextStyleFontStyle(txtStyle, FONT_STYLE_NORMAL);
        // 将文本样式对象加入到handler中
        if (!isFirst) {
            OH_Drawing_TypographyHandlerPopTextStyle(handler);
        }
        // 调用KRGradientRichTextShadow 绘制渐变色，KRGradientRichTextShadow支持对整个文本基于BackgroundImage属性绘制渐变色
        // 此调用与当前RichtextShadow中的渐变操作不冲突
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
            // 仅当此 placeholder 是由 PostProcessor("richtext") 展开产生的内置 image span
            // 时，登记到 image_draw_records_ 以便 view 层在 OnForegroundDraw 中绘制图片。
            // 业务自己声明的 ImageSpan（无 kInternalImageSrcKey 字段）继续走"父节点 ImageView"
            // 老链路，不被本机制接管。
            auto image_src = GetKRValue(kuikly::richtext::kInternalImageSrcKey, spanMap, spanMap)->toString();
            if (!image_src.empty()) {
                KRImageDrawRecord rec;
                rec.placeholder_index = placeholder_count;
                rec.src_uri = image_src;
                rec.width_vp = static_cast<float>(placeholderWidth);
                rec.height_vp = static_cast<float>(placeholderHeight);
                image_draw_records_.push_back(std::move(rec));
            }
            placeholder_count++;
            charOffset += 1;
        } else {
            OH_Drawing_TypographyHandlerAddText(handler, text.c_str());  // 添加文本
            text_content.append(text);

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
    context_thread_typography_ = KRMakeTypographyHandle(OH_Drawing_CreateTypography(handler));
    OH_Drawing_Typography *typography_raw = context_thread_typography_.get();
    if (constraint_width == 0) {
        constraint_width = 10000000;  // 无限宽
    }
    // headIndent: 首行缩进（第一个元素为首行缩进，第二个元素为0表示后续行不缩进）
    auto headIndent = GetKRValue("headIndent", props_, props_)->toFloat();
    if (headIndent > 0) {
        float indents[] = {static_cast<float>(headIndent * dpi), 0.0f};
        OH_Drawing_TypographySetIndents(typography_raw, 2, indents);
    }
    double maxWidth = constraint_width * dpi;
    OH_Drawing_TypographyLayout(typography_raw, maxWidth);
    did_exceed_max_lines_ = OH_Drawing_TypographyDidExceedMaxLines(typography_raw);
    // 获取文本布局结果的宽高
    auto height = OH_Drawing_TypographyGetHeight(typography_raw);
    auto ouput_measure_height_ = height / dpi;
    auto longestLineWidth =
        std::fmax(0, std::fmin(std::ceil(OH_Drawing_TypographyGetLongestLine(typography_raw)), maxWidth));
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
    text_content_ = text_content;
    // 触发 image span 异步预加载（决策 3C）。当 image_draw_records_ 为空（业务未注册
    // PostProcessor / 全是文本）时本方法立即返回，零开销。
    TriggerImagePrefetchIfNeed();
    return typography_raw;
}

void KRRichTextShadow::ReleaseLastTypography() {
    // 同步在当前线程（通常是 context 线程，但在 “主线程 Kotlin 同步回调递归
    // 触发 measure” 的场景下也可能是主线程）上释放本身对 typography 的强引用。
    //
    // 这里不再主动投递 destroy 任务到主线程：如果同一个 typography 还被
    // main_thread_typography_ 、或者某条尚未消费的 SetShadow lambda、或者本线程
    // 当前调用栈上起上游代码所持有，那么这些持有者会延长它的寿命，直到
    // 安全的时机（由 shared_ptr 的 deleter）才调 OH_Drawing_DestroyTypography。
    context_thread_typography_.reset();
    context_thread_drawOffsetY_ = 0;
    context_thread_drawOffsetX_ = 0;
    context_thread_text_align_ = TEXT_ALIGN_LEFT;
    context_measure_size_ = KRSize(0, 0);
}

// ===== Phase 3: image span 异步预加载（委托 KRCustomEmojiPixmapCache） =====
// 在 BuildTextTypography 末尾被调用：遍历 image_draw_records_，对每个 uri 调用
// KRCustomEmojiPixmapCache::Prefetch。缓存 / 去重 / 后台解码 / 主线程回调都由
// 该单例管理；shadow 只负责在解码完成时转发给 view（markDirty）。
// weak_from_this 拦截 shadow 销毁后的悬挂访问；view 销毁时
// SetImageLoadedCallback(nullptr) 避免反向访问已销毁的 view。
void KRRichTextShadow::TriggerImagePrefetchIfNeed() {
    if (image_draw_records_.empty()) {
        return;
    }
    auto weak_self = std::weak_ptr<IKRRenderShadowExport>(shared_from_this());
    for (const auto &rec : image_draw_records_) {
        if (rec.src_uri.empty()) {
            continue;
        }
        KRCustomEmojiPixmapCache::GetInstance().Prefetch(
            rec.src_uri, [weak_self](const std::string &uri) {
                auto strong = weak_self.lock();
                if (!strong) {
                    return;
                }
                auto *self = static_cast<KRRichTextShadow *>(strong.get());
                ImageLoadedCallback cb_copy;
                {
                    std::lock_guard<std::mutex> lk(self->image_loaded_callback_mutex_);
                    cb_copy = self->image_loaded_callback_;
                }
                // 在锁外调用 view 回调，避免业务回调里反向访问缓存形成死锁。
                if (cb_copy) {
                    cb_copy(uri);
                }
            });
    }
}

/**
 * 调用获取Span位置方法
 */
KRAnyValue KRRichTextShadow::SpanRect(int spanIndex) {
    if(auto paragraph = GetParagraph()){
        auto [paragraphX, paragraphY, paragraphW, paragraphH] = paragraph->SpanRect(spanIndex);
        char buffer[50] = {0};
        auto dpi = KRConfig::GetDpi();
        std::snprintf(buffer, sizeof(buffer), "%.0f %.0f %.0f %.0f", paragraphX / dpi, paragraphY / dpi, paragraphW / dpi, paragraphH / dpi);
        return NewKRRenderValue(buffer);
    }

    if (placeholder_index_map_.find(spanIndex) != placeholder_index_map_.end()) {
        auto placeholderIndex = placeholder_index_map_[spanIndex];
        // 在调用栈内拷贝一份强引用，避免其它线程同时 ReleaseLastTypography 释放。
        KRTypographyHandle typo = context_thread_typography_;
        OH_Drawing_Typography *typo_raw = typo ? typo.get() : nullptr;
        if (typo_raw == nullptr) {
            return NewKRRenderValue("0 0 0 0");
        }
        auto placeholderRects = OH_Drawing_TypographyGetRectsForPlaceholders(typo_raw);
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
    int paragraphResultIndex = -1;
    if(auto paragraph = GetParagraph()){
        paragraphResultIndex = paragraph->SpanIndexAt(spanX, spanY);
        return paragraphResultIndex;
    }
    int resultIndex = -1;
    // 同上，拿到强引用后再使用裸指针调 OH 接口。
    KRTypographyHandle main_typo = main_thread_typography_;
    OH_Drawing_Typography *main_typo_raw = main_typo ? main_typo.get() : nullptr;
    if (main_typo_raw == nullptr) {
        return resultIndex;
    }
    for (int index = 0; index < span_offsets_.size(); ++index) {
        int lastSpanIndex = std::get<0>(span_offsets_[index]);
        int lastSpanBegin = std::get<1>(span_offsets_[index]);
        int lastSpanEnd = std::get<2>(span_offsets_[index]);
        OH_Drawing_TextBox *box = OH_Drawing_TypographyGetRectsForRange(
            main_typo_raw, lastSpanBegin, lastSpanEnd, RECT_HEIGHT_STYLE_MAX, RECT_WIDTH_STYLE_MAX);
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

OH_Drawing_Array *KRRichTextShadow::GetTextLines(){
    if(text_lines_ == nullptr && OH_Drawing_TypographyGetTextLines){
        // 拿到强引用，防止在调用 OH_Drawing_TypographyGetTextLines 期间被其他线程释放。
        KRTypographyHandle main_typo = main_thread_typography_;
        OH_Drawing_Typography *main_typo_raw = main_typo ? main_typo.get() : nullptr;
        if (main_typo_raw == nullptr) {
            return text_lines_;
        }
        text_lines_ = OH_Drawing_TypographyGetTextLines(main_typo_raw);
    }
    return text_lines_;
}

void KRRichTextShadow::DestroyCachedTextLines(){
    if (text_lines_ && OH_Drawing_DestroyTextLines){
        OH_Drawing_DestroyTextLines(text_lines_);
        text_lines_ = nullptr;
    }
}