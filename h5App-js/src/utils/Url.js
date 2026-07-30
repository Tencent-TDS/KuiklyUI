/**
 * URL Utilities (JavaScript Implementation)
 * Equivalent to URL.kt
 * 
 * Unified URL operation encapsulation
 */

export class URLUtils {
  /**
   * Format and return URL parameters
   * @param {string} url - URL string
   * @returns {Object} - Parameters object
   */
  static parseParams(url) {
    const params = {};
    
    if (url.includes('?')) {
      const query = url.split('?')[1];
      
      if (query) {
        // Only process if there are query parameters
        query.split('&').forEach(param => {
          const parts = param.split('=');
          if (parts.length === 2) {
            const name = parts[0];
            const value = decodeURIComponent(parts[1]);
            params[name] = value;
          }
        });
      }
    }
    
    return params;
  }

  /**
   * Build URL with parameters
   * @param {string} baseUrl - Base URL
   * @param {Object} params - Parameters object
   * @returns {string} - URL with parameters
   */
  static buildUrl(baseUrl, params) {
    if (!params || Object.keys(params).length === 0) {
      return baseUrl;
    }
    
    const queryString = Object.entries(params)
      .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
      .join('&');
    
    const separator = baseUrl.includes('?') ? '&' : '?';
    return `${baseUrl}${separator}${queryString}`;
  }

  /**
   * Get query parameter by name
   * @param {string} name - Parameter name
   * @param {string} url - URL string (defaults to current URL)
   * @returns {string|null} - Parameter value or null
   */
  static getQueryParam(name, url = window.location.href) {
    const params = URLUtils.parseParams(url);
    return params[name] || null;
  }

  /**
   * Update query parameter
   * @param {string} name - Parameter name
   * @param {string} value - Parameter value
   * @param {string} url - URL string (defaults to current URL)
   * @returns {string} - Updated URL
   */
  static updateQueryParam(name, value, url = window.location.href) {
    const [baseUrl] = url.split('?');
    const params = URLUtils.parseParams(url);
    params[name] = value;
    return URLUtils.buildUrl(baseUrl, params);
  }

  /**
   * Remove query parameter
   * @param {string} name - Parameter name
   * @param {string} url - URL string (defaults to current URL)
   * @returns {string} - Updated URL
   */
  static removeQueryParam(name, url = window.location.href) {
    const [baseUrl] = url.split('?');
    const params = URLUtils.parseParams(url);
    delete params[name];
    return URLUtils.buildUrl(baseUrl, params);
  }
}
