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

#include "KRCanvasView.h"

#include <native_drawing/drawing_bitmap.h>
#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_canvas.h>
#include <native_drawing/drawing_path.h>
#include <native_drawing/drawing_path_effect.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_rect.h>
#include <native_drawing/drawing_shader_effect.h>
#include <native_drawing/drawing_types.h>
#include <native_drawing/drawing_matrix.h>

#include "libohos_render/utils/KRColor.h"
#include "libohos_render/utils/KRJSONObject.h"

static constexpr std::string_view LINE_CAP = "lineCap";
static constexpr std::string_view LINE_WIDTH = "lineWidth";
static constexpr std::string_view LINE_DASH = "lineDash";
static constexpr std::string_view STROKE_STYLE = "strokeStyle";
static constexpr std::string_view FILL_STYLE = "fillStyle";
static constexpr std::string_view BEGIN_PATH = "beginPath";
static constexpr std::string_view MOVE_TO = "moveTo";
static constexpr std::string_view LINE_TO = "lineTo";
static constexpr std::string_view ARC = "arc";
static constexpr std::string_view CLOSE_PATH = "closePath";
static constexpr std::string_view STROKE = "stroke";
static constexpr std::string_view FILL = "fill";
static constexpr std::string_view CREATE_LINEAR_GRADIENT = "createLinearGradient";
static constexpr std::string_view QUADRATIC_CURVE_TO = "quadraticCurveTo";
static constexpr std::string_view TEXT_ALIGN = "textAlign";
static constexpr std::string_view FONT = "font";
static constexpr std::string_view FILL_TEXT = "fillText";
static constexpr std::string_view STROKE_TEXT = "strokeText";
static constexpr std::string_view BEZIER_CURVE_TO = "bezierCurveTo";
static constexpr std::string_view RESET = "reset";
static constexpr std::string_view LINEAR_GRADIENT = "linear-gradient";
static constexpr std::string_view SAVE = "save";
static constexpr std::string_view SAVE_LAYER = "saveLayer";
static constexpr std::string_view RESTORE = "restore";
static constexpr std::string_view CLIP = "clip";
static constexpr std::string_view TRANSLATE = "translate";
static constexpr std::string_view SCALE = "scale";
static constexpr std::string_view ROTATE = "rotate";
static constexpr std::string_view SKEW = "skew";
static constexpr std::string_view TRANSFORM = "transform";

KRCanvasView::KRCanvasView()
    : KRView(), cachable_methods_({LINE_CAP, LINE_WIDTH, LINE_DASH, STROKE_STYLE, FILL_STYLE, BEGIN_PATH, MOVE_TO,
                                   LINE_TO, ARC, CLOSE_PATH, STROKE, FILL, CREATE_LINEAR_GRADIENT, QUADRATIC_CURVE_TO,
                                   TEXT_ALIGN, FONT, FILL_TEXT, STROKE_TEXT, BEZIER_CURVE_TO, SAVE, SAVE_LAYER,
                                   RESTORE, CLIP, TRANSLATE, SCALE, ROTATE, SKEW, TRANSFORM}) {
    // ctor body left blank
}
void KRCanvasView::DidMoveToParentView() {
    KRView::DidMoveToParentView();
    auto self = shared_from_this();
    KREventDispatchCenter::GetInstance().RegisterCustomEvent(self, ARKUI_NODE_CUSTOM_EVENT_ON_DRAW);
}
void KRCanvasView::DidInit() {
    IKRRenderViewExport::DidInit();
}

bool KRCanvasView::ShouldCacheOp(const std::string &method) {
    return cachable_methods_.find(method) != cachable_methods_.end();
}

bool KRCanvasView::MarkDirtyIfNeeded(const std::string &method) {
    if (method == STROKE || method == FILL || method == RESET) {
        kuikly::util::GetNodeApi()->markDirty(GetNode(), NODE_NEED_RENDER);
        return true;
    }
    return false;
}

void KRCanvasView::CallMethod(const std::string &method, const KRAnyValue &params, const KRRenderCallback &cb) {
    if (ShouldCacheOp(method)) {
        AddOp(method, params);
    } else if (method == RESET) {
        Reset();
    } else {
        KRView::CallMethod(method, params, cb);
        return;
    }
    MarkDirtyIfNeeded(method);
}

void KRCanvasView::OnCustomEvent(ArkUI_NodeCustomEvent *event, const ArkUI_NodeCustomEventType &event_type) {
    KRView::OnCustomEvent(event, event_type);

    auto type = OH_ArkUI_NodeCustomEvent_GetEventType(event);
    if (type == ArkUI_NodeCustomEventType::ARKUI_NODE_CUSTOM_EVENT_ON_DRAW) {
        OnDraw(event);
    }
}

void KRCanvasView::SetLineCap(const std::string &params) {
    auto obj = kuikly::util::JSONObject::Parse(params);
    std::string str = obj->GetString("style");
    OH_Drawing_PenLineCapStyle style = LINE_FLAT_CAP;
    if (str == "round") {
        style = LINE_ROUND_CAP;
    } else if (str == "square") {
        style = LINE_SQUARE_CAP;
    }
    if (pen_ == nullptr) {
        pen_ = OH_Drawing_PenCreate();
    }
    OH_Drawing_PenSetCap(pen_, style);
}

void KRCanvasView::SetLineWidth(const std::string &params) {
    auto obj = kuikly::util::JSONObject::Parse(params);
    float width = obj->GetNumber("width");

    if (pen_ == nullptr) {
        pen_ = OH_Drawing_PenCreate();
    }
    OH_Drawing_PenSetWidth(pen_, width);
}

void KRCanvasView::SetLineDash(const std::string &params) {
    auto obj = kuikly::util::JSONObject::Parse(params);
    auto intervalsVector = obj->GetNumberArray("intervals");
    int count = intervalsVector.size();
    if (pen_ == nullptr) {
        pen_ = OH_Drawing_PenCreate();
    }
    if (count == 0) {
        OH_Drawing_PenSetPathEffect(pen_, nullptr);
        return;
    }
    std::vector<float> intervals(intervalsVector.begin(), intervalsVector.end());
    auto pathEffect = OH_Drawing_CreateDashPathEffect(intervals.data(), count, 0);
    OH_Drawing_PenSetPathEffect(pen_, pathEffect);
    OH_Drawing_PathEffectDestroy(pathEffect);
}

void processColorStops(const std::string &colorStopsStr, std::vector<uint32_t> &colors, std::vector<float> &locations) {
    std::vector<std::string> splits = kuikly::util::ConvertSplit(colorStopsStr, ",");

    for (const auto &colorStopStr : splits) {
        if (colorStopStr.empty()) {
            continue;
        }
        std::vector<std::string> colorAndStop = kuikly::util::ConvertSplit(colorStopStr, " ");
        if (colorAndStop.size() < 2) {
            continue;
        }
        colors.push_back(kuikly::util::ConvertToHexColor(colorAndStop[0]));
        locations.push_back(std::stof(colorAndStop[1]));
    }
}

OH_Drawing_ShaderEffect *parseGradientStyle(const std::string &style) {
    auto paramObj = kuikly::util::JSONObject::Parse(style.substr(LINEAR_GRADIENT.size()));
    OH_Drawing_ShaderEffect *colorShaderEffect = nullptr;
    if (paramObj) {
        float x0 = paramObj->GetNumber("x0");
        float y0 = paramObj->GetNumber("y0");
        float x1 = paramObj->GetNumber("x1");
        float y1 = paramObj->GetNumber("y1");
        const std::string colorStopsStr = paramObj->GetString("colorStops");

        std::vector<uint32_t> colors;
        std::vector<float> locations;
        processColorStops(colorStopsStr, colors, locations);

        // 开始点
        OH_Drawing_Point *startPt = OH_Drawing_PointCreate(x0, y0);
        // 结束点
        OH_Drawing_Point *endPt = OH_Drawing_PointCreate(x1, y1);
        // 创建线性渐变着色器效果
        colorShaderEffect = OH_Drawing_ShaderEffectCreateLinearGradient(startPt, endPt, colors.data(), locations.data(),
                                                                        colors.size(), OH_Drawing_TileMode::CLAMP);
        OH_Drawing_PointDestroy(startPt);
        OH_Drawing_PointDestroy(endPt);
    }
    return colorShaderEffect;
}

void KRCanvasView::SetStrokeStyle(const std::string &params) {
    auto paramObj = kuikly::util::JSONObject::Parse(params);
    if (paramObj) {
        const std::string style = paramObj->GetString("style");
        if (style.substr(0, LINEAR_GRADIENT.size()) == LINEAR_GRADIENT) {
            OH_Drawing_ShaderEffect *colorShaderEffect = parseGradientStyle(style);
            if (pen_ == nullptr) {
                pen_ = OH_Drawing_PenCreate();
            }
            OH_Drawing_PenSetShaderEffect(pen_, colorShaderEffect);
        } else {
            if (pen_ == nullptr) {
                pen_ = OH_Drawing_PenCreate();
            }
            OH_Drawing_PenSetShaderEffect(pen_, nullptr);
            OH_Drawing_PenSetColor(pen_, kuikly::util::ConvertToHexColor(style));
        }
    }
}
void KRCanvasView::SetFillStyle(const std::string &params) {
    auto paramObj = kuikly::util::JSONObject::Parse(params);
    if (paramObj) {
        const std::string style = paramObj->GetString("style");
        if (style.substr(0, LINEAR_GRADIENT.size()) == LINEAR_GRADIENT) {
            OH_Drawing_ShaderEffect *colorShaderEffect = parseGradientStyle(style);
            if (brush_ == nullptr) {
                brush_ = OH_Drawing_BrushCreate();
            }
            OH_Drawing_BrushSetShaderEffect(brush_, colorShaderEffect);
        } else {
            if (brush_ == nullptr) {
                brush_ = OH_Drawing_BrushCreate();
            }
            OH_Drawing_BrushSetShaderEffect(brush_, nullptr);
            kuikly::graphics::Color color = kuikly::graphics::Color::FromString(style);
            OH_Drawing_BrushSetColor(brush_, color.value);
        }
    }
}

void KRCanvasView::BeginPath() {
    if (drawingPath_) {
        OH_Drawing_PathDestroy(drawingPath_);
    }
    drawingPath_ = OH_Drawing_PathCreate();
}

void KRCanvasView::MoveTo(const std::string &params) {
    if (drawingPath_ == nullptr) {
        return;
    }
    auto obj = kuikly::util::JSONObject::Parse(params);
    float x = obj->GetNumber("x");
    float y = obj->GetNumber("y");

    OH_Drawing_PathMoveTo(drawingPath_, x, y);
}

void KRCanvasView::LineTo(const std::string &params) {
    if (drawingPath_ == nullptr) {
        return;
    }
    auto obj = kuikly::util::JSONObject::Parse(params);
    float x = obj->GetNumber("x");
    float y = obj->GetNumber("y");
    OH_Drawing_PathLineTo(drawingPath_, x, y);
}

void KRCanvasView::Arc(const std::string &params) {
    if (drawingPath_ == nullptr) {
        return;
    }
    auto paramObj = kuikly::util::JSONObject::Parse(params);
    if (paramObj) {
        float x = paramObj->GetNumber("x");
        float y = paramObj->GetNumber("y");
        float r = paramObj->GetNumber("r");
        float startAngle = paramObj->GetNumber("sAngle") * 180 / M_PI;
        float endAngle = paramObj->GetNumber("eAngle") * 180 / M_PI;
        bool ccw = paramObj->GetNumber("counterclockwise");
        float sweepAngle = endAngle - startAngle;
        if (ccw) {
            if (sweepAngle > 0) {
                sweepAngle = std::fmod(sweepAngle, 360) - 360;
            }
        } else {
            if (sweepAngle < 0) {
                sweepAngle = std::fmod(sweepAngle, 360) + 360;
            }
        }
        if (std::fabs(sweepAngle) < 360) {
            OH_Drawing_PathArcTo(drawingPath_, x - r, y - r, x + r, y + r, startAngle, sweepAngle);
        } else {
            // TODO(userName):
        }
    }
}

void KRCanvasView::ClosePath() {
    if (drawingPath_) {
        OH_Drawing_PathClose(drawingPath_);
    }
}

void KRCanvasView::Stroke() {
    if (canvas_ && drawingPath_) {
        if (pen_) {
            OH_Drawing_CanvasAttachPen(canvas_, pen_);
        }
        OH_Drawing_CanvasDrawPath(canvas_, drawingPath_);
        if (pen_) {
            OH_Drawing_CanvasDetachPen(canvas_);
        }
    }
}

void KRCanvasView::Fill() {
    if (canvas_ && drawingPath_) {
        if (brush_) {
            OH_Drawing_CanvasAttachBrush(canvas_, brush_);
        }
        OH_Drawing_CanvasDrawPath(canvas_, drawingPath_);
        if (brush_) {
            OH_Drawing_CanvasDetachBrush(canvas_);
        }
    }
}

void KRCanvasView::SetTextAlign(const std::string &params) {
    if (params == "left") {
        text_feature_.textAlign = TEXT_ALIGN_LEFT;
    } else if (params == "center") {
        text_feature_.textAlign = TEXT_ALIGN_CENTER;
    } else if (params == "right") {
        text_feature_.textAlign = TEXT_ALIGN_RIGHT;
    }
}

void KRCanvasView::SetFont(const std::string &params) {
    auto paramObj = kuikly::util::JSONObject::Parse(params);
    auto size = paramObj->GetNumber("size");
    auto style = paramObj->GetString("style");
    auto weight = std::stoi(paramObj->GetString("weight"));
    auto family = paramObj->GetString("family");

    text_feature_.fontSize = size;
    text_feature_.fontStyle = kuikly::util::ConvertToFontStyle(style);
    text_feature_.fontWeight = kuikly::util::ConvertFontWeight(weight);
    text_feature_.fontFamily = family;
}

void KRCanvasView::FillText(const std::string &params) {
    if (canvas_) {
        std::shared_ptr<struct KRFontCollectionWrapper> font_collection_wrapper =
            std::make_shared<KRFontCollectionWrapper>();
        DrawText(params, font_collection_wrapper, FILL_TEXT);
    }
}

void KRCanvasView::StrokeText(const std::string &params) {
    if (canvas_) {
        std::shared_ptr<struct KRFontCollectionWrapper> font_collection_wrapper =
            std::make_shared<KRFontCollectionWrapper>();
        DrawText(params, font_collection_wrapper, STROKE_TEXT);
    }
}

void KRCanvasView::DrawText(std::string params, std::shared_ptr<struct KRFontCollectionWrapper> wrapper,
                            std::string_view type) {
    OH_Drawing_TextStyle *txtStyle = OH_Drawing_CreateTextStyle();
    // 设置文字大小、字重等属性
    float fontSizeScale = 1;
    auto rootView = GetRootView().lock();
    if (rootView == nullptr) {
        return;
    }
    if (auto context = rootView->GetContext()) {
        fontSizeScale = context->Config()->GetFontSizeScale();
    }
    // 这里fontSize不用 * dpi 因为画布已经整体缩放
    double fontSize = text_feature_.fontSize * fontSizeScale;
    OH_Drawing_SetTextStyleFontSize(txtStyle, fontSize);
    OH_Drawing_SetTextStyleFontWeight(txtStyle, text_feature_.fontWeight);
    OH_Drawing_SetTextStyleBaseLine(txtStyle, TEXT_BASELINE_ALPHABETIC);
    OH_Drawing_SetTextStyleFontHeight(txtStyle, 1);
    OH_Drawing_SetTextStyleFontStyle(txtStyle, text_feature_.fontStyle);
    OH_Drawing_SetTextStyleLocale(txtStyle, "en");

    // 自定义字体
    auto fontAdapters = KRFontAdapterManager::GetInstance()->AllAdapters();
    if (!text_feature_.fontFamily.empty()) {
        const char *fontFamilyPtr = text_feature_.fontFamily.c_str();
        const char *fontFamilies[] = {fontFamilyPtr};
        OH_Drawing_SetTextStyleFontFamilies(txtStyle, 1, fontFamilies);
        auto nativeResMgr = rootView->GetNativeResourceManager();
        SetCustomFontIfApplicable(nativeResMgr, wrapper, text_feature_.fontFamily, fontAdapters);
    }

    OH_Drawing_TypographyStyle *typoStyle = OH_Drawing_CreateTypographyStyle();
    OH_Drawing_SetTypographyTextDirection(typoStyle, TEXT_DIRECTION_LTR);
    // 使用左对齐
    OH_Drawing_SetTypographyTextAlign(typoStyle, TEXT_ALIGN_LEFT);

    if (type == FILL_TEXT) {
        if (brush_ == nullptr) {
            brush_ = OH_Drawing_BrushCreate();
        }
        OH_Drawing_SetTextStyleForegroundBrush(txtStyle, brush_);
    } else if (type == STROKE_TEXT) {
        if (pen_ == nullptr) {
            pen_ = OH_Drawing_PenCreate();
        }
        OH_Drawing_SetTextStyleForegroundPen(txtStyle, pen_);
    }

    auto paramObj = kuikly::util::JSONObject::Parse(params);
    auto text = paramObj->GetString("text");
    auto x = paramObj->GetNumber("x");
    auto y = paramObj->GetNumber("y");

    OH_Drawing_TypographyCreate *handler = OH_Drawing_CreateTypographyHandler(typoStyle, wrapper->fontCollection);
    OH_Drawing_TypographyHandlerPushTextStyle(handler, txtStyle);
    // 设置文字内容
    OH_Drawing_TypographyHandlerAddText(handler, text.c_str());
    OH_Drawing_TypographyHandlerPopTextStyle(handler);
    OH_Drawing_Typography *typography = OH_Drawing_CreateTypography(handler);
    // 设置页面最大宽度
    auto default_width = 10000000;  // 无限宽
    double maxWidth = default_width;
    OH_Drawing_TypographyLayout(typography, maxWidth);

    auto textWidth = OH_Drawing_TypographyGetLongestLine(typography);
    auto baseLineHeight = OH_Drawing_TypographyGetAlphabeticBaseline(typography);
    // 根据对齐方式计算实际位置
    double left = 0;
    if (text_feature_.textAlign == TEXT_ALIGN_CENTER) {
        left = textWidth / 2;
    } else if (text_feature_.textAlign == TEXT_ALIGN_RIGHT) {
        left = textWidth;
    } else {
        left = 0;
    }

    x -= left;
    // 修改y为baseLine在屏幕上的位置
    y -= baseLineHeight;

    double position[2] = {x, y};
    OH_Drawing_TypographyPaint(typography, canvas_, position[0], position[1]);
    // 释放变量
    OH_Drawing_DestroyTypography(typography);
    OH_Drawing_DestroyTypographyHandler(handler);
    OH_Drawing_DestroyTypographyStyle(typoStyle);
    OH_Drawing_DestroyTextStyle(txtStyle);
}

void KRCanvasView::QuadraticCurveTo(const std::string &params) {
    if (drawingPath_ == nullptr) {
        return;
    }
    auto obj = kuikly::util::JSONObject::Parse(params);
    float cpx = obj->GetNumber("cpx");
    float cpy = obj->GetNumber("cpy");
    float x = obj->GetNumber("x");
    float y = obj->GetNumber("y");
    OH_Drawing_PathQuadTo(drawingPath_, cpx, cpy, x, y);
}

void KRCanvasView::BezierCurveTo(const std::string &params) {
    if (drawingPath_ == nullptr) {
        return;
    }
    auto obj = kuikly::util::JSONObject::Parse(params);
    float cp1x = obj->GetNumber("cp1x");
    float cp1y = obj->GetNumber("cp1y");
    float cp2x = obj->GetNumber("cp2x");
    float cp2y = obj->GetNumber("cp2y");
    float x = obj->GetNumber("x");
    float y = obj->GetNumber("y");
    OH_Drawing_PathCubicTo(drawingPath_, cp1x, cp1y, cp2x, cp2y, x, y);
}

void KRCanvasView::Save() {
    if (canvas_) {
        OH_Drawing_CanvasSave(canvas_);
    }
}

void KRCanvasView::SaveLayer(const std::string &params) {
    if (canvas_) {
        auto obj = kuikly::util::JSONObject::Parse(params);
        float x = obj->GetNumber("x");
        float y = obj->GetNumber("y");
        float width = obj->GetNumber("width");
        float height = obj->GetNumber("height");
        OH_Drawing_Rect *rect = OH_Drawing_RectCreate(x, y, x + width, y + height);
        OH_Drawing_CanvasSaveLayer(canvas_, rect, brush_);
        OH_Drawing_RectDestroy(rect);
    }
}

void KRCanvasView::Restore() {
    if (canvas_) {
        OH_Drawing_CanvasRestore(canvas_);
    }
}

void KRCanvasView::clip(const std::string &params) {
    if (canvas_ && drawingPath_) {
        auto obj = kuikly::util::JSONObject::Parse(params);
        auto op = obj->GetNumber("intersect") ? OH_Drawing_CanvasClipOp::INTERSECT
                                              : OH_Drawing_CanvasClipOp::DIFFERENCE;
        OH_Drawing_CanvasClipPath(canvas_, drawingPath_, op, true);
    }
}

void KRCanvasView::Translate(const std::string &params) {
    if (canvas_) {
        auto obj = kuikly::util::JSONObject::Parse(params);
        float x = obj->GetNumber("x");
        float y = obj->GetNumber("y");
        OH_Drawing_CanvasTranslate(canvas_, x, y);
    }
}

void KRCanvasView::Scale(const std::string &params) {
    if (canvas_) {
        auto obj = kuikly::util::JSONObject::Parse(params);
        float x = obj->GetNumber("x");
        float y = obj->GetNumber("y");
        OH_Drawing_CanvasScale(canvas_, x, y);
    }
}

void KRCanvasView::Rotate(const std::string &params) {
    if (canvas_) {
        auto obj = kuikly::util::JSONObject::Parse(params);
        float degrees = obj->GetNumber("angle") * 180 / M_PI;
        OH_Drawing_CanvasRotate(canvas_, degrees, 0, 0);
    }
}

void KRCanvasView::Skew(const std::string &params) {
    if (canvas_) {
        auto obj = kuikly::util::JSONObject::Parse(params);
        float x = obj->GetNumber("x");
        float y = obj->GetNumber("y");
        OH_Drawing_CanvasSkew(canvas_, x, y);
    }
}

void KRCanvasView::Transform(const std::string &params) {
    if (canvas_) {
        auto obj = kuikly::util::JSONObject::Parse(params);
        auto values = obj->GetNumberArray("values");
        if (values.size() < 9) {
            return;
        }
        auto matrix = OH_Drawing_MatrixCreate();
        OH_Drawing_MatrixSetMatrix(matrix,
                                   values[0], values[1], values[2],
                                   values[3], values[4], values[5],
                                   values[6], values[7], values[8]);
        OH_Drawing_CanvasConcatMatrix(canvas_, matrix);
        OH_Drawing_MatrixDestroy(matrix);
    }
}

void KRCanvasView::Reset() {
    ops_.clear();

    if (drawingPath_) {
        OH_Drawing_PathDestroy(drawingPath_);
        drawingPath_ = nullptr;
    }
    if (pen_) {
        OH_Drawing_PenDestroy(pen_);
        pen_ = nullptr;
    }
    if (brush_) {
        OH_Drawing_BrushDestroy(brush_);
        brush_ = nullptr;
    }
}

void KRCanvasView::AddOp(const std::string &method, const KRAnyValue &params) {
    ops_.push_back(std::pair{method, params->toString()});
}

void KRCanvasView::OnDraw(ArkUI_NodeCustomEvent *event) {
    auto drawContext = OH_ArkUI_NodeCustomEvent_GetDrawContextInDraw(event);
    canvas_ = reinterpret_cast<OH_Drawing_Canvas *>(OH_ArkUI_DrawContext_GetCanvas(drawContext));

    float density = 1;
    if (auto root = GetRootView().lock()) {
        if (auto context = root->GetContext()) {
            density = context->Config()->GetDpi();
        }
    }

    OH_Drawing_CanvasScale(canvas_, density, density);
    // 设置可绘制区域为 Canvas 的整个布局区域
    auto frame = GetFrame();
    OH_Drawing_Rect *rect = OH_Drawing_RectCreate(0, 0, frame.width, frame.height);
    OH_Drawing_CanvasClipRect(canvas_, rect, OH_Drawing_CanvasClipOp::INTERSECT, true);
    OH_Drawing_RectDestroy(rect);

    std::for_each(ops_.begin(), ops_.end(), [this](auto item) {
        const std::string &method = item.first;
        const std::string &params = item.second;
        if (method == LINE_CAP) {
            SetLineCap(params);
        } else if (method == LINE_WIDTH) {
            SetLineWidth(params);
        } else if (method == LINE_DASH) {
            SetLineDash(params);
        } else if (method == STROKE_STYLE) {
            SetStrokeStyle(params);
        } else if (method == FILL_STYLE) {
            SetFillStyle(params);
        } else if (method == BEGIN_PATH) {
            BeginPath();
        } else if (method == MOVE_TO) {
            MoveTo(params);
        } else if (method == LINE_TO) {
            LineTo(params);
        } else if (method == ARC) {
            Arc(params);
        } else if (method == CLOSE_PATH) {
            ClosePath();
        } else if (method == STROKE) {
            Stroke();
        } else if (method == FILL) {
            Fill();
        } else if (method == CREATE_LINEAR_GRADIENT) {
            //
        } else if (method == QUADRATIC_CURVE_TO) {
            QuadraticCurveTo(params);
        } else if (method == TEXT_ALIGN) {
            SetTextAlign(params);
        } else if (method == FONT) {
            SetFont(params);
        } else if (method == FILL_TEXT) {
            FillText(params);
        } else if (method == STROKE_TEXT) {
            StrokeText(params);
        } else if (method == BEZIER_CURVE_TO) {
            BezierCurveTo(params);
        } else if (method == RESET) {
            Reset();
        } else if (method == SAVE) {
            Save();
        } else if (method == SAVE_LAYER) {
            SaveLayer(params);
        } else if (method == RESTORE) {
            Restore();
        } else if (method == CLIP) {
            clip(params);
        } else if (method == TRANSLATE) {
            Translate(params);
        } else if (method == SCALE) {
            Scale(params);
        } else if (method == ROTATE) {
            Rotate(params);
        } else if (method == SKEW) {
            Skew(params);
        } else if (method == TRANSFORM) {
            Transform(params);
        }
    });
}
