#!/usr/bin/env bash
#
# 注册 GitHub self-hosted runner（macOS + vivo 真机线）
#
# 用法：bash ci/setup-selfhosted-runner.sh
#
# 做什么：
#   1. 下载 actions-runner（arm64 macOS）到 ~/actions-runner
#   2. 用 gh 自动申请 registration token（免去手动去网页复制）
#   3. 以 label `self-hosted,macos` 注册到 nikazhao/KuiklyUI
#   4. 装成 launchd 服务后台常驻（重启自动拉起）
#
# 为什么要 label macos：workflow 里 e2e-device 的 runs-on: [self-hosted, macos]
# 必须两个 label 都命中才会派发。
#
# 幂等：已注册过会先提示，可选择重装。
set -uo pipefail

REPO="nikazhao/KuiklyUI"
RUNNER_DIR="$HOME/actions-runner"
RUNNER_VERSION="2.336.0"

command -v gh >/dev/null 2>&1 || { echo "!! 需要 gh CLI"; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "!! gh 未登录，先跑 gh auth login"; exit 1; }

ARCH="$(uname -m)"
case "$ARCH" in
  arm64) PKG="actions-runner-osx-arm64-${RUNNER_VERSION}.tar.gz" ;;
  x86_64) PKG="actions-runner-osx-x64-${RUNNER_VERSION}.tar.gz" ;;
  *) echo "!! 不支持的架构: $ARCH"; exit 1 ;;
esac

# ---- 已存在则先卸载旧服务 ----
if [ -d "$RUNNER_DIR" ]; then
  echo "==> 检测到已有 $RUNNER_DIR"
  read -r -p "    先卸载旧 runner 再重装？[y/N] " ans
  if [ "$ans" = "y" ] || [ "$ans" = "Y" ]; then
    ( cd "$RUNNER_DIR" && sudo ./svc.sh uninstall 2>/dev/null; \
      ./config.sh remove --token "$(gh api -X POST "repos/$REPO/actions/runners/remove-token" --jq .token)" 2>/dev/null )
    rm -rf "$RUNNER_DIR"
  else
    echo "    保留现有 runner，退出。"; exit 0
  fi
fi

# ---- 下载 ----
echo "==> 下载 runner $RUNNER_VERSION ($ARCH)"
mkdir -p "$RUNNER_DIR" && cd "$RUNNER_DIR"
curl -fsSL -o "$PKG" \
  "https://github.com/actions/runner/releases/download/v${RUNNER_VERSION}/${PKG}" \
  || { echo "!! 下载失败"; exit 1; }
tar xzf "$PKG" && rm -f "$PKG"

# ---- 申请 token 并注册 ----
echo "==> 申请 registration token"
REG_TOKEN="$(gh api -X POST "repos/$REPO/actions/runners/registration-token" --jq .token)" \
  || { echo "!! 申请 token 失败（需要仓库 admin 权限）"; exit 1; }

echo "==> 注册到 $REPO（labels: self-hosted,macos）"
./config.sh \
  --url "https://github.com/$REPO" \
  --token "$REG_TOKEN" \
  --name "$(scutil --get LocalHostName 2>/dev/null || hostname)-vivo" \
  --labels "macos" \
  --work "_work" \
  --unattended \
  --replace \
  || { echo "!! 注册失败"; exit 1; }

# ---- 装成后台服务 ----
echo "==> 安装为 launchd 服务（开机自启）"
./svc.sh install && ./svc.sh start

echo
echo "==> 完成。校验："
echo "    gh api repos/$REPO/actions/runners --jq '.runners[]|{name,status,labels:[.labels[].name]}'"
echo
echo "  停止： cd $RUNNER_DIR && ./svc.sh stop"
echo "  卸载： cd $RUNNER_DIR && sudo ./svc.sh uninstall"
