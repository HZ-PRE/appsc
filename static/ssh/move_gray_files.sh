#!/bin/bash
GRAY_DIR="/var/www/html/download_gray"
FULL_DIR="/var/www/html/download"

# 遍历灰度目录下的所有文件，找到7天前的，保持目录结构迁移到全量目录
find "$GRAY_DIR" -type f -mtime +7 | while read filepath; do
    # 计算相对路径
    rel_path="${filepath#$GRAY_DIR/}"
    target_dir="$FULL_DIR/$(dirname "$rel_path")"
    ext="${rel_path##*.}"
    filename=$(basename "$rel_path")
    # 确保目标目录存在
    mkdir -p "$target_dir/backups/$ext"
    #备份
    mv $target_dir/$filename $target_dir/backups/$ext/$filename;
#    # 移动文件
    mv "$filepath" "$target_dir/"
    echo "Moved: $filepath -> $target_dir/"
done

# 热加载 Nginx（可选）
nginx -s reload
