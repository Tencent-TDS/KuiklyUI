## Core Module

- [x] Add `focus()` method to `DeclarativeBaseView`
- [x] Add `blur()` method to `DeclarativeBaseView`
- [x] Add `VIEW_FOCUS_CHANGE("viewFocusChange")` to `EventName` enum
- [x] Add `onFocusChange(handler: (isFocused: Boolean) -> Unit)` to base `Event` class
- [x] Add `override` to existing `focus()`/`blur()` in InputView, TextAreaView, AutoHeightTextAreaView

## Android Renderer

- [x] Handle `focus` callMethod in `KRView` (set focusable + requestFocus)
- [x] Handle `blur` callMethod in `KRView` (clearFocus)
- [x] Emit `viewFocusChange` event with `{ "isFocused": true/false }` via OnFocusChangeListener

## Demo

- [x] Create `FocusBlurDemoPager.kt` with 3 focusable cards and a "Blur All" button
