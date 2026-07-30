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
 * Created by kam on 2022/6/25.
 */
abstract class KuiklyCoreAbsEntryBuilder {

    fun build(
        pagesAnnotations: List<PageInfo>
    ): List<FileSpec> {
        val fileSpecs = mutableListOf<FileSpec>()
        val fileSpecBuilder = FileSpec.builder(packageName(), entryFileName())
        for (comment in getCommonComments(pagesAnnotations)) {
            fileSpecBuilder.addComment(comment)
        }
        fileSpecBuilder
            .addComment("\nthis file is generating by ksp\n")
            .addComment("please do not modified it!!!")
            .addImport("com.tencent.kuikly.core.manager", "BridgeManager")
            .addImport(PACKAGE_NAME_NVI, "NativeBridge")
        build(fileSpecBuilder, pagesAnnotations)
        fileSpecs.add(fileSpecBuilder.build())
        return fileSpecs
    }

    abstract fun build(
        builder: FileSpec.Builder,
        pagesAnnotations: List<PageInfo>
    )

    abstract fun entryFileName(): String

    abstract fun packageName(): String

    protected open fun getCommonComments(pagesAnnotations: List<PageInfo>): List<String> {
        return emptyList()
    }

    protected fun createKtMethodParameters(): List<ParameterSpec> {
        return listOf(
            ParameterSpec.builder("methodId", Int::class.asTypeName()).build(),
            createNullableAnyParameterSpec("arg0"),
            createNullableAnyParameterSpec("arg1"),
            createNullableAnyParameterSpec("arg2"),
            createNullableAnyParameterSpec("arg3"),
            createNullableAnyParameterSpec("arg4"),
            createNullableAnyParameterSpec("arg5")
        )
    }

    protected fun createNullableAnyParameterSpec(name: String): ParameterSpec =
        ParameterSpec.builder(name, Any::class.asTypeName().copy(nullable = true)).build()

    fun FunSpec.Builder.addRegisterPageRouteStatement(pagesAnnotations: List<PageInfo>): FunSpec.Builder {
        pagesAnnotations.forEach { info ->
            addStatement(createRegisterRouter(info.pageName, info.pageFullName))
        }
        return this
    }

    private fun createRegisterRouter(pageName: String, pageQualifiedName: String): String {
        return "BridgeManager.registerPageRouter(\"$pageName\") {\n" +
                "$pageQualifiedName()\n" +
                "}"
    }

    companion object {
        const val PACKAGE_NAME_NVI = "com.tencent.kuikly.core.nvi"

        const val FUNC_NAME_CALL_KT_METHOD = "callKotlinMethod"
    }

}