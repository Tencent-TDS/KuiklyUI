/**
 * Cache Module (JavaScript Implementation)
 * Equivalent to KRCacheModule.kt
 * 
 * Cache module for localStorage operations
 */

export class KRCacheModule {
  static MODULE_NAME = 'HRCacheModule';
  static GET_ITEM = 'getItem';
  static SET_ITEM = 'setItem';

  /**
   * Call module method
   * @param {string} method - Method name
   * @param {string} params - JSON string params
   * @param {Function} callback - Callback function
   * @returns {*}
   */
  call(method, params, callback) {
    switch (method) {
      case KRCacheModule.GET_ITEM:
        return this.getItem(params);
      
      case KRCacheModule.SET_ITEM:
        return this.setItem(params);
      
      default:
        console.warn(`[KRCacheModule] Unknown method: ${method}`);
        return null;
    }
  }

  /**
   * Get content from localStorage cache
   * @param {string} key - Cache key
   * @returns {string|null}
   */
  getItem(key) {
    if (!key) {
      return null;
    }
    
    try {
      return window.localStorage.getItem(key);
    } catch (e) {
      console.error('[KRCacheModule] localStorage get error:', e);
      return '';
    }
  }

  /**
   * Set localStorage cache
   * @param {string} params - JSON string params with key and value
   */
  setItem(params) {
    try {
      const json = JSON.parse(params || '{}');
      const key = json.key || '';
      const value = json.value || '';
      
      if (key) {
        window.localStorage.setItem(key, value);
      }
    } catch (e) {
      console.error('[KRCacheModule] localStorage set error:', e);
    }
  }

  /**
   * Remove item from localStorage
   * @param {string} key - Cache key
   */
  removeItem(key) {
    if (!key) {
      return;
    }
    
    try {
      window.localStorage.removeItem(key);
    } catch (e) {
      console.error('[KRCacheModule] localStorage remove error:', e);
    }
  }

  /**
   * Clear all localStorage
   */
  clear() {
    try {
      window.localStorage.clear();
    } catch (e) {
      console.error('[KRCacheModule] localStorage clear error:', e);
    }
  }
}
