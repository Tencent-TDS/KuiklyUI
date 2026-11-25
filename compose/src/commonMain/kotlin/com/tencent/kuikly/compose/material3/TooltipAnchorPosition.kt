/*
 * Copyright 2023 The Android Open Source Project
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

package com.tencent.kuikly.compose.material3

import kotlin.jvm.JvmInline

/**
 * The positioning of the tooltip relative to the anchor.
 */
@JvmInline
value class TooltipAnchorPosition private constructor(private val value: Int) {
    override fun toString(): String {
        return when (this) {
            Above -> "Above"
            Below -> "Below"
            Left -> "Left"
            Right -> "Right"
            Start -> "Start"
            End -> "End"
            else -> "Invalid"
        }
    }

    companion object {
        /** Places the tooltip above the anchor */
        val Above = TooltipAnchorPosition(1)

        /** Places the tooltip below the anchor */
        val Below = TooltipAnchorPosition(2)

        /** Places the tooltip on the left of the anchor */
        val Left = TooltipAnchorPosition(3)

        /** Places the tooltip on the right of the anchor */
        val Right = TooltipAnchorPosition(4)

        /** Places the tooltip at the start of the anchor */
        val Start = TooltipAnchorPosition(5)

        /** Places the tooltip at the end of the anchor */
        val End = TooltipAnchorPosition(6)
    }
}
