/**
 * Custom WebView Component (JavaScript Implementation)
 * 
 * 直接依赖 KuiklyCore-render-web-miniapp.js 中的 Kotlin 编译产物
 */

/**
 * 获取全局对象
 */
function getGlobal() {
  return typeof global !== 'undefined' ? global : (typeof window !== 'undefined' ? window : this);
}

/**
 * 获取 Kotlin MiniDivElement 类
 */
function getMiniDivElement() {
  const g = getGlobal();
  if (g.com && g.com.tencent && g.com.tencent.kuikly 
      && g.com.tencent.kuikly.core && g.com.tencent.kuikly.core.render
      && g.com.tencent.kuikly.core.render.web && g.com.tencent.kuikly.core.render.web.runtime
      && g.com.tencent.kuikly.core.render.web.runtime.miniapp
      && g.com.tencent.kuikly.core.render.web.runtime.miniapp.dom) {
    return g.com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniDivElement;
  }
  return null;
}

/**
 * Custom WebView 组件
 * 使用 web-view 组件实现
 */
export class KRWebView {
  static VIEW_NAME = 'KRWebView';
  static NODE_NAME = 'web-view';
  static SRC = 'src';
  
  // 组件别名配置
  static componentsAlias = {
    'web-view': 'web-view'
  };

  constructor() {
    const MiniDivElement = getMiniDivElement();
    
    if (!MiniDivElement) {
      console.error('[KRWebView] Kotlin MiniDivElement not found');
      this._div = null;
      return;
    }

    // 创建一个 div 容器，实际的 web-view 由小程序原生渲染
    this._div = new MiniDivElement();
    this._src = '';

    // Required by IKuiklyRenderModuleExport interface
    this.kuiklyRenderContext = null;
  }

  /**
   * Kotlin 调用入口（通过 @JsName("_call") 映射）
   * 视图组件通常不需要实现 call 方法，但为了兼容性提供空实现
   */
  _call(method, params, callback) {
    return this.call ? this.call(method, params, callback) : null;
  }

  /**
   * 获取元素
   * @returns {Object}
   */
  get ele() {
    return this._div;
  }

  /**
   * 设置属性
   * @param {string} propKey - 属性键
   * @param {*} propValue - 属性值
   * @returns {boolean} - 是否处理了该属性
   */
  setProp(propKey, propValue) {
    if (!this._div) return false;

    switch (propKey) {
      case KRWebView.SRC:
        this._src = String(propValue);
        // 设置 web-view 的 src 属性
        if (this._div.setAttribute) {
          this._div.setAttribute('src', this._src);
        }
        return true;
      default:
        return false;
    }
  }

  /**
   * 重置属性
   * @param {string} propKey - 属性键
   * @returns {boolean} - 是否重置了该属性
   */
  resetProp(propKey) {
    if (!this._div) return false;

    switch (propKey) {
      case KRWebView.SRC:
        this._src = '';
        if (this._div.setAttribute) {
          this._div.setAttribute('src', '');
        }
        return true;
      default:
        return false;
    }
  }
}
