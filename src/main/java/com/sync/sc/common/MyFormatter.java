package com.sync.sc.common;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class MyFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(record.getMillis())))
                .append(" ")
                .append(record.getLevel().getName())
                .append(" [")
                .append(Thread.currentThread().getName())
                .append("] ")
                .append(record.getSourceClassName()).append(".").append(record.getSourceMethodName())
                .append(" - ")
                .append(formatMessage(record))
                .append("\n");
        if (record.getThrown() != null) {
            try (java.io.StringWriter sw = new java.io.StringWriter();
                 java.io.PrintWriter pw = new java.io.PrintWriter(sw)) {
                record.getThrown().printStackTrace(pw);
                sb.append(sw.toString());
            } catch (Exception ignored) {}
        }
        return sb.toString();
    }
}