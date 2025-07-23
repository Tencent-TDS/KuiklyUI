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

#ifndef CORE_RENDER_OHOS_KRCONVERTUTIL_H
#define CORE_RENDER_OHOS_KRCONVERTUTIL_H

#include <arkui/native_type.h>
#include <native_drawing/drawing_text_typography.h>
#include <string>
#include "libohos_render/foundation/KRBorderRadiuses.h"
#include "libohos_render/foundation/KRSize.h"
#include "libohos_render/foundation/type/KRRenderValue.h"

namespace kuikly {
namespace util {

ArkUI_EnterKeyType ConvertToEnterKeyType(const std::string &enter_key_type);

ArkUI_TextInputType ConvertToInputType(const std::string &input_type);

std::u32string ConvertToU32String(const std::string &input);

std::string ConvertToNormalString(const std::u32string &input);

ArkUI_TextAlignment ConvertToArkUITextAlign(const std::string &textAlign);

OH_Drawing_TextAlign ConvertToTextAlign(const std::string &textAlign);

OH_Drawing_TextDecoration ConvertToTextDecoration(const std::string &textDecoration);

OH_Drawing_EllipsisModal ConvertToTextBreakMode(const std::string &breakeMode);

OH_Drawing_FontStyle ConvertToFontStyle(const std::string &fontStyle);

uint32_t ConvertToHexColor(const std::string &colorStr);

ArkUI_BorderStyle ConverToBorderStyle(const std::string &string);

float ConvertToDouble(const std::string &string);

std::tuple<float, float, float, float> ToArgb(const std::string &color_str);

std::string ConvertDoubleToString(double value);

std::vector<std::string> ConvertSplit(const std::string &str, const std::string &delimiters);

OH_Drawing_FontWeight ConvertFontWeight(int fontWeight);

ArkUI_FontWeight ConvertArkUIFontWeight(int fontWeight);

std::string ConvertSizeToString(const KRSize &size);

KRBorderRadiuses ConverToBorderRadiuses(const std::string &borderRadiusString);

}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRCONVERTUTIL_H
