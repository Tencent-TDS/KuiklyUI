#!/usr/bin/env node
/**
 * Kuikly 页面构建器 - 从配置文件读取
 * 
 * 功能：从配置文件读取页面列表，然后调用 run-script.js 执行构建
 * 
 * 用法：
 *   node scripts/build-from-config.js [dev]
 * 
 * 参数：
 *   dev - 可选：传入 dev 则走开发环境；不传默认走生产环境
 * 
 * 配置文件：
 *   项目根目录下的 pages.json
 *   格式：{ "pages": ["PageName1", "PageName2"] }
 */

const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

// 获取目录路径
const scriptsDir = __dirname;
const h5AppJsDir = path.dirname(scriptsDir);

// 配置文件路径
const configFile = path.join(h5AppJsDir, 'pages.json');

// 解析参数
const args = process.argv.slice(2);
const isDev = args.includes('dev');

// 检查配置文件是否存在
if (!fs.existsSync(configFile)) {
    console.error('❌ 错误: 找不到配置文件');
    console.error(`   预期位置: ${configFile}`);
    console.log('');
    console.log('💡 提示: 请创建配置文件，格式如下：');
    console.log('   {');
    console.log('     "pages": ["PageName1", "PageName2"]');
    console.log('   }');
    process.exit(1);
}

// 读取配置文件
let config;
try {
    const configContent = fs.readFileSync(configFile, 'utf8');
    config = JSON.parse(configContent);
} catch (err) {
    console.error(`❌ 读取配置文件失败: ${err.message}`);
    process.exit(1);
}

// 验证配置
if (!config.pages || !Array.isArray(config.pages)) {
    console.error('❌ 配置文件中缺少 pages 数组');
    process.exit(1);
}

if (config.pages.length === 0) {
    console.error('❌ pages 数组为空');
    process.exit(1);
}

// 生成页面列表字符串
const pageList = config.pages.join(',');

console.log(`📁 配置文件: ${configFile}`);
console.log(`📄 页面列表: ${pageList}`);
console.log(`🧩 构建模式: ${isDev ? 'development' : 'production'}`);
console.log('');

// 构建 run-script.js 的参数
const runScriptPath = path.join(scriptsDir, 'run-script.js');
const runScriptArgs = ['build-and-copy-bundles'];

if (isDev) {
    runScriptArgs.push('dev');
}
runScriptArgs.push(pageList);

// 调用 run-script.js 执行构建
const child = spawn('node', [runScriptPath, ...runScriptArgs], {
    stdio: 'inherit',
    cwd: h5AppJsDir
});

child.on('error', (err) => {
    console.error(`❌ 启动构建失败: ${err.message}`);
    process.exit(1);
});

child.on('close', (code) => {
    process.exit(code || 0);
});