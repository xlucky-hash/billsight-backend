package com.ldj.mapper;

import com.ldj.pojo.BayesWordFreq;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface BayesWordFreqMapper {
    // 核心：存在则词频+1，不存在则插入(初始频次为1)
    void insertOrIncrement(String categoryName, Set<String> uniqueWord);

    @Select("SELECT * FROM bayes_word_freq")
    List<BayesWordFreq> selectAll();
}
