package com.ldj.service;

import com.ldj.common.utlis.FileStorage;
import com.ldj.pojo.FileResult;
import org.springframework.web.multipart.MultipartFile;

public interface BillImportService {
    void processImport(Integer userId, MultipartFile file, String batchId);


    void processOcrImport(Integer userId, MultipartFile file, String batchId);
}
