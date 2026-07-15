/**
 * Kuikly UMD Deep-Merge Patch (业务侧一次性方案 Y)
 *
 * 背景：
 *   Kuikly >= 2.19.0 的 core-render-web:base / core-render-web:h5 里新增了
 *   若干 `@file:JsExport` 顶层文件（KuiklyView / IKuiklyView / KuiklyRenderViewDelegator /
 *   JSHelper 及 base 里的 8 个 export 文件），使得 h5App.js 打包产物的 exports
 *   顶层出现 `com` 键，且分支只有 `com.tencent.kuikly.core.render.web.*`，
 *   缺失 `com.tencent.kuikly.core.nvi`。
 *
 *   而 kotlin-webpack 生成的 UMD 头默认是：
 *
 *     var a = factory();
 *     for (var i in a) (typeof exports === 'object' ? exports : root)[i] = a[i];
 *
 *   这一段会把 h5App.js 产物的 `com` 整体赋给 `window.com`，从而抹掉
 *   nativevue2.js 之前挂载的 `window.com.tencent.kuikly.core.nvi.registerCallNative`，
 *   导致 KuiklyRenderContextHandler.init() 报 undefined、桥接失败。
 *
 *   该问题仅在“多模块工程”（enableMultiModule = true + JSMultiEntryBuilder，
 *   业务 shared 与 h5App 分别产出 nativevue2.js / h5App.js 两个 KMM webpack 产物）
 *   下才会出现；单模块工程只有一个产物，所有 @JsExport 分支合并到同一棵树中，
 *   不存在覆盖问题。
 *
 * 修复思路：
 *   保留 UMD 头的其它逻辑，仅把最后那段“逐 key 覆盖挂全局”替换为“逐 key
 *   深合并挂全局”——针对已经存在于 root 的对象 key，做深度合并而不是整体
 *   替换。这样 nativevue2.js 与 h5App.js 各自挂到 window.com 的分支就能
 *   共存，`com.tencent.kuikly.core.nvi` 分支不再被覆盖。
 *
 * 与 output.js 的关系（两套方案二选一）：
 *   本目录下的 output.js 采用的是“方案 X”：直接把 h5App.js 的 libraryTarget
 *   置空、走 iife: true，让 h5App.js 不再暴露任何 UMD exports，也就不会
 *   触碰 window.com。方案 X 更彻底，是当前默认启用的方案。
 *
 *   本文件（方案 Y）保留 UMD 输出、但在 emit 前重写 UMD 尾部，改为深合并。
 *   适用于以下场景：
 *     1) 某些集成方要求 h5App.js 仍以 UMD 方式对外暴露 KuiklyView 等符号；
 *     2) 由于历史原因无法关闭 UMD wrapper；
 *     3) 想同时保护 nativevue2.js（shared 模块）产物也做类似合并。
 *
 *   如启用本方案，请同时把 output.js 中 `iife`/`libraryTarget` 相关行注释
 *   掉，避免 UMD 尾部被提前抹掉、导致本 patch 的正则匹配不到而失效。
 *
 * 说明：
 *   这个 patch 只影响 UMD 尾部的“挂全局”分支（浏览器 <script> 场景），
 *   CommonJS / AMD 两个分支保持不变。改动是纯字符串替换，不改任何 chunk。
 */
/*
config.plugins = config.plugins || [];

config.plugins.push({
    apply: function (compiler) {
        compiler.hooks.thisCompilation.tap("KuiklyUmdDeepMergePatch", function (compilation) {
            var stage =
                (compiler.webpack &&
                    compiler.webpack.Compilation &&
                    compiler.webpack.Compilation.PROCESS_ASSETS_STAGE_REPORT) ||
                5000;

            compilation.hooks.processAssets.tap(
                {
                    name: "KuiklyUmdDeepMergePatch",
                    stage: stage,
                },
                function (assets) {
                    var RawSource =
                        (compiler.webpack &&
                            compiler.webpack.sources &&
                            compiler.webpack.sources.RawSource) ||
                        require("webpack-sources").RawSource;

                    // UMD 头默认生成的关键片段（webpack 5 UmdLibraryPlugin 的模板）
                    // 完整匹配包含缩进的两行代码：
                    //     var a = factory();
                    //     for(var i in a) (typeof exports === 'object' ? exports : root)[i] = a[i];
                    // 用宽松正则匹配（兼容不同缩进/空白）。
                    var umdTailRegex =
                        /var\s+a\s*=\s*factory\(\)\s*;\s*[\r\n\t ]*for\s*\(\s*var\s+i\s+in\s+a\s*\)\s*\(\s*typeof\s+exports\s*===\s*['"]object['"]\s*\?\s*exports\s*:\s*root\s*\)\s*\[\s*i\s*\]\s*=\s*a\s*\[\s*i\s*\]\s*;/;

                    // 深合并版：只对 root 侧生效（非 CJS 分支）；如果新 value 是
                    // plain object 且 root 上已存在 plain object，则做递归合并，
                    // 否则维持原有整体赋值语义。
                    var deepMergeSnippet = [
                        "var a = factory();",
                        "var __kuiklyIsPlainObj = function(v){ return v !== null && typeof v === 'object' && !Array.isArray(v); };",
                        "var __kuiklyDeepAssign = function(target, source){",
                        "  if (!__kuiklyIsPlainObj(target) || !__kuiklyIsPlainObj(source)) return source;",
                        "  for (var k in source) {",
                        "    if (!Object.prototype.hasOwnProperty.call(source, k)) continue;",
                        "    var sv = source[k];",
                        "    var tv = target[k];",
                        "    if (__kuiklyIsPlainObj(sv) && __kuiklyIsPlainObj(tv)) {",
                        "      __kuiklyDeepAssign(tv, sv);",
                        "    } else if (typeof tv === 'undefined') {",
                        "      target[k] = sv;",
                        "    }",
                        "    // 已有非对象值时，保留旧值，避免抹掉已挂载的桥接函数",
                        "  }",
                        "  return target;",
                        "};",
                        "for (var i in a) {",
                        "  var __kuiklyDst = typeof exports === 'object' ? exports : root;",
                        "  var __kuiklyExisting = __kuiklyDst[i];",
                        "  var __kuiklyIncoming = a[i];",
                        "  if (__kuiklyIsPlainObj(__kuiklyExisting) && __kuiklyIsPlainObj(__kuiklyIncoming)) {",
                        "    __kuiklyDeepAssign(__kuiklyExisting, __kuiklyIncoming);",
                        "  } else {",
                        "    __kuiklyDst[i] = __kuiklyIncoming;",
                        "  }",
                        "}",
                    ].join("\n                ");

                    Object.keys(assets).forEach(function (name) {
                        if (!/\.js$/.test(name)) return;
                        var asset = compilation.getAsset(name);
                        if (!asset) return;
                        var src = asset.source.source();
                        if (typeof src !== "string") return;
                        if (!umdTailRegex.test(src)) return;
                        var patched = src.replace(umdTailRegex, deepMergeSnippet);
                        compilation.updateAsset(name, new RawSource(patched));
                        console.log(
                            "[KuiklyUmdDeepMergePatch] Patched UMD tail in " + name
                        );
                    });
                }
            );
        });
    },
});
*/