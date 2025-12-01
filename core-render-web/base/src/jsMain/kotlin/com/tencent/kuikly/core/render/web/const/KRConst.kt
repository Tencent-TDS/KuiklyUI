package com.tencent.kuikly.core.render.web.const

object KRViewConst {
    const val WIDTH = "width"
    const val HEIGHT = "height"
    const val X = "x"
    const val Y = "y"
    const val PI_AS_ANGLE = 180f
    const val ROUND_ANGLE = 360f
}

object KRExtConst {
    const val FIRST_ARG_INDEX = 0
    const val SECOND_ARG_INDEX = 1
    const val THIRD_ARG_INDEX = 2
    const val FOURTH_ARG_INDEX = 3
    const val FIFTH_ARG_INDEX = 4
    const val SIXTH_ARG_INDEX = 5
    const val ROUND_SCALE_VALUE = 0.5f
}

object KRCssConst {
    const val TEXT_SHADOW = "textShadow"
    const val STROKE_WIDTH = "strokeWidth"
    const val STROKE_COLOR = "strokeColor"
    const val MASK_LINEAR_GRADIENT = "maskLinearGradient"
    const val COLOR = "color"
    const val TRANSFORM_LAZY_BLOCK = "transformLazyBlock"
    // Touch down event
    const val TOUCH_DOWN = "touchDown"
    // Touch up event
    const val TOUCH_UP = "touchUp"
    // Touch move event
    const val TOUCH_MOVE = "touchMove"
    const val OPACITY = "opacity"
    const val VISIBILITY = "visibility"
    const val OVERFLOW = "overflow"
    const val BACKGROUND_COLOR = "backgroundColor"
    const val BACKGROUND_COLOR_ATTR = "background-color"
    const val TOUCH_ENABLE = "touchEnable"
    const val TRANSFORM = "transform"
    const val BACKGROUND_IMAGE = "backgroundImage"
    const val BOX_SHADOW = "boxShadow"
    const val BORDER_RADIUS = "borderRadius"
    const val BORDER = "border"
    const val CLICK = "click"
    const val PRE_CLICK = "preClick"
    const val DOUBLE_CLICK = "doubleClick"
    const val LONG_PRESS = "longPress"
    const val FRAME = "frame"
    const val Z_INDEX = "zIndex"
    const val PAN = "pan"
    const val SUPER_TOUCH = "superTouch"
    const val ANIMATION = "animation"
    const val ANIMATION_QUEUE = "animationQueue"
    const val ANIMATION_COMPLETION_BLOCK = "animationCompletion"
    const val KUIKLY_ANIMATION = "kuiklyAnimation"
    const val VIEW_DECORATOR = "viewDecorator"
    const val BLANK_SEPARATOR = " "
    const val EMPTY_STRING = ""
    const val ON_SET_FRAME_BLOCK_OBSERVERS = "onSetFrameBlockObservers"
    const val HAD_SET_FRAME = "hadSetFrame"
    const val ACCESSIBILITY = "accessibility"
    const val ACCESSIBILITY_ROLE = "accessibilityRole"

    // Same as Attr.StyleConst.DEBUG_NAME
    const val DEBUG_NAME = "debugName"
    const val AUTO_DARK_ENABLE = "autoDarkEnable"
    const val TURBO_DISPLAY_AUTO_UPDATE_ENABLE = "turboDisplayAutoUpdateEnable"
    const val SCROLL_INDEX = "scrollIndex"
    
    // Frame related attributes
    val FRAME_ATTRS = listOf("width", "height", "left", "top")
}

/**
 * DOM Event name constants
 */
object KREventConst {
    // Touch events
    const val TOUCH_START = "touchstart"
    const val TOUCH_END = "touchend"
    const val TOUCH_MOVE = "touchmove"
    const val TOUCH_CANCEL = "touchcancel"

    // Mouse events
    const val MOUSE_DOWN = "mousedown"
    const val MOUSE_UP = "mouseup"
    const val MOUSE_MOVE = "mousemove"
    const val MOUSE_LEAVE = "mouseleave"

    // Other events
    const val SCROLL = "scroll"
    const val WHEEL = "wheel"
    const val CLICK = "click"
    const val SELECT_START = "selectstart"
    const val DRAG_START = "dragstart"
    const val TRANSITION_END = "transitionend"
    const val INPUT = "input"
    const val FOCUS = "focus"
    const val BLUR = "blur"
    const val KEYDOWN = "keydown"
    const val COMPOSITION_START = "compositionstart"
    const val COMPOSITION_END = "compositionend"
    const val BEFORE_INPUT = "beforeinput"
    const val LOAD = "load"
    const val ERROR = "error"
}

/**
 * Event parameter key constants
 */
object KRParamConst {
    // Position keys
    const val X = "x"
    const val Y = "y"
    const val PAGE_X = "pageX"
    const val PAGE_Y = "pageY"

    // State keys
    const val STATE = "state"
    const val POINTER_ID = "pointerId"
    const val HASH = "hash"
    const val TIMESTAMP = "timestamp"
    const val ACTION = "action"
    const val TOUCHES = "touches"
    const val CONSUMED = "consumed"

    // Offset keys
    const val OFFSET_X = "offsetX"
    const val OFFSET_Y = "offsetY"
    const val VIEW_WIDTH = "viewWidth"
    const val VIEW_HEIGHT = "viewHeight"
    const val CONTENT_WIDTH = "contentWidth"
    const val CONTENT_HEIGHT = "contentHeight"
    const val IS_DRAGGING = "isDragging"

    // Text field keys
    const val TEXT = "text"
    const val CURSOR_INDEX = "cursorIndex"
}

/**
 * State value constants
 */
object KRStateConst {
    const val START = "start"
    const val MOVE = "move"
    const val END = "end"
    const val CANCEL = "cancel"
}

/**
 * Touch action value constants
 */
object KRActionConst {
    const val TOUCH_DOWN = "touchDown"
    const val TOUCH_MOVE = "touchMove"
    const val TOUCH_UP = "touchUp"
    const val TOUCH_CANCEL = "touchCancel"
}

/**
 * CSS style value constants
 */
object KRStyleConst {
    // Position
    const val POSITION_ABSOLUTE = "absolute"

    // Visibility
    const val VISIBILITY_HIDDEN = "hidden"
    const val VISIBILITY_VISIBLE = "visible"

    // Overflow
    const val OVERFLOW_HIDDEN = "hidden"
    const val OVERFLOW_VISIBLE = "visible"
    const val OVERFLOW_SCROLL = "scroll"

    // Pointer events
    const val POINTER_EVENTS_NONE = "none"
    const val POINTER_EVENTS_AUTO = "auto"

    // Box sizing
    const val BOX_SIZING_BORDER_BOX = "border-box"

    // Display
    const val DISPLAY_NONE = "none"
    const val DISPLAY_BLOCK = "block"

    // Border
    const val BORDER_NONE = "none"

    // Background
    const val BG_TRANSPARENT = "transparent"

    // Unit suffix
    const val PX_SUFFIX = "px"
    const val MS_SUFFIX = "ms"

    // Timing functions
    const val EASE_IN = "ease-in"
}

/**
 * HTML tag name constants
 */
object KRTagConst {
    const val SPAN = "span"
    const val P = "p"
    const val STYLE = "style"
}

/**
 * JS type value constants
 */
object KRJsTypeConst {
    const val UNDEFINED = "undefined"
    const val OBJECT = "object"
    const val STRING = "string"
    const val FUNCTION = "function"
}

/**
 * DOM attribute name constants
 */
object KRAttrConst {
    const val ANIMATION = "animation"
    const val ARIA_LABEL = "aria-label"
    const val TYPE = "type"
    const val TEXT_CSS = "text/css"
    const val PASSIVE = "passive"
    const val DATA_NESTED_SCROLL = "data-nested-scroll"
}

/**
 * Animation related key constants
 */
object KRAnimationConst {
    // View data keys
    const val KUIKLY_ANIMATION_GROUP = "kuiklyAnimationGroup"
    const val IS_BIND_ANIMATION_END_EVENT = "isBindAnimationEndEvent"
    const val IS_REPEAT_ANIMATION = "isRepeatAnimation"
    const val FRAME_ANIMATION_END_COUNT = "frameAnimationEndCount"
    const val FRAME_ANIMATION_REMAIN_COUNT = "frameAnimationRemainCount"
    const val EXPORT_ANIMATION_TIMEOUT_ID = "exportAnimationTimeoutId"

    // Animation step option keys
    const val DURATION = "duration"
    const val DELAY = "delay"
    const val TIMING_FUNCTION = "timingFunction"
    const val TRANSFORM_ORIGIN = "transformOrigin"

    // Animation data keys
    const val RULES = "rules"
    const val OLD_VALUE = "oldValue"
    const val NEW_VALUE = "newValue"
    const val TRANSITION = "transition"

    // Animation completion callback keys
    const val FINISH = "finish"
    const val ATTR = "attr"
    const val ANIMATION_KEY = "animationKey"

    // Timing constants (in milliseconds)
    const val STYLE_ANIMATION_RESTART_DELAY_MS = 50
    const val ANIMATION_EXPORT_DELAY_MS = 10
    const val REPEAT_ANIMATION_DELAY_MS = 10
    const val DOUBLE_CLICK_TIMEOUT_MS = 200
}

/**
 * Input type constants
 */
object KRInputTypeConst {
    const val PASSWORD = "password"
    const val NUMBER = "number"
    const val EMAIL = "email"
    const val TEXT = "text"
    const val INSERT_TEXT = "insertText"
    const val DELETE_BACKWARD = "deleteContentBackward"
}

/**
 * Keyboard constants
 */
object KRKeyboardConst {
    const val KEY_ENTER = "Enter"
    const val ENTER_KEY_CODE = 13

    // Return key types
    const val RETURN_KEY_SEARCH = "search"
    const val RETURN_KEY_SEND = "send"
    const val RETURN_KEY_DONE = "done"
    const val RETURN_KEY_GO = "go"
    const val RETURN_KEY_NEXT = "next"
}

/**
 * List view constants
 */
object KRListConst {
    // Scroll directions
    const val SCROLL_DIRECTION_COLUMN = "column"
    const val SCROLL_DIRECTION_ROW = "row"
    const val SCROLL_DIRECTION_NONE = "none"

    // CSS class names
    const val IS_LIST = "isList"
    const val NO_SCROLL_BAR_CLASS = "list-no-scrollbar"
    const val PAGE_LIST_CLASS = "pageList"

    // Timeout durations (in milliseconds)
    const val SCROLL_END_OVERTIME = 200
    const val BOUND_BACK_DURATION = 250L
    const val CLICK_DETECTION_TIMEOUT_TOUCH = 300
    const val CLICK_DETECTION_TIMEOUT_MOUSE = 200
    const val DOUBLE_CLICK_TIMEOUT = 200
    const val WHEEL_STOP_TIMEOUT = 300
    const val IMMEDIATE_TIMEOUT = 0
    const val PAGING_SCROLL_DELAY = 50L
    const val PAGING_SCROLL_ANIMATION_TIME = 300
    const val WHEEL_RESET_TIMEOUT = 150

    // Threshold constants
    const val SCROLL_THRESHOLD = 8
    const val SCROLL_CAPTURE_THRESHOLD = 2
    const val DOUBLE_CLICK_COUNT = 2
    const val SCROLL_DIRECTION_THRESHOLD = 5.0
    const val WHEEL_DELTA_THRESHOLD = 2

    // Boolean flag values
    const val ENABLED_FLAG = 1
    const val ANIMATE_FLAG = "1"

    // Mouse button constants
    const val LEFT_MOUSE_BUTTON: Short = 0

    // Pull-to-refresh transform pattern
    const val REFRESH_CHILD_TRANSFORM = "translate(0%, -100%) rotate(0deg) scale(1, 1) skew(0deg, 0deg)"

    // Transform templates
    const val TRANSFORM_RESET = "translate(0, 0)"

    // Media query constants
    const val POINTER_COARSE_QUERY = "(pointer: coarse)"
    const val POINTER_FINE_QUERY = "(pointer: fine)"

    // Nested scroll
    const val ATTR_VALUE_TRUE = "true"
    const val NESTED_SCROLL_SELECTOR = "[data-nested-scroll=\"true\"]"
    const val EVENT_NESTED_SCROLL_TO_PARENT = "nestedScrollToParent"
    const val EVENT_NESTED_SCROLL_TO_CHILD = "nestedScrollToChild"
    const val DETAIL_DELTA_X = "deltaX"
    const val DETAIL_DELTA_Y = "deltaY"
    const val JSON_KEY_FORWARD = "forward"
    const val JSON_KEY_BACKWARD = "backward"

    // Drag states
    const val DRAG_STATE_IDLE = 0
    const val DRAG_STATE_DRAGGING = 1

    // Inertia scroll constants
    const val INERTIA_FRICTION = 0.95
    const val MIN_VELOCITY_THRESHOLD = 0.5
    const val FRAME_DURATION_MS = 16
    const val MIN_SCROLL_POSITION = 0.0
    const val VELOCITY_ZERO = 0.0
}

/**
 * Placeholder color constants
 */
object KRPlaceholderConst {
    const val CLASS_PREFIX = "phcolor_"
    const val MAX_RANDOM = 1_000_000
}
