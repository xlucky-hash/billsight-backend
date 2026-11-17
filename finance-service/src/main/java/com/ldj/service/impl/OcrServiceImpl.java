package com.ldj.service.impl;

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.ocr_api20210707.models.RecognizePaymentRecordRequest;
import com.aliyun.ocr_api20210707.models.RecognizePaymentRecordResponse;
import com.ldj.pojo.Txn;
import com.ldj.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final Client client;
    private final ObjectMapper objectMapper;

    @Override
    public Txn recognizeImage(InputStream inputStream) {
        try (inputStream) {
            byte[] imageBytes = inputStream.readAllBytes();
            RecognizePaymentRecordRequest request =
                    new RecognizePaymentRecordRequest().setBody(new ByteArrayInputStream(imageBytes));
            RecognizePaymentRecordResponse response = client.recognizePaymentRecord(request);
            String jsonData = response.getBody().getData();
            return parseJsonToTxn(jsonData);
        } catch (Exception e) {
            throw new RuntimeException("OCR 识别失败", e); // 自定义业务异常
        }
    }

    @Override
    public Txn parseJsonToTxn(String jsonData) {
        if (jsonData == null || jsonData.isBlank()) return null;

        try {
            // 1. 解析整个 JSON
            JsonNode root = objectMapper.readTree(jsonData);

            // 2. 拿到最核心的 data 节点
            JsonNode dataNode = root.path("data");
            if (dataNode.isMissingNode() || dataNode.isEmpty()) {
                log.warn("OCR 识别结果中没有 data 字段");
                return null;
            }

            // 3. 组装 Txn 对象
            Txn txn = new Txn();
            txn.setTempRecordJson(jsonData);
            // 提取收款方 (recipientName)
            if (dataNode.hasNonNull("recipientName")) {
                txn.setCounterpartyEnc(dataNode.get("recipientName").asText());
            }

            // 提取金额 (totalAmount) 注意：有时会带负号或千分位
            if (dataNode.hasNonNull("totalAmount")) {
                String amountStr = dataNode.get("totalAmount").asText().replace(",", "");
                        // 去千分位
                if (amountStr.startsWith("-")) {
                    // 负数：支出
                    txn.setDirection("EXPENSE");
                    amountStr = amountStr.replace("-", ""); // 去掉负号，只保留纯数字
                } else if (amountStr.startsWith("+")) {
                    // 正数且带加号：收入
                    txn.setDirection("INCOME");
                    amountStr = amountStr.replace("+", "");
                } else {
                    // 没有符号：默认为支出（绝大多数账单截图默认是扣款）
                    txn.setDirection("EXPENSE");
                }
                amountStr = amountStr.replace("¥", "").replace("$", "").trim();
                if (!amountStr.isEmpty()) {
                    txn.setAmount(new BigDecimal(amountStr));
                }
            }

            // 提取商品说明 (description) 作为摘要
            if (dataNode.hasNonNull("description")) {
                txn.setSummary(dataNode.get("description").asText());
            } else {
                txn.setSummary("");
            }

            // 提取支付时间 (paymentTime)
            if (dataNode.hasNonNull("paymentTime")) {
                txn.setTxnTime(parseTime(dataNode.get("paymentTime").asText()));
            }
            return txn;

        } catch (Exception e) {
            log.error("解析 OCR JSON 失败", e);
            return null;
        }
    }

    @Override
    public LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;

        // ★ 增加了 "yyyy-MM-dd HH:mm" 无秒格式
        List<String> patterns = List.of(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy年MM月dd日 HH:mm:ss"
        );

        for (String pattern : patterns) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored)
            {

            }
        }
        log.warn("时间格式无法解析: {}", value);
        return null;
    }
}
