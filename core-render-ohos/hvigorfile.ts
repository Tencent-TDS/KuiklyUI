import { harTasks } from '@ohos/hvigor-ohos-plugin';
import { HvigorNode, HvigorPlugin } from '@ohos/hvigor';

export default {
  system: harTasks, /* Built-in plugin of Hvigor. It cannot be modified. */
  plugins: [
    copyRenderHeader()
  ]         /* Custom plugin to extend the functionality of Hvigor. */
}

function copyRenderHeader(): HvigorPlugin {
  return {
    pluginId: 'copyRenderHeader',
    apply(node: HvigorNode) {
      // 插件主体
      var exec = require('child_process').exec;
      const currentDirectory = process.cwd();
      const exist = process.env.MODULE_SEM_VERSIONS
      if (exist) {
        exec(`sh ./copy_header.sh`, (err, stdout, stderr) => {

        });
      }
    }
  }
}