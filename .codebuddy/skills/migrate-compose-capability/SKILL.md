
---
name: migrate-compose-capability
description: 用于将官方 Jetpack Compose / Compose Multiplatform 的组件和能力迁移到 KuiklyCompose 框架。当用户需要迁移官方 Compose 组件、API、布局、动画等能力到 KuiklyCompose 时使用。该 Skill 提供包名映射、代码放置规则、平台适配策略、mock 模式及完整迁移模板。
---

# Migrate Compose Capability to KuiklyCompose

## 1. Overview

This skill guides how to migrate official Jetpack Compose / Compose Multiplatform components, APIs, and capabilities to the KuiklyCompose framework. Migrated code should run under KuiklyCompose's cross-platform architecture, covering Android, iOS, HarmonyOS, H5, and WeChat Mini Programs.

---

## 2. KuiklyCompose vs Official Compose

### 2.1 Core Architecture Differences

| Dimension | KuiklyCompose | Official Compose Multiplatform |
|-----------|--------------|-------------------------------|
| **Rendering** | Native rendering (via Kuikly Render layer bridging to platform-native UI components) | Skia self-rendering |
| **Compose Runtime** | Reuses official `androidx.compose.runtime` (Compiler + Runtime unchanged) | Same |
| **UI Layer** | Based on Jetpack Compose 1.7.3 source with package rename from `androidx.compose` to `com.tencent.kuikly.compose` | Official package `androidx.compose` |
| **Node Tree** | `KNode<DeclarativeBaseView>` → maps to Kuikly native view tree | `LayoutNode` → Skia Canvas drawing |
| **Layout Engine** | Reuses Compose Measure/Layout flow, syncs layout info to native views via `KNode` | Compose internal layout & draw |
| **Dynamic Updates** | Supports hot updates and dynamic delivery | Not supported |
| **Cross-platform** | Android/iOS/HarmonyOS/H5/WeChat Mini Programs | Android/iOS/Desktop/H5 |

### 2.2 Key Bridging Mechanism

KuiklyCompose core bridging layer:

```
Compose DSL (@Composable)
    ↓
Compose Runtime (Recomposition, State)
    ↓
KuiklyApplier (AbstractApplier<KNode>)
    ↓
KNode<DeclarativeBaseView> (Node Tree)
    ↓
Kuikly Core Render (Platform Native UI)
```

- **`KuiklyApplier`**: Extends `AbstractApplier`, maps Compose tree operations (insert, remove, move) to `KNode` tree
- **`KNode`**: Wraps `DeclarativeBaseView` (Kuikly's native view base class), bridges Compose nodes to Kuikly native views
- **`MakeKuiklyComposeNode`**: Core utility function for creating `ReusableComposeNode` that connects Compose to Kuikly native views
- **`ComposeContainer`**: Page base class, extends `Pager`, initializes `ComposeScene`, `ComposeSceneMediator`, etc.

### 2.3 Package Name Mapping Rules

All official Compose package names must be converted as follows:

| Official Package | KuiklyCompose Package |
|-----------------|----------------------|
| `androidx.compose.ui.*` | `com.tencent.kuikly.compose.ui.*` |
| `androidx.compose.foundation.*` | `com.tencent.kuikly.compose.foundation.*` |
| `androidx.compose.material3.*` | `com.tencent.kuikly.compose.material3.*` |
| `androidx.compose.material.*` | `com.tencent.kuikly.compose.material3.*` (merged into material3) |
| `androidx.compose.animation.*` | `com.tencent.kuikly.compose.animation.*` |
| `androidx.compose.runtime.*` | `androidx.compose.runtime.*` (**UNCHANGED!**) |

> **IMPORTANT**: `androidx.compose.runtime` package name stays unchanged because KuiklyCompose directly reuses the official Compose Runtime.

---

## 3. Project Structure & Code Placement Rules

### 3.1 Directory Structure

```
compose/
├── src/
│   ├── commonMain/          # ⭐ Cross-platform shared code (preferred location)
│   │   └── kotlin/com/tencent/kuikly/compose/
│   │       ├── foundation/  # Foundation components (Box, Column, Row, LazyList, etc.)
│   │       ├── material3/   # Material3 components (Scaffold, Surface, Button, etc.)
│   │       ├── animation/   # Animation system
│   │       ├── ui/          # UI core (Modifier, Layout, Node, etc.)
│   │       ├── extension/   # Kuikly bridging extensions (MakeKuiklyNode, etc.)
│   │       ├── container/   # Container related (SuperTouchManager, SlotProvider, etc.)
│   │       └── views/       # Kuikly native view wrappers
│   ├── androidMain/         # Android platform-specific implementations
│   ├── nativeMain/          # iOS and HarmonyOS platform-specific implementations
│   └── jsMain/              # H5 and Mini Program platform-specific implementations
```

### 3.2 Code Placement Principles

**Core Principle: Code should be placed in `commonMain` as much as possible, NOT in `androidMain`.**

| Scenario | Location | Notes |
|----------|----------|-------|
| Pure Compose UI components | `commonMain` | Use KuiklyCompose Composable APIs |
| Compose Runtime API usage | `commonMain` | `@Composable`, `remember`, `mutableStateOf`, etc. |
| KuiklyCompose Modifier usage | `commonMain` | All Modifiers implemented in commonMain |
| Android-specific APIs (`Context`, `Activity`) | `androidMain` + mock | Define `expect` in commonMain, `actual` in each platform; or use mock pattern |
| `ViewModel`, `LiveData` and other AndroidX libs | `androidMain` + mock | Provide platform-agnostic abstraction in commonMain |
| Platform-specific rendering (e.g., Android Canvas) | Per-platform `*Main` | Via `expect/actual` or interface abstraction |

### 3.3 Mock Strategies (when androidMain is unavoidable)

If the migrated capability depends on Android-specific APIs, use these mock patterns:

**Pattern 1: expect/actual**
```kotlin
// commonMain
expect class PlatformContext

expect fun getPlatformName(): String

// androidMain
actual class PlatformContext(val context: android.content.Context)

actual fun getPlatformName(): String = "Android"

// nativeMain (iOS/HarmonyOS)
actual class PlatformContext

actual fun getPlatformName(): String = "Native"
```

**Pattern 2: Interface abstraction + CompositionLocal injection**
```kotlin
// commonMain
interface ClipboardManager {
    fun setText(text: String)
    fun getText(): String?
}

val LocalClipboardManager = compositionLocalOf<ClipboardManager> {
    error("No ClipboardManager provided")
}

// androidMain
class AndroidClipboardManager(context: Context) : ClipboardManager {
    // actual implementation
}

// other platforms
class MockClipboardManager : ClipboardManager {
    override fun setText(text: String) { /* mock */ }
    override fun getText(): String? = null
}
```

**Pattern 3: Optional feature degradation**
```kotlin
// commonMain
@Composable
fun MyComponent() {
    // Core UI in commonMain
    Column {
        Text("Hello")
        // Platform-specific features via optional CompositionLocal injection
        val hapticFeedback = LocalHapticFeedback.current
        Button(onClick = { hapticFeedback?.performHapticFeedback() }) {
            Text("Click")
        }
    }
}
```

---

## 4. Migration Steps (Referencing Scaffold Implementation)

### 4.1 Analyze Official Source Code

Using `Scaffold` as an example, official `androidx.compose.material3.Scaffold` core implementation:

1. **Function signature**: Accepts `topBar`, `bottomBar`, `snackbarHost`, `floatingActionButton`, `content`, etc.
2. **Internal usage**: `Surface` (background/content color), `SubcomposeLayout` (sub-composition for measuring regions)
3. **Dependencies**: `Surface`, `WindowInsets`, `PaddingValues`, `SubcomposeLayout`

### 4.2 Migration Flow

#### Step 1: Define Migration Scope

- List all Composable functions and their public APIs to migrate
- List dependent internal components and utility classes
- Confirm which dependencies already exist in KuiklyCompose

#### Step 2: Package Name Replacement

Replace all `androidx.compose.xxx` with `com.tencent.kuikly.compose.xxx`, **but keep `androidx.compose.runtime`**.

```kotlin
// ❌ Official original
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable

// ✅ After migration
import com.tencent.kuikly.compose.material3.Scaffold   // package changed
import com.tencent.kuikly.compose.foundation.layout.PaddingValues  // package changed
import com.tencent.kuikly.compose.ui.Modifier           // package changed
import androidx.compose.runtime.Composable               // ⚠️ UNCHANGED!
```

#### Step 3: Adapt to Kuikly Rendering Layer

KuiklyCompose uses native rendering; some Compose features may need adaptation:

**Fully supported capabilities (direct migration):**
- Compose state management (`remember`, `mutableStateOf`, `derivedStateOf`, etc.)
- Compose animation system (`animate*AsState`, `AnimatedVisibility`, `Transition`, etc.)
- Compose gesture system (`pointerInput`, `detectTapGestures`, etc.)
- Layout system (`Layout`, `SubcomposeLayout`, `MeasurePolicy`, etc.)
- Modifier chaining
- `CompositionLocal`

**Capabilities requiring special handling:**
- `Canvas` drawing → KuiklyCompose has its own Canvas adaptation
- `graphicsLayer` → Some properties mapped via native view properties
- `Ripple` effect → Not fully supported yet (code has `todo: jonas` comments)
- Platform-specific `WindowInsets` → Obtained via Kuikly's `pageData`

#### Step 4: Handle Platform-Specific Dependencies

If the migrated component depends on platform APIs:

```kotlin
// Example: WindowInsets handling in Scaffold
// Official depends on Android's WindowInsets API
// KuiklyCompose provides cross-platform WindowInsets abstraction

// Already available in commonMain
import com.tencent.kuikly.compose.foundation.layout.WindowInsets
import com.tencent.kuikly.compose.material3.internal.MutableWindowInsets
import com.tencent.kuikly.compose.material3.internal.systemBarsForVisualComponents
```

#### Step 5: Write Code and Place in commonMain

Reference Scaffold's actual migrated code:

```kotlin
// File location: compose/src/commonMain/kotlin/com/tencent/kuikly/compose/material3/Scaffold.kt
package com.tencent.kuikly.compose.material3

// KuiklyCompose package imports
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.WindowInsets
// ... other KuiklyCompose imports

// Compose Runtime imports (keep original package)
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit
) {
    // Use existing KuiklyCompose Surface, SubcomposeLayout, etc.
    val safeInsets = remember(contentWindowInsets) { MutableWindowInsets(contentWindowInsets) }
    Surface(
        modifier = modifier.onConsumedWindowInsetsChanged { consumedWindowInsets ->
            safeInsets.insets = contentWindowInsets.exclude(consumedWindowInsets)
        },
        color = containerColor,
        contentColor = contentColor
    ) {
        ScaffoldLayout(
            fabPosition = floatingActionButtonPosition,
            topBar = topBar,
            bottomBar = bottomBar,
            content = content,
            snackbar = snackbarHost,
            contentWindowInsets = safeInsets,
            fab = floatingActionButton
        )
    }
}
```

#### Step 6: Verify and Test

Create a test page in the demo project:

```kotlin
// demo/src/commonMain/kotlin/.../MyMigratedComponentDemo.kt
@Page("MyComponentDemo")
internal class MyComponentDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("Component Demo") {
                // Use migrated component
                MyMigratedComponent()
            }
        }
    }
}
```

---

## 5. Migration Checklist

Check each item when migrating a component:

- [ ] **Package replacement**: All `androidx.compose.ui/foundation/material3` replaced with `com.tencent.kuikly.compose.*`
- [ ] **Runtime preserved**: `androidx.compose.runtime.*` unchanged
- [ ] **Code location**: Code placed in `commonMain`, not `androidMain`
- [ ] **Platform dependency handling**: Android-specific APIs use expect/actual or mock pattern
- [ ] **Dependency confirmation**: Dependencies already exist in KuiklyCompose
- [ ] **Modifier compatibility**: Using `com.tencent.kuikly.compose.ui.Modifier`
- [ ] **SubcomposeLayout compatibility**: If used, confirmed KuiklyCompose version compatible
- [ ] **Drawing adaptation**: If Canvas drawing involved, use KuiklyCompose's Canvas adapter
- [ ] **Unsupported feature annotation**: Graceful degradation or comment for unsupported features
- [ ] **Demo verification**: Test page created in demo
- [ ] **Multi-platform testing**: Works on at least Android + iOS

---

## 6. Common Migration Scenarios & Patterns

### 6.1 Pure UI Component Migration (most common)

Direct package rename, e.g., `Button`, `Card`, `Divider`:

```kotlin
// Direct use in commonMain
@Composable
fun MyButton(onClick: () -> Unit) {
    com.tencent.kuikly.compose.material3.Button(onClick = onClick) {
        com.tencent.kuikly.compose.material3.Text("Click Me")
    }
}
```

### 6.2 Custom Layout Component Migration

Using `Layout` or `SubcomposeLayout`:

```kotlin
// commonMain
@Composable
fun MyCustomLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    com.tencent.kuikly.compose.ui.layout.Layout(
        content = content,
        modifier = modifier,
        measurePolicy = { measurables, constraints ->
            // Custom measure logic
            val placeables = measurables.map { it.measure(constraints) }
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEach { it.place(0, 0) }
            }
        }
    )
}
```

### 6.3 Native View Bridging (KuiklyCompose specific)

When wrapping Kuikly native views:

```kotlin
// commonMain
@Composable
fun MyNativeView(
    modifier: Modifier = Modifier,
    viewInit: MyKuiklyView.() -> Unit = {},
    viewUpdate: (MyKuiklyView) -> Unit = {}
) {
    MakeKuiklyComposeNode<MyKuiklyView>(
        factory = { MyKuiklyView() },
        modifier = modifier,
        viewInit = viewInit,
        viewUpdate = viewUpdate,
        measurePolicy = KuiklyDefaultMeasurePolicy
    )
}
```

### 6.4 ViewModel Component Migration

ViewModel is Android-specific; abstract it during migration:

```kotlin
// commonMain - define platform-agnostic state container
class MyScreenState {
    var items by mutableStateOf<List<String>>(emptyList())
    var isLoading by mutableStateOf(false)

    fun loadData() {
        isLoading = true
        // load logic
    }
}

@Composable
fun MyScreen() {
    val state = remember { MyScreenState() }
    // Build UI using state
}

// If ViewModel support is truly needed, provide actual implementation in androidMain
```

### 6.5 Navigation Migration

Navigation migration requires special attention as official Compose Navigation depends on Android's NavController:

```kotlin
// commonMain - define cross-platform navigation abstraction
interface Navigator {
    fun navigateTo(route: String)
    fun navigateBack()
    fun getCurrentRoute(): String?
}

val LocalNavigator = compositionLocalOf<Navigator> {
    error("No Navigator provided")
}

// androidMain - use mock or actual implementation
// As shown in current compose-navigation module
```

---

## 7. Migration Notes

### 7.1 DON'Ts

- ❌ Don't import `androidx.compose.ui.*` or other replaced official UI packages (causes conflicts)
- ❌ Don't use Android Context / Activity APIs in commonMain
- ❌ Don't assume Skia Canvas is available (KuiklyCompose uses native rendering)
- ❌ Don't introduce `accompanist` or other Android-only third-party Compose libraries
- ❌ Don't write cross-platform UI code in `androidMain`

### 7.2 DOs

- ✅ Prioritize writing all Compose UI code in commonMain
- ✅ Use `com.tencent.kuikly.compose.*` package names
- ✅ Keep `androidx.compose.runtime.*` package names unchanged
- ✅ Use existing KuiklyCompose components as building blocks
- ✅ Gracefully degrade unsupported features
- ✅ Add `@Composable` annotation to composable functions
- ✅ Use `MakeKuiklyComposeNode` for native view bridging
- ✅ Add test pages in demo

### 7.3 Known Limitations

These official Compose features are not fully supported in KuiklyCompose:

1. **Ripple effect** - Partially commented out (`todo: jonas`)
2. **Some Material3 components** - e.g., `BottomSheet` full interaction
3. **Compose Desktop-specific APIs** - Not applicable
4. **Some graphicsLayer properties** - Native rendering doesn't support all Skia effects
5. **ComposeView embedding in Android View** - KuiklyCompose has its own view embedding mechanism

---

## 8. Reference File Index

| File | Description |
|------|-------------|
| `compose/src/commonMain/.../material3/Scaffold.kt` | Scaffold migration reference |
| `compose/src/commonMain/.../material3/Surface.kt` | Surface migration reference |
| `compose/src/commonMain/.../extension/MakeKuiklyNode.kt` | Native view bridging core utility |
| `compose/src/commonMain/.../ComposeContainer.kt` | Page base class, manages ComposeScene |
| `compose/src/commonMain/.../ComposeSceneMediator.kt` | Compose scene mediator |
| `compose/src/commonMain/.../KuiklyApplier.kt` | Compose tree ops to KNode mapping |
| `compose/src/commonMain/.../ui/layout/Layout.kt` | Layout base component |
| `compose/src/commonMain/.../ui/layout/SubcomposeLayout.kt` | SubcomposeLayout implementation |
| `compose/src/commonMain/.../foundation/layout/Box.kt` | Box layout implementation |
| `demo/src/commonMain/.../compose/ScaffoldDemo.kt` | Scaffold usage example |
| `demo/src/commonMain/.../compose/DemoScaffold.kt` | Demo scaffold wrapper |
| `docs/ComposeDSL/overview.md` | KuiklyCompose overview doc |
| `docs/ComposeDSL/allApi.md` | Supported API list |

---

## 9. Quick Migration Templates

### New Component Migration File Template

```kotlin
/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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

package com.tencent.kuikly.compose.material3  // or other appropriate package

// KuiklyCompose UI imports
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.foundation.layout.*

// Compose Runtime imports (keep original package)
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

/**
 * [ComponentName] - Migrated from official Compose [OriginalComponentName]
 *
 * [Component description]
 *
 * @param modifier [Modifier] modifier
 * @param [other params]
 */
@Composable
fun MyComponent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Build using existing KuiklyCompose components
    Box(modifier = modifier) {
        content()
    }
}
```

### Demo Page Template

```kotlin
package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.core.annotations.Page

@Page("MyComponentDemo")
internal class MyComponentDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("ComponentName Demo") {
                MyComponentDemoContent()
            }
        }
    }
}

@Composable
private fun MyComponentDemoContent() {
    // Demo the migrated component
}
```
