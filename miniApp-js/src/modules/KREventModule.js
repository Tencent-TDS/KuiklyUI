/**
 * Event Module (JavaScript Implementation)
 */

/**
 * 获取全局对象
 */
function getGlobal() {
  return typeof global !== 'undefined' ? global : (typeof window !== 'undefined' ? window : this);
}

export class KREventModule {
  static MODULE_NAME = 'KuiklyEventModule';

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
  	let result = this.call(method, params, callback)
  	console.log(`[KREventModule] _call ${method} ${params} ${result}`)
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
    const g = getGlobal();

    switch (method) {
      case 'openURL':
        this.openURL(params);
        return;

      case 'toast':
        this.toast(params);
        return;

      case 'copyText':
        this.copyText(params);
        return;

      case 'alert':
        this.alert(params, callback);
        return;

      case 'logoff':
        if (g.logoff) {
            g.logoff();
        }
        return;

      case 'toJSONObjectAsync':
        this.toJSONObjectAsync(params, callback);
        return;

      case 'toJSONArrayAsync':
        this.toJSONArrayAsync(params, callback);
        return;

      case 'getLastLoginTime':
        return g.getLastLoginTime ? g.getLastLoginTime() : 0;

      case 'getDeviceInfo':
        return g.getDeviceInfo ? g.getDeviceInfo() : '{}';

      default:
        console.error(`[KREventModule] ${method} not found`);
        if (callback) {
          callback({
            code: -1,
            message: "Method not found"
          });
        }
        return null;
    }
  }

  openURL(params) {
      try {
          const data = JSON.parse(params || '{}');
          const url = data.url;
          if (!url) return;
          
          const g = getGlobal();
          if (g.openURL) {
              g.openURL(url);
          } else {
              // Fallback to wx api if host method not present, though host logic is preferred
              // Note: direct wx calls might be restricted depending on context, preferring host delegation
              console.warn('[KREventModule] global.openURL not found');
          }
      } catch (e) {
          console.error('[KREventModule] openURL error', e);
      }
  }

  toast(params) {
      try {
          const data = JSON.parse(params || '{}');
          const message = data.message;
          if (!message) return;

          const g = getGlobal();
          if (g.wx && g.wx.showToast) {
              g.wx.showToast({
                  title: message,
                  icon: 'none'
              });
          }
      } catch (e) {
          console.error('[KREventModule] toast error', e);
      }
  }

  copyText(params) {
      try {
          const data = JSON.parse(params || '{}');
          const text = data.text;
          const g = getGlobal();
          if (g.wx && g.wx.setClipboardData) {
              g.wx.setClipboardData({
                  data: text || ''
              });
          }
      } catch (e) {
          console.error('[KREventModule] copyText error', e);
      }
  }

  alert(params, callback) {
      const g = getGlobal();
      // Prefer host implementation
      if (g.alert) {
          g.alert(params, callback);
          return;
      }

      // Fallback implementation using wx.showModal
      try {
          const data = JSON.parse(params || '{}');
          const title = data.title || '';
          const message = (data.message || '').replace(/<br\/>/g, '\n');
          const buttons = data.buttons || [];
          
          if (!buttons || buttons.length === 0 || buttons.length === 1) {
              const confirmText = buttons.length === 1 ? buttons[0] : '确定';
              if (g.wx && g.wx.showModal) {
                  g.wx.showModal({
                      title: title,
                      content: message,
                      showCancel: false,
                      confirmText: confirmText,
                      success: (res) => {
                          if (res.confirm && callback) {
                              callback({ result: "success", index: "0" });
                          }
                      }
                  });
              }
          } else if (buttons.length === 2) {
              // Android: buttons[0] -> Positive (index 0), buttons[1] -> Negative (index 1)
              
              if (g.wx && g.wx.showModal) {
                  g.wx.showModal({
                      title: title,
                      content: message,
                      confirmText: buttons[0], // Positive text
                      cancelText: buttons[1],  // Negative text
                      success: (res) => {
                          if (callback) {
                              if (res.confirm) {
                                  // Clicked Positive button
                                  callback({ result: "success", index: "0" });
                              } else if (res.cancel) {
                                  // Clicked Negative button
                                  callback({ result: "success", index: "1" });
                              }
                          }
                      }
                  });
              }
          } else {
              // 3 buttons not supported by wx.showModal, fail gracefully or show with 2
              if (callback) callback(null);
          }
      } catch (e) {
          console.error('[KREventModule] alert error', e);
          if (callback) callback(null);
      }
  }

  toJSONObjectAsync(params, callback) {
      if (!callback) return;
      // Simulate async
      setTimeout(() => {
          try {
              const json = JSON.parse(params || '{}');
              callback(json);
          } catch (e) {
              console.error('[KREventModule] toJSONObjectAsync error', e);
              callback(null);
          }
      }, 0);
  }

  toJSONArrayAsync(params, callback) {
      if (!callback) return;
      setTimeout(() => {
          try {
              const jsonArr = JSON.parse(params || '[]');
              callback({ array: jsonArr });
          } catch (e) {
              console.error('[KREventModule] toJSONArrayAsync error', e);
              callback(null);
          }
      }, 0);
  }

  /**
   * 清理模块资源 (IKuiklyRenderModuleExport 接口要求)
   */
  onDestroy() {
    // 清理工作（如果需要）
  }
}

