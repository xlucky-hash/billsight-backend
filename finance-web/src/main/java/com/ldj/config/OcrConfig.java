package com.ldj.config;

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcrConfig {

    @Value("${aliyun.ocr.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.ocr.access-key-secret}")
    private String accessKeySecret;

    @Bean
    public Client ocrClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint("ocr-api.cn-hangzhou.aliyuncs.com");
        return new Client(config);
    }
}