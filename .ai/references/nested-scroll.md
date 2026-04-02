# 嵌套滚动（Nested Scroll）实现原理

> 以下场景读取本文件：使用 `nestedScroll` 属性后滚动行为不符合预期、父子 ScrollView 滚动冲突、`SELF_FIRST`/`PARENT_FIRST`/`SELF_ONLY` 优先级不生效、scroll lock/unlock 异常、三层嵌套滚动问题、`activeInnerScrollView` 或 `cascadeLock` 相关排查。

## 概述

KuiklyUI 的嵌套滚动机制允许多层 ScrollView 嵌套时，按照配置的优先级策略（谁先滚、谁后滚）协调滚动行为。

**核心设计**：每一对相邻的同方向 ScrollView 之间有一个 `NestedScrollCoordinator` 实例负责协调。Coordinator 通过 iOS 的 `UIScrollViewDelegate` 监听滚动事件，在 `scrollViewDidScroll:` 中动态 lock/unlock 对应的 ScrollView 来控制滚动优先级。

## 1. 属性传递链路（Kotlin → iOS）

### 1.1 Kotlin 侧定义

```kotlin
// core/src/commonMain/kotlin/com/tencent/kuikly/core/views/ScrollerView.kt

enum class KRNestedScrollMode(val value: String) {
    SELF_ONLY("SELF_ONLY"),    // 仅自身滚动，不传递给父容器
    SELF_FIRST("SELF_FIRST"),  // 自身优先滚动，到达边缘后传递给父容器
    PARENT_FIRST("PARENT_FIRST"), // 父容器优先滚动，到达边缘后传递给自身
}

// ScrollerAttr 中的 nestedScroll 方法
fun nestedScroll(forward: KRNestedScrollMode, backward: KRNestedScrollMode) {
    val param = JSONObject()
    param.put("forward", forward.value)   // 向前滚动（手指从下往上滑 = 内容向上滚）
    param.put("backward", backward.value) // 向后滚动（手指从上往下滑 = 内容向下滚）
    NESTED_SCROLL with param.toString()
}
```

**注意 forward/backward 的含义**：
- `forward`：内容前进方向（竖向列表 = 向上滚、横向列表 = 向左滚）
- `backward`：内容后退方向（竖向列表 = 向下滚、横向列表 = 向右滚）

### 1.2 iOS 侧解析

```objc
// core-render-ios/Extension/Components/KRScrollView.m

- (void)setCss_nestedScroll:(NSString *)css_nestedScroll {
    NSDictionary *dic = [css_nestedScroll kr_stringToDictionary];
    NSString *forwardStr = [dic objectForKey:@"forward"];
    NSString *backwardStr = [dic objectForKey:@"backward"];
    [self parseScrollMode:forwardStr forward:YES];
    [self parseScrollMode:backwardStr forward:NO];
}

- (void)parseScrollMode:(NSString *)modeStr forward:(BOOL)isForward {
    NestedScrollPriority pri = NestedScrollPriorityUndefined;
    if ([modeStr isEqualToString:@"SELF_ONLY"])    pri = NestedScrollPrioritySelfOnly;
    else if ([modeStr isEqualToString:@"SELF_FIRST"])  pri = NestedScrollPrioritySelf;
    else if ([modeStr isEqualToString:@"PARENT_FIRST"]) pri = NestedScrollPriorityParent;

    // 竖向列表：forward → TopPriority，backward → BottomPriority
    if (isForward && ![self horizontal])     [self setNestedScrollTopPriority:pri];
    else if (isForward && [self horizontal]) [self setNestedScrollLeftPriority:pri];
    else if (!isForward && ![self horizontal]) [self setNestedScrollBottomPriority:pri];
    else if (!isForward && [self horizontal])  [self setNestedScrollRightPriority:pri];
}
```

**方向映射关系**（以竖向列表为例）：

| Kotlin | 手指方向 | 页面方向 | iOS Priority 属性 |
|--------|----------|----------|-------------------|
| `forward` | 手指从下往上 ↑ | 内容向上滚 | `nestedScrollTopPriority` |
| `backward` | 手指从上往下 ↓ | 内容向下滚 | `nestedScrollBottomPriority` |

### 1.3 Priority 枚举

```objc
// core-render-ios/Extension/Components/NestScroll/NestedScrollProtocol.h

typedef NS_ENUM(char, NestedScrollPriority) {
    NestedScrollPriorityUndefined = 0,  // 未设置，取全局 nestedScrollPriority 或默认 Self
    NestedScrollPriorityNone,           // 不参与嵌套滚动
    NestedScrollPrioritySelfOnly,       // SELF_ONLY：仅自身，不传递
    NestedScrollPrioritySelf,           // SELF_FIRST：自身优先
    NestedScrollPriorityParent,         // PARENT_FIRST：父容器优先
};
```

### 1.4 优先级确定逻辑

`isDirection:hasPriority:` 方法决定当前方向使用哪个优先级：

```
1. 先看方向专属 priority（如 nestedScrollTopPriority）
2. 如果是 Undefined，再看全局 nestedScrollPriority
3. 如果都是 Undefined，默认使用 PrioritySelf（SELF_FIRST）
```

**注意方向反转**：属性名中的 top/bottom 指的是**手指方向**，而 `scrollViewDidScroll` 中的 direction 指的是**页面方向（contentOffset 变化方向）**，两者是相反的：
- 手指从下往上 → contentOffset.y 增大 → direction = Down → 使用 `nestedScrollTopPriority`（手指向上）
- 手指从上往下 → contentOffset.y 减小 → direction = Up → 使用 `nestedScrollBottomPriority`（手指向下）

## 2. NestedScrollCoordinator 核心机制

### 2.1 角色与创建

每个设置了 `nestedScroll` 属性的 KRScrollView 会创建一个 Coordinator：

```objc
// KRScrollView+NestedScroll.m

- (void)setupNestedScrollCoordinatorIfNeeded {
    if (!self.nestedScrollCoordinator) {
        self.nestedScrollCoordinator = [NestedScrollCoordinator new];
        self.nestedScrollCoordinator.innerScrollView = self;  // 自身作为 inner
        self.nestedGestureDelegate = self.nestedScrollCoordinator;
        [self addScrollViewDelegate:self.nestedScrollCoordinator];
    }
}
```

outer 的发现是延迟的（在首次手势识别时通过 `findNestedOuterScrollView:` 向上遍历 superview 查找同方向的 ScrollView）。

**Coordinator 拥有关系**：Coordinator 由 **inner ScrollView** 持有，inner 是设置了 `nestedScroll` 属性的那个 ScrollView。Coordinator 同时注册为 outer 的 scrollViewDelegate。

### 2.2 outer 发现机制

```objc
// NestedScrollCoordinator.mm

+ (id<ScrollableProtocol>)findNestedOuterScrollView:(UIScrollView *)innerScrollView {
    UIView *outerScrollView = innerScrollable.superview;
    while (outerScrollView) {
        if ([outerScrollView conformsToProtocol:@protocol(ScrollableProtocol)]) {
            BOOL isInnerHorizontal = [innerScrollable horizontal];
            BOOL isOuterHorizontal = [outerScrollable horizontal];
            if (isInnerHorizontal == isOuterHorizontal) {  // 必须同方向
                break;
            }
        }
        outerScrollView = outerScrollView.superview;
    }
    return outerScrollView;
}
```

关键规则：
- 沿 superview 链向上遍历
- 只匹配 **同方向**（都是竖向或都是横向）的 ScrollView
- 跳过不同方向的 ScrollView（如竖向 List 内嵌横向 List，不会配对）
- 找到的第一个就是 outer（最近的同方向祖先）

### 2.3 Coordinator 副作用

```objc
- (void)setInnerScrollView:(UIScrollView<NestedScrollProtocol> *)innerScrollView {
    _innerScrollView = innerScrollView;
    _innerScrollView.bounces = NO;  // ← 强制关闭 inner 的 bounces
}
```

**设置 inner 时会强制关闭 bounces**。这意味着参与嵌套滚动的子 ScrollView 默认不会有回弹效果。

## 3. 手势识别链路

### 3.1 gestureRecognizerShouldBegin

当用户触摸一个嵌套的 ScrollView 区域时，iOS 会自底向上逐层询问每个 ScrollView 的 `gestureRecognizerShouldBegin`。

KRScrollView 在此方法中实现了 **SelfOnly 拦截**：

```objc
// KRScrollView+NestedScroll.m

- (BOOL)gestureRecognizerShouldBegin:(UIGestureRecognizer *)gestureRecognizer {
    if (gestureRecognizer == self.panGestureRecognizer) {
        CGPoint location = [gestureRecognizer locationInView:self];
        UIView *hitView = [self hitTest:location withEvent:nil];
        // 向上遍历，收集触摸点到 self 之间的所有 KRScrollView
        NSMutableArray<KRScrollView *> *scrollViews = [NSMutableArray array];
        UIView *current = hitView;
        while (current && current != self) {
            if ([current isKindOfClass:[KRScrollView class]])
                [scrollViews addObject:(KRScrollView *)current];
            current = current.superview;
        }
        // 如果子 ScrollView 有 SelfOnly 优先级且方向匹配，则父不识别手势
        for (KRScrollView *scrollView in scrollViews) {
            if ([scrollView isSelfOnlyPriorityForPan:pan])
                return NO;
        }
    }
    return YES;
}
```

**重要限制**：iOS UIScrollView 的 panGestureRecognizer 有内部优化——当 `bounces=NO` 且 `contentOffset` 已经在对应滑动方向的边缘时，panGesture 不会尝试 begin。这意味着 `gestureRecognizerShouldBegin` **可能根本不会被调用**。

### 3.2 shouldRecognizeSimultaneouslyWithGestureRecognizer（同时识别）

这是嵌套滚动能工作的关键——允许多个 ScrollView 的 panGesture 同时识别：

```objc
// KRScrollView+NestedScroll.m

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer
    shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    if (self.nestedGestureDelegate &&
        gestureRecognizer == self.panGestureRecognizer) {
        return [self.nestedGestureDelegate
                shouldRecognizeScrollGestureSimultaneouslyWithView:otherGestureRecognizer.view];
    }
    return NO;
}
```

转发到 Coordinator：

```objc
// NestedScrollCoordinator.mm

- (BOOL)shouldRecognizeScrollGestureSimultaneouslyWithView:(UIView *)view {
    // 延迟初始化 outer
    if (!self.outerScrollView) {
        KRScrollView *scrollableView = [self.class findNestedOuterScrollView:self.innerScrollView];
        [scrollableView addScrollViewDelegate:self];
        self.outerScrollView = scrollableView;
    }

    if (view == self.outerScrollView) {
        // view 是自己的 outer → 允许同时识别
        self.outerScrollView.shouldHaveActiveInner = YES;
        return YES;
    } else if (self.outerScrollView.nestedGestureDelegate) {
        // view 不是自己的 outer → 链式转发给 outer 的 nestedGestureDelegate
        return [self.outerScrollView.nestedGestureDelegate
                shouldRecognizeScrollGestureSimultaneouslyWithView:view];
    }
    return NO;
}
```

**链式转发机制**：当 inner 的 coordinator 收到一个不是自己 outer 的 view 时，会把请求转发给 outer 的 coordinator。这样 3 层嵌套时，最内层可以间接和最外层建立 simultaneous 关系。

### 3.3 多层嵌套的 simultaneous 链路示例

假设 3 层嵌套：A（最内层）→ B（中间层）→ C（最外层）

```
A 的 panGesture 询问: 能否和 C 的 panGesture 同时识别？
  → A 的 coordinator: view(=C) != outer(=B) → 转发给 B.nestedGestureDelegate
    → B 的 coordinator: view(=C) == outer(=C) → return YES ✓

结果: A 和 C 直接建立 simultaneous，即使 B 没有参与手势
```

**这是已知的设计特点**：`shouldRecognizeSimultaneously` 的链式转发可以穿越中间层。

## 4. scrollViewDidScroll 核心逻辑

这是 Coordinator 最核心的方法，负责在每次滚动时决定 lock 谁。

### 4.1 事件过滤（Step 0）

```objc
// 0. 用 activeInnerScrollView 过滤无关事件
if (outerScrollView.activeInnerScrollView &&
    outerScrollView.activeInnerScrollView != innerScrollView) {
    return;  // 当前 coordinator 的 inner 不是活跃的那个 → 跳过
}
if (isOuter && !outerScrollView.activeInnerScrollView) {
    if (outerScrollView.shouldHaveActiveInner) {
        return;  // outer 应该有 activeInner 但还没有 → 跳过（等待设置）
    } else if (!isIntersect(outerScrollView, innerScrollView)) {
        return;  // 两个 ScrollView 不相交 → 跳过
    }
}
```

**activeInnerScrollView 的作用**：当 outer 有多个 inner 时（如 WaterfallList 同时包含多个子 List），用 `activeInnerScrollView` 标记当前正在响应的是哪一个，过滤掉其他 coordinator 的事件。

**activeInnerScrollView 的设置时机**：在 `scrollViewWillBeginDragging:` 中，当 inner 开始拖拽时异步设置：

```objc
dispatch_async(dispatch_get_main_queue(), ^{
    if (scrollView == self.innerScrollView) {
        self.outerScrollView.activeInnerScrollView = self.innerScrollView;
        self.innerScrollView.activeOuterScrollView = self.outerScrollView;
        self.dragType = NestedScrollDragTypeBoth;
    }
});
```

### 4.2 方向判定（Step 1）

```objc
// 通过 lastContentOffset 和当前 contentOffset 比较确定方向
if (lastContentOffset.y > sv.contentOffset.y)      direction = Up;    // 内容向上滚回
else if (lastContentOffset.y < sv.contentOffset.y)  direction = Down;  // 内容向下滚
```

**多层嵌套的 lastContentOffset 问题**：一个 ScrollView 可能同时被多个 Coordinator 监听。如果第一个 Coordinator 更新了 `lContentOffset`，第二个 Coordinator 就无法正确计算方向。解决方案是 `tempLastContentOffsetForMultiLayerNested`，在处理前记录原始值供下一个 Coordinator 使用。

### 4.3 PARENT_FIRST 模式（Step 2）

**核心思想**：锁定 inner，让 outer 先滚。outer 到达边缘后解锁 inner。

```
outer 未到边缘 → lock inner（inner 不动）
outer 到边缘 + inner 未到边缘 → unlock inner（inner 开始滚）
outer 到边缘 + inner 也到边缘 → lock inner（都不动了）
```

实际 lock 操作是将 inner 的 contentOffset 强制设回 lastContentOffset：

```objc
static inline void lockScrollView(scrollView, lastContentOffset) {
    scrollView.lContentOffset = lastContentOffset;
    scrollView.contentOffset = lastContentOffset;  // 强制回退
    scrollView.isLockedInNestedScroll = YES;
}
```

### 4.4 SELF_FIRST 模式（Step 3）

**核心思想**：锁定 outer，让 inner 先滚。inner 到达边缘后解锁 outer。

```
inner 未到边缘 → lock outer（outer 不动）
inner 到边缘 → unlock outer（outer 开始滚）
```

```objc
if (hasScrollToTheDirectionEdge(innerScrollView, direction)) {
    self.shouldUnlockOuterScrollView = YES;   // inner 到边缘 → 解锁 outer
} else if (outerScrollView.activeInnerScrollView) {
    self.shouldUnlockOuterScrollView = NO;    // inner 未到边缘 → 锁定 outer
}

// 执行 lock
if (dragType != OuterOnly && isOuter && !shouldUnlockOuterScrollView) {
    lockScrollView(outerScrollView, lastContentOffset);
}
```

**SelfOnly 特殊处理**：即使 inner 到了边缘，如果是 SelfOnly 模式且不是 OuterOnly 拖拽，也强制锁定 outer。

### 4.5 cascadeLock（3 层嵌套支持）

当存在 ≥3 层嵌套时，需要将 lock 行为级联传递：

**PARENT_FIRST 场景**：如果 outer 锁定了 inner，但 inner 自己又有一个更深层的 activeInner，那么锁定行为需要传递到更深层：

```objc
if (!shouldUnlockInnerScrollView && isOuter && innerScrollView.activeInnerScrollView) {
    innerScrollView.cascadeLockForNestedScroll = YES;
    innerScrollView.activeInnerScrollView.cascadeLockForNestedScroll = YES;
}
```

**SELF_FIRST 场景**：如果 inner 锁定了 outer，但 outer 自己又有一个更外层的 activeOuter，那么锁定行为需要传递到更外层：

```objc
if (isInner && !shouldUnlockOuterScrollView && outerScrollView.activeOuterScrollView) {
    outerScrollView.cascadeLockForNestedScroll = YES;
    outerScrollView.activeOuterScrollView.cascadeLockForNestedScroll = YES;
}
```

## 5. DragType 状态机

```
NestedScrollDragTypeUndefined = 0   // 初始状态 / 拖拽结束后
NestedScrollDragTypeOuterOnly = 1   // 只有 outer 在拖拽（手指在 outer 的非 inner 区域）
NestedScrollDragTypeBoth = 2        // inner 和 outer 同时在拖拽（手指在 inner 区域）
```

**设置时机**（在 `scrollViewWillBeginDragging:` 的异步 block 中）：
- inner 开始拖拽 → `Both`
- outer 开始拖拽且当前是 Undefined → `OuterOnly`

**重置时机**：`scrollViewDidEndDragging:` 中重置为 `Undefined`。

**对 lock 逻辑的影响**：
- `OuterOnly` 时不会锁定 outer（因为这时候只有 outer 在滚，inner 不参与）
- fling 传递也受 `dragType` 影响

## 6. NestedScrollProtocol 属性详解

```objc
@protocol NestedScrollProtocol <NSObject>

@property CGPoint lContentOffset;
// 记录上一次的 contentOffset，用于 lock（回退 offset）和方向判定。

@property BOOL shouldHaveActiveInner;
// 在 shouldRecognizeSimultaneously 中设为 YES，表示 outer 期望有一个 activeInner。
// 在 scrollViewDidEndDragging 中重置为 NO。

@property (weak) UIScrollView<NestedScrollProtocol> *activeInnerScrollView;
// 当前正在拖拽的 inner ScrollView。

@property (weak) UIScrollView<NestedScrollProtocol> *activeOuterScrollView;
// 当前正在拖拽的 outer ScrollView。用于 cascadeLock 向外层传递。

@property (weak) id<NestedScrollGestureDelegate> nestedGestureDelegate;
// 指向自己的 NestedScrollCoordinator，用于链式转发 shouldRecognizeSimultaneously。

@property BOOL cascadeLockForNestedScroll;
// 级联锁标志。3 层嵌套中某层需要被锁定但不是直接被当前 Coordinator 管理时使用。

@property BOOL isLockedInNestedScroll;
// 当前帧是否被 lock 了。用于决定是否屏蔽 onScroll 事件的发送。

@property NSValue *tempLastContentOffsetForMultiLayerNested;
// 多层嵌套中记录原始 lastContentOffset，解决多个 Coordinator 共享同一 ScrollView
// 时 lContentOffset 被提前更新的问题。使用一次后置 nil。

@end
```

## 7. flingEnable 的实现

```objc
// KRScrollView.m - scrollViewWillEndDragging:withVelocity:targetContentOffset:

if (_css_flingEnable && ![_css_flingEnable boolValue] && targetContentOffset) {
    *targetContentOffset = scrollView.contentOffset;  // 将目标位置设为当前位置 → 无惯性
}
```

## 8. 完整生命周期时序

以 2 层嵌套 inner + outer 为例，用户手指在 inner 上向上滑动：

```
1. [iOS] 触摸开始，hit test 找到 inner

2. [iOS] inner.gestureRecognizerShouldBegin → YES
   [iOS] outer.gestureRecognizerShouldBegin → YES

3. [iOS] inner.shouldRecognizeSimultaneously(outer) → YES
   → Coordinator 设置 outer.shouldHaveActiveInner = YES

4. [iOS] inner.panGesture + outer.panGesture 同时开始

5. outer.scrollViewWillBeginDragging
   → Coordinator: shouldUnlockOuterScrollView = NO（重置）

6. inner.scrollViewWillBeginDragging
   → Coordinator: shouldUnlockInnerScrollView = NO（重置）
   → dispatch_async: activeInnerScrollView = inner, dragType = Both

7. [每帧] inner.scrollViewDidScroll + outer.scrollViewDidScroll
   → Coordinator 根据 priority 执行 lock/unlock

8. inner.scrollViewDidEndDragging
   → dragType = Undefined
   → shouldHaveActiveInner = NO

9. [如果 fling] inner/outer 继续惯性滚动，Coordinator 继续工作

10. 滚动停止
```

## 9. Android 嵌套滚动对比

Android 使用完全不同的机制——基于 `NestedScrollingChild2` / `NestedScrollingParent2` 接口：

### 9.1 核心流程

```
子 RecyclerView 开始滚动
  → dispatchNestedPreScroll（给父处理机会）
    → 父 onNestedPreScroll
      → scrollParentIfNeeded（根据 mode 决定是否消耗）
  → 子自身消耗剩余距离
  → dispatchNestedScroll（未消耗的给父）
    → 父 onNestedScroll
```

### 9.2 scrollParentIfNeeded 判定逻辑

```kotlin
// KRRecyclerView.kt

val shouldScrollParentY = when {
    // PARENT_FIRST：forward 方向 → 父先滚
    parentDy > 0 && target.scrollForwardMode == PARENT_FIRST -> true
    parentDy < 0 && target.scrollBackwardMode == PARENT_FIRST -> true
    // SELF_FIRST：子到边缘后 → 父接手
    parentDy > 0 && target.scrollForwardMode == SELF_FIRST
        && !target.canScrollVertically(parentDy) -> true
    parentDy < 0 && target.scrollBackwardMode == SELF_FIRST
        && !target.canScrollVertically(parentDy) -> true
    else -> false
}
```

### 9.3 与 iOS 的关键差异

| 方面 | iOS | Android |
|------|-----|---------|
| 协调方式 | 通过 delegate 监听 + lock（强制回退 offset） | 通过 NestedScrolling 接口分发距离 |
| 手势处理 | simultaneous gesture + lock/unlock | RecyclerView 内建 nested scroll |
| 多层嵌套 | cascadeLock 传递，依赖 activeInnerScrollView | `dispatchNestedPreScroll` 天然支持链式传递 |
| bounces | Coordinator 强制关闭 inner.bounces | 通过 OverScrollHandler 控制 |
| fling 传递 | 由 UIScrollView 系统行为决定 | 通过 `smoothScrollWithNestIfNeeded` 显式处理 |

## 10. 已知限制与注意事项

### 10.1 Coordinator.setInnerScrollView 强制关闭 bounces

即使业务代码设置了 `bouncesEnable(true)`，如果该 ScrollView 作为嵌套滚动的 inner，bounces 仍会被强制关闭。需要注意属性设置顺序。

### 10.2 activeInnerScrollView 的异步设置

`activeInnerScrollView` 在 `dispatch_async(main_queue)` 中设置，这意味着在 `scrollViewWillBeginDragging` 返回后的同一 runloop 周期内，`activeInnerScrollView` 仍为 nil。`scrollViewDidScroll` 中的过滤逻辑通过 `shouldHaveActiveInner` 来桥接这个时间差。

## 11. 关键源文件索引

| 文件 | 职责 |
|------|------|
| `core/src/commonMain/.../ScrollerView.kt` | KRNestedScrollMode 定义、ScrollerAttr.nestedScroll() |
| `core-render-ios/.../NestScroll/NestedScrollProtocol.h` | NestedScrollPriority 枚举、NestedScrollProtocol 属性 |
| `core-render-ios/.../NestScroll/NestedScrollCoordinator.h/.mm` | 核心协调器：lock/unlock、事件过滤、cascadeLock |
| `core-render-ios/.../NestScroll/KRScrollView+NestedScroll.h/.m` | 手势识别：simultaneous、shouldBegin、SelfOnly 拦截 |
| `core-render-ios/.../NestScroll/ScrollableProtocol.h` | ScrollableProtocol：horizontal、priority setter |
| `core-render-ios/.../KRScrollView.h/.m` | 属性解析：setCss_nestedScroll、parseScrollMode、flingEnable |
| `core-render-android/.../list/KRRecyclerView.kt` | Android 嵌套滚动：scrollParentIfNeeded、NestedScrollingChild2/Parent2 |
| `compose/.../extension/ModifierNestScroll.kt` | Compose DSL 的 nestedScroll Modifier |
