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

package com.tencent.kuikly.core.annotations

/**
 * Kuikly页面注解
 * @property name 页面名字
 * @property supportInLocal 是否内置打包。true: 将所有true的页面打包成一个产物，然后可内置到宿主安装包，false: 不内置打包
 * @property moduleId 页面属于哪个模块，可用于按模块维度将页面打包
re */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Page(val name: String = "", val supportInLocal: Boolean = false, val moduleId: String = "")
