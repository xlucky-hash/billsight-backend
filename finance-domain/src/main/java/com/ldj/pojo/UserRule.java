package com.ldj.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class UserRule {
    // 主键
    private Integer id;
    // 用户Id
    private Integer userId;
    // 特征词
    private String feature;
    // 分类Id
    private Integer categoryId;
    // 命中次数
    private Integer hitCount;
    // 权重
    private BigDecimal weight;
    // 删除Id
    private Integer enabled;
    // 创建时间和修改时间
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
