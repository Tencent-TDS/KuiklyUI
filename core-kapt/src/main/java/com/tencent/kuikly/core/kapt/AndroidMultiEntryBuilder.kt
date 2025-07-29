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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

class AndroidMultiEntryBuilder(private val subModules: String) : AndroidTargetEntryBuilder() {
    override fun build(builder: FileSpec.Builder, pagesAnnotations: List<PageInfo>) {
        if (subModules.isEmpty()) {
            if (pagesAnnotations.isEmpty()) {
                return
            }
            builder.addType(
                TypeSpec.objectBuilder(entryFileName() + "_" + pagesAnnotations.first().moduleId)
                        .addFunction(createSubModuleRegisterPagesFuncSpec(pagesAnnotations))
                        .build()
            )
        } else {
            builder.addType(
                TypeSpec.classBuilder(entryFileName())
                        .addSuperinterface(ClassName("com.tencent.kuikly.core", "IKuiklyCoreEntry"))
                        .addProperty(createHadRegisterNativeBridgeProperty())
                        .addProperty(createDelegateProperty())
                        .addFunction(createCallKtMethodFuncSpec())
                        .addFunction(createPagesFuncSpec())
                        .build()
            )
        }
    }
    private fun createSubModuleRegisterPagesFuncSpec(
        pagesAnnotations: List<PageInfo>
    ): FunSpec {
        return FunSpec.builder("triggerRegisterPages")
                .addRegisterPageRouteStatement(pagesAnnotations)
                .build()
    }
    private fun createPagesFuncSpec(): FunSpec {
        return FunSpec.builder("triggerRegisterPages")
                .addModifiers(KModifier.OVERRIDE)
                .addMultiStatement()
                .build()
    }

    private fun FunSpec.Builder.addMultiStatement(): FunSpec.Builder {
        subModules.split("&").forEach {
            addStatement(entryFileName() + "_" + it + ".triggerRegisterPages()")
        }
        return this
    }

    override fun entryFileName(): String = "KuiklyCoreEntry"

    override fun packageName(): String = "com.tencent.kuikly.core.android"
}