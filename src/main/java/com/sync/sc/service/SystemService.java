package com.sync.sc.service;

import com.sync.sc.entity.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

@Service
public class SystemService {
    private static final Logger log = Logger.getLogger(SystemService.class.getName());
    @Value("${hc.path}")
    private String hcPath;
    @Autowired
    private ClientService clientService;


    /**
     * 下载远程文件到指定目录，支持断点续传和下载进度显示
     *
     * @param fileUrl    远程文件 URL
     * @param targetPath 保存文件完整路径
     * @throws IOException
     */
    @Async
    public void downloadFile(String fileUrl, String targetPath,String progressName) {
        Config config =new Config();
        try {
            if(!StringUtils.isEmpty(progressName)){
                config.setType(progressName).setVal(0);
                clientService.updateConfigData(config);
            }
            File targetFile = new File(targetPath);
            File parentFile = targetFile.getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }
            // 获取已下载文件长度，实现断点续传
            long downloadedSize = targetFile.exists() ? targetFile.length() : 0;

            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Accept-Encoding", "identity"); // 避免服务器 gzip 影响断点续传
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(600000); // 10分钟防止大文件超时

            // 设置断点续传头
            if (downloadedSize > 0) {
                conn.setRequestProperty("Range", "bytes=" + downloadedSize + "-");
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK
                    && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw new IOException("文件下载失败，HTTP响应码：" + responseCode);
            }

            if (downloadedSize > 0 && responseCode == HttpURLConnection.HTTP_OK) {
                downloadedSize = 0;
            }

            long totalSize = conn.getContentLengthLong() + downloadedSize;

            try (InputStream in = conn.getInputStream();
                 BufferedInputStream bis = new BufferedInputStream(in);
                 RandomAccessFile raf = new RandomAccessFile(targetFile, "rw")) {

                // 如果断点续传，跳到已下载位置
                if (downloadedSize == 0) {
                    raf.setLength(0);
                }
                raf.seek(downloadedSize);

                byte[] buffer = new byte[64 * 1024]; // 64KB缓冲
                int bytesRead;
                long currentSize = downloadedSize;
                long lastPrintTime = System.currentTimeMillis();

                while ((bytesRead = bis.read(buffer)) != -1) {
                    raf.write(buffer, 0, bytesRead);
                    currentSize += bytesRead;

                    // 每隔1秒打印一次进度
                    if (System.currentTimeMillis() - lastPrintTime > 1000) {
                        double progress = currentSize * 100.0 / totalSize;
                        if(!StringUtils.isEmpty(progressName)){
                            config.setType(progressName).setVal(String.format("%.4f", progress));
                            clientService.updateConfigData(config);
                        }
                        System.out.printf("已下载: %.2f%% (%d/%d bytes)%n", progress, currentSize, totalSize);
                        lastPrintTime = System.currentTimeMillis();
                    }
                }
                // 下载完成打印100%
                System.out.printf("下载完成: 100%% (%d/%d bytes)%n", currentSize, totalSize);
                if(!StringUtils.isEmpty(progressName)){
                    config.setType(progressName).setVal(100);
                    clientService.updateConfigData(config);
                }
            } catch (Exception e) {
                throw new IOException(e);
            }finally {
                conn.disconnect();
            }
        }catch (Exception e) {
            e.printStackTrace();
            log.warning(e.getMessage());
            if(!StringUtils.isEmpty(progressName)){
                config.setType(progressName).setVal(-1);
                clientService.updateConfigData(config);
            }
        }
    }
}
