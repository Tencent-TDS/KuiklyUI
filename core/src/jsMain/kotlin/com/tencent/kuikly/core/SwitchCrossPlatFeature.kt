package com.tencent.kuikly.core

import com.tencent.kuikly.core.base.CrossPlatFeature

@JsName(name = "setIsEnableFastCollection")
@JsExport
@ExperimentalJsExport
/**
 *  enable FastCollection
 */
fun setIsEnableFastCollection(isEnable: Boolean) {
    CrossPlatFeature.isUseFastCollection = isEnable
}

@JsName(name = "setIsIgnoreRenderViewForFlatLayer")
@JsExport
@ExperimentalJsExport
/**
 *  set to Ignore isRenderViewForFlatLayer,
 */
fun setIsIgnoreRenderViewForFlatLayer(isIgnore: Boolean) {
    CrossPlatFeature.isIgnoreRenderViewForFlatLayer = isIgnore
}