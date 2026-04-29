## Context

The current ModalView demo implementation on Android uses `addContentView` to overlay content on top of the Activity. While this works visually, it fails to block accessibility focus on underlying elements — screen readers (e.g., TalkBack) can still navigate and read content beneath the modal.

A `Dialog`-based ModalView creates a separate `Window` with its own accessibility node tree, which naturally isolates the modal content from the underlying Activity. However, Android `Dialog` has several default behaviors that conflict with full-screen overlay requirements:

1. `windowIsFloating=true` (default in most themes) adds padding and minimum width constraints
2. `FLAG_LAYOUT_INSET_DECOR` causes the Window to avoid system bars (status bar, navigation bar)
3. Notch/display cutout areas are excluded by default on API 28+
4. `show()` resets `DecorView` padding after programmatic changes

This change is Android-only and applies to the demo module (`androidApp/`).

## Goals / Non-Goals

**Goals:**
- Create a Dialog-based ModalView that renders as true full-screen (covering status bar, notch, and navigation bar)
- Ensure accessibility focus is properly isolated within the modal
- Support notch devices (Huawei, etc.) without height truncation
- Clean up Dialog lifecycle automatically on view destruction

**Non-Goals:**
- Changes to iOS, HarmonyOS, Web, or miniApp renderers
- Changes to `core/` or `compose/` modules
- Production-ready framework-level ModalView API (this is demo-only)
- Animation or transition effects for modal show/hide

## Decisions

### Decision: Use Dialog instead of PopupWindow or custom Window
**Rationale**: Dialog provides built-in accessibility isolation (separate accessibility node tree), lifecycle management, and touch-outside/cancel handling. PopupWindow shares the same Window as the Activity and does not isolate accessibility. Custom Window requires manual token management.

**Alternative considered**: `WindowManager.addView()` with `TYPE_APPLICATION_PANEL` — rejected because it requires `SYSTEM_ALERT_WINDOW` permission for reliable full-screen and does not provide accessibility isolation.

### Decision: Custom theme with `windowIsFloating=false`
**Rationale**: The default `Theme_Translucent_NoTitleBar` has `windowIsFloating=true`, which forces a minimum width and adds default margins. A custom theme explicitly disables floating behavior.

**Alternative considered**: Programmatically override theme attributes after creation — rejected because `windowIsFloating` must be set before `Dialog` initializes its internal Window state.

### Decision: Set `layoutInDisplayCutoutMode=ALWAYS` (API 28+)
**Rationale**: Without this, the Dialog Window is constrained to the "safe area" excluding the notch. On a Huawei device with 120px notch, the Dialog height was truncated from 2412px to 2292px. This flag is the only way to extend into the cutout area.

**Alternative considered**: `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` — rejected because it only extends on short edges; `ALWAYS` is required for full coverage regardless of orientation.

### Decision: Remove view from parent before `setContentView`
**Rationale**: A View can only have one parent. Since the view is first added to the Activity's view hierarchy by the renderer, it must be removed before being attached to the Dialog's content view.

## Risks / Trade-offs

- **[Risk]** Dialog creates a separate Window, which may have slightly higher memory overhead than `addContentView` → **Mitigation**: Dialog is dismissed on `onDestroy`, lifecycle is managed
- **[Risk]** `systemUiVisibility` flags are deprecated in API 30+ in favor of `WindowInsetsController` → **Mitigation**: Current implementation uses deprecated flags for broad API compatibility; can migrate when minSdk is raised
- **[Risk]** Some OEMs (e.g., Xiaomi, OPPO) may have custom WindowManager behavior that ignores certain flags → **Mitigation**: `layoutInDisplayCutoutMode` is part of AOSP and generally respected; tested on Huawei

## Migration Plan

N/A — this is a new demo implementation, no migration needed.

## Open Questions

- Should this pattern be promoted to `core-render-android` as a framework-level component?
- How should iOS renderer achieve equivalent accessibility isolation (UIAccessibilityScreenChangedNotification?)
