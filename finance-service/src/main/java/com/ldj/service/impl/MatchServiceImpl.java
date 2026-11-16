package com.ldj.service.impl;

import com.ldj.mapper.MatchMapper;
import com.ldj.pojo.MatchResult;
import com.ldj.pojo.sysRule;
import com.ldj.pojo.UserRule;
import com.ldj.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {
    private final MatchMapper matchMapper;
    private final MatchMethod matchMethod;

    @Override
    public Optional<MatchResult> match(Integer userId, String text) {
        // 1.判断文本是否为空
        if(text == null || text.isBlank()) return Optional.empty();
        // 2.格式化文本
        text = matchMethod.normalize(text);
        // 3.查找数据库中该用户的所有规则
        List<UserRule> userRules = matchMapper.selectAllUserRuleById(userId);
        List<sysRule> systemRules = matchMapper.selectAllSystemRuleById(userId);
        // 4.进行规则匹配
        // 精切匹配
        for (UserRule rule : userRules) {
            // 匹配成功
            if(text.equals(rule.getFeature()))
            {
                matchMethod.updateUserRuleHitCount(rule);
                return Optional.of(matchMethod.buildResult(rule.getCategoryId(), rule.getId(), "USER", true, rule.getHitCount(), rule.getWeight()));
            }
        }

        for (sysRule rule : systemRules) {
            if(text.equals(rule.getKeyword()))
            {
                matchMethod.updateSystemRuleHitCount(rule);
                return Optional.of(matchMethod.buildResult(rule.getCategoryId(), rule.getId(), "SYSTEM", true, null, null));
            }
        }
        // 模糊匹配
        for (UserRule rule : userRules) {
            // 匹配成功
            if(text.contains(rule.getFeature()))
            {
                matchMethod.updateUserRuleHitCount(rule);
                return Optional.of(matchMethod.buildResult(rule.getCategoryId(), rule.getId(), "USER", false, rule.getHitCount(), rule.getWeight()));
            }
        }

        for (sysRule rule : systemRules) {
            if(text.contains(rule.getKeyword()))
            {
                matchMethod.updateSystemRuleHitCount(rule);
                return Optional.of(matchMethod.buildResult(rule.getCategoryId(), rule.getId(), "SYSTEM", false, null, null));
            }
        }
        // 匹配失败返回空的
        return Optional.empty();
    }

}
