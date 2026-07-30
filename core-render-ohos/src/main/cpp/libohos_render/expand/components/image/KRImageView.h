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

#ifndef CORE_RENDER_OHOS_KRIMAGEVIEW_H
#define CORE_RENDER_OHOS_KRIMAGEVIEW_H

#include "libohos_render/expand/components/image/KRImageLoadOption.h"
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/foundation/KRSize.h"

#include <vector>

// OH_Drawing_Lattice：native drawing 层的九宫格分割线对象；这里只做前向声明，
// 具体类型在 util 层通过 <native_drawing/drawing_lattice.h> 使用。KRImageView
// 侧只保存指针到 lattice_pool_，在 OnDestroy 时统一销毁。
struct OH_Drawing_Lattice;

using namespace std::string_view_literals;
constexpr std::string_view KR_ASSET_PREFIX = "assets://"sv;

class KRImageView : public IKRRenderViewExport {
 public:
    KRImageView() = default;
    KRImageView(const KRImageView &) = delete;
    KRImageView(KRImageView &&) = delete;
    KRImageView &operator=(const KRImageView &) = delete;
    KRImageView &operator=(KRImageView &&) = delete;

    ArkUI_NodeHandle CreateNode() override;
    void DidInit() override;
    bool ReuseEnable() override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    bool ResetProp(const std::string &prop_key) override;
    void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;
    void OnDestroy() override;

 private:
    bool SetImageSrc(const KRAnyValue &value);
    bool SetResizeMode(const KRAnyValue &value);
    bool SetDragEnable(const KRAnyValue &value);
    bool SetBlurRadius(const KRAnyValue &value);
    bool SetTintColor(const KRAnyValue &value);
    bool SetColorFilter(const KRAnyValue &value);
    bool ResetColorFilter();
    bool SetCapInsets(const KRAnyValue &value);
    bool SetImageParams(const KRAnyValue &value);
    bool SetDotNineImage(const KRAnyValue &value);
    bool SetMaskLinearGradient(const KRAnyValue &value);
    void ResetMaskLinearGradientNode();
    void EnsureLoadCompleteEventRegistered();
    // 图片加载完成后，按 view frame 与图片像素比例下发 NODE_IMAGE_SOURCE_SIZE
    // （cover 语义，vp 单位），再调用 lattice 精确九宫格。
    // 前置条件：has_cap_insets_ && has_loaded_image_ && loaded_image_size_ 有效。
    void ApplyCapInsetsWithLattice();
    // KRImageView 侧的 lattice 封装：内部调用 kuikly::util::SetArkUIImageCapInsetsWithLattice，
    // 把 util 通过 out_lattice 交出的 OH_Drawing_Lattice* 存进 lattice_pool_，
    // 到 OnDestroy 时再统一销毁。这样避免"下发后立即销毁"造成的 ArkUI 内部
    // 崩溃，同时不泄漏（本实例生命周期结束时全部释放）。参数语义与 util 版本
    // 完全一致，见 KRViewUtil.h 中的注释。
    void SetArkUIImageCapInsetsWithLattice(float top, float left, float bottom, float right,
                                           float image_width_px, float image_height_px);
    bool RegisterLoadSuccessCallback(const KRRenderCallback &event_callback);
    bool RegisterLoadResolutionCallback(const KRRenderCallback &event_callback);
    bool RegisterLoadFailureCallback(const KRRenderCallback &event_callback);
    void FireOnImageCompleteEvent(ArkUI_NodeEvent *event);
    void FireOnImageErrorEvent(ArkUI_NodeEvent *event);
    std::shared_ptr<KRImageLoadOption> ToImageLoadOption(const std::string &src);
    void LoadFromSrc(const std::string image_src);
    void LoadFromNetwork(const std::shared_ptr<KRImageLoadOption> image_option);
    void LoadFromBase64(const std::shared_ptr<KRImageLoadOption> image_option);
    void LoadFromFile(const std::shared_ptr<KRImageLoadOption> image_option);
    void LoadFromResourceMedia(const std::shared_ptr<KRImageLoadOption> image_option);
    void LoadFromAssets(const std::shared_ptr<KRImageLoadOption> image_option);

 private:
    std::string image_src_;
    std::shared_ptr<KRImageLoadOption> image_option_ = nullptr;
    KRRenderCallback load_success_callback_ = nullptr;
    KRRenderCallback load_resolution_callback_ = nullptr;
    KRRenderCallback load_failure_callback_ = nullptr;
    bool has_loaded_image_ = false;
    KRSize loaded_image_size_;
    bool had_register_on_complete_event_ = false;
    bool had_register_on_error_event_ = false;
    bool is_dot_nine_image_ = false;
    // 用户设定的 capInsets（top / left / bottom / right），四值单位为**图片 vp**
    //（与上层协议其他布局字段保持一致，属于"上层协议单位"）。has_cap_insets_ 为
    // true 表示当前存在有效 capInsets，需要在图片加载完成后按图片像素尺寸走
    // lattice 精确九宫格（API 24+）路径。"vp -> 图片像素"的换算集中在
    // ApplyCapInsetsWithLattice 里下发 lattice 前统一做（* dpi），SetCapInsets
    // 阶段**不做任何换算**，避免历史实现里 top 用 dpi、其余三边硬编码 3.25 这类
    // 四边不一致的坑再次出现。see SetCapInsets 注释。
    bool has_cap_insets_ = false;
    float cap_insets_top_ = 0.f;
    float cap_insets_left_ = 0.f;
    float cap_insets_bottom_ = 0.f;
    float cap_insets_right_ = 0.f;
    ArkUI_NodeHandle mask_linear_gradient_node_ = nullptr;
    KRAnyValue image_params_ = nullptr;
    // capInsets + lattice 路径下 NODE_IMAGE_SOURCE_SIZE 的 scale 参数：
    // 当前实现**固定为 DEFAULT_CAPINST_IMAGE_SCALE = 1.0**（"1 图片像素 = 1 vp"），
    // 不再从 imageParams 中读取任何 __scale__ 键。保留字段以便未来恢复动态
    // 素材倍率支持时低成本改回。
    // 数学模型（仍适用）：source_scale = 1 / capinset_image_scale_ * dpi = dpi（当值=1时），
    // 即 source 像素 = 原图像素 * dpi，与屏幕 dpi 严格对齐，不受 view frame 影响。
    float capinset_image_scale_ = 1.f;
    // === 循环防护相关（避免 SetArkUIImageSourceSize 触发重解码→ON_COMPLETE→
    // ApplyCapInsetsWithLattice→再次 SetArkUIImageSourceSize 的死循环）===
    //
    // source_size_applied_：当前 image_src_ 生命周期内是否已经下发过一次
    //   NODE_IMAGE_SOURCE_SIZE。第二次进入 ApplyCapInsetsWithLattice 时不再
    //   下发 source size（只重发 lattice），从而彻底断掉重解码回环。
    //   src 变更时重置为 false，让新 src 下第一次 apply 时能重新下发 source size。
    bool source_size_applied_ = false;
    // original_image_size_ / has_original_image_size_：首次从 ArkUI ON_COMPLETE
    //   拿到的**原图像素尺寸**快照。由于 SetArkUIImageSourceSize 会让 ArkUI
    //   重采样 pixmap，后续 ON_COMPLETE 携带的 width/height 变为**重采样后**
    //   尺寸；直接用它当"原图 * source_scale"就会尺寸雪崩。因此 apply 换算
    //   里始终使用 original_image_size_ 而非 loaded_image_size_——
    //   loaded_image_size_ 保持原语义继续更新（对外 load_resolution 回调需要）。
    //   src 变更时必须清空，让新图第一次 ON_COMPLETE 重新捕获原图尺寸。
    KRSize original_image_size_;
    bool has_original_image_size_ = false;
    // 本实例生命周期内累计创建的 OH_Drawing_Lattice 池。每次 ApplyCapInsetsWithLattice
    // 通过 util 底层函数创建的 lattice 都会 push 进来；OnDestroy 时逐个
    // OH_Drawing_LatticeDestroy 释放。
    // 之所以不"下发后立即销毁"：实测在 ArkUI 立即 setAttribute 结束后销毁 lattice
    // 会引发崩溃（ArkUI 内部似乎异步引用了 lattice 数据），所以必须把生命周期延长
    // 到本组件销毁时。同一实例内 lattice 可能累积多个（例如 cap insets 变化或复用），
    // 由于 ArkUI 每次拿到的都是新指针、旧 lattice 不再被外部访问，累积几个 lattice
    // 只是轻微内存滞留，OnDestroy 兜底释放不会真泄漏。
    std::vector<OH_Drawing_Lattice *> lattice_pool_;
    
    static void AdapterSetImageCallback(const void* context,
                                   const char *src,
                                   ArkUI_DrawableDescriptor *imageDescriptor,
                                   const char *new_src);
};

#endif  // CORE_RENDER_OHOS_KRIMAGEVIEW_H
