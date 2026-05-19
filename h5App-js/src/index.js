/**
 * Kuikly H5 App - Main Entry (JavaScript Implementation)
 * Equivalent to Main.kt
 */

import { KuiklyWebRenderViewDelegator } from './KuiklyWebRenderViewDelegator';
import { URLUtils } from '@utils/Url';

/**
 * WebApp entry, use renderView delegate method to initialize and create renderView
 */
function main() {
  console.log('##### Kuikly H5 (JavaScript) #####');
  
  // 检查 bundle 加载器是否已经完成加载
  if (!window.kuiklyBundlesReady) {
    console.log('⏳ 等待 Bundle 加载器完成...');
    // 标记主函数等待执行
    window.kuiklyMainPending = true;
    window.kuiklyMainFunction = main;
    return;
  }
  
  // 检查 Kotlin/JS 模块是否已加载
  // Kotlin/JS 库通过 UMD 格式导出，模块会被导出到 window.com.tencent.kuikly 命名空间
  console.log('Checking Kotlin/JS modules...');
  
  // 检查 Kotlin 标准库
  const hasKotlinStdlib = typeof window.kotlin !== 'undefined';
  console.log('- kotlin stdlib:', hasKotlinStdlib);
  
  // 检查 Kuikly 核心模块（通过完整的命名空间路径）
  const hasKuiklyCore = typeof window.com !== 'undefined' 
    && typeof window.com.tencent !== 'undefined' 
    && typeof window.com.tencent.kuikly !== 'undefined'
    && typeof window.com.tencent.kuikly.core !== 'undefined'
    && typeof window.com.tencent.kuikly.core.render !== 'undefined'
    && typeof window.com.tencent.kuikly.core.render.web !== 'undefined';
  console.log('- Kuikly core modules:', hasKuiklyCore);
  
  if (!hasKotlinStdlib || !hasKuiklyCore) {
    console.error('❌ Kotlin/JS modules not fully loaded!');
    console.error('Available window properties:', Object.keys(window).filter(k => 
      k.includes('kotlin') || k.includes('Kuikly') || k.includes('com') || k === 'com'
    ));
    if (window.com) {
      console.error('window.com structure:', window.com);
    }
    return;
  }
  
  console.log('✅ All Kotlin/JS modules loaded successfully');
  
  // 清除等待标记
  window.kuiklyMainPending = false;
  
  // Root container id, note that this needs to match the container id in the actual index.html file
  const containerId = 'root';
  const H5Sign = 'is_H5';
  
  // Process URL parameters
  const urlParams = URLUtils.parseParams(window.location.href);
  
  // Page name, default is router
  const pageName = urlParams['page_name'] || 'router';
  
  // Container size
  const containerWidth = window.innerWidth;
  const containerHeight = window.innerHeight;
  
  // Business parameters
  const params = {};
  
  // Add business parameters
  if (Object.keys(urlParams).length > 0) {
    // Append all URL parameters to business parameters
    Object.assign(params, urlParams);
  }
  
  // Add web-specific parameters
  params[H5Sign] = '1';
  
  // Page parameter Map
  const paramMap = {
    statusBarHeight: 0,
    activityWidth: containerWidth,
    activityHeight: containerHeight,
    param: params,
  };
  
  // Initialize delegator
  const delegator = new KuiklyWebRenderViewDelegator();
  
  // Create render view
  delegator.init(
    containerId,
    pageName,
    paramMap,
    { width: containerWidth, height: containerHeight }
  );
  
  // Trigger resume
  delegator.resume();
  
  // Register visibility event
  document.addEventListener('visibilitychange', () => {
    const hidden = document.hidden;
    if (hidden) {
      // Page hidden
      delegator.pause();
    } else {
      // Page restored
      delegator.resume();
    }
  });
}

// Execute main function when DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', main);
} else {
  main();
}

// 监听 bundle 加载完成事件
window.addEventListener('bundleLoadComplete', () => {
  console.log('📦 Bundle 加载完成，准备执行主应用');
  if (window.kuiklyMainPending) {
    main();
  }
});
