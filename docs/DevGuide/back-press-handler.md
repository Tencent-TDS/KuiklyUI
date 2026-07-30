# 接入系统返回键

系统返回键处理是移动端开发中的常见需求。Kuikly 提供了完整的返回键处理机制，本文档介绍如何在业务代码中使用返回键处理功能。

## 概述

要在 Kuikly 中使用返回键处理功能，需要完成以下步骤：

1. **渲染层接入**（前置条件）：在 Android/iOS/OHOS 平台接入系统返回键事件，将事件传递给 Kuikly 框架处理
2. **业务代码使用**：在 Pager 或 Compose DSL 中使用 `BackPressHandler` 或 `BackHandler` 处理返回键逻辑

**重要**：渲染层接入是前置条件，只有完成了渲染层的接入，公共层的 API（`BackPressHandler`、`BackHandler` 等）才能正常工作。

## 渲染层接入步骤

如果你需要在渲染层接入系统返回键事件（例如自定义 Kuikly 的接入方式），可以参考以下步骤：

### Android 平台

在 Activity 或 Fragment 中接入系统返回键事件：

```kotlin
class YourActivity : AppCompatActivity() {
    private lateinit var kuiklyRenderViewDelegator: KuiklyRenderViewBaseDelegator

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            // 将返回键事件交由框架处理
            val isConsumed = kuiklyRenderViewDelegator.onBackPressed()
            if (isConsumed) {
                // 已消费，不执行默认行为
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
    
    // 或者使用 onBackPressed（已废弃，但某些场景仍可使用）
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val isConsumed = kuiklyRenderViewDelegator.onBackPressed()
        if (!isConsumed) {
            super.onBackPressed()
        }
    }
}
```

`onBackPressed()` 方法会：
1. 调用 `renderView.onBackPressed()` 分发返回键事件到 Kuikly 侧
2. 检查 `KRBackPressModule` 是否消费了返回键事件
3. 返回 `true` 表示已消费，`false` 表示未消费

### iOS 平台

在 ViewController 中接入系统返回键事件：

```objective-c
- (void)handleBackPress {
    [self.kuiklyRenderViewDelegator onBackPressedWithCompletion:^(BOOL isConsumed) {
        if (!isConsumed) {
            // 未消费，执行默认行为（如 pop 或 dismiss）
            [self.navigationController popViewControllerAnimated:YES];
        }
    }];
}
```

**注意**：iOS 平台的拦截结果是异步的，`onBackPressedWithCompletion:` 方法不会阻塞等待结果，而是通过 completion 回调异步返回。如果需要同步阻塞等待结果，需要自行实现同步等待逻辑。

`onBackPressedWithCompletion:` 方法会：
1. 通过事件机制发送 `onBackPressed` 事件到 Kuikly 侧
2. 异步等待 Kuikly 侧处理完成后，通过 completion 回调返回是否消费

### OHOS 平台

在 Page 中接入系统返回键事件：

```typescript
@Entry
@Component
struct YourPage {
  private kuiklyController: KRNativeRenderController | null = null;

  onBackPress(): boolean | void {
    if (this.kuiklyController) {
      // 发送返回键事件并等待消费结果（同步）
      return this.kuiklyController?.onBackPress();
    }
    // 未接入 Kuikly，返回 false 执行默认行为
    return false;
  }
}
```

`onBackPress()` 方法会：
1. 通过事件机制发送 `onBackPressed` 事件到 Kuikly 侧
2. 同步等待 Kuikly 侧处理结果
3. 返回 `true` 表示已消费，`false` 表示未消费

## 在 Pager 中使用

### 基础用法

在 Pager 中使用 `BackPressHandler` 处理返回键：

```kotlin
@Page("BackPressHandlerPager")
class BackPressHandlerPager : BasePager() {

    private var inEditMode by observable(false)

    // 创建一次性返回键回调（执行后自动移除）
    private val singleShotBack = object : BackPressCallback() {
        override fun handleOnBackPressed() {
            // 退出编辑态并移除自身，消费一次返回
            inEditMode = false
            getBackPressHandler().removeCallback(this)
        }
    }
    
    // 创建持久返回键回调（需要手动移除）
    private val persistentBack = object : BackPressCallback() {
        override fun handleOnBackPressed() {
            // 持久消费返回，不自动移除
            // 可以在这里执行一些持久性的拦截逻辑
        }
    }
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                Button {
                    attr {
                        titleAttr {
                            text(if (ctx.inEditMode) "BackPress拦截生效中" else "启用单次拦截BackPress")
                        }
                    }
                    event {
                        click {
                            ctx.inEditMode = !ctx.inEditMode
                            if (ctx.inEditMode) {
                                // 进入编辑态时，注册一次性回调
                                ctx.getBackPressHandler().addCallback(ctx.singleShotBack)
                            } else {
                                ctx.getBackPressHandler().removeCallback(ctx.singleShotBack)
                            }
                        }
                    }
                }
                Button {
                    event {
                        click {
                            if (ctx.getBackPressHandler().containsCallback(ctx.persistentBack)) {
                                ctx.getBackPressHandler().removeCallback(ctx.persistentBack)
                            } else {
                                ctx.getBackPressHandler().addCallback(ctx.persistentBack)
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### API 说明

#### BackPressHandler

`BackPressHandler` 是返回键事件的分发器，Pager 提供了 `getBackPressHandler()` 方法获取实例：

```kotlin
// 添加返回键回调
fun addCallback(onBackPressedCallback: BackPressCallback)

// 移除返回键回调
fun removeCallback(onBackPressedCallback: BackPressCallback)

// 判断回调是否存在
fun containsCallback(onBackPressedCallback: BackPressCallback): Boolean
```

#### BackPressCallback

`BackPressCallback` 是返回键回调的抽象类，需要实现 `handleOnBackPressed()` 方法：

```kotlin
abstract class BackPressCallback() {
    abstract fun handleOnBackPressed()
}
```

## 在 Compose DSL 中使用

在 Compose DSL 中，可以使用 `BackHandler` 来处理返回键：

```kotlin
@Composable
fun BackHandlerTest() {
    var isEditing by remember { mutableStateOf(false) }

    Column {
        if (isEditing) {
            Text("当前处于编辑态")
            Text("按返回键将退出编辑态")

            // 处理返回键退出编辑态
            BackHandler {
                isEditing = false
            }
        } else {
            Text("当前处于非编辑态")
            Button(onClick = { isEditing = true }) {
                Text("进入编辑态")
            }
        }
    }
}
```

`BackHandler` 是一个 `@Composable` 函数，当组件进入 Composition 时自动注册回调，离开时自动移除。

## 工作原理

1. **回调栈管理**：`BackPressHandler` 使用栈结构管理回调，后添加的回调会优先处理
2. **事件分发**：当系统返回键事件触发时，从栈顶开始查找，执行第一个有效的回调
3. **消费机制**：回调执行后，会自动通知系统是否消费了返回键事件

## 使用场景

### 场景 1：编辑态拦截

在编辑态时拦截返回键，退出编辑态：

```kotlin
private val editModeBack = object : BackPressCallback() {
    override fun handleOnBackPressed() {
        exitEditMode()
        getBackPressHandler().removeCallback(this)
    }
}

fun enterEditMode() {
    isEditing = true
    getBackPressHandler().addCallback(editModeBack)
}

fun exitEditMode() {
    isEditing = false
    getBackPressHandler().removeCallback(editModeBack)
}
```

### 场景 2：对话框拦截

显示对话框时拦截返回键，关闭对话框：

```kotlin
private val dialogBack = object : BackPressCallback() {
    override fun handleOnBackPressed() {
        closeDialog()
        getBackPressHandler().removeCallback(this)
    }
}

fun showDialog() {
    getBackPressHandler().addCallback(dialogBack)
    // 显示对话框
}

fun closeDialog() {
    getBackPressHandler().removeCallback(dialogBack)
    // 关闭对话框
}
```

### 场景 3：持久拦截

某些场景下需要持久拦截返回键：

```kotlin
private val persistentBack = object : BackPressCallback() {
    override fun handleOnBackPressed() {
        // 持久拦截，不执行默认行为
        // 注意：需要手动管理回调的添加和移除
    }
}

fun enablePersistentIntercept() {
    getBackPressHandler().addCallback(persistentBack)
}

fun disablePersistentIntercept() {
    getBackPressHandler().removeCallback(persistentBack)
}
```

## 注意事项

1. **回调移除**：记得在适当的时机移除回调，避免内存泄漏
2. **回调顺序**：后添加的回调会优先处理，适合实现"最近添加的优先处理"的场景
3. **自动移除**：`BackHandler` 在 Compose DSL 中会自动管理回调的生命周期，无需手动移除
4. **渲染层接入**：如果需要在渲染层接入，确保正确调用框架提供的方法，并处理消费结果

## 完整示例

可以参考以下示例代码：

- Pager 使用示例：`demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/BackPressHandlerPager.kt`
- Compose DSL 使用示例：`demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/BackHandlerDemo.kt`
