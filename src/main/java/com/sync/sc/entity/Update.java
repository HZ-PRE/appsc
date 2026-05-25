package com.sync.sc.entity;

import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

public class Update implements Serializable {

    private static final long serialVersionUID = 1L;

    private MultipartFile appfile;
    private Integer chunkSizeMB;
    private Integer threads;
    private String filename;
    private String filePath; // 本地保存的文件路径（用于异步上传）
    private String appReleaseType;

    private String bucketName;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;

    private String appType;
    private String appTypeConf;
    private String user;
    private String host;
    private int port;// 默认 SSH 端口
    private String privateKeyPath;// SSH 私钥路径
    private String remoteDir;// 远程目标目录
    private String command;// 远程目标sh
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public MultipartFile getAppfile() {
        return appfile;
    }

    public void setAppfile(MultipartFile appfile) {
        this.appfile = appfile;
    }

    public Integer getChunkSizeMB() {
        return chunkSizeMB;
    }

    public void setChunkSizeMB(Integer chunkSizeMB) {
        this.chunkSizeMB = chunkSizeMB;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getAppReleaseType() {
        return appReleaseType;
    }

    public void setAppReleaseType(String appReleaseType) {
        this.appReleaseType = appReleaseType;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getAppTypeConf() {
        return appTypeConf;
    }

    public void setAppTypeConf(String appTypeConf) {
        this.appTypeConf = appTypeConf;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getRemoteDir() {
        return remoteDir;
    }

    public void setRemoteDir(String remoteDir) {
        this.remoteDir = remoteDir;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}

