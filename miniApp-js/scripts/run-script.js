#!/usr/bin/env node
/**
 * 跨平台脚本运行器
 * 用法: node run-script.js <script-name> [args...]
 * 
 * 在 Windows 上运行 .bat 文件，在 Unix 上运行 .sh 文件
 */

const { spawn } = require('child_process');
const path = require('path');
const os = require('os');

const scriptName = process.argv[2];
const args = process.argv.slice(3);

if (!scriptName) {
    console.error('Usage: node run-script.js <script-name> [args...]');
    process.exit(1);
}

const scriptsDir = __dirname;
const isWindows = os.platform() === 'win32';

let scriptPath;
let command;
let commandArgs;

if (isWindows) {
    scriptPath = path.join(scriptsDir, `${scriptName}.bat`);
    command = 'cmd';
    commandArgs = ['/c', scriptPath, ...args];
} else {
    scriptPath = path.join(scriptsDir, `${scriptName}.sh`);
    command = 'bash';
    commandArgs = [scriptPath, ...args];
}

console.log(`Running: ${scriptPath} ${args.join(' ')}`);
console.log('');

const child = spawn(command, commandArgs, {
    stdio: 'inherit',
    cwd: path.dirname(scriptsDir),
    shell: false
});

child.on('error', (err) => {
    console.error(`Failed to start script: ${err.message}`);
    process.exit(1);
});

child.on('close', (code) => {
    process.exit(code || 0);
});
