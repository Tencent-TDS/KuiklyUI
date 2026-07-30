/**
 * Cache Module (JavaScript Implementation)
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
 * Cache 模块
 */
export class KRCacheModule {
  static MODULE_NAME = 'HRCacheModule';
  static GET_ITEM = 'getItem';
  static SET_ITEM = 'setItem';

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
   * 从缓存获取内容
   * @param {string} key - 缓存键
   * @returns {string|null}
   */
  getItem(key) {
    if (!key) {
      return null;
    }

    try {
      const NativeApi = getNativeApi();
      if (NativeApi && NativeApi.getStorageSync) {
        return NativeApi.getStorageSync(key);
      }
      
      // 回退到 wx API
      const g = getGlobal();
      if (g.wx && g.wx.getStorageSync) {
        return g.wx.getStorageSync(key);
      }
      
      return null;
    } catch (e) {
      console.error('[KRCacheModule] storage get error:', e);
      return '';
    }
  }

  /**
   * 设置缓存
   * @param {string} params - JSON 字符串参数，包含 key 和 value
   */
  setItem(params) {
    try {
      const json = JSON.parse(params || '{}');
      const key = json.key || '';
      const value = json.value || '';

      if (key) {
        const NativeApi = getNativeApi();
        if (NativeApi && NativeApi.setStorageSync) {
          NativeApi.setStorageSync(key, value);
          return;
        }
        
        // 回退到 wx API
        const g = getGlobal();
        if (g.wx && g.wx.setStorageSync) {
          g.wx.setStorageSync(key, value);
        }
      }
    } catch (e) {
      console.error('[KRCacheModule] storage set error:', e);
    }
  }

  /**
   * 从缓存移除项
   * @param {string} key - 缓存键
   */
  removeItem(key) {
    if (!key) {
      return;
    }

    try {
      const NativeApi = getNativeApi();
      if (NativeApi && NativeApi.removeStorageSync) {
        NativeApi.removeStorageSync(key);
        return;
      }
      
      // 回退到 wx API
      const g = getGlobal();
      if (g.wx && g.wx.removeStorageSync) {
        g.wx.removeStorageSync(key);
      }
    } catch (e) {
      console.error('[KRCacheModule] storage remove error:', e);
    }
  }

  /**
   * 清理模块资源 (IKuiklyRenderModuleExport 接口要求)
   */
  onDestroy() {
    // 清理工作（如果需要）
  }
}
