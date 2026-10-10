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
    explicit KRConfig(const KRRenderValue &configJson) {
        Update(configJson);
    }

    void Update(const KRRenderValue &configJson) {
        // ArkTS JSON.stringify text arrives as a NAPI UTF-16 string. ParseUTF16
        // at this edge so path leaves match the source encoding.
        const auto cfg = configJson.Parsed();
        if (auto vp2px = cfg.opt(u"vp2px")) {
            vp2px_ = vp2px.toFloat();
            GetDpi(vp2px_);
        }
        if (auto screen_width = cfg.opt(u"screen_width")) {
            screen_width_ = screen_width.toFloat();
        }
        if (auto screen_height = cfg.opt(u"screen_height")) {
            screen_height_ = screen_height.toFloat();
        }
        if (auto resfile_dir = cfg.opt(u"resfile_dir")) {
            resfile_dir_ = resfile_dir.toString();
        }
        if (auto files_dir = cfg.opt(u"files_dir")) {
            files_dir_ = files_dir.toString();
        }
        if (auto assets_dir = cfg.opt(u"assets_dir")) {
            assets_dir_ = assets_dir.toString();
        }
        if (auto useOhSharedPreferences = cfg.opt(u"useOhSharedPreferences")) {
            useOhSharedPreferences_ = (useOhSharedPreferences.toString().compare("1") == 0);
        }
        if (auto screenDensity = cfg.opt(u"screenDensity")) {
            screenDensity_ = screenDensity.toFloat();
        }
        if (auto fontWeightScale = cfg.opt(u"fontWeightScale")) {
            fontWeightScale_ = fontWeightScale.toFloat();
        }
        if (auto fontSizeScale = cfg.opt(u"fontSizeScale")) {
            fontSizeScale_ = fontSizeScale.toFloat();
        }
        if (auto ime_mode = cfg.opt(u"imeMode")) {
            ime_mode_ = ime_mode.toBool();
        }
        if (auto windowId = cfg.opt(u"windowId")) {
            window_id_ = windowId.toString();
        }
        if (auto fontSizeScaleFollowSystem = cfg.opt(u"fontSizeScaleFollowSystem")) {
            fontSizeScaleFollowSystem_ = fontSizeScaleFollowSystem.toBool();
        }
        if (auto performanceMonitorTypesMask = cfg.opt(u"performanceMonitorTypesMask")) {
            performanceMonitorTypesMask_ = performanceMonitorTypesMask.toInt();
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

    /**
     * 将字体相关单位fp转换为像素值px
     * 
     * @param fp 输入的单位值（字体相关单位）
     * @return 转换后的像素值
     */
    float fp2px(float fp) {
        return fp * fontSizeScale_ * vp2px_;
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
    
    const bool GetUseOhSharedPreferences() {
        return useOhSharedPreferences_;
    }
    
    const bool ImeMode() {
        return ime_mode_;
    }

    const std::string &GetWindowId() {
        return window_id_;
    }

    const bool GetFontSizeScaleFollowSystem() {
        return fontSizeScaleFollowSystem_;
    }

    const int GetPerformanceMonitorTypesMask() {
        return performanceMonitorTypesMask_;
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
    std::string window_id_; // 页面所在的窗口ID，用于标识页面所在的窗口
    bool ime_mode_ = false;
    bool fontSizeScaleFollowSystem_ = true;
    int performanceMonitorTypesMask_ = 0;
    bool useOhSharedPreferences_ = true;    // 默认使用新的SharedPreferencesModule
};

#endif  // CORE_RENDER_OHOS_KRCONFIG_H
