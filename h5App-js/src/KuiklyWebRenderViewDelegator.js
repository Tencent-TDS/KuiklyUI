/**
 * Kuikly Web Render View Delegator (Using Kotlin/JS compiled artifacts)
 * Wraps the compiled Kotlin KuiklyRenderViewDelegator
 */

import { KRMyView } from '@components/KRMyView';
import { KRWebView } from '@components/KRWebView';
import { KuiklyPageView } from '@components/KuiklyPageView';
import { KRBridgeModule } from '@modules/KRBridgeModule';
import { KRCacheModule } from '@modules/KRCacheModule';

/**
 * ViewPropExternalHandler - Handle external view properties
 */
class ViewPropExternalHandler {
  /**
   * Set view external property
   * @param {Object} renderViewExport - Render view export instance
   * @param {string} propKey - Property key
   * @param {*} propValue - Property value
   * @returns {boolean} - Whether the property was handled
   */
  setViewExternalProp(renderViewExport, propKey, propValue) {
    switch (propKey) {
      case 'needCustomWrapper':
        renderViewExport.ele.setAttribute('data-needCustomWrapper', propValue.toString());
        return true;
      default:
        return false;
    }
  }

  /**
   * Reset view external property
   * @param {Object} renderViewExport - Render view export instance
   * @param {string} propKey - Property key
   * @returns {boolean} - Whether the property was handled
   */
  resetViewExternalProp(renderViewExport, propKey) {
    switch (propKey) {
      case 'needCustomWrapper':
        renderViewExport.ele.setAttribute('data-needCustomWrapper', undefined);
        return true;
      default:
        return false;
    }
  }
}

/**
 * Kuikly Web Render View Delegator
 * Uses compiled Kotlin/JS KuiklyRenderViewDelegator
 */
export class KuiklyWebRenderViewDelegator {
  constructor() {
    this.viewPropHandler = new ViewPropExternalHandler();
    this.kotlinDelegator = null;
    this._checkKotlinModules();
  }

  /**
   * Check if Kotlin/JS modules are loaded
   * @private
   */
  _checkKotlinModules() {
    if (typeof window === 'undefined') {
      throw new Error('Window object not available');
    }

    // Check Kotlin stdlib
    if (typeof window.kotlin === 'undefined') {
      throw new Error('Kotlin stdlib not loaded. Make sure kotlin-kotlin-stdlib.js is loaded before this script.');
    }

    // Check Kuikly core modules (through full namespace path)
    if (typeof window.com === 'undefined' 
        || typeof window.com.tencent === 'undefined' 
        || typeof window.com.tencent.kuikly === 'undefined'
        || typeof window.com.tencent.kuikly.core === 'undefined'
        || typeof window.com.tencent.kuikly.core.render === 'undefined'
        || typeof window.com.tencent.kuikly.core.render.web === 'undefined') {
      throw new Error('KuiklyCore modules not loaded. Make sure KuiklyCore-render-web-base.js and KuiklyCore-render-web-h5.js are loaded.');
    }

    console.log('✅ All Kotlin/JS modules loaded successfully in delegator');
  }

  /**
   * Get Kotlin render web module namespace
   * @private
   */
  _getRenderWebModule() {
    return window.com.tencent.kuikly.core.render.web;
  }

  /**
   * Get Kotlin stdlib
   * @private
   */
  _getKotlinStdlib() {
    return window.kotlin;
  }

  /**
   * Create delegate implementation for Kotlin
   * @private
   */
  _createDelegateImpl() {
    const renderWebModule = this._getRenderWebModule();
    const self = this;

    return {
      // Register custom render views
      registerExternalRenderView(kuiklyRenderExport) {
        console.log('[Delegate] registerExternalRenderView called');
        self.registerExternalRenderView(kuiklyRenderExport);
      },

      // Register custom modules
      registerExternalModule(kuiklyRenderExport) {
        console.log('[Delegate] registerExternalModule called');
        self.registerExternalModule(kuiklyRenderExport);
      },

      // Register view external property handler
      registerViewExternalPropHandler(kuiklyRenderExport) {
        console.log('[Delegate] registerViewExternalPropHandler called');
        self.registerViewExternalPropHandler(kuiklyRenderExport);
      },

      // Return execution mode (JS = 2)
      coreExecuteMode() {
        const ExecuteMode = renderWebModule.context.KuiklyRenderCoreExecuteMode;
        return ExecuteMode.JS;
      },

      // Return performance monitor types (empty array = no monitoring)
      performanceMonitorTypes() {
        // Use exported helper function to create empty list
        // Need to check where helpers are actually exported
        const renderWebModule = self._getRenderWebModule();
        if (renderWebModule.runtime.web && renderWebModule.runtime.web.expand && renderWebModule.runtime.web.expand.emptyList) {
          return renderWebModule.runtime.web.expand.emptyList();
        } else if (renderWebModule.runtime.expand && renderWebModule.runtime.expand.emptyList) {
          return renderWebModule.runtime.expand.emptyList();
        } else {
          // Fallback: return empty array
          console.warn('[Delegate] emptyList helper not found, using empty array');
          return [];
        }
      },

      // Lifecycle callbacks
      onKuiklyRenderViewCreated() {
        console.log('[Delegate] onKuiklyRenderViewCreated');
      },

      onKuiklyRenderContentViewCreated() {
        console.log('[Delegate] onKuiklyRenderContentViewCreated');
      },

      syncRenderingWhenPageAppear() {
        return true;
      },

      onGetLaunchData(data) {
        console.log('[Delegate] onGetLaunchData:', data);
      },

      onGetPerformanceData(data) {
        console.log('[Delegate] onGetPerformanceData:', data);
      },

      onUnhandledException(throwable, errorReason, executeMode) {
        console.error('[Delegate] onUnhandledException:', throwable, errorReason);
      },

      onPageLoadComplete(isSucceed, errorReason, executeMode) {
        console.log('[Delegate] onPageLoadComplete:', isSucceed);
        if (!isSucceed) {
          console.error('[Delegate] Page load failed:', errorReason);
        }
      }
    };
  }

  /**
   * Initialize and create view
   * @param {string} containerId - Container element ID
   * @param {string} pageName - Page name
   * @param {Object} pageData - Page data
   * @param {Object} size - Size object {width, height}
   */
  init(containerId, pageName, pageData, size) {
    console.log(`[KuiklyWebRenderViewDelegator] Initializing page: ${pageName}`);
    console.log('[KuiklyWebRenderViewDelegator] Page data:', pageData);

    try {
      // Get Kotlin render web module
      const renderWebModule = this._getRenderWebModule();
      
      // Debug: Check what's available in runtime
      console.log('[Debug] runtime:', renderWebModule.runtime);
      console.log('[Debug] runtime keys:', Object.keys(renderWebModule.runtime));
      
      // Try to find KuiklyRenderViewDelegator
      // It might be at runtime.web.expand or just runtime.expand
      let KotlinDelegator;
      if (renderWebModule.runtime.web && renderWebModule.runtime.web.expand) {
        KotlinDelegator = renderWebModule.runtime.web.expand.KuiklyRenderViewDelegator;
      } else if (renderWebModule.runtime.expand) {
        KotlinDelegator = renderWebModule.runtime.expand.KuiklyRenderViewDelegator;
      } else {
        throw new Error('Cannot find KuiklyRenderViewDelegator in runtime namespace');
      }

      // Create delegate implementation
      const delegateImpl = this._createDelegateImpl();

      // Create Kotlin delegator instance
      this.kotlinDelegator = new KotlinDelegator(delegateImpl);
      console.log('✅ Kotlin KuiklyRenderViewDelegator created');

      // Convert JavaScript object to Kotlin Map using jsObjectToMap helper
      const pageDataMap = this._convertToKotlinMap(pageData);

      // Create SizeI object
      const kotlinSize = this._createSizeI(size.width, size.height);

      // Call onAttach with containerId (string), not DOM element
      // This matches the Kotlin implementation
      this.kotlinDelegator.onAttach(containerId, pageName, pageDataMap, kotlinSize);
      console.log('✅ KuiklyRenderViewDelegator.onAttach called');

    } catch (error) {
      console.error('[KuiklyWebRenderViewDelegator] Initialization failed:', error);
      throw error;
    }
  }

  /**
   * Convert JavaScript object to Kotlin Map
   * Uses exported jsObjectToMap helper function from Kotlin
   * @private
   */
  _convertToKotlinMap(jsObject) {
    const renderWebModule = this._getRenderWebModule();
    
    // Try to find jsObjectToMap helper
    if (renderWebModule.runtime.web && renderWebModule.runtime.web.expand && renderWebModule.runtime.web.expand.jsObjectToMap) {
      return renderWebModule.runtime.web.expand.jsObjectToMap(jsObject);
    } else if (renderWebModule.runtime.expand && renderWebModule.runtime.expand.jsObjectToMap) {
      return renderWebModule.runtime.expand.jsObjectToMap(jsObject);
    } else {
      console.warn('[Delegate] jsObjectToMap helper not found, passing object directly');
      return jsObject;
    }
  }

  /**
   * Create SizeI object for size parameter
   * Uses exported SizeI constructor from JSHelper
   * @private
   */
  _createSizeI(width, height) {
    const renderWebModule = this._getRenderWebModule();
    
    // Try to find exported SizeI constructor
    if (renderWebModule.runtime.web && renderWebModule.runtime.web.expand && renderWebModule.runtime.web.expand.SizeI) {
      return renderWebModule.runtime.web.expand.SizeI(width, height);
    } else if (renderWebModule.runtime.expand && renderWebModule.runtime.expand.SizeI) {
      return renderWebModule.runtime.expand.SizeI(width, height);
    } else {
      // Fallback: create Kotlin Pair using stdlib
      const kotlinStdlib = this._getKotlinStdlib();
      if (kotlinStdlib && kotlinStdlib.kotlin && kotlinStdlib.kotlin.Pair) {
        return new kotlinStdlib.kotlin.Pair(width, height);
      } else if (kotlinStdlib && kotlinStdlib.Pair) {
        return new kotlinStdlib.Pair(width, height);
      } else {
        // Final fallback: create simple object with first/second properties
        console.warn('[Delegate] SizeI constructor and Kotlin Pair not found, using object');
        return { first: width, second: height };
      }
    }
  }

  /**
   * Page becomes visible
   */
  resume() {
    console.log('[KuiklyWebRenderViewDelegator] Page resumed');
    if (this.kotlinDelegator) {
      this.kotlinDelegator.onResume();
    }
  }

  /**
   * Page becomes invisible
   */
  pause() {
    console.log('[KuiklyWebRenderViewDelegator] Page paused');
    if (this.kotlinDelegator) {
      this.kotlinDelegator.onPause();
    }
  }

  /**
   * Page unload
   */
  detach() {
    console.log('[KuiklyWebRenderViewDelegator] Page detached');
    if (this.kotlinDelegator) {
      this.kotlinDelegator.onDetach();
      this.kotlinDelegator = null;
    }
  }

  /**
   * Send event to page
   * @param {string} event - Event name
   * @param {Object} data - Event data
   */
  sendEvent(event, data) {
    if (this.kotlinDelegator) {
      // Convert JavaScript object to Kotlin Map
      const dataMap = this._convertToKotlinMap(data);
      this.kotlinDelegator.sendEvent(event, dataMap);
    }
  }

  /**
   * Register custom modules (called by Kotlin delegate)
   */
  registerExternalModule(kuiklyRenderExport) {
    console.log('[KuiklyWebRenderViewDelegator] Registering external modules');
    
    // Register bridge module
    kuiklyRenderExport.moduleExport(KRBridgeModule.MODULE_NAME, () => {
      console.log(`[Module] Creating: ${KRBridgeModule.MODULE_NAME}`);
      return new KRBridgeModule();
    });
    
    // Register cache module
    kuiklyRenderExport.moduleExport(KRCacheModule.MODULE_NAME, () => {
      console.log(`[Module] Creating: ${KRCacheModule.MODULE_NAME}`);
      return new KRCacheModule();
    });
  }

  /**
   * Register custom render views (called by Kotlin delegate)
   */
  registerExternalRenderView(kuiklyRenderExport) {
    console.log('[KuiklyWebRenderViewDelegator] Registering external render views');
    
    // Register custom views
    kuiklyRenderExport.renderViewExport(KRMyView.VIEW_NAME, () => {
      console.log(`[View] Creating: ${KRMyView.VIEW_NAME}`);
      return new KRMyView();
    });
    
    kuiklyRenderExport.renderViewExport(KuiklyPageView.VIEW_NAME, () => {
      console.log(`[View] Creating: ${KuiklyPageView.VIEW_NAME}`);
      return new KuiklyPageView();
    });
    
    kuiklyRenderExport.renderViewExport(KRWebView.VIEW_NAME, () => {
      console.log(`[View] Creating: ${KRWebView.VIEW_NAME}`);
      return new KRWebView();
    });
  }

  /**
   * Register view external property handler (called by Kotlin delegate)
   */
  registerViewExternalPropHandler(kuiklyRenderExport) {
    console.log('[KuiklyWebRenderViewDelegator] Registering view property handler');
    kuiklyRenderExport.viewPropExternalHandlerExport(this.viewPropHandler);
  }

  /**
   * Get Kotlin render context
   */
  getKuiklyRenderContext() {
    if (this.kotlinDelegator) {
      return this.kotlinDelegator.getKuiklyRenderContext();
    }
    return null;
  }
}

