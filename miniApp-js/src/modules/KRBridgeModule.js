/**
 * Bridge Interface Module (JavaScript Implementation)
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
 * 获取 Kotlin NativeApi 对象
 */
function getNativeApi() {
  const g = getGlobal();
  if (g.com && g.com.tencent && g.com.tencent.kuikly 
      && g.com.tencent.kuikly.core && g.com.tencent.kuikly.core.render
      && g.com.tencent.kuikly.core.render.web && g.com.tencent.kuikly.core.render.web.runtime
      && g.com.tencent.kuikly.core.render.web.runtime.miniapp
      && g.com.tencent.kuikly.core.render.web.runtime.miniapp.core) {
    return g.com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi;
  }
  return null;
}

/**
 * Bridge 模块
 */
export class KRBridgeModule {
  static MODULE_NAME = 'HRBridgeModule';

  constructor() {
    // Required by IKuiklyRenderModuleExport interface
    this.kuiklyRenderContext = null;
  }

  /**
   * Kotlin 调用入口（通过 @JsName("_call") 映射）
   * @param {string} method - 方法名
   * @param {*} params - 参数（可以是任意类型）
   * @param {Function} callback - 回调函数
   * @returns {*}
   */
  _call(method, params, callback) {
    return this.call(method, params, callback);
  }

  /**
   * 调用模块方法
   * @param {string} method - 方法名
   * @param {string} params - JSON 字符串参数
   * @param {Function} callback - 回调函数
   * @returns {*}
   */
  call(method, params, callback) {
    switch (method) {
      case 'toast':
        return this.showToast(params);

      case 'log':
        if (params) {
          console.log(params);
        }
        return;

      case 'readAssetFile':
        return this.readAssetFile(params, callback);

      case 'currentTimestamp':
        return this.currentTimestamp(params);

      case 'dateFormatter':
        return this.dateFormatter(params);

      default:
        console.error(`${method} not found`);
        if (callback) {
          callback('{}');
        }
        return null;
    }
  }

  /**
   * 显示 toast 消息
   * @param {string} params - JSON 字符串参数
   */
  showToast(params) {
    if (!params) {
      return;
    }

    try {
      const data = JSON.parse(params);
      
      const iconMap = {
        1: 'success',
        2: 'error',
        3: 'none',
      };
      const icon = iconMap[data.mode] || 'none';

      const NativeApi = getNativeApi();
      if (NativeApi && NativeApi.showToast) {
        NativeApi.showToast({
          title: data.content || '',
          icon: icon,
        });
      } else {
        // 回退到 wx API
        const g = getGlobal();
        if (g.wx && g.wx.showToast) {
          g.wx.showToast({
            title: data.content || '',
            icon: icon,
          });
        }
      }
    } catch (e) {
      console.error('toast json parse error', e);
    }
  }

  /**
   * 读取 asset 文件
   * @param {string} params - JSON 字符串参数
   * @param {Function} callback - 回调函数
   */
  readAssetFile(params, callback) {
    try {
      const data = JSON.parse(params);
      const assetPath = data.assetPath;

      // 在小程序中使用 global.getAssetJson
      const g = getGlobal();
      if (typeof g.getAssetJson === 'function') {
        const jsonData = g.getAssetJson(assetPath);
        if (callback) {
          callback({
            result: JSON.stringify(jsonData),
          });
        }
      } else {
        if (callback) {
          callback({
            code: -1,
            message: 'getAssetJson not available',
          });
        }
      }
    } catch (e) {
      console.error('readAssetFile error', e);
      if (callback) {
        callback({
          code: -1,
          message: e.message,
        });
      }
    }
  }

  /**
   * 获取当前时间戳
   * @param {string} params - JSON 字符串参数
   * @returns {string}
   */
  currentTimestamp(params) {
    return Date.now().toString();
  }

  /**
   * 格式化日期
   * @param {Date} date - Date 对象
   * @param {string} format - 格式字符串
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
      'ss': pad(date.getSeconds()),
    };

    let result = format;
    for (const [key, value] of Object.entries(replacements)) {
      result = result.replace(key, value);
    }

    return result;
  }

  /**
   * 日期格式化器
   * @param {string} params - JSON 字符串参数
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
   * 清理模块资源 (IKuiklyRenderModuleExport 接口要求)
   */
  onDestroy() {
    // 清理工作（如果需要）
  }
}
