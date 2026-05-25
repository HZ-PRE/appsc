package com.sync.sc.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class SimpleS3Uploader {
    private static final String SERVICE = "s3";
    private static final String HASH_UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
    private static final DateTimeFormatter AMZ_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT);
    private static final DateTimeFormatter SHORT_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);

    private SimpleS3Uploader() {
    }

    static void putAws(String region, String bucket, String key, String accessKey, String secretKey,
                       InputStream inputStream, long contentLength, boolean publicRead) throws IOException {
        String host = bucket + ".s3." + region + ".amazonaws.com";
        URI uri = URI.create("https://" + host + "/" + encodePath(key));
        put(uri, "/" + encodePath(key), region, accessKey, secretKey, inputStream, contentLength, publicRead);
    }

    static void putR2(String endpoint, String bucket, String key, String accessKey, String secretKey,
                      InputStream inputStream, long contentLength) throws IOException {
        URI base = URI.create(endpoint.startsWith("http") ? endpoint : "https://" + endpoint);
        String basePath = trimRight(base.getPath(), "/");
        String canonicalUri = basePath + "/" + encodePath(bucket) + "/" + encodePath(key);
        URI uri = base.resolve(canonicalUri);
        put(uri, canonicalUri, "auto", accessKey, secretKey, inputStream, contentLength, false);
    }

    private static void put(URI uri, String canonicalUri, String region, String accessKey, String secretKey,
                            InputStream inputStream, long contentLength, boolean publicRead) throws IOException {
        ZonedDateTime now = ZonedDateTime.now(java.time.ZoneOffset.UTC);
        String amzDate = AMZ_DATE.format(now);
        String date = SHORT_DATE.format(now);
        String host = uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");

        TreeMap<String, String> headers = new TreeMap<>();
        headers.put("content-type", "application/octet-stream");
        headers.put("host", host);
        if (publicRead) {
            headers.put("x-amz-acl", "public-read");
        }
        headers.put("x-amz-content-sha256", HASH_UNSIGNED_PAYLOAD);
        headers.put("x-amz-date", amzDate);

        String signedHeaders = String.join(";", headers.keySet());
        StringBuilder canonicalHeaders = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            canonicalHeaders.append(entry.getKey()).append(':').append(entry.getValue().trim()).append('\n');
        }

        String canonicalRequest = "PUT\n"
                + canonicalUri + "\n\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + HASH_UNSIGNED_PAYLOAD;
        String scope = date + "/" + region + "/" + SERVICE + "/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n"
                + amzDate + "\n"
                + scope + "\n"
                + hex(sha256(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        String signature = hex(hmac(signingKey(secretKey, date, region), stringToSign));

        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(600000);
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setFixedLengthStreamingMode(contentLength);
        conn.setRequestProperty("Authorization",
                "AWS4-HMAC-SHA256 Credential=" + accessKey + "/" + scope
                        + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (!"host".equals(entry.getKey())) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        try (OutputStream out = conn.getOutputStream()) {
            inputStream.transferTo(out);
        }
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            String message;
            try (InputStream error = conn.getErrorStream()) {
                message = error == null ? conn.getResponseMessage() : new String(error.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new IOException("S3 upload failed: " + code + ", " + message);
        }
        conn.disconnect();
    }

    private static byte[] signingKey(String secretKey, String date, String region) {
        byte[] kDate = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] kRegion = hmac(kDate, region);
        byte[] kService = hmac(kRegion, SERVICE);
        return hmac(kService, "aws4_request");
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static String encodePath(String path) {
        String[] parts = path.replace("\\", "/").split("/", -1);
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return encoded.toString();
    }

    private static String trimRight(String value, String suffix) {
        String result = value == null ? "" : value;
        while (result.endsWith(suffix)) {
            result = result.substring(0, result.length() - suffix.length());
        }
        return result;
    }
}
