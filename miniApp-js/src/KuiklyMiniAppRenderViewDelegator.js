/**
 * Kuikly Mini App Render View Delegator (JavaScript Implementation)
 * 
 * 直接依赖 KuiklyCore-render-web-miniapp.js 中的 Kotlin 编译产物
 */

import { KRMyView } from './components/KRMyView';
import { KRWebView } from './components/KRWebView';
import { KRBridgeModule } from './modules/KRBridgeModule';
import { KRCacheModule } from './modules/KRCacheModule';
import { KRAppInfoModule } from './modules/KRAppinfoModule';
import { KREventModule } from './modules/KREventModule';

/**
 * 获取全局对象
 */
function getGlobal() {
  return typeof global !== 'undefined' ? global : (typeof window !== 'undefined' ? window : this);
}

/**
 * 获取 Kotlin render web module 命名空间
 */
function getRenderWebModule() {
  const g = getGlobal();
  if (g.com && g.com.tencent && g.com.tencent.kuikly 
      && g.com.tencent.kuikly.core && g.com.tencent.kuikly.core.render
      && g.com.tencent.kuikly.core.render.web) {
    return g.com.tencent.kuikly.core.render.web;
  }
  return null;
}

/**
 * 获取 Kotlin Transform 对象
 */
function getKotlinTransform() {
  const renderWebModule = getRenderWebModule();
  if (renderWebModule && renderWebModule.runtime && renderWebModule.runtime.miniapp
      && renderWebModule.runtime.miniapp.core) {
    return renderWebModule.runtime.miniapp.core.Transform;
  }
  return null;
}

/**
 * 获取 Kotlin Pair 类（用于创建 SizeI）
 */
function getKotlinPair() {
  const g = getGlobal();
  if (g.kotlin && g.kotlin.Pair) {
    return g.kotlin.Pair;
  }
  return null;
}



/**
 * 创建 SizeI (Pair<Int, Int>)
 * @param {number} width 
 * @param {number} height 
 * @returns Kotlin Pair 对象或 null
 */
function createSize(width, height) {
  const KotlinPair = getKotlinPair();
  if (KotlinPair && width != null && height != null) {
    return new KotlinPair(width, height);
  }
  return null;
}

/**
 * ViewPropExternalHandler - 处理外部视图属性
 */
class ViewPropExternalHandler {
  /**
   * 设置视图外部属性
   * @param {Object} renderViewExport - Render view export 实例
   * @param {string} propKey - 属性键
   * @param {*} propValue - 属性值
   * @returns {boolean} - 是否处理了该属性
   */
  setViewExternalProp(renderViewExport, propKey, propValue) {
    switch (propKey) {
      case 'needCustomWrapper':
        // 设置 needCustomWrapper 属性
        if (renderViewExport.ele && typeof renderViewExport.ele.needCustomWrapper !== 'undefined') {
          renderViewExport.ele.needCustomWrapper = propValue === 'true' || propValue === true;
        }
        return true;
      default:
        return false;
    }
  }

  /**
   * 重置视图外部属性
   * @param {Object} renderViewExport - Render view export 实例
   * @param {string} propKey - 属性键
   * @returns {boolean} - 是否处理了该属性
   */
  resetViewExternalProp(renderViewExport, propKey) {
    switch (propKey) {
      case 'needCustomWrapper':
        return true;
      default:
        return false;
    }
  }
}

/**
 * Kuikly Mini App Render View Delegator
 * 实现 Web Render 提供的 delegate 接口
 */
export class KuiklyMiniAppRenderViewDelegator {
  constructor() {
    this.viewPropHandler = new ViewPropExternalHandler();
    this.kotlinDelegator = null;
  }

  /**
   * 创建 delegate 实现
   * 实现 KuiklyRenderViewDelegatorDelegate 接口的所有方法
   * @private
   */
  _createDelegateImpl() {
    const self = this;
    const renderWebModule = getRenderWebModule();

    return {
      // 注册自定义渲染视图
      registerExternalRenderView(kuiklyRenderExport) {
        console.log('[Delegate] registerExternalRenderView called');
        self.registerExternalRenderView(kuiklyRenderExport);
      },

      // 注册自定义模块
      registerExternalModule(kuiklyRenderExport) {
        console.log('[Delegate] registerExternalModule called');
        self.registerExternalModule(kuiklyRenderExport);
      },

      // 注册视图外部属性处理器
      registerViewExternalPropHandler(kuiklyRenderExport) {
        console.log('[Delegate] registerViewExternalPropHandler called');
        self.registerViewExternalPropHandler(kuiklyRenderExport);
      },

      // 返回执行模式 (JS = 2)
      coreExecuteMode() {
        // 尝试获取 Kotlin 枚举
        if (renderWebModule && renderWebModule.context && renderWebModule.context.KuiklyRenderCoreExecuteMode) {
          return renderWebModule.context.KuiklyRenderCoreExecuteMode.JS;
        }
        return 2; // JS mode fallback
      },

      // 返回性能监控类型
      // JS 对象字面量不会自动继承 Kotlin 接口的默认实现,必须显式提供
      // 但可以返回 undefined,让 Kotlin 端处理
      performanceMonitorTypes() {
        // 返回 undefined,让 Kotlin 端使用默认值
        return undefined;
      },

      // KuiklyRenderView 创建回调
      onKuiklyRenderViewCreated() {
        console.log('[Delegate] onKuiklyRenderViewCreated');
      },

      // KuiklyRenderView 子视图创建回调
      onKuiklyRenderContentViewCreated() {
        console.log('[Delegate] onKuiklyRenderContentViewCreated');
      },

      // 首屏是否同步渲染
      syncRenderingWhenPageAppear() {
        return true;
      },

      // 启动数据回调
      onGetLaunchData(data) {
        console.log('[Delegate] onGetLaunchData:', data);
      },

      // 性能数据回调
      onGetPerformanceData(data) {
        console.log('[Delegate] onGetPerformanceData:', data);
      },

      // 异常回调
      onUnhandledException(throwable, errorReason, executeMode) {
        console.error('[Delegate] onUnhandledException:', throwable, errorReason);
      },

      // 页面加载完成回调
      onPageLoadComplete(isSucceed, errorReason, executeMode) {
        console.log('[Delegate] onPageLoadComplete:', isSucceed);
        if (!isSucceed) {
          console.error('[Delegate] Page load failed:', errorReason);
        }
      }
    };
  }

  /**
   * 初始化并 attach 视图
   * @param {number} pageId - 页面 ID
   * @param {string} pageName - 页面名称
   * @param {Object} pageData - 页面数据 (FastMutableMap from Kotlin)
   * @param {number|null} width - 宽度（像素），可选
   * @param {number|null} height - 高度（像素），可选
   */
  onAttach(pageId, pageName, pageData, width, height) {
    console.log(`[KuiklyMiniAppRenderViewDelegator] Initializing page: ${pageName}`);
    console.log('[KuiklyMiniAppRenderViewDelegator] Page data:', pageData);

    try {
      // 获取 Kotlin render web module
      const renderWebModule = getRenderWebModule();
      
      if (!renderWebModule) {
        console.error('[KuiklyMiniAppRenderViewDelegator] Render web module not found');
        return;
      }

      // 获取 @JsExport 导出的 KuiklyRenderViewDelegator
      // Kotlin/JS 的 @JsExport 会将类导出到 package 对应的命名空间
      const KotlinDelegator = renderWebModule?.runtime?.miniapp?.expand?.KuiklyRenderViewDelegator;

      if (!KotlinDelegator) {
        console.error('[KuiklyMiniAppRenderViewDelegator] Cannot find KuiklyRenderViewDelegator');
        console.error('Expected path: com.tencent.kuikly.core.render.web.runtime.miniapp.expand.KuiklyRenderViewDelegator');
        console.error('Available at renderWebModule:', renderWebModule);
        
        // 详细诊断信息
        if (renderWebModule?.runtime?.miniapp?.expand) {
          console.error('Available exports in expand:', Object.keys(renderWebModule.runtime.miniapp.expand));
        }
        return;
      }

      // 创建 delegate 实现
      const delegateImpl = this._createDelegateImpl();

      // 创建 Kotlin delegator 实例
      this.kotlinDelegator = new KotlinDelegator(delegateImpl);
      console.log('✅ Kotlin KuiklyRenderViewDelegator created');

      // 创建 size (Kotlin Pair)
      const size = createSize(width, height);

      // 调用 onAttach - 使用原始 Kotlin 签名 (pageId, pageName, paramsMap, size)
      this.kotlinDelegator.onAttach(pageId, pageName, pageData, size);
      console.log('✅ KuiklyRenderViewDelegator.onAttach called');

    } catch (error) {
      console.error('[KuiklyMiniAppRenderViewDelegator] Initialization failed:', error);
      throw error;
    }
  }

  /**
   * 页面可见
   */
  onResume() {
    console.log('[KuiklyMiniAppRenderViewDelegator] Page resumed');
    if (this.kotlinDelegator) {
      this.kotlinDelegator.onResume();
    }
  }

  /**
   * 页面不可见
   */
  onPause() {
    console.log('[KuiklyMiniAppRenderViewDelegator] Page paused');
    if (this.kotlinDelegator) {
      this.kotlinDelegator.onPause();
    }
  }

  /**
   * 页面卸载
   */
  onDetach() {
    console.log('[KuiklyMiniAppRenderViewDelegator] Page detached');
    if (this.kotlinDelegator) {
      this.kotlinDelegator.onDetach();
      this.kotlinDelegator = null;
    }
  }

  /**
   * 发送事件到页面
   * @param {string} event - 事件名称
   * @param {Object} data - 事件数据
   */
  sendEvent(event, data) {
    if (this.kotlinDelegator) {
      this.kotlinDelegator.sendEvent(event, data);
    }
  }

  /**
   * 注册自定义模块 (由 Kotlin delegate 调用)
   */
  registerExternalModule(kuiklyRenderExport) {
    console.log('[KuiklyMiniAppRenderViewDelegator] Registering external modules');

    // 注册 bridge 模块
    kuiklyRenderExport.moduleExport(KRBridgeModule.MODULE_NAME, () => {
      console.log(`[Module] Creating: ${KRBridgeModule.MODULE_NAME}`);
      return new KRBridgeModule();
    });

    // 注册 cache 模块
    kuiklyRenderExport.moduleExport(KRCacheModule.MODULE_NAME, () => {
      console.log(`[Module] Creating: ${KRCacheModule.MODULE_NAME}`);
      return new KRCacheModule();
    });

    // 注册 AppInfo 模块
    kuiklyRenderExport.moduleExport(KRAppInfoModule.MODULE_NAME, () => {
      console.log(`[Module] Creating: ${KRAppInfoModule.MODULE_NAME}`);
      return new KRAppInfoModule();
    });

    // 注册 Event 模块
    kuiklyRenderExport.moduleExport(KREventModule.MODULE_NAME, () => {
      console.log(`[Module] Creating: ${KREventModule.MODULE_NAME}`);
      return new KREventModule();
    });
  }

  /**
   * 注册自定义渲染视图 (由 Kotlin delegate 调用)
   */
  registerExternalRenderView(kuiklyRenderExport) {
    console.log('[KuiklyMiniAppRenderViewDelegator] Registering external render views');

    // 使用 Kotlin Transform 添加组件别名
    const KotlinTransform = getKotlinTransform();
    if (KotlinTransform && KRWebView.NODE_NAME && KRWebView.componentsAlias) {
      KotlinTransform.addComponentsAlias(KRWebView.NODE_NAME, KRWebView.componentsAlias);
    }

    // 注册自定义视图
    kuiklyRenderExport.renderViewExport(KRWebView.VIEW_NAME, () => {
      console.log(`[View] Creating: ${KRWebView.VIEW_NAME}`);
      return new KRWebView();
    });

    kuiklyRenderExport.renderViewExport(KRMyView.VIEW_NAME, () => {
      console.log(`[View] Creating: ${KRMyView.VIEW_NAME}`);
      return new KRMyView();
    });
  }

  /**
   * 注册视图外部属性处理器 (由 Kotlin delegate 调用)
   */
  registerViewExternalPropHandler(kuiklyRenderExport) {
    console.log('[KuiklyMiniAppRenderViewDelegator] Registering view property handler');
    kuiklyRenderExport.viewPropExternalHandlerExport(this.viewPropHandler);
  }

  /**
   * 获取 Kotlin render context
   */
  getKuiklyRenderContext() {
    if (this.kotlinDelegator) {
      return this.kotlinDelegator.getKuiklyRenderContext();
    }
    return null;
  }
}
