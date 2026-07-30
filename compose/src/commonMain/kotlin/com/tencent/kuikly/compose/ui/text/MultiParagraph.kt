/*
 * Copyright 2019 The Android Open Source Project
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

package com.tencent.kuikly.compose.ui.text

import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.unit.Constraints

/**
 * Lays out and renders multiple paragraphs at once. Unlike [Paragraph], supports multiple
 * [ParagraphStyle]s in a given text.
 *
 * @param intrinsics previously calculated text intrinsics
 * @param constraints how wide and tall the text is allowed to be. [Constraints.maxWidth]
 * will define the width of the MultiParagraph. [Constraints.maxHeight] helps defining the
 * number of lines that fit with ellipsis is true. Minimum components of the [Constraints]
 * object are no-op.
 * @param maxLines the maximum number of lines that the text can have
 * @param ellipsis whether to ellipsize text, applied only when [maxLines] is set
 */
class MultiParagraph(
    val lineCount: Int = 0,
    val placeholderRects: List<Rect?>
) {

}
