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

import com.google.devtools.ksp.symbol.KSClassDeclaration
import impl.PageInfo

/**
 * Created by kam on 2022/6/25.
 */

fun String.iosFamily(): Boolean {
    return contains("ios")
}

fun String.androidJVMFamily(): Boolean {
    return contains("android")
}

fun String.ohosFamily(): Boolean {
    return contains("ohos")
}

private fun KSClassDeclaration.pageAnnotateValue(): String {
    var name = ""
    annotations.toList()[0].arguments.forEach {
        if (it.name?.asString() == "name") {
            name = (it.value as? String) ?: ""
        }
    }
    if (name.isEmpty()) {
        name = simpleName.asString()
    }
    return name
}

private fun KSClassDeclaration.supportInLocalAnnotateValue(): Boolean {
    val pageAnnotation = annotations.toList()[0]
    pageAnnotation.arguments.forEach {
        if (it.name?.asString() == "supportInLocal") {
            return (it.value as? Boolean) ?: false
        }
    }
    return false
}

@Synchronized
private fun KSClassDeclaration.moduleIdAnnotateValue(): String {
    annotations.toList()[0].arguments.forEach {
        if (it.name?.asString() == "moduleId") {
            return (it.value as? String) ?: ""
        }
    }
    return ""
}

fun KSClassDeclaration.toPageInfo(): PageInfo {
    return PageInfo(
        pageAnnotateValue(),
        qualifiedName!!.asString(),
        supportInLocalAnnotateValue(),
        moduleIdAnnotateValue()
    )
}

fun String.sourceSetBelow(startDirectoryName: String): String =
    substringAfter("/$startDirectoryName/").substringBefore("/kotlin/").substringAfterLast('/')