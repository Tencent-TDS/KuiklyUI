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

package com.tencent.kuikly.core.base

class Animation internal constructor() {
    private lateinit var timingFuncType: TimingFuncType
    private var duration: Float = 0f
    private var animationType = AnimationType.PLAIN
    private var damping: Float = 0f
    private var velocity: Float = 0f
    private var delay: Float = 0f
    private var repeatForever: Boolean = false

    var key: String = ""

    fun delay(time: Float): Animation {
        delay = time
        return this
    }

    fun repeatForever(forever: Boolean): Animation {
        repeatForever = forever
        return this
    }

    override fun toString(): String {
        return "${animationType.value} ${timingFuncType.value} $duration $damping $velocity $delay ${repeatForever.toInt()} $key"
    }

    companion object {

        /*
         * @param durationS 动画持续时间，单位秒
         * @param key 业务方设置，动画结束回调会回传，用于区分是哪个动画结束
         */
        fun linear(durationS: Float, key: String = ""): Animation {
            return create(AnimationType.PLAIN, TimingFuncType.LINEAR, durationS, key = key)
        }

        /*
         * @param durationS 动画持续时间，单位秒
         * @param key 业务方设置，动画结束回调会回传，用于区分是哪个动画结束
         */
        fun easeIn(durationS: Float, key: String = ""): Animation {
            return create(AnimationType.PLAIN, TimingFuncType.EASE_IN, durationS, key = key)
        }

        /*
         * @param durationS 动画持续时间，单位秒
         * @param key 业务方设置，动画结束回调会回传，用于区分是哪个动画结束
         */
        fun easeOut(durationS: Float, key: String = ""): Animation {
            return create(AnimationType.PLAIN, TimingFuncType.EASE_OUT, durationS, key = key)
        }

        /*
         * @param durationS 动画持续时间，单位秒
         * @param key 业务方设置，动画结束回调会回传，用于区分是哪个动画结束
         */
        fun easeInOut(durationS: Float, key: String = ""): Animation {
            return create(AnimationType.PLAIN, TimingFuncType.EASE_IN_OUT, durationS, key = key)
        }

        fun springLinear(
            durationS: Float,
            damping: Float,
            velocity: Float,
            key: String = ""
        ): Animation {
            return createSpring(TimingFuncType.LINEAR, durationS, damping, velocity, key = key)
        }

        fun springEaseIn(
            durationS: Float,
            damping: Float,
            velocity: Float,
            key: String = ""
        ): Animation {
            return createSpring(TimingFuncType.EASE_IN, durationS, damping, velocity, key = key)
        }

        fun springEaseOut(
            durationS: Float,
            damping: Float,
            velocity: Float,
            key: String = ""
        ): Animation {
            return createSpring(TimingFuncType.EASE_OUT, durationS, damping, velocity, key = key)
        }

        fun springEaseInOut(
            durationS: Float,
            damping: Float,
            velocity: Float,
            key: String = ""
        ): Animation {
            return createSpring(TimingFuncType.EASE_IN_OUT, durationS, damping, velocity, key = key)
        }

        private fun create(
            animationType: AnimationType,
            timingFuncType: TimingFuncType,
            durationS: Float,
            delay: Float = 0f,
            repeatForever: Boolean = false,
            key: String = ""
        ): Animation {
            val animation = Animation()
            animation.animationType = animationType
            animation.timingFuncType = timingFuncType
            animation.duration = durationS
            animation.delay = delay
            animation.repeatForever = repeatForever
            animation.key = key
            return animation
        }

        private fun createSpring(
            timingFuncType: TimingFuncType,
            durationS: Float,
            damping: Float,
            velocity: Float,
            delay: Float = 0f,
            repeatForever: Boolean = false,
            key: String = ""
        ): Animation {
            val animation = Animation()
            animation.animationType = AnimationType.SPRING
            animation.timingFuncType = timingFuncType
            animation.duration = durationS
            animation.damping = damping
            animation.velocity = velocity
            animation.delay = delay
            animation.repeatForever = repeatForever
            animation.key = key
            return animation
        }
    }
}

internal enum class TimingFuncType(internal val value: Int) {
    LINEAR(0),
    EASE_IN(1),
    EASE_OUT(2),
    EASE_IN_OUT(3);
}

internal enum class AnimationType(internal val value: Int) {
    PLAIN(0),
    SPRING(1);
}
