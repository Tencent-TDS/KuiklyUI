/**
 * Custom WebView Component (JavaScript Implementation)
 * Equivalent to KRWebView.kt
 */

export class KRWebView {
  static VIEW_NAME = 'KRWebView';
  static SRC = 'src';

  constructor() {
    this.iframe = document.createElement('iframe');
    this.iframe.classList.add('kr-web-view');
    this.iframe.style.width = '100%';
    this.iframe.style.height = '100%';
    this.iframe.style.border = 'none';
  }

  /**
   * Get the HTML element
   * @returns {HTMLIFrameElement}
   */
  get ele() {
    return this.iframe;
  }

  /**
   * Set property
   * @param {string} propKey - Property key
   * @param {*} propValue - Property value
   * @returns {boolean} - Whether the property was handled
   */
  setProp(propKey, propValue) {
    switch (propKey) {
      case KRWebView.SRC:
        this.ele.src = propValue;
        return true;
      default:
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
      case KRWebView.SRC:
        this.ele.src = '';
        return true;
      default:
        return false;
    }
  }
}
