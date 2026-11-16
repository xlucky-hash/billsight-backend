package com.ldj.service.impl;


import com.ldj.mapper.BayesCategoryStatMapper;
import com.ldj.pojo.MatchResult;
import com.ldj.pojo.Txn;
import com.ldj.service.BayesService;
import com.ldj.service.ClassificationService;
import com.ldj.service.LlmService;
import com.ldj.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassificationServiceImpl implements ClassificationService {
    private final BayesService bayesService;
    private final LlmService llmService;
    private final MatchService matchService;
    private final BayesCategoryStatMapper bayesCategoryStatMapper;

    // 指定阈值
    public static final double matchThreshold = 0.6;
    public static final double bayesThreshold = 0.002;


    @Override
    public void classify(Txn txn) {
        if(txn == null) return;

        String counterparty = txn.getCounterpartyEnc() == null ? "" : txn.getCounterpartyEnc();
        String summary = txn.getSummary() == null ? "" : txn.getSummary();
        String text = counterparty + "," + summary;

        // 1. 调用用户匹配规则
        Optional<MatchResult> matchResult = matchService.match(txn.getUserId(), text);
        if(matchResult.isPresent() && matchResult.get().confidence() >= matchThreshold)
        {
            txn.setCategoryId(matchResult.get().categoryId());
            txn.setClassifySource("RULE");
            txn.setConfidence(matchResult.get().confidence());
            log.info("匹配成功，分类引擎模式为RULE");
            return;
        }

        Optional<Map.Entry<String, Double>> bayesResult = bayesService.predict(text);
        if(bayesResult.isPresent() && bayesResult.get().getValue() >= bayesThreshold)
        {
            Integer categoryId = getCategoryId(bayesResult.get().getKey());
            txn.setCategoryId(categoryId);
            txn.setConfidence(bayesResult.get().getValue());
            txn.setClassifySource("NB");
            log.info("匹配模式为：{}，正确率为：{}", "贝叶斯算法", bayesResult.get().getValue());
            return;
        }

        Optional<String> llmResult = llmService.classify(txn.getCounterpartyEnc(), txn.getSummary());
        if (llmResult.isPresent())
        {
            Integer categoryId = getCategoryId(llmResult.get());
            txn.setCategoryId(categoryId);
            txn.setClassifySource("LLM");
            txn.setConfidence(1.0);
            log.info("匹配模式为：{}，正确率为：{}", "大模型", 100.0);
            return;
        }
        log.warn("连接超时，降级为贝叶斯算法");
        if(bayesResult.isPresent())
        {
            Integer categoryId = getCategoryId(bayesResult.get().getKey());
            txn.setCategoryId(categoryId);
            txn.setConfidence(bayesResult.get().getValue());
            txn.setClassifySource("NB");
            log.info("大模型连接超时，降级为贝叶斯算法，置信度为：{}", bayesResult.get().getValue());
        }
        else{
            txn.setCategoryId(0); // 假设 0 代表“其他”
            txn.setConfidence(0.0);
            txn.setClassifySource("DEFAULT"); // 终极兜底
            log.error("降级失败：所有模型均无有效结果，置为默认分类(其他)");
        }
    }

    private Integer getCategoryId(String key) {
        Integer categoryId = bayesCategoryStatMapper.selectCategoryIdByCategoryName(key);
        return categoryId == null ? 0 : categoryId;
    }
}
