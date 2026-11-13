package com.ldj.mapper;

import com.ldj.pojo.sysRule;
import com.ldj.pojo.UserRule;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MatchMapper {

    @Select("select * from user_rule where user_id = #{userId} and enabled = 1")
    List<UserRule> selectAllUserRuleById(Integer userId);

    @Select("select * from user_rule where user_id = #{userId} and feature = #{feature} and enabled = 1")
    UserRule selectUserRuleByIdAndFeature(Integer userId, String feature);

    @Select("select * from sys_rule where enabled = 1")
    List<sysRule> selectAllSystemRuleById(Integer userId);

    void userSave(UserRule userRule);

    @Insert("insert into user_rule(user_id, feature, category_id, hit_count, weight, enabled, created_at, updated_at) VALUES (#{userId}, #{feature}, #{categoryId}, #{hitCount}, #{weight}, #{enabled}, #{createdAt}, #{updatedAt})")
    void insertUserRule(UserRule rule);

    @Update("update sys_rule set priority = #{priority} where id = #{id} and enabled = 1")
    void sysSave(sysRule rule);
}
