package com.ldj.controller;

import com.ldj.common.utlis.CurrentHolder;
import com.ldj.pojo.Result;
import com.ldj.service.BillImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RequestMapping("/api/files")
@RestController
@RequiredArgsConstructor
public class FileController {
    private final BillImportService billImportService;

    @PostMapping("/upload")
    public Result upload(MultipartFile file) {
        Integer userId = CurrentHolder.getCurrentUserId();
        String fileName = file.getOriginalFilename();
        String batchId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        // 暂时没有写登录接口，所以没有配置拦截器，这里的用户id先固定，后期写好了登录接口配置好拦截器通过ThreadLocal获取当前用户ID
        if (fileName.endsWith(".csv") || fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            billImportService.processImport(1, file, batchId);
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) {
            billImportService.processOcrImport(1, file, batchId);
        } else {
            return Result.error("不支持改类型");
        }

        // 4. 立即返回 batchId 给前端轮询
        return Result.success(batchId);
    }
}
