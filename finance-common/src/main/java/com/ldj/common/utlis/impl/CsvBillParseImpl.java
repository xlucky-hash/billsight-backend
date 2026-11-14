package com.ldj.common.utlis.impl;

import com.ldj.common.utlis.BillParser;
import com.ldj.pojo.Txn;
import com.opencsv.CSVReader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class CsvBillParseImpl implements BillParser {
    @Override
    public boolean support(String fileName) {
        return fileName.toLowerCase().endsWith(".csv");
    }

    @Override
    public List<Txn> parse(InputStream inputStream) {
        List<Txn> txns = new ArrayList<>();
        try(CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
        {
            int timeIndex = -1, merchantIndex = -1, amountIndex = -1, directionIndex = -1;
            String[] lines;
            boolean isHeader = true;
            while((lines = reader.readNext()) != null)
            {
                if(isHeader)
                {
                    for (int i = 0; i < lines.length; i++) {
                        String header = lines[i].trim();
                        if (header.contains("时间") || header.contains("日期")) {
                            timeIndex = i;
                        } else if (header.contains("商户") || header.contains("交易对方") || header.contains("对方")) {
                            merchantIndex = i;
                        } else if (header.contains("金额") || header.contains("发生额")) {
                            amountIndex = i;
                        }else if(header.contains("收支") || header.contains("方向")){
                            directionIndex = i;
                        }
                    }

                    if(merchantIndex == -1 || amountIndex == -1 || directionIndex == -1)
                    {
                        throw new RuntimeException("查找不到表格数据");
                    }

                    isHeader = false;
                    continue;
                }
                String tempRawRecord = Arrays.toString(lines).replace("[", "").replace("]", "");
                if(lines.length < Math.max(merchantIndex, amountIndex))
                {
                    continue;
                }

                Txn txn = new Txn();
                txn.setRecordType("CSV_ROW");
                txn.setCounterpartyEnc(lines[merchantIndex]);
                txn.setAmount(new BigDecimal(lines[amountIndex]));
                txn.setDirection(lines[directionIndex]);
                txn.setTempRawRecord(tempRawRecord);
                if(timeIndex != -1) txn.setTxnTime(LocalDateTime.parse(lines[timeIndex].trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                txns.add(txn);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return txns;
    }
}
