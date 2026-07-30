#!/bin/bash

# 从 miniApp-js 目录执行此脚本
# 用法: ./copy-kotlin-libs.sh [dev]
#   dev - 从开发版本目录复制产物
#
# 注：miniapp 模块已包含 base 模块代码，只需复制 miniapp 模块即可

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MINIAPP_JS_DIR="$(dirname "$SCRIPT_DIR")"
KUIKLY_ROOT="$(dirname "$MINIAPP_JS_DIR")"

echo "📦 Copying Kotlin/JS compiled artifacts..."

# 创建目标目录
mkdir -p "$MINIAPP_JS_DIR/src/libs"

# 检查是否使用开发模式
if [ "$1" = "dev" ]; then
  BUILD_DIR="developmentExecutable"
  echo "   (using DEVELOPMENT build)"
else
  BUILD_DIR="productionExecutable"
  echo "   (using PRODUCTION build)"
fi

# 源文件路径 - 使用 webpack 打包版本（包含所有依赖）
# 注：miniapp 模块已包含 base 模块代码，无需单独复制 base 模块
MINIAPP_MODULE="$KUIKLY_ROOT/core-render-web/miniapp/build/kotlin-webpack/js/$BUILD_DIR/KuiklyCore-render-web-miniapp.js"

# TypeScript 声明文件
MINIAPP_DTS="$KUIKLY_ROOT/core-render-web/miniapp/build/compileSync/js/main/$BUILD_DIR/kotlin/KuiklyCore-render-web-miniapp.d.ts"

# 备选：从 miniApp 项目复制（如果 core-render-web 没有构建）
MINIAPP_FALLBACK="$KUIKLY_ROOT/miniApp/dist/lib/miniprogramApp.js"

if [ -f "$MINIAPP_MODULE" ]; then
  # 复制文件
  echo "📋 Copying KuiklyCore-render-web-miniapp.js..."
  cp "$MINIAPP_MODULE" "$MINIAPP_JS_DIR/src/libs/"
  
  # 复制 TypeScript 声明文件（如果存在）
  if [ -f "$MINIAPP_DTS" ]; then
    echo "📋 Copying KuiklyCore-render-web-miniapp.d.ts..."
    cp "$MINIAPP_DTS" "$MINIAPP_JS_DIR/src/libs/"
  fi
  
  echo "✅ Done! Kotlin/JS libraries copied to src/libs/ (ready for webpack -> dist)"
  echo ""
  echo "📝 Files copied:"
  echo "   - src/libs/KuiklyCore-render-web-miniapp.js"
  if [ -f "$MINIAPP_DTS" ]; then
    echo "   - src/libs/KuiklyCore-render-web-miniapp.d.ts"
  fi
elif [ -f "$MINIAPP_FALLBACK" ]; then
  echo "📋 Copying miniprogramApp.js from miniApp project..."
  cp "$MINIAPP_FALLBACK" "$MINIAPP_JS_DIR/src/libs/"
  
  echo "✅ Done! Kotlin/JS libraries copied to src/libs/ (ready for webpack -> dist)"
  echo ""
  echo "📝 Files copied:"
  echo "   - src/libs/miniprogramApp.js"
else
  echo "❌ Error: KuiklyCore-render-web-miniapp.js not found!"
  echo "   Expected path: $MINIAPP_MODULE"
  echo "   Fallback path: $MINIAPP_FALLBACK"
  if [ "$1" = "dev" ]; then
    echo "   Please run: ./gradlew :core-render-web:miniapp:jsBrowserDevelopmentWebpack"
    echo "   Or run: ./gradlew :miniApp:jsBrowserDevelopmentWebpack"
  else
    echo "   Please run: ./gradlew :core-render-web:miniapp:jsBrowserProductionWebpack"
    echo "   Or run: ./gradlew :miniApp:jsBrowserProductionWebpack"
  fi
  exit 1
fi
