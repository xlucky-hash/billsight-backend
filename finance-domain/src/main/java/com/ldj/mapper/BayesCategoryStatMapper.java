package com.ldj.mapper;

import com.ldj.pojo.BayesCategoryStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BayesCategoryStatMapper {
    // 核心：存在则更新账单数和总词数，不存在则插入
    void insertOrUpdate(String categoryName, Integer addWords);

    @Select("SELECT * FROM bayes_category_stat")
    List<BayesCategoryStat> selectAll();

    @Select("select id from bayes_category_stat where category_name = #{categoryName}")
    Integer selectCategoryIdByCategoryName(String categoryName);

    @Select("select category_name from bayes_category_stat where id = #{categoryId}")
    String selectCategoryNameById(Integer categoryId);
}
