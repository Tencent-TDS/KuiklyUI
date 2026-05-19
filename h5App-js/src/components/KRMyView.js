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
