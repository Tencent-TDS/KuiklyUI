#!/bin/bash

src_dir="$PWD/ohos_render/src/main/cpp"
dest_dir="$PWD/include"
zip_file="$PWD/headers.zip"

find "$src_dir" -type f -name "*.h" | while read -r header_file; do
    # 获取源文件的相对路径
    rel_path="${header_file#$src_dir}"

    # 构造目标文件的路径
    dest_file="$dest_dir/$rel_path"

    # 创建目标文件所在的目录
    mkdir -p "$(dirname "$dest_file")"

    # 复制源文件到目标位置
    cp "$header_file" "$dest_file"
done

mv "$dest_dir" "$src_dir/types"

#cd "$dest_dir" && zip -r "$zip_file" . && cd - > /dev/null