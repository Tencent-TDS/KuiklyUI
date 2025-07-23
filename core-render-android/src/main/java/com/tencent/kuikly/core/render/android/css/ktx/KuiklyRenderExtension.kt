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

package com.tencent.kuikly.core.render.android.css.ktx

import android.content.res.Resources
import android.graphics.Rect
import android.graphics.RectF
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.util.DisplayMetrics
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.const.KRExtConst
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.exception.KRKotlinBizException
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport
import com.tencent.tdf.utils.TDFListUtils
import com.tencent.tdf.utils.TDFMapUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 转为px[Int]
 * @return [Float]对应的px[Float]
 */
fun Float.toPxF(): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )
}

/**
 * 转为px[Int]
 * @return [Float]对应的px[Int]
 */
fun Float.toPxI(): Int = (toPxF() + KRExtConst.ROUND_SCALE_VALUE).toInt()

/**
 * 转为px[Int]
 * a
 * @return [Float]对应的px[Float]
 */
fun IKuiklyRenderContext?.toPxF(value: Float): Float {
    return toPxF(this?.useHostDisplayMetrics(), value)
}

fun toPxF(useHostDisplayMetrics: Boolean?, value: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value,
        getDisplayMetrics(useHostDisplayMetrics)
    )
}

/**
 * 转为px[Int]
 * @return [Float]对应的px[Int]
 */
fun IKuiklyRenderContext?.toPxI(value: Float): Int = toPxI(this?.useHostDisplayMetrics(), value)
fun toPxI(useHostDisplayMetrics: Boolean?, value: Float): Int = (toPxF(useHostDisplayMetrics, value) + KRExtConst.ROUND_SCALE_VALUE).toInt()

/**
 * 转为dp[Float]
 * @return [Float]对应的dp[Float]
 */
fun IKuiklyRenderContext?.toDpF(value: Float): Float = toDpF(this?.useHostDisplayMetrics(), value)

fun toDpF(useHostDisplayMetrics: Boolean?, value: Float): Float = value / getDisplayMetrics(useHostDisplayMetrics).density

/**
 * 转为dp[Int]
 * @return [Float]对应的dp[Int]
 */
fun IKuiklyRenderContext?.toDpI(value: Float): Int = toDpI(this?.useHostDisplayMetrics(), value)
fun toDpI(useHostDisplayMetrics: Boolean?, value: Float) = (toDpF(useHostDisplayMetrics, value) + KRExtConst.ROUND_SCALE_VALUE).toInt()

/**
 * 转为sp[Float]
 * @return [Float]对应的sp[Float]
 */
fun IKuiklyRenderContext?.spToPxF(value: Float): Float {
    return spToPxF(this?.useHostDisplayMetrics(), value)
}
fun spToPxF(useHostDisplayMetrics: Boolean?, value: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        value,
        getDisplayMetrics(useHostDisplayMetrics)
    )
}

private fun IKuiklyRenderContext?.getDisplayMetrics(): DisplayMetrics {
    return getDisplayMetrics(this?.useHostDisplayMetrics())
}

private fun getDisplayMetrics(useHostDisplayMetrics: Boolean?): DisplayMetrics {
    return KuiklyRenderAdapterManager.krFontAdapter?.getDisplayMetrics(useHostDisplayMetrics) ?: Resources.getSystem().displayMetrics
}

fun IKuiklyRenderContext?.spToPxI(value: Float): Int = spToPxI(this?.useHostDisplayMetrics(), value)
fun spToPxI(useHostDisplayMetrics: Boolean?, value: Float): Int = (spToPxF(useHostDisplayMetrics, value) +  KRExtConst.ROUND_SCALE_VALUE).toInt()

fun Any.toNumberFloat(): Float = (this as Number).toFloat()

/**
 * 将[SizeF]中的值转换为px值
 */
fun IKuiklyRenderContext?.toPxSizeF(sizeF: SizeF): SizeF = toPxSizeF(this?.useHostDisplayMetrics(), sizeF)
fun toPxSizeF(useHostDisplayMetrics: Boolean?, sizeF: SizeF) = SizeF(toPxF(useHostDisplayMetrics, sizeF.width), toPxF(useHostDisplayMetrics, sizeF.height))

/**
 * 将[SizeF]中的值转为dp值
 */
fun IKuiklyRenderContext?.toDpSizeF(sizeF: SizeF): SizeF = toDpSizeF(this?.useHostDisplayMetrics(), sizeF)
fun toDpSizeF(useHostDisplayMetrics: Boolean?, sizeF: SizeF) = SizeF(toDpF(useHostDisplayMetrics, sizeF.width), toDpF(useHostDisplayMetrics, sizeF.height))

/**
 * 将[RectF]中的值设置给[ViewGroup.MarginLayoutParams]
 */
internal fun View.setFrameF(context: IKuiklyRenderContext?, frameF: RectF) {
    setCommonProp(KRCssConst.FRAME, Rect().apply {
        left = context.toPxI(frameF.left)
        top = context.toPxI(frameF.top)
        right = context.toPxI(frameF.right)
        bottom = context.toPxI(frameF.bottom)
    })
}

/**
 * frame中的Rect将right和bottom定义为矩形的宽度和高度
 * 故扩展符合可读的宽度字段.krWidth代替.right
 */
val Rect.krWidth: Int
    get() = right

val Rect.krHeight: Int
    get() = bottom

/**
 * 为View扩展frame属性，其中
 * [Rect.left]为frame的x
 * [Rect.top]为frame的y
 * [Rect.right]为frame的width
 * [Rect.bottom]为frame的height
 */
var View.frame: Rect
    get() {
        return Rect().apply {
            val lp = (layoutParams as? ViewGroup.MarginLayoutParams) ?: ViewGroup.MarginLayoutParams(0, 0)
            left = lp.leftMargin
            top = lp.topMargin
            right = lp.width
            bottom = lp.height
        }
    }
    set(value) {
        if (isBeforeM) {
            setFrameForAndroidM(value)
        } else {
            val lp = (layoutParams as? ViewGroup.MarginLayoutParams) ?: ViewGroup.MarginLayoutParams(0, 0)
            lp.leftMargin = value.left
            lp.topMargin = value.top
            lp.width = value.right
            lp.height = value.bottom
            layoutParams = lp
        }
    }

/**
 * 获取Frame的宽度
 */
val View.frameWidth: Int
    get() = layoutParams?.width ?: 0

/**
 * 获取Frame的高度
 */
val View.frameHeight: Int
    get() = layoutParams?.height ?: 0

/**
 * 判断当前线程是否为主线程
 * @return 是否为主线程
 */
fun isMainThread(): Boolean = Thread.currentThread() == Looper.getMainLooper().thread

/**
 * 将[IKuiklyRenderViewExport.view]转为[ViewGroup]
 */
internal val IKuiklyRenderViewExport.viewGroup: ViewGroup?
    get() = view() as? ViewGroup

/**
 * 将[IKuiklyRenderViewExport.view]从父节点中移除
 */
internal fun IKuiklyRenderViewExport.removeFromParent() {
    val v = view()
    (v.parent as? ViewGroup)?.also { p ->
        p.removeView(v)
        onRemoveFromParent(p)
    }
}

/**
 * [Map]转[JSONObject]
 */
@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.toJSONObject(): JSONObject {
    val serializationObject = JSONObject()
    forEach { (key, value) ->
        if (key != null) { // key可能为null（from java类）
            when (value) {
                is Int -> {
                    serializationObject.put(key, value)
                }
                is Long -> {
                    serializationObject.put(key, value)
                }
                is Double -> {
                    serializationObject.put(key, value)
                }
                is Float -> {
                    serializationObject.put(key, value)
                }
                is String -> {
                    serializationObject.put(key, value)
                }
                is Boolean -> {
                    serializationObject.put(key, value)
                }
                is Map<*, *> -> {
                    val map = value as Map<String, Any>
                    serializationObject.put(key, map.toJSONObject())
                }
                is List<*> -> {
                    val list = value as List<Any>
                    serializationObject.put(key, list.toJSONArray())
                }
            }
        }
    }
    return serializationObject
}

/**
 * [List]转[JSONArray]
 */
@Suppress("UNCHECKED_CAST")
internal fun List<Any>.toJSONArray(): JSONArray {
    val serializationArray = JSONArray()
    forEach { value ->
        when (value) {
            is Int -> {
                serializationArray.put(value)
            }
            is Long -> {
                serializationArray.put(value)
            }
            is Float -> {
                serializationArray.put(value)
            }
            is Double -> {
                serializationArray.put(value)
            }
            is String -> {
                serializationArray.put(value)
            }
            is Boolean -> {
                serializationArray.put(value)
            }
            is Map<*, *> -> {
                val map = value as Map<String, Any>
                serializationArray.put(map.toJSONObject())
            }
            is List<*> -> {
                val list = value as List<Any>
                serializationArray.put(list.toJSONArray())
            }
        }
    }
    return serializationArray
}

fun JSONObject.toMap(): MutableMap<String, Any> {
    val map = mutableMapOf<String, Any>()
    val keys = keys()
    for (key in keys) {
        when (val value = opt(key)) {
            is Int -> {
                map[key] = value
            }
            is Long -> {
                map[key] = value
            }
            is Double -> {
                map[key] = value
            }
            is Float -> {
                map[key] = value
            }
            is String -> {
                map[key] = value
            }
            is Boolean -> {
                map[key] = value
            }
            is JSONObject -> {
                map[key] = value.toMap()
            }
            is JSONArray -> {
                map[key] = value.toList()
            }
        }
    }
    return map
}

fun JSONArray.toList(): MutableList<Any> {
    val list = mutableListOf<Any>()
    val size = length()
    for (i in 0 until size) {
        when (val value = opt(i)) {
            is Int -> {
                list.add(value)
            }
            is Long -> {
                list.add(value)
            }
            is Float -> {
                list.add(value)
            }
            is Double -> {
                list.add(value)
            }
            is String -> {
                list.add(value)
            }
            is Boolean -> {
                list.add(value)
            }
            is JSONObject -> {
                list.add(value.toMap())
            }
            is JSONArray -> {
                list.add(value.toList())
            }
        }
    }
    return list
}

inline fun buildSpannedString(
    builderAction: SpannableStringBuilder.() -> Unit
): SpannedString {
    val builder = SpannableStringBuilder()
    builder.builderAction()
    return SpannedString(builder)
}

/**
 * 将spans设置到[SpannableStringBuilder]对象中
 * @param spans span列表. [SpannableStringBuilder]对象设置span的类型是any, 因此spans类型也是List<Any>
 * @param builderAction 构建闭包
 */
inline fun SpannableStringBuilder.inSpans(
    spans: List<Any>,
    builderAction: SpannableStringBuilder.() -> Unit
): SpannableStringBuilder {
    val start = length
    builderAction()
    for (span in spans) setSpan(span, start, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    return this
}

/**
 * 获取index所在的数据
 * @param index
 * @return 返回结果
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> List<Any?>.arg(index: Int): T = this[index] as T

/**
 * 获取[List]中第一个数据
 */
internal fun <T> List<Any?>.firstArg(): T = arg(KRExtConst.FIRST_ARG_INDEX)

/**
 * 获取[List]中第二个数据
 */
internal fun <T> List<Any?>.secondArg(): T = arg(KRExtConst.SECOND_ARG_INDEX)

/**
 * 获取[List]中第三个数据
 */
internal fun <T> List<Any?>.thirdArg(): T = arg(KRExtConst.THIRD_ARG_INDEX)

/**
 * 获取[List]中第四个数据
 */
internal fun <T> List<Any?>.fourthArg(): T = arg(KRExtConst.FOURTH_ARG_INDEX)

/**
 * 获取[List]中第五个数据
 */
internal fun <T> List<Any?>.fifthArg(): T = arg(KRExtConst.FIFTH_ARG_INDEX)

/**
 * 获取[List]中第六个数据
 */
internal fun <T> List<Any?>.sixthArg(): T = arg(KRExtConst.SIXTH_ARG_INDEX)

/**
 * stackTraceToString在 1.4上才支持，为了支持 1.3，单独抽出来
 */
internal fun Throwable.stackTraceToString(): String {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    printStackTrace(pw)
    pw.flush()
    return sw.toString()
}

fun Throwable.kuiklyExceptionStackTraceToString(): String {
    return if (this is KRKotlinBizException) {
        message ?: "null"
    } else {
        stackTraceToString()
    }
}

/**
 * 将字符串转为Color的十六进制
 * @return
 */
fun String.toColor(): Int = KuiklyRenderAdapterManager.krColorParseAdapter?.toColor(this) ?: toLong().toInt()

/**
 * 转成 TDFModule 调用的结果
 */
fun Any.toTDFModuleCallResult(): String {
    val jsonObj = JSONObject()
    val resultKey = "result"
    when(this) {
        is Byte -> {
            jsonObj.put(resultKey, toInt())
        }
        is Int -> {
            jsonObj.put(resultKey, this)
        }
        is Long -> {
            jsonObj.put(resultKey, this)
        }
        is Double -> {
            jsonObj.put(resultKey, this)
        }
        is Float -> {
            jsonObj.put(resultKey, this)
        }
        is String -> {
            jsonObj.put(resultKey, this)
        }
        is Boolean -> {
            jsonObj.put(resultKey, this)
        }
        is Map<*, *> -> {
            jsonObj.put(resultKey, TDFMapUtils.toJSONObject(this as Map<String, Any?>))
        }
        is List<*> -> {
            jsonObj.put(resultKey, TDFListUtils.toJSONArray(this))
        }
        else -> {
            jsonObj.put(resultKey, this.toString())
        }
    }
    return jsonObj.toString()
}