package com.sync.sc.common;

import com.sync.sc.entity.Config;
import com.sync.sc.entity.Update;
import com.sync.sc.service.ClientService;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.sync.sc.util.BaseUtil.DeleteDirectory;
import static com.sync.sc.util.BaseUtil.removeFile;

@Component
public class SshOpt {
    private static final Logger log = Logger.getLogger(SshOpt.class.getName());

    @Value("${file.upload.dir}")
    private String uploadDir;
    @Autowired
    private ClientService clientService;

    //判断式
    public boolean executeRemoteScript(Update sshUpdate){
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(sshUpdate.getHost(), sshUpdate.getPort());
            ssh.useCompression();
            KeyProvider keys = ssh.loadKeys(sshUpdate.getPrivateKeyPath());
            ssh.authPublickey(sshUpdate.getUser(), keys);
            try (Session session = ssh.startSession()) {
                Session.Command cmd = session.exec(sshUpdate.getCommand());
                cmd.join();
                return true;
            }finally {
                ssh.disconnect();
            }
        }catch (Exception e){
            return false;
        }
    }
    //输出式
    private String executeRemoteScriptV1(Update sshUpdate) throws IOException {
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(sshUpdate.getHost(), sshUpdate.getPort());
            ssh.useCompression();
            KeyProvider keys = ssh.loadKeys(sshUpdate.getPrivateKeyPath());
            ssh.authPublickey(sshUpdate.getUser(), keys);
            var session = ssh.startSession();
            var cmd  = session.exec(sshUpdate.getCommand());

            String hash = "";
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(cmd.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    hash= line.trim();
                }
            }
            cmd.join();
            ssh.disconnect();
            return hash;
        }
    }
    @Async
    public void uploadFile(Update sshUpdate){
        Config config =new Config();
        sshUpdate.setChunkSizeMB(Optional.ofNullable(sshUpdate.getChunkSizeMB()).orElse(5));
        sshUpdate.setThreads(Optional.ofNullable(sshUpdate.getThreads()).orElse(4));
        ExecutorService executor = Executors.newFixedThreadPool(sshUpdate.getThreads());
        try {
            // 使用Controller中已保存的本地文件
            File file = new File(sshUpdate.getFilePath());
            String fileName = sshUpdate.getFilename();
            String endCommand = sshUpdate.getCommand();
            sshUpdate.setCommand(String.format("find %s -type f -name %s* -mmin +0 -mmin +5 -exec rm -f {} \\; &&  find %s -type f -name %s*",
                    sshUpdate.getRemoteDir(), fileName, sshUpdate.getRemoteDir(), fileName));
            if(!StringUtils.isEmpty(executeRemoteScriptV1(sshUpdate))){
                throw new RuntimeException("有人正在上传或在部署此文件，请稍后...");
            }

            // 1. 从本地文件分片
            List<File> parts = splitFile(file, sshUpdate.getChunkSizeMB());

            // 2. 获取远端已有的分片 (支持断点续传)
            Set<String> remoteParts = getRemoteParts(sshUpdate);

            List<Callable<Void>> tasks = parts.stream()
                    .filter(part -> !remoteParts.contains(part.getName())) // 断点续传：只传缺的
                    .map(part -> (Callable<Void>) () -> {
                        uploadPart(sshUpdate,part);
                        return null;
                    })
                    .collect(Collectors.toList());

            // 进度条
            final int totalParts = parts.size();
            final int[] uploadedCount = {remoteParts.size()};
            for (Future<Void> f : executor.invokeAll(tasks)) {
                uploadedCount[0]++;
                double percent = 100.0 * uploadedCount[0] / totalParts;
                config.setType("updateProgress").setVal(percent);
                clientService.updateConfigData(config);
                System.out.printf("\r上传进度: %.2f%% (%d/%d)", percent, uploadedCount[0], totalParts);
                Thread.sleep(20);
                f.get();
            }
            // 3. 合并文件
            sshUpdate.setCommand(String.format("cat %s/%s.part* > %s/%s && rm %s/%s.part*",
                    sshUpdate.getRemoteDir(), sshUpdate.getFilename(), sshUpdate.getRemoteDir(), sshUpdate.getFilename(), sshUpdate.getRemoteDir(), sshUpdate.getFilename()));
            if (!executeRemoteScript(sshUpdate)){
                sshUpdate.setCommand(String.format("find %s -type f -name %s* -exec rm -f {} \\;",
                        sshUpdate.getRemoteDir(), sshUpdate.getFilename()));
                executeRemoteScript(sshUpdate);
                throw new RuntimeException("文件部署失败");
            }
            // 4. 校验完整性 (SHA-256) - 直接使用原始文件
            String localHash = sha256Hex(file);
            /** 远程 SHA-256 */
            sshUpdate.setCommand("sha256sum " + sshUpdate.getRemoteDir() + "/" + sshUpdate.getFilename() + " | awk '{print $1}'");
            String remoteHash = executeRemoteScriptV1(sshUpdate);
            sshUpdate.setCommand(endCommand);
            if (!(localHash.equals(remoteHash) && executeRemoteScript(sshUpdate))) {
                throw new RuntimeException("❌ 文件校验失败");
            }
            config.setType("isUpdateSuccess").setVal("1");
            clientService.updateConfigData(config);
            System.out.println("✅ 上传并校验成功: " + sshUpdate.getRemoteDir() + "/" + fileName);
            String TEMP_DIR = uploadDir + "/temp/"+sshUpdate.getFilename();
            if (!removeFile(TEMP_DIR)) {
                throw new IOException("无法清理临时文件");
            }
        } catch (Exception e){
            config.setType("isUpdateSuccess").setVal("0");
            clientService.updateConfigData(config);
            config.setType("updateProgress").setVal(e.getMessage());
            clientService.updateConfigData(config);
            log.warning(e.getMessage());
        }finally {
            executor.shutdown();
        }
    }

    /** 从本地文件分片 */
    private List<File> splitFile(File file, int chunkSizeMB) throws IOException {
        String CHUNK_DIR = uploadDir + "/chunk/";
        // 清理旧分片目录
        if (!DeleteDirectory(CHUNK_DIR)) {
            throw new IOException("无法清理分片目录");
        }
        // 创建目录
        File directory = new File(CHUNK_DIR);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("无法创建分片目录");
        }

        String fileName = file.getName();
        long fileSize = file.length();
        int chunkSize = chunkSizeMB * 1024 * 1024;

        List<File> parts = new java.util.ArrayList<>();
        int partCount = (int) Math.ceil((double) fileSize / chunkSize);

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[chunkSize];
            for (int i = 0; i < partCount; i++) {
                File partFile = new File(CHUNK_DIR, fileName + ".part" + String.format("%04d", i));
                parts.add(partFile);
                try (FileOutputStream fos = new FileOutputStream(partFile)) {
                    int remaining = (int) Math.min(chunkSize, fileSize - (long) i * chunkSize);
                    int totalRead = 0;
                    while (totalRead < remaining) {
                        int read = bis.read(buffer, totalRead, remaining - totalRead);
                        if (read == -1) break;
                        totalRead += read;
                    }
                    if (totalRead > 0) {
                        fos.write(buffer, 0, totalRead);
                    }
                }
            }
        }
        return parts;
    }

    /** 上传单个分片 */
    private void uploadPart(Update sshUpdate, File part) throws IOException {
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(sshUpdate.getHost(), sshUpdate.getPort());
            ssh.useCompression();
            KeyProvider keys = ssh.loadKeys(sshUpdate.getPrivateKeyPath());
            ssh.authPublickey(sshUpdate.getUser(), keys);
            try (SFTPClient sftp = ssh.newSFTPClient()) {
                sftp.put(new FileSystemFile(part), sshUpdate.getRemoteDir() + "/" + part.getName());
            }finally {
                ssh.disconnect();
            }
        }
    }

    /** 获取远端已有的分片文件名 (用于断点续传) */
    private Set<String> getRemoteParts(Update sshUpdate) throws IOException {
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(sshUpdate.getHost(), sshUpdate.getPort());
            ssh.useCompression();
            KeyProvider keys = ssh.loadKeys(sshUpdate.getPrivateKeyPath());
            ssh.authPublickey(sshUpdate.getUser(), keys);
            try (SFTPClient sftp = ssh.newSFTPClient()) {
                List<RemoteResourceInfo> files = sftp.ls(sshUpdate.getRemoteDir());
                return files.stream().map(RemoteResourceInfo::getName).filter(name->name.contains(sshUpdate.getFilename()+".part")).collect(Collectors.toSet());
            }finally {
                ssh.disconnect();
            }
        }
    }

    /** 本地 SHA-256 */
    private String sha256Hex(File file) throws Exception {
        try (InputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            return HexFormat.of().formatHex(digest.digest());
        }
    }
}
