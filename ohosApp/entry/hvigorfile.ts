import { hapTasks } from '@ohos/hvigor-ohos-plugin';
import * as fs from 'fs';
import * as path from 'path';
import * as https from 'https';

export default {
  system: hapTasks, /* Built-in plugin of Hvigor. It cannot be modified. */
  plugins: [kuiklyPullOhosProduct(), kuiklyCopyAssets()]   /* Custom plugin to extend the functionality of Hvigor. */
}

// 用于首次拉取ohos产物
function kuiklyPullOhosProduct(): HvigorPlugin {
  return {
    pluginId: 'kuiklyPullOhosProductPlugin',
    apply(node: HvigorNode) {
      node.registerTask({
        name: 'kuikly_pull_ohos_product',
        run: () => {
          const soDir = path.join(node.getNodePath(), 'libs', 'arm64-v8a');
          const soFile = path.join(soDir, 'libshared.so');
          const apiDir = path.join(node.getNodePath(), 'src', 'main', 'cpp', 'thirdparty', 'biz_entry');
          const apiFile = path.join(apiDir, 'libshared_api.h');
          const soDownloadUrl = 'https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/d88d0cf7.so';
          const apiDownloadUrl = 'https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/3f86ae77.h';
          try {
            const shouldDownload = !fs.existsSync(soFile) || !fs.existsSync(apiFile);
            if (!shouldDownload) {
              return
            }
            console.error('\x1b[31m%s\x1b[0m', '\n═══════════════════════════════════════════════');
            console.error('\x1b[31m%s\x1b[0m', '     ⚠️⚠️⚠️⚠️ACTION REQUIRED⚠️⚠️⚠️⚠️');
            console.error('\x1b[31m%s\x1b[0m', 'Please compile the kuikly ohos product first');
            console.error('\x1b[31m%s\x1b[0m', 'And copy them to specified folder');
            console.error('\x1b[31m%s\x1b[0m', `so to ${soFile}`);
            console.error('\x1b[31m%s\x1b[0m', `.h to ${apiFile}`);
            console.error('\x1b[31m%s\x1b[0m', '═══════════════════════════════════════════════\n');
            console.log('kuikly pull ohos product start');
            ensureFileExists(soDir, soFile, soDownloadUrl),
            ensureFileExists(apiDir, apiFile, apiDownloadUrl)
            console.log('kuikly pull ohos product finish');
          } catch (err) {
            console.error('\x1b[31mPull failed:', err, '\x1b[0m');
            throw err;
          }
        },
        postDependencies: ['default@PreBuild']
      })
    }
  }
}


function ensureFileExists(dir: String, file: String, url: String) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
    console.log(`Created directory: ${dir}`);
  }
  return downloadFile(url, file);
}

function downloadFile(url: string, dest: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest);
    https.get(url, (response) => {
      if (response.statusCode !== 200) {
        fs.unlinkSync(dest);
        return reject(new Error(`HTTP ${response.statusCode}`));
      }

      response.pipe(file);
      file.on('finish', () => {
        file.close((err) => err ? reject(err) : resolve());
      });
    }).on('error', (err) => {
      fs.unlinkSync(dest);
      reject(err);
    });
  });
}

// 编译时copy assets
function kuiklyCopyAssets(): HvigorPlugin {
  return {
    pluginId: 'kuiklyCopyAssetsPlugin',
    apply(node: HvigorNode) {
      node.registerTask({
        name: 'kuikly_copy_assets',
        run: (taskContext) => {
          console.log('kuikly copy assets start');
          const sourceDir = path.join(node.getNodePath(),
            '..', '..', 'demo', 'src', 'commonMain', 'assets');
          const destDir = path.join(node.getNodePath(),
            'build', 'default', 'intermediates', 'res', 'default', 'resources', 'resfile');
          console.log(`assets file copy from: ${sourceDir}`);
          console.log(`assets file copy to: ${destDir}`);
          if (!fs.existsSync(destDir)) {
            fs.mkdirSync(destDir, { recursive: true });
          }
          fs.cpSync(sourceDir, destDir, {
            recursive: true,
            force: true,
          });
          console.log('kuikly copy assets finish');
        },
        dependencies: [`default@CompileResource`],
        postDependencies: [`default@CompileArkTS`]
      })
    }
  }
}