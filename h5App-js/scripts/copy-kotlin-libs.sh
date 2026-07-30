#!/bin/bash

# 从 h5App-js 目录执行此脚本
# 用法: ./copy-kotlin-libs.sh [dev]
#   dev - 从开发版本目录复制产物
#
# 注：h5 模块已包含 base 模块代码，只需复制 h5 模块即可

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
H5APP_JS_DIR="$(dirname "$SCRIPT_DIR")"
KUIKLY_ROOT="$(dirname "$H5APP_JS_DIR")"

echo "📦 Copying Kotlin/JS compiled artifacts..."

# 创建目标目录
mkdir -p "$H5APP_JS_DIR/src/libs"

# 检查是否使用开发模式
if [ "$1" = "dev" ]; then
  BUILD_DIR="developmentExecutable"
  echo "   (using DEVELOPMENT build)"
else
  BUILD_DIR="productionExecutable"
  echo "   (using PRODUCTION build)"
fi

# 源文件路径 - 使用 webpack 打包版本（包含所有依赖）
# 注：h5 模块已包含 base 模块代码，无需单独复制 base 模块
H5_MODULE="$KUIKLY_ROOT/core-render-web/h5/build/kotlin-webpack/js/$BUILD_DIR/KuiklyCore-render-web-h5.js"

# TypeScript 声明文件
H5_DTS="$KUIKLY_ROOT/core-render-web/h5/build/compileSync/js/main/$BUILD_DIR/kotlin/KuiklyCore-render-web-h5.d.ts"

if [ ! -f "$H5_MODULE" ]; then
  echo "❌ Error: KuiklyCore-render-web-h5.js not found!"
  echo "   Expected path: $H5_MODULE"
  if [ "$1" = "dev" ]; then
    echo "   Please run: ./gradlew :core-render-web:h5:jsBrowserDevelopmentWebpack"
  else
    echo "   Please run: ./gradlew :core-render-web:h5:jsBrowserProductionWebpack"
  fi
  exit 1
fi

# 复制文件
echo "📋 Copying KuiklyCore-render-web-h5.js..."
cp "$H5_MODULE" "$H5APP_JS_DIR/src/libs/"

# 复制 TypeScript 声明文件（如果存在）
if [ -f "$H5_DTS" ]; then
  echo "📋 Copying KuiklyCore-render-web-h5.d.ts..."
  cp "$H5_DTS" "$H5APP_JS_DIR/src/libs/"
fi

echo "✅ Done! Kotlin/JS libraries copied to src/libs/"
echo ""
echo "📝 Files copied:"
echo "   - src/libs/KuiklyCore-render-web-h5.js"
if [ -f "$H5_DTS" ]; then
  echo "   - src/libs/KuiklyCore-render-web-h5.d.ts"
fi
