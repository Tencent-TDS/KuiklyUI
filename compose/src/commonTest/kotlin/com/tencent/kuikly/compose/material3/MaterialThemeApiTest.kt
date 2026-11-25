/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.compose.material3

import androidx.compose.runtime.Composable
import kotlin.test.Test

class MaterialThemeApiTest {

    /**
     * Guards the two public call forms at test-compilation time. A future overload
     * collision or removed legacy signature makes this test source fail to compile.
     */
    @Test
    fun overloadsResolveWithoutAmbiguity() = Unit

    @Composable
    private fun legacyShapesOverload(shapes: Shapes) {
        MaterialTheme(shapes = shapes) {}
    }

    @Composable
    private fun colorAndTypographyOverload(
        colorScheme: ColorScheme,
        typography: Typography,
        shapes: Shapes,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
        ) {}
    }
}
