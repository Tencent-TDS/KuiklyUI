/**
 * Bridge Interface Module (JavaScript Implementation)
 * Equivalent to KRBridgeModule.kt
 * 
 * Bridge interface module used by business side
 */

import { UIUtils } from '@utils/UI';

export class KRBridgeModule {
  static MODULE_NAME = 'HRBridgeModule';

  /**
   * Call module method
   * @param {string} method - Method name
   * @param {string} params - JSON string params
   * @param {Function} callback - Callback function
   * @returns {*}
   */
  call(method, params, callback) {
    switch (method) {
      case 'toast':
        return this.toast(params);
      
      case 'log':
        console.log(params);
        return;
      
      case 'currentTimestamp':
        return this.currentTimestamp(params);
      
      case 'dateFormatter':
        return this.dateFormatter(params);
      
      case 'readAssetFile':
        return this.readAssetFile(params, callback);
      
      default:
        if (callback) {
          callback({
            code: -1,
            message: 'Method does not exist'
          });
        }
        return null;
    }
  }

  /**
   * Show toast message on page
   * @param {string} params - JSON string params
   */
  toast(params) {
    if (params) {
      try {
        const message = JSON.parse(params);
        UIUtils.showToast(message);
      } catch (e) {
        console.error('[KRBridgeModule] Toast JSON parse error:', e);
      }
    }
  }

  /**
   * Get current timestamp
   * @param {string} params - JSON string params
   * @returns {string}
   */
  currentTimestamp(params) {
    return Date.now().toString();
  }

  /**
   * Format date
   * @param {Date} date - Date object
   * @param {string} format - Format string (e.g., 'yyyy-MM-dd HH:mm:ss')
   * @returns {string}
   */
  formatDate(date, format) {
    const pad = (num) => num.toString().padStart(2, '0');
    
    const replacements = {
      'yyyy': date.getFullYear().toString(),
      'MM': pad(date.getMonth() + 1),
      'dd': pad(date.getDate()),
      'HH': pad(date.getHours()),
      'mm': pad(date.getMinutes()),
      'ss': pad(date.getSeconds())
    };
    
    let result = format;
    for (const [key, value] of Object.entries(replacements)) {
      result = result.replace(key, value);
    }
    
    return result;
  }

  /**
   * Date formatter
   * @param {string} params - JSON string params
   * @returns {string}
   */
  dateFormatter(params) {
    try {
      const paramObj = JSON.parse(params || '{}');
      const timestamp = paramObj.timeStamp || Date.now();
      const format = paramObj.format || 'yyyy-MM-dd HH:mm:ss';
      const date = new Date(timestamp);
      return this.formatDate(date, format);
    } catch (e) {
      console.error('[KRBridgeModule] Date formatter error:', e);
      return '';
    }
  }

  /**
   * Read asset file
   * @param {string} params - JSON string params
   * @param {Function} callback - Callback function
   */
  async readAssetFile(params, callback) {
    try {
      const paramObj = JSON.parse(params);
      const assetPath = paramObj.assetPath;
      const url = `${window.location.protocol}//${window.location.host}/assets/${assetPath}`;
      
      const response = await fetch(url);
      const data = await response.json();
      
      if (callback) {
        callback({
          result: JSON.stringify(data)
        });
      }
    } catch (e) {
      console.error('[KRBridgeModule] Read asset file error:', e);
      if (callback) {
        callback({
          code: -1,
          message: e.message
        });
      }
    }
  }
}
