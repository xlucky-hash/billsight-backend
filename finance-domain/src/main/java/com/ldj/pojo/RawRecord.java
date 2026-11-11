package com.ldj.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 原始记录实体类 (对应 raw_record 表)
 * 用于记录未经处理的原始行、原始文本、OCR结果，保证数据可溯源
 */
@Data
public class RawRecord {

    /** 主键 ID */
    private Integer id;

    /** 用户 ID */
    private Integer userId;

    /** 导入批次 ID */
    private String batchId;

    /** 
     * 记录类型: OCR_TEXT / CSV_ROW / API_JSON 
     */
    private String recordType;

    /** 
     * 原始文本或原始行内容 
     */
    private String rawContent;

    /** 
     * 结构化原始数据 (通常存 JSON 字符串) 
     * 对应数据库的 JSON 类型字段
     */
    private String rawJson;

    /** 
     * 解析状态: SUCCESS / FAILED 
     */
    private String parseStatus;

    /** 创建时间 */
    private LocalDateTime createdAt;
}