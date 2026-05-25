package com.sync.sc.controller;

import com.sync.sc.entity.Config;
import com.sync.sc.entity.Mail;
import com.sync.sc.service.ClientService;
import com.sync.sc.service.MailService;
import com.sync.sc.service.SystemService;
import com.sync.sc.util.BaseUtil;
import com.sync.sc.util.NavicatPassword;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.logging.Logger;

import static com.sync.sc.util.BaseUtil.getExtension;

@RestController
@RequestMapping(value = "/api2")
public class Client2Controller {
    private static final Logger log = Logger.getLogger(Client2Controller.class.getName());
    private final HttpClient client = HttpClient.newHttpClient();
    @Autowired
    private ClientService clientService;
    @Autowired
    private MailService mailService;
    @Autowired
    private SystemService systemService;

    /**
     * 解析Navicat密码
     * @param version Navicat版本，11版本及以前的密码，12版本及以后的密码
     * @param text 加密密码或者解密密码
     * @return
     */
    @GetMapping("/getPassword/{type}/{version}")
        public ResponseEntity lookup(@PathVariable(value = "type") Integer type,@PathVariable(value = "version") Integer version,String text) {
        try {
            String decode = null;
            if (type == 0) {
                decode = NavicatPassword.decrypt(text, version);
            }else if (type == 1) {
                decode = NavicatPassword.encrypt(text, version);
            }
            return ResponseEntity.ok(decode);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取失败:" + e.getMessage());
        }
    }
    @PostMapping("/senMail/{type}/{importType}")
    public ResponseEntity senMail(@PathVariable(value = "type") Integer type,@PathVariable(value = "importType") Integer importType,Mail mail) {
        try {
            Config config =new Config();
            config.setType(mail.getAppTypeConf()+"mailSmtpHost").setVal(mail.getSmtpHost());
            clientService.updateConfigData(config);
            config.setType(mail.getAppTypeConf()+"mailSmtpPort").setVal(mail.getSmtpPort());
            clientService.updateConfigData(config);
            config.setType(mail.getAppTypeConf()+"mailSmtpUser").setVal(mail.getSmtpUser());
            clientService.updateConfigData(config);
            config.setType(mail.getAppTypeConf()+"mailSmtpPass").setVal(mail.getSmtpPass());
            clientService.updateConfigData(config);
            config.setType(mail.getAppTypeConf()+"mailUseSsl").setVal(mail.getUseSsl());
            clientService.updateConfigData(config);
            config.setType(mail.getAppTypeConf()+"mailFromUser").setVal(mail.getFromUser());
            clientService.updateConfigData(config);
            if (!Objects.isNull(MailService.session)) {
                Map<String,Object> progress = clientService.getConfigByTypes("senExtMailProgressName");
                if(!CollectionUtils.isEmpty(progress) && progress.containsKey("senExtMailProgressName") && (!progress.get("senExtMailProgressName").toString().startsWith("true") && !progress.get("senExtMailProgressName").toString().startsWith("ERR"))) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("正在发送中...不可多次发送.");
                }
            }
            if(1==importType){
                String ext = getExtension(mail.getFile().getOriginalFilename()).toLowerCase();
                if (!ext.equals("xls") && !ext.equals("csv")) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ 不支持的文件类型: ." + ext);
                }
                List<List<String>> list= BaseUtil.readTable(mail.getFile().getInputStream(), ext);
                mail.setContent(mail.getContent().trim().replaceAll("\\$\\{time\\}", BaseUtil.formatDate(new Date(),null)));
                mailService.senExtMail(type,list,mail);
            }else {
                if(0==type){
                    mailService.sendHtml(mail);
                }else if (type == 1) {
                    mailService.sendHtmlWithInlineImageAndAttachment(mail);
                }else {
                    mailService.sendText(mail);
                }
            }
            System.out.println("邮件发送成功");
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("失败ERR:" + e.getMessage());
        }
    }

    @GetMapping("/urlCheck")
    public ResponseEntity urlCheck(@RequestParam String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            return ResponseEntity.ok(status >= 200 && status < 300?1:0);
        } catch (Exception e) {
            return ResponseEntity.ok(1);
        }
    }
}
