package com.ldj.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BayesWordFreq {
    private Integer id;
    private String categoryName;    // 分类名
    private String word;    // 词语
    private Integer freqCount;  // 命中次数
    private LocalDateTime createAt;
    private LocalDateTime updatedAt;
}
