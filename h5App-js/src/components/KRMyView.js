/**
 * Custom MyView (JavaScript Implementation)
 * Equivalent to KRMyView.kt
 */

export class KRMyView {
  static VIEW_NAME = 'KRMyView';
  static MESSAGE = 'message';
  static TAP = 'tap';

  constructor() {
    this.div = document.createElement('div');
    this.div.classList.add('kr-my-view');
    this.tapCallback = null;
    this._bindEvents();
  }

  /**
   * Get the HTML element
   * @returns {HTMLElement}
   */
  get ele() {
    return this.div;
  }

  /**
   * Called when view is added to parent.
   * Keep this method for runtime interface compatibility.
   * @param {HTMLElement} parent
   */
  onAddToParent(parent) {
    // no-op
  }

  /**
   * Called when view is removed from parent.
   * Keep this method for runtime interface compatibility.
   * @param {HTMLElement} parent
   */
  onRemoveFromParent(parent) {
    // no-op
  }

  /**
   * Bind DOM events
   * @private
   */
  _bindEvents() {
    this.ele.addEventListener('click', (event) => {
      this._invokeCallback(this.tapCallback, {
        x: event.clientX,
        y: event.clientY
      });
    });
  }

  /**
   * Get exported Kotlin callback invoke bridge function.
   * @returns {Function|null}
   * @private
   */
  _getInvokeKuiklyRenderCallbackBridge() {
    const renderWebModule = window?.com?.tencent?.kuikly?.core?.render?.web;
    if (!renderWebModule) {
      return null;
    }

    if (typeof renderWebModule?.runtime?.web?.expand?.invokeKuiklyRenderCallback === 'function') {
      return renderWebModule.runtime.web.expand.invokeKuiklyRenderCallback;
    }

    if (typeof renderWebModule?.runtime?.expand?.invokeKuiklyRenderCallback === 'function') {
      return renderWebModule.runtime.expand.invokeKuiklyRenderCallback;
    }

    return null;
  }

  /**
   * Invoke callback through stable bridge.
   * Fallback to direct function call for plain JS callbacks.
   * @param {*} callback
   * @param {*} result
   * @returns {boolean}
   * @private
   */
  _invokeCallback(callback, result) {
    if (!callback) {
      return false;
    }

    const invokeBridge = this._getInvokeKuiklyRenderCallbackBridge();
    if (typeof invokeBridge === 'function') {
      try {
        return !!invokeBridge(callback, result);
      } catch (_) {
        return false;
      }
    }

    if (typeof callback === 'function') {
      try {
        callback(result);
        return true;
      } catch (_) {
        return false;
      }
    }

    return false;
  }

  /**
   * Set property
   * @param {string} propKey - Property key
   * @param {*} propValue - Property value
   * @returns {boolean} - Whether the property was handled
   */
  setProp(propKey, propValue) {
    switch (propKey) {
      case KRMyView.MESSAGE:
        this.ele.innerHTML = propValue;
        return true;
      case KRMyView.TAP:
        this.tapCallback = propValue;
        return true;
      default:
        return this._setCommonProp(propKey, propValue);
    }
  }

  /**
   * Fallback to common property processor exported from Kotlin runtime.
   * This mirrors Kotlin KRMyView's `else -> super.setProp(...)` behavior.
   *
   * @param {string} propKey
   * @param {*} propValue
   * @returns {boolean}
   * @private
   */
  _setCommonProp(propKey, propValue) {
    const renderWebModule = window?.com?.tencent?.kuikly?.core?.render?.web;
    const commonPropBridge =
      renderWebModule?.runtime?.web?.expand?.setCommonProp
      || renderWebModule?.runtime?.expand?.setCommonProp;

    if (typeof commonPropBridge !== 'function') {
      return false;
    }

    try {
      return !!commonPropBridge(this.ele, propKey, propValue);
    } catch (_) {
      return false;
    }
  }

  /**
   * Reset property
   * @param {string} propKey - Property key
   * @returns {boolean} - Whether the property was reset
   */
  resetProp(propKey) {
    switch (propKey) {
      case KRMyView.MESSAGE:
        this.ele.innerHTML = '';
        return true;
      case KRMyView.TAP:
        this.tapCallback = null;
        return true;
      default:
        return false;
    }
  }
}
