/**
 * Kuikly Page View (JavaScript Implementation)
 * Equivalent to KuiklyPageView.kt
 */

import { KuiklyRenderView } from './KuiklyRenderView';

export class KuiklyPageView {
  static VIEW_NAME = 'KuiklyPageView';

  constructor() {
    this.kuiklyRenderView = null;
    this.pageName = '';
    this.pageData = '{}';
    this.loadSuccessCallback = null;
    this.loadFailureCallback = null;
    this.lazyEvents = [];
    this.divElement = document.createElement('div');
    this.divElement.classList.add('kuikly-page-view');
  }

  /**
   * Get the HTML element
   * @returns {HTMLDivElement}
   */
  get ele() {
    return this.divElement;
  }

  /**
   * Set load success callback
   * @param {Function} callback
   * @returns {boolean}
   */
  setLoadSuccessCallback(callback) {
    this.loadSuccessCallback = callback;
    return true;
  }

  /**
   * Set load failure callback
   * @param {Function} callback
   * @returns {boolean}
   */
  setLoadFailureCallback(callback) {
    this.loadFailureCallback = callback;
    return true;
  }

  /**
   * Perform task when Kuikly view is loaded
   * @param {Function} callback
   */
  performTaskWhenKuiklyViewDidLoad(callback) {
    if (this.kuiklyRenderView !== null) {
      callback();
    } else {
      this.lazyEvents.push(callback);
    }
  }

  /**
   * Send event with params
   * @param {string} params - JSON string params
   */
  sendEventWithParams(params) {
    try {
      const json = JSON.parse(params || '{}');
      const event = json.event || '';
      const data = json.data || {};
      
      this.performTaskWhenKuiklyViewDidLoad(() => {
        if (this.kuiklyRenderView) {
          this.kuiklyRenderView.sendEvent(event, data);
        }
      });
    } catch (e) {
      console.error('[KuiklyPageView] Failed to parse event params:', e);
    }
  }

  /**
   * Get host page data
   * @returns {Object}
   */
  getHostPageData() {
    return {};
  }

  /**
   * Perform all lazy tasks
   */
  performAllLazyTasks() {
    this.lazyEvents.forEach(task => task());
    this.lazyEvents = [];
  }

  /**
   * Initialize Kuikly view if needed
   */
  initKuiklyViewIfNeeded() {
    if (this.kuiklyRenderView !== null) {
      return;
    }

    // Container size
    const containerWidth = window.innerWidth;
    const containerHeight = window.innerHeight;

    if (this.pageName) {
      const hostPageData = { ...this.getHostPageData() };
      
      try {
        const pageDataObj = JSON.parse(this.pageData);
        Object.assign(hostPageData, pageDataObj);
      } catch (e) {
        console.error('[KuiklyPageView] Failed to parse page data:', e);
      }

      this.kuiklyRenderView = new KuiklyRenderView(this);
      
      // After view initialization is complete, create container
      this.kuiklyRenderView.onAttach(
        this.ele,
        this.pageName,
        {},
        { width: containerWidth, height: containerHeight }
      );

      // Execute delayed tasks
      this.performAllLazyTasks();
    }
  }

  /**
   * Called when added to parent
   * @param {HTMLElement} parent
   */
  onAddToParent(parent) {
    console.log('[KuiklyPageView] Added to parent');
    // Page load complete, start loading View
    this.initKuiklyViewIfNeeded();
  }

  /**
   * Set property
   * @param {string} propKey - Property key
   * @param {*} propValue - Property value
   * @returns {boolean} - Whether the property was handled
   */
  setProp(propKey, propValue) {
    switch (propKey) {
      case 'loadSuccess':
        return this.setLoadSuccessCallback(propValue);
      case 'loadFailure':
        return this.setLoadFailureCallback(propValue);
      case 'pageName':
        this.pageName = propValue;
        return true;
      case 'pageData':
        this.pageData = propValue;
        return true;
      default:
        return false;
    }
  }

  /**
   * Call method
   * @param {string} method - Method name
   * @param {string} params - JSON string params
   * @param {Function} callback - Callback function
   * @returns {*}
   */
  call(method, params, callback) {
    switch (method) {
      case 'sendEvent':
        return this.sendEventWithParams(params);
      default:
        console.warn(`[KuiklyPageView] Unknown method: ${method}`);
        return null;
    }
  }
}
