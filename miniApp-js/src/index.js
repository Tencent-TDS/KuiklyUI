/**
 * Kuikly Mini App - Main Entry (JavaScript Implementation)
 * 
 * 直接依赖 KuiklyCore-render-web-miniapp.js 中的 Kotlin 编译产物
 * 注意：只有标记了 @JsExport 的 Kotlin 类才能从 JS 访问
 * 目前只有 MiniDocument 有 @JsExport
 */

import { KuiklyMiniAppRenderViewDelegator } from './KuiklyMiniAppRenderViewDelegator';

const TAG = 'Main';

/**
 * 获取全局对象
 */
function getGlobal() {
  if (typeof global !== 'undefined') return global;
  if (typeof globalThis !== 'undefined') return globalThis;
  if (typeof window !== 'undefined') return window;
  return this;
}

/**
 * 获取 Kotlin MiniDocument
 * 注意：必须在运行时获取，不能缓存，因为 Kotlin 模块可能在之后加载
 */
function getKotlinMiniDocument() {
  var g = getGlobal();
  try {
    return g.com.tencent.kuikly.core.render.web.runtime.miniapp.MiniDocument;
  } catch (e) {
    return null;
  }
}

/**
 * 初始化小程序 App
 * 在小程序 app.js 中调用
 * 注意：Kotlin App 类没有 @JsExport，无法直接调用
 * 这里使用微信小程序原生 App() 方法
 */
function initApp(options) {
  options = options || {};
  
  // 直接使用微信小程序的 App() 方法
  // Kotlin 的 App.initApp 内部也是调用微信的 App()
  App({
    onLaunch: function(params) {
      console.log(TAG, 'app launch');
      if (typeof options.onLaunch === 'function') {
        options.onLaunch(params);
      }
    },
    onShow: function(params) {
      console.log(TAG, 'app show');
      if (typeof options.onShow === 'function') {
        options.onShow(params);
      }
    },
    onHide: function(params) {
      console.log(TAG, 'app hide');
      if (typeof options.onHide === 'function') {
        options.onHide(params);
      }
    },
    onError: function(params) {
      console.error(TAG, 'app error:', params);
      if (typeof options.onError === 'function') {
        options.onError(params);
      }
    },
    onPageNotFound: function(params) {
      console.warn(TAG, 'page not found:', params);
      if (typeof options.onPageNotFound === 'function') {
        options.onPageNotFound(params);
      }
    }
  });
}

/**
 * 小程序页面入口
 * 使用 renderView 代理方法初始化和创建 renderView
 * @param {Object} json - 渲染参数
 */
function renderView(json) {
  json = json || {};
  
  // ========== 步骤 1：按需加载页面 Bundle ==========
  // 每个页面的 bundle 包含页面初始化逻辑，必须在 renderView 之前加载
  var pageName = json.pageName;
  if (pageName) {
    var g = getGlobal();
    if (typeof g.loadPageBundle === 'function') {
      console.log('[renderView] Loading page bundle for:', pageName);
      g.loadPageBundle(pageName);
    } else {
      console.warn('[renderView] global.loadPageBundle not found, page bundle may not be loaded');
    }
  }
  
  // ========== 步骤 2：获取 Kotlin MiniDocument ==========
  var KotlinMiniDocument = getKotlinMiniDocument();
  if (!KotlinMiniDocument) {
    console.error('[renderView] Kotlin MiniDocument not found! Make sure KuiklyCore-render-web-miniapp.js is loaded.');
    return;
  }

  // ========== 步骤 3：准备渲染参数 ==========
  // 视图尺寸 - 直接使用数字，不需要创建 Kotlin Pair
  var width = json.width != null ? json.width : null;
  var height = json.height != null ? json.height : null;

  // 构建渲染参数
  var renderParams = {};
  if (pageName) {
    renderParams.pageName = pageName;
  }

  // ========== 步骤 4：初始化页面 ==========
  // 使用 Kotlin MiniDocument.initPage 初始化 MiniPage
  KotlinMiniDocument.initPage(renderParams, function(pageId, returnedPageName, paramsMap) {
    console.log('[renderView] Page initialized: ' + returnedPageName + ', pageId: ' + pageId);
    
    // 创建 delegator 并 attach - 传递 width 和 height 作为单独参数
    var delegator = new KuiklyMiniAppRenderViewDelegator();
    delegator.onAttach(pageId, returnedPageName, paramsMap, width, height);
  });
}

// 导出给小程序使用
export { initApp, renderView };
