# the minimum version of CMake.
cmake_minimum_required(VERSION 3.4.1)
project(myNpmLib)

set(NATIVERENDER_ROOT_PATH .)
set(CMAKE_CXX_STANDARD 17)
if(DEFINED PACKAGE_FIND_FILE)
    include(${PACKAGE_FIND_FILE})
endif()

set(SOURCE_SET
        napi_init.cpp
        libohos_render/api/src/Kuikly.cpp
        libohos_render/foundation/ark_ts.cpp
        libohos_render/foundation/thread/KRMainThread.cpp
        libohos_render/manager/KRRenderManager.cpp
        libohos_render/view/KRRenderView.cpp
        libohos_render/scheduler/KRUIScheduler.cpp
        libohos_render/scheduler/KRContextScheduler.cpp
        libohos_render/context/IKRRenderNativeContextHandler.cpp
        libohos_render/context/KRRenderNativeContextHandlerManager.cpp
        libohos_render/context/DefaultRenderNativeContextHandler.cpp
        libohos_render/context/KRRenderExecuteMode.cpp
        libohos_render/context/KRRenderNativeMode.cpp
        libohos_render/context/KRRenderExecuteModeWrapper.cpp
        libohos_render/adapter/KRRenderAdapterManager.cpp
        libohos_render/manager/KRArkTSManager.cpp
        libohos_render/manager/KRSnapshotManager.cpp
        libohos_render/core/KRRenderCore.cpp
        libohos_render/expand/modules/network/KRNetworkModule.cpp
        libohos_render/expand/components/apng/KRApngView.cpp
        libohos_render/expand/components/apng/ApngParser.cpp
        libohos_render/expand/components/apng/APNGAnimateView.cpp
        libohos_render/expand/components/apng/APNGStructs.cpp
        libohos_render/utils/KREventUtil.cpp
        libohos_render/layer/KRRenderLayerHandler.cpp
        libohos_render/expand/events/KREventDispatchCenter.cpp
        libohos_render/expand/events/gesture/KRGestureGroupHandler.cpp
        libohos_render/expand/events/gesture/KRGestureEventHandler.cpp
        libohos_render/expand/events/gesture/KRGestureCaptureRule.cpp
        libohos_render/expand/components/base/animation/KRNodeAnimationHandler.cpp
        libohos_render/expand/components/base/animation/KRNodeAnimation.cpp
        libohos_render/expand/components/base/KRBasePropsHandler.cpp
        libohos_render/expand/events/KRBaseEventHandler.cpp
        libohos_render/expand/modules/cache/KRMemoryCacheModule.cpp
        libohos_render/expand/modules/log/KRLogModule.cpp
        libohos_render/expand/components/view/SuperTouchHandler.cpp
        libohos_render/expand/components/view/KRView.cpp
        libohos_render/expand/components/image/KRImageAdapterManager.cpp
        libohos_render/expand/components/image/KRImageView.cpp
        libohos_render/expand/components/image/KRImageViewWrapper.cpp
        libohos_render/expand/components/richtext/KRFontAdapterManager.cpp
        libohos_render/expand/components/richtext/KRRichTextShadow.cpp
        libohos_render/expand/components/scroller/KRScrollerView.cpp
        libohos_render/expand/components/richtext/KRRichTextView.cpp
        libohos_render/utils/KRLinearGradientParser.cpp
        libohos_render/expand/components/richtext/gradient_richtext/KRGradientRichTextShadow.cpp
        libohos_render/expand/components/richtext/gradient_richtext/KRGradientRichTextView.cpp
        libohos_render/utils/KRTransformParser.cpp
        libohos_render/expand/components/input/KRTextFieldView.cpp
        libohos_render/manager/KRKeyboardManager.cpp
        libohos_render/expand/modules/forward/KRForwardArkTSModule.cpp
        libohos_render/expand/modules/preferences/KRPreferences.cpp
        libohos_render/expand/modules/preferences/KRSharedPreferencesModule.cpp
        libohos_render/expand/components/forward/KRForwardArkTSView.cpp
        libohos_render/expand/components/forward/KRForwardArkTSViewV2.cpp
        libohos_render/expand/components/modal/KRModalView.cpp
        libohos_render/expand/components/ActivityIndicator/KRActivityIndicatorAnimationView.cpp
        libohos_render/expand/components/hover/KRHoverView.cpp
        libohos_render/expand/components/canvas/KRCanvasView.cpp
        libohos_render/export/IKRRenderViewExport.cpp
        libohos_render/expand/modules/codec/codec.c
        libohos_render/expand/modules/codec/md5.c
        libohos_render/expand/modules/codec/sha256.c
        libohos_render/expand/modules/codec/KRCodec.cpp
        libohos_render/expand/modules/codec/KRCodecModule.cpp
        libohos_render/expand/modules/calendar/KRDate.cpp
        libohos_render/expand/modules/calendar/KRCalendarModule.cpp
        libohos_render/expand/modules/back_press/KRBackPressModule.cpp
        libohos_render/utils/KRURIHelper.cpp
        libohos_render/utils/KRBase64Util.cpp
        libohos_render/utils/KRJSONObject.cpp
        libohos_render/utils/KRStringUtil.cpp
        libohos_render/utils/KRViewUtil.cpp
        libohos_render/utils/KRThreadChecker.cpp
        libohos_render/utils/KRJsUtil.cpp
        libohos_render/utils/NAPIUtil.cpp
        libohos_render/utils/KRConvertUtil.cpp
        thirdparty/cJSON/cJSON.c
        thirdparty/tinyXml/tinyxml2.cpp
        libohos_render/performance/KRPerformanceManager.cpp
        libohos_render/performance/KRPerformanceData.cpp
        libohos_render/performance/KRMonitor.cpp
        libohos_render/performance/launch/KRLaunchMonitor.cpp
        libohos_render/performance/launch/KRLaunchData.cpp
        libohos_render/expand/modules/performance/KRPageCreateTrace.cpp
        libohos_render/expand/modules/performance/KRPerformanceModule.cpp
)

add_library(kuikly SHARED ${SOURCE_SET})
target_include_directories(kuikly PUBLIC ${HMOS_SDK_NATIVE}/sysroot/usr/include)
target_link_directories(kuikly PUBLIC ${HMOS_SDK_NATIVE}/sysroot/usr/lib/aarch64-linux-ohos)
target_include_directories(kuikly PRIVATE ${NATIVERENDER_ROOT_PATH}
                    ${NATIVERENDER_ROOT_PATH}/include)
target_link_libraries(kuikly PUBLIC libace_napi.z.so libace_ndk.z.so hilog_ndk.z.so libnative_drawing.so libjsvm.so libohfileuri.so libpixelmap_ndk.z.so libimage_source.so libpixelmap.so libimage_packer_ndk.z.so librcp_c.so librawfile.z.so libohresmgr.so)