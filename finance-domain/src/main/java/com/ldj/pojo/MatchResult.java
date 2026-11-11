package com.ldj.pojo;

public record MatchResult(Integer categoryId, String classifySource, Double confidence, Integer ruleId, String ruleType)
{ }
