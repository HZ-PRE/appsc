#!/bin/bash
#回滚渠道号
rollback_array=(${1//\,/ });
#文件后缀 exe,apk
suffix=$2;
#文件类型 ym,bzy,tz
suffix_name=$3;

#文件目录
folder_path="/var/www/html/download";

echo $(date '+%Y-%m-%d %H:%M:%S')':'$4'->回滚'$3'.'$2'：'$1 >> /var/www/exeym/logs/log.log
for channel in "${rollback_array[@]}"; do
	mv $folder_path/$channel/backups/$suffix/$suffix_name.$suffix $folder_path/$channel/$suffix_name.$suffix;
done
