// 小程序分包配置示例

/**
 * Webpack Split Chunks Configuration for Kuikly
 * 
 * This configuration optimizes bundle size by extracting common code into shared chunks:
 * - runtime.bundle.js: Webpack runtime code (module system, chunk loading)
 * - kotlin-stdlib.bundle.js: Kotlin standard library (shared across all pages)
 * - vendors.bundle.js: Third-party dependencies from node_modules
 * - common.bundle.js: Shared code between multiple pages (or common modules in single entry)
 * - [pageName].bundle.js: Page-specific code
 * 
 * Supports both single-entry and multi-entry modes:
 * - Single-entry: Extracts vendor libraries and common modules for better caching
 * - Multi-entry: Additionally splits shared code between multiple pages
 * 
 * Usage:
 * This file will be automatically merged into webpack.config.js by Kotlin Multiplatform plugin.
 * It works in conjunction with packEntryJSBundle task for code splitting optimization.
 * 
 * @see JSSplitProcessor.kt for multi-entry configuration generation
 */
/*
(function(config) {
    console.log('[Split Chunks] Configuring code splitting optimization');
    
    const entryCount = config.entry && typeof config.entry === 'object' ? Object.keys(config.entry).length : 1;
    const isMultiEntry = entryCount > 1;
    
    console.log(`[Split Chunks] ${isMultiEntry ? 'Multi' : 'Single'}-entry mode detected, enabling code splitting`);
    
    config.optimization = config.optimization || {};
    
    // Enable runtime chunk extraction
    // This extracts webpack runtime into a separate file that can be cached independently
    config.optimization.runtimeChunk = {
        name: 'runtime'
    };
    
    // Configure split chunks strategy
    config.optimization.splitChunks = {
        chunks: 'all',  // Split both async and non-async chunks
        
        // Minimum size threshold for chunk extraction (bytes)
        // Only extract chunks larger than 20KB to avoid creating too many small files
        minSize: 20000,
        
        // Maximum size hint for chunks (bytes)
        // Webpack will try to split chunks larger than this size
        maxSize: 6440000,  // ~244KB
        
        // Minimum number of chunks that must share a module before splitting
        minChunks: isMultiEntry ? 2 : 1,
        
        // Maximum number of parallel requests when loading a page
        maxAsyncRequests: 30,
        
        // Maximum number of parallel requests at an entry point
        maxInitialRequests: 30,
        
        // Character used to join chunk names
        automaticNameDelimiter: '.',
        
        cacheGroups: {
            // 提取 Kotlin 标准库及公共依赖（kotlin-kotlin-stdlib、atomicfu、kotlinx-serialization、kuiklycore-core）
            // 精确匹配公共库目录，避免将业务代码（如 nativevue2）误打包
            kotlinStdlib: {
                test: /[\\/](?:kotlin-kotlin-stdlib|atomicfu-js-ir|kotlinx-serialization-[\w-]+|kuiklycore-core)[\\/]/,
                name: 'kotlin-stdlib',
                priority: 30,
                reuseExistingChunk: true,
                enforce: true
            },
            
            // 提取 node_modules 中的第三方依赖
            vendors: {
                test: /[\\/]node_modules[\\/]/,
                name: 'vendors',
                priority: 20,
                reuseExistingChunk: true
            },
            
            // 提取多页面间共享的业务公共代码
            // 对于多入口：提取被多个页面引用的公共业务模块
            // 对于单入口：提取可独立缓存的公共模块
            common: {
                name: 'common',
                minChunks: isMultiEntry ? 2 : 1,
                priority: 10,
                reuseExistingChunk: true,
                enforce: false
            },
            
            // 默认分组
            default: {
                minChunks: isMultiEntry ? 2 : 1,
                priority: -20,
                reuseExistingChunk: true
            }
        }
    };
    
    // Update output filename pattern to support content hashing in production
    // This enables long-term caching by adding content hash to filenames
    const isProduction = config.mode === 'production';
    
    if (isProduction) {
        // Production: Use content hash for cache busting
        config.output.filename = '[name].[contenthash:8].bundle.js';
        config.output.chunkFilename = '[name].[contenthash:8].chunk.js';
        console.log('[Split Chunks] Production mode: enabled content hashing');
    } else {
        // Development: Use simple names for easier debugging
        config.output.filename = '[name].bundle.js';
        config.output.chunkFilename = '[name].chunk.js';
        console.log('[Split Chunks] Development mode: using simple names');
    }
    
    // Configure module concatenation (scope hoisting) for better performance
    if (isProduction) {
        config.optimization.concatenateModules = true;
    }
    
    // Enable deterministic module IDs for better long-term caching
    config.optimization.moduleIds = 'deterministic';
    config.optimization.chunkIds = 'deterministic';
    
    console.log('[Split Chunks] Configuration completed:');
    console.log(`  - Entry count: ${entryCount}`);
    console.log(`  - Runtime chunk: ${config.optimization.runtimeChunk.name}.bundle.js`);
    console.log('  - Cache groups: kotlin-stdlib, vendors, common, default');
    
})(config);
*/
