## ADDED Requirements

### Requirement: SemanticsNode construction shall be null-safe
The system SHALL construct `SemanticsNode` instances only when both the `SemanticsModifierNode` head and `collapsedSemantics` configuration are non-null. If either is missing, the construction SHALL return null instead of throwing.

#### Scenario: Node removal during semantics tree traversal
- **WHEN** a `SemanticsModifierNode` is being removed from a `LayoutNode`'s modifier chain
- **AND** a concurrent or synchronous semantics tree traversal is triggered by `autoInvalidateRemovedNode()`
- **THEN** the `SemanticsNode` factory function SHALL return null rather than throwing NPE

#### Scenario: Head pointer drift after padChain
- **WHEN** `NodeChain.updateFrom()` calls `padChain()` and subsequently removes the old head node
- **AND** the `head` property still points to the removed old head
- **THEN** `headToTail` traversal SHALL not abort prematurely; the `head` property SHALL be synchronized to the next valid node in `removeNode()`

### Requirement: Semantics tree traversal shall use head() as source of truth
The system SHALL determine whether a `LayoutNode` has semantics capabilities by calling `head(Nodes.Semantics)` directly, rather than relying on `has(Nodes.Semantics)` which uses a cached `aggregateChildKindSet` bitmask.

#### Scenario: Aggregate bitmask out of sync with actual chain state
- **WHEN** `fillOneLayerOfSemanticsWrappers` evaluates a child `LayoutNode`
- **AND** `has(Nodes.Semantics)` returns true because the bitmask has not yet been updated
- **BUT** `head(Nodes.Semantics)` returns null because the actual modifier chain no longer contains a semantics node
- **THEN** the system SHALL skip adding the child to the semantics list and proceed to recursive traversal or skip

### Requirement: NodeChain head pointer shall remain consistent after removeNode
The system SHALL ensure that after `removeNode()` removes the node referenced by the `head` property, the `head` property is updated to point to the next valid node in the chain.

#### Scenario: Removing the current head node
- **WHEN** `removeNode()` is called with a node that is currently referenced by `head`
- **THEN** `head` SHALL be reassigned to the removed node's child, or to the tail node if no child exists
