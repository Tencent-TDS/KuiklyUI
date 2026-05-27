# Mobile Test 脚本

业务 / 任务级 E2E 脚本放这里，**不要**放进 `.claude/skills/kuikly-mobile-test/`（harness）。

| 目录 | 职责 |
|------|------|
| `scripts/mobile-test/` | 测试脚本源码（可提交 git，**在 checkout / worktree 内**） |
| `.claude/skills/kuikly-mobile-test/src/` | harness：MobileDriver、HTTP Server、`run-mobile-test.ts` |
| `<checkout>/logs/` | **运行产物**（session log、报告、快照；gitignore） |

## 运行

在 **checkout 根**（skill 常 symlink 到 harness，勿单独 `cd` 进 skill 裸跑）：

```bash
npm run lazy-prefetch:e2e --prefix .claude/skills/kuikly-mobile-test
IOS_UDID=<sim> npm run lazy-prefetch:ios-native --prefix .claude/skills/kuikly-mobile-test
```

或 `export KUIKLY_REPO_ROOT=/path/to/checkout` 后再 `npm run`。也可 `npx tsx scripts/mobile-test/<脚本>.ts`（cwd 在 checkout 根）。

## 新增脚本

1. 在本目录新建 `*.ts`，从 `../../.claude/skills/kuikly-mobile-test/src/` 引用 harness
2. 证据写入 `logs/`（用 `paths.ts` 的 `LOGS_DIR`）
3. 在 skill 的 `package.json` 增加：`"my-case": "tsx src/run-mobile-test.ts my-case.ts"`
