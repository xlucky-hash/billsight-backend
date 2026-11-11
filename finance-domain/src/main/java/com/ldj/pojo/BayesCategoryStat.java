package com.ldj.pojo;

import lombok.Data;

@Data
public class BayesCategoryStat {
    private Integer id;
    private String categoryName; // 分类名
    private Integer docCount; // 账单数
    private Integer totalWords; // 该分类中出现的总词数
}
