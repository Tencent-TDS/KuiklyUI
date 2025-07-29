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

#include "libohos_render/utils/KRConvertUtil.h"
#include <codecvt>
#include <iostream>
#include <locale>
#include <sstream>

namespace kuikly {
namespace util {

ArkUI_EnterKeyType ConvertToEnterKeyType(const std::string &enter_key_type) {
    if (enter_key_type == "search") {
        return ARKUI_ENTER_KEY_TYPE_SEARCH;
    }
    if (enter_key_type == "send") {
        return ARKUI_ENTER_KEY_TYPE_SEND;
    }
    if (enter_key_type == "go") {
        return ARKUI_ENTER_KEY_TYPE_GO;
    }
    if (enter_key_type == "done") {
        return ARKUI_ENTER_KEY_TYPE_DONE;
    }
    if (enter_key_type == "next") {
        return ARKUI_ENTER_KEY_TYPE_NEXT;
    }
    return ARKUI_ENTER_KEY_TYPE_DONE;
}

ArkUI_TextInputType ConvertToInputType(const std::string &input_type) {
    if (input_type == "password") {
        return ARKUI_TEXTINPUT_TYPE_PASSWORD;
    }
    if (input_type == "number") {
        return ARKUI_TEXTINPUT_TYPE_NUMBER;
    }
    if (input_type == "email") {
        return ARKUI_TEXTINPUT_TYPE_EMAIL;
    }
    return ARKUI_TEXTINPUT_TYPE_NORMAL;
}

std::u32string ConvertToU32String(const std::string &input) {
    std::wstring_convert<std::codecvt_utf8<char32_t>, char32_t> converter;
    return converter.from_bytes(input);
}

std::string ConvertToNormalString(const std::u32string &input) {
    std::wstring_convert<std::codecvt_utf8<char32_t>, char32_t> converter;
    return converter.to_bytes(input);
}

ArkUI_TextAlignment ConvertToArkUITextAlign(const std::string &textAlign) {
    if (textAlign == "center") {
        return ARKUI_TEXT_ALIGNMENT_CENTER;
    }
    if (textAlign == "left") {
        return ARKUI_TEXT_ALIGNMENT_START;
    }
    if (textAlign == "right") {
        return ARKUI_TEXT_ALIGNMENT_END;
    }
    return ARKUI_TEXT_ALIGNMENT_START;
    // OH_Drawing_SetTextStyleDecoration(OH_Drawing_TextStyle *, int)
}

OH_Drawing_TextAlign ConvertToTextAlign(const std::string &textAlign) {
    if (textAlign == "center") {
        return TEXT_ALIGN_CENTER;
    }
    if (textAlign == "left") {
        return TEXT_ALIGN_LEFT;
    }
    if (textAlign == "right") {
        return TEXT_ALIGN_RIGHT;
    }
    return TEXT_ALIGN_LEFT;
    // OH_Drawing_SetTextStyleDecoration(OH_Drawing_TextStyle *, int)
}

OH_Drawing_TextDecoration ConvertToTextDecoration(const std::string &textDecoration) {
    if (textDecoration == "underline") {
        return TEXT_DECORATION_UNDERLINE;
    }

    if (textDecoration == "line-through") {
        return TEXT_DECORATION_LINE_THROUGH;
    }
    return TEXT_DECORATION_NONE;
}

OH_Drawing_EllipsisModal ConvertToTextBreakMode(const std::string &breakeMode) {
    if (breakeMode == "middle") {
        return ELLIPSIS_MODAL_MIDDLE;
    }

    if (breakeMode == "head") {
        return ELLIPSIS_MODAL_HEAD;
    }

    return ELLIPSIS_MODAL_TAIL;
}

OH_Drawing_FontStyle ConvertToFontStyle(const std::string &fontStyle) {
    if (fontStyle == "italic") {
        return FONT_STYLE_ITALIC;
    }
    return FONT_STYLE_NORMAL;
}

uint32_t ConvertToHexColor(const std::string &colorStr) {
    try {
        auto color_adapter = KRRenderAdapterManager::GetInstance().GetColorAdapter();
        if (color_adapter) {
            std::int64_t hex = color_adapter->GetHexColor(colorStr);
            if (hex != -1) {
                return hex;
            }
        }
        uint32_t hex = std::stol(colorStr);
        return hex;
    } catch (...) {
    }
    return 0;
}

ArkUI_BorderStyle ConverToBorderStyle(const std::string &string) {
    if (string == "dotted") {
        return ARKUI_BORDER_STYLE_DOTTED;
    }
    if (string == "dashed") {
        return ARKUI_BORDER_STYLE_DASHED;
    }
    return ARKUI_BORDER_STYLE_SOLID;
}

float ConvertToDouble(const std::string &string) {
    if (string.length() == 1 && string == "0") {
        return 0;
    }
    auto value = std::make_shared<KRRenderValue>(string);
    return value->toFloat();
}

std::tuple<float, float, float, float> ToArgb(const std::string &color_str) {
    auto hex_color = ConvertToHexColor(color_str);
    float ratio = 255;
    auto a = static_cast<float>((hex_color >> 24) & 0xff) / ratio;
    auto r = static_cast<float>((hex_color >> 16) & 0xff) / ratio;
    auto g = static_cast<float>((hex_color >> 8) & 0xff) / ratio;
    auto b = static_cast<float>((hex_color >> 0) & 0xff) / ratio;
    return std::make_tuple(a, r, g, b);
}

std::string ConvertDoubleToString(double value) {
    std::ostringstream oss;
    oss << value;
    std::string str = oss.str();

    // 去除小数点后所有的零
    std::size_t last_not_zero = str.find_last_not_of('0');
    if (last_not_zero != std::string::npos) {
        if (str[last_not_zero] == '.') {
            last_not_zero--;  // 保留小数点前的数字
        }
        str = str.substr(0, last_not_zero + 1);
    }
    return str;
}

std::vector<std::string> ConvertSplit(const std::string &str, const std::string &delimiters) {
    std::vector<std::string> result;
    std::size_t start = 0;
    std::size_t end = str.find_first_of(delimiters);

    while (end != std::string::npos) {
        result.push_back(str.substr(start, end - start));
        start = end + 1;
        end = str.find_first_of(delimiters, start);
    }

    result.push_back(str.substr(start));
    return result;
}

OH_Drawing_FontWeight ConvertFontWeight(int fontWeight) {
    if (fontWeight == 200) {
        return FONT_WEIGHT_200;
    }
    if (fontWeight == 300) {
        return FONT_WEIGHT_300;
    }
    if (fontWeight == 400) {
        return FONT_WEIGHT_400;
    }
    if (fontWeight == 500) {
        return FONT_WEIGHT_500;
    }
    if (fontWeight == 600) {
        return FONT_WEIGHT_600;
    }
    if (fontWeight == 700) {
        return FONT_WEIGHT_700;
    }
    if (fontWeight > 700) {
        return FONT_WEIGHT_800;
    }
    return FONT_WEIGHT_400;
}

ArkUI_FontWeight ConvertArkUIFontWeight(int fontWeight) {
    if (fontWeight < 200) {
        return ARKUI_FONT_WEIGHT_W100;
    }
    if (fontWeight < 300) {
        return ARKUI_FONT_WEIGHT_W200;
    }
    if (fontWeight < 400) {
        return ARKUI_FONT_WEIGHT_W300;
    }
    if (fontWeight < 500) {
        return ARKUI_FONT_WEIGHT_W400;
    }
    if (fontWeight < 600) {
        return ARKUI_FONT_WEIGHT_W500;
    }
    if (fontWeight < 700) {
        return ARKUI_FONT_WEIGHT_W600;
    }
    if (fontWeight < 800) {
        return ARKUI_FONT_WEIGHT_W700;
    }
    if (fontWeight < 900) {
        return ARKUI_FONT_WEIGHT_W800;
    }
    return ARKUI_FONT_WEIGHT_W900;
}

std::string ConvertSizeToString(const KRSize &size) {
    std::array<char, 50> buffer;
    std::snprintf(buffer.data(), buffer.size(), "%.2lf|%.2lf", size.width, size.height);
    return std::string(buffer.data());
}

KRBorderRadiuses ConverToBorderRadiuses(const std::string &borderRadiusString) {
    auto splits = ConvertSplit(borderRadiusString, ",");
    return KRBorderRadiuses(ConvertToDouble(splits[0]), ConvertToDouble(splits[1]), ConvertToDouble(splits[2]),
                            ConvertToDouble(splits[3]));
}

}  // namespace util
}  // namespace kuikly