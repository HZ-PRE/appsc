package com.sync.sc.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sync.sc.controller.ClientController;
import com.sync.sc.entity.Update;
import org.springframework.util.StringUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class BaseUtil {
    private static final Logger log = Logger.getLogger(ClientController.class.getName());
    public static int getRandom(int num) {
        if (num <= 0) {
            num = 1;
        }
        Random r = new Random();
        return r.nextInt(num);
    }
    /**
     * long类型转时间格式
     */
    public static String formatDate(Date date, String pattern){
        pattern= StringUtils.isEmpty(pattern)?"yyyy-MM-dd HH:mm:ss":pattern;
        return new SimpleDateFormat(pattern).format(date);
    }
    // 工具方法：提取扩展名
    public static String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }
    //删除指定文件
    public static boolean removeFile(String filePath) {
        Path path = Paths.get(filePath);
        try {
            if (Files.exists(path)) {
                Files.delete(path);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    //移动文件到指定位置
    public static void copyFile(String filePath, String targetPath) throws IOException {
        Path src = Paths.get(filePath);
        Path dest = Paths.get(targetPath);

        Files.createDirectories(dest.getParent()); // 确保目录存在
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }
    //删除目录及其所有子文件
    public static boolean DeleteDirectory(String filePath)  {
        try {
            Path dir = Paths.get(filePath);
            if (Files.exists(dir)) {
                Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                System.out.println("目录及文件已删除: " + dir.toAbsolutePath());
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    // 获取github文件 SHA，如果不存在返回 null
    public static String getGithubFileSha(Update ossUpdate) throws Exception {
        String url = "https://api.github.com/repos/" + ossUpdate.getAccessKeyId() + "/" + ossUpdate.getEndpoint() + "/contents/" + ossUpdate.getFilename() + "?ref=" + ossUpdate.getBucketName();
        ObjectMapper objectMapper = new ObjectMapper();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "token " + ossUpdate.getAccessKeySecret());
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        try {
            int statusCode = conn.getResponseCode();
            if (statusCode == 200) {
                try (InputStream is = conn.getInputStream()) {
                    JsonNode node = objectMapper.readTree(is);
                    return node.get("sha").asText();
                }
            } else if (statusCode == 404) {
                return null; // 文件不存在
            } else {
                throw new RuntimeException("获取文件 SHA 失败，状态码: " + statusCode);
            }
        } finally {
            conn.disconnect();
        }
    }
    public static boolean SimpleHttpsGet(String url){
        try {
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestProperty("User-Agent","Mozilla/5.0");
            con.setRequestProperty("Accept", "*/*");
            int responseCode = con.getResponseCode();
            con.disconnect();
            if (responseCode == 200) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.warning(e.getMessage());
            return false;
        }
        return false;
    }
    public static String readRemoteFile(String filePath) {
        try{
            URL url = new URL(filePath);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line="";
                int count=0;
                for(String str : reader.readLine().split("\n")){
                    if(StringUtils.isEmpty(str)){
                        if(count == 3){
                            break;
                        }
                        count ++;
                    }else {
                        line += str+"\n";
                    }
                }
                return line.trim();
            }
        }catch (Exception e){
            e.printStackTrace();
            log.warning(e.getMessage());
            return "";
        }
    }
    public static List<List<String>> readTable(InputStream in, String extension) throws IOException {
        return LightweightTableReader.read(in, extension);
    }

    public static List<List<String>> readExcel(InputStream in) throws IOException {
        return LightweightTableReader.read(in, "xls");
    }
}
