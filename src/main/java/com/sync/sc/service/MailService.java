package com.sync.sc.service;

import com.sync.sc.entity.Config;
import com.sync.sc.entity.Mail;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;

@Service
public class MailService {

    public static Session session;
    private static String newMailHost;
    @Autowired
    private ClientService clientService;

    /**
     * @param host SMTP 主机（例如 smtp.example.com）
     * @param port SMTP 端口（587 = STARTTLS, 465 = SSL）
     * @param username 登录用户名（发信邮箱）
     * @param password 登录密码或应用专用密码
     * @param useSsl 是否使用 SSL（true -> 465）
     */
    public synchronized  void  init(Mail mail) {
        if (session != null && mail.getSmtpHost().equals(newMailHost)) {
            return;
        }
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        if (mail.getUseSsl()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", String.valueOf(mail.getSmtpPort()));
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        props.put("mail.smtp.host", mail.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(mail.getSmtpPort()));
        props.put("java.net.preferIPv4Stack", "true");
        // 增加连接和读超时
        props.put("mail.smtp.connectiontimeout", "30000"); // 30秒
        props.put("mail.smtp.timeout", "30000");           // 30秒
        props.put("mail.smtp.writetimeout", "30000");      // 写超时
        // 强制 JavaMail 使用自定义 Message-ID 生成策略
        props.put("mail.mime.address.strict", "false");

        session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mail.getFromUser(), mail.getSmtpPass());
            }
        });

        // 可开启调试：
//        session.setDebug(true);
        newMailHost=mail.getSmtpHost();
    }
    /**
     * 发送 文本
     */
    public void sendText(Mail mail) {
        Config config =new Config();
        try {
            config.setType("senExtMailProgressName").setVal("无");
            clientService.updateConfigData(config);
            sendTextMessage(mail);
            config.setType("senExtMailProgressName").setVal("true");
            clientService.updateConfigData(config);
        }catch (Exception e){
            config.setType("senExtMailProgressName").setVal("ERR:"+e.getMessage());
            clientService.updateConfigData(config);
            throw new RuntimeException(e.getMessage());
        }
    }
    /**
     * 发送 HTML 邮件（简单版，无附件）
     */
    public void sendHtml(Mail mail){
        Config config =new Config();
        try {
            config.setType("senExtMailProgressName").setVal("无");
            clientService.updateConfigData(config);
            sendHtmlMessage(mail);
            config.setType("senExtMailProgressName").setVal("true");
            clientService.updateConfigData(config);
        }catch (Exception e){
            config.setType("senExtMailProgressName").setVal("ERR:"+e.getMessage());
            clientService.updateConfigData(config);
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 发送 HTML 带内嵌图片（CID）和附件的示例方法（可按需扩展）
     */
    @Async
    public void sendHtmlWithInlineImageAndAttachment(Mail mail){
        Config config =new Config();
        try {
            config.setType("senExtMailProgressName").setVal("无");
            clientService.updateConfigData(config);
            sendHtmlWithAttachmentMessage(mail);
            config.setType("senExtMailProgressName").setVal("true");
            clientService.updateConfigData(config);
        }catch (Exception e){
            config.setType("senExtMailProgressName").setVal(e.getMessage());
            clientService.updateConfigData(config);
            throw new RuntimeException(e.getMessage());
        }
    }
    @Async
    public void senExtMail(Integer type,List<List<String>> list, Mail mail){
        Config config =new Config();
        config.setType("senExtMailProgressName").setVal("无");
        clientService.updateConfigData(config);
        String content=mail.getContent();
        int nextIndex = 0;
        String lastSuccess = "无";
        for (int retry = 0; retry <= 5; retry++) {
            try {
                this.init(mail);
                for (int y = nextIndex; y < list.size(); y++) {
                    List<String> list2 = list.get(y);
                    if (list2.isEmpty()) {
                        break;
                    }
                    String to = valueAt(list2, 0);
                    if (!to.contains("@")) {
                        nextIndex = y + 1;
                        continue;
                    }
                    mail.setTo(to);
                    mail.setContent(applyMailTemplate(content, list2));
                    Thread.sleep(18000);
                    sendMessage(type, mail);
                    lastSuccess = to;
                    nextIndex = y + 1;
                    config.setType("senExtMailProgressName").setVal(to);
                    clientService.updateConfigData(config);
                }
                config.setType("senExtMailProgressName").setVal("true");
                clientService.updateConfigData(config);
                return;
            } catch (Exception e) {
                String err = "ERR:最后发送成功的账号" + lastSuccess + "。" + e.getMessage();
                config.setType("senExtMailProgressName").setVal(err + (retry < 5 ? ",正在尝试再次发送" : ",通信已断，无法发送"));
                clientService.updateConfigData(config);
                MailService.session = null;
            }
        }
    }

    private void sendMessage(Integer type, Mail mail) throws Exception {
        if (0 == type) {
            sendHtmlMessage(mail);
        } else if (type == 1) {
            sendHtmlWithAttachmentMessage(mail);
        } else {
            sendTextMessage(mail);
        }
    }

    private void sendTextMessage(Mail mail) throws Exception {
        this.init(mail);
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(mail.getFromUser(), mail.getFromUserName()));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.getTo()));
        msg.setSubject(mail.getSubject());
        msg.setText(mail.getContent());
        Transport.send(msg);
    }

    private void sendHtmlMessage(Mail mail) throws Exception {
        this.init(mail);
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(mail.getFromUser(), mail.getFromUserName()));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.getTo(), false));
        msg.setSubject(mail.getSubject(), "UTF-8");
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(mail.getContent(), "text/html; charset=utf-8");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(htmlPart);
        msg.setContent(multipart);
        msg.setSentDate(new java.util.Date());
        Transport.send(msg);
    }

    private void sendHtmlWithAttachmentMessage(Mail mail) throws Exception {
        this.init(mail);
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(mail.getFromUser(), mail.getFromUserName()));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.getTo(), false));
        msg.setSubject(mail.getSubject(), "UTF-8");
        msg.setSentDate(new java.util.Date());

        MimeBodyPart htmlBody = new MimeBodyPart();
        htmlBody.setContent(mail.getContent(), "text/html; charset=utf-8");

        MimeBodyPart imagePart = new MimeBodyPart();
        imagePart.attachFile(mail.getInlineImage());
        imagePart.setHeader("Content-ID", "<inlineImg>");
        imagePart.setDisposition(MimeBodyPart.INLINE);

        MimeMultipart related = new MimeMultipart("related");
        related.addBodyPart(htmlBody);
        related.addBodyPart(imagePart);

        MimeBodyPart relatedBodyPart = new MimeBodyPart();
        relatedBodyPart.setContent(related);

        MimeBodyPart filePart = new MimeBodyPart();
        filePart.attachFile(mail.getAttachment());

        MimeMultipart mixed = new MimeMultipart("mixed");
        mixed.addBodyPart(relatedBodyPart);
        mixed.addBodyPart(filePart);

        msg.setContent(mixed);
        Transport.send(msg);
    }

    private String applyMailTemplate(String content, List<String> row) {
        String result = content == null ? "" : content.trim();
        result = result.replace("${to}", valueAt(row, 0));
        for (int i = 1; i < row.size(); i++) {
            String value = valueAt(row, i);
            if (value.isEmpty()) {
                break;
            }
            result = result.replaceAll("\\$\\{e" + i + "\\}", Matcher.quoteReplacement(value));
        }
        return result;
    }

    private String valueAt(List<String> row, int index) {
        if (row == null || index >= row.size() || row.get(index) == null) {
            return "";
        }
        return row.get(index).trim();
    }
}
