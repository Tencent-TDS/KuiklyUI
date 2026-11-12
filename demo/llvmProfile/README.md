# llvmProfile

本目录存放 iOS PGO 数据：

| 文件 | 说明 |
|------|------|
| `profraw/*.profraw` | 插装 App 运行后从设备/模拟器导出的原始 profile |
| `real_ios.profdata` | `merge_profraw.sh` 合并后的产物，MachineOutliner 编译读取 |

详见 [machine-outliner-pgo-guide.md](../../docs/DevGuide/machine-outliner-pgo-guide.md)。
