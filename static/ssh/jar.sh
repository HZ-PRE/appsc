#!/bin/bash
#文件后缀 exe,apk
suffix="${1##*.}";
#文件类型 ym,bzy,tz
suffix_name=$2;
#目标文件目录
folder_path="/var/www/html/download";
#灰度目录
GRAY_DIR="/var/www/html/download_gray"
#测试目录
TEXT_DIR="/var/www/html/public/text"
#以上的根据需求条件替换

echo "SUFFIX: $suffix"
echo "SUFFIX_NAME: $suffix_name"
# 函数：提取连续 >=4 位数字
get_nums4() {
    local str="$1"
    # 使用 grep -oE 匹配连续4位及以上数字
    # 输出所有匹配，用空格分隔
    echo "$str" | grep -oE '[0-9]{4,}'
}

# 进入文件夹
cd /var/www/exeym/data/work
if [ -e "$4" ] & [ "$4" = "gray" ]; then
	folder_path=$GRAY_DIR
fi
if [ -e "$4" ] & [ "$4" = "text" ]; then
	folder_path=$TEXT_DIR
fi
if [ -e "$1" ]; then
    if [ "$suffix" = "exe" ] || [ "$suffix" = "apk" ]; then
        echo $(date '+%Y-%m-%d %H:%M:%S')':'$3'  '$1'->'$4$suffix'上包'$suffix_name >> /var/www/exeym/logs/log.log
        #加.后缀
        suffix_str="."$suffix;
        result=$(get_nums4 "$1")
        file_path=$folder_path/$result;
         mkdir -p $file_path
        if [ -z "${4:-}" ] || [ "$4" = "pub" ]; then
             mkdir -p $file_path/backups/$suffix;
            # 查找并列出七天前创建的文件夹
#             find $file_path/backups/$suffix -type d -mtime +7 -exec rm -rf {} \;
	        mv $file_path/$suffix_name$suffix_str $file_path/backups/$suffix/$suffix_name$suffix_str;
             echo $file_path/backups/$suffix_name$suffix_str;
        fi
        mv $1 $file_path/$suffix_name$suffix_str;
        echo "$file_path/$suffix_name$suffix_str匹配结果: $result"
    elif [ "$suffix" = "zip" ]; then
        # 解压文件
        rm -r pro
        unzip -o $1 -d pro
        rm -r $1
        # 遍历文件夹中的所有文件
        for file in $(find ./pro \( -name "*.exe" -o -name "*.apk" \) -type f); do
         filename=$(basename "$file");
         echo $(date '+%Y-%m-%d %H:%M:%S')':'$3'  '$filename'->'$4$suffix'上包'$suffix_name >> /var/www/exeym/logs/log.log
         suffix="${filename##*.}";
         #加.后缀
         suffix_str="."$suffix;
         result=$(get_nums4 "$filename")
         file_path=$folder_path/$result;
         mkdir -p $file_path
         if [ -z "${4:-}" ] || [ "$4" = "pub" ]; then
            mkdir -p $file_path/backups/$suffix;
            # 查找并列出七天前创建的文件夹
#            find $file_path/backups/$suffix -type d -mtime +7 -exec rm -rf {} \;
            mv $file_path/$suffix_name$suffix_str $file_path/backups/$suffix/$suffix_name$suffix_str;
            echo $file_path/backups/$suffix_name$suffix_str;
         fi
         mv $file $file_path/$suffix_name$suffix_str;
        done
    fi
else
    echo "$1 不存在"
fi




