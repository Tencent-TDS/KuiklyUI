// 微信小程序兼容性配置
// 禁用 eval 相关功能，因为小程序环境不支持 eval

config.devtool = false; // 完全禁用 source map 中的 eval

// 确保输出配置兼容小程序
config.output = config.output || {};
config.output.devtoolModuleFilenameTemplate = undefined;
config.output.globalObject = 'global';
config.target = 'node';

config.node = config.node || {};
config.node.global = false;
// 禁用 webpack 内置的 eval 优化和压缩
config.optimization = config.optimization || {};
config.optimization.usedExports = false;
config.optimization.minimize = false; // 开发和生产模式都不压缩
config.optimization.minimizer = []; // 清空压缩器列表

// 如果是生产模式，确保 TerserPlugin 也不会被使用
if (config.mode === 'production') {
    config.mode = 'none'; // 改为 none 模式，完全避免生产优化
}

console.log('[webpack.config.d/miniapp-no-eval.js] 已禁用 eval 和代码压缩，兼容微信小程序环境');
