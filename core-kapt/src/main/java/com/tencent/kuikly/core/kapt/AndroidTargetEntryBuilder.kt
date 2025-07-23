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

package com.tencent.kuikly.core.kapt

import com.squareup.kotlinpoet.*

/**
 * Created by kam on 2022/6/26.
 */
open class AndroidTargetEntryBuilder : KuiklyCoreAbsEntryBuilder() {

    override fun build(
        builder: FileSpec.Builder,
        pagesAnnotations: List<PageInfo>
    ) {
        builder.addType(
            TypeSpec.classBuilder(entryFileName())
                .addSuperinterface(ClassName("com.tencent.kuikly.core", "IKuiklyCoreEntry"))
                .addProperty(createHadRegisterNativeBridgeProperty())
                .addProperty(createDelegateProperty())
                .addFunction(createCallKtMethodFuncSpec())
                .addFunction(createTriggerRegisterPagesFuncSpec(pagesAnnotations))
                .build()
        )
    }

    fun createDelegateProperty(): PropertySpec {
        return PropertySpec.builder(
            "delegate",
            ClassName("com.tencent.kuikly.core.IKuiklyCoreEntry", INTERFACE_NAME_DELEGATE).copy(true)
        )
            .addModifiers(KModifier.OVERRIDE)
            .mutable()
            .initializer("null")
            .build()
    }

    fun createHadRegisterNativeBridgeProperty(): PropertySpec {
        return PropertySpec.builder(
            "hadRegisterNativeBridge",
            Boolean::class.asTypeName()
        )
            .addModifiers(KModifier.PRIVATE)
            .mutable()
            .initializer("false")
            .build()
    }

    fun createCallKtMethodFuncSpec(): FunSpec {
        return FunSpec.builder(FUNC_NAME_CALL_KT_METHOD)
            .addParameters(createKtMethodParameters())
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("if (!hadRegisterNativeBridge) {\n")
            .addStatement("triggerRegisterPages()")
            .addStatement("          hadRegisterNativeBridge = true\n" +
                    "          val nativeBridge = NativeBridge()\n" +
                    "          nativeBridge.delegate = object : NativeBridge.NativeBridgeDelegate {\n" +
                    "              override fun callNative(\n" +
                    "                    methodId: Int,\n" +
                    "                    arg0: Any?,\n" +
                    "                    arg1: Any?,\n" +
                    "                    arg2: Any?,\n" +
                    "                    arg3: Any?,\n" +
                    "                    arg4: Any?,\n" +
                    "                    arg5: Any?\n" +
                    "              ): Any? {\n" +
                    "                  return delegate?.callNative(methodId, arg0, arg1, arg2, arg3, arg4,\n" +
                    "                        arg5)\n" +
                    "              }\n" +
                    "          }\n" +
                    "          BridgeManager.registerNativeBridge(arg0 as String, nativeBridge)\n" +
                    "      }")
//            .addRegisterPageRouteStatement(pagesAnnotations)
            .addStatement("BridgeManager.callKotlinMethod(methodId, arg0, arg1, arg2, arg3, arg4, arg5)")
            .build()
    }

    private fun createTriggerRegisterPagesFuncSpec(
        pagesAnnotations: List<PageInfo>
    ): FunSpec {
        return FunSpec.builder("triggerRegisterPages")
            .addModifiers(KModifier.OVERRIDE)
            .addRegisterPageRouteStatement(pagesAnnotations)
            .build()
    }

    private fun createDelegateTypeSpec(): TypeSpec {
        return TypeSpec.interfaceBuilder(INTERFACE_NAME_DELEGATE)
            .addFunction(
                FunSpec.builder(FUNC_NAME_CALL_NATIVE)
                    .addParameters(
                        createKtMethodParameters()
                    )
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(Any::class.asTypeName().copy(nullable = true))
                    .build()
            )
            .build()
    }

    private fun createCompanionObjectBuilder(pagesAnnotations: List<PageInfo>): TypeSpec {
        return TypeSpec.companionObjectBuilder()
            .addFunction(
                FunSpec.builder("registerAllPages")
                    .addStatement(
                        "if (!BridgeManager.isDidInit()) {\n" +
                                "BridgeManager.init()\n"
                    )
                    .addRegisterPageRouteStatement(pagesAnnotations)
                    .addStatement("}\n")
                    .build()
            )
            .build()
    }

    override fun entryFileName(): String = "KuiklyCoreEntry"

    override fun packageName(): String = "com.tencent.kuikly.core.android"

    companion object {
        const val FUNC_NAME_CALL_NATIVE = "callNative"
        private const val INTERFACE_NAME_DELEGATE = "Delegate"
    }
}