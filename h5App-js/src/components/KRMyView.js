/**
 * Custom MyView (JavaScript Implementation)
 * Equivalent to KRMyView.kt
 */

export class KRMyView {
  static VIEW_NAME = 'KRMyView';
  static MESSAGE = 'message';

  constructor() {
    this.div = document.createElement('div');
    this.div.classList.add('kr-my-view');
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
      default:
        return false;
    }
  }
}
