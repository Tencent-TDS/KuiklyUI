/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.resources

import androidx.compose.runtime.Immutable
import com.tencent.kuikly.compose.ui.text.font.Font
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.font.KuiklyFont
import com.tencent.kuikly.compose.ui.util.fastJoinToString
import kotlin.jvm.JvmInline

@Immutable
@JvmInline
value class FontResource
@InternalResourceApi constructor(internal val value: String)

/**
 * Creates a font using the specified font resource, weight, and style.
 *
 * @param resource The font resource to be used.
 * @param weight The weight of the font. Default value is [FontWeight.Normal].
 * @param style The style of the font. Default value is [FontStyle.Normal].
 *
 * @return The created [Font] object.
 *
 * @throws NotFoundException if the specified resource ID is not found.
 */
fun Font(
    resource: FontResource,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = KuiklyFont(resource.value, weight, style)

fun List<Font>.toKuiklyFontFamily(): String = fastJoinToString(",") {
    (it as? KuiklyFont)?.fontName ?: "Unknown"
}