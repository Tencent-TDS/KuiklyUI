# KuiklyUI 编码规范

> 以下场景读取本文件：新建文件需要添加版权头；选择 import 包路径（Compose 相关包名）；新增 `expect/actual` 需要确认平台覆盖；render 层新建组件或 Module 需要确认命名前缀（KR）；添加日志需要确认 Tag 规范；新增类或方法需要确认 `public` vs `internal` 可见性。

## 1. 版权头

所有文件必须包含标准版权头：
```kotlin
/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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
```

---

## 2. 包名规则（重要⚠️）

| 类别 | 正确包名 | ❌ 禁止使用 |
|------|---------|------------|
| Compose Runtime | `androidx.compose.runtime.*` | — （直接用官方） |
| Compose UI | `com.tencent.kuikly.compose.ui.*` | `androidx.compose.ui.*` |
| Compose Foundation | `com.tencent.kuikly.compose.foundation.*` | `androidx.compose.foundation.*` |
| Compose Material3 | `com.tencent.kuikly.compose.material3.*` | `androidx.compose.material3.*` |
| Compose Animation | `com.tencent.kuikly.compose.animation.*` | `androidx.compose.animation.*` |
| Core 引擎 | `com.tencent.kuikly.core.*` | — |

**规则总结**：只有 `androidx.compose.runtime.*` 用官方包，其他所有 Compose UI 相关包一律用 `com.tencent.kuikly.compose.*`。

`core-render-android/` 中可使用原生 Android 依赖（`androidx.annotation.*`、`androidx.recyclerview.*` 等），**仅限该模块**，`core/` 和 `compose/` 中禁止。

> DSL 具体写法规范见 [.ai/self-dsl/AGENTS.md](../self-dsl/AGENTS.md) 和 [.ai/compose-dsl/AGENTS.md](../compose-dsl/AGENTS.md)

---

## 3. expect/actual 编写规范

新增 `expect/actual` 时，必须为全部 **5 个平台**提供 actual 实现：

| Source Set | 平台 |
|-----------|------|
| `androidMain` | Android |
| `appleMain` | iOS / macOS（注意：**不是** `iosMain`） |
| `jsMain` | Web / 小程序 (Kotlin/JS) |
| `jvmMain` | 纯 JVM |
| `ohosArm64Main` | HarmonyOS |

**检查清单**：
- [ ] 5 个平台都提供了 actual 实现
- [ ] 包路径在所有平台完全一致
- [ ] 方法签名完全匹配
- [ ] actual 文件与 expect 文件同包同名，放在对应 `<platform>Main/` 目录下

---

## 4. 命名规范

Render 层（`core-render-android/` / `core-render-ios/`）的组件和模块以 `KR` 开头：

- 组件视图：`KR` + 视图名，如 `KRView`、`KRImageView`、`KRScrollView`
- 原生模块：`KR` + 功能 + `Module`，如 `KRLogModule`、`KRNetworkModule`
- 适配器接口：`IKR` 前缀，如 `IKRImageAdapter`、`IKRFontAdapter`

---

## 5. API 可见性规范

KuiklyUI 是 SDK 框架，**默认用 `internal`，谨慎使用 `public`**。

- `public`：仅对外暴露的稳定 API（一旦 public 就是对外承诺，变更需要兼容考虑）
- `internal`：模块内部实现、未稳定的 API、仅供框架内部使用的类
- 新增类/方法时，先问：「下游业务代码需要直接使用这个吗？」，不确定就用 `internal`

---

## 6. 日志规范（KLog）

```kotlin
KLog.i(TAG, "页面加载完成")
KLog.d(TAG, "调试: $data")
KLog.e(TAG, "错误: $error")
```

**Tag 规范**：同一功能开发或 Bug 定位，使用**统一前缀**，便于日志过滤追踪：

```kotlin
// ✅ 同一功能统一前缀
private const val TAG = "Payment"
KLog.i(TAG, "开始支付")
KLog.d(TAG, "支付参数: $params")
KLog.e(TAG, "支付失败: $error")

// ❌ 各自为政，无法过滤
KLog.i("PayPage", "开始支付")
KLog.d("PaymentManager", "支付参数: $params")
KLog.e("OrderModule", "支付失败: $error")
```

**输出格式**：`[KLog][tag]:msg` — 通过 `KRLogModule` 桥接到各平台原生日志系统。
