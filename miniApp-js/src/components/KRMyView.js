/**
 * Custom MyView Component (JavaScript Implementation)
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
 * 获取 Kotlin MiniSpanElement 类
 */
function getMiniSpanElement() {
  const g = getGlobal();
  if (g.com && g.com.tencent && g.com.tencent.kuikly 
      && g.com.tencent.kuikly.core && g.com.tencent.kuikly.core.render
      && g.com.tencent.kuikly.core.render.web && g.com.tencent.kuikly.core.render.web.runtime
      && g.com.tencent.kuikly.core.render.web.runtime.miniapp
      && g.com.tencent.kuikly.core.render.web.runtime.miniapp.dom) {
    return g.com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniSpanElement;
  }
  return null;
}

/**
 * Custom MyView 组件
 */
export class KRMyView {
  static VIEW_NAME = 'KRMyView';
  static MESSAGE = 'message';
  static MY_PROP = 'myProp';

  constructor() {
    const MiniDivElement = getMiniDivElement();
    const MiniSpanElement = getMiniSpanElement();
    
    if (!MiniDivElement || !MiniSpanElement) {
      console.error('[KRMyView] Kotlin MiniDivElement or MiniSpanElement not found');
      this._div = null;
      this._innerText = null;
      return;
    }

    this._div = new MiniDivElement();
    this._innerText = new MiniSpanElement();
    this._div.appendChild(this._innerText);

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
    if (!this._innerText) return false;

    switch (propKey) {
      case KRMyView.MESSAGE:
        this._innerText.textContent = String(propValue);
        return true;
      case KRMyView.MY_PROP:
        this._innerText.textContent = String(propValue);
        if (typeof this._innerText.updateUiText === 'function') {
          this._innerText.updateUiText();
        }
        console.log(KRMyView.MY_PROP, ': is', propValue);
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
    if (!this._innerText) return false;

    switch (propKey) {
      case KRMyView.MESSAGE:
      case KRMyView.MY_PROP:
        this._innerText.textContent = '';
        return true;
      default:
        return false;
    }
  }
}
