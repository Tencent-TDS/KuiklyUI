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

#include "libohos_render/expand/components/richtext/KRRichTextView.h"

#include <multimedia/image_framework/image/pixelmap_native.h>
#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_pixel_map.h>
#include <native_drawing/drawing_point.h>
#include <native_drawing/drawing_rect.h>
#include <native_drawing/drawing_sampling_options.h>
#include <native_drawing/drawing_shader_effect.h>
#include "libohos_render/expand/components/base/KRCustomUserCallback.h"
#include "libohos_render/expand/components/richtext/KRCustomEmojiPixmapCache.h"
#include "libohos_render/expand/components/richtext/KRRichTextShadow.h"
#include "libohos_render/foundation/KRConfig.h"
#include "libohos_render/foundation/thread/KRMainThread.h"

#ifdef __cplusplus
extern "C" {
#endif
// Remove this declaration if compatable api is raised to 14 and above
extern size_t OH_Drawing_GetDrawingArraySize(OH_Drawing_Array* drawingArray) __attribute__((weak));
extern OH_Drawing_TextLine* OH_Drawing_TextLineCreateTruncatedLine(OH_Drawing_TextLine* line, double width, int mode,
    const char* ellipsis) __attribute__((weak));
extern void OH_Drawing_TextLinePaint(OH_Drawing_TextLine* line, OH_Drawing_Canvas* canvas, double x, double y) __attribute__((weak));
extern OH_Drawing_TextLine* OH_Drawing_GetTextLineByIndex(OH_Drawing_Array* lines, size_t index) __attribute__((weak));
extern void OH_Drawing_DestroyTextLine(OH_Drawing_TextLine* line) __attribute__((weak));
#ifdef __cplusplus
}
#endif

static const char * kPropNameLineBreakMargin = "lineBreakMargin";
static const char * kPropNameClick = "click";

ArkUI_NodeHandle KRRichTextView::CreateNode() {
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_TEXT);
}

void KRRichTextView::OnDestroy() {
    kuikly::util::GetNodeApi()->resetAttribute(GetNode(), NODE_TEXT_CONTENT_WITH_STYLED_STRING);

    IKRRenderViewExport::OnDestroy();
    auto self = shared_from_this();
    KREventDispatchCenter::GetInstance().UnregisterCustomEvent(self);
    shadow_ = nullptr;
}

bool KRRichTextView::ReuseEnable() {
    return true;
}

void KRRichTextView::SetRenderViewFrame(const KRRect &frame) {
    IKRRenderViewExport::SetRenderViewFrame(frame);
}

void KRRichTextView::OnCustomEvent(ArkUI_NodeCustomEvent *event, const ArkUI_NodeCustomEventType &event_type) {
    if (event_type == ARKUI_NODE_CUSTOM_EVENT_ON_FOREGROUND_DRAW) {
        OnForegroundDraw(event);
    }
}

void KRRichTextView::DidInit() {
    IKRRenderViewExport::DidInit();
}

void KRRichTextView::SetShadow(const std::shared_ptr<IKRRenderShadowExport> &shadow) {
    shadow_ = shadow;

    auto textShadow = std::dynamic_pointer_cast<KRRichTextShadow>(shadow);
    // 决策 6C：image span（由 PostProcessor("richtext") 拆段产生）只在 V1（老 typography）
    // OnForegroundDraw 路径下能被绘制——因为 V2 的 StyledString 是交给 ArkUI 节点直接
    // 渲染，SDK 当前没暴露插入图片绘制 hook 的入口。这个判定已收敛到
    // shadow->StyledStringEnabled()（含 image span 时返 false），本处只读一个结果。
    bool use_styled_string = textShadow && textShadow->StyledStringEnabled();
    bool has_image_span = textShadow && textShadow->HasImageSpans();
    if(use_styled_string){
        ArkUI_AttributeItem item;
        if(std::shared_ptr<KRParagraph> paragraph = std::dynamic_pointer_cast<KRRichTextShadow>(shadow)->GetParagraph()){
            item.object = paragraph->GetStyledString();
            kuikly::util::GetNodeApi()->setAttribute(GetNode(), NODE_TEXT_CONTENT_WITH_STYLED_STRING, &item);
            // Note:
            // The ownership of the styled string is not going to be transferred,
            // by setting the style item to the note by calling 
            // setAttribute with NODE_TEXT_CONTENT_WITH_STYLED_STRING.
            // Besides, it is not reference counted,
            // we need to make sure it is alive after setting it to the node.
            paragraph_ = paragraph;
        }
    }else {
        KREventDispatchCenter::GetInstance().RegisterCustomEvent(shared_from_this(), ARKUI_NODE_CUSTOM_EVENT_ON_FOREGROUND_DRAW);
        kuikly::util::GetNodeApi()->markDirty(GetNode(), NODE_NEED_RENDER);
    }
    // 注册 image span 解码完成回调：weak 引用 view，避免悬挂访问；解码完成在主线程被
    // 调用（shadow 端已用 KRMainThread::RunOnMainThread 切换），这里直接 markDirty 即可。
    if (textShadow && has_image_span) {
        std::weak_ptr<IKRRenderViewExport> weak_self = shared_from_this();
        textShadow->SetImageLoadedCallback(
            [weak_self](const std::string & /*uri*/) {
                auto strong = weak_self.lock();
                if (!strong) {
                    return;
                }
                kuikly::util::GetNodeApi()->markDirty(strong->GetNode(), NODE_NEED_RENDER);
            });
    } else if (textShadow) {
        textShadow->SetImageLoadedCallback(nullptr);
    }
}

void KRRichTextView::DidMoveToParentView() {
    IKRRenderViewExport::DidMoveToParentView();
    auto self = shared_from_this();
    KREventDispatchCenter::GetInstance().RegisterCustomEvent(self, ARKUI_NODE_CUSTOM_EVENT_ON_FOREGROUND_DRAW);
}

void KRRichTextView::DidRemoveFromParentView() {
    kuikly::util::GetNodeApi()->resetAttribute(GetNode(), NODE_TEXT_CONTENT_WITH_STYLED_STRING);
    IKRRenderViewExport::DidRemoveFromParentView();
    shadow_ = nullptr;
    paragraph_ = nullptr;
    last_draw_frame_width_ = -1.0;
}

void KRRichTextView::OnForegroundDraw(ArkUI_NodeCustomEvent *event) {
    if (shadow_ == nullptr && GetFrame().width == 0) {
        KR_LOG_ERROR << "OnForegroundDraw, shadow or frame not ready, shadow:" << shadow_.get()
                     << ", frame width:" << GetFrame().width;
        return;
    }
    if (auto rootView = GetRootView().lock()) {
        if (rootView->IsPerformMainTasking()) {
            std::weak_ptr<IKRRenderViewExport> weakSelf = shared_from_this();
            KRMainThread::RunOnMainThreadForNextLoop([weakSelf] {
                if(auto strongSelf = weakSelf.lock()){
                    kuikly::util::GetNodeApi()->markDirty(strongSelf->GetNode(), NODE_NEED_RENDER);
                }
            });
#ifndef NDEBUG
            KR_LOG_ERROR << "OnForegroundDraw, IsPerformMainTasking Skip:" << shadow_.get();
#endif
            return;
        }
    }
    auto richTextShadow = reinterpret_cast<KRRichTextShadow *>(shadow_.get());
    if (richTextShadow == nullptr) {
        KR_LOG_ERROR << "OnForegroundDraw, richTextShadow null";
        return;
    }
    // 拿一份 typography 的强引用并保持到本函数返回。这样即使主线程中途因
    // Kotlin 同步回调递归进入 ReleaseLastTypography()/SetMainThreadTypography(新)，
    // 被替换下来的旧 typography 也不会在本调用栈还在使用它的期间被销毁。
    KRTypographyHandle textTypoHandle = richTextShadow->MainThreadTypographyHandle();
    OH_Drawing_Typography *textTypo = textTypoHandle ? textTypoHandle.get() : nullptr;
    if (textTypo == nullptr) {
        KR_LOG_ERROR << "OnForegroundDraw, textTypo null, shadow:" << richTextShadow;
        return;
    }
    double drawOffsetY = richTextShadow->DrawOffsetY();
    OH_Drawing_TextAlign textAlign = richTextShadow->TextAlign();
    auto textTypoSize = richTextShadow->MainMeasureSize();
    // 在容器前景上绘制额外图形，实现图形显示在子组件之上。
    auto *drawContext = OH_ArkUI_NodeCustomEvent_GetDrawContextInDraw(event);
    auto *drawingHandle = reinterpret_cast<OH_Drawing_Canvas *>(OH_ArkUI_DrawContext_GetCanvas(drawContext));
    auto frameWidth = GetFrame().width;
    bool needReLayout = false;
    if (last_draw_frame_width_ > 0 && fabs(last_draw_frame_width_ - frameWidth) > 0.01) {
        needReLayout = true;
    }
    if (fabs(textTypoSize.width - frameWidth) > 1 || textAlign != TEXT_ALIGN_LEFT) {
        needReLayout = true;
    }
    if (needReLayout) {
        auto dpi = KRConfig::GetDpi();
        OH_Drawing_TypographyLayout(textTypo, frameWidth * dpi);
        last_draw_frame_width_ = frameWidth;
        if (textAlign != TEXT_ALIGN_LEFT) {
            richTextShadow->ResetTextAlign();
        }
    }
    
    if(OH_Drawing_TextLinePaint && line_break_margin_ > 0 && richTextShadow->DidExceedMaxLines()){
        auto text_lines = richTextShadow->GetTextLines();
        size_t line_count = OH_Drawing_GetDrawingArraySize(text_lines);
        for(int i = 0; i < line_count; ++i){
            OH_Drawing_TextLine* line = OH_Drawing_GetTextLineByIndex(text_lines, i);

            if(i + 1 == line_count && richTextShadow->DidExceedMaxLines()){
                OH_Drawing_TextLine *truncated_text_line = OH_Drawing_TextLineCreateTruncatedLine(line, (frameWidth - line_break_margin_) * KRConfig::GetDpi(), ELLIPSIS_MODAL_TAIL, "...");
                OH_Drawing_TextLinePaint( truncated_text_line, drawingHandle, 0, -drawOffsetY);
                OH_Drawing_DestroyTextLine(truncated_text_line);
            }else{
                OH_Drawing_TextLinePaint( line, drawingHandle, 0, -drawOffsetY);
            }
        }
        if(line_count > 0){
            return;
        }
    }
    // fallback
    OH_Drawing_TypographyPaint(textTypo, drawingHandle, 0, -drawOffsetY);

    // ===== Phase 4: 绘制由 PostProcessor("richtext") 产生的 image span =====
    // 时机：紧跟 OH_Drawing_TypographyPaint 之后；图片画在文字上层是预期效果——表情
    // 通常带轻微阴影/抗锯齿，需要覆盖排版底色。坐标系：placeholder rect 为 px（已乘 dpi），
    // canvas 同样 px；drawOffsetY 也是 px，保持与 TypographyPaint 一致即可。
    // 业务声明的 ImageSpan（spanPropsMap 自带 placeholderWidth）不在 image_draw_records_
    // 中，不会被本块绘制——它们继续走"父节点 ImageView" 老链路。
    const auto &image_records = richTextShadow->GetImageDrawRecords();
    if (!image_records.empty()) {
        OH_Drawing_TextBox *placeholder_rects = OH_Drawing_TypographyGetRectsForPlaceholders(textTypo);
        if (placeholder_rects) {
            int rect_count = OH_Drawing_GetSizeOfTextBox(placeholder_rects);
            // 复用一份 sampling options：emoji 缩放优先 LINEAR，避免最近邻产生锯齿。
            OH_Drawing_SamplingOptions *sampling = OH_Drawing_SamplingOptionsCreate(
                FILTER_MODE_LINEAR, MIPMAP_MODE_NONE);
            for (const auto &rec : image_records) {
                if (rec.placeholder_index < 0 || rec.placeholder_index >= rect_count) {
                    continue;
                }
                // 持有 shared_ptr 强引用：在本地变量 pm_holder 存活期间，即便此刻被并发
                // Evict / TrimLocked 从 cache 中移除，底层 pixmap 仍保活直到本作用域结束
                // ——彻底消除原本"裸指针 + 锁外使用"的悬挂访问风险。
                auto pm_holder = KRCustomEmojiPixmapCache::GetInstance().Get(rec.src_uri);
                OH_PixelmapNative *pm = pm_holder.get();
                if (!pm) {
                    // 解码尚未就绪：本帧空过；KRCustomEmojiPixmapCache 解码完成后会通过
                    // ImageLoadedCallback 跳转到 view 这里的 markDirty 触发下一帧重绘。
                    // 第一次绘制 → 解码 → markDirty → 第二次绘制命中本路径，体感几乎无延迟。
                    continue;
                }
                OH_Drawing_PixelMap *draw_pm = OH_Drawing_PixelMapGetFromOhPixelMapNative(pm);
                if (!draw_pm) {
                    continue;
                }
                // 取 placeholder 在 typography 内的 px 矩形，注意：drawOffsetY 与
                // TypographyPaint 时一致（向上平移）。
                float left = OH_Drawing_GetLeftFromTextBox(placeholder_rects, rec.placeholder_index);
                float top = OH_Drawing_GetTopFromTextBox(placeholder_rects, rec.placeholder_index);
                float right = OH_Drawing_GetRightFromTextBox(placeholder_rects, rec.placeholder_index);
                float bottom = OH_Drawing_GetBottomFromTextBox(placeholder_rects, rec.placeholder_index);
                // src 矩形：整张图（pixmap 原始像素尺寸）。
                OH_Pixelmap_ImageInfo *info = nullptr;
                OH_PixelmapImageInfo_Create(&info);
                uint32_t img_w = 0, img_h = 0;
                if (info && OH_PixelmapNative_GetImageInfo(pm, info) == IMAGE_SUCCESS) {
                    OH_PixelmapImageInfo_GetWidth(info, &img_w);
                    OH_PixelmapImageInfo_GetHeight(info, &img_h);
                }
                if (info) {
                    OH_PixelmapImageInfo_Release(info);
                }
                if (img_w == 0 || img_h == 0) {
                    // 没拿到尺寸时不绘制，避免 src=(0,0,0,0) 导致的未定义行为。
                    OH_Drawing_PixelMapDissolve(draw_pm);
                    continue;
                }
                OH_Drawing_Rect *src = OH_Drawing_RectCreate(0, 0, static_cast<float>(img_w),
                                                              static_cast<float>(img_h));
                OH_Drawing_Rect *dst = OH_Drawing_RectCreate(left, top - drawOffsetY,
                                                              right, bottom - drawOffsetY);
                OH_Drawing_CanvasDrawPixelMapRect(drawingHandle, draw_pm, src, dst, sampling);
                OH_Drawing_RectDestroy(src);
                OH_Drawing_RectDestroy(dst);
                // PixelMapGetFromOhPixelMapNative 返回的 OH_Drawing_PixelMap 与 native pixmap
                // 形成关联；按 SDK 规范，使用完毕后必须 Dissolve 解除关联，但 native 对象
                // 由 KRCustomEmojiPixmapCache（进程级单例）持续持有，不在此 release。
                OH_Drawing_PixelMapDissolve(draw_pm);
            }
            if (sampling) {
                OH_Drawing_SamplingOptionsDestroy(sampling);
            }
            OH_Drawing_TypographyDestroyTextBox(placeholder_rects);
        }
    }
}

void KRRichTextView::ToSetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                               const KRRenderCallback event_callback) {
    if (kuikly::util::isEqual(prop_key, kPropNameClick)) {
        std::weak_ptr<KRRichTextView> weakSelf = std::dynamic_pointer_cast<KRRichTextView>(shared_from_this());
        KRRenderCallback middleManCallback = [weakSelf, event_callback](KRAnyValue res) {
            auto strongSelf = weakSelf.lock();
            if(strongSelf == nullptr){
                return;
            }
            if (res->isMap()) {
                const auto oldParam = res->toMap();
                const auto x = oldParam.find("x");
                const auto y = oldParam.find("y");

                KRRenderValueMap params;
                if (x != oldParam.end()) {
                    params["x"] = x->second;
                }

                if (y != oldParam.end()) {
                    params["y"] = y->second;
                }

                const auto pageX = oldParam.find("pageX");
                const auto pageY = oldParam.find("pageY");
                if (pageX != oldParam.end()) {
                    params["pageX"] = pageX->second;
                }
                if (pageY != oldParam.end()) {
                    params["pageY"] = pageY->second;
                }

                if (auto richTextShadow = std::dynamic_pointer_cast<KRRichTextShadow>(strongSelf->shadow_)) {
                    int index = richTextShadow->SpanIndexAt(x->second->toFloat(), y->second->toFloat());
                    if (index < 0) {
                        index = 0;
                    }
                    params["index"] = NewKRRenderValue(index);
                }
                event_callback(NewKRRenderValue(params));
            } else {
                event_callback(res);
            }
        };
        IKRRenderViewExport::ToSetProp(prop_key, prop_value, middleManCallback);
    } else if(prop_key == kPropNameLineBreakMargin) {
        line_break_margin_ = prop_value->toFloat();
    }else {
        IKRRenderViewExport::ToSetProp(prop_key, prop_value, event_callback);
    } 
}

