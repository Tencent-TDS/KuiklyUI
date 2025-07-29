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

#include "libohos_render/expand/components/ActivityIndicator/KRActivityIndicatorAnimationView.h"

constexpr char kGrayImage[] = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAJYAAACWCAMAAAAL34HQAAAAS1BMVEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADmYDp0AAAAGXRSTlMAJk0zQGZzBCITGh8OCggXSTotRDEqXVVqQNv4pgAABP9JREFUeNrs2ctyozAQheHTGKEbEtgk2O//pJOaSSjUQUiAqGHhb+vNX6ZpjIy3t6vTlshqXItT9JdyuBBHkyt1KZooXIammevMl6UZi6ugAK7iP2ZJ83iYI1m+bT1Ku72aL6/b3iyt6IvSKGpovlX7sgx961BQ30zqPVkdTQyKsc2M2p7laMahlKqZEXJzlqIZg1JEM3fbmqVpTqGUJvBy27IkhVDKqwn027I6CqiCsxXqtmR5ChmUQizruSXLUqhFMSPr+szPailkUY5hWUJmZykKOTAlp+uem6WLThbXNkyblyWJkSjqzrKqvCxDIY2ypGBdJifLUUihtE+WNUazzl0O3JN1PWJZ5y4HrmNZL5/KkopCHifoWVedytIU6nAG92JdOpJ18nLgbnxJxF5f08vh7CURf9n3RZeDGUYx9gpLFMvq145GTNbDsDWKlHFIqMU/T5OxJMTaQZLKeBg6yz6NVk0GB3C6Ca0du6WXgzSZd+lDzN0kuCFcXWuHlCq1HHTuDSFHERgVGB8siSdi+GwpCaZV2XdEJ7hKI/TBfqTGtWvfhbfEecSQ+K120akfscpG51129FubyGLGDxle6OkSeqySU5eNDFVmVicWPS1mXPW9tKba1HgZyc7KF3lEjWJZrzHT3obhQyOD1F2ngyhvaJnKWhDc3aMA2VGMxopBxIwPHKYVxRisuouoXuIYQ1EdEuxTxDw9DpCWYpRDkvwYRcRwznelkcXVIqLFbpIiOolcuhKLHthN0yLrsYUaF9cEdutogWqxkbydn6WxgxvOvYhGYh/zPG/krcN+FIxYjQMMG6pD5F1MKlnqz1AtcVTb/1R5HCItG6qDupq9Om7DXwo93t7e3gqQVPfVor4miZK0pQirEdJDtWLQKMYpWqFcUFUl6GJVlDDrkkOVMEiUoShBYUJVEqEITUkaP+oqqUYRlpIsfvRVUo8iKMPVs/60c7Y7DoJAFKVAguInFN7/VTfpbqnedTRt6jA/PE9wVLzKMCD0IQod8kIDQmicSv34SP1UX1xcXHxGm+7zHLys0shg9C/BySkkuawLQUzZrZv1giSjSNlavWb4Ykl3/HxQIU31Ari7zfofsfZygb/rDWLdxZU+6E2amktRY9IEQ8WFu2bWBKbeMud01xS23qJw1iSh3hK60RRzU6/hoNEU0VVsz6AGe+ixIcUPHzazdO83s7TE7HVa/1PYB5mt9afbHlRuZWXsH8kxNEpRWgmuItqCYWora8lBVfB2wcTThIdD/u4V4IJdEDlaFjEg5o2SyM0uCQtfn1PK3n2nwROJumCGre6bNa9rj+ZB7L/TDosk/YvdtE92+271ptATdb/uveZhpDX0pLC1a/LzsqMpRHdCq/U+AbT6p4FZ4MnG9JPoig+8iNksyMxt/CPerHLrk1mQmDc93MDq9VTMCt4tIj1YBUdo8W6oSaDlFaHFuv1oAiujKC3OzVqOCAdCiyskGiJJCS2mkBgtMO5rMYVEJsKB0GLYZEqEA63FtyXXgNakDrRYQqIFq6SOtFhCImI47GqxbY6HzLqpXS22owTo8U5r0SFx0t3q1L4W2zEVGX7g97V4DvXAN7F9X2s4Kbgy5PuuFseBMfhnmtWh1pnH6yA+PMZVp461zj2MCOm9hxglteijmzhBLSnAhEwKMH2VAkz2pQClETGsCkmCKGU3UValSCnoCV5cbPIDQR5TIrqY83gAAAAASUVORK5CYII=";
constexpr char KWhiteImage[] = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAJYAAACWCAMAAAAL34HQAAAAb1BMVEUAAAD///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////8v0wLRAAAAJHRSTlMATWZZv5mM2UUHIDM+FBo4JgwrEVJ9qDCHbbd0XejRx5D2sZ/haBjGAAAFg0lEQVR42uzZW5eaMBSG4W9TMEfOHaxOFUfz/39ju9oZS5AQDnGVC59Lr94VN1tAvLxsXaGJdIFtSTn9wVNsSEp3W+ridMexGQV1bGe+NHVobAVZsBX/MUvkUaTXZKVS1gjtcDK/nd6XZklOv3GJoBLzqVmWpejTHgE15m63JKuiO4VguOmg+VkZdaQBD6uDidlZnOgZx3UyXe9zsyR1cYTyYWV9ZPOyBNkCnpblNi9rT086rauxqTlZNdlyhPLd2No5WZpsGYI5G9u36VkZ2TTC0cZ2EpOzONnSsGve9jY1qyCbQkjyw1g+5LQsQT0CQb0ZWzMtS5GtQFjiZGx6SlZKNo7Qvhnb2ZPlWA7BtcYW+bMk2TTCU/0lUfqyBCdbjSe4GdvOl1WRbY9nyPpLohjPKqlH4Cneje06/viak03iOQQztnzsYb8OuhzyS3xuE44h1N+pY69G1KTlkOWcuErhsWN/XRUGHHsX49iLJD7hNivV034qf7C7S4YHVW/ox167ka3EA6EmXqU/Wcf5INCX9E/L/ZKSU1eFB8XUC0K0zBITeuqT6ThiRE4dXKAn45OvCMX6mgq2Q+8m1S0bO4taU18Nl+/s0Y/UOfUtRmnnvIs9PZKerJ5zJNBRnu9VKUYJ7fiJlkSzshQbFHN0pI354ybg83koSjiGypLCqWXDbgU65CFJDhITlLKqZGl9ktMwDreIubzVCEBU5CIx4sJc2p9YTXJyURj1xpySEuvk5FTBg8fM5VpjBaHJhWfwEtGZOVywgvKMlVf6gzlkWKwkh73AVFXDBq0Ye0mDdI05KB5cE1isWjZUfeJwfnpWgQWyy3O/RCWwjLoymww38jrFct9a1rHDCnvq4BKrlO//RqwRof4MrQTWkslXVQ0EWfN5iRDULj63N8JC/YfCGi8vLy8BiCg5xoOOSSQQUqHJQRewqSYe0SgEk3IawVOrKvZQwarIo9MlmtijEQiDkwfHXRR7RQiiIK8CX5LYK0EQmrw0vhxjryOCoAm2nvWrXbtZchSEogB85RYUoPEnwVJLF/3+TzlVdoeBO9iMUUoWfttkccqQo8LN9EfMdMlnWhCZ1mmuN59cb9W32+32Gc1GLqYmr62ReuLf+i6fjSRluDVns+1WCe5geWxS6p77Hhls6b4Yp+TlG+Cq4P/Cq48LmpEH4LWHK3rmQfLKo6hu4RseFx7cyS++gV13zDmMfEvfXXYojHzTpC47Qmd8y5e8buBA8i3YXTieIXjYXPujc8b8372ja8uyPTzMonnQOPj1IVao4osJV83B0Z8mvKgUONQsfkyR31VJ/CHVzkGpeCxGapgJa4r0H1rVobEyzamZfrkSjhJ+odHRHhnCo0t+bIDoRuFgsYtlmQMji7QgCgWUEa7enSE1jBn3Ecqgq9w14EktZFERtfCMfz9AtsIa3tD3+mgcltZ83wY/FeGrVTPL5irQIyPDwxHt+lIY/tpT+BC+KWQWKltavsio9QE9ifVeDyVzlO8rgb4CEpEkFbOrmzmM7RL0PSGJbiQL3l565rHfR0JBCih8BfweCwb0NZBATVL1KhZLGfQ94HwTidVALBZopCVxupKkmiEeCyT6NJxMBcohHouWhAErTTksEIkV7tQBTvWi5fCKxNoqiQ7OtAifhHis1TNlSeiNcojHgiJhScwk1gDxWOlLYiCpJojG2i6JGs7CSKx6T6wX+io4C+kshD2xoEnVXaNfDt2+WAp9ia6WhF2xaEkUaVqrh72xoEjTXK1wPPfHqtP8EwHJw9/OWFAmqnmba4FPYkGV6Am16td1VcFnseBp1nJo4Wy6aTTAjljEQ+sHpBOPlYtMY5HX11yQl/1ckK2RbHgbSRmx225ZpbKblBn9grdb0B9tgZ0KTsLMSAAAAABJRU5ErkJggg==";
constexpr char kStyleKey[] = "style";
constexpr char kWhiteStyle[] = "white";
constexpr int kAnimationIterations = 1;
constexpr int kRotateTimesPerCycle = 3;
constexpr int kTotalDuration = 1200;
constexpr float kDurations[] = {0.3 * kTotalDuration, 0.3 * kTotalDuration, 0.4 * kTotalDuration};
constexpr int kMinFrameRate = 10;
constexpr int kMaxFrameRate = 120;
constexpr int kExpectedFrameRate = 60;

ArkUI_NodeHandle KRActivityIndicatorAnimationView::CreateNode() {
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_IMAGE);
}

void KRActivityIndicatorAnimationView::DidInit() {
    RegisterEvent(NODE_IMAGE_ON_COMPLETE);
}

bool KRActivityIndicatorAnimationView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                               const KRRenderCallback event_call_back) {
    if (kuikly::util::isEqual(prop_key, kStyleKey)) {
        SetStyle(prop_value->toString());
        return true;
    }
    return IKRRenderViewExport::SetProp(prop_key, prop_value, event_call_back);
}

void KRActivityIndicatorAnimationView::SetStyle(const std::string &style) {
    auto nodeApi = kuikly::util::GetNodeApi();
    ArkUI_AttributeItem src_attr_item = {.string = kGrayImage};
    if (style == kWhiteStyle) {
        src_attr_item = {.string = KWhiteImage};
    }
    nodeApi->setAttribute(GetNode(), NODE_IMAGE_SRC, &src_attr_item);
}

void KRActivityIndicatorAnimationView::SetRenderViewFrame(const KRRect &frame) {
    IKRRenderViewExport::SetRenderViewFrame(frame);
    auto scaleInset = 2;
    kuikly::util::UpdateNodeFrame(GetNode(), KRRect(frame.x - scaleInset, frame.y - scaleInset, frame.width + 2 * scaleInset,
                                      frame.height + 2 * scaleInset));
}

void KRActivityIndicatorAnimationView::OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
    if (event_type == NODE_IMAGE_ON_COMPLETE) {
        FireOnImageCompleteEvent(event);
    }
}

void KRActivityIndicatorAnimationView::FireOnImageCompleteEvent(ArkUI_NodeEvent *event) {
    if (!kuikly::util::IsImageLoadSuccessStatus(event)) {
        return;
    }
    ArkUI_AnimateOption *option = OH_ArkUI_AnimateOption_Create();
    OH_ArkUI_AnimateOption_SetIterations(option, kAnimationIterations);
    OH_ArkUI_AnimateOption_SetCurve(option, ArkUI_AnimationCurve::ARKUI_CURVE_LINEAR);
    OH_ArkUI_AnimateOption_SetPlayMode(option, ARKUI_ANIMATION_PLAY_MODE_NORMAL);

    auto range = std::make_shared<ArkUI_ExpectedFrameRateRange>();
    range->min = kMinFrameRate;
    range->max = kMaxFrameRate;
    range->expected = kExpectedFrameRate;
    OH_ArkUI_AnimateOption_SetExpectedFrameRateRange(option, range.get());

    auto root_view = GetRootView().lock();
    if (!root_view) {
        return;
    }
    ArkUI_ContextHandle handle = root_view->GetUIContextHandle();

    struct MyUserData {
        ArkUI_ContextHandle handle;
        std::weak_ptr<IKRRenderViewExport> weak_view;
        ArkUI_ContextCallback update;
        ArkUI_AnimateCompleteCallback complete_callback;
        ArkUI_AnimateOption *option;
        uint32_t index;
        bool stop;
    };
    MyUserData *user_data = new MyUserData;
    user_data->weak_view = weak_from_this();
    user_data->handle = handle;
    user_data->option = option;
    user_data->index = 0;
    user_data->update.userData = user_data;
    user_data->update.callback = [](void *userData) {
        struct MyUserData *user_data = (struct MyUserData *)userData;
        auto view = user_data->weak_view.lock();
        if (view) {
            float angle = static_cast<float>(user_data->index + 1) / kRotateTimesPerCycle * 360;
            // set rotate
            ArkUI_NumberValue rotateValue[] = {0, 0, 1, angle, 0};  // {x, y, z, angle, view_distance}
            ArkUI_AttributeItem rotateItem = {rotateValue, sizeof(rotateValue) / sizeof(ArkUI_NumberValue)};
            if (auto node = view->GetNode()) {
                kuikly::util::GetNodeApi()->setAttribute(node, NODE_ROTATE, &rotateItem);
            }
        }
    };

    user_data->complete_callback.userData = user_data;
    user_data->complete_callback.type = ARKUI_FINISH_CALLBACK_REMOVED;
    user_data->complete_callback.callback = [](void *userData) {
        struct MyUserData *user_data = (struct MyUserData *)userData;
        if (user_data->stop) {
            user_data->weak_view.reset();
            free(userData);
        } else {
            // start next iteration
            user_data->index = (user_data->index + 1) % kRotateTimesPerCycle;
            auto view = user_data->weak_view.lock();
            if (view) {
                if (user_data->index == 0) {                            // reset angle
                    ArkUI_NumberValue rotateValue[] = {0, 0, 1, 0, 0};  // {x, y, z, angle, view_distance}
                    ArkUI_AttributeItem rotateItem = {rotateValue, sizeof(rotateValue) / sizeof(ArkUI_NumberValue)};
                    kuikly::util::GetNodeApi()->setAttribute(view->GetNode(), NODE_ROTATE, &rotateItem);
                }
                OH_ArkUI_AnimateOption_SetDuration(user_data->option, kDurations[user_data->index]);
                ArkUI_NativeAnimateAPI_1 *animate_api = reinterpret_cast<ArkUI_NativeAnimateAPI_1 *>(
                    OH_ArkUI_QueryModuleInterfaceByName(ARKUI_NATIVE_ANIMATE, "ArkUI_NativeAnimateAPI_1"));
                animate_api->animateTo(user_data->handle, user_data->option, &user_data->update,
                                       &user_data->complete_callback);
            } else {
                user_data->weak_view.reset();
                free(userData);
            }
        }
    };

    OH_ArkUI_AnimateOption_SetDuration(option, kDurations[user_data->index]);
    ArkUI_NativeAnimateAPI_1 *animate_api = reinterpret_cast<ArkUI_NativeAnimateAPI_1 *>(
        OH_ArkUI_QueryModuleInterfaceByName(ARKUI_NATIVE_ANIMATE, "ArkUI_NativeAnimateAPI_1"));

    animate_api->animateTo(handle, option, &user_data->update, &user_data->complete_callback);
}
