package com.ldj.common.utlis.impl;

import com.ldj.common.utlis.BillParser;
import com.ldj.pojo.Txn;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelBillParseImpl implements BillParser {
    @Override
    public boolean support(String fileName) {
        if(fileName == null) return false;
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(".xlsx") || lowerCase.endsWith(".xls");
    }

    @Override
    public List<Txn> parse(InputStream inputStream) {
        List<Txn> list = new ArrayList<>();
        try(Workbook workBook = WorkbookFactory.create(inputStream)) {
            // 获取第一个表格
            Sheet sheetAt = workBook.getSheetAt(0);
            if(sheetAt == null) return list;
            int timeIndex = -1, merchantIndex = -1, amountIndex = -1, directionIndex = -1;
            boolean isHeader = true;

            // 安全的读取表格里面的数据，不管是什么类型
            DataFormatter formatter = new DataFormatter();
            // 遍历表中的行
            for (Row row : sheetAt) {
                if(isHeader)
                {
                    // 遍历行中的字段
                    for (Cell cell : row) {
                       String header = formatter.formatCellValue(cell);

                       if (header.contains("时间") || header.contains("日期")) {
                           timeIndex = cell.getColumnIndex();
                       } else if (header.contains("商户") || header.contains("交易对方") || header.contains("对方")) {
                           merchantIndex = cell.getColumnIndex();
                       } else if (header.contains("金额") || header.contains("发生额")) {
                           amountIndex = cell.getColumnIndex();
                       }else if(header.contains("收支") || header.contains("方向")){
                           directionIndex = cell.getColumnIndex();
                       }
                    }

                    if(merchantIndex == -1 || amountIndex == -1 || directionIndex == -1)
                    {
                        throw new RuntimeException("表格传输错误");
                    }
                    isHeader = false;
                    continue;
                }
                if(row == null) continue;
                List<String> tempRawRecord = new ArrayList<>();
                for (Cell cell : row) {
                    if(cell == null) continue;
                    tempRawRecord.add(formatter.formatCellValue(cell));
                }
                try{
                    Txn txn = new Txn();
                    Cell merchant = row.getCell(merchantIndex);
                    if(merchant != null)
                        txn.setCounterpartyEnc(formatter.formatCellValue(merchant).trim());
                    Cell direction = row.getCell(directionIndex);
                    if(direction != null)
                        txn.setDirection(formatter.formatCellValue(direction).trim());
                    Cell amount = row.getCell(amountIndex);
                    if(amount != null)
                    {
                        if(amount.getCellType() == CellType.NUMERIC)
                        {
                            txn.setAmount(BigDecimal.valueOf(amount.getNumericCellValue()));
                        }
                        else{
                            String money = formatter.formatCellValue(amount).trim()
                                    .replace(",", "")
                                    .replace("¥", "")
                                    .replace("￥", "")
                                    .replace(" ", "");
                            if(!money.isEmpty())
                            {
                                txn.setAmount(new BigDecimal(money));
                            }
                        }
                    }
                    if (timeIndex != -1) {
                        Cell timeCell = row.getCell(timeIndex);
                        if (timeCell != null) {
                            if (timeCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(timeCell)) {
                                // 如果用户填的是真正的 Excel 日期格式
                                txn.setTxnTime(timeCell.getLocalDateTimeCellValue());
                            } else {
                                // 如果是文本格式，尝试手动解析
                                String timeStr = formatter.formatCellValue(timeCell).trim();
                                if (!timeStr.isEmpty()) {
                                    try {
                                        txn.setTxnTime(LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                                    } catch (Exception ignored) {
                                        // 可以在这里兼容更多格式，比如 "yyyy/MM/dd"
                                    }
                                }
                            }
                        }
                    }
                    txn.setRecordType("EXCEL_ROW");
                    txn.setTempRawRecord(tempRawRecord.toString().replace("[", "").replace("]", ""));
                    list.add(txn);
                } catch (RuntimeException ignored) {

                }
            }
        } catch (Exception e) {
            throw new RuntimeException("文件解析失败：", e);
        }
        return list;
    }
}
