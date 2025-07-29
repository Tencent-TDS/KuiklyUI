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

package impl.submodule

import com.squareup.kotlinpoet.*
import impl.IOSTargetEntryBuilder
import impl.PageInfo

/**
 * Created by kam on 2022/6/25.
 */
class IOSMultiTargetEntryBuilder(private val isMainModule: Boolean, private val subModules: String, private val moduleId: String) : IOSTargetEntryBuilder() {

    override fun build(builder: FileSpec.Builder, pagesAnnotations: List<PageInfo>) {
        if (!isMainModule) {
            builder.addType(
                TypeSpec.objectBuilder(entryFileName() + "_" + moduleId)
                    .addFunction(createSubModuleRegisterPagesFuncSpec(pagesAnnotations))
                    .build()
            )
        } else {
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
    }

    private fun createSubModuleRegisterPagesFuncSpec(
        pagesAnnotations: List<PageInfo>,
    ) : FunSpec {
        return FunSpec.builder("triggerRegisterPages")
                .addRegisterPageRouteStatement(pagesAnnotations)
                .addSubModuleStatement()
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
                            .addSubModuleStatement()
                            .addStatement("$PROP_NAME_HAD_REGISTER_PAGES=true")
                            .addStatement("}")
                            .build()
                )
                .build()
    }

    private fun FunSpec.Builder.addSubModuleStatement(): FunSpec.Builder {
        subModules.split("&").forEach {
            val name = it.trim()
            if(name.isNotEmpty()) {
                addStatement(entryFileName() + "_" + name + ".triggerRegisterPages()")
            }
        }
        return this
    }
}