package com.ldj.service;

import com.ldj.pojo.Txn;

import java.io.InputStream;
import java.time.LocalDateTime;

public interface OcrService {
    public Txn recognizeImage(InputStream inputStream);
    public Txn parseJsonToTxn(String txnJson);
    public LocalDateTime parseTime(String value);
}
