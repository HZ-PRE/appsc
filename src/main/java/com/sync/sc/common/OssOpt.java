package com.sync.sc.common;

import com.sync.sc.entity.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.sync.sc.util.BaseUtil.*;

@Component
public class OssOpt {
    /** 阿里上传文件 */
    public String aliOssUploadFile(Update ossUpdate) throws IOException {
        if(!StringUtils.isEmpty(ossUpdate.getRemoteDir())){
            ossUpdate.setFilename((ossUpdate.getRemoteDir().endsWith("/")?ossUpdate.getRemoteDir().substring(0,ossUpdate.getRemoteDir().length()-1):ossUpdate.getRemoteDir())+ "/" +ossUpdate.getFilename());
        }
        String ret  = "https://" + ossUpdate.getBucketName() + "." + ossUpdate.getEndpoint() + "/" +ossUpdate.getFilename();
        if("0".equals(ossUpdate.getAppReleaseType()) && SimpleHttpsGet(ret)){
            return "1:"+ret;
        }
        putAliOssObject(ossUpdate);
        return ret;
    }
    /** 亚马逊上传文件 */
    public String awsOssUploadFile(Update ossUpdate) throws IOException {
        if(!StringUtils.isEmpty(ossUpdate.getRemoteDir())){
            ossUpdate.setFilename((ossUpdate.getRemoteDir().endsWith("/")?ossUpdate.getRemoteDir().substring(0,ossUpdate.getRemoteDir().length()-1):ossUpdate.getRemoteDir())+ "/" +ossUpdate.getFilename());
        }
        String ret  = "https://" + ossUpdate.getBucketName() + ".s3."+ossUpdate.getEndpoint()+".amazonaws.com/" + ossUpdate.getFilename();
        if("0".equals(ossUpdate.getAppReleaseType()) && SimpleHttpsGet(ret)){
            return "1:"+ret;
        }
        try (InputStream inputStream = ossUpdate.getAppfile().getInputStream()) {
            SimpleS3Uploader.putAws(
                    ossUpdate.getEndpoint(),
                    ossUpdate.getBucketName(),
                    ossUpdate.getFilename(),
                    ossUpdate.getAccessKeyId(),
                    ossUpdate.getAccessKeySecret(),
                    inputStream,
                    ossUpdate.getAppfile().getSize(),
                    true
            );
        }
        return ret;
    }
    /** 泛播上传文件 */
    public String cloudflareUploadFile(Update ossUpdate) throws IOException {
        if(!StringUtils.isEmpty(ossUpdate.getRemoteDir())){
            ossUpdate.setFilename((ossUpdate.getRemoteDir().endsWith("/")?ossUpdate.getRemoteDir().substring(0,ossUpdate.getRemoteDir().length()-1):ossUpdate.getRemoteDir())+ "/" +ossUpdate.getFilename());
        }
        String ret  = ossUpdate.getPrivateKeyPath() + "/" + ossUpdate.getFilename();
        if("0".equals(ossUpdate.getAppReleaseType()) && SimpleHttpsGet(ret)){
            return "1:"+ret;
        }
        try (InputStream inputStream = ossUpdate.getAppfile().getInputStream()) {
            SimpleS3Uploader.putR2(
                    ossUpdate.getEndpoint(),
                    ossUpdate.getBucketName(),
                    ossUpdate.getFilename(),
                    ossUpdate.getAccessKeyId(),
                    ossUpdate.getAccessKeySecret(),
                    inputStream,
                    ossUpdate.getAppfile().getSize()
            );
        }
        return ret;
    }
    /** github上传文件 */
    public String githubOssUploadFile(Update ossUpdate) throws Exception {
        if(!StringUtils.isEmpty(ossUpdate.getRemoteDir())){
            ossUpdate.setFilename((ossUpdate.getRemoteDir().endsWith("/")?ossUpdate.getRemoteDir().substring(0,ossUpdate.getRemoteDir().length()-1):ossUpdate.getRemoteDir())+ "/" +ossUpdate.getFilename());
        }
        String ret  = "https://raw.githubusercontent.com/" + ossUpdate.getAccessKeyId() + "/" + ossUpdate.getEndpoint() + "/"+ossUpdate.getBucketName()+"/" + ossUpdate.getFilename();
        if("0".equals(ossUpdate.getAppReleaseType()) && SimpleHttpsGet(ret)){
            return "1:"+ret;
        }
        try (InputStream fileStream = ossUpdate.getAppfile().getInputStream()) {
            byte[] fileBytes = fileStream.readAllBytes();
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);
            // 获取 SHA（检查文件是否存在）
            String sha = getGithubFileSha(ossUpdate);
            String json;
            if (sha != null) {
                json = "{"
                        + "\"message\":\"创建 " + ossUpdate.getFilename() + "\","
                        + "\"content\":\"" + base64Content + "\","
                        + "\"branch\":\"" + ossUpdate.getBucketName() + "\","
                        + "\"sha\":\"" + sha + "\""
                        + "}";
            } else {
                json = "{"
                        + "\"message\":\"更新 " + ossUpdate.getFilename() + "\","
                        + "\"content\":\"" + base64Content + "\","
                        + "\"branch\":\"" + ossUpdate.getBucketName() + "\""
                        + "}";
            }
            String url = "https://api.github.com/repos/" + ossUpdate.getAccessKeyId() + "/" + ossUpdate.getEndpoint() + "/contents/" + ossUpdate.getFilename();
            int statusCode = putJson(url, ossUpdate.getAccessKeySecret(), json);
            if (statusCode == 200 || statusCode == 201) {
                return ret;
            } else {
                throw new IOException("上传失败: " + statusCode);
            }
        }
    }

    private void putAliOssObject(Update ossUpdate) throws IOException {
        String objectKey = normalizeObjectKey(ossUpdate.getFilename());
        String encodedKey = encodeObjectKey(objectKey);
        String url = "https://" + ossUpdate.getBucketName() + "." + ossUpdate.getEndpoint() + "/" + encodedKey;
        String date = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
                .format(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC));
        String contentType = "application/octet-stream";
        String canonicalResource = "/" + ossUpdate.getBucketName() + "/" + objectKey;
        String stringToSign = "PUT\n\n" + contentType + "\n" + date + "\n" + canonicalResource;
        String signature = hmacSha1Base64(ossUpdate.getAccessKeySecret(), stringToSign);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(600000);
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setFixedLengthStreamingMode(ossUpdate.getAppfile().getSize());
        conn.setRequestProperty("Date", date);
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Authorization", "OSS " + ossUpdate.getAccessKeyId() + ":" + signature);
        try (InputStream in = ossUpdate.getAppfile().getInputStream();
             OutputStream out = conn.getOutputStream()) {
            in.transferTo(out);
        }
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            String msg;
            try (InputStream error = conn.getErrorStream()) {
                msg = error == null ? conn.getResponseMessage() : new String(error.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new IOException("Ali OSS upload failed: " + code + ", " + msg);
        }
        conn.disconnect();
    }

    private int putJson(String url, String token, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(600000);
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setFixedLengthStreamingMode(bytes.length);
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        try (OutputStream out = conn.getOutputStream()) {
            out.write(bytes);
        }
        int code = conn.getResponseCode();
        conn.disconnect();
        return code;
    }

    private String hmacSha1Base64(String secret, String data) throws IOException {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private String normalizeObjectKey(String key) {
        return key.replace("\\", "/");
    }

    private String encodeObjectKey(String key) {
        String[] parts = normalizeObjectKey(key).split("/", -1);
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    .replace("%7E", "~"));
        }
        return encoded.toString();
    }
}
