#!/usr/bin/env bash
# 已迁移到 semi-auto-912-e2e.ts（含 9.12 断言与报告）。保留本脚本作快捷入口。
set -euo pipefail
REPO_ROOT="${KUIKLY_REPO_ROOT:-$(cd "$(dirname "$0")/../.." && pwd)}"
cd "$REPO_ROOT"
export KUIKLY_REPO_ROOT="$REPO_ROOT"
export ANDROID_UDID="${ANDROID_UDID:-15490798770018J}"
export MOBILE_TEST_PORT="${MOBILE_TEST_PORT:-7902}"
exec npm run semi-auto:912 --prefix .claude/skills/kuikly-mobile-test
