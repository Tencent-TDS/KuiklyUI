# Kuikly H5 App (JavaScript)

JavaScript 实现的 Kuikly H5 应用，通过集成编译后的 Kotlin/JS 核心渲染库实现完整的 Kuikly 渲染能力。

## ✨ 特性

- 🎯 **委托模式集成**：通过委托模式无缝集成 Kotlin/JS 核心渲染引擎
- 📦 **智能代码分包**：基于 Webpack 的自动代码分包优化，支持长期缓存
- 🔄 **动态 Bundle 加载**：自动扫描和加载依赖文件，无需手动配置
- 🚀 **热重载开发**：修改 JavaScript 代码即时生效，开发体验流畅
- 🛠️ **完整工具链**：提供一键式编译、复制、生成脚本，自动化开发流程
- 📝 **TypeScript 支持**：完整的类型定义文件，IDE 智能提示

## 🚀 快速开始

### 📋 常用 npm 命令

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器 |
| `npm run build` | 构建生产版本 |
| `npm run rebuild-kotlin` | 编译并复制 Kotlin/JS 核心库（生产版本） |
| `npm run rebuild-kotlin:dev` | 编译并复制 Kotlin/JS 核心库（开发版本） |
| `npm run build-bundles:dev -- HelloWorldPage,000` | 编译指定页面 Bundle（开发环境，PowerShell 需加引号） |
| `npm run build-bundles:all:dev` | 编译所有页面 Bundle（开发环境） |
| `npm run build-bundles -- HelloWorldPage,000` | 编译指定页面 Bundle（生产环境，PowerShell 需加引号） |
| `npm run build-bundles:all` | 编译所有页面 Bundle（生产环境） |
| `npm run generate-manifest` | 生成 Bundle manifest 文件 |
| `npm run copy-libs` | 仅复制 Kotlin/JS 核心库（生产版本，不编译） |
| `npm run copy-libs:dev` | 仅复制 Kotlin/JS 核心库（开发版本，不编译） |

### 配置准备

`core-gradle-plugin`和`core-ksp`使用支持分包的版本。

`buildSrc/src/main/java/KuiklyKotlinBuildVar.kt`配置：

```bash
object BuildPlugin {
    val kuikly by lazy {
        "com.tencent.kuikly-open:core-gradle-plugin:2.18.1-2.0.21"
    }
}
```

`demo/build.gradle.kts`配置：

```bash
dependencies {
    compileOnly("com.tencent.kuikly-open:core-ksp:2.18.1-2.0.21") {
        add("kspIosArm64", this)
        add("kspIosX64", this)
        add("kspIosSimulatorArm64", this)
        add("kspMacosArm64", this)
        add("kspMacosX64", this)
        add("kspAndroid", this)
        add("kspJs", this)
    }
}
```

使用js按页分包时，如有多模块，需要关闭多模块，如`demo/build.gradle.kts`里配置：

```bash
ksp {
    arg("enableMultiModule", "false")
}
```

### 完整初始化流程（首次运行）

```bash
# 1. 安装依赖
npm install

# 2. 编译并复制 Kotlin 核心库
npm run rebuild-kotlin

# 3. 编译并复制页面 Bundle（指定页面名称）
# 说明：推荐带上 `--`，确保参数能被透传到脚本。
# - macOS / Linux（bash/zsh）：可直接用逗号列表（是否加引号都可以）
# - Windows PowerShell：逗号会被解析为数组分隔符，必须加引号，或拆成两个参数
npm run build-bundles:dev -- "HelloWorldPage,000"

# 4. 启动开发服务器
npm run dev
```

访问 http://localhost:8080/?page_name=xxx

### 快速开始（推荐的开发流程）

1. **修改核心库代码** → 运行 `npm run rebuild-kotlin`
2. **修改页面代码** → 运行 `npm run build-bundles:dev -- 页面名`
3. **修改 JS 代码** → 自动热重载，无需操作

---

## 📖 详细说明

### 1. 安装依赖
```bash
npm install
```

### 2. 编译并复制 Kotlin/JS 核心库

```bash
npm run rebuild-kotlin
```

此命令会自动完成：
1. 编译 `core-render-web:h5` 模块（已包含 base 模块代码）
2. 复制编译产物到 `src/libs/`

**手动编译（可选）**
```bash
# 从项目根目录编译 Kotlin 模块
./gradlew :core-render-web:h5:clean :core-render-web:h5:jsBrowserProductionWebpack

# 复制编译产物到 h5App-js
npm run copy-libs
```

编译产物说明：
- `KuiklyCore-render-web-h5.js` + `.d.ts`：H5 渲染模块（webpack 打包，已包含 base 模块和所有依赖）

**开发模式编译（可选）**
```bash
npm run rebuild-kotlin:dev
```
开发模式生成的产物包含更多调试信息，便于排查问题。

### 3. 编译并复制页面 Bundle

Kuikly 页面需要先编译成 JS bundle 文件才能在 Web 中加载。

```bash
# 开发环境（推荐开发调试）
# macOS / Linux：
npm run build-bundles:dev -- HelloWorldPage,000
# Windows PowerShell（二选一）：
npm run build-bundles:dev -- "HelloWorldPage,000"
# 或：npm run build-bundles:dev -- HelloWorldPage 000

# 生产环境（默认）
# macOS / Linux：
npm run build-bundles -- HelloWorldPage,000
# Windows PowerShell（二选一）：
npm run build-bundles -- "HelloWorldPage,000"
# 或：npm run build-bundles -- HelloWorldPage 000

# 编译所有页面
npm run build-bundles:all
# 编译所有页面（开发环境）
npm run build-bundles:all:dev
```

**手动编译（可选）**

**Windows（推荐，PowerShell/Terminal 直接可运行）**
```bash
# 生产环境（默认）
cmd /c "cd /d D:\Projects\KuiklyUI && gradlew.bat clean && gradlew.bat :demo:packEntryJSBundleRelease -PpageNameList=HelloWorldPage,000"
cmd /c "cd /d D:\Projects\KuiklyUI && gradlew.bat clean && gradlew.bat :demo:packEntryJSBundleRelease -PpageNameList=all"

# 开发环境
cmd /c "cd /d D:\Projects\KuiklyUI && gradlew.bat clean && gradlew.bat :demo:packEntryJSBundleDebug -PpageNameList=HelloWorldPage,000"
cmd /c "cd /d D:\Projects\KuiklyUI && gradlew.bat clean && gradlew.bat :demo:packEntryJSBundleDebug -PpageNameList=all"
```

**macOS / Linux（可选）**
```bash
# 从项目根目录执行
cd ..

# 生产环境（默认）
./gradlew clean && ./gradlew :demo:packEntryJSBundleRelease -PpageNameList=HelloWorldPage,000
./gradlew clean && ./gradlew :demo:packEntryJSBundleRelease -PpageNameList=all

# 开发环境
./gradlew clean && ./gradlew :demo:packEntryJSBundleDebug -PpageNameList=HelloWorldPage,000
./gradlew clean && ./gradlew :demo:packEntryJSBundleDebug -PpageNameList=all
```

> 💡 更推荐直接使用 `npm run build-bundles:dev -- <页面名>`（开发环境）或 `npm run build-bundles -- <页面名>`（生产环境），它会自动完成编译、复制 bundle、生成 manifest。

#### 编译产物说明

Bundle 文件采用 **Webpack 智能代码分包**策略，自动将代码拆分为多个文件以优化加载性能：

| 文件类型 | 命名规则 | 说明 | 缓存策略 |
|---------|---------|------|---------|
| **Runtime** | `runtime.bundle.js` | Webpack 运行时代码 | 长期缓存 |
| **Kotlin 核心** | `kotlin-stdlib.kotlin_k.bundle.js` | Kotlin 标准库核心 | 长期缓存 |
| **Kotlin 标准库** | `kotlin-stdlib.kotlin_kotlin-*.bundle.js` | Kotlin 标准库模块 | 长期缓存 |
| **KuiklyCore 基础** | `kotlin-stdlib.kotlin_KuiklyCore-*.bundle.js` | KuiklyCore 框架模块 | 中期缓存 |
| **页面代码** | `[PageName].bundle.js`（dev） / `[PageName].[contenthash:8].bundle.js`（prod） | 页面业务逻辑 | 频繁更新 |
| **公共依赖** | `vendors.bundle.js` | node_modules 依赖 | 长期缓存 |
| **公共代码** | `common.bundle.js` | 页面间共享代码 | 中期缓存 |
| **Manifest** | `manifest.json` | 包含 `files`（公共依赖）与 `pages`（页面映射） | 每次更新 |
| **资源文件** | `composeResources/` | Compose 资源 | 按需缓存 |

**分包优势：**
- ✅ **长期缓存**：Kotlin 标准库等基础文件很少变化，可以长期缓存
- ✅ **按需更新**：修改页面代码只需更新对应的 bundle 文件
- ✅ **并行加载**：多个小文件可以并行下载，提升加载速度
- ✅ **共享复用**：多个页面共享相同的基础库文件

### Bundle 依赖管理

#### 🔍 动态发现机制

项目采用**基于 manifest.json 的动态加载机制**，无需手动维护 bundle 文件列表：

```
┌─────────────────────────────────────────┐
│  1. 编译并复制 bundles                   │
│     npm run build-bundles               │
│     ↓                                   │
│  2. 生成 manifest.json                   │
│     scripts/generate-manifest.js        │
│     - files: 公共依赖 bundle 列表         │
│     - pages: page_name → 页面 bundle 文件  │
│     ↓                                   │
│  3. 运行时读取 manifest.json             │
│     - 按规则排序并加载 files              │
│     - 根据 pages 加载当前页面 bundle       │
└─────────────────────────────────────────┘
```

#### 📋 工作流程

1. **产物准备**：`npm run build-bundles` 会把编译产物复制到 `src/bundles/`
2. **生成清单**：`generate-manifest.js` 生成 `manifest.json`（包含 `files` 和 `pages`）
3. **公共依赖加载**：运行时读取 `files`，按 `bundle-config.js` 规则排序并加载
4. **页面加载**：运行时根据 `pages[page_name]` 加载页面 bundle（支持带 hash）
5. **自动适应**：文件名变化时，重新构建并生成 manifest 即可自动更新

#### ⚙️ 加载配置（bundle-config.js）

```javascript
window.BundleConfig = {
  // 📦 加载顺序规则（按优先级排序的正则表达式）
  dynamicLoadingOrder: [
    /^runtime\.bundle\.js$/,                     // 1. Webpack runtime
    /^kotlin-stdlib\.kotlin_k\.bundle\.js$/,     // 2. Kotlin 核心
    /^kotlin-stdlib\.kotlin_kotlin-.*\.js$/,     // 3. Kotlin 标准库
    /^kotlin-stdlib\.kotlin_KuiklyCore-.*\.js$/, // 4. KuiklyCore 框架
    /^vendors\.bundle\.js$/,                     // 5. 第三方依赖
    /^common\.bundle\.js$/                       // 6. 公共代码
  ],
  
  // 🎯 页面特殊依赖配置
  pageDependencies: {
    'SpecialPage': ['extra-lib.bundle.js']
  },
  
  // 🚀 性能优化配置
  enableParallelLoading: true,  // 启用并行加载
  loadTimeout: 10000            // 加载超时时间（毫秒）
};
```

#### 手动生成 manifest（仅在需要时）

如果需要单独生成 manifest.json：

```bash
npm run generate-manifest
```

#### ✅ 关键特性

| 特性 | 说明 |
|------|------|
| ✅ **全自动化** | `npm run build-bundles` 自动生成 manifest.json |
| ✅ **动态发现** | 公共依赖与页面 bundle 均从 manifest.json 读取（`files`/`pages`） |
| ✅ **智能排序** | 正则规则确保正确的依赖加载顺序 |
| ✅ **自动适应** | 文件名变化时重新构建即可更新 |
| ✅ **并行优化** | 支持并行加载提升性能 |
| ⚠️ **依赖清单** | 必须有 manifest.json 才能运行 |

### 4. 启动开发服务器
```bash
npm run dev
```

访问 http://localhost:8080/?page_name=xxx

### 5. 构建生产版本
```bash
npm run build
```

构建产物位于 `build/distributions/`

## 📁 项目结构

```
h5App-js/
├── src/
│   ├── libs/                # Kotlin/JS 核心库（webpack 打包版本）
│   │   ├── KuiklyCore-render-web-h5.js          # H5 渲染模块（已包含 base 模块）
│   │   └── KuiklyCore-render-web-h5.d.ts        # TypeScript 类型定义
│   ├── bundles/             # 页面 Bundle 文件
│   │   ├── router.bundle.js
│   │   ├── home.bundle.js
│   │   └── composeResources/  # Compose 资源
│   ├── resources/           # 静态资源和配置
│   │   └── bundle-config.js # Bundle依赖配置文件
│   ├── components/          # 自定义组件
│   ├── modules/             # 自定义模块
│   ├── KuiklyWebRenderViewDelegator.js  # 委托实现
│   └── index.js            # 应用入口
├── scripts/                 # 构建脚本（通过 npm 命令调用）
│   ├── build-and-copy-bundles.sh       # 编译并复制页面 Bundle
│   ├── copy-kotlin-libs.sh             # 复制 Kotlin 核心库
│   ├── rebuild-kotlin-and-copy.sh      # 编译并复制 Kotlin 核心库
│   └── generate-manifest.js            # 生成 bundle manifest 文件
└── package.json
```

## 🎯 核心架构

本项目通过 **委托模式** 集成 Kotlin/JS 核心渲染库：

```
JavaScript App
    ↓
KuiklyWebRenderViewDelegator.js (JS 委托实现)
    ↓
KuiklyRenderViewDelegator (Kotlin/JS 编译)
    ↓
核心渲染引擎 (Kotlin)
```

## 🔧 关键实现

### 1. Kotlin/JS 模块加载

在 `index.html` 中加载：
```html
<script src="libs/KuiklyCore-render-web-h5.js"></script>
```

> 注：h5 模块已包含 base 模块代码，无需单独加载 base 模块。使用 webpack 打包版本，已包含所需的 Kotlin 标准库依赖。

### 2. 访问导出的 Kotlin 类

Kotlin/JS 编译后会在 `window` 对象上注册全局模块：

```javascript
// H5 模块（主入口类）
const h5Module = window['com.tencent.kuikly-open.core-render-web:h5'];
const KotlinDelegator = h5Module.com.tencent.kuikly.core.render.web.runtime.web.expand.KuiklyRenderViewDelegator;

// Base 模块（枚举和工具类）
const baseModule = window['KuiklyCore-render-web-base'];
const ExecuteMode = baseModule.com.tencent.kuikly.core.render.web.context.KuiklyRenderCoreExecuteMode;
const ErrorReason = baseModule.com.tencent.kuikly.core.render.web.exception.ErrorReason;
```

### 3. 实现委托接口

`KuiklyWebRenderViewDelegator.js` 实现了 Kotlin 委托接口的所有方法：

```javascript
const delegateImpl = {
  // 注册自定义渲染视图
  registerExternalRenderView(kuiklyRenderExport) {
    // 注册 MyView, WebView, PageView 等
  },
  
  // 注册自定义模块
  registerExternalModule(kuiklyRenderExport) {
    // 注册 Bridge, Cache 等模块
  },
  
  // 返回执行模式（JS = 2）
  coreExecuteMode() {
    return baseModule.com.tencent.kuikly.core.render.web.context
           .KuiklyRenderCoreExecuteMode.JS_getInstance();
  },
  
  // 生命周期回调
  onKuiklyRenderViewCreated() { /* ... */ },
  onPageLoadComplete(isSucceed, errorReason, executeMode) { /* ... */ },
  onUnhandledException(throwable, errorReason, executeMode) { /* ... */ }
};

// 创建 Kotlin 实例
const kotlinDelegator = new KotlinDelegator(delegateImpl);
```

### 4. JavaScript ↔ Kotlin 类型转换

**JS Object → Kotlin Map**
```javascript
const kotlinStdlib = window['kotlin-kotlin-stdlib'];
const pageDataMap = kotlinStdlib.kotlin.collections.KtMap.fromJsMap(
  new Map(Object.entries(pageData))
);
```

**尺寸转换（Pair<Int, Int>）**
```javascript
// Kotlin Pair 在 JS 中用数组表示
const kotlinSize = [width, height];
```

### 5. 使用示例

```javascript
import { KuiklyWebRenderViewDelegator } from './KuiklyWebRenderViewDelegator';

const delegator = new KuiklyWebRenderViewDelegator();
delegator.init(
  'container-id',           // 容器 DOM ID
  'page-name',              // 页面名称
  { key: 'value' },         // 页面数据（自动转为 Kotlin Map）
  { width: 375, height: 667 }  // 尺寸
);

// 生命周期管理
delegator.resume();  // 页面可见
delegator.pause();   // 页面不可见
delegator.detach();  // 页面卸载

// 发送事件
delegator.sendEvent('custom_event', { data: 'value' });
```

## 🔄 开发工作流

### 修改 Kotlin 核心代码后

```bash
npm run rebuild-kotlin
```

**手动方式：**
1. 重新编译 h5 模块：`./gradlew :core-render-web:h5:clean :core-render-web:h5:jsBrowserProductionWebpack`
2. 复制产物：`cd h5App-js && ./scripts/copy-kotlin-libs.sh`
3. 刷新浏览器

**脚本功能：**
- ✅ 自动编译 h5 模块（已包含 base 模块代码）
- ✅ 自动复制 JS 文件和 TypeScript 声明文件（`.d.ts`）
- ✅ 提供编译进度提示
- ✅ 错误时自动停止

### 修改页面代码后

```bash
# 只编译修改的页面（开发环境）
npm run build-bundles:dev -- HelloWorldPage

# 或编译所有页面（开发环境）
npm run build-bundles:all:dev
```

**手动方式：**
1. 编译页面（按需二选一；Windows 用 `gradlew.bat`）：
   - 生产环境：`./gradlew clean && ./gradlew :demo:packEntryJSBundleRelease -PpageNameList=HelloWorldPage`
   - 开发环境：`./gradlew clean && ./gradlew :demo:packEntryJSBundleDebug -PpageNameList=HelloWorldPage`
2. 复制 bundle（按需二选一）：
   - 生产环境：`cp ../demo/build/dist/js/productionExecutable/*.bundle.js src/bundles/`
   - 开发环境：`cp ../demo/build/dist/js/developmentExecutable/*.bundle.js src/bundles/`
3. 刷新浏览器

**脚本功能：**
- ✅ 自动清理旧产物
- ✅ 编译指定页面或全部页面
- ✅ 自动复制 bundle 文件和资源
- ✅ **自动生成 manifest.json**
- ✅ 显示详细的编译信息

### 修改 JavaScript 代码后
开发服务器自动热重载，无需操作

## ⚠️ 注意事项

### 🔧 构建相关

| 注意点 | 说明 |
|--------|------|
| **动态依赖发现** | Bundle 文件通过 `manifest.json` 动态发现和加载 |
| **自动化流程** | `npm run build-bundles` 自动生成 manifest.json |
| **依赖排序** | 加载顺序通过 `bundle-config.js` 正则规则定义 |
| **文件名变化** | Webpack 使用确定性 ID，文件名可能变化，重新构建自动适应 |
| **代码分包** | Webpack 自动分包优化，无需手动配置 |

### 📝 代码相关

| 注意点 | 说明 |
|--------|------|
| **类型转换** | JS 对象需转换为 Kotlin 类型（Map、Pair） |
| **全局变量** | Kotlin 类通过 `@JsExport` 挂载到 `window` 对象 |
| **TypeScript 支持** | 编译产物包含 `.d.ts` 类型定义文件 |
| **异步加载** | Bundle 采用异步加载，注意处理加载时序 |

### 🎯 Kotlin/JS 相关

| 注意点 | 说明 |
|--------|------|
| **@JsExport** | 关键类已添加 `@JsExport` 和 `@JsName` 注解 |
| **模块系统** | 使用 UMD 模块格式，兼容多种环境 |
| **依赖版本** | 编译前需确保 h5 模块依赖 base（implementation） |

## 🔧 npm 脚本详解

### 开发相关
- `npm run dev` - 启动开发服务器（支持热重载）
- `npm run build` - 构建生产版本
- `npm run build:dev` - 构建开发版本

### Kotlin 编译相关
库来源是 core-render-web，编译前需要将 KuiklyUI/core-render-web/h5/build.gradle.kts 中对 base 的依赖方式从 complieOnly 改成 implementation

- `npm run rebuild-kotlin` - 编译并复制 Kotlin/JS 核心库（生产版本）
- `npm run rebuild-kotlin:dev` - 编译并复制 Kotlin/JS 核心库（开发版本，含调试信息）
- `npm run copy-libs` - 仅复制已编译的核心库（生产版本，不执行编译）
- `npm run copy-libs:dev` - 仅复制已编译的核心库（开发版本，不执行编译）

### 页面 Bundle 相关
- `npm run build-bundles:dev -- <页面名>` - 编译指定页面（开发环境；Windows PowerShell：`npm run build-bundles:dev -- "HelloWorldPage,000"` 或 `npm run build-bundles:dev -- HelloWorldPage 000`）
- `npm run build-bundles -- <页面名>` - 编译指定页面（生产环境；Windows PowerShell：`npm run build-bundles -- "HelloWorldPage,000"` 或 `npm run build-bundles -- HelloWorldPage 000`）
- `npm run build-bundles:all:dev` - 编译所有页面（开发环境）
- `npm run build-bundles:all` - 编译所有页面（生产环境）
- `npm run generate-manifest` - 生成 Bundle manifest 文件

> 💡 **提示**：所有 npm 脚本内部都调用了 `scripts/` 目录下的 Shell 脚本，如需了解详细执行逻辑，可以查看对应的 `.sh` 文件。

## 🔧 故障排查

### 📦 编译相关

<details>
<summary><b>❌ 编译失败</b></summary>

**症状：** Gradle 编译报错或构建失败

**可能原因：**
- Kotlin 代码存在语法错误
- 依赖配置问题
- Gradle 缓存损坏

**解决方案：**
1. 检查控制台输出的详细错误信息
2. 清理 Gradle 缓存：`./gradlew clean`
3. 检查 `build.gradle.kts` 中的依赖配置
4. 确保 h5 模块使用 `implementation` 依赖 base 模块
5. 修复代码错误后重新运行 `npm run build-bundles`

</details>

<details>
<summary><b>❌ 找不到 Bundle 文件</b></summary>

**症状：** 编译成功但找不到生成的 bundle 文件

**可能原因：**
- 页面名称拼写错误
- 编译产物路径不正确
- 复制脚本失败

**解决方案：**
1. 检查页面名称：查看 `demo/src/commonMain/assets/` 目录
2. 确认编译产物：检查 `demo/build/dist/js/productionExecutable/`（默认）或 `demo/build/dist/js/developmentExecutable/`（dev）
3. 验证复制结果：检查 `h5App-js/src/bundles/` 目录
4. 重新运行完整流程：`npm run build-bundles -- PageName`（生产）或 `npm run build-bundles:dev -- PageName`（开发）

</details>

### 🌐 运行时相关

<details>
<summary><b>❌ Module not loaded 错误</b></summary>

**症状：** 浏览器控制台报 "Module not loaded" 错误

**可能原因：**
- Kotlin/JS 模块加载顺序不正确
- 模块未加载完成就被调用
- 缺少必要的依赖文件

**解决方案：**
1. 检查 HTML 中脚本加载顺序是否正确
2. 确认核心库文件存在：`src/libs/KuiklyCore-render-web-h5.js`
3. 清空浏览器缓存（Ctrl + Shift + Delete）
4. 查看 Network 面板，确认没有 404 错误
5. 检查 bundle-config.js 的加载顺序配置

</details>

<details>
<summary><b>❌ Bundle 加载失败</b></summary>

**症状：** 页面无法正常显示，控制台报 bundle 加载错误

**可能原因：**
- `src/bundles/manifest.json` 不存在或过期
- `manifest.json` 中缺少 `pages` 映射，或 `pages` 中没有当前 `page_name`
- bundle 文件名变化但清单未更新

**解决方案：**
1. 重新生成 manifest：`npm run generate-manifest`
2. 检查文件存在：`src/bundles/manifest.json`
3. 验证文件内容：确认存在 `files` 与 `pages`，并且 `pages` 包含当前 `page_name`
4. 重新编译 bundle：`npm run build-bundles:dev -- PageName`（开发）或 `npm run build-bundles -- PageName`（生产）
5. 清空浏览器缓存并刷新

</details>

<details>
<summary><b>❌ 页面白屏或无响应</b></summary>

**症状：** 页面显示空白或无法交互

**可能原因：**
- JavaScript 执行错误
- Bundle 文件损坏
- 页面数据格式错误

**解决方案：**
1. 打开浏览器开发者工具（F12）
2. 查看 Console 面板的错误信息
3. 检查 Network 面板的资源加载状态
4. 验证页面参数：URL 中的 `page_name` 参数
5. 重新编译相关 bundle 文件

</details>

### 🛠️ 开发环境

<details>
<summary><b>❌ 热重载不工作</b></summary>

**症状：** 修改代码后页面不自动刷新

**可能原因：**
- Webpack Dev Server 配置问题
- 端口被占用
- 浏览器缓存过强

**解决方案：**
1. 重启开发服务器：停止后重新 `npm run dev`
2. 检查端口占用：`lsof -i :8080`
3. 修改端口：在 `webpack.config.js` 中配置
4. 禁用浏览器缓存：开发者工具 → Network → Disable cache

</details>

### 💡 快速诊断

运行以下命令进行快速诊断：

```bash
# 检查 manifest.json
cat src/bundles/manifest.json

# 列出所有 bundle 文件
ls -lh src/bundles/*.bundle.js

# 检查核心库文件
ls -lh src/libs/

# 重新生成所有文件
npm run rebuild-kotlin && npm run build-bundles:all
# 或（开发环境）：npm run rebuild-kotlin:dev && npm run build-bundles:all:dev
```

## 💡 性能优化建议

### 🚀 编译优化

| 优化项 | 方法 | 效果 |
|--------|------|------|
| **增量编译** | `npm run build-bundles:dev -- PageName` / `npm run build-bundles -- PageName` | 只编译修改的页面 |
| **并行开发** | 核心库和页面分别编译 | 互不影响，提升效率 |
| **开发模式** | `npm run rebuild-kotlin:dev` | 包含 source map，便于调试 |

### 📦 Bundle 优化

| 优化项 | 说明 | 启用方式 |
|--------|------|---------|
| **代码分包** | Webpack 自动分包 | 已默认启用 |
| **长期缓存** | 基础库文件名固定 | 已配置 |
| **并行加载** | 多个 bundle 并行下载 | `enableParallelLoading: true` |
| **按需加载** | 只加载当前页面所需 bundle | 已实现 |

### 🔧 开发体验

| 优化项 | 方法 | 效果 |
|--------|------|------|
| **热重载** | 修改 JS 代码自动生效 | 无需手动刷新 |
| **TypeScript** | 使用 `.d.ts` 类型定义 | IDE 智能提示 |
| **错误提示** | 完善的日志系统 | 快速定位问题 |

## 📚 自定义扩展

### 自定义组件
- `KRMyView.js`：自定义视图示例
- `KRWebView.js`：WebView 组件
- `KuiklyPageView.js`：页面容器
- `KuiklyRenderView.js`：业务封装类，注册模块

### 自定义模块
- `KRBridgeModule.js`：桥接通信
- `KRCacheModule.js`：缓存管理

## 🐛 调试

浏览器控制台日志前缀：
- `[Delegate]`：委托回调
- `[Module]`：模块注册
- `[View]`：视图注册
- `[KuiklyWebRenderViewDelegator]`：委托器

## 📚 技术细节

### 导出的关键类（通过 @JsExport）

| 类名 | 模块 | 用途 |
|------|------|------|
| `KuiklyRenderViewDelegator` | h5 | 主入口类，管理渲染生命周期 |
| `KuiklyRenderViewDelegatorDelegate` | base | 委托接口定义 |
| `KuiklyRenderCoreExecuteMode` | base | 执行模式枚举（WEB/JS） |
| `ErrorReason` | base | 错误原因枚举 |
| `KRMonitorType` | base | 性能监控类型枚举 |

### Webpack 自动复制配置

`webpack.config.js` 已配置自动复制 Kotlin 库：
```javascript
new CopyWebpackPlugin({
  patterns: [
    { from: 'src/libs', to: 'libs' }
  ]
})
```

### 一键更新脚本

修改 Kotlin 代码后运行：
```bash
cd h5App-js
./scripts/rebuild-kotlin-and-copy.sh
```

此脚本包含以下步骤：
1. 编译 `core-render-web:h5` 模块（已包含 base 模块代码）
2. 复制所有编译产物（包括 `.d.ts` 类型定义文件）
