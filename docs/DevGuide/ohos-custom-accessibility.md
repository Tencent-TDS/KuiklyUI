# 鸿蒙自定义 ArkTS 组件无障碍接入指南

## 背景

Kuikly 在鸿蒙上有两条渲染路径：

- **CAPI 内置组件**（`KRView` / `KRImageView` 等）：由 SDK 直接处理无障碍属性，**业务方零改动**。
- **ArkTS 转发组件**（业务方通过 `viewName()` 注册的自定义组件）：无障碍属性必须**通过 ArkTS 侧的修饰器**在业务方自己的 `@Component` 里应用（鸿蒙 SDK 限制 CAPI 无法修改 ArkTS 节点）。

本文档面向 ArkTS 自定义组件的业务方开发者。

## 最小接入模板

假设你有一个自定义组件 `KRHelloView`，先让它的 `KuiklyRenderBaseView` 子类接受 kuikly 的 `accessibility*` 属性：

```typescript
import { KRAny, KuiklyRenderBaseView, KuiklyRenderCallback } from '@kuikly-open/render';

@Observed
export class KRHelloView extends KuiklyRenderBaseView {
  static readonly VIEW_NAME = 'KRHelloView';

  cssMessage: string | null = null;

  setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean {
    switch (propKey) {
      case 'message':
        this.cssMessage = propValue as string;
        return true;
      default:
        // 让基类处理 backgroundColor / backgroundImage / accessibility*
        return super.setProp(propKey, propValue);
    }
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
    // 交给基类兜底以支持 accessibilityAnnounce / accessibilityFocus
    super.call(method, params, callback);
  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> { /* ... */ }
}
```

然后在 `@Component` 最外层容器上应用无障碍修饰器：

```typescript
@Component
export struct KRHelloViewComponent {
  @ObjectLink renderView: KRHelloView;

  build() {
    Stack() {
      Text(this.renderView.cssMessage)
    }
    // .id()：提供 customId 供 accessibilityFocus 定位（缺失会导致 focus 静默失效）
    .id(this.renderView.getNodeId())
    // .accessibilityText()：应用 kuikly `accessibility` 属性
    .accessibilityText(this.renderView.cssAccessibilityText ?? '')
    // .accessibilityGroup()：使用者调 accessibilityRole(非 NONE) 时，把组件聚合为一个焦点单元；
    .accessibilityGroup(this.renderView.cssAccessibilityRole != null
      && this.renderView.cssAccessibilityRole !== 'none')
    // .accessibilityLevel()：role=none 时让外层退出无障碍树；其余用默认 'auto'
    .accessibilityLevel(this.renderView.cssAccessibilityRole === 'none' ? 'no' : 'auto')
    // === 以下是你自己的样式 ===
    .backgroundColor(this.renderView.cssBackgroundColor)
    .size({ width: '100%', height: '100%' })
  }
}
```

> **完整可运行示例**：`ohosApp/entry/src/main/ets/kuikly/components/KRMyDemoCustomView.ets`。

## 关键点解释

### `.accessibilityGroup` 为什么绑定到 role？

使用者设 `role = BUTTON` 后，需要 `accessibilityGroup` 把组件当一个整体来响应无障碍焦点。

### 什么时候给子节点加 `.accessibilityLevel('no')`？

**规则**（仅在 group=false 时适用）：

- 会抢焦点、又不需要独立聚焦的**纯展示子节点**（作为装饰的 `Text` / `Image`）：加 `.accessibilityLevel('no')`。
- 需要独立聚焦并可被读屏双击激活的**可交互子控件**（`Button` 等）：**绝不加**，否则读屏用户无法激活它。
- 组件是单一叶子控件、或子节点本身没有会被朗读的默认内容：什么都不加。

### 为什么必须 `.id(getNodeId())`？

`accessibilityFocus` 通过 `id` 定位目标节点，**遗漏 `.id()` 会导致 `accessibilityFocus` 静默失效**（不抛异常，但焦点不跳转）。

### 我怎么在 ArkTS 转发组件里表达"按钮 / 复选框"这种角色语义？

ArkTS 由于 API 版本限制，不支持 `accessibilityRole`，推荐做法：**把角色语义写进 `accessibility` 文案**：

```kotlin
MyDemoCustom {
    attr {
        accessibility("提交按钮") // 把角色语义写进文案
        accessibilityRole(AccessibilityRole.BUTTON)
    }
}
```

朗读器会读出"提交按钮"，听感与真正设置了 role 的组件基本等价。

> 此兼容性限制**仅影响 ArkTS 转发组件**。CAPI 内置组件（如 kuikly 的 `View`/`Image`）设 `accessibilityRole(BUTTON)` 仍然会读出"按钮"角色。

### accessibilityAnnounce / accessibilityFocus 需要业务方做什么？

**只要 `KuiklyRenderBaseView.call` 里调了 `super.call(method, params, callback)`，就自动支持**。业务方无需额外代码。

## 常见问题

**Q：`view.accessibilityFocus()` 调了但焦点没跳？**
A：外层容器忘了 `.id(this.renderView.getNodeId())`；或者你手动指定了固定 id 字符串（会覆盖 nodeId）。

**Q：读屏开启后，自定义组件里的 `Button.onClick` 点不动？**
A：使用者在 kotlin 侧设了 `accessibilityRole(非 NONE)`，触发 `.accessibilityGroup(true)`——读屏拦截了内部子控件的原生手势。这是使用者选择 role 的自然后果。若需要子控件独立可点：使用者应**不设** role（或设为 NONE）；对于"整个组件是一个按钮"的场景，交互事件应挂在 kotlin 侧外层 `event { click { ... } }`。

## 参考

- Kuikly 基础 accessibility API：[basic-attr-event.md](../API/components/basic-attr-event.md#accessibility方法)
- 完整示例：`ohosApp/entry/src/main/ets/kuikly/components/KRMyDemoCustomView.ets`
- ArkTS 无障碍 API：[@ohos.accessibility](https://developer.huawei.com/consumer/cn/doc/harmonyos-references/js-apis-accessibility)