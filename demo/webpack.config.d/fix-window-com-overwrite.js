/**
 * Fix: Prevent window.com namespace from being overwritten
 * 
 * Problem:
 * When h5App.js (UMD module) loads, it executes:
 *   globalThis['KuiklyUI:h5App'] = factory(...)
 * 
 * The factory function returns an object with _.com namespace, and the UMD wrapper
 * syncs this to window.com, which overwrites the existing window.com.tencent.kuikly.core.nvi
 * that was set up by the page bundles (e.g., 000.bundle.js).
 * 
 * Solution:
 * Intercept window.com assignments and perform deep merge instead of overwrite.
 * This preserves existing properties (like nvi) while adding new ones (like render).
 * 
 * This code is injected into runtime.bundle.js via webpack BannerPlugin.
 */

const webpack = require('webpack');

const deepMergeCode = `
(function() {
    // 兼容小程序环境：使用 global 而不是 window
    var _global = typeof global !== 'undefined' ? global : (typeof window !== 'undefined' ? window : this);
    var _com = _global.com;
    
    function deepMerge(target, source) {
        if (!source || typeof source !== 'object') return target;
        if (!target || typeof target !== 'object') return source;
        
        var keys = Object.keys(source);
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            var targetVal = target[key];
            var sourceVal = source[key];
            
            if (targetVal && sourceVal && 
                typeof targetVal === 'object' && typeof sourceVal === 'object' &&
                !Array.isArray(targetVal) && !Array.isArray(sourceVal)) {
                deepMerge(targetVal, sourceVal);
            } else if (!(key in target)) {
                target[key] = sourceVal;
            }
        }
        return target;
    }
    
    Object.defineProperty(_global, 'com', {
        get: function() { return _com; },
        set: function(val) {
            if (_com && val && typeof _com === 'object' && typeof val === 'object') {
                deepMerge(_com, val);
            } else {
                _com = val;
            }
        },
        configurable: true
    });
})();
`;

config.plugins.push(
    new webpack.BannerPlugin({
        banner: deepMergeCode,
        raw: true,
        entryOnly: false,
        include: /runtime.*\.js$/
    })
);
