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

package com.tencent.kuikly.compose.profiler

/**
 * Compose compiler $dirty bitmask parameter stability flags.
 *
 * The compiler passes $dirty values to traceEventStart(key, dirty1, dirty2, info).
 * Each parameter occupies **3 bits** encoding its runtime comparison result.
 *
 * Encoding (from Compose compiler `ParamState`):
 * - 001 (1) = Same — parameter value unchanged since last composition
 * - 010 (2) = Different — parameter value has changed
 * - 011 (3) = Static — compile-time constant, never changes
 * - 100 (4) = Unknown — cannot determine
 * - 000 (0) = Unused slot (padding)
 */
internal enum class ParamStability {
    /** 000 — Unused / padding slot */
    UNUSED,
    /** 001 — Parameter value is the same as last composition */
    SAME,
    /** 010 — Parameter value has changed */
    DIFFERENT,
    /** 011 — Compile-time static constant, never changes */
    STATIC,
    /** 100 — Cannot determine if changed */
    UNKNOWN
}

/**
 * Per-parameter change information.
 */
internal data class ParamChangeInfo(
    /** Parameter position in the function signature (0-based) */
    val index: Int,
    /** Stability state of this parameter */
    val stability: ParamStability
)

/**
 * Summary of parameter changes for a single Composable invocation.
 */
data class ParamChangeSummary(
    /** Total number of parameters */
    val totalParams: Int,
    /** Indices of parameters marked as DIFFERENT */
    val changedParams: List<Int>,
    /** Indices of parameters marked as UNKNOWN */
    val unknownParams: List<Int>
) {
    /** Whether any parameter changed */
    val hasChanges: Boolean get() = changedParams.isNotEmpty()

    /** Whether there are unknown parameters */
    val hasUnknown: Boolean get() = unknownParams.isNotEmpty()
}

/**
 * Parser for the Compose compiler's $dirty bitmask.
 *
 * Bitmask layout per Int (31 usable bits):
 * - Bit 0: reserved (always 0)
 * - Bits [3:1]: slot 0 (parameter #0)
 * - Bits [6:4]: slot 1 (parameter #1)
 * - ...
 * - Bits [30:28]: slot 9 (parameter #9)
 *
 * Each slot is 3 bits. Each Int holds up to 10 parameter slots.
 * dirty2 is used for parameter #10 onward (only when >10 parameters).
 * dirty2 == -1 means no second dirty variable (≤10 parameters).
 */
internal object DirtyFlagsParser {

    private const val BITS_PER_SLOT = 3
    private const val SLOTS_PER_INT = 10
    private const val SLOT_MASK = 0x7 // 0b111

    /**
     * Parse dirty1 and dirty2 bitmask into a parameter change summary.
     *
     * @param dirty1 First dirty bitmask (parameters #0 ~ #9)
     * @param dirty2 Second dirty bitmask (parameters #10+), -1 when no extra parameters
     * @return ParamChangeSummary, or null if dirty1 has no valid slot data
     */
    fun parse(dirty1: Int, dirty2: Int): ParamChangeSummary? {
        // dirty1 == 0: no parameters; dirty1 == -1: legacy API sentinel (no dirty info)
        if (dirty1 == 0 || dirty1 == -1) return null

        val allParams = mutableListOf<ParamChangeInfo>()

        // Parse dirty1: all 10 slots are parameter slots (slot 0 = param #0)
        parseSlotsFromInt(dirty1, allParams)

        // Parse dirty2: only if it's a real dirty variable (not -1 sentinel)
        if (dirty2 != -1 && dirty2 != 0) {
            parseSlotsFromInt(dirty2, allParams)
        }

        // Trim trailing UNUSED(000) slots — these are padding
        val trimmedParams = trimTrailingUnused(allParams)

        if (trimmedParams.isEmpty()) return null

        val changed = trimmedParams.filter { it.stability == ParamStability.DIFFERENT }.map { it.index }
        val unknown = trimmedParams.filter { it.stability == ParamStability.UNKNOWN }.map { it.index }

        return ParamChangeSummary(
            totalParams = trimmedParams.size,
            changedParams = changed,
            unknownParams = unknown
        )
    }

    private fun parseSlotsFromInt(
        dirty: Int,
        output: MutableList<ParamChangeInfo>
    ) {
        for (slot in 0 until SLOTS_PER_INT) {
            // Bit layout: bit0 is reserved, slot N starts at bit (N * 3 + 1)
            val shift = slot * BITS_PER_SLOT + 1
            if (shift + BITS_PER_SLOT > 32) break
            val bits = (dirty shr shift) and SLOT_MASK
            val stability = when (bits) {
                0 -> ParamStability.UNUSED
                1 -> ParamStability.SAME
                2 -> ParamStability.DIFFERENT
                3 -> ParamStability.STATIC
                4 -> ParamStability.UNKNOWN
                else -> ParamStability.UNUSED
            }
            output.add(
                ParamChangeInfo(
                    index = output.size,
                    stability = stability
                )
            )
        }
    }

    /**
     * Trim trailing consecutive UNUSED(000) slots — these are padding bits
     * that don't represent real parameters.
     */
    private fun trimTrailingUnused(
        params: List<ParamChangeInfo>
    ): List<ParamChangeInfo> {
        val lastUsed = params.indexOfLast {
            it.stability != ParamStability.UNUSED
        }
        return if (lastUsed >= 0) {
            params.subList(0, lastUsed + 1).mapIndexed { i, p ->
                p.copy(index = i)
            }
        } else {
            emptyList()
        }
    }
}
