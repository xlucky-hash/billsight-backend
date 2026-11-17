package com.ldj.common.utlis.impl;

import com.ldj.common.utlis.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileImplStorage implements FileStorage {

    @Value(value = "${app.storage.local.base:/tmp/bills}")
    private String basePath;

    @Override
    public String save(Integer userId, MultipartFile file) {
        try {
            String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String objectKey = userId + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "/" + UUID.randomUUID() + userId + ext;
            Path targetPath = Paths.get(basePath, objectKey).normalize();
            if(!targetPath.startsWith(basePath))throw new RuntimeException("非法路径");
            Files.createDirectories(targetPath.getParent());
//            file.transferTo(targetPath.toFile());

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("文件保存失败", e);
        }
    }

    @Override
    public InputStream load(Integer userId, String key) {
        try{
            Path rootPath = Paths.get(basePath).toAbsolutePath().normalize();
            Path targetPath = rootPath.resolve(key).normalize();
            if(!key.startsWith(userId + "/"))
                throw new SecurityException("越权访问");
            if(!targetPath.startsWith(rootPath))
                throw new SecurityException("越权访问");
            return Files.newInputStream(targetPath);
        } catch (Exception e) {
            throw new RuntimeException("资源访问失败", e);
        }
    }
}
