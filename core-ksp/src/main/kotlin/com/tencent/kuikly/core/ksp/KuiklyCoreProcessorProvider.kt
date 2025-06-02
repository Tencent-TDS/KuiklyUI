/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.kuikly.core.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.tencent.kuikly.core.annotations.Page
import impl.AndroidTargetEntryBuilder
import impl.KuiklyCoreAbsEntryBuilder
import impl.IOSTargetEntryBuilder
import impl.OhOsTargetEntryBuilder
import impl.OhOsTargetMultiEntryBuilder
import impl.PageInfo
import impl.submodule.AndroidMultiEntryBuilder
import impl.submodule.IOSMultiTargetEntryBuilder

/**
 * Created by kam on 2022/6/19.
 */

class KuiklyCoreProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return CoreProcessor(environment.codeGenerator, environment.logger, environment.options)
    }
}

class CoreProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val option: Map<String, String>,
) :
    SymbolProcessor {

    private var isInitialInvocation = true

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val newFiles = resolver.getNewFiles()
        if (!isInitialInvocation || newFiles.firstOrNull() == null) {
            // * A subsequent invocation is for processing generated files. We do not need to process these.
            // * If there are no new files to process, we avoid generating an output file, as this would break
            //   incremental compilation.
            //   TODO: This could be omitted if file generation were not required to discover the output source set.
            return emptyList()
        }
        isInitialInvocation = false
        codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true),
            packageName = "",
            fileName = "KuiklyCoreEntry",
            extensionName = "kt"
        ).use { output ->
            // TODO: This hack to discover the output source set should be replaced with a better solution.
            getEntryBuilder().also {
                buildEntryFile(resolver, it).forEach { fileSpec ->
                    output.write(fileSpec.toString().toByteArray())
                }
            }
        }
        return emptyList()
    }

    private fun buildEntryFile(
        resolver: Resolver,
        absEntryBuilder: KuiklyCoreAbsEntryBuilder,
    ): List<FileSpec> {
        val pageName = option["pageName"] ?: ""
        val packLocalAarBundle = option["packLocalAarBundle"] ?: ""
        val packBundleByModuleId = option["packBundleByModuleId"] ?: ""
        val pageClassDeclarations = mutableListOf<PageInfo>()
        val moduleSet = packBundleByModuleId.split("&").toSet()
        resolver.getSymbolsWithAnnotation(Page::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { classDeclaration ->
                val pageInfo = classDeclaration.toPageInfo()
                if (packLocalAarBundle == "1") {
                    if (pageInfo.packLocal) { // 只打包支持内置的page
                        pageClassDeclarations.add(pageInfo)
                    }
                } else if (pageName.isNotEmpty()) { // 全部page打成一个包
                    if (pageName == pageInfo.pageName) {
                        pageClassDeclarations.add(pageInfo)
                    }
                } else if (packBundleByModuleId.isNotEmpty()) { // 按照moduleId打包bundle
                    if (moduleSet.contains(pageInfo.moduleId)) {
                        pageClassDeclarations.add(pageInfo)
                    }
                } else {
                    pageClassDeclarations.add(pageInfo)
                }
            }
        return absEntryBuilder.build(pageClassDeclarations)
    }

    private fun getEntryBuilder(): KuiklyCoreAbsEntryBuilder{
        val enableMultiModule = option["enableMultiModule"]?.toBoolean() ?: false
        val isMainModule = option["isMainModule"]?.toBoolean() ?: false
        val subModules = option["subModules"] ?: ""
        val moduleId = option["moduleId"] ?: ""
        val outputSourceSet =
            codeGenerator.generatedFile.first().toString().sourceSetBelow("ksp")
        return when {
            outputSourceSet.androidJVMFamily() -> {
                if (enableMultiModule) {
                    AndroidMultiEntryBuilder(isMainModule, subModules, moduleId)
                } else {
                    AndroidTargetEntryBuilder()
                }
            }
            outputSourceSet.iosFamily() -> {
                if (enableMultiModule) {
                    IOSMultiTargetEntryBuilder(isMainModule, subModules, moduleId)
                } else {
                    IOSTargetEntryBuilder()
                }
            }
            outputSourceSet.ohosFamily() -> {
                if (enableMultiModule) {
                    OhOsTargetMultiEntryBuilder(isMainModule, subModules, moduleId)
                }else{
                    OhOsTargetEntryBuilder()
                }
            }
            else -> {
                AndroidTargetEntryBuilder()
            }
        }
    }
}
