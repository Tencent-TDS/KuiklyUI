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

package com.tencent.kuikly.compose.foundation.relocation

import androidx.compose.runtime.Stable
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.layout.LayoutCoordinates
import com.tencent.kuikly.compose.ui.node.GlobalPositionAwareModifierNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.findNearestAncestor
import com.tencent.kuikly.compose.ui.platform.InspectorInfo

@Stable
class BringIntoViewRequester {
    internal var requesterNode: BringIntoViewRequesterNode? = null

    /**
     * Brings the entire node associated with this requester into view.
     * If the node is already fully visible, this is a no-op.
     */
    suspend fun bringIntoView() {
        requesterNode?.bringIntoView(null)
    }

    /**
     * Brings the specified [rect] (in local coordinates of the requester node) into view.
     * Use this for cursor/selection-level precision, e.g. bringing the cursor line into view
     * during text input. Aligned with official BringIntoViewRequester.bringIntoView(rect: Rect?).
     *
     * @param rect The rectangle in local coordinates that should be brought into view.
     *             Pass null to use the entire node bounds (same as [bringIntoView]).
     */
    suspend fun bringIntoView(rect: Rect?) {
        requesterNode?.bringIntoView(rect)
    }
}

fun Modifier.bringIntoViewRequester(requester: BringIntoViewRequester): Modifier =
    this.then(BringIntoViewRequesterElement(requester))

private data class BringIntoViewRequesterElement(
    val requester: BringIntoViewRequester,
) : ModifierNodeElement<BringIntoViewRequesterNode>() {
    override fun create(): BringIntoViewRequesterNode = BringIntoViewRequesterNode(requester)

    override fun update(node: BringIntoViewRequesterNode) {
        node.updateRequester(requester)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "bringIntoViewRequester"
        properties["requester"] = requester
    }
}

internal class BringIntoViewRequesterNode(
    private var requester: BringIntoViewRequester,
) : Modifier.Node(), GlobalPositionAwareModifierNode {
    private var coordinates: LayoutCoordinates? = null

    override fun onAttach() {
        requester.requesterNode = this
    }

    override fun onDetach() {
        if (requester.requesterNode === this) {
            requester.requesterNode = null
        }
        coordinates = null
    }

    fun updateRequester(requester: BringIntoViewRequester) {
        if (this.requester === requester) {
            return
        }
        if (this.requester.requesterNode === this) {
            this.requester.requesterNode = null
        }
        this.requester = requester
        if (isAttached) {
            this.requester.requesterNode = this
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    suspend fun bringIntoView(rect: Rect? = null) {
        val coordinates = coordinates?.takeIf { it.isAttached } ?: return
        val responder = findNearestAncestor(BringIntoViewResponderNode.TraverseKey) as? BringIntoViewResponder
            ?: return
        responder.bringChildIntoView(coordinates, rect)
    }
}
