package com.sync.sc.config;

import com.sync.sc.common.MyFormatter;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

public class LoggingInit {

    public static void init(Environment env) throws IOException {
        String logDir = env.getProperty("app.log.path", System.getProperty("user.home") + "/appsc/logs");
        String levelStr = env.getProperty("app.log.level", "INFO");

        // 创建目录
        File dir = new File(logDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                System.err.println("无法创建日志目录: " + logDir);
            }
        }

        // 设置日志级别
        Level level = Level.parse(levelStr.toUpperCase());

        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(level);

        // 清空默认 Handler
        for (Handler h : rootLogger.getHandlers()) {
            rootLogger.removeHandler(h);
        }

        // 控制台
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);
        consoleHandler.setFormatter(new MyFormatter());
        rootLogger.addHandler(consoleHandler);

        // 文件日志
        FileHandler fileHandler = new FileHandler(logDir + "/app_%u_%g.log", true);
        fileHandler.setLevel(level);
        fileHandler.setFormatter(new MyFormatter());
        rootLogger.addHandler(fileHandler);
    }
}