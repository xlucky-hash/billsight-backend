package com.ldj.pojo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易账单实体类
 */
@Data
public class Txn {

    // ================== 核心业务字段 ==================

    private Integer id;

    /** 用户ID (多租户隔离) */
    private Integer userId;

    /** 关联账户ID (比如是关联的哪张银行卡/支付宝) */
    private Integer accountId;

    /** 交易时间 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime txnTime;

    /** 交易金额 (金额明文存储，用于报表聚合计算) */
    private BigDecimal amount;

    /** 收支方向 (INCOME / EXPENSE) */
    private String direction;

    private String batchId;

    /**
     * ★ 交易对方（商户名）
     * 数据库真实字段名是 counterparty_enc，这里存的是加密后的字符串。
     * 如果你用了 JPA 的 AttributeConverter 或 MyBatis 的 TypeHandler，
     * 在查询出来赋值给这个字段时，应该自动解密。
     * 为了保证匹配逻辑能跑通，最好再提供一个只读的明文 getter。
     */
    private String counterpartyEnc;

    /**
     * 为了配合你之前的分类代码，加一个 merchant 的 get 方法。
     * 分类引擎里调用的 txn.getMerchant() 实际就是返回明文的交易对方。
     */
    public String getMerchant() {
        // 如果使用了加密框架，这里实际上已经是解密后的明文了
        return this.counterpartyEnc;
    }

    /** 摘要/备注 (如：午餐、打车) */
    private String summary;

    /** 原始行号 (防止导入CSV时丢失行数追踪) */
    private Integer rawRecordId;


    // ================== 智能分类结果字段 ==================

    /**
     * 最终分类ID
     * (不管是通过规则、贝叶斯还是大模型算出来的，最终都存这个ID)
     */
    private Integer categoryId;

    /**
     * 分类来源 (留痕溯源，核心考点)
     * RULE: 规则命中
     * NB: 朴素贝叶斯
     * LLM: 大模型兜底
     * DEFAULT: 终极降级(其他)
     * MANUAL: 用户手动修改
     */
    private String classifySource;

    /** 置信度 (0.0 ~ 1.0) */
    private Double confidence;


    // ================== 状态控制字段 ==================

    /**
     * 账单状态
     * PENDING_REVIEW: 待校对 (刚导入进来)
     * CONFIRMED: 已确认 (用户在前端点了确认，或者没改直接确认)
     * IGNORED: 已忽略
     */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    // 记录导入类型
    private String recordType;

    // 原始数据
    private String tempRawRecord;

    private String tempRecordJson;

    // 反哺
    private boolean ruleUpdate;
}