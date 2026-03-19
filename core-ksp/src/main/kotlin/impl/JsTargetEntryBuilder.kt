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

class JsTargetEntryBuilder(val catchException: Boolean) : KuiklyCoreAbsEntryBuilder() {

    override fun build(
        builder: FileSpec.Builder,
        pagesAnnotations: List<PageInfo>,
    ) {
        builder.addType(
            TypeSpec.classBuilder(entryFileName())
                .addSuperinterface(ClassName("com.tencent.kuikly.core", "IKuiklyCoreEntry"))
                .addProperty(createHadRegisterNativeBridgeProperty())
                .addProperty(createDelegateProperty())
                .addFunction(createCatchExceptionFuncSpec())
                .addFunction(createCallKtMethodFuncSpec())
                .addFunction(createTriggerRegisterPagesFuncSpec(pagesAnnotations))
                .build()
        )
    }

    internal fun createDelegateProperty(): PropertySpec {
        return PropertySpec.builder(
            "delegate",
            ClassName("com.tencent.kuikly.core.IKuiklyCoreEntry", "Delegate").copy(true)
        )
            .addModifiers(KModifier.OVERRIDE)
            .mutable()
            .initializer("null")
            .build()
    }

    internal fun createHadRegisterNativeBridgeProperty(): PropertySpec {
        return PropertySpec.builder(
            "hadRegisterNativeBridge",
            Boolean::class.asTypeName()
        )
            .addModifiers(KModifier.PRIVATE)
            .mutable()
            .initializer("false")
            .build()
    }

    internal fun createCatchExceptionFuncSpec(): FunSpec {
        return FunSpec.builder(FUNC_NAME_CATCH_EXCEPTION_METHOD)
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("return BridgeManager.catchException")
            .returns(BOOLEAN)
            .build()
    }

    internal fun createCallKtMethodFuncSpec(): FunSpec {
        return FunSpec.builder(FUNC_NAME_CALL_KT_METHOD)
            .addParameters(createKtMethodParameters())
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("if (!hadRegisterNativeBridge) {\n")
                    .addStatement("triggerRegisterPages()\n")
            .addStatement("          hadRegisterNativeBridge = true\n" +
                    "          val nativeBridge = NativeBridge()\n" +
                    "          BridgeManager.registerNativeBridge(arg0 as String, nativeBridge)\n" +
                    "      }")
            .addStatement("BridgeManager.callKotlinMethod(methodId, arg0, arg1, arg2, arg3, arg4, arg5)")
            .build()
    }

    private fun createTriggerRegisterPagesFuncSpec(
        pagesAnnotations: List<PageInfo>,
    ) : FunSpec {
        return FunSpec.builder("triggerRegisterPages")
            .addModifiers(KModifier.OVERRIDE)
            .addRegisterPageRouteStatement(pagesAnnotations)
            .build()
    }

    override fun entryFileName(): String = "KuiklyCoreEntry"

    override fun packageName(): String {
        return "com.tencent.kuikly.core.js"
    }

}
