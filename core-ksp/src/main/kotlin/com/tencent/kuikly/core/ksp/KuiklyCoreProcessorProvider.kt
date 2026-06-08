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

package com.tencent.kuikly.core.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
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
        if (!isInitialInvocation) {
            // A subsequent invocation is for processing generated files. We do not need to process these.
            logger.warn("skip subsequent invocation")
            return emptyList()
        }
        isInitialInvocation = false

        val enableMultiModule = option["enableMultiModule"]?.toBoolean() ?: false
        val isMainModule = option["isMainModule"]?.toBoolean() ?: false
        val moduleId = option["moduleId"] ?: ""

        val pageAnnotationName = Page::class.qualifiedName!!
        val pageClasses = resolver.getSymbolsWithAnnotation(pageAnnotationName)
            .filterIsInstance<KSClassDeclaration>()
        val pages = pageClasses.map {
            logger.info("new file with @Page: $it")
            it.containingFile!!
        }
            .toList()
            .toTypedArray()

        val currentModulePages = pageClasses.map { it.toPageInfo() }.toList()

        // 主模块额外扫描子模块页面一起判重，非主模块仅做包内判重
        val subModulePages = if (enableMultiModule && isMainModule) {
            discoverSubModulePages(resolver)
        } else {
            emptyList()
        }
        val duplicates = (currentModulePages + subModulePages)
            .groupBy { it.pageName }.filter { it.value.size > 1 }
        if (duplicates.isNotEmpty()) {
            val msg = buildString {
                appendLine("发现重复的 @Page 页面名称，将导致运行时路由覆盖：")
                duplicates.forEach { (name, dupPages) ->
                    appendLine("  页面名 \"$name\" 被以下类使用：")
                    dupPages.forEach { appendLine("    - ${it.pageFullName}") }
                }
            }
            error(msg)
        }

        codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, *pages),
            packageName = "",
            fileName = "KuiklyCoreEntry",
            extensionName = "kt"
        ).use { output ->
            buildEntryFile(pageClasses, getEntryBuilder()).forEach { fileSpec ->
                output.write(fileSpec.toString().toByteArray())
            }
        }

        // 子模块：生成 KuiklyPages_<moduleId> 注解文件，供主模块扫描发现
        if (enableMultiModule && !isMainModule && moduleId.isNotEmpty()) {
            generatePageRegistry(currentModulePages, moduleId)
        }

        return emptyList()
    }

    private fun buildEntryFile(
        pageClasses: Sequence<KSClassDeclaration>,
        absEntryBuilder: KuiklyCoreAbsEntryBuilder,
    ): List<FileSpec> {
        val pageName = option["pageName"] ?: ""
        val packLocalAarBundle = option["packLocalAarBundle"] ?: ""
        val packBundleByModuleId = option["packBundleByModuleId"] ?: ""
        val pageClassDeclarations = mutableListOf<PageInfo>()
        val moduleSet = packBundleByModuleId.split("&").toSet()

        pageClasses.forEach { classDeclaration ->
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
        val caughtException = option["caughtException"]?.toBoolean() ?: (option["catchException"]?.toBoolean() ?: true)
        val outputSourceSet =
            codeGenerator.generatedFile.first().toString().sourceSetBelow("ksp")
        return when {
            outputSourceSet.androidJVMFamily() -> {
                if (enableMultiModule) {
                    AndroidMultiEntryBuilder(caughtException, isMainModule, subModules, moduleId)
                } else {
                    AndroidTargetEntryBuilder(caughtException)
                }
            }
            outputSourceSet.iosFamily() -> {
                if (enableMultiModule) {
                    IOSMultiTargetEntryBuilder(caughtException, isMainModule, subModules, moduleId)
                } else {
                    IOSTargetEntryBuilder(caughtException)
                }
            }
            outputSourceSet.ohosFamily() -> {
                if (enableMultiModule) {
                    OhOsTargetMultiEntryBuilder(caughtException, isMainModule, subModules, moduleId)
                }else{
                    OhOsTargetEntryBuilder(caughtException)
                }
            }
            else -> {
                AndroidTargetEntryBuilder(caughtException)
            }
        }
    }

    // 子模块：在 ksp 输出目录下生成 KuiklyPages_<moduleId>，用注解存储本模块全部页面信息
    private fun generatePageRegistry(pages: List<PageInfo>, moduleId: String) {
        if (pages.isEmpty()) return
        val registryClassName = "$REGISTRY_CLASS_PREFIX$moduleId"
        val pagesValue = pages.joinToString(", ") { "\"${it.pageName}::${it.pageFullName}\"" }
        val annotationSpec = AnnotationSpec
            .builder(ClassName("com.tencent.kuikly.core.annotations", "KuiklyModulePages"))
            .addMember("pages = [$pagesValue]")
            .build()
        val objectSpec = TypeSpec.objectBuilder(registryClassName)
            .addModifiers(KModifier.INTERNAL)
            .addAnnotation(annotationSpec)
            .build()
        val fileSpec = FileSpec.builder(REGISTRY_PACKAGE, registryClassName)
            .addComment("\nthis file is generated by ksp\n")
            .addComment("please do not modify it!!!")
            .addType(objectSpec)
            .build()
        codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true),
            packageName = REGISTRY_PACKAGE,
            fileName = registryClassName,
            extensionName = "kt"
        ).use { it.write(fileSpec.toString().toByteArray()) }
    }

    // 主模块：从 classpath 扫描所有子模块的 KuiklyPages_* 注解，汇总页面信息
    @OptIn(KspExperimental::class)
    private fun discoverSubModulePages(resolver: Resolver): List<PageInfo> {
        return resolver.getDeclarationsFromPackage(REGISTRY_PACKAGE)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.simpleName.asString().startsWith(REGISTRY_CLASS_PREFIX) }
            .flatMap { classDecl ->
                val ann = classDecl.annotations
                    .firstOrNull { it.shortName.asString() == "KuiklyModulePages" }
                    ?: return@flatMap emptyList()
                val pagesArg = ann.arguments.firstOrNull { it.name?.asString() == "pages" }
                @Suppress("UNCHECKED_CAST")
                val entries = (pagesArg?.value as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                entries.mapNotNull { entry ->
                    val parts = entry.split("::")
                    if (parts.size == 2) PageInfo(parts[0], parts[1]) else null
                }
            }
            .toList()
    }

    companion object {
        private const val REGISTRY_PACKAGE = "com.tencent.kuikly.ksp"
        private const val REGISTRY_CLASS_PREFIX = "KuiklyPages_"
    }
}
