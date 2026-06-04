package com.tencent.kuikly.core.render.android.expand.component

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.Magnifier
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.css.ktx.toDpF
import com.tencent.kuikly.core.render.android.css.ktx.toPxF
import com.tencent.kuikly.core.render.android.css.ktx.toPxI
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject
import java.lang.ref.WeakReference

private const val DEBUG_LOG = false

private const val SELECTABLE_INHERIT = 0
private const val SELECTABLE_ENABLE = 1
private const val SELECTABLE_DISABLE = 2

private const val SELECTION_TYPE_CHARACTER = 0
private const val SELECTION_TYPE_WORD = 1
private const val SELECTION_TYPE_PARAGRAPH = 2
private const val SELECTION_TYPE_SENTENCE = 3

private inline fun logInfo(msg: () -> String) {
    if (DEBUG_LOG) {
        KuiklyRenderLog.i("KRTextSelector", msg())
    }
}

/**
 * Text selection controller for KRView and its KRRichTextView children.
 * Manages selection highlights, cursor handles, magnifier, and selection callbacks.
 */
internal class KRTextSelector(
    private val view: KRView
) : ViewTreeObserver.OnPreDrawListener,
    ViewTreeObserver.OnScrollChangedListener,
    View.OnLayoutChangeListener {

    companion object {

        /** Iterates all KRRichTextView in hierarchy, skipping disabled ones */
        private fun KRTextSelector.forEachText(
            action: KRRichTextView.(offsetX: Int, offsetY: Int) -> Unit
        ) {
            assert(selectable != SELECTABLE_DISABLE)
            // FIXME: KRRichTextView is unexpectedly a KRView, should we check self first?
            if (view is KRRichTextView) {
                view.action(0, 0)
                return
            }
            fun forEachTextInner(parent: ViewGroup, parentOffsetX: Int, parentOffsetY: Int) {
                assert(parent !is KRRichTextView)
                for (i in 0 until parent.childCount) {
                    val child = parent.getChildAt(i)
                    if (child is KRView && child.textSelector?.selectable == SELECTABLE_DISABLE) {
                        continue
                    }
                    if (child is KRRichTextView) {
                        // must check first, because KRRichTextView is a ViewGroup
                        child.action(parentOffsetX + child.left, parentOffsetY + child.top)
                    } else if (child is ViewGroup) {
                        forEachTextInner(
                            child,
                            parentOffsetX + child.left,
                            parentOffsetY + child.top
                        )
                    }
                }
            }
            forEachTextInner(view, 0, 0)
        }

        private fun getAttrColor(context: Context, attr: Int, defValue: Int): Int {
            val typedValue = TypedValue()
            if (context.theme.resolveAttribute(attr, typedValue, true)) {
                if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                    typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT
                ) {
                    return typedValue.data
                } else if (typedValue.resourceId != 0) {
                    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        context.getColor(typedValue.resourceId)
                    } else {
                        @Suppress("DEPRECATION")
                        context.resources.getColor(typedValue.resourceId)
                    }
                }
            }
            return defValue
        }

        private inline fun <reified T, R> ArrayList<T>.use(block: (list: ArrayList<T>) -> R): R {
            try {
                return block(this)
            } finally {
                this.clear()
            }
        }

        private fun View.distanceTo(
            relativeX: Float,
            relativeY: Float
        ): Float {
            // Calculate distance to edge (0 if inside the view)
            val dx = when {
                relativeX < 0f -> -relativeX
                relativeX > width -> relativeX - width
                else -> 0f
            }
            val dy = when {
                relativeY < 0f -> -relativeY
                relativeY > height -> relativeY - height
                else -> 0f
            }
            return dx * dx + dy * dy
        }

        private fun View.relativeTo(ancestorView: ViewGroup): Pair<Int, Int> {
            var offsetX = 0
            var offsetY = 0
            var current: View? = this
            while (current != null && current != ancestorView) {
                offsetX += current.left
                offsetY += current.top
                val parent = current.parent
                current = parent as? View
            }
            return Pair(offsetX, offsetY)
        }
    }

    private inline val kuiklyContext get() = view.kuiklyRenderContext

    // Reusable objects to avoid allocations
    private val reusableTextViewList = ArrayList<Triple<KRRichTextView, Int, Int>>()
    private val reusableRect = Rect()
    private val reusableRectF = RectF()
    private val reusablePath = Path()
    private val reusableIntArray = IntArray(2)

    private val selectionPaint = Paint().also {
        it.style = Paint.Style.FILL
        // default to translucent blue
        it.color = getAttrColor(view.context, android.R.attr.textColorHighlight, 0x6633B5E5)
    }

    private var cursorColor = 0
    private val _cursorStartView = lazy(LazyThreadSafetyMode.NONE) {
        val view = SelectionHandleView(this, view, true)
        if (cursorColor != 0) {
            view.setColor(cursorColor)
        }
        view
    }
    private val cursorViewInitialized get() = _cursorStartView.isInitialized()
    private val cursorStartView by _cursorStartView
    private val cursorEndView by lazy(LazyThreadSafetyMode.NONE) {
        val view = SelectionHandleView(this, view, false)
        if (cursorColor != 0) {
            view.setColor(cursorColor)
        }
        view
    }
    private val focusHelperView by lazy(LazyThreadSafetyMode.NONE) {
        View(view.context).apply {
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                // we should listen ACTION_UP, but unfortunately
                // KuiklyRenderViewDelegator.onBackPressed has higher priority
                if (event.action == KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_BACK
                ) {
                    return@setOnKeyListener performBackPressed()
                }
                return@setOnKeyListener false
            }
        }
    }
    private val handler by lazy(LazyThreadSafetyMode.NONE) {
        Handler(Looper.getMainLooper())
    }

    private var selectable = SELECTABLE_INHERIT
    private inline val enabled get() = selectable == SELECTABLE_ENABLE
    private var cursorStartX: Float = Float.NaN
    private var cursorStartTop: Float = Float.NaN
    private var cursorStartBottom: Float = Float.NaN
    private inline val cursorStartY get() = (cursorStartTop + cursorStartBottom) / 2f
    private var cursorEndX: Float = Float.NaN
    private var cursorEndTop: Float = Float.NaN
    private var cursorEndBottom: Float = Float.NaN
    private inline val cursorEndY get() = (cursorEndTop + cursorEndBottom) / 2f
    // Weak references to first and last text views with selection for scroll updates
    private var cursorStartRef: WeakReference<KRRichTextView>? = null
    private var cursorEndRef: WeakReference<KRRichTextView>? = null

    private var selectStartCallback: KuiklyRenderCallback? = null
    private var selectChangeCallback: KuiklyRenderCallback? = null
    private var selectEndCallback: KuiklyRenderCallback? = null
    private var selectCancelCallback: KuiklyRenderCallback? = null

    /**
     * active selecting mode
     */
    private var active: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (value) {
                subscribeSelectionHandleUpdate()
                view.activity?.also {
                    handler.post { // post to avoid add during view iteration
                        it.addContentView(
                            focusHelperView, ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                        focusHelperView.requestFocus()
                        focusHelperView.onFocusChangeListener =
                            View.OnFocusChangeListener { _, hasFocus ->
                                if (!hasFocus) {
                                    this@KRTextSelector.clearSelection()
                                }
                            }
                    }
                }
            } else {
                view.activity?.also {
                    handler.post { // post to avoid remove during view iteration
                        focusHelperView.onFocusChangeListener = null
                        focusHelperView.clearFocus()
                        (focusHelperView.parent as? ViewGroup)?.removeView(focusHelperView)
                    }
                }
                unsubscribeSelectionHandleUpdate()
            }
        }
    private var selectionRect = RectF()

    // Dragging state: fixed cursor stays, movable cursor follows finger
    private var dragging = false
    private var dragFixedX: Float = Float.NaN
    private var dragFixedY: Float = Float.NaN
    private var dragMovableX: Float = Float.NaN
    private var dragMovableY: Float = Float.NaN

    // magnifier support
    private var minLineHeightForMagnifier: Float = 0f
    private var maxLineHeightForMagnifier: Float = Float.MAX_VALUE
    private val magnifierAnimator by lazy(LazyThreadSafetyMode.NONE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            minLineHeightForMagnifier = kuiklyContext.toPxF(25f)
            maxLineHeightForMagnifier = kuiklyContext.toPxF(36f)
            MagnifierMotionAnimator(view)
        } else {
            null
        }
    }

    private var invalidatePending = false

    fun handleTouchDown(isStart: Boolean) {
        assert(active)
        dragging = true
        if (isStart) {
            dragFixedX = cursorEndX
            dragFixedY = cursorEndY
            dragMovableX = cursorStartX
            dragMovableY = cursorStartY
            cursorStartView.hide()
        } else {
            dragFixedX = cursorStartX
            dragFixedY = cursorStartY
            dragMovableX = cursorEndX
            dragMovableY = cursorEndY
            cursorEndView.hide()
        }
    }

    fun handleTouchMove(deltaX: Float, deltaY: Float) {
        assert(dragging)
        val x1 = dragFixedX
        val y1 = dragFixedY
        val x2 = dragMovableX + deltaX
        val y2 = dragMovableY + deltaY

        val oldLeft = selectionRect.left
        val oldTop = selectionRect.top
        val oldRight = selectionRect.right
        val oldBottom = selectionRect.bottom
        updateSelection(x1, y1, x2, y2)
        if (selectionRect.left != oldLeft || selectionRect.top != oldTop ||
            selectionRect.right != oldRight || selectionRect.bottom != oldBottom
        ) {
            notifySelectChange()
        }
        magnifierAnimator?.apply {
            val lineHeight: Float
            val centerY: Float
            if (draggingCursorStart()) {
                lineHeight = cursorStartBottom - cursorStartTop
                centerY = cursorStartY
            } else {
                lineHeight = cursorEndBottom - cursorEndTop
                centerY = cursorEndY
            }

            if (lineHeight > maxLineHeightForMagnifier) {
                dismiss()
            } else {
                updateZoom((minLineHeightForMagnifier / lineHeight).coerceAtLeast(1f))
                show(x2, centerY)
            }
        }
    }

    fun handleTouchUp() {
        assert(dragging)
        dragging = false
        dragFixedX = Float.NaN
        dragFixedY = Float.NaN
        dragMovableX = Float.NaN
        dragMovableY = Float.NaN
        magnifierAnimator?.dismiss()
        if (active) {
            notifySelectEnd()
        }
    }

    /** @return true if back press was consumed */
    fun performBackPressed(): Boolean {
        if (active) {
            clearSelection()
            return true
        }
        return false
    }

    fun destroy() {
        logInfo { "destroy" }
        clearSelection()
    }

    fun setSelectable(option: Int) {
        if (selectable == option) {
            return
        }
        selectable = option
        if (!enabled) {
            clearSelection()
        }
    }

    /** @param option JSON with "background" and "cursor" color values */
    fun setSelectionColor(option: JSONObject) {
        val background = option.optLong("background", -1L)
        val cursor = option.optLong("cursor", -1L)
        if (background != -1L && cursor != -1L) {
            selectionPaint.color = background.toInt()
            cursorColor = cursor.toInt()
            if (cursorViewInitialized) {
                cursorStartView.setColor(cursor.toInt())
                cursorEndView.setColor(cursor.toInt())
            }
        }
    }

    fun setSelectStartCallback(callback: KuiklyRenderCallback) {
        selectStartCallback = callback
    }

    fun setSelectChangeCallback(callback: KuiklyRenderCallback) {
        selectChangeCallback = callback
    }

    fun setSelectEndCallback(callback: KuiklyRenderCallback) {
        selectEndCallback = callback
    }

    fun setSelectCancelCallback(callback: KuiklyRenderCallback) {
        selectCancelCallback = callback
    }

    /** @param option JSON with "x", "y" coordinates and optional "type" for selection granularity */
    fun createSelection(option: JSONObject) {
        logInfo { "createSelection: $option" }
        if (active) {
            forEachText { _, _ -> this.clearSelection() }
        }
        val x = kuiklyContext.toPxF(option.optDouble("x").toFloat())
        val y = kuiklyContext.toPxF(option.optDouble("y").toFloat())
        val type = when (option.optInt("type")) {
            SELECTION_TYPE_CHARACTER -> SelectionType.CHARACTER
            SELECTION_TYPE_WORD -> SelectionType.WORD
            SELECTION_TYPE_PARAGRAPH -> SelectionType.PARAGRAPH
            SELECTION_TYPE_SENTENCE -> SelectionType.SENTENCE
            else -> SelectionType.WORD
        }

        val oldLeft = selectionRect.left
        val oldTop = selectionRect.top
        val oldRight = selectionRect.right
        val oldBottom = selectionRect.bottom
        val created = createSelectionInternal(x, y, type)
        if (created) {
            if (!active || (selectionRect.left != oldLeft || selectionRect.top != oldTop ||
                        selectionRect.right != oldRight || selectionRect.bottom != oldBottom)
            ) {
                notifySelectStart()
            }
        } else if (active) {
            notifySelectCancel()
        }
    }

    /**
     * Creates selection at coordinate, iterating views from y-axis to x-axis
     * @return true if selection was created
     */
    private fun createSelectionInternal(x: Float, y: Float, type: SelectionType): Boolean {
        reusableTextViewList.use { list ->
            forEachText { offsetX, offsetY ->
                // check hit, visible and has content
                if (x < offsetX + this.width && x >= offsetX &&
                    y < offsetY + this.height && y >= offsetY &&
                    this.visibility == View.VISIBLE && this.length() > 0
                ) {
                    list.add(Triple(this, offsetX, offsetY))
                }
            }
            logInfo { "collect size=${list.size}" }
            // sort by top-left-coordinate, top to bottom, left to right
            list.sortWith(
                Comparator { (_, x1, y1), (_, x2, y2) -> if (y1 == y2) x1 - x2 else y1 - y2 }
            )
            // Reverse iterate to find hit
            for (i in list.size - 1 downTo 0) {
                val (textView, offsetX, offsetY) = list[i]
                if (textView.setSelectionByCoordinate(this, x - offsetX, y - offsetY, type)) {
                    textView.getStartSelectionEdge().also { (px, pt, pb) ->
                        cursorStartX = px + offsetX
                        cursorStartTop = pt + offsetY
                        cursorStartBottom = pb + offsetY
                    }
                    textView.getEndSelectionEdge().also { (px, pt, pb) ->
                        cursorEndX = px + offsetX
                        cursorEndTop = pt + offsetY
                        cursorEndBottom = pb + offsetY
                    }
                    cursorStartRef = WeakReference(textView)
                    cursorEndRef = WeakReference(textView)
                    logInfo { "position hit i=$i view hash=${textView.hashCode()}" }
                    textView.getSelectionRect(reusableRectF)
                    selectionRect.left = reusableRectF.left + offsetX
                    selectionRect.top = reusableRectF.top + offsetY
                    selectionRect.right = reusableRectF.right + offsetX
                    selectionRect.bottom = reusableRectF.bottom + offsetY
                    return true
                }
            }
            // unfortunately no hit, try to select the closest text view
            findClosestTextView(x, y)?.also { (textView, offsetX, offsetY) ->
                logInfo { "closest view hash=${textView.hashCode()}" }
                if (textView.setSelectionByCoordinate(
                        this,
                        x - offsetX,
                        y - offsetY,
                        type,
                        force = true
                    )
                ) {
                    textView.getStartSelectionEdge().also { (px, pt, pb) ->
                        cursorStartX = px + offsetX
                        cursorStartTop = pt + offsetY
                        cursorStartBottom = pb + offsetY
                    }
                    textView.getEndSelectionEdge().also { (px, pt, pb) ->
                        cursorEndX = px + offsetX
                        cursorEndTop = pt + offsetY
                        cursorEndBottom = pb + offsetY
                    }
                    cursorStartRef = WeakReference(textView)
                    cursorEndRef = WeakReference(textView)
                    textView.getSelectionRect(reusableRectF)
                    selectionRect.left = reusableRectF.left + offsetX
                    selectionRect.top = reusableRectF.top + offsetY
                    selectionRect.right = reusableRectF.right + offsetX
                    selectionRect.bottom = reusableRectF.bottom + offsetY
                    return true
                }
            }
            logInfo { "create selection failed" }
            cursorStartX = Float.NaN
            cursorStartTop = Float.NaN
            cursorStartBottom = Float.NaN
            cursorEndX = Float.NaN
            cursorEndTop = Float.NaN
            cursorEndBottom = Float.NaN
            cursorStartRef = null
            cursorEndRef = null
            return false
        }
    }

    private fun findClosestTextView(x: Float, y: Float): Triple<KRRichTextView, Int, Int>? {
        var distance = Float.MAX_VALUE
        var textView: KRRichTextView? = null
        var offsetX = 0
        var offsetY = 0
        forEachText { oX, oY ->
            if (distance > 0 && visibility == View.VISIBLE && length() > 0) {
                val d = distanceTo(x - oX, y - oY)
                if (d < distance) {
                    distance = d
                    textView = this
                    offsetX = oX
                    offsetY = oY
                }
            }
        }
        return textView?.let { Triple(it, offsetX, offsetY) }
    }

    fun selectAll() {
        logInfo { "select all active=$active" }

        var firstX = Int.MAX_VALUE
        var firstY = Int.MAX_VALUE
        var firstView: KRRichTextView? = null
        var lastX: Int = Int.MIN_VALUE
        var lastY: Int = Int.MIN_VALUE
        var lastView: KRRichTextView? = null

        var selectionLeft = Float.MAX_VALUE
        var selectionTop = Float.MAX_VALUE
        var selectionRight = Float.MIN_VALUE
        var selectionBottom = Float.MIN_VALUE

        forEachText { offsetX, offsetY ->
            if (this.visibility == View.VISIBLE) {
                val hit = setSelectAll(this@KRTextSelector)
                if (hit) {
                    if (offsetY < firstY || (offsetY == firstY && offsetX < firstX)) {
                        firstX = offsetX
                        firstY = offsetY
                        firstView = this
                    }
                    if (offsetY > lastY || (offsetY == lastY && offsetX > lastX)) {
                        lastX = offsetX
                        lastY = offsetY
                        lastView = this
                    }
                    getSelectionRect(reusableRectF)
                    selectionLeft = minOf(selectionLeft, reusableRectF.left + offsetX)
                    selectionTop = minOf(selectionTop, reusableRectF.top + offsetY)
                    selectionRight = maxOf(selectionRight, reusableRectF.right + offsetX)
                    selectionBottom = maxOf(selectionBottom, reusableRectF.bottom + offsetY)
                }
            }
        }
        if (firstView != null && lastView != null) {
            firstView!!.getStartSelectionEdge().also { (px, pt, pb) ->
                cursorStartX = px + firstX
                cursorStartTop = pt + firstY
                cursorStartBottom = pb + firstY
            }
            cursorStartRef = WeakReference(firstView!!)
            lastView!!.getEndSelectionEdge().also { (px, pt, pb) ->
                cursorEndX = px + lastX
                cursorEndTop = pt + lastY
                cursorEndBottom = pb + lastY
            }
            cursorEndRef = WeakReference(lastView!!)
            val oldLeft = selectionRect.left
            val oldTop = selectionRect.top
            val oldRight = selectionRect.right
            val oldBottom = selectionRect.bottom
            selectionRect.left = selectionLeft
            selectionRect.top = selectionTop
            selectionRect.right = selectionRight
            selectionRect.bottom = selectionBottom
            if (!active || (selectionRect.left != oldLeft || selectionRect.top != oldTop ||
                        selectionRect.right != oldRight || selectionRect.bottom != oldBottom)
            ) {
                notifySelectStart()
            }
        }
    }

    /** Updates selection across multiple text views based on two points */
    private fun updateSelection(startX: Float, startY: Float, endX: Float, endY: Float) {
        logInfo { "updateSelectionByCoordinate: ($startX,$startY)-($endX,$endY)" }
        reusableTextViewList.use { list ->
            val minY = minOf(startY, endY)
            val maxY = maxOf(startY, endY)
            forEachText { offsetX, offsetY ->
                // check hit, visible and has content
                if (minY < offsetY + this.height && maxY >= offsetY &&
                    this.visibility == View.VISIBLE && this.length() > 0
                ) {
                    list.add(Triple(this, offsetX, offsetY))
                } else {
                    clearSelection()
                }
            }
            logInfo { "collect size=${list.size}" }
            // sort by top-left-coordinate, top to bottom, left to right
            list.sortWith(
                Comparator { (_, x1, y1), (_, x2, y2) -> if (y1 == y2) x1 - x2 else y1 - y2 }
            )

            // step 1 find startIndex and endIndex
            val size = list.size
            var distance1 = Float.MAX_VALUE
            var distance2 = Float.MAX_VALUE
            var index1 = -1
            var index2 = -1
            for (i in 0 until size) {
                val (textView, offsetX, offsetY) = list[i]
                textView.distanceTo(
                    startX - offsetX,
                    startY - offsetY
                ).also {
                    if (it < distance1) {
                        distance1 = it
                        index1 = i
                    }
                }
                textView.distanceTo(
                    endX - offsetX,
                    endY - offsetY
                ).also {
                    if (it < distance2) {
                        distance2 = it
                        index2 = i
                    }
                }
                if (distance1 == 0f && distance2 == 0f) {
                    break
                }
            }
            val startIndex = minOf(index1, index2)
            val endIndex = maxOf(index1, index2)
            val reverse = index1 > index2
            // step 2 update selection in range
            var selectionLeft = Float.MAX_VALUE
            var selectionTop = Float.MAX_VALUE
            var selectionRight = Float.MIN_VALUE
            var selectionBottom = Float.MIN_VALUE
            var currentHitIndex = -1
            if (startIndex != -1 && endIndex != -1) {
                for (i in 0 until size) {
                    val (textView, offsetX, offsetY) = list[i]
                    val hit = if (i < startIndex || i > endIndex) {
                        textView.clearSelection()
                        false
                    } else if (i > startIndex && i < endIndex) {
                        textView.setSelectAll(this)
                    } else {
                        val x1: Float
                        val y1: Float
                        if (i == startIndex) {
                            x1 = (if (reverse) endX else startX) - offsetX
                            y1 = (if (reverse) endY else startY) - offsetY
                        } else {
                            x1 = 0f
                            y1 = 0f
                        }
                        val x2: Float
                        val y2: Float
                        if (i == endIndex) {
                            x2 = (if (reverse) startX else endX) - offsetX
                            y2 = (if (reverse) startY else endY) - offsetY
                        } else {
                            x2 = textView.width.toFloat()
                            y2 = textView.height.toFloat()
                        }
                        val force = currentHitIndex == -1 && i == endIndex
                        textView.setSelectionByCoordinates(this, x1, y1, x2, y2, force)
                    }
                    if (hit) {
                        if (currentHitIndex == -1) {
                            // found first selection
                            logInfo { "start hit i=$i view hash=${textView.hashCode()}" }
                            textView.getStartSelectionEdge().also { (px, pt, pb) ->
                                cursorStartX = px + offsetX
                                cursorStartTop = pt + offsetY
                                cursorStartBottom = pb + offsetY
                            }
                            cursorStartRef = WeakReference(textView)
                        }
                        textView.getSelectionRect(reusableRectF)
                        selectionLeft = minOf(selectionLeft, reusableRectF.left + offsetX)
                        selectionTop = minOf(selectionTop, reusableRectF.top + offsetY)
                        selectionRight = maxOf(selectionRight, reusableRectF.right + offsetX)
                        selectionBottom = maxOf(selectionBottom, reusableRectF.bottom + offsetY)
                        currentHitIndex = i
                    }
                }
            }
            if (currentHitIndex == -1) {
                // this should never happen, but just in case, clear selection
                logInfo { "updateSelection empty" }
                handler.post { clearSelection() } // post to avoid cancel during touchMove
                return
            }
            val (lastTextView, lastOffsetX, lastOffsetY) = list[currentHitIndex]
            logInfo { "end hit i=$currentHitIndex view hash=${lastTextView.hashCode()}" }
            lastTextView.getEndSelectionEdge().also { (px, pt, pb) ->
                cursorEndX = px + lastOffsetX
                cursorEndTop = pt + lastOffsetY
                cursorEndBottom = pb + lastOffsetY
            }
            cursorEndRef = WeakReference(lastTextView)
            selectionRect.left = selectionLeft
            selectionRect.top = selectionTop
            selectionRect.right = selectionRight
            selectionRect.bottom = selectionBottom
        }
    }

    /** Generates callback params with bounds in dp */
    private fun generateSelectEventParam() = mapOf(
        "x" to kuiklyContext.toDpF(selectionRect.left),
        "y" to kuiklyContext.toDpF(selectionRect.top),
        "width" to kuiklyContext.toDpF(selectionRect.width()),
        "height" to kuiklyContext.toDpF(selectionRect.height())
    )

    private fun notifySelectStart() {
        active = true
        selectStartCallback?.invoke(generateSelectEventParam())
        updateSelectionHandles()
    }

    private fun notifySelectChange() {
        selectChangeCallback?.invoke(generateSelectEventParam())
        updateSelectionHandles()
    }

    private fun notifySelectEnd() {
        selectEndCallback?.invoke(generateSelectEventParam())
        updateSelectionHandles()
    }

    private fun notifySelectCancel() {
        active = false
        selectCancelCallback?.invoke(null)
        cursorStartView.dismiss()
        cursorEndView.dismiss()
    }

    /** Gets selected text from all views in reading order */
    fun getSelection(callback: KuiklyRenderCallback) {
        reusableTextViewList.use { list ->
            forEachText { offsetX, offsetY ->
                if (this.visibility == View.VISIBLE && this.length() > 0) {
                    list.add(Triple(this, offsetX, offsetY))
                }
            }
            // sort by top-left-coordinate, top to bottom, left to right
            list.sortWith(
                Comparator { (_, x1, y1), (_, x2, y2) -> if (y1 == y2) x1 - x2 else y1 - y2 }
            )
            var startIndex = -1
            var endIndex = -1
            val content = mutableListOf<String>()
            val preContent = mutableListOf<String>()
            val postContent = mutableListOf<String>()
            list.forEachIndexed { index, (view) ->
                if (view.hasSelection()) {
                    if (startIndex == -1) {
                        startIndex = index
                    }
                    endIndex = index
                    content.add(view.getSelectionText())
                }
            }
            if (startIndex != -1) {
                if (startIndex > 0) {
                    preContent.add(list[startIndex - 1].first.getText())
                }
                preContent.add(list[startIndex].first.getPreSelectionText())
            }
            if (endIndex != -1) {
                postContent.add(list[endIndex].first.getPostSelectionText())
                if (endIndex < list.size - 1) {
                    postContent.add(list[endIndex + 1].first.getText())
                }
            }
            callback(mapOf(
                "content" to content,
                "preContent" to preContent,
                "postContent" to postContent
            ))
        }
    }

    fun clearSelection() {
        logInfo { "clearSelection active=$active" }
        if (active) {
            cursorStartX = Float.NaN
            cursorStartTop = Float.NaN
            cursorStartBottom = Float.NaN
            cursorEndX = Float.NaN
            cursorEndTop = Float.NaN
            cursorEndBottom = Float.NaN
            cursorStartRef = null
            cursorEndRef = null
            forEachText { _, _ -> clearSelection() }
            notifySelectCancel()
        }
    }

    /** Called by KRRichTextView to draw selection highlight */
    fun drawSelection(textView: KRRichTextView, canvas: Canvas) {
        if (!active) {
            return
        }
        if (textView.getSelectionPath(reusablePath)) {
            canvas.drawPath(reusablePath, selectionPaint)
        }
    }

    private fun updateSelectionHandles() {
        assert(active)
        val flag = view.getLocalVisibleRect(reusableRect)
        if (flag) {
            reusableRect.inset(-1, -1) // include edge
        }
        val visibleStart = flag &&
                reusableRect.contains(cursorStartX.toInt(), cursorStartBottom.toInt()) &&
                !draggingCursorStart()
        val visibleEnd = flag &&
                reusableRect.contains(cursorEndX.toInt(), cursorEndBottom.toInt()) &&
                !draggingCursorEnd()
        if (visibleStart || visibleEnd) {
            view.getLocationInWindow(reusableIntArray)
        }
        if (!visibleStart) {
            cursorStartView.hide()
        } else {
            cursorStartView.show(
                reusableIntArray[0] + cursorStartX,
                reusableIntArray[1] + cursorStartBottom
            )
        }
        if (!visibleEnd) {
            cursorEndView.hide()
        } else {
            cursorEndView.show(
                reusableIntArray[0] + cursorEndX,
                reusableIntArray[1] + cursorEndBottom
            )
        }
    }

    private fun subscribeSelectionHandleUpdate() {
        view.viewTreeObserver.addOnPreDrawListener(this)
        view.viewTreeObserver.addOnScrollChangedListener(this)
        view.addOnLayoutChangeListener(this)
    }

    private fun unsubscribeSelectionHandleUpdate() {
        view.removeOnLayoutChangeListener(this)
        view.viewTreeObserver.removeOnScrollChangedListener(this)
        view.viewTreeObserver.removeOnPreDrawListener(this)
    }

    override fun onPreDraw(): Boolean {
        logInfo { "onPreDraw" }
        if (active) {
            updateSelectionHandles()
        }
        return true
    }

    override fun onScrollChanged() {
        logInfo { "onScrollChanged" }
        if (active) {
            cursorStartRef?.get()?.also {
                val (offsetX, offsetY) = it.relativeTo(view)
                val (px, pt, pb) = it.getStartSelectionEdge()
                cursorStartX = px + offsetX
                cursorStartTop = pt + offsetY
                cursorStartBottom = pb + offsetY
            }
            cursorEndRef?.get()?.also {
                val (offsetX, offsetY) = it.relativeTo(view)
                val (px, pt, pb) = it.getEndSelectionEdge()
                cursorEndX = px + offsetX
                cursorEndTop = pt + offsetY
                cursorEndBottom = pb + offsetY
            }
        }
    }

    override fun onLayoutChange(
        v: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        if ((bottom - top != oldBottom - oldTop) || (right - left != oldRight - oldLeft)) {
            logInfo { "layout changed active=$active" }
            if (active) {
                clearSelection()
            }
        }
    }

    /** During drag, cursors may swap roles based on position */
    private fun draggingCursorStart(): Boolean {
        if (dragging) {
            // end cursor at fixed position or even above (means trimmed for line end)
            return dragFixedY > cursorEndY ||
                    (dragFixedY == cursorEndY && dragFixedX == cursorEndX)
        }
        return false
    }

    private fun draggingCursorEnd(): Boolean {
        if (dragging) {
            // start cursor at fixed position or even below (means trimmed for line start)
            return dragFixedY < cursorStartY ||
                    (dragFixedY == cursorStartY && dragFixedX == cursorStartX)
        }
        return false
    }

    fun postInvalidate() {
        logInfo { "postInvalidate active=$active pending=$invalidatePending" }
        if (active && !invalidatePending) {
            invalidatePending = true
            handler.post {
                invalidatePending = false
                if (!active) {
                    return@post
                }
                updateSelection(
                    cursorStartX, (cursorStartTop + cursorStartBottom) / 2f,
                    cursorEndX, (cursorEndTop + cursorEndBottom) / 2f
                )
            }
        }
    }

}

/**
 * A value animator used to animate the magnifier.
 * copied from aosp [android.widget.Editor.MagnifierMotionAnimator]
 */
private class MagnifierMotionAnimator
@RequiresApi(Build.VERSION_CODES.P) constructor(view: View) {
    // The magnifier being animated.
    private val mMagnifier: Magnifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Magnifier.Builder(view).build()
    } else {
        @Suppress("DEPRECATION", "NewApi")
        Magnifier(view)
    }

    // Prepare the animator used to run the motion animation.
    private val mAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)

    // Whether the magnifier is currently visible.
    private var mMagnifierIsShowing = false

    // The coordinates of the magnifier when the currently running animation started.
    private var mAnimationStartX = 0f
    private var mAnimationStartY = 0f

    // The coordinates of the magnifier in the latest animation frame.
    private var mAnimationCurrentX = 0f
    private var mAnimationCurrentY = 0f

    // The latest coordinates the motion animator was asked to #show() the magnifier at.
    private var mLastX = 0f
    private var mLastY = 0f

    init {
        mAnimator.setDuration(DURATION)
        mAnimator.interpolator = LinearInterpolator()
        mAnimator.addUpdateListener(ValueAnimator.AnimatorUpdateListener { animation: ValueAnimator? ->
            // Interpolate to find the current position of the magnifier.
            mAnimationCurrentX =
                (mAnimationStartX + (mLastX - mAnimationStartX) * animation!!.animatedFraction)
            mAnimationCurrentY =
                (mAnimationStartY + (mLastY - mAnimationStartY) * animation.animatedFraction)
            mMagnifier.show(mAnimationCurrentX, mAnimationCurrentY)
        })
    }

    fun updateZoom(zoom: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (zoom != mMagnifier.zoom) {
                mMagnifier.zoom = zoom
            }
        }
    }

    /**
     * Shows the magnifier at a new position.
     * If the y coordinate is different from the previous y coordinate
     * (probably corresponding to a line jump in the text), a short
     * animation is added to the jump.
     */
    fun show(x: Float, y: Float) {
        val startNewAnimation = mMagnifierIsShowing && y != mLastY

        if (startNewAnimation) {
            if (mAnimator.isRunning()) {
                mAnimator.cancel()
                mAnimationStartX = mAnimationCurrentX
                mAnimationStartY = mAnimationCurrentY
            } else {
                mAnimationStartX = mLastX
                mAnimationStartY = mLastY
            }
            mAnimator.start()
        } else {
            if (!mAnimator.isRunning()) {
                @Suppress("NewApi")
                mMagnifier.show(x, y)
            }
        }
        mLastX = x
        mLastY = y
        mMagnifierIsShowing = true
    }

    /**
     * Dismisses the magnifier, or does nothing if it is already dismissed.
     */
    fun dismiss() {
        @Suppress("NewApi")
        mMagnifier.dismiss()
        mAnimator.cancel()
        mMagnifierIsShowing = false
    }

    companion object {
        private const val DURATION: Long = 100 /* miliseconds */
    }
}

/**
 * Selection cursor handle displayed as a draggable popup window.
 */
@SuppressLint("ViewConstructor")
private class SelectionHandleView(
    private val selector: KRTextSelector,
    private val view: KRView,
    private val isStart: Boolean
) : View(view.context) {
    companion object {
        private val setWindowLayoutTypeMethod by lazy(LazyThreadSafetyMode.NONE) {
            try {
                PopupWindow::class.java.getDeclaredMethod(
                    "setWindowLayoutType",
                    Int::class.javaPrimitiveType!!
                ).also {
                    it.isAccessible = true
                }
            } catch (_: Exception) {
                null
            }
        }

        /** Sets window layout type with reflection fallback for older APIs */
        private fun PopupWindow.compatSetWindowLayoutType(layoutType: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                windowLayoutType = layoutType
            } else {
                try {
                    setWindowLayoutTypeMethod?.invoke(this, layoutType)
                } catch (_: Exception) {
                    // ignore
                }
            }
        }

        private fun getAttrDrawable(context: Context, attr: Int): Drawable? {
            val typedValue = TypedValue()
            val found = context.theme.resolveAttribute(attr, typedValue, true)
            return if (found && typedValue.resourceId != 0) {
                context.theme.getDrawable(typedValue.resourceId).mutate()
            } else {
                null
            }
        }

    }

    private val container = PopupWindow(context, null, android.R.attr.textSelectHandleWindowStyle)
    private val isLeft: Boolean
    private val drawable: Drawable
    private var positionX: Float = Float.NaN
    private var positionY: Float = Float.NaN
    private var touchDownX: Float = Float.NaN
    private var touchDownY: Float = Float.NaN

    init {
        container.isClippingEnabled = false
        container.animationStyle = 0 // no animation
        container.compatSetWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL)
        container.width = ViewGroup.LayoutParams.WRAP_CONTENT
        container.height = ViewGroup.LayoutParams.WRAP_CONTENT
        container.setContentView(this)
        val isRtl = context.resources.configuration.layoutDirection == LAYOUT_DIRECTION_RTL
        isLeft = isStart != isRtl
        drawable = if (isLeft) {
            getAttrDrawable(context, android.R.attr.textSelectHandleLeft)
                ?: FallbackDrawableLeft(view)
        } else {
            getAttrDrawable(context, android.R.attr.textSelectHandleRight)
                ?: FallbackDrawableRight(view)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.rawX
                touchDownY = event.rawY
                selector.handleTouchDown(isStart)
            }

            MotionEvent.ACTION_MOVE -> {
                selector.handleTouchMove(event.rawX - touchDownX, event.rawY - touchDownY)
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                touchDownX = Float.NaN
                touchDownY = Float.NaN
                selector.handleTouchUp()
            }
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(drawable.intrinsicWidth, drawable.intrinsicHeight)
    }

    override fun onDraw(canvas: Canvas) {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
    }

    fun setColor(color: Int) {
        if (drawable is ColorDrawable) {
            drawable.color = color
        } else {
            drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
        invalidate()
    }

    /**
     * Hotspot offset where handle connects to cursor,
     * copied from aosp [android.widget.Editor.SelectionHandleView.getHotspotX]
     */
    private fun hotSpotX(): Int {
        return if (isLeft) {
            drawable.intrinsicWidth * 3 / 4
        } else {
            drawable.intrinsicWidth / 4
        }
    }

    fun show(x: Float, y: Float) {
        visibility = VISIBLE
        if (positionX == x && positionY == y) {
            return
        }
        positionX = x
        positionY = y
        val finalX = x.toInt() - hotSpotX()
        val finalY = y.toInt()
        if (container.isShowing) {
            container.update(finalX, finalY, -1, -1)
        } else {
            container.showAtLocation(view, Gravity.NO_GRAVITY, finalX, finalY)
        }
    }

    fun hide() {
        visibility = INVISIBLE
    }

    fun dismiss() {
        positionX = Float.NaN
        positionY = Float.NaN
        container.dismiss()
    }
}

private abstract class PathDrawable(kuiklyContext: IKuiklyRenderContext?) : ColorDrawable() {

    init {
        color = 0xFF33B5E5.toInt() // default to blue
    }

    protected val cursorHeight = kuiklyContext.toPxI(25f)
    protected val cursorWidth = kuiklyContext.toPxI(10f)
    protected val cursorRadius = kuiklyContext.toPxF(6f)

    abstract val path: Path

    override fun getIntrinsicHeight() = cursorHeight

    override fun getIntrinsicWidth() = cursorWidth * 4 // keep same as system drawable

    override fun draw(canvas: Canvas) {
        canvas.clipPath(path)
        super.draw(canvas)
    }
}

private class FallbackDrawableLeft(view: KRView) : PathDrawable(view.kuiklyRenderContext) {
    override val path: Path = Path().apply {
        moveTo(cursorWidth * 3f, 0f)
        lineTo(cursorWidth * 2f, cursorWidth.toFloat())
        lineTo(cursorWidth * 2f, cursorHeight - cursorRadius)
        arcTo(
            cursorWidth * 2f,
            cursorHeight - cursorRadius * 2,
            cursorWidth * 2f + cursorRadius * 2,
            cursorHeight.toFloat(),
            180f,
            -90f,
            false
        )
        lineTo(cursorWidth * 3f, cursorHeight.toFloat())
        close()
    }
}

private class FallbackDrawableRight(view: KRView) : PathDrawable(view.kuiklyRenderContext) {
    override val path: Path = Path().apply {
        moveTo(cursorWidth.toFloat(), 0f)
        lineTo(cursorWidth * 2f, cursorWidth.toFloat())
        lineTo(cursorWidth * 2f, cursorHeight - cursorRadius)
        arcTo(
            cursorWidth * 2f - cursorRadius * 2,
            cursorHeight - cursorRadius * 2,
            cursorWidth * 2f,
            cursorHeight.toFloat(),
            0f,
            90f,
            false
        )
        lineTo(cursorWidth.toFloat(), cursorHeight.toFloat())
        close()
    }
}
