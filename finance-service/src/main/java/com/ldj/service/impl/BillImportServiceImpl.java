package com.ldj.service.impl;

import com.ldj.common.utlis.BillParser;
import com.ldj.common.utlis.FileStorage;
import com.ldj.mapper.RawRecordMapper;
import com.ldj.mapper.TxnMapper;
import com.ldj.pojo.RawRecord;
import com.ldj.pojo.Txn;
import com.ldj.service.BillImportService;
import com.ldj.service.ClassificationService;
import com.ldj.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillImportServiceImpl implements BillImportService {
    private final FileStorage fileStorage;
    private final List<BillParser> billParser;
    private final TxnMapper txnMapper;
    private final RawRecordMapper rawRecordMapper;
    private final ObjectMapper objectMapper;
    private final OcrService ocrService;
    private final ClassificationService classificationService;
    @Qualifier("classifyWorkerExecutor")
    private final Executor classifyWorkerExecutor;

    @Async("classifyExecutor")
    @Override
    public void processImport(Integer userId, MultipartFile file, String batchId) {

        String ext = file.getOriginalFilename();
        // 1.存储文件并返回路径
        String objectKey = fileStorage.save(userId, file);
        // 2.读取文件中的内容返回IO流
        try(InputStream inputStream = fileStorage.load(userId, objectKey))
        {
            BillParser parser = billParser.stream()
                    .filter(p -> p.support(ext))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("未找到合适的解析器"));
            log.info("开始解析文件");
            // 3.找到合适的适配器进行解析数据
            List<Txn> txns = parser.parse(inputStream);
            List<RawRecord> rawRecords = new ArrayList<>();
            // 4.存储原始数据
            for (Txn txn : txns) {
                RawRecord record = new RawRecord();
                record.setRawContent(txn.getTempRawRecord());
                String rawJson = objectMapper.writeValueAsString(txn.getTempRawRecord());
                record.setRawJson(rawJson);
                record.setUserId(userId);
                record.setBatchId(batchId);
                record.setRecordType(txn.getRecordType());
                record.setParseStatus("SUCCESS");
                record.setCreatedAt(txn.getCreatedAt());
                rawRecords.add(record);
            }
            rawRecordMapper.insert(rawRecords);

            log.info("进行分类");
            // 用异步线程去调用分类引擎
            // ★ 核心：使用 CompletableFuture 发起异步任务，主线程不阻塞！
            List<CompletableFuture<Void>> futures = txns.stream()
                    .map(txn -> CompletableFuture.runAsync(() -> {
                        try {
                            txn.setUserId(userId);
                            txn.setStatus("PENDING_REVIEW");
                            txn.setBatchId(batchId);
                            // 【重点】这里如果调用了 LlmService 去请求大模型，
                            // 线程会在这里等待 3 秒。但是！它不会卡死系统，
                            // 它只会占用线程池里的一个线程，其他线程依然在飞速处理别的账单。
                            classificationService.classify(txn);

                        } catch (Exception e) {
                            log.error("账单分类失败", e);
                        }
                    }, classifyWorkerExecutor))
                    .collect(Collectors.toList());

            // ★ 这行代码：等所有的分类任务（包括大模型的 3 秒等待）全部结束后，才继续往下走
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            txnMapper.insert(txns);
            log.info("存储数据");
        } catch (Exception e) {
            log.error("文件解析失败");
            throw new RuntimeException("导入失败",e);
        }
    }

    @Async("classifyExecutor")
    @Override
    public void processOcrImport(Integer userId, MultipartFile file, String batchId) {
        log.info("异步调用OCR识别 批次号：{}，用户：{}", batchId, userId);
        String objectKey = fileStorage.save(userId, file);
        try(InputStream inputStream = fileStorage.load(userId, objectKey))
        {
            Txn txn = ocrService.recognizeImage(inputStream);
            if(txn == null)
            {
                log.error("OCR未识别出来");
                return;
            }
            RawRecord rawRecord = new RawRecord();
            rawRecord.setUserId(userId);
            rawRecord.setBatchId(batchId);
            rawRecord.setRecordType("OCR_IMAGE");
            rawRecord.setRawContent(txn.getTempRawRecord());
            rawRecord.setRawJson(txn.getTempRecordJson());
            rawRecord.setParseStatus("SUCCESS");
            rawRecordMapper.insertOne(rawRecord);

            txn.setUserId(userId);
            txn.setBatchId(batchId);
            txn.setStatus("PENDING_REVIEW");

            classificationService.classify(txn);
            log.info("【批次 {}】图片识别并分类成功", batchId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
