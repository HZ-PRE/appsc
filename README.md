### 环境
- jdk21
- javaFx版本

### 生成用的的jdk模块
- ./jdk21\bin\jlink --module-path jdk\jmods --add-modules java.base,java.logging,java.xml,jdk.xml.dom,java.sql,java.naming,java.management,java.net.http,jdk.crypto.ec,java.desktop,jdk.unsupported,jdk.jsobject,jdk.httpserver,jdk.zipfs,java.security.jgss,java.security.sasl,jdk.jfr,jdk.management --output my-runtime --strip-debug --compress zip-6 --no-header-files --no-man-pages


### 转化exe工具
- 方法一：launch4j
- 方法二（将所有jar（包括主包）都丢如lib），使用jdk自带的jpackage打包exe：
  - ./jdk/bin/jpackage  --type app-image   --name appsc  --input ./lib --main-class com.sync.sc.ScFXApp  --main-jar appsc.jar --app-version 1.5  --icon appsc.ico  --runtime-image ./jre



### 最新安装包
- https://pub-files.markoctopus.cc/other/appsc/install.zip