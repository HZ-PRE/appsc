package com.sync.sc.entity;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.Serializable;

public class Mail implements Serializable {

    private static final long serialVersionUID = 1L;

    private String smtpHost;
    private Integer smtpPort; // 587 for STARTTLS, 465 for SSL
    private String smtpUser;
    private String smtpPass;
    private boolean useSsl;// true if using port 465 (SSL)
    private String fromUserName;
    private String fromUser;
    private String content;
    private String subject;
    private String to;
    private File inlineImage;
    private File attachment;
    private String appTypeConf;
    private MultipartFile file;
    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }
    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }
    public String getAppTypeConf() {
        return appTypeConf;
    }

    public void setAppTypeConf(String appTypeConf) {
        this.appTypeConf = appTypeConf;
    }

    public File getInlineImage() {
        return inlineImage;
    }

    public void setInlineImage(File inlineImage) {
        this.inlineImage = inlineImage;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public File getAttachment() {
        return attachment;
    }

    public void setAttachment(File attachment) {
        this.attachment = attachment;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean getUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    public String getSmtpPass() {
        return smtpPass;
    }

    public void setSmtpPass(String smtpPass) {
        this.smtpPass = smtpPass;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public void setSmtpUser(String smtpUser) {
        this.smtpUser = smtpUser;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }
}

