#!/bin/bash
set -e

rm -rf core-render-ios/include
mkdir -p core-render-ios/include

find core-render-ios -type f -name "*.h" ! -path "core-render-ios/include/*" | while read header; do
  ln -sf "../${header#core-render-ios/}" "core-render-ios/include/$(basename "$header")"
done