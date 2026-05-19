/**
 * Kuikly Render View (JavaScript Implementation)
 * Equivalent to h5App/components/KuiklyRenderView.kt
 * 
 * This is a thin wrapper around Kotlin's KuiklyView that registers custom modules
 */

import { KRBridgeModule } from '@modules/KRBridgeModule';
import { KRCacheModule } from '@modules/KRCacheModule';

/**
 * Kuikly Render View
 * 
 * Note: In Kotlin, KuiklyRenderView extends KuiklyView and overrides registerExternalModule.
 * In JavaScript, we cannot extend the compiled Kotlin class, so we:
 * 1. Create a delegate wrapper that intercepts registerExternalModule
 * 2. Register our custom modules (Bridge, Cache) in the intercepted method
 * 3. Forward all other calls to KuiklyView
 */
export class KuiklyRenderView {
  constructor(delegate = null) {
    this.kotlinView = null;
    this._createKotlinView(delegate);
  }

  /**
   * Create Kotlin KuiklyView instance with custom module registration
   * @private
   */
  _createKotlinView(originalDelegate) {
    const h5Module = window['com.tencent.kuikly-open.core-render-web:h5'];
    if (!h5Module) {
      throw new Error('Kotlin h5 module not loaded');
    }

    const KotlinView = h5Module.com.tencent.kuikly.core.render.web.runtime.web.expand.KuiklyView;
    
    // Create a delegate wrapper that intercepts registerExternalModule
    const delegateWrapper = {
      // Override registerExternalModule to add Bridge and Cache modules
      registerExternalModule: (kuiklyRenderExport) => {
        // First call original delegate's method (if exists)
        originalDelegate?.registerExternalModule?.(kuiklyRenderExport);
        
        // Then register our custom modules (equivalent to KuiklyRenderView.kt override)
        console.log('[KuiklyRenderView] Registering custom modules');
        
        kuiklyRenderExport.moduleExport(KRBridgeModule.MODULE_NAME, () => {
          console.log(`[KuiklyRenderView] Creating ${KRBridgeModule.MODULE_NAME}`);
          return new KRBridgeModule();
        });
        
        kuiklyRenderExport.moduleExport(KRCacheModule.MODULE_NAME, () => {
          console.log(`[KuiklyRenderView] Creating ${KRCacheModule.MODULE_NAME}`);
          return new KRCacheModule();
        });
      },
      
      // Forward all other delegate methods to original delegate (if exists)
      registerExternalRenderView: (export_) => originalDelegate?.registerExternalRenderView?.(export_),
      registerViewExternalPropHandler: (export_) => originalDelegate?.registerViewExternalPropHandler?.(export_),
      coreExecuteMode: () => originalDelegate?.coreExecuteMode?.(),
      performanceMonitorTypes: () => originalDelegate?.performanceMonitorTypes?.(),
      onKuiklyRenderViewCreated: () => originalDelegate?.onKuiklyRenderViewCreated?.(),
      onKuiklyRenderContentViewCreated: () => originalDelegate?.onKuiklyRenderContentViewCreated?.(),
      syncRenderingWhenPageAppear: () => originalDelegate?.syncRenderingWhenPageAppear?.(),
      onGetLaunchData: (data) => originalDelegate?.onGetLaunchData?.(data),
      onGetPerformanceData: (data) => originalDelegate?.onGetPerformanceData?.(data),
      onUnhandledException: (t, r, m) => originalDelegate?.onUnhandledException?.(t, r, m),
      onPageLoadComplete: (s, r, m) => originalDelegate?.onPageLoadComplete?.(s, r, m),
    };

    // Create Kotlin KuiklyView with our delegate wrapper
    this.kotlinView = new KotlinView(delegateWrapper);
  }

  /**
   * Attach view to container
   */
  onAttach(container, pageName, pageData, size) {
    // Convert size
    const kotlinSize = Array.isArray(size) ? size : [size.width, size.height];

    // Convert pageData to Kotlin Map
    const kotlinStdlib = window['kotlin-kotlin-stdlib'];
    const pageDataMap = kotlinStdlib.kotlin.collections.KtMap.fromJsMap(
      new Map(Object.entries(pageData))
    );

    this.kotlinView.onAttach(container, pageName, pageDataMap, kotlinSize);
  }

  /**
   * Resume view
   */
  onResume() {
    this.kotlinView?.onResume();
  }

  /**
   * Pause view
   */
  onPause() {
    this.kotlinView?.onPause();
  }

  /**
   * Detach view
   */
  onDetach() {
    this.kotlinView?.onDetach();
  }

  /**
   * Send event to page
   */
  sendEvent(event, data) {
    if (!this.kotlinView) return;
    
    const kotlinStdlib = window['kotlin-kotlin-stdlib'];
    const dataMap = kotlinStdlib.kotlin.collections.KtMap.fromJsMap(
      new Map(Object.entries(data))
    );
    
    this.kotlinView.sendEvent(event, dataMap);
  }

  /**
   * Get underlying Kotlin view instance
   */
  getKotlinView() {
    return this.kotlinView;
  }
}
