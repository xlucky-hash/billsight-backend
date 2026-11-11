package com.ldj.pojo;

import lombok.Data;

import java.time.LocalTime;

@Data
public class sysRule {
    private Integer id;
    private String keyword;
    private Integer categoryId;
    private Integer priority;
    private Integer enable;
    private LocalTime createAt;
}
