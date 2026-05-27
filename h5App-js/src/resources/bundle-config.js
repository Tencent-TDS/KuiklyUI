/**
 * Bundle 依赖配置文件
 * 
 * 使用动态发现模式：自动扫描 bundles 目录下的文件
 * manifest.json 由 generate-manifest.js 脚本自动生成
 */
window.BundleConfig = {
  /**
   * 动态发现时的加载顺序规则（正则表达式）
   * 按照数组顺序匹配并排序，未匹配的文件放在最后
   */
  dynamicLoadingOrder: [
    // 按 demo/webpack.config.d/split-chunks.js 的产物模型排序：
    // 1) runtime -> 2) kotlin-stdlib -> 3) vendors -> 4) common -> 其余

    // 1. Webpack 运行时（必须最先）
    // 兼容：runtime.bundle.js（dev） / runtime.{hash}.bundle.js（prod）
    /^runtime(\.[0-9a-f]{8})?\.bundle\.js$/i,

    // 2. Kotlin 标准库（全局共享）
    // 兼容：
    // - kotlin-stdlib.bundle.js（dev）
    // - kotlin-stdlib.{hash}.bundle.js（prod）
    // - kotlin-stdlib.kotlin_k.bundle.js（历史产物）
    /^kotlin-stdlib(\.(kotlin_k|[0-9a-f]{8}))?\.bundle\.js$/i,

    // 3. 第三方依赖（node_modules 拆分）
    /^vendors(\.[0-9a-f]{8})?\.bundle\.js$/i,

    // 4. 共享业务代码（common chunk）
    /^common(\.[0-9a-f]{8})?\.bundle\.js$/i
  ],
  
  /**
   * 页面bundle的特殊依赖配置
   * 如果某个页面需要额外的依赖，可以在这里配置
   */
  pageDependencies: {
    // 示例：如果某个页面需要特殊的依赖
    // 'SpecialPage': ['special-lib.bundle.js']
  },
  
  /**
   * 预加载配置
   * 可以配置哪些bundle需要预加载
   */
  preloadBundles: [
    // 可以在这里配置需要预加载的bundle
  ],
  
  /**
   * 加载超时配置（毫秒）
   */
  loadTimeout: 10000,
  
  /**
   * 是否启用并行加载优化
   * 现在顺序正确了，可以启用并行加载提升性能
   */
  enableParallelLoading: true
};