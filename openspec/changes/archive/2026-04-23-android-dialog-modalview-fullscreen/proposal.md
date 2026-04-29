## Why

The current `addContentView`-based ModalView implementation cannot properly block accessibility focus on underlying content, causing screen readers to read elements beneath the modal. A `Dialog`-based approach is needed to create a separate Window with proper accessibility isolation, but the Dialog must display as true full-screen (including status bar and notch areas) to match the visual coverage of the original implementation.

## What Changes

- Add `MyModalView` custom view extending `KRView` that creates a full-screen `Dialog` on `onAddToParent`
- Add `MyDialogFullScreen` theme in `styles.xml` with `windowIsFloating=false` to remove default Dialog margins
- Clear `FLAG_LAYOUT_INSET_DECOR` and set immersive `systemUiVisibility` to allow content behind status bar
- Set `layoutInDisplayCutoutMode=ALWAYS` (API 28+) to prevent height truncation on notch devices (e.g., Huawei loses 120px without this)
- Remove view from parent before attaching to Dialog to avoid "IllegalStateException: The specified child already has a parent"
- Dialog dismisses automatically on `onDestroy`

## Capabilities

### New Capabilities
- `android-dialog-modalview`: Full-screen Dialog-based modal container for Android render layer with accessibility isolation and notch support

### Modified Capabilities
- (none)

## Impact

- **Module**: `androidApp/` (demo module)
- **Platform**: Android only
- **Files modified**:
  - `androidApp/src/main/java/com/tencent/kuikly/android/demo/MyModalView.kt`
  - `androidApp/src/main/res/values/styles.xml`
- **API changes**: None (demo-only implementation)
- **Breaking changes**: None
