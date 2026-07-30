#!/bin/bash

# 完整的重新编译和复制 Kotlin/JS 产物脚本
# 用法: ./rebuild-kotlin-and-copy.sh [dev]
#   dev - 使用开发版本编译（jsBrowserDevelopmentWebpack）
#
# 注：使用 miniApp 项目编译，产物为 miniprogramApp.js

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MINIAPP_JS_DIR="$(dirname "$SCRIPT_DIR")"
KUIKLY_ROOT="$(dirname "$MINIAPP_JS_DIR")"

# 检查是否使用开发模式
BUILD_MODE="production"
WEBPACK_TASK="jsBrowserProductionWebpack"

if [ "$1" = "dev" ]; then
  BUILD_MODE="development"
  WEBPACK_TASK="jsBrowserDevelopmentWebpack"
  echo "🔨 Rebuilding Kotlin/JS modules (DEVELOPMENT mode)..."
else
  echo "🔨 Rebuilding Kotlin/JS modules (PRODUCTION mode)..."
fi
echo ""

# 进入项目根目录
cd "$KUIKLY_ROOT"

echo "📦 Step 1/2: Compiling miniApp module..."
./gradlew :core-render-web:miniapp:clean :core-render-web:miniapp:$WEBPACK_TASK

if [ $? -ne 0 ]; then
  echo "❌ Failed to compile miniApp module"
  exit 1
fi

echo ""
echo "📦 Step 2/2: Copying compiled artifacts..."
cd "$MINIAPP_JS_DIR"
./scripts/copy-kotlin-libs.sh $1

if [ $? -ne 0 ]; then
  echo "❌ Failed to copy artifacts"
  exit 1
fi

echo ""
echo "✅ All done! You can now run 'npm run build' to build the miniapp (outputs to ./dist)."
