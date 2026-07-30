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

package com.tencent.kuikly.core.render.android.expand.vendor

import java.lang.reflect.*

/**
 * 反射封装类
 */
class KRReflect private constructor(val clazz: Class<*>, private var instance: Any?) {

    // 构造方法操作区
    /**
     * 使用匹配参数的构造函数创建一个对象实例，并生成新的KRReflect实例返回
     */
    fun instance(vararg args: Any?): KRReflect {
        return getConstructor(*types(*args)).newInstance(*args)
    }

    /**
     * 根据传入的参数类型匹配对应的构造器
     */
    fun getConstructor(vararg types: Class<*>): ConstructorReflect {
        val constructor: Constructor<*> = try {
            clazz.getDeclaredConstructor(*types)
        } catch (e: NoSuchMethodException) {
            var matched: Constructor<*>? = null
            for (constructor in clazz.declaredConstructors) {
                if (match(constructor.parameterTypes, types)) {
                    matched = constructor
                    break
                }
            }
            matched ?: throw ReflectException("")
        }
        return ConstructorReflect(accessible(constructor), this)
    }

    // 成员变量操作区
    /**
     * 为指定name的成员变量赋值为value
     */
    fun setField(name: String, value: Any?): KRReflect {
        getField(name).setValue(value)
        return this
    }

    /**
     * 获取指定字段name的具体值(包括父类中的字段)
     */
    fun <T> getFieldValue(name: String): T? {
        return getField(name).getValue()
    }

    /**
     * 根据指定name获取对应的FieldReflect
     */
    fun getField(name: String): FieldReflect {
        var type: Class<*>? = clazz

        val field = try {
            accessible(type!!.getField(name))
        } catch (e: NoSuchFieldException) {
            var find: Field? = null
            do {
                try {
                    find = accessible(type!!.getDeclaredField(name))
                    if (find != null) {
                        break
                    }
                } catch (ignore: NoSuchFieldException) {
                }
                type = type?.superclass
            } while (type != null)

            find ?: throw ReflectException(e)
        }
        return FieldReflect(field, this)
    }

    // 普通方法操作区
    /**
     * 执行指定name的方法, 并将方法的返回值作为新数据。创建出对应的KRReflect实例并返回
     */
    fun callWithReturn(name: String, vararg args: Any?): KRReflect {
        return getMethod(name, *types(*args)).callWithReturn(*args)
    }

    /**
     * 获取与此name与参数想匹配的Method实例
     */
    fun getMethod(name: String, vararg types: Class<*>): MethodReflect {
        var find: Method? = null
        var type: Class<*>? = clazz
        while (type != null) {
            try {
                find = type.getDeclaredMethod(name, *types)
                break
            } catch (e: NoSuchMethodException) {
                for (method in type.declaredMethods) {
                    if (method.name == name && match(method.parameterTypes, types)) {
                        find = method
                        break
                    }
                }
                type = type.superclass
            }
        }
        accessible(find ?: throw RuntimeException("method:$name not found"))
        return MethodReflect(find, this)
    }
    /**
     * 获取与此KRReflect相绑定的实例。
     */
    fun <T> get(): T? {
        return try {
            @Suppress("UNCHECKED_CAST")
            instance as T?
        } catch (e: ClassCastException) {
            null
        }
    }

    // 检查是否存在有效的可操作实例。若不存在则抛出异常。
    private fun checkInstance() {

    }

    override fun toString(): String {
        return "KRReflect(clazz=$clazz, instance=$instance)"
    }

    companion object {
        @JvmStatic
        fun create(clazz: Class<*>, any: Any? = null): KRReflect {
            return KRReflect(clazz, any)
        }

        @JvmStatic
        fun create(any: Any): KRReflect {
            return when (any) {
                is Class<*> -> create(any)
                is String -> create(any)
                else -> create(any.javaClass, any)
            }
        }

        @JvmStatic
        fun create(name: String, loader: ClassLoader? = null): KRReflect {
            return try {
                if (loader == null) {
                    create(Class.forName(name))
                } else {
                    create(Class.forName(name, true, loader))
                }
            } catch (e: Exception) {
                KRReflect(name.javaClass, name)
            }
        }

        @JvmStatic
        fun types(vararg args: Any?): Array<Class<*>> {
            if (args.isEmpty()) {
                return arrayOf()
            }
            return Array(args.size) { index -> args[index]?.javaClass ?: Void::class.java }
        }

        @JvmStatic
        fun <T : AccessibleObject> accessible(accessible: T): T {
            if (!accessible.isAccessible) {
                accessible.isAccessible = true
            }
            return accessible
        }

        @JvmStatic
        fun match(declaredTypes: Array<out Class<*>>, actualTypes: Array<out Class<*>>): Boolean {
            if (declaredTypes.size != actualTypes.size) return false
            for ((index, declared) in declaredTypes.withIndex()) {
                val actualType = actualTypes[index]
                if (actualType == Void::class.java && !declared.isPrimitive) {
                    continue
                }
                if (box(declared).isAssignableFrom(box(actualTypes[index]))) {
                    continue
                }
                return false
            }
            return true
        }

        @JvmStatic
        fun box(source: Class<*>): Class<*> = when (source.name) {
            "byte" -> Class.forName("java.lang.Byte")
            "short" -> Class.forName("java.lang.Short")
            "int" -> Class.forName("java.lang.Integer")
            "long" -> Class.forName("java.lang.Long")
            "float" -> Class.forName("java.lang.Float")
            "double" -> Class.forName("java.lang.Double")
            "boolean" -> Class.forName("java.lang.Boolean")
            "char" -> Class.forName("java.lang.Character")
            else -> source
        }
    }

    class ConstructorReflect(
        val constructor: Constructor<*>,
        @Suppress("unused") val upper: KRReflect
    ) {
        // 参数是否为可变参数
        fun newInstance(vararg args: Any?): KRReflect {
            return create(constructor.newInstance(*args))
        }

    }

    // 成员方法反射操作类
    class MethodReflect(val method: Method, val upper: KRReflect) {
        val isStatic = Modifier.isStatic(method.modifiers)

        fun callWithReturn(vararg args: Any?): KRReflect {
            val value = if (isStatic) {
                method.invoke(upper.clazz, *args)
            } else {
                upper.checkInstance()
                method.invoke(upper.instance, *args)
            }

            return create(value ?: "")
        }
    }

    // 成员变量反射操作类
    class FieldReflect(val field: Field, val upper: KRReflect) {
        val isStatic = Modifier.isStatic(field.modifiers)

        @Suppress("UNCHECKED_CAST")
        fun <T> getValue(): T? {
            return try {
                if (isStatic) {
                    field.get(upper.clazz) as T
                } else {
                    upper.checkInstance()
                    field.get(upper.instance) as T
                }
            } catch (e: Exception) {
                null
            }
        }

        fun setValue(value: Any?): FieldReflect {
            if (isStatic) {
                field.set(upper.clazz, value)
            } else {
                upper.checkInstance()
                field.set(upper.instance, value)
            }
            return this
        }
    }
}

/**
 * 用于在进行反射操作过程中。对受检异常错误进行包裹。
 */
class ReflectException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
}