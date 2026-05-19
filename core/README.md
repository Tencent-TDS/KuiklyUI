# Core Module

Kuikly 跨平台核心模块，包含共享业务逻辑、响应式框架、布局系统、桥接管理等核心能力。

## ⚠️ Breaking Change: JS moduleName 大小写变更

### 变更内容

`core` 模块的 JS 编译目标 `moduleName` 由 `KuiklyCore-core` 改为 `kuiklycore-core`（全小写）。

涉及的构建配置文件：

| 构建文件 | 变更前 | 变更后 |
|---|---|---|
| `build.2.1.21.gradle.kts` | `KuiklyCore-core` | `kuiklycore-core` |
| `build.2.0.21.gradle.kts` | `KuiklyCore-core` | `kuiklycore-core` |
| `build.1.9.22.gradle.kts` | `KuiklyCore-core` | `kuiklycore-core` |

### 变更原因

Kotlin 编译为 JS 后，编译产物的目录路径会使用 `moduleName` 作为目录名（如 `kotlin/{moduleName}/entry/`）。在**小程序 JS 分包**场景下，Webpack 的 split chunks 配置和多入口扫描均依赖该路径来定位编译产物。

当 `moduleName` 包含大写字母时（如 `KuiklyCore-core`），小程序环境的文件系统或路径解析对大小写处理不一致，会导致分包后**找不到对应的路径文件**，从而引发 JS 加载失败。将 `moduleName` 统一改为全小写可以彻底规避此类路径匹配问题。

### 影响范围及注意事项

#### 1. Webpack split chunks 配置

如果业务方在 `webpack.config.d/split-chunks.js` 中自定义了 `cacheGroups`，且通过正则匹配了旧的 `KuiklyCore-core` 路径，需要同步更新为 `kuiklycore-core`：

```js
// ❌ 旧配置（不再匹配）
test: /[\\/]KuiklyCore-core[\\/]/

// ✅ 新配置
test: /[\\/]kuiklycore-core[\\/]/
```

#### 2. 自定义 Webpack 多入口配置

如果业务方手动引用了编译产物路径，需要将路径中的大写改为小写：

```
# ❌ 旧路径（不再有效）
kotlin/KuiklyCore-core/entry/

# ✅ 新路径
kotlin/kuiklycore-core/entry/
```

#### 3. JS 产物文件名引用

如果业务方在代码中硬编码了 JS 产物文件名，需同步修改：

```
# ❌ 旧文件名
KuiklyCore-core.js

# ✅ 新文件名
kuiklycore-core.js
```

#### 4. 不受影响的场景

- **Android、iOS、HarmonyOS 原生平台**：不受此变更影响，`moduleName` 变更仅影响 JS 编译目标。
- **未自定义 Webpack 配置的业务**：如果使用框架默认的分包配置（由 `JSSplitProcessor` 自动生成），则无需手动修改，插件会自动使用正确的 `moduleName`。
- **非分包模式**：如果未启用 JS 分包功能，此变更对产物加载无影响。

### 升级检查清单

升级到新版本时，请检查以下内容：

- [ ] 项目中是否有自定义的 `webpack.config.d/split-chunks.js`，如有则更新正则匹配规则
- [ ] 项目中是否有硬编码的 `KuiklyCore-core` 路径引用，如有则改为 `kuiklycore-core`
- [ ] 项目中是否有手动引用 `KuiklyCore-core.js` 产物文件名，如有则改为 `kuiklycore-core.js`
