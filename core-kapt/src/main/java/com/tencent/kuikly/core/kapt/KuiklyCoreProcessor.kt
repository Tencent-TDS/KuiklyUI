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

import com.tencent.kuikly.core.annotations.Page
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

/**
 * Created by kam on 2022/11/9.
 */
class KuiklyCoreProcessor : AbstractProcessor() {

    private lateinit var mFiler: Filer
    private lateinit var options: Map<String, Any>

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        mFiler = processingEnv.filer
        options = processingEnv.options
    }

    override fun process(
        set: MutableSet<out TypeElement>,
        roundEnvironment: RoundEnvironment
    ): Boolean {

        if (set.isEmpty()) {
            return true
        }

        val pageName = (options["pageName"] as? String) ?: ""
        val packBundleByModuleId = (options["packBundleByModuleId"] as? String?) ?: ""
        val packLocalAar = (options["packLocalAar"] as? String) ?: ""
        println("kapt: pgName: $pageName")
        println("kapt: packLocalAar$packLocalAar")
        println("kapt: packBundleByModuleId: $packBundleByModuleId")
        val pageAnnotations = roundEnvironment.getElementsAnnotatedWith(Page::class.java)
        val pageInfoList = mutableListOf<PageInfo>()
        pageAnnotations.forEach {
            val type = it as TypeElement
            val pageInfo = type.toPageInfo()
            if (packLocalAar.isNotEmpty()) {
                val pair = parsePackLocalAarParams(packLocalAar)
                val forcePackAarList = pair.first
                val notPackAarList = pair.second
                if (forcePackAarList.contains(pageInfo.pageName)) {
                    pageInfoList.add(pageInfo)
                } else if (pageInfo.packLocal && !notPackAarList.contains(pageInfo.pageName)) {
                    pageInfoList.add(pageInfo)
                }
            } else if (packBundleByModuleId.isNotEmpty()) {
                val moduleSet = packBundleByModuleId.split("&").toSet()
                if (moduleSet.contains(pageInfo.moduleId)) {
                    pageInfoList.add(pageInfo)
                }
            } else if (pageName.isNotEmpty()) {
                if (pageName == pageInfo.pageName) {
                    pageInfoList.add(pageInfo)
                }
            } else {
                pageInfoList.add(type.toPageInfo())
            }
        }

        getAndroidEntry().build(pageInfoList).forEach {
            it.writeTo(mFiler)
        }
        return true
    }

    private fun getAndroidEntry(): KuiklyCoreAbsEntryBuilder {
        val enableMultiModule = (options["enableMultiModule"] as? Boolean) ?: false
        val subModules = options["subModules"] as? String ?: ""
        return if (enableMultiModule) AndroidMultiEntryBuilder(subModules) else AndroidTargetEntryBuilder()
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            Page::class.java.canonicalName
        )
    }

    private fun TypeElement.toPageInfo(): PageInfo {
        var name = getAnnotation(Page::class.java).name
        if (name.isEmpty()) {
            name = simpleName.toString()
        }
        return PageInfo(
            name, qualifiedName.toString(), getAnnotation(Page::class.java).supportInLocal,
            getAnnotation(Page::class.java).moduleId
        )
    }

    private fun parsePackLocalAarParams(packLocalAar: String): Pair<List<String>, List<String>> {
        val packLocalAarSpilt = packLocalAar.split(",")
        return Pair(packLocalAarSpilt[0].split("|").toList(), packLocalAarSpilt[1].split("|").toList())
    }
}