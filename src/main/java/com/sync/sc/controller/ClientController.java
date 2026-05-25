package com.sync.sc.controller;

import com.sync.sc.common.OssOpt;
import com.sync.sc.common.SshOpt;
import com.sync.sc.entity.Config;
import com.sync.sc.entity.Update;
import com.sync.sc.service.ClientService;
import com.sync.sc.service.SystemService;
import com.sync.sc.util.Ip2regionUtil;
import com.sync.sc.util.SimpleMultipartFile;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

import static com.sync.sc.util.BaseUtil.*;

@RestController
@RequestMapping(value = "/api")
public class ClientController {
    private static final Logger log = Logger.getLogger(ClientController.class.getName());

    // 允许的扩展名
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("apk", "exe", "zip");

    @Value("${hc.path}")
    private String hcPath;
    @Value("${file.upload.dir}")
    private String uploadDir;
    @Value("${app.version}")
    private String appVersion;
    @Autowired
    private ClientService clientService;
    @Resource
    private SshOpt sshOpt;
    @Autowired
    private OssOpt ossOpt;
    @Autowired
    private SystemService systemService;
    @GetMapping("/ip/lookup/{ip}")
    public ResponseEntity lookup(@PathVariable(value = "ip") String ip) {
        log.info("ip:" + ip);
        Map<String,String> results =Ip2regionUtil.search(ip);
        if(CollectionUtils.isEmpty(results)){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取失败");
        }
        results.put("status","success");
        return ResponseEntity.ok(results);
    }

    @PostMapping("/appUpdate/{type}")
    public ResponseEntity<Object> appUpdate(@PathVariable(value = "type") String type,@RequestBody String url) {
        try {
            if("0".equals(type)){
                Map<String,Object> update = clientService.getConfigByTypes("appUpdateProgress");
                if(CollectionUtils.isEmpty(update)){
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("false");
                } else if ("-1".equals(update.get("appUpdateProgress"))) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新失败");
                } else if ("100".equals(update.get("appUpdateProgress"))) {
                    new ProcessBuilder(
                            "powershell",
                            "-WindowStyle",
                            "Hidden",
                            "-Command",
                            "Start-Process cmd -ArgumentList '/c update.bat' -WindowStyle Hidden"
                    ).directory(new File(System.getProperty("user.dir")))
                            .start();

                    System.exit(0);
                    return ResponseEntity.ok("100%，安装成功，正在重新启动...");
                }
                return ResponseEntity.ok(update.get("appUpdateProgress"));
            }else {
                if(removeFile(System.getProperty("user.dir")+"/update.zip")){
                    systemService.downloadFile(url,System.getProperty("user.dir")+"/update.zip","appUpdateProgress");
                    return ResponseEntity.ok("安装包下载成功，正在安装中...");
                }else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("下载失败");
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("安装失败: " + e.getMessage());
        }
    }
    @PostMapping("/ip/getIpDb/{type}")
    public ResponseEntity updateIpDb(@PathVariable(value = "type") String type) {
        if("0".equals(type)){
            Map<String,Object> update = clientService.getConfigByTypes("fileUpProgress");
            if(CollectionUtils.isEmpty(update)){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("false");
            } else if ("-1".equals(update.get("fileUpProgress"))) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新失败");
            } else if ("100".equals(update.get("fileUpProgress"))) {
                try {
                    Config config=new Config();
                    copyFile(hcPath+"/sys/ip2region.xdb",hcPath+"/data/ip2region.xdb");
                    Thread.sleep(500);
                    removeFile(hcPath+"/sys/ip2region.xdb");
                    config.setType("ip_db_update_time").setVal(System.currentTimeMillis() / 1000);
                    clientService.updateConfigData(config);
                }catch (Exception e){
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
                }
            }
            return ResponseEntity.ok(update.get("fileUpProgress"));
        }else{
            Map<String,Object> configs =  clientService.getConfigByTypes("ip_db_update_time");
            String dbTime = readRemoteFile("https://down.bzyvpn.net/public/static/ip2region_uptime.txt");
            if(StringUtils.isEmpty(dbTime)){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新失败");
            }
            long ipT1 = Long.parseLong(dbTime);
            if(configs.containsKey("ip_db_update_time")){
                long ipT =  Long.parseLong((String) configs.get("ip_db_update_time"));
                if(ipT1 <= ipT){
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("已经是最新版本");
                }
            }
            systemService.downloadFile("https://download.bzyvpn.net/public/static/ip2region.xdb",hcPath+"/sys/ip2region.xdb","fileUpProgress");
            return ResponseEntity.ok("正在加载中");
        }
    }
    @GetMapping("/updateProgress")
    public ResponseEntity<Object> updateProgress() {
        try {
            Map<String,Object> update = clientService.getConfigByTypes("isUpdateSuccess,updateProgress");
            if(CollectionUtils.isEmpty(update)){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("false");
            }
            if ("1".equals(update.get("isUpdateSuccess"))){
                return ResponseEntity.ok("true");
            }else if("0".equals(update.get("isUpdateSuccess"))){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(update.get("updateProgress"));
            }else {
                return ResponseEntity.ok(update.get("updateProgress"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity upload(Update update) {
        if (update.getAppReleaseType().isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ 发布类型为空");
        }
        if (update.getAppType().isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ 应用类型为空");
        }
        if (update.getAppfile().isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ 文件为空");
        }
        // 检查文件扩展名
        String filename = update.getAppfile().getOriginalFilename();
        if (filename == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ 文件名无效");
        }
        String ext = getExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ 不支持的文件类型: ." + ext);
        }
        try {
            update.setFilename(update.getAppfile().getOriginalFilename());

            // 在异步调用前，先将MultipartFile保存到本地文件（避免临时文件被清理）
            File tempDir = new File(uploadDir + "/temp/");
            if (!tempDir.exists()) tempDir.mkdirs();
            File savedFile = new File(tempDir, update.getFilename());
            update.getAppfile().transferTo(savedFile);
            update.setFilePath(savedFile.getAbsolutePath());

            Config config =new Config();
            config.setType("isUpdateSuccess").setVal("-1");
            clientService.updateConfigData(config);
            config.setType("updateProgress").setVal(0);
            clientService.updateConfigData(config);
            config.setType(update.getAppTypeConf()+"updateAppHost").setVal(update.getHost());
            clientService.updateConfigData(config);
            config.setType(update.getAppTypeConf()+"updateAppUser").setVal(update.getUser());
            clientService.updateConfigData(config);
            config.setType(update.getAppTypeConf()+"updateAppPort").setVal(String.valueOf(update.getPort()));
            clientService.updateConfigData(config);
            config.setType(update.getAppTypeConf()+"updateAppPrivateKeyPath").setVal(String.valueOf(update.getPrivateKeyPath()));
            clientService.updateConfigData(config);
            config.setType(update.getAppTypeConf()+"updateAppRemoteDir").setVal(String.valueOf(update.getRemoteDir()));
            clientService.updateConfigData(config);
            config.setType(update.getAppTypeConf()+"updateAppCommand").setVal(String.valueOf(update.getCommand()));
            clientService.updateConfigData(config);
            // 方法1：通过InetAddress获取完整计算机名
            String computerName = InetAddress.getLocalHost().getHostName();
            String userName = clientService.getConfigByTypes("appAccessKey").get("appAccessKey").toString();
            update.setCommand(update.getCommand() + " " + update.getFilename() + " " + update.getAppType() + " " + computerName+"-"+userName + " " + update.getAppReleaseType());
            sshOpt.uploadFile(update);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            e.printStackTrace();
            log.warning(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @PostMapping("/updateRollback")
    public ResponseEntity updateRollback(Update update) {
        try {
            Config config =new Config();
            config.setType(update.getAppTypeConf()+"rollbackAppHost").setVal(update.getHost());
            clientService.updateConfigData(config);
            config.setType(update.getAppTypeConf()+"rollbackAppUser").setVal(update.getUser());
            clientService.updateConfigData(config);
            config.setType(update.getAppTypeConf()+"rollbackAppPort").setVal(String.valueOf(update.getPort()));
            clientService.updateConfigData(config);
            config.setType(update.getAppTypeConf()+"rollbackAppPrivateKeyPath").setVal(String.valueOf(update.getPrivateKeyPath()));
            clientService.updateConfigData(config);
            config.setType(update.getAppTypeConf()+"rollbackAppCommand").setVal(String.valueOf(update.getCommand()));
            clientService.updateConfigData(config);
            // 方法1：通过InetAddress获取完整计算机名
            String computerName = InetAddress.getLocalHost().getHostName();
            String userName = clientService.getConfigByTypes("appAccessKey").get("appAccessKey").toString();
            update.setCommand(update.getCommand() + " " + update.getRemoteDir() + " " + update.getFilename() + " " + update.getAppType() + " " + computerName+"-"+userName + " " + update.getAppReleaseType());
            sshOpt.executeRemoteScript(update);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            e.printStackTrace();
            log.warning(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @PostMapping("/ossUpload")
    public ResponseEntity ossUpload(Update update) {
        if (update.getBucketName().isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ 桶为空");
        }
        if (update.getAppfile().isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ 文件为空");
        }
        if (update.getAccessKeyId().isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ keyId为空");
        }
        if (update.getAccessKeySecret().isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ secret为空");
        }
        if (update.getEndpoint().isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ endpoint为空");
        }
        if(update.getAppType().isEmpty()){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ 资源类型为空");
        }
        if(update.getAppType().contains("dashOss") && update.getPrivateKeyPath().isEmpty()){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ 云链接为空");
        }
        try {
            String appType = update.getAppType()+update.getAppTypeConf();
            Config config =new Config();
            config.setType(appType + "PrivateKeyPath").setVal(update.getPrivateKeyPath());
            clientService.updateConfigData(config);
            config.setType(appType + "BucketName").setVal(update.getBucketName());
            clientService.updateConfigData(config);
            config.setType(appType + "Endpoint").setVal(update.getEndpoint());
            clientService.updateConfigData(config);
            config.setType(appType + "AccessKeyId").setVal(update.getAccessKeyId());
            clientService.updateConfigData(config);
            config.setType(appType + "AccessKeySecret").setVal(update.getAccessKeySecret());
            clientService.updateConfigData(config);
            config.setType(appType + "RemoteDir").setVal(update.getRemoteDir());
            clientService.updateConfigData(config);
            update.setFilename(update.getAppfile().getOriginalFilename());

            String computerName = InetAddress.getLocalHost().getHostName();
            String userName = clientService.getConfigByTypes("appAccessKey").get("appAccessKey").toString();
            String url="https://down.bzyvpn.net/appsclog/"+userName+"?h="+computerName+"&p=";
            if(SimpleHttpsGet(url+update.getRemoteDir()+"-"+update.getFilename())){
                String ret = null;
                if(update.getAppType().contains("dashOss")){
                    ret=ossOpt.cloudflareUploadFile(update);
                }else if(update.getAppType().contains("aliOss")){
                    ret=ossOpt.aliOssUploadFile(update);
                }else if(update.getAppType().contains("awsOss")){
                    ret=ossOpt.awsOssUploadFile(update);
                }else if(update.getAppType().contains("githubOss")){
                    ret=ossOpt.githubOssUploadFile(update);
                }
                if(ret!=null){
                    if(ret.startsWith("http")){
                        SimpleHttpsGet(url+ret);
                    }
                    return ResponseEntity.ok(ret);
                }
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("没有找到云资源");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("没有上传云资源的权限");
        } catch (Exception e) {
            e.printStackTrace();
            log.warning(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    @PostMapping("/ossUploadV1")
    public ResponseEntity<String> ossUploadV1(@RequestBody Map<String, String> m) {
        String url = m.get("url");
        String content = m.get("content");
        String fileName = m.get("fileName");
        String fileType = m.get("fileType");
        if (url == null || url.trim().isEmpty() || content == null || content.trim().isEmpty() || fileName == null || fileName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("url，content，fileName不能为空");
        }

        try {
            String flag=null;
            String ret= "";
            Update update =new Update();
            update.setAppfile(new SimpleMultipartFile(content.getBytes(),fileName,fileType));
            update.setFilename(fileName);
            Map<String,Map<String, String>> ms=clientService.getConfigByOss();
            for (Map.Entry<String, Map<String, String>> entry : ms.entrySet()) {
                Map<String, String> map = entry.getValue();
                update.setBucketName(map.get("BucketName"));
                update.setEndpoint(map.get("Endpoint"));
                update.setAccessKeyId(map.get("AccessKeyId"));
                update.setAccessKeySecret(map.get("AccessKeySecret"));
                update.setPrivateKeyPath(map.get("PrivateKeyPath"));
                if(entry.getKey().contains("dashOss")){
                    if(url.contains(update.getPrivateKeyPath())){
                        url = url.replaceAll(update.getPrivateKeyPath(),"");
                        ret=ret + ossOpt.cloudflareUploadFile(update)+"<br/>";
                    }
                }else if(entry.getKey().contains("aliOss")){
                    flag="https://" + update.getBucketName() + "." + update.getEndpoint();
                    if(url.contains(flag)){
                        url = url.replaceAll(flag,"");
                        ret=ret + ossOpt.aliOssUploadFile(update)+"<br/>";
                    }
                }else if(entry.getKey().contains("awsOss")){
                    flag="https://" + update.getBucketName() + ".s3."+update.getEndpoint()+".amazonaws.com";
                    if(url.contains(flag)){
                        url = url.replaceAll(flag,"");
                        ret=ret + ossOpt.awsOssUploadFile(update)+"<br/>";
                    }
                }else if(entry.getKey().contains("githubOss")){
                    flag="https://raw.githubusercontent.com/" + update.getAccessKeyId() + "/" + update.getEndpoint() + "/"+update.getBucketName();
                    if(url.contains(flag)){
                        url = url.replaceAll(flag,"");
                        ret=ret + ossOpt.githubOssUploadFile(update)+"<br/>";
                    }
                }
            }
            return ResponseEntity.ok(ret);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping("/getAppVersion")
    public ResponseEntity<Integer> getAppVersion() {
        try {
            return ResponseEntity.ok(Integer.parseInt(appVersion.replaceAll("\\.","")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping("/getConfig")
    public ResponseEntity<Map<String,Object>> getConfigByUpdateApp(@RequestParam(value = "types") String types) {
        try {
            return ResponseEntity.ok(clientService.getConfigByTypes(types));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @PostMapping("/saveConfig")
    public ResponseEntity<String> saveConfig(@RequestBody Config config) {
        try {
            clientService.updateConfigData(config);
            return ResponseEntity.ok("数据更新成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("更新失败: " + e.getMessage());
        }
    }
    @PostMapping("/saveFileAppConf")
    public ResponseEntity saveFileAppConf(Update update) {
        if (StringUtils.isEmpty(update.getAppfile())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ 配置文件为空");
        }
        String filename = update.getAppfile().getOriginalFilename();
        if (!filename.endsWith(".conf")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("配置文件有误！");
        }
        try {
            File savedFile = new File(hcPath, filename);
            update.getAppfile().transferTo(savedFile);
            copyFile(hcPath+"/"+filename,hcPath+"/_sc_pm.db");
            removeFile(hcPath+"/"+filename);
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            e.printStackTrace();
            log.warning(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}