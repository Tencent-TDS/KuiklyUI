## 1. Theme Setup

- [x] 1.1 Add `MyDialogFullScreen` theme to `androidApp/src/main/res/values/styles.xml`
  - Set `windowIsFloating=false`
  - Set `windowBackground` to transparent
  - Disable `backgroundDimEnabled`

## 2. ModalView Implementation

- [x] 2.1 Create `MyModalView` class extending `KRView`
  - Add `didMoveToWindow` flag to prevent duplicate Dialog creation
  - Implement `onAddToParent` to remove self from parent and call `setupDialog()`
  - Implement `onDestroy` to dismiss and clear Dialog reference
- [x] 2.2 Implement `setupDialog()` method
  - Resolve Activity from Context via `findActivity()`
  - Create Dialog with `R.style.MyDialogFullScreen`
  - Call `setContentView(this)` after removing from original parent
  - Set `setCancelable(false)` and `setCanceledOnTouchOutside(false)`
- [x] 2.3 Configure Dialog Window for full-screen
  - Clear `FLAG_LAYOUT_INSET_DECOR` and `FLAG_DIM_BEHIND`
  - Add `FLAG_LAYOUT_IN_SCREEN`, `FLAG_LAYOUT_NO_LIMITS`, `FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS`
  - Set immersive `systemUiVisibility` (stable + hide navigation + fullscreen)
  - Set `statusBarColor=0` and `navigationBarColor=0`
  - Clear `DecorView` padding and background
- [x] 2.4 Add notch/cutout support (API 28+)
  - Set `layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`
  - Set `width/height = MATCH_PARENT`, `gravity = FILL`, margins = 0

## 3. Testing

- [x] 3.1 Verify full-screen coverage on standard Android device (no notch)
  - Run `dumpsys window windows` and confirm `Requested w/h` equals display size
- [x] 3.2 Verify full-screen coverage on notch device (e.g., Huawei)
  - Confirm Dialog extends into cutout area (height not truncated)
- [x] 3.3 Verify accessibility isolation
  - Enable TalkBack, confirm swipe navigation is confined to modal content
  - Confirm underlying Activity elements are not focusable
- [x] 3.4 Verify lifecycle
  - Confirm Dialog dismisses when ModalView is destroyed
  - Confirm no memory leaks after repeated show/hide

## 4. Documentation

- [x] 4.1 Update commit message to follow Angular Convention
- [x] 4.2 Complete OpenSpec artifacts (proposal, design, specs, tasks)
