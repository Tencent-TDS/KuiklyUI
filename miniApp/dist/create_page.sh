#!/bin/bash

# Demo页面列表, 新增页面可以这里补充
demos=(
    "ComposeImageDemo"
    "TextDemo"
)

# 基础目录 (相对于当前脚本位置)
base_dir="./pages"

# 为每个demo创建目录和文件
for demo in "${demos[@]}"; do
    demo_dir="$base_dir/$demo"
    
    # 创建目录
    mkdir -p "$demo_dir"
    
    # 创建 index.js
    cat > "$demo_dir/index.js" << 'EOF'
var render = require('../../lib/miniprogramApp.js')

render.renderView({})
EOF
    
    # 创建 index.json
    cat > "$demo_dir/index.json" << EOF
{
    "navigationBarTitleText":"$demo",
    "disableScroll":true,
    "usingComponents":{"custom-wrapper":"../../custom-wrapper","comp":"../../comp"}
}
EOF
    
    # 创建 index.wxml
    cat > "$demo_dir/index.wxml" << 'EOF'
<import src="../../base.wxml"/>
<template is="kuikly_tmpl" data="{{root:root}}" />
EOF
    
    # 创建 index.wxss
    cat > "$demo_dir/index.wxss" << 'EOF'
/* pages/demo/index.wxss */
EOF
    
    echo "Created page: $demo"
done

echo "All demo pages created successfully!"