/**
 * Kuikly Render - 统一入口文件
 * 
 * 将 KuiklyCore-render-web-miniapp.js (Kotlin 编译产物) 和 miniprogramApp (JS 业务代码) 
 * 打包成一个文件，简化小程序端的使用
 */

// ========== 第一步：设置全局对象兼容 ==========
if (typeof globalThis === 'undefined') {
  global.globalThis = global;
}
if (typeof window === 'undefined') {
  global.window = global;
}

// ========== 第二步：加载 Kotlin 渲染层代码 ==========
var kuiklyRenderWeb = require('./libs/KuiklyCore-render-web-miniapp.js');

// 将 Kotlin 模块挂载到全局
if (kuiklyRenderWeb && kuiklyRenderWeb.com) {
  global.com = kuiklyRenderWeb.com;
  globalThis.com = kuiklyRenderWeb.com;
}
if (kuiklyRenderWeb && kuiklyRenderWeb.kotlin) {
  global.kotlin = kuiklyRenderWeb.kotlin;
  globalThis.kotlin = kuiklyRenderWeb.kotlin;
}

// ========== 第三步：导出业务 API ==========
var { initApp, renderView } = require('./index.js');

module.exports = {
  initApp: initApp,
  renderView: renderView,
  // 也导出原始的 Kotlin 模块，方便高级用法
  kuiklyRenderWeb: kuiklyRenderWeb
};
