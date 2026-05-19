#!/bin/bash

# Create a new page for miniApp-js
# Usage: ./create_page.sh PageName

if [ -z "$1" ]; then
    echo "Usage: ./create_page.sh PageName"
    exit 1
fi

PAGE_NAME=$1
PAGE_DIR="pages/$PAGE_NAME"

# Create page directory
mkdir -p "$PAGE_DIR"

# Create index.js
cat > "$PAGE_DIR/index.js" << EOF
var render = require('../../lib/miniprogramApp.js')

render.renderView({})
EOF

# Create index.json
cat > "$PAGE_DIR/index.json" << EOF
{
  "usingComponents": {
    "comp": "../../comp",
    "custom-wrapper": "../../custom-wrapper"
  }
}
EOF

# Create index.wxml
cat > "$PAGE_DIR/index.wxml" << EOF
<import src="../../base.wxml"/>
<template is="kuikly_tmpl" data="{{root:root}}" />
EOF

# Create index.wxss
cat > "$PAGE_DIR/index.wxss" << EOF
/* pages/$PAGE_NAME/index.wxss */
EOF

echo "✅ Created page: $PAGE_NAME"
echo "Don't forget to add \"pages/$PAGE_NAME/index\" to app.json"
