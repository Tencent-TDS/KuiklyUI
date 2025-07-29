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

#ifndef CORE_RENDER_OHOS_KRCONFIG_H
#define CORE_RENDER_OHOS_KRCONFIG_H

#include <cassert>
#include <string>
#include "libohos_render/foundation/type/KRRenderValue.h"

class KRConfig {
 public:
    explicit KRConfig(const std::string &configJson) {
        Update(configJson);
    }

    void Update(const std::string &configJson) {
        auto configValue = std::make_shared<KRRenderValue>(configJson);
        auto map = configValue->toMap();
        auto vp2px = map.find("vp2px");
        if (vp2px != map.end()) {
            vp2px_ = vp2px->second->toFloat();
            GetDpi(vp2px_);
        }

        auto screen_width = map.find("screen_width");
        if (screen_width != map.end()) {
            screen_width_ = screen_width->second->toFloat();
        }

        auto screen_height = map.find("screen_height");
        if (screen_height != map.end()) {
            screen_height_ = screen_height->second->toFloat();
        }

        auto resfile_dir = map.find("resfile_dir");
        if (resfile_dir != map.end()) {
            resfile_dir_ = resfile_dir->second->toString();
        }

        auto files_dir = map.find("files_dir");
        if (files_dir != map.end()) {
            files_dir_ = files_dir->second->toString();
        }
        
        auto assets_dir = map.find("assets_dir");
        if (assets_dir != map.end()) {
            assets_dir_ = assets_dir->second->toString();
        }

        auto screenDensity = map.find("screenDensity");
        if (screenDensity != map.end()) {
            screenDensity_ = screenDensity->second->toFloat();
        }

        auto fontWeightScale = map.find("fontWeightScale");
        if (fontWeightScale != map.end()) {
            fontWeightScale_ = fontWeightScale->second->toFloat();
        }

        auto fontSizeScale = map.find("fontSizeScale");
        if (fontSizeScale != map.end()) {
            fontSizeScale_ = fontSizeScale->second->toFloat();
        }
    }

    /**
     * 获取dpi转换比例
     * @param firstInitDpi 首次初始化（外部调用不用传）
     * @return 返回1vp等于多少px的比例，一般为3.0
     */
    static double GetDpi(double firstInitDpi = 0) {
        static double gVp2px = 0;
        if (firstInitDpi != 0) {
            gVp2px = firstInitDpi;
        }
        if (gVp2px == 0) {
            assert(0);
        }
        return gVp2px;
    }

    float vp2px(float vp) {
        return vp * vp2px_;
    }

    float Px2Vp(float px) {
        return px / vp2px_;
    }

    const std::string &GetResfileDir() {
        return resfile_dir_;
    }

    const std::string &GetFilesDir() {
        return files_dir_;
    }
    
    const std::string &GetAssetsDir() {
        if (!assets_dir_.empty()) {
            return assets_dir_;
        } else {
            return resfile_dir_;
        }
    }

    const float GetFontWeightScale() {
        return fontWeightScale_;
    }

    const float GetFontSizeScale() {
        return fontSizeScale_;
    }

    const float GetScreenDensity() {
        return screenDensity_;
    }

 private:
    float vp2px_ = 0;
    float fontWeightScale_ = 1;
    float fontSizeScale_ = 1;
    float screenDensity_ = 1;
    float screen_width_ = 0;   // vp单位
    float screen_height_ = 0;  // vp单位
    std::string resfile_dir_;
    std::string files_dir_;
    std::string assets_dir_;
};

#endif  // CORE_RENDER_OHOS_KRCONFIG_H
