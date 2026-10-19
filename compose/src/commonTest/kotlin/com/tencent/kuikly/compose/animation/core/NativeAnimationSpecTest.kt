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

package com.tencent.kuikly.compose.animation.core

import com.tencent.kuikly.core.base.Animation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NativeAnimationSpecTest {
    @Test
    fun legacyCoreDescriptorIsUnchanged() {
        assertEquals(
            "0 0 0.2 0.0 0.0 0.0 0 legacy",
            Animation.linear(0.2f, "legacy").toString()
        )
    }

    @Test
    fun preferNativeKeepsVectorizedBehavior() {
        val original = tween<Float>(
            durationMillis = 400,
            delayMillis = 20,
            easing = CubicBezierEasing(0.1f, 0.2f, 0.3f, 0.4f)
        )
        val wrapped = original.preferNative()
        val originalVectorized = original.vectorize(Float.VectorConverter)
        val wrappedVectorized = wrapped.vectorize(Float.VectorConverter)

        assertEquals(
            originalVectorized.getValueFromNanos(
                170_000_000,
                AnimationVector1D(0f),
                AnimationVector1D(1f),
                AnimationVector1D(0f)
            ),
            wrappedVectorized.getValueFromNanos(
                170_000_000,
                AnimationVector1D(0f),
                AnimationVector1D(1f),
                AnimationVector1D(0f)
            )
        )
    }

    @Test
    fun preferNativeIsIdempotent() {
        val wrapped = tween<Float>().preferNative()
        assertSame(wrapped, wrapped.preferNative())
    }

    @Test
    fun cubicProducesVersionedDescriptor() {
        val animation = tween<Float>(
            durationMillis = 250,
            delayMillis = 10,
            easing = CubicBezierEasing(0.11f, 0.22f, 0.33f, 0.44f)
        ).preferNative().toNativeAnimationOrNull(0f, 1f, 0f, Float.VectorConverter)

        assertNotNull(animation)
        assertTrue(animation.toString().endsWith("v2,cubic,0.11,0.22,0.33,0.44"))
    }

    @Test
    fun springFallsBackAndSnapProducesVersionedDescriptor() {
        val spring = spring<Float>(
            dampingRatio = 0.7f,
            stiffness = 420f
        ).preferNative().toNativeAnimationOrNull(0f, 1f, 0f, Float.VectorConverter)
        val snap = snap<Float>(delayMillis = 17)
            .preferNative().toNativeAnimationOrNull(0f, 1f, 0f, Float.VectorConverter)

        assertNull(spring)
        assertNotNull(snap)
        assertTrue(snap.toString().endsWith("v2,snap"))
    }

    @Test
    fun unsupportedSpecsAndNonZeroVelocityFallBack() {
        val customEasing = Easing { it * it }
        assertNull(
            tween<Float>(easing = customEasing).preferNative()
                .toNativeAnimationOrNull(0f, 1f, 0f, Float.VectorConverter)
        )
        assertNull(
            spring<Float>().preferNative()
                .toNativeAnimationOrNull(0f, 1f, 2f, Float.VectorConverter)
        )
        assertNull(
            keyframes<Float> { durationMillis = 100 }.preferNative()
                .toNativeAnimationOrNull(0f, 1f, 0f, Float.VectorConverter)
        )
    }
}
