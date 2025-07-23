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

import com.squareup.kotlinpoet.*

/**
 * Created by kam on 2022/6/25.
 */
open class IOSTargetEntryBuilder : KuiklyCoreAbsEntryBuilder() {

    override fun build(builder: FileSpec.Builder, pagesAnnotations: List<PageInfo>) {
        builder.addType(
            TypeSpec.classBuilder(entryFileName())
                .addProperty(createDelegateProperty())
                .addProperty(createHadRegisterNativeBridgeProperty())
                .addFunction(createCallKtMethodFuncSpec(pagesAnnotations))
                .addType(createDelegateTypeSpec())
                .addType(createCompanionObject(pagesAnnotations))
                .build()
        )
    }

    override fun entryFileName(): String {
        return "KuiklyCoreEntry"
    }

    override fun packageName(): String {
        return ""
    }

    fun createDelegateProperty(): PropertySpec {
        return PropertySpec.builder(
            VAR_NAME_DELEGATE,
            ClassName("", HR_CORE_ENTRY_DELEGATE).copy(true)
        )
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

    fun createCallKtMethodFuncSpec(
        pagesAnnotations: List<PageInfo>,
    ): FunSpec {
        return FunSpec.builder(FUNC_NAME_CALL_KT_METHOD)
            .addParameters(createKtMethodParameters())
            .addStatement("try {")
            .addStatement(
                "if (!BridgeManager.isDidInit()) {\n" +
                        "BridgeManager.init()\n"
            )
            .addStatement("$METHOD_NAME_TRIGGER_REGISTER_PAGES()")
            .addStatement("}\n")
            .addStatement("        if (!hadRegisterNativeBridge) {\n" +
                    "            hadRegisterNativeBridge = true\n" +
                    "            val nativeBridge = NativeBridge()\n" +
                    "            nativeBridge.iosNativeBridgeDelegate = object : NativeBridge.IOSNativeBridgeDelegate {\n" +
                    "                override fun callNative(\n" +
                    "                    methodId: Int,\n" +
                    "                    arg0: Any?,\n" +
                    "                    arg1: Any?,\n" +
                    "                    arg2: Any?,\n" +
                    "                    arg3: Any?,\n" +
                    "                    arg4: Any?,\n" +
                    "                    arg5: Any?\n" +
                    "                ): Any? {\n" +
                    "                    return hrCoreDelegate?.callNative(methodId, arg0, arg1, arg2, arg3, arg4, arg5)\n" +
                    "                }\n" +
                    "            }\n" +
                    "            BridgeManager.registerNativeBridge(arg0 as String, nativeBridge)\n" +
                    "        }")
            .addStatement("BridgeManager.callKotlinMethod(methodId, arg0, arg1, arg2, arg3, arg4, arg5)")
            .addStatement("}")
            .addStatement("catch(t: Throwable) {")
            .addStatement("BridgeManager.callExceptionMethod(t.stackTraceToString())")
            .addStatement("}")
            .build()
    }

    fun createDelegateTypeSpec(): TypeSpec {
        return TypeSpec.interfaceBuilder(HR_CORE_ENTRY_DELEGATE)
            .addFunction(
                FunSpec.builder(AndroidTargetEntryBuilder.FUNC_NAME_CALL_NATIVE)
                    .addParameters(
                        createKtMethodParameters()
                    )
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(Any::class.asTypeName().copy(nullable = true))
                    .build()
            )
            .build()
    }

    private fun createCompanionObject(pagesAnnotations: List<PageInfo>): TypeSpec {
        return TypeSpec.companionObjectBuilder()
            .addProperty(
                PropertySpec.builder(PROP_NAME_HAD_REGISTER_PAGES, Boolean::class.asTypeName())
                    .mutable(true)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("false")
                    .build()
            )
            .addFunction(
                FunSpec.builder(METHOD_NAME_PAGE_EXIST)
                    .addParameter(ParameterSpec.builder(PARAM_NAME_PAGE_NAME, String::class.asTypeName()).build())
                    .addStatement("$METHOD_NAME_TRIGGER_REGISTER_PAGES()")
                    .addStatement("return BridgeManager.$METHOD_NAME_PAGE_EXIST($PARAM_NAME_PAGE_NAME)")
                    .returns(Boolean::class.asTypeName())
                    .build()
            )
            .addFunction(
                FunSpec.builder(METHOD_NAME_TRIGGER_REGISTER_PAGES)
                    .addModifiers(KModifier.PRIVATE)
                    .addStatement("if(!$PROP_NAME_HAD_REGISTER_PAGES) {")
                    .addRegisterPageRouteStatement(pagesAnnotations)
                    .addStatement("$PROP_NAME_HAD_REGISTER_PAGES=true")
                    .addStatement("}")
                    .build()
            )
            .build()
    }

    companion object {
        private const val HR_CORE_ENTRY_DELEGATE = "Delegate"
        private const val VAR_NAME_DELEGATE = "hrCoreDelegate"
        internal const val METHOD_NAME_TRIGGER_REGISTER_PAGES = "triggerRegisterPages"
        internal const val METHOD_NAME_PAGE_EXIST = "isPageExist"
        internal const val PROP_NAME_HAD_REGISTER_PAGES = "hadRegisterPages"
        internal const val PARAM_NAME_PAGE_NAME = "pageName"
    }
}