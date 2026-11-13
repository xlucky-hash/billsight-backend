package com.ldj.common.utlis;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorage {
    public String save(Integer userId, MultipartFile file);

    public InputStream load(Integer userId, String key);
}
