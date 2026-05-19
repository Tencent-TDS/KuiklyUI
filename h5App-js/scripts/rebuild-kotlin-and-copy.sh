#!/bin/bash

# 完整的重新编译和复制 Kotlin/JS 产物脚本
# 用法: ./rebuild-kotlin-and-copy.sh [dev]
#   dev - 使用开发版本编译（jsBrowserDevelopmentWebpack）
#
# 注：h5 模块已包含 base 模块代码，只需编译 h5 模块即可

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
H5APP_JS_DIR="$(dirname "$SCRIPT_DIR")"
KUIKLY_ROOT="$(dirname "$H5APP_JS_DIR")"

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

echo "📦 Step 1/2: Compiling core-render-web:h5 (includes base module)..."
./gradlew :core-render-web:h5:clean :core-render-web:h5:$WEBPACK_TASK

if [ $? -ne 0 ]; then
  echo "❌ Failed to compile h5 module"
  exit 1
fi

echo ""
echo "📦 Step 2/2: Copying compiled artifacts..."
cd "$H5APP_JS_DIR"
./scripts/copy-kotlin-libs.sh $1

if [ $? -ne 0 ]; then
  echo "❌ Failed to copy artifacts"
  exit 1
fi

echo ""
echo "✅ All done! You can now run 'npm start' to start the dev server."
