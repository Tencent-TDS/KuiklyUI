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

package impl

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.asTypeName

/**
 * Created by kamlin on 2024/6/5.
 */
class OhOsTargetMultiEntryBuilder(private val isMainModule: Boolean, private val subModules: String, private val moduleId: String) : KuiklyCoreAbsEntryBuilder() {
    override fun build(builder: FileSpec.Builder, pagesAnnotations: List<PageInfo>) {
        with(builder) {
            if(!isMainModule){
                addFunction(createInitKuiklySubmoduleMethod(pagesAnnotations))
            }else {
                builder.addImport("com.tencent.kuikly.core.exception", "ExceptionTracker")
                addImport("kotlinx.cinterop", "memScoped")
                addImport("kotlinx.cinterop", "invoke")
                addImport("com.tencent.kuikly.core.utils", "asString")
                addImport("com.tencent.kuikly.core.manager", "KotlinMethod")
                addImport("kotlinx.cinterop", "staticCFunction")
                addImport("ohos", "com_tencent_kuikly_SetCallKotlin")

                addFunction(createCallNativeFunc())
                addFunction(createInitKuiklyMethod(pagesAnnotations))
            }
        }
    }

    override fun entryFileName(): String {
        return ""
    }

    override fun packageName(): String {
        return ""
    }

    private fun createInitKuiklyMethod(
        pagesAnnotations: List<PageInfo>,
    ): FunSpec {
        return FunSpec.builder("initKuikly")
            .addAnnotations(createCFuncAnnotations())
            .returns(Int::class)
            .addStatement(
                "if (!BridgeManager.isDidInit()) {\n" +
                        "BridgeManager.init()\n"
            )
            .addRegisterPageRouteStatement(pagesAnnotations)
            .addSubModuleStatement()
            .addStatement("}\n")
            .addStatement("""
                return com_tencent_kuikly_SetCallKotlin(staticCFunction { methodId, arg0, arg1, arg2, arg3, arg4, arg5 ->
                            try {
                                if (methodId == KotlinMethod.CREATE_INSTANCE) {
                                    val nativeBridge = NativeBridge()
                                    nativeBridge.callNativeCallback = { methodId, arg0, arg1, arg2, arg3, arg4, arg5 ->
                                        callNative(methodId, arg0, arg1, arg2, arg3, arg4, arg5)
                                    }
                                    BridgeManager.registerNativeBridge(arg0.asString(), nativeBridge)
                                }
                                BridgeManager.callKotlinMethod(
                                     methodId,
                                     arg0.toAny(),
                                     arg1.toAny(),
                                     arg2.toAny(),
                                     arg3.toAny(),
                                     arg4.toAny(),
                                     arg5.toAny()
                                )
                            } catch(t: Throwable){
                                ExceptionTracker.notifyKuiklyException(t)
                            }
                })
            """.trimIndent())
            .build()
    }

    private fun createInitKuiklySubmoduleMethod(
        pagesAnnotations: List<PageInfo>,
    ): FunSpec {
        return FunSpec.builder("initKuikly_" + moduleId)
            .addAnnotations(createCFuncAnnotations())
            .addRegisterPageRouteStatement(pagesAnnotations)
            .build()
    }

    private fun FunSpec.Builder.addSubModuleStatement(): FunSpec.Builder {
        subModules.split("&").forEach {
            val name = it.trim()
            if(name.isNotEmpty()) {
                addStatement("initKuikly_" + name + "()")
            }
        }
        return this
    }

    private fun createCFuncAnnotations(): List<AnnotationSpec> {
        return listOf(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember("kotlinx.cinterop.ExperimentalForeignApi::class")
                .build()
        )
    }

    private fun createCallNativeFunc(): FunSpec {
        val optInAnnotation = ClassName("kotlin", "OptIn")
        val experimentalForeignApi = ClassName("kotlinx.cinterop", "ExperimentalForeignApi")
        val experimentalNativeApi = ClassName("kotlin.experimental", "ExperimentalNativeApi")
        val toKRRenderCValue = ClassName("com.tencent.kuikly.core.utils", "toKRRenderCValue")
        val toAny = ClassName("com.tencent.kuikly.core.utils", "toAny")

        return FunSpec.builder("callNative")
            .addModifiers(KModifier.PRIVATE)
            .addAnnotation(AnnotationSpec.builder(optInAnnotation).apply {
                addMember("%T::class", experimentalForeignApi)
                addMember("%T::class", experimentalNativeApi)
            }.build())
            .addParameter("methodId", Int::class)
            .addParameter("arg0", Any::class.asTypeName().copy(nullable = true))
            .addParameter("arg1", Any::class.asTypeName().copy(nullable = true))
            .addParameter("arg2", Any::class.asTypeName().copy(nullable = true))
            .addParameter("arg3", Any::class.asTypeName().copy(nullable = true))
            .addParameter("arg4", Any::class.asTypeName().copy(nullable = true))
            .addParameter("arg5", Any::class.asTypeName().copy(nullable = true))
            .returns(Any::class.asTypeName().copy(nullable = true))
            .addCode(
                """
            |return memScoped {
            |    val cValue = ohos.com_tencent_kuikly_CallNative(
            |        methodId,
            |        arg0.%T(this),
            |        arg1.%T(this),
            |        arg2.%T(this),
            |        arg3.%T(this),
            |        arg4.%T(this),
            |        arg5.%T(this)
            |    )?.%T()
            |    cValue
            |}
        """.trimMargin(),
                toKRRenderCValue,
                toKRRenderCValue,
                toKRRenderCValue,
                toKRRenderCValue,
                toKRRenderCValue,
                toKRRenderCValue,
                toAny
            )
            .build()
    }

}