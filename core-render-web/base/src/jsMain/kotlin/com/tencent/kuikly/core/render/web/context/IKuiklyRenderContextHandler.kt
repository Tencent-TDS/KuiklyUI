package com.tencent.kuikly.core.render.web.context

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderNativeMethodCallback

/**
 * Rendering process execution environment
 */
interface IKuiklyRenderContextHandler {
    /**
     * Initialize rendering execution environment
     */
    fun init(url: String?, pageId: String)

    /**
     * Destroy rendering environment
     */
    fun destroy()

    /**
     * Call kuikly core methods from native side
     */
    fun call(method: KuiklyRenderContextMethod, args: JsArray<Any?>)

    /**
     * Register callback for kuikly side to call native methods
     */
    fun registerCallNative(callback: KuiklyRenderNativeMethodCallback)
}

/**
 * Method indices exposed to Native for calling Kuikly execution environment
 */
enum class KuiklyRenderContextMethodIndex {
    Unknown,
    CreateInstance,
    UpdateInstance,
    DestroyInstance,
    FireCallback,
    FireViewEvent,
    LayoutView,
}

/**
 * Method list exposed to Native for calling Kuikly execution environment
 */
enum class KuiklyRenderContextMethod(value: Int) {
    KuiklyRenderContextMethodUnknown(
        KuiklyRenderContextMethodIndex.Unknown.ordinal
    ),

    // "createInstance" method
    KuiklyRenderContextMethodCreateInstance(
        KuiklyRenderContextMethodIndex.CreateInstance.ordinal
    ),

    // "updateInstance" method
    KuiklyRenderContextMethodUpdateInstance(
        KuiklyRenderContextMethodIndex.UpdateInstance.ordinal
    ),

    // "destroyInstance" method
    KuiklyRenderContextMethodDestroyInstance(
        KuiklyRenderContextMethodIndex.DestroyInstance.ordinal
    ),

    // "fireCallback" method
    KuiklyRenderContextMethodFireCallback(
        KuiklyRenderContextMethodIndex.FireCallback.ordinal
    ),

    // "fireViewEvent" method
    KuiklyRenderContextMethodFireViewEvent(
        KuiklyRenderContextMethodIndex.FireViewEvent.ordinal
    ),

    // "layoutView" method
    KuiklyRenderContextMethodLayoutView(
        KuiklyRenderContextMethodIndex.LayoutView.ordinal
    );
}

/**
 * Method indices exposed to Kuikly for calling Native execution environment
 */
enum class KuiklyRenderNativeMethodIndex {
    Unknown,
    CreateRenderView,
    RemoveRenderView,
    InsertSubRenderView,
    SetViewProp,
    SetRenderViewFrame,
    CalculateRenderViewSize,
    CallViewMethod,
    CallModuleMethod,
    CreateShadow,
    RemoveShadow,
    SetShadowProp,
    SetShadowForView,
    SetTimeout,
    CallShadowMethod,
}

/**
 * Method list exposed to Kuikly for calling Native execution environment
 */
enum class KuiklyRenderNativeMethod(val value: Int) {
    KuiklyRenderNativeMethodUnknown(
        KuiklyRenderNativeMethodIndex.Unknown.ordinal
    ),

    // "createRenderView" method
    KuiklyRenderNativeMethodCreateRenderView(
        KuiklyRenderNativeMethodIndex.CreateRenderView.ordinal
    ),

    // "removeRenderView" method
    KuiklyRenderNativeMethodRemoveRenderView(
        KuiklyRenderNativeMethodIndex.RemoveRenderView.ordinal
    ),

    // "insertSubRenderView" method
    KuiklyRenderNativeMethodInsertSubRenderView(
        KuiklyRenderNativeMethodIndex.InsertSubRenderView.ordinal
    ),

    // "setViewProp" method
    KuiklyRenderNativeMethodSetViewProp(
        KuiklyRenderNativeMethodIndex.SetViewProp.ordinal
    ),

    // "setRenderViewFrame" method
    KuiklyRenderNativeMethodSetRenderViewFrame(
        KuiklyRenderNativeMethodIndex.SetRenderViewFrame.ordinal
    ),

    // "calculateRenderViewSize" method
    KuiklyRenderNativeMethodCalculateRenderViewSize(
        KuiklyRenderNativeMethodIndex.CalculateRenderViewSize.ordinal
    ),

    // "callViewMethod" method
    KuiklyRenderNativeMethodCallViewMethod(
        KuiklyRenderNativeMethodIndex.CallViewMethod.ordinal
    ),

    // "callModuleMethod" method
    KuiklyRenderNativeMethodCallModuleMethod(
        KuiklyRenderNativeMethodIndex.CallModuleMethod.ordinal
    ),

    // "createShadow" method
    KuiklyRenderNativeMethodCreateShadow(
        KuiklyRenderNativeMethodIndex.CreateShadow.ordinal
    ),

    // "removeShadow" method
    KuiklyRenderNativeMethodRemoveShadow(
        KuiklyRenderNativeMethodIndex.RemoveShadow.ordinal
    ),

    // "setShadowProp" method
    KuiklyRenderNativeMethodSetShadowProp(
        KuiklyRenderNativeMethodIndex.SetShadowProp.ordinal
    ),

    // "setShadowForView" method
    KuiklyRenderNativeMethodSetShadowForView(
        KuiklyRenderNativeMethodIndex.SetShadowForView.ordinal
    ),

    // "setTimeout" method
    KuiklyRenderNativeMethodSetTimeout(
        KuiklyRenderNativeMethodIndex.SetTimeout.ordinal
    ),

    // "callShadowModule" method
    KuiklyRenderNativeMethodCallShadowMethod(
        KuiklyRenderNativeMethodIndex.CallShadowMethod.ordinal
    );

    companion object {
        fun fromInt(value: Int): KuiklyRenderNativeMethod =
            values().firstOrNull { it.ordinal == value } ?: KuiklyRenderNativeMethodUnknown
    }
}
