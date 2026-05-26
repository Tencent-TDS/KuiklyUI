/**
 * miniApp-js 入口文件
 * 
 * 简化版：只需加载一个统一的 kuiklyRender.js
 */

// ========== 第一步：初始化全局环境（关键！）==========
// 确保 globalThis 存在，供 Webpack 运行时使用
if (typeof globalThis === 'undefined') {
  global.globalThis = global;
}

// ========== 第二步：加载 Kuikly 渲染库（包含 Kotlin 层和 JS 层）==========
var kuiklyRender = require('./lib/kuiklyRender.js');

// 保存到 global 供页面使用
global.render = kuiklyRender;

console.log('[app.js] kuiklyRender loaded');
console.log('[app.js] render.initApp:', typeof kuiklyRender.initApp);
console.log('[app.js] render.renderView:', typeof kuiklyRender.renderView);

// ========== 第二步：加载业务代码公共库 ==========
// 从 manifest.js 读取需要加载的公共文件
// 注意：小程序不支持 require JSON 文件，所以使用 .js 文件
var manifest = require('./business/manifest.js');

console.log('[app.js] Manifest loaded, version:', manifest.version);
console.log('[app.js] Common files count:', manifest.fileCount);
console.log('[app.js] Pages count:', manifest.pageCount);

// 加载所有公共库文件（runtime, kotlin-stdlib 等）
if (manifest.files && manifest.files.length > 0) {
  manifest.files.forEach(function(filename) {
    try {
      console.log('[app.js] Loading common file:', filename);
      require('./business/' + filename);
    } catch (e) {
      console.error('[app.js] Failed to load common file:', filename, e);
    }
  });
  console.log('[app.js] Common bundles loaded');
} else {
  console.warn('[app.js] No common files found in manifest');
}

// ========== 第三步：页面 Bundle 按需加载机制 ==========
// 每个页面的 bundle 文件实际上是一个入口点，会初始化该页面
// 不能同时加载多个页面 bundle，否则会相互覆盖初始化状态
// 
// 解决方案：根据当前页面路径，动态加载对应的 bundle
var loadedPages = {};  // 记录已加载的页面
var pageManifest = manifest.pages || {};  // 页面映射表

// 将页面名称映射表保存到 global，供调试使用
global.pageManifest = pageManifest;

/**
 * 加载页面 Bundle
 * @param {string} pageName - 页面名称（如 "router"）
 */
global.loadPageBundle = function(pageName) {
  if (loadedPages[pageName]) {
    console.log('[app.js] Page bundle already loaded:', pageName);
    return;
  }
  
  // 从 manifest 中查找对应的 bundle 文件名
  var bundleFilename = pageManifest[pageName];
  
  if (!bundleFilename) {
    console.error('[app.js] Page not found in manifest:', pageName);
    console.log('[app.js] Available pages:', Object.keys(pageManifest));
    return;
  }
  
  try {
    console.log('[app.js] Loading page bundle:', pageName, '→', bundleFilename);
    require('./business/' + bundleFilename);
    loadedPages[pageName] = true;
    console.log('[app.js] Page bundle loaded successfully:', pageName);
  } catch (e) {
    console.error('[app.js] Failed to load page bundle:', pageName, e);
  }
};

console.log('[app.js] Page lazy-loading mechanism initialized');
console.log('[app.js] Available pages:', Object.keys(pageManifest));

// 资源加载函数
global.getAssetJson = function(path) {
  var json = require('./assets/' + path.replace('.json','.js'));
  return json;
};

// 初始化应用
kuiklyRender.initApp();
