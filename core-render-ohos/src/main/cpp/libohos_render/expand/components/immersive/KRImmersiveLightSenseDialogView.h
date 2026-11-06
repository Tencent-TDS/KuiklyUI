/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of the KuiklyUI;
 * you may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef CORE_RENDER_OHOS_KRIMMERSIVELIGHTSENSEDIALOGVIEW_H
#define CORE_RENDER_OHOS_KRIMMERSIVELIGHTSENSEDIALOGVIEW_H

#include "libohos_render/expand/components/modal/KRModalView.h"

/**
 * 鸿蒙「沉浸光感」弹窗组件。
 *
 * 设计上是**独立组件**，而不是给 Modal 加一堆 Ohos 后缀属性，原因：
 *   1) 沉浸材质生效的前提是 customStyle=false，而它会改变弹窗布局语义（宽度走栅格、
 *      圆角固定 24vp）。把这种有副作用的模式藏在属性开关里，业务无从感知；
 *      独立成组件后语义是明示的。
 *   2) 避免跨平台噪音 —— 一堆仅鸿蒙生效的属性挂在通用 Modal 上。
 *
 * 实现上**继承 KRModalView** 复用弹窗生命周期（创建 / setContent / show / close /
 * dispose / onWillDismiss），只覆盖"沉浸光感"相关的差异：
 *   - customStyle 固定 false（不暴露给业务）
 *   - 材质默认启用，业务不设也有效果
 *   - 把 Kuikly 通用属性（尺寸 / 圆角 / 背景色）转发成弹窗背板的样式
 */
class KRImmersiveLightSenseDialogView : public KRModalView {
 public:
    /**
     * 背板样式的采集必须在 ToSetProp 这一层，不能放在 SetProp 里。
     *
     * 基类 IKRRenderViewExport::ToSetProp 的分发顺序是：
     *   先给 base_props_handler_ 处理；只有它没接（didHanded == false）才会调子类 SetProp。
     * 而 borderRadius / border / backgroundColor 全是通用属性，必然被 base handler 接走，
     * 子类 SetProp 永远收不到 —— 放在那里等于死代码。
     */
    void ToSetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                   const KRRenderCallback event_call_back = nullptr) override;
    void SetRenderViewFrame(const KRRect &frame) override;
    void DidMoveToParentView() override;

 protected:
    void ApplyExtraBackplaneStyle(ArkUI_NativeDialogHandle dialog) override;

 private:
    // 边框：映射到弹窗背板。画在内容节点上会和背板不重合（两者不是同一个矩形），
    // 内容节点被 NativeDialog 接管后布局尺寸为 0，视觉上就是"白框套在背板里侧"。
    bool has_border_ = false;
    float border_width_ = 0.0f;
    uint32_t border_color_ = 0xFFFFFFFF;
    int32_t border_style_ = 0;  // ArkUI_BorderStyle：0=SOLID 1=DASHED 2=DOTTED
};

#endif  // CORE_RENDER_OHOS_KRIMMERSIVELIGHTSENSEDIALOGVIEW_H
