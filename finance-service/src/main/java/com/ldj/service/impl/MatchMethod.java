package com.ldj.service.impl;

import com.ldj.mapper.MatchMapper;
import com.ldj.pojo.MatchResult;
import com.ldj.pojo.sysRule;
import com.ldj.pojo.UserRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchMethod {

    private final MatchMapper matchMapper;

    // 计算规则匹配置信度
    public MatchResult buildResult(Integer categoryId, Integer id, String matchType, boolean isExact, Integer hitCount, BigDecimal weight) {
        double confidence;
        // 判断是否为精确匹配
        if(isExact)
        {
            confidence = 1.0;
            if("SYSTEM".equals(matchType)) confidence = 0.95;
        }
        else{
            double base = 0.9;
            double userRuleWeight = "USER".equals(matchType) ? (weight != null ? weight.doubleValue() : 1.0) : 0.85;
            double hitCountBonus = Math.min((hitCount == null ? 0 : hitCount) * 0.01, 0.08);
            confidence = 0.8 * base * userRuleWeight + hitCountBonus;
        }
        confidence = Math.min(confidence, 0.99);
        return new MatchResult(categoryId, "RULE", confidence, id, matchType);
    }

    // 更新规则
    public void updateUserRuleHitCount(UserRule rule) {
        try {
            rule.setHitCount(rule.getHitCount() + 1);
            matchMapper.userSave(rule);
            log.info("用户规则已更新：用户Id：{}，用户分类Id：{}，特征：{}，用户命中次数：{}", rule.getUserId(), rule.getCategoryId(), rule.getFeature(), rule.getHitCount());
        } catch (Exception e) {

        }
    }

    public void updateSystemRuleHitCount(sysRule rule) {
        try {
            rule.setPriority(rule.getPriority() + 1);
            matchMapper.sysSave(rule);
            log.info("用户规则已更新：用户Id：{}，用户分类Id：{}，特征：{}，用户命中次数：{}", rule.getId(), rule.getCategoryId(), rule.getKeyword(), rule.getPriority());
        } catch (Exception e) {

        }
    }

    public void trainUserRuleWeight(Integer userId, String feature, Integer categoryId)
    {
        UserRule rule = matchMapper.selectUserRuleByIdAndFeature(userId, feature);

        if(rule != null)
        {
            if(!rule.getCategoryId().equals(categoryId))
            {
                rule.setCategoryId(categoryId);
            }
            rule.setWeight(rule.getWeight().add(new BigDecimal("0.2")));
            rule.setHitCount(rule.getHitCount() + 1);
            matchMapper.userSave(rule);
            log.info("反哺成功：更新已有用户规则 -> {}", feature);
        }
        else
        {
            UserRule newUser = new UserRule();
            newUser.setUserId(userId);
            newUser.setCategoryId(categoryId);
            newUser.setFeature(feature);
            newUser.setWeight(new BigDecimal("1.0"));
            newUser.setHitCount(1);
            newUser.setEnabled(1);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setUpdatedAt(LocalDateTime.now());
            matchMapper.insertUserRule(newUser);

            log.info("反哺成功：生成全新用户规则 -> {}", feature);
        }
    }

    public String normalize(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", "").toLowerCase();
    }
}
