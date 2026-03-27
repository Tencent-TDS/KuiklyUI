/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.bundle

import kotlin.reflect.KClass

@Suppress("ReplaceGetOrSet")
class Bundle internal constructor(
    private val bundleData: MutableMap<String?, Any?>
) {
    constructor() : this(bundleData = LinkedHashMap())
    constructor(initialCapacity: Int) : this(bundleData = LinkedHashMap(initialCapacity))
    constructor(bundle: Bundle) : this(bundleData = LinkedHashMap(bundle.bundleData))

    fun size(): Int = bundleData.size
    fun isEmpty(): Boolean = bundleData.isEmpty()
    fun clear() { bundleData.clear() }
    fun containsKey(key: String?): Boolean = bundleData.containsKey(key)
    fun remove(key: String?) { bundleData.remove(key) }
    fun keySet(): Set<String?> = bundleData.keys
    fun putAll(bundle: Bundle) { bundleData.putAll(bundle.bundleData) }

    fun putBoolean(key: String?, value: Boolean) { setObject(key, value) }
    fun putByte(key: String?, value: Byte) { setObject(key, value) }
    fun putChar(key: String?, value: Char) { setObject(key, value) }
    fun putShort(key: String?, value: Short) { setObject(key, value) }
    fun putInt(key: String?, value: Int) { setObject(key, value) }
    fun putLong(key: String?, value: Long) { setObject(key, value) }
    fun putFloat(key: String?, value: Float) { setObject(key, value) }
    fun putDouble(key: String?, value: Double) { setObject(key, value) }
    fun putString(key: String?, value: String?) { setObject(key, value) }
    fun putCharSequence(key: String?, value: CharSequence?) { setObject(key, value) }
    fun putBundle(key: String?, value: Bundle?) { setObject(key, value) }
    fun putIntegerArrayList(key: String?, value: ArrayList<Int?>?) { setObject(key, value) }
    fun putStringArrayList(key: String?, value: ArrayList<String?>?) { setObject(key, value) }
    fun putBooleanArray(key: String?, value: BooleanArray?) { setObject(key, value) }
    fun putByteArray(key: String?, value: ByteArray?) { setObject(key, value) }
    fun putShortArray(key: String?, value: ShortArray?) { setObject(key, value) }
    fun putCharArray(key: String?, value: CharArray?) { setObject(key, value) }
    fun putIntArray(key: String?, value: IntArray?) { setObject(key, value) }
    fun putLongArray(key: String?, value: LongArray?) { setObject(key, value) }
    fun putFloatArray(key: String?, value: FloatArray?) { setObject(key, value) }
    fun putDoubleArray(key: String?, value: DoubleArray?) { setObject(key, value) }
    fun putStringArray(key: String?, value: Array<String?>?) { setObject(key, value) }
    fun putCharSequenceArray(key: String?, value: Array<CharSequence?>?) { setObject(key, value) }

    // Narrowed alternative of Android's [putParcelableArray]
    fun putBundleArray(key: String?, value: Array<Bundle?>?) { setObject(key, value) }

    @Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")
    private inline fun <T> setObject(key: String?, value: T) {
        bundleData[key] = value
    }

    fun getBoolean(key: String?): Boolean = getBoolean(key, defaultValue = false)
    fun getBoolean(key: String?, defaultValue: Boolean): Boolean =
        getObject(key, defaultValue)
    fun getByte(key: String?): Byte = getByte(key, defaultValue = 0)
    fun getByte(key: String?, defaultValue: Byte): Byte = getObject(key, defaultValue)
    fun getChar(key: String?): Char = getChar(key, defaultValue = 0.toChar())
    fun getChar(key: String?, defaultValue: Char): Char = getObject(key, defaultValue)
    fun getShort(key: String?): Short = getShort(key, defaultValue = 0)
    fun getShort(key: String?, defaultValue: Short): Short = getObject(key, defaultValue)
    fun getInt(key: String?): Int = getInt(key, defaultValue = 0)
    fun getInt(key: String?, defaultValue: Int): Int = getObject(key, defaultValue)
    fun getLong(key: String?): Long = getLong(key, defaultValue = 0L)
    fun getLong(key: String?, defaultValue: Long): Long = getObject(key, defaultValue)
    fun getFloat(key: String?): Float = getFloat(key, defaultValue = 0f)
    fun getFloat(key: String?, defaultValue: Float): Float = getObject(key, defaultValue)
    fun getDouble(key: String?): Double = getDouble(key, defaultValue = 0.0)
    fun getDouble(key: String?, defaultValue: Double): Double = getObject(key, defaultValue)
    fun getString(key: String?): String? = getObject(key)
    fun getString(key: String?, defaultValue: String): String =
        getString(key) ?: defaultValue
    fun getCharSequence(key: String?): CharSequence? = getObject(key)
    fun getCharSequence(key: String?, defaultValue: CharSequence): CharSequence =
        getCharSequence(key) ?: defaultValue
    fun getBundle(key: String?): Bundle? = getObject(key)
    fun getIntegerArrayList(key: String?): ArrayList<Int?>? = getArrayList(key)
    fun getStringArrayList(key: String?): ArrayList<String?>? = getArrayList(key)
    fun getBooleanArray(key: String?): BooleanArray? = getObject(key)
    fun getByteArray(key: String?): ByteArray? = getObject(key)
    fun getShortArray(key: String?): ShortArray? = getObject(key)
    fun getCharArray(key: String?): CharArray? = getObject(key)
    fun getIntArray(key: String?): IntArray? = getObject(key)
    fun getLongArray(key: String?): LongArray? = getObject(key)
    fun getFloatArray(key: String?): FloatArray? = getObject(key)
    fun getDoubleArray(key: String?): DoubleArray? = getObject(key)
    fun getStringArray(key: String?): Array<String?>? = getArray(key)
    fun getCharSequenceArray(key: String?): Array<CharSequence?>? = getArray(key)

    // Narrowed alternative of Android's [getParcelableArray]
    fun getBundleArray(key: String?): Array<Bundle?>? = getArray(key)

    @Deprecated("Use the type-safe specific APIs depending on the type of the item to be retrieved")
    operator fun get(key: String?): Any? = bundleData.get(key)

    private inline fun <reified T : Any> getObject(key: String?): T? {
        val value = bundleData.get(key) ?: return null
        return try {
            value as T?
        } catch (e: ClassCastException) {
            typeWarning(key, value, T::class.canonicalName!!, e)
            null
        }
    }

    private inline fun <reified T : Any> getObject(key: String?, defaultValue: T): T {
        val value = bundleData.get(key) ?: return defaultValue
        return try {
            value as T
        } catch (e: ClassCastException) {
            typeWarning(key, value, T::class.canonicalName!!, defaultValue, e)
            defaultValue
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> getArrayList(key: String?): ArrayList<T?>? {
        val value = bundleData.get(key) ?: return null
        return try {
            value as ArrayList<T?>?
        } catch (e: ClassCastException) {
            typeWarning(key, value, "ArrayList<" + T::class.canonicalName!! + ">", e)
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> getArray(key: String?): Array<T?>? {
        val value = bundleData.get(key) ?: return null
        return try {
            value as Array<T?>?
        } catch (e: ClassCastException) {
            typeWarning(key, value, "Array<" + T::class.canonicalName!! + ">", e)
            null
        }
    }

    // Log a message if the value was non-null but not of the expected type
    private fun typeWarning(
        key: String?,
        value: Any?,
        className: String,
        defaultValue: Any?,
        e: RuntimeException
    ) {
        val sb = StringBuilder()
        sb.append("Key ")
        sb.append(key)
        sb.append(" expected ")
        sb.append(className)
        if (value != null) {
            sb.append(" but value was a ")
            sb.append(value::class.canonicalName)
        } else {
            sb.append(" but value was of a different type")
        }
        sb.append(".  The default value ")
        sb.append(defaultValue)
        sb.append(" was returned.")
        println(sb.toString())
        println("Attempt to cast generated internal exception: $e")
    }

    private fun typeWarning(key: String?, value: Any?, className: String, e: RuntimeException) {
        typeWarning(key, value, className, "<null>", e)
    }

    override fun toString(): String = "$bundleData"
}

fun bundleOf(vararg pairs: Pair<String, Any?>): Bundle = Bundle(pairs.size).apply {
    for ((key, value) in pairs) {
        when (value) {
            null -> putString(key, null) // Any nullable type will suffice.

            // Scalars
            is Boolean -> putBoolean(key, value)
            is Byte -> putByte(key, value)
            is Char -> putChar(key, value)
            is Double -> putDouble(key, value)
            is Float -> putFloat(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Short -> putShort(key, value)

            // References
            is Bundle -> putBundle(key, value)
            is String -> putString(key, value)
            is CharSequence -> putCharSequence(key, value)

            // Scalar arrays
            is BooleanArray -> putBooleanArray(key, value)
            is ByteArray -> putByteArray(key, value)
            is CharArray -> putCharArray(key, value)
            is DoubleArray -> putDoubleArray(key, value)
            is FloatArray -> putFloatArray(key, value)
            is IntArray -> putIntArray(key, value)
            is LongArray -> putLongArray(key, value)
            is ShortArray -> putShortArray(key, value)

            // Reference arrays
            is ArrayList<*> -> putArrayList(key, value)
            // Perform extra round (with copy) because of compatibility purposes.
            // Unlike Android, `listOf` result might be not castable to `ArrayList`.
            is List<*> -> putArrayList(key, ArrayList(value))

            else -> throwIllegalValueType(key, value)
        }
    }
}

@Suppress("UNCHECKED_CAST")
private inline fun Bundle.putArrayList(key: String?, value: ArrayList<*>) {
    // Unlike JVM, there is no reflection available to check component type
    when (value.firstOrNull()) {
        is Int -> putIntegerArrayList(key, value as ArrayList<Int?>?)
        is String -> putStringArrayList(key, value as ArrayList<String?>?)
        else -> {
            if (value.isEmpty()) {
                putStringArrayList(key, value as ArrayList<String?>?)
            } else {
                throwIllegalValueType(key, value)
            }
        }
    }
}

private inline fun throwIllegalValueType(key: String?, value: Any): Nothing {
    val valueType = value::class.canonicalName
    throw IllegalArgumentException("Illegal value type $valueType for key \"$key\"")
}

internal val <T : Any> KClass<T>.canonicalName: String?
    get() = this.simpleName
