package com.tencent.kuikly.core.layout

import com.tencent.kuikly.core.layout.*
import com.tencent.kuikly.core.log.KLog
import kotlin.math.max

object LayoutImpl {

    /**
     * 测量节点
     * @param node 待测量的节点
     * @param parentMaxWidth 父节点宽度约束
     * @param layoutContext 布局上下文
     * @param parentDirection 布局方向
     * @param needLayoutAbsoluteChild 是否需要进行绝对布局的二次测量；为 true 时不一定会触发绝对布局的二次测量，
     *                                内部会按需检测到需要二次测量才测量
     * @param dirtyList 脏节点列表
     */
    fun layoutNode(
        node: FlexNode,
        parentMaxWidth: Float,
        layoutContext: FlexLayoutContext? = null,
        parentDirection: FlexLayoutDirection? = null,
        needLayoutAbsoluteChild: Boolean = true,
        dirtyList: MutableSet<FlexNode>
    ) {
        if (needsRelayout(node, parentMaxWidth)) {
            //先更新上一次的计算 layout值
            node.lastLayoutWidth = node.layoutWidth
            node.lastLayoutHeight = node.layoutHeight
            node.lastParentMaxWith = parentMaxWidth

            //进行新的layout 计算
            layoutNodeImpl(node, parentMaxWidth, layoutContext, parentDirection, dirtyList, needLayoutAbsoluteChild)
            dirtyList.add(node)
        } else {
            node.updateLayoutUsingLast()
            node.markNotDirty()
        }
    }

    private fun layoutNodeImpl(
        node: FlexNode,
        parentMaxWidth: Float,
        layoutContext: FlexLayoutContext? = null,
        parentDirection: FlexLayoutDirection? = null,
        dirtyList: MutableSet<FlexNode>,
        needLayoutAbsoluteChild: Boolean
    ) {
        //先把下一层的子节点的flexLayout reset
        for (index in node.childCount - 1 downTo 0) {
            node.getChildAt(index)?.resetLayout()
        }
        //不可见元素不进行布局计算
        if (!node.isShow) {
            return
        }

        //排版方向
        val direction = node.resolveDirection(parentDirection)
        //主轴方向
        val mainAxis = resolveAxis(node.flexDirection, direction)
        //交叉轴方向
        val crossAxis = getCrossFlexDirection(mainAxis, direction)
        //横轴方向（无论ROW/COLUMN,LTR/RTL 都是一行一行布局）
        val resolvedRowAxis = resolveAxis(FlexDirection.ROW, direction)

        // 预处理，先把Style 指定的宽高，当作layout的结果。
        node.setDimensionFromStyle(mainAxis)
        node.setDimensionFromStyle(crossAxis)
        node.flexLayoutDirection = direction

        //处理Style 中的margin  对position的影响，margin 不算入 盒子模型的 content
        node.setLayoutLeadingPosition(mainAxis,
            node.getLayoutLeadingPosition(mainAxis) + node.getStyleMarginLeadingSpacing(mainAxis) + node.getRelativePosition(
                mainAxis))
        node.setLayoutTrailingPosition(mainAxis,
            node.getLayoutTrailingPosition(mainAxis) + node.getStyleMarginTrailingSpacing(mainAxis) + node.getRelativePosition(
                mainAxis))
        node.setLayoutLeadingPosition(crossAxis,
            node.getLayoutLeadingPosition(crossAxis) + node.getStyleMarginLeadingSpacing(crossAxis) + node.getRelativePosition(
                crossAxis))
        node.setLayoutTrailingPosition(crossAxis,
            node.getLayoutTrailingPosition(crossAxis) + node.getStyleMarginTrailingSpacing(crossAxis) + node.getRelativePosition(
                crossAxis))

        // Inline immutable values from the target node to avoid excessive method
        // invocations during the csslayout calculation.
        //行内元素 减轻执行
        val childCount: Int = node.childCount
        //行排列的时候的padding 左右和border 左右的 sum
        val paddingAndBorderAxisResolvedRow = node.paddingAndBorderDimension(resolvedRowAxis)

        /**
         * step1 处理行内元素 / 有 measure的节点
         */
        //如果有注入的 测量函数的情况（如TextView这类 inline行内元素，需要有测量函数）
        if (node.isMeasureDefined() && node.performMeasureFunction(parentMaxWidth,
                paddingAndBorderAxisResolvedRow,
                resolvedRowAxis,
                layoutContext)
        ) {
            return
        }

        val isNodeFlexWrap = node.flexWrap == FlexWrap.WRAP
        val justifyContent = node.justifyContent

        // 分别计算出 主轴&交叉轴的 leadinPading+Border
        val leadingPaddingAndBorderMain =
            node.getStylePaddingLeadingSpacing(mainAxis) + node.getStyleBorderLeadingSpacing(
                mainAxis)
        val leadingPaddingAndBorderCross =
            node.getStylePaddingLeadingSpacing(crossAxis) + node.getStyleBorderLeadingSpacing(
                crossAxis)
        val paddingAndBorderAxisMain = node.paddingAndBorderDimension(mainAxis)
        val paddingAndBorderAxisCross = node.paddingAndBorderDimension(crossAxis)

        //依然先看视图在主轴/交叉轴的大小
        val isMainDimDefined = node.isLayoutDimenDefined(mainAxis)
        val isCrossDimDefined = node.isLayoutDimenDefined(crossAxis)
        val isMainRowDirection =
            mainAxis == FlexDirection.ROW || mainAxis == FlexDirection.ROW_REVERSE

        var i = 0
        var ii = 0
        var child: FlexNode?
        var axis: FlexDirection

        //维护 绝对定位的 子节点的链表
        var firstAbsoluteChild: FlexNode? = null
        var currentAbsoluteChild: FlexNode? = null

        var firstCrossStretchChild: FlexNode? = null
        var currentCrossStretchChild: FlexNode? = null

        var definedMainDim = Float.undefined
        if (isMainDimDefined) {
            definedMainDim = node.getLayoutDimension(mainAxis) - paddingAndBorderAxisMain
        }

        var startLine = 0
        var endLine = 0
        // int nextOffset = 0;
        var alreadyComputedNextLayout = 0
        var linesCrossDim = 0f
        var linesMainDim = 0f
        var linesCount = 0
        while (endLine < childCount) {
            /**
             * Loop A 找出不是flexible的 children, 并且分类计算出不同类型的children的个数
             * */
            //累计主轴上所有的non flexible children 的demesions ,
            //目的：1.是否没有剩下的空间可以提供给节点了，2.剩下的空间 用于分配给 flexible children
            /**
            循环A主要是实现的是layout布局中不可以flex的子视图的布局，
            mainContentDim变量是用来记录所有的尺寸以及所有不能flex的子视图的margin的总和。
            它被用来设置node节点的尺寸，和计算剩余空间以便供可flex子视图进行拉伸适配。
             */
            var mainContentDim = 0f

            //分为三类children: flexible,noflexible,absolute
            var flexibleChildrenCount = 0
            var totalFlexible = 0f //子节点 flex的总和，用于计算每份flex

            var nonFlexibleChildrenCount = 0

            //是否可简单在主轴堆上去：（主轴上 子元素有大小 & FLEX_START）|| 主轴上 子元素无大小 && FlexJustifyContent.CENTER；
            //利用一层循环在主轴上简单的堆叠子视图，在循环C中，会忽略这些已经在循环A中已经排列好的子视图
            var isSimpleStackMain =
                (isMainDimDefined && justifyContent == FlexJustifyContent.FLEX_START) ||
                        (!isMainDimDefined && justifyContent != FlexJustifyContent.CENTER)
            var firstComplexMain = if (isSimpleStackMain) childCount else startLine

            // 利用一层循环在侧轴上简单的堆叠子视图，在循环D中，会忽略这些已经在循环A中已经排列好的子视图
            var isSimpleStackCross = true
            var firstComplexCross = childCount

            var firstFlexChild: FlexNode? = null
            var currentFlexChild: FlexNode? = null

            var mainDim = leadingPaddingAndBorderMain
            var crossDim = 0f

            var maxWidth: Float
            // 循环A从这里开始
            for (index in startLine until childCount) {
                i = index
                child = node.getChildAt(i)
                if (child != null) {
                    if (!child.isShow) {
                        endLine = i + 1
                        continue
                    }
                } else {
                    endLine = i + 1
                    continue
                }
                child.lineIndex = linesCount
                child.nextAbsoluteChild = null
                child.nextFlexChild = null

                val alignItem = node.getAlignItem(child)
                // 在递归layout之前，先预填充侧轴上可以被拉伸的子视图
                if (alignItem == FlexAlign.STRETCH && child.isFlexRelative() && isCrossDimDefined && !child.isStyleLayoutDimenDefined(
                        crossAxis)
                ) {
                    // 这里要进行一个比较，比较子视图在侧轴上的尺寸 和 侧轴上减去两边的Margin、padding、Border剩下的可拉伸的空间 进行比较，因为拉伸是不会压缩原始的大小的。
                    val childBoundAxis = child.boundAxis(crossAxis,
                        node.getLayoutDimension(crossAxis) - paddingAndBorderAxisCross - child.getStyleMargin(
                            crossAxis))
                    val layoutDimen =
                        max(childBoundAxis, child.paddingAndBorderDimension(crossAxis))
                    child.setLayoutDimension(crossAxis, layoutDimen)
                } else if (child.isFlexAbsolute()) {
                    // 这里会储存一个绝对布局子视图的链表。这样我们在后面布局的时候可以快速的跳过它们。
                    if (firstAbsoluteChild == null) {
                        firstAbsoluteChild = child
                    }
                    if (currentAbsoluteChild != null) {
                        currentAbsoluteChild.nextAbsoluteChild = child
                    }
                    currentAbsoluteChild = child
                    // 预填充子视图，这里需要用到视图在轴上面的绝对坐标，如果是水平轴，需要用到左右的偏移量，如果是竖直轴，需要用到上下的偏移量。
                    //TODO: kamlin fixme
                    for (inx in 0 until 2) {
                        ii = inx
                        axis = if (ii != 0) {
                            FlexDirection.ROW
                        } else {
                            FlexDirection.COLUMN
                        }
                        if (node.isLayoutDimenDefined(axis) && !child.isStyleLayoutDimenDefined(axis)
                            && child.isStyleLeadingPositionDefined(axis)
                            && child.isStyleTrailingPositionDefined(axis)
                        ) {
                            // 这里是绝对布局，还需要减去leading和trailing
                            val childBoundAxis = child.boundAxis(axis,
                                node.getLayoutDimension(axis) - node.paddingAndBorderDimension(axis) -
                                        child.getStyleMargin(axis) - child.getStyleLeadingPosition(
                                    axis,
                                    0f) - child.getStyleTrailingPosition(axis, 0f))

                            child.setLayoutDimension(axis,
                                max(childBoundAxis, child.paddingAndBorderDimension(axis)))
                        }
                    }
                }

                var nextContentDim = 0f

                // 统计可以拉伸flex的子视图
                if (isMainDimDefined && child.isFlex()) {
                    flexibleChildrenCount++
                    totalFlexible += child.flex
                    // 存储一个链表维护可以flex的子视图
                    //一遍后续可以快速计算
                    if (firstFlexChild == null) {
                        firstFlexChild = child
                    }
                    if (currentFlexChild != null) {
                        currentFlexChild.nextFlexChild = child
                    }
                    currentFlexChild = child

                    // 这时我们虽然不知道确切的尺寸信息，但是已经知道了padding , border , margin，我们可以利用这些信息来给子视图确定一个最小的size，计算剩余可用的空间。
                    // 下一个content的距离等于当前子视图Leading和Trailing的padding , border , margin6个尺寸之和。
                    nextContentDim = child.paddingAndBorderAndMarginDimension(mainAxis)
                } else {
                    maxWidth = Float.undefined
                    if (!isMainRowDirection) {
                        //如果是 COLUMN 模式，行排列，最大宽度 与父节点宽度已定：(父节点 - 父节点padding-border)，夫节点宽度没定（父节点的夫节点的宽度 - 夫节点的maargin - 父节点的padding- 父节点的border）
                        maxWidth = if (node.isStyleLayoutDimenDefined(resolvedRowAxis)) {
                            node.getStyleLayoutDimension(resolvedRowAxis) - paddingAndBorderAxisResolvedRow
                        } else {
                            parentMaxWidth - node.getStyleMargin(resolvedRowAxis) - paddingAndBorderAxisResolvedRow
                        }
                    }

                    // 递归调用layout函数，进行不能拉伸的子视图的递归布局。
                    if (alreadyComputedNextLayout == 0) {
                        layoutNode(child, maxWidth, layoutContext, direction, needLayoutAbsoluteChild, dirtyList)
                    }

                    // 由于绝对布局的子视图的位置和layout无关，所以我们不能用它们来计算mainContentDim,需要跳过
                    if (child.isFlexRelative()) {
                        nonFlexibleChildrenCount++
                        // At this point we know the final size and margin of the element.
                        /*** 至此确定出了不可拉伸的子视图的布局。 */
                        nextContentDim =
                            child.getLayoutDimension(mainAxis) + child.getStyleMargin(mainAxis)
                    }
                }

                // 将要加入的元素可能会被挤到下一行 （挤不下了，并且是wrap）
                if (isNodeFlexWrap && isMainDimDefined && mainContentDim + nextContentDim > definedMainDim && i != startLine) {
                    // 如果这里只有一个元素，它可能就需要单独占一行
                    nonFlexibleChildrenCount--
                    alreadyComputedNextLayout = 1
                    break
                }

                // 停止在主轴上堆叠子视图，剩余的子视图都在循环C里面布局
                if (isSimpleStackMain && (!child.isFlexRelative() || child.isFlex())) {
                    isSimpleStackMain = false
                    firstComplexMain = i
                }

                // 停止在侧轴上堆叠子视图，剩余的子视图都在循环D里面布局
                if (isSimpleStackCross && (!child.isFlexRelative() ||
                            (alignItem != FlexAlign.STRETCH && alignItem != FlexAlign.FLEX_START) ||
                            child.getStyleLayoutDimension(crossAxis).isUndefined())
                ) {
                    isSimpleStackCross = false
                    firstComplexCross = i
                }

                if (isSimpleStackMain) {
                    child.setLayoutPosition(mainAxis, child.getLayoutPosition(mainAxis) + mainDim)
                    if (isMainDimDefined) {
                        child.setLayoutTrailingPosition(mainAxis,
                            node.getLayoutDimension(mainAxis) - child.getLayoutDimension(mainAxis) - child.getLayoutPosition(
                                mainAxis))
                    }
                    mainDim += child.getLayoutDimension(mainAxis) + child.getStyleMargin(mainAxis)
                    crossDim = max(crossDim,
                        child.boundAxis(crossAxis,
                            child.getLayoutDimension(crossAxis) + child.getStyleMargin(crossAxis)))
                }

                if (isSimpleStackCross) {
                    child.setLayoutPosition(crossAxis,
                        child.getLayoutPosition(crossAxis) + linesCrossDim + leadingPaddingAndBorderCross)
                    if (isCrossDimDefined) {
                        child.setLayoutTrailingPosition(crossAxis,
                            node.getLayoutDimension(crossAxis) - child.getLayoutDimension(crossAxis) - child.getLayoutPosition(
                                crossAxis))
                    }
                }

                alreadyComputedNextLayout = 0
                mainContentDim += nextContentDim
                endLine = i + 1
            }
            /**循环A结束以后，会计算出endLine，计算出主轴上的尺寸，侧轴上的尺寸。不可拉伸的子视图的布局也会被确定。**/

            /**
             * 循环B主要分为2个部分，第一个部分是用来布局可拉伸的子视图。
             *
             */

            // 为了在主轴上布局，需要控制两个space，一个是第一个子视图和最左边的距离，另一个是两个子视图之间的距离
            var leadingMainDim = 0f
            var betweenMainDim = 0f

            // The remaining available space that needs to be allocated
            // 记录剩余的可用空间

            // The remaining available space that needs to be allocated
            // 记录剩余的可用空间
            var remainingMainDim = 0f
            remainingMainDim = if (isMainDimDefined) {
                definedMainDim - mainContentDim
            } else {
                max(mainContentDim, 0f) - mainContentDim
            }

            // 如果当前还有可拉伸的子视图，它们就要填充剩余的可用空间
            if (flexibleChildrenCount != 0) {
                //每一份flex dimesion
                var flexibleMainDim = remainingMainDim / totalFlexible
                var baseMainDim: Float
                var boundMainDim: Float

                //遍历下 flexChile链条
                // 如果剩余的空间不能提供给可拉伸的子视图，不能满足它们的最大或者最小的bounds，那么这些子视图也要排除到计算拉伸的过程之外
                currentFlexChild = firstFlexChild
                while (currentFlexChild != null) {
                    if (currentFlexChild.isShow) {
                        baseMainDim =
                            flexibleMainDim * currentFlexChild.flex + currentFlexChild.paddingAndBorderDimension(
                                mainAxis)
                        boundMainDim = currentFlexChild.boundAxis(mainAxis, baseMainDim)

                        //如果剩余的空间不能提供给可拉伸的子视图，不能满足它们的最大或者最小的bounds，那么这些子视图也要排除到计算拉伸的过程之外
                        //只能是他们的boundMainDim的占用，不需要去分割 剩下的空间（分割也满足不了）
                        if (baseMainDim != boundMainDim) {
                            remainingMainDim -= boundMainDim
                            totalFlexible -= currentFlexChild.flex
                        }
                    }
                    currentFlexChild = currentFlexChild.nextFlexChild
                }

                //真正每份flex 的大小
                flexibleMainDim = remainingMainDim / totalFlexible

                // 不可以拉伸的子视图可以在父视图内部overflow，在这种情况下，假设没有可用的拉伸space
                if (flexibleMainDim < 0) {
                    flexibleMainDim = 0f
                }

                //重新遍历 flexable链表
                currentFlexChild = firstFlexChild
                while (currentFlexChild != null) {
                    if (currentFlexChild.isShow) {

                        // 在这层循环里面我们已经可以确认子视图的主轴最终大小了
                        currentFlexChild.setLayoutDimension(mainAxis,
                            currentFlexChild.boundAxis(mainAxis,
                                flexibleMainDim * currentFlexChild.flex + currentFlexChild.paddingAndBorderDimension(
                                    mainAxis)))

                        // 计算水平方向轴上子视图的最大宽度
                        maxWidth = Float.undefined
                        if (node.isStyleLayoutDimenDefined(resolvedRowAxis)) {
                            maxWidth =
                                node.getStyleLayoutDimension(resolvedRowAxis) - paddingAndBorderAxisResolvedRow
                        } else if (!isMainRowDirection) {
                            maxWidth =
                                parentMaxWidth - node.getStyleMargin(resolvedRowAxis) - paddingAndBorderAxisResolvedRow
                        }

                        layoutNode(currentFlexChild, maxWidth, layoutContext, direction, needLayoutAbsoluteChild, dirtyList)
                    }
                    child = currentFlexChild
                    currentFlexChild = currentFlexChild.nextFlexChild
                    child.nextFlexChild = null
                }
                //在上述2个while结束以后，所有可以被拉伸的子视图就都dimension 大小就完成了。
                // We use justifyContent to figure out how to allocate the remaining
                // space available
                //已知需要分多少，下面是通过justifyContent 去确定怎么分剩下的空间，分那里的空间（坐标）
                //可flex拉伸的视图布局完成以后，这里是收尾工作，根据justifyContent，更改betweenMainDim和leadingMainDim的大小。
            } else if (justifyContent != FlexJustifyContent.FLEX_START) {
                when (justifyContent) {
                    FlexJustifyContent.CENTER -> {
                        leadingMainDim = remainingMainDim / 2
                    }
                    FlexJustifyContent.FLEX_END -> {
                        leadingMainDim = remainingMainDim
                    }
                    FlexJustifyContent.SPACE_BETWEEN -> {
                        remainingMainDim = max(remainingMainDim, 0f)
                        betweenMainDim =
                            if (flexibleChildrenCount + nonFlexibleChildrenCount - 1 != 0) {
                                remainingMainDim / (flexibleChildrenCount + nonFlexibleChildrenCount - 1)
                            } else {
                                0f
                            }
                    }
                    FlexJustifyContent.SPACE_AROUND -> {
                        // leadingMainDim 是 betweenMainDim 的一半
                        betweenMainDim =
                            if (flexibleChildrenCount + nonFlexibleChildrenCount == 0) {
                                remainingMainDim
                            } else {
                                remainingMainDim / (flexibleChildrenCount + nonFlexibleChildrenCount)
                            }
                        leadingMainDim = betweenMainDim / 2
                    }
                    FlexJustifyContent.SPACE_EVENLY -> {
                        betweenMainDim =
                            if (flexibleChildrenCount + nonFlexibleChildrenCount == 0) {
                                remainingMainDim
                            } else {
                                remainingMainDim / ((flexibleChildrenCount + nonFlexibleChildrenCount) + 1)
                            }
                        leadingMainDim = betweenMainDim
                    }
                    else -> {}
                }
            }

            // <Loop C>

            // 在这个循环中，所有子视图的宽和高都将被确定下来。在确定各个子视图的坐标的时候，同时也将确定父视图的宽和高。

            mainDim += leadingMainDim

            // 按照Line，一层层的循环
            for (j in firstComplexMain until endLine) {
                child = node.getChildAt(j)
                if (child != null) {
                    if (!child.isShow) {
                        continue
                    }
                } else {
                    continue
                }

                if (child.isFlexAbsolute() && (child.isStyleLeadingPositionDefined(mainAxis) || child.isStyleLayoutDimenDefined(mainAxis))) {
                    // 到这里，绝对坐标的子视图的坐标已经确定下来了，左边距和上边距已经被定下来了。这时子视图的相对坐标可以确定了(and parent's border/self margin).。
                    val dimen = child.getStyleLeadingPosition(mainAxis,
                        0f) + node.getStyleBorderLeadingSpacing(mainAxis) + child.getStyleMarginLeadingSpacing(
                        mainAxis)
                    child.setLayoutPosition(mainAxis, dimen)
                } else {
                    // 如果子视图不是绝对坐标，坐标是相对的，或者还没有确定下来左边距和上边距，那么就根据当前位置确定坐标
                    child.setLayoutPosition(mainAxis, child.getLayoutPosition(mainAxis) + mainDim)

                    // 确定trailing的坐标位置
                    if (isMainDimDefined) {
                        child.setLayoutTrailingPosition(mainAxis,
                            node.getLayoutDimension(mainAxis) - child.getLayoutDimension(mainAxis) - child.getLayoutPosition(
                                mainAxis))
                    }

                    // 接下来开始处理相对坐标的子视图，具有绝对坐标的子视图不会参与下述的布局计算中
                    if (child.isFlexRelative()) {
                        // 主轴上的宽度是由所有的子视图的宽度累加而成
                        mainDim += betweenMainDim + child.getLayoutDimension(mainAxis) + child.getStyleMargin(
                            mainAxis)
                        // 侧轴的高度是由最高的子视图决定的
                        crossDim = max(crossDim,
                            child.boundAxis(crossAxis,
                                child.getLayoutDimension(crossAxis) + child.getStyleMargin(crossAxis)))
                    }
                }
            }

            //test
            var containerCrossAxis = node.getLayoutDimension(crossAxis)
            //没有指定交叉轴dim的时候
            if (!isCrossDimDefined) {
                containerCrossAxis =
                    max(node.boundAxis(crossAxis, crossDim + paddingAndBorderAxisCross),
                        paddingAndBorderAxisCross)
            }

            /**在循环C中，会在主轴上计算出所有子视图的坐标，包括各个子视图的宽和高。****/

            /**LOOP D 处理在交叉轴的坐标****/
            // <Loop D> Position elements in the cross axis
            for (k in firstComplexCross until endLine) {
                child = node.getChildAt(k)
                if (child != null) {
                    if (!child.isShow) {
                        continue
                    }
                } else {
                    continue
                }

                if (child.isFlexAbsolute() && (child.isStyleLeadingPositionDefined(crossAxis) || child.isStyleLayoutDimenDefined(crossAxis))) {
                    // 到这里，绝对坐标的子视图的坐标已经确定下来了，上下左右至少有一边的坐标已经被定下来了。这时子视图的绝对坐标可以确定了。
                    child.setLayoutPosition(crossAxis,
                        child.getStyleLeadingPosition(crossAxis,
                            0f) + node.getStyleBorderLeadingSpacing(crossAxis) + child.getStyleMarginLeadingSpacing(
                            crossAxis))
                } else {
                    var leadingCrossDim = leadingPaddingAndBorderCross

                    // 在侧轴上，针对相对坐标的子视图，我们利用父视图的alignItems或者子视图的alignSelf来确定具体的坐标位置
                    if (child.isFlexRelative()) {
                        val alignItem = node.getAlignItem(child)
                        if (alignItem == FlexAlign.STRETCH) {
                            // 如果在侧轴上子视图还没有确定尺寸，那么才会相应STRETCH拉伸。
                            if (!child.isLayoutDimenDefined(crossAxis)) {
                                val dm = max(child.boundAxis(crossAxis,
                                    containerCrossAxis - paddingAndBorderAxisCross -
                                            (child.getStyleMargin(crossAxis))),
                                    child.paddingAndBorderDimension(crossAxis))
                                child.setLayoutDimension(crossAxis, dm)
                                if (firstCrossStretchChild == null) {
                                    firstCrossStretchChild = child
                                    currentCrossStretchChild = firstCrossStretchChild
                                } else {
                                    currentCrossStretchChild!!.nextMinHeightChild = child
                                    currentCrossStretchChild = child
                                }
                            }
                        } else if (alignItem != FlexAlign.FLEX_START) {
                            // 在侧轴上剩余的空间等于父视图在侧轴上的高度减去子视图的在侧轴上padding、Border、Margin以及高度
                            val remainingCrossDim =
                                containerCrossAxis - paddingAndBorderAxisCross - (child.getLayoutDimension(
                                    crossAxis) + child.getStyleMargin(crossAxis))
                            leadingCrossDim += if (alignItem == FlexAlign.CENTER) {
                                remainingCrossDim / 2
                            } else { // CSSAlign.FLEX_END
                                remainingCrossDim
                            }
                        }
                    }

                    // 确定子视图在侧轴上的坐标位置
                    child.setLayoutPosition(crossAxis,
                        child.getLayoutPosition(crossAxis) + linesCrossDim + leadingCrossDim)

                    // 确定trailing的坐标.
                    if (isCrossDimDefined) {
                        child.setLayoutTrailingPosition(crossAxis,
                            node.getLayoutDimension(crossAxis) - child.getLayoutDimension(crossAxis) - child.getLayoutPosition(
                                crossAxis))
                    }
                }
            }

            linesCrossDim += crossDim
            linesMainDim = max(linesMainDim, mainDim)
            linesCount += 1
            startLine = endLine
        }

        /**** <Loop E> 多行时，处理alignContent
         * http://www.w3.org/TR/2012/CR-css3-flexbox-20120918/#csslayout-algorithm
         *section 9.4
         *执行循环E有一个前提，就是，行数至少要超过一行，并且侧轴上有高度定义。满足了这个前提条件以后才会开始下面的align规则。
         *在循环E中会处理侧轴上的align拉伸规则。这里会布局alignContent和AlignItem。
         * */
        if (linesCount > 1 && isCrossDimDefined) {
            val nodeCrossAxisInnerSize =
                node.getLayoutDimension(crossAxis) - paddingAndBorderAxisCross
            val remainingAlignContentDim = nodeCrossAxisInnerSize - linesCrossDim

            var crossDimLead = 0f
            var currentLead = leadingPaddingAndBorderCross

            // 布局alignContent
            val alignContent = node.alignContent
            if (alignContent == FlexAlign.FLEX_END) {
                currentLead += remainingAlignContentDim
            } else if (alignContent == FlexAlign.CENTER) {
                currentLead += remainingAlignContentDim / 2
            } else if (alignContent == FlexAlign.STRETCH) {
                if (nodeCrossAxisInnerSize > linesCrossDim) {
                    crossDimLead = remainingAlignContentDim / linesCount
                }
            }

            var endIndex = 0
            for (index in 0 until linesCount) {
                i = index
                val startIndex = endIndex

                // 计算每一行的行高，行高根据lineHeight和子视图在侧轴上的高度加上下的Margin之和比较，取最大值
                var lineHeight = 0f
                for (innerIndex in startIndex until childCount) {
                    ii = innerIndex
                    child = node.getChildAt(ii)
                    if (child != null) {
                        if (!child.isShow || !child.isFlexRelative()) {
                            continue
                        }
                    } else {
                        continue
                    }
                    if (child.lineIndex != i) {
                        break
                    }
                    //may has bug
                    if (child.isLayoutDimenDefined(crossAxis)) {
                        lineHeight = max(lineHeight,
                            child.getLayoutDimension(crossAxis) + child.getStyleMargin(crossAxis))
                    }
                }
                endIndex = ii
                lineHeight += crossDimLead

                for (innerIndex in startIndex until endIndex) {
                    ii = innerIndex
                    child = node.getChildAt(ii)
                    if (child != null) {
                        if (!child.isShow || !child.isFlexRelative()) {
                            continue
                        }
                    } else {
                        continue
                    }

                    // 布局AlignItem,algnContent 对alignItem的影响
                    when (node.getAlignItem(child)) {
                        FlexAlign.FLEX_START -> {
                            child.setLayoutPosition(crossAxis,
                                currentLead + child.getStyleMarginLeadingSpacing(crossAxis))
                        }
                        FlexAlign.FLEX_END -> {
                            child.setLayoutPosition(crossAxis,
                                currentLead + lineHeight - child.getStyleMarginTrailingSpacing(
                                    crossAxis) - child.getStyleLayoutDimension(crossAxis))
                        }
                        FlexAlign.CENTER -> {
                            val childHeight = child.getLayoutDimension(crossAxis)
                            child.setLayoutPosition(crossAxis,
                                currentLead + (lineHeight - childHeight) / 2.0f)
                        }
                        FlexAlign.STRETCH -> {
                            child.setLayoutPosition(crossAxis,
                                currentLead + child.getStyleMarginLeadingSpacing(crossAxis))
                            // TODO(prenaux): Correctly set the height of items with undefined
                            //                (auto) crossAxis dimension.
                        }
                        else -> {}
                    }
                }

                currentLead += lineHeight
            }
        }

        var needsMainTrailingPos = false
        var needsCrossTrailingPos = false

        // 如果某个视图没有被指定宽或者高，并且也没有被父视图设置宽和高，那么在这里通过子视图来设置宽和高
        if (!isMainDimDefined) {
            val originMainDim = linesMainDim + node.getStylePaddingTrailingSpacing(mainAxis) + node.getStyleBorderTrailingSpacing(mainAxis)
            val dimen = max(node.boundAxis(mainAxis, originMainDim), paddingAndBorderAxisMain)
            node.setLayoutDimension(mainAxis, dimen)
            if (originMainDim != dimen && node.justifyContent != FlexJustifyContent.FLEX_START && node.childCount != 0) {
                node.alignChildMainAxis(mainAxis, paddingAndBorderAxisMain)
            }
            if (mainAxis == FlexDirection.ROW_REVERSE || mainAxis == FlexDirection.COLUMN_REVERSE) {
                needsMainTrailingPos = true
            }
        }

        if (!isCrossDimDefined) {
            // 视图的高度等于内部子视图的高度加上上下的Padding、Border的宽度和侧轴上Padding、Border，两者取最大值。
            node.setLayoutDimension(crossAxis,
                max(node.boundAxis(crossAxis, linesCrossDim + paddingAndBorderAxisCross),
                    paddingAndBorderAxisCross))
            if (crossAxis == FlexDirection.ROW_REVERSE || crossAxis == FlexDirection.COLUMN_REVERSE) {
                needsCrossTrailingPos = true
            }
        }

        /**
         * <Loop F> 这一步是设置当前node节点的Trailing坐标，如果有必要的话。如果不需要，这一步会直接跳过
         */
        if (needsMainTrailingPos || needsCrossTrailingPos) {
            for (index in 0 until childCount) {
                child = node.getChildAt(index)
                if (child != null) {
                    if (!child.isShow) {
                        continue
                    }
                } else {
                    continue
                }

                if (needsMainTrailingPos) {
                    child.setLayoutTrailingPosition(mainAxis,
                        node.getLayoutDimension(mainAxis) - child.getLayoutDimension(mainAxis) - child.getLayoutPosition(
                            mainAxis))
                }

                if (needsCrossTrailingPos) {
                    child.setLayoutTrailingPosition(crossAxis,
                        node.getLayoutDimension(crossAxis) - child.getLayoutDimension(crossAxis) - child.getLayoutPosition(
                            crossAxis))
                }
            }
        }

        // <Loop G> 用来给绝对坐标的子视图计算宽度和高度。
        currentAbsoluteChild = firstAbsoluteChild
        while (currentAbsoluteChild != null) {
            if (currentAbsoluteChild.isShow) {
                var relayoutAbsoluteChild = false
                // 绝对坐标的子视图在主轴上的宽度，在侧轴上的高度都不能比Padding、Border的总和小
                for (index in 0 until 2) {
                    axis = if (index != 0) {
                        FlexDirection.ROW
                    } else {
                        FlexDirection.COLUMN
                    }

                    if (node.isLayoutDimenDefined(axis)
                        && !currentAbsoluteChild.isStyleLayoutDimenDefined(axis)
                        && !currentAbsoluteChild.getStyleLeadingPosition(axis).isUndefined()
                        && !currentAbsoluteChild.getStyleTrailingPosition(axis).isUndefined()
                    ) {
                        val dimen =
                            max(currentAbsoluteChild.boundAxis(axis, node.getLayoutDimension(axis) -
                                    (node.getStyleBorderTrailingSpacing(axis) + node.getStyleBorderLeadingSpacing(
                                        axis)) -
                                    currentAbsoluteChild.getStyleMargin(axis) - currentAbsoluteChild.getStyleLeadingPosition(
                                axis,
                                0f) -
                                    currentAbsoluteChild.getStyleTrailingPosition(axis, 0f)),
                                currentAbsoluteChild.paddingAndBorderDimension(axis))
                        val oldDim = currentAbsoluteChild.getLayoutDimension(axis)
                        currentAbsoluteChild.setLayoutDimension(axis, dimen)

                        if (!relayoutAbsoluteChild) {
                            relayoutAbsoluteChild = currentAbsoluteChild.detectNeedRelayoutAbsoluteChild(oldDim, dimen, axis)
                        }

                    }

                    // 当前子视图的坐标等于当前视图的宽度减去子视图的宽度再减去trailing
                    if (!currentAbsoluteChild.getStyleTrailingPosition(axis)
                            .isUndefined() && currentAbsoluteChild.getStyleLeadingPosition(axis)
                            .isUndefined()
                    ) {
                        currentAbsoluteChild.setLayoutLeadingPosition(axis,
                            node.getLayoutDimension(axis) - currentAbsoluteChild.getLayoutDimension(
                                axis) - currentAbsoluteChild.getStyleTrailingPosition(axis, 0f))
                    }
                }

                if (needLayoutAbsoluteChild && currentAbsoluteChild.childCount > 0 && relayoutAbsoluteChild) {
                    node.relayoutAbsoluteChild(currentAbsoluteChild, layoutContext, direction, dirtyList)
                }
            }
            child = currentAbsoluteChild
            currentAbsoluteChild = currentAbsoluteChild.nextAbsoluteChild
            child.nextAbsoluteChild = null
        }
    }

    private fun FlexNode.relayoutAbsoluteChild(
        absoluteChild: FlexNode,
        layoutContext: FlexLayoutContext?,
        flexDirection: FlexLayoutDirection,
        dirtyList: MutableSet<FlexNode>
    ) {
        val mainAxis = resolveAxis(absoluteChild.flexDirection, flexDirection)
        val crossAxis = getCrossFlexDirection(absoluteChild.flexDirection, flexDirection)
        val mainLayoutLeadingPosition = absoluteChild.getLayoutLeadingPosition(mainAxis)
        val crossLayoutLeadingPosition = absoluteChild.getLayoutLeadingPosition(crossAxis)
        if (!mainLayoutLeadingPosition.isUndefined()) {
            // 重新布局绝对布局时，需要减去算spacing和position，因为进入布局的时候会累加
            absoluteChild.setLayoutLeadingPosition(mainAxis, mainLayoutLeadingPosition - absoluteChild.getStyleMarginLeadingSpacing(mainAxis) - absoluteChild.getRelativePosition(mainAxis))
        }
        if (!crossLayoutLeadingPosition.isUndefined()) {
            // 重新布局绝对布局时，需要减去算spacing和position，因为进入布局的时候会累加
            absoluteChild.setLayoutLeadingPosition(crossAxis, crossLayoutLeadingPosition - absoluteChild.getStyleMarginLeadingSpacing(crossAxis) - absoluteChild.getRelativePosition(crossAxis))
        }

        layoutNode(
            absoluteChild,
            layoutWidth,
            layoutContext,
            flexDirection,
            false,
            dirtyList
        )
    }

    /**
     * 检测绝对布局是否需要重新布局
     */
    private fun FlexNode.detectNeedRelayoutAbsoluteChild(
        oldDim: Float,
        newDim: Float,
        axis: FlexDirection
    ): Boolean {
        if (!oldDim.isUndefined() && !newDim.isUndefined() && oldDim != newDim) { // 绝对布局前后值发生变化的前提才会考虑是否重新布局
            if (axis == flexDirection) {
                // 如果是非Flex_START并且对应的轴没有设置大小的话，需要重新测量
                if (justifyContent != FlexJustifyContent.FLEX_START && !isStyleLayoutDimenDefined(axis)) {
                    return true
                }
            }  else {
                // 如果alignItems或者alignSelf是需要对齐，并且对应的轴没有设置大小的话，需要重新测量
                if (((alignItems != FlexAlign.FLEX_START && alignItems != FlexAlign.STRETCH)
                            || hadAlignSelfNoFlexStartChild()) && !isStyleLayoutDimenDefined(axis)) {
                    return true
                }
            }
        }
        return false
    }

    private fun FlexNode.hadAlignSelfNoFlexStartChild(): Boolean {
        val size = childCount
        for (i in 0 until size) {
            val child = getChildAt(i) ?: continue
            if (child.alignSelf != FlexAlign.FLEX_START && child.alignSelf != FlexAlign.AUTO) {
                return true
            }
        }
        return false
    }

    /**
     * 校准孩子在主轴上的对齐
     * @param mainAxis 主轴
     * @param paddingAndBorderAxisMain 主轴额外的间距大小
     */
    private fun FlexNode.alignChildMainAxis(mainAxis: FlexDirection, paddingAndBorderAxisMain: Float) {
        val dimen = getLayoutDimension(mainAxis)
        val size = childCount
        var mainContentDim = 0f // 孩子在主轴的总大小
        for (i in 0 until size) {
            val child = getChildAt(i) ?: continue
            mainContentDim += child.getLayoutDimension(mainAxis) + child.getStyleMargin(mainAxis)
        }
        val remainingMainDim = dimen - mainContentDim // 主轴剩余大小
        var mainDim = paddingAndBorderAxisMain
        var leadingMainDim = 0f
        var betweenMainDim = 0f
        when (justifyContent) {
            FlexJustifyContent.CENTER -> {
                leadingMainDim = remainingMainDim / 2
            }
            FlexJustifyContent.FLEX_END -> {
                leadingMainDim = remainingMainDim
            }
            FlexJustifyContent.SPACE_BETWEEN -> {
                betweenMainDim = remainingMainDim / (childCount - 1)
            }
            FlexJustifyContent.SPACE_AROUND -> {
                // leadingMainDim 是 betweenMainDim 的一半
                betweenMainDim = remainingMainDim / childCount
                leadingMainDim = betweenMainDim / 2
            }
            FlexJustifyContent.SPACE_EVENLY -> {
                betweenMainDim = remainingMainDim / (childCount + 1)
                leadingMainDim = betweenMainDim
            }
            else -> {}
        }

        mainDim += leadingMainDim
        for (i in 0 until size) {
            val child = getChildAt(i) ?: continue
            child.setLayoutPosition(mainAxis, child.getStyleMargin(mainAxis) + mainDim)

            if (child.isFlexRelative()) {
                // 主轴上的宽度是由所有的子视图的宽度累加而成
                mainDim += betweenMainDim + child.getLayoutDimension(mainAxis) + child.getStyleMargin(mainAxis)
            }
        }
    }

    private fun needsRelayout(
        node: FlexNode,
        parentMaxWidth: Float
    ): Boolean {
        return node.isDirty
                || !node.lastLayoutHeight.layoutSizeEqual(node.layoutHeight)
                || !node.lastLayoutWidth.layoutSizeEqual(node.layoutWidth)
                || !node.lastParentMaxWith.layoutSizeEqual(parentMaxWidth)
    }

    // done
    private fun FlexNode.isFlexRelative(): Boolean {
        return positionType == FlexPositionType.RELATIVE
    }

    // done
    private fun FlexNode.isFlexAbsolute(): Boolean {
        return positionType == FlexPositionType.ABSOLUTE
    }

    //解析排版规则：默认LTR
    private fun FlexNode.resolveDirection(
        parentDirection: FlexLayoutDirection?
    ): FlexLayoutDirection {
        var direction = styleDirection
        if (direction == FlexLayoutDirection.INHERIT) {
            direction = parentDirection ?: FlexLayoutDirection.LTR
        }
        return direction
    }

    //排版规则，对Axis 的影响，如在RTL （right to left）模式下，ROW 其实是 ROW_REVERSE，ROW_REVERSE 其实是ROW
    //解析主轴模式是否会因为 排版方向影响
    private fun resolveAxis(
        axis: FlexDirection,
        direction: FlexLayoutDirection
    ): FlexDirection {
        return if (direction == FlexLayoutDirection.RTL) {
            return when (axis) {
                FlexDirection.ROW -> {
                    FlexDirection.ROW_REVERSE
                }
                FlexDirection.ROW_REVERSE -> {
                    FlexDirection.ROW
                }
                else -> {
                    axis
                }
            }
        } else {
            axis
        }
    }

    //交叉轴方向
    private fun getCrossFlexDirection(
        axis: FlexDirection,
        direction: FlexLayoutDirection
    ): FlexDirection {
        //主轴是 COLUMN 的时候，交叉轴是ROW，但是可能受到排版影响，故需要resolveAxis一下
        return if (axis == FlexDirection.COLUMN || axis == FlexDirection.COLUMN_REVERSE) {
            resolveAxis(FlexDirection.ROW, direction)
        } else {
            FlexDirection.COLUMN
        }
    }

    //先简单通过 Style中指的值指定某个轴 dimension，但是style指定的值不一定是最终的 layout 的dimension
    // done
    private fun FlexNode.setDimensionFromStyle(axis: FlexDirection) {
        // The parent already computed us a width or height. We just skip it
        if (isLayoutDimenDefined(axis)) {
            return
        }

        // We only run if there's a width or height defined or min height or max height
        // 当 flexStyle 中有指定的时候填充  同时需要关注max or min
        if (axis == FlexDirection.COLUMN || axis == FlexDirection.COLUMN_REVERSE) {
            if ((styleMinHeight.isUndefined() && styleMaxHeight.isUndefined() && getStyleLayoutDimension(
                    axis).isUndefined())
                || getStyleLayoutDimension(axis) < 0.0f // fixme
            ) {
                return
            }
        } else {
            if ((getStyleLayoutDimension(axis).isUndefined() && styleMinWidth.isUndefined() && styleMaxWidth.isUndefined())
                || getStyleLayoutDimension(axis) < 0.0f // fixme
            ) {
                return
            }
        }

        // The dimensions can never be smaller than the padding and border
        // dimensions 中的宽高 至少 为 padding + border =>盒子模型
        val maxLayoutDimension =
            max(boundAxis(axis, getStyleLayoutDimension(axis)), paddingAndBorderDimension(axis))
        setLayoutDimension(axis, maxLayoutDimension)
    }

    //[0,0,1,1]
    //当dim [axis] 为 COLUMN(0),COLUMN_REVERSE(1),dim[axix] = 1
    //当dim [axis] 为 ROW(2),ROW_REVERSE(3),dim[axix] = 0
    // 故 dimensions[dim [axis]] 在 COLUMN 指定高 ， ROW 指定宽
    // done
    private val dim = intArrayOf(
        FlexLayout.DimensionType.DIMENSION_HEIGHT.ordinal,
        FlexLayout.DimensionType.DIMENSION_HEIGHT.ordinal,
        FlexLayout.DimensionType.DIMENSION_WIDTH.ordinal,
        FlexLayout.DimensionType.DIMENSION_WIDTH.ordinal
    )

    //[1,3,0,2]
    private val leading = arrayOf(
        StyleSpace.Type.TOP,
        StyleSpace.Type.BOTTOM,
        StyleSpace.Type.LEFT,
        StyleSpace.Type.RIGHT
    )

    /****
     * [1,3,6,6] -> 表示leading 的方位
     * leadingSpacing[axis]
     * leadingSpacing[axis] 为 COLUMN(0),leadingSpacing[axis] = TOP(1)
     * leadingSpacing[axis] 为 COLUMN(1),leadingSpacing[axis] = BOTTOM(3)
     * leadingSpacing[axis] 为 ROW(2),leadingSpacing[axis] = START(6)
     * leadingSpacing[axis] 为 ROW_REVERSE(3),leadingSpacing[axis] = START(6)
     * 如 COLUMN leadingSpacing[axis] 表示这个时候 border的时候，为leadingBordr 为 border[Top]
     */
    private val leadingSpacing = arrayOf(
        StyleSpace.Type.TOP,
        StyleSpace.Type.BOTTOM,
        StyleSpace.Type.START,
        StyleSpace.Type.START
    )

    /****
     * [3,1,7,7] -> 表示trailing 的方位
     * 同理如leadingSpacing
     * done
     */
    private val trailingSpacing = arrayOf(
        StyleSpace.Type.BOTTOM,
        StyleSpace.Type.TOP,
        StyleSpace.Type.END,
        StyleSpace.Type.END
    )

    //[3,1,2,0]
    // done
    private val trailing = arrayOf(
        StyleSpace.Type.BOTTOM,
        StyleSpace.Type.TOP,
        StyleSpace.Type.RIGHT,
        StyleSpace.Type.LEFT
    )

    //元素在某轴上占空间的大小值
    //   min|---------|max
    // done
    private fun FlexNode.boundAxis(
        axis: FlexDirection,
        value: Float
    ): Float {
        var min = Float.undefined
        var max = Float.undefined
        if (axis == FlexDirection.COLUMN || axis == FlexDirection.COLUMN_REVERSE) {
            min = styleMinHeight
            max = styleMaxHeight
        } else if (axis == FlexDirection.ROW || axis == FlexDirection.ROW_REVERSE) {
            min = styleMinWidth
            max = styleMaxWidth
        }
        var boundValue = value

        //value 比 max  大，那么以max为准（不能超过max）
        if (!max.isUndefined() && max >= 0.0 && boundValue > max) {
            boundValue = max
        }
        //value 比 min 小，那么以min为主 (起码有min)
        if (!min.isUndefined() && min >= 0.0 && min > boundValue) {
            boundValue = min
        }
        return boundValue
    }

    //大于0 表示是leading的位置，小于0 表示是 trailing的位置？
    // done
    private fun FlexNode.getRelativePosition(
        axis: FlexDirection
    ): Float {
        val lead = stylePosition[leading[axis.ordinal].ordinal]
        if (!lead.isUndefined()) {
            return lead
        }

        val trailingPos = stylePosition[trailing[axis.ordinal].ordinal]
        return if (trailingPos.isUndefined()) 0f else -trailingPos
    }

    //某一个节点是否有自己的测量函数
    internal fun FlexNode.isMeasureDefined(): Boolean {
        return measureFunction != null
    }

    // done
    private fun FlexNode.getLayoutDimension(direction: FlexDirection): Float {
        return layoutDimensions[dim[direction.ordinal]]
    }

    private fun FlexNode.setLayoutDimension(direction: FlexDirection, value: Float) {
        layoutDimensions[dim[direction.ordinal]] = value
    }

    private fun FlexNode.setLayoutWidth(width: Float) {
        layoutWidth = width
    }

    // done
    private fun FlexNode.setLayoutHeight(height: Float) {
        layoutHeight = height
    }

    // done
    private fun FlexNode.getStyleLayoutDimension(direction: FlexDirection): Float {
        return styleDimensions[dim[direction.ordinal]]
    }

    // about border
    private fun FlexNode.getStyleBorderTrailingSpacing(direction: FlexDirection): Float {
        return getStyleBorderWithFallback(trailingSpacingStyleSpaceType(direction),
            trailingStyleSpaceType(direction))
    }

    private fun FlexNode.getStyleBorderLeadingSpacing(direction: FlexDirection): Float {
        return getStyleBorderWithFallback(leadingSpacingStyleSpaceType(direction),
            leadingStyleSpaceType(direction))
    }

    // about padding
    private fun FlexNode.getStylePaddingTrailingSpacing(direction: FlexDirection): Float {
        return getStylePaddingWithFallback(trailingSpacingStyleSpaceType(direction),
            trailingStyleSpaceType(direction))
    }

    private fun FlexNode.getStylePaddingLeadingSpacing(direction: FlexDirection): Float {
        return getStylePaddingWithFallback(leadingSpacingStyleSpaceType(direction),
            leadingStyleSpaceType(direction))
    }

    // about style margin
    // done
    private fun FlexNode.getStyleMarginTrailingSpacing(direction: FlexDirection): Float {
        return getStyleMarginWithFallback(trailingSpacingStyleSpaceType(direction),
            trailingStyleSpaceType(direction))
    }

    // done
    private fun FlexNode.getStyleMarginLeadingSpacing(direction: FlexDirection): Float {
        return getStyleMarginWithFallback(leadingSpacingStyleSpaceType(direction),
            leadingStyleSpaceType(direction))
    }

    // done
    private fun FlexNode.getStyleMargin(direction: FlexDirection): Float {
        return getStyleMarginLeadingSpacing(direction) + getStyleMarginTrailingSpacing(direction)
    }

    // done
    private fun FlexNode.isLayoutDimenDefined(direction: FlexDirection): Boolean {
        return !layoutDimensions[dim[direction.ordinal]].isUndefined()
    }

    // done
    private fun FlexNode.isStyleLayoutDimenDefined(direction: FlexDirection): Boolean {
        val dimen = getStyleLayoutDimension(direction)
        return !dimen.isUndefined() && dimen >= 0.0f
    }

    private fun FlexNode.getAlignItem(child: FlexNode): FlexAlign {
        return if (child.alignSelf != FlexAlign.AUTO) {
            child.alignSelf
        } else {
            alignItems
        }
    }

    private fun FlexNode.setLayoutPosition(direction: FlexDirection, value: Float) {
        layoutPosition[pos[direction.ordinal]] = value
    }

    // done
    private fun FlexNode.setLayoutLeadingPosition(direction: FlexDirection, value: Float) {
        layoutPosition[leading[direction.ordinal].ordinal] = value
    }

    // done
    private fun FlexNode.setLayoutTrailingPosition(direction: FlexDirection, value: Float) {
        layoutPosition[trailing[direction.ordinal].ordinal] = value
    }

    // done
    private fun FlexNode.getLayoutPosition(direction: FlexDirection): Float {
        return layoutPosition[pos[direction.ordinal]]
    }

    // done
    private fun FlexNode.getLayoutLeadingPosition(direction: FlexDirection): Float {
        return layoutPosition[leading[direction.ordinal].ordinal]
    }

    // done
    private fun FlexNode.getLayoutTrailingPosition(direction: FlexDirection): Float {
        return layoutPosition[trailing[direction.ordinal].ordinal]
    }

    // done
    private fun FlexNode.paddingAndBorderDimension(direction: FlexDirection): Float {
        return getStylePaddingLeadingSpacing(direction) + getStyleBorderLeadingSpacing(direction) +
                getStylePaddingTrailingSpacing(direction) + getStyleBorderTrailingSpacing(direction)
    }

    private fun FlexNode.paddingAndBorderAndMarginDimension(direction: FlexDirection): Float {
        return paddingAndBorderDimension(direction) + getStyleMarginLeadingSpacing(direction) + getStyleMarginTrailingSpacing(
            direction)
    }

    private fun FlexNode.isStyleLeadingPositionDefined(direction: FlexDirection): Boolean {
        return !getStyleLeadingPosition(direction).isUndefined()
    }

    // done
    private fun FlexNode.getStyleLeadingPosition(
        direction: FlexDirection,
        defaultValue: Float
    ): Float {
        val v = getStyleLeadingPosition(direction)
        return if (v.isUndefined()) {
            defaultValue
        } else {
            v
        }
    }

    // done
    private fun FlexNode.getStyleLeadingPosition(direction: FlexDirection): Float {
        return stylePosition[leading[direction.ordinal].ordinal]
    }

    private fun FlexNode.isStyleTrailingPositionDefined(direction: FlexDirection): Boolean {
        return !getStyleTrailingPosition(direction).isUndefined()
    }

    // done
    private fun FlexNode.getStyleTrailingPosition(
        direction: FlexDirection,
        defaultValue: Float
    ): Float {
        val v = getStyleTrailingPosition(direction)
        return if (getStyleTrailingPosition(direction).isUndefined()) {
            defaultValue
        } else {
            v
        }
    }

    private fun FlexNode.isFlex(): Boolean {
        return positionType == FlexPositionType.RELATIVE && flex > 0
    }

    // done
    private fun FlexNode.getStyleTrailingPosition(direction: FlexDirection): Float {
        return stylePosition[trailing[direction.ordinal].ordinal]
    }

    // done
    private fun trailingSpacingStyleSpaceType(direction: FlexDirection): StyleSpace.Type {
        return trailingSpacing[direction.ordinal]
    }

    // done
    private fun trailingStyleSpaceType(direction: FlexDirection): StyleSpace.Type {
        return trailing[direction.ordinal]
    }

    // done
    private fun leadingSpacingStyleSpaceType(direction: FlexDirection): StyleSpace.Type {
        return leadingSpacing[direction.ordinal]
    }

    // done
    private fun leadingStyleSpaceType(direction: FlexDirection): StyleSpace.Type {
        return leading[direction.ordinal]
    }

    //[1,3,0,2]
    private val pos = intArrayOf(
        FlexLayout.PositionType.POSITION_TOP.ordinal,
        FlexLayout.PositionType.POSITION_BOTTOM.ordinal,
        FlexLayout.PositionType.POSITION_LEFT.ordinal,
        FlexLayout.PositionType.POSITION_RIGHT.ordinal
    )

    private fun FlexNode.performMeasureFunction(
        parentMaxWidth: Float,
        paddingAndBorderAxisResolvedRow: Float,
        resolvedRowAxis: FlexDirection,
        layoutContext: FlexLayoutContext?
    ): Boolean {
        //横轴
        val isResolvedRowDimDefined = isLayoutDimenDefined(resolvedRowAxis)
        var width = if (isStyleLayoutDimenDefined(resolvedRowAxis)) {
            styleWidth
        } else if (isResolvedRowDimDefined) {
            //layout 的宽度
            getLayoutDimension(resolvedRowAxis)
        } else {
            //两者都没有定义的情况下，起始宽度是 （父视图宽度 - 左右 margin）
            parentMaxWidth - getStyleMargin(resolvedRowAxis)
        }
        //再去掉 padding border
        width -= paddingAndBorderAxisResolvedRow
        width =
            if ((width.isUndefined() && !styleMaxWidth.isUndefined())
                || (!styleMaxWidth.isUndefined() && styleMaxWidth < width)) {
                styleMaxWidth
            } else {
                width
            }
        if (width < 0) {
            KLog.e("LayoutImpl", "layout width < 0")
            width = 0f
        }

        // We only need to give a dimension for the text if we haven't got any
        // for it computed yet. It can either be from the cssstyle attribute or because
        // the element is flexible.

        //有宽高，或者指定为 flexible的时候，
//        val isRowUndefined = !isStyleLayoutDimenDefined(resolvedRowAxis) && !isResolvedRowDimDefined
//        val isColumnUndefined =
//            !isStyleLayoutDimenDefined(FlexDirection.COLUMN) && getStyleLayoutDimension(FlexDirection.COLUMN).isUndefined()

        // Let's not measure the text if we already know both dimensions
        //没有指定宽/高 的行内元素 就需要执行mesure，并且告知measure container剩余的空间
        // 宽高都被计算出来的时候，其实不需要执行measure方法了
        val measureDim = measure(
            layoutContext?.measureOutput ?: MeasureOutput(),
            width
        )
        setLayoutWidth(measureDim.width + paddingAndBorderAxisResolvedRow)
        setLayoutHeight(measureDim.height + paddingAndBorderDimension(FlexDirection.COLUMN))

        //measure节点是 叶子节点的时候
        if (childCount == 0) {
            return true
        }
        return false
    }
}