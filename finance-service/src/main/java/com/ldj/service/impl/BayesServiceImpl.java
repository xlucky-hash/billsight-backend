package com.ldj.service.impl;

import com.ldj.mapper.BayesCategoryStatMapper;
import com.ldj.mapper.BayesWordFreqMapper;
import com.ldj.pojo.BayesCategoryStat;
import com.ldj.pojo.BayesWordFreq;
import com.ldj.service.BayesService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BayesServiceImpl implements BayesService {
    private final BayesCategoryStatMapper bayesCategoryStatMapper;
    private final BayesWordFreqMapper bayesWordFreq;

    // 存储分类中出现的总账单
    private Map<String, Integer> categoryDocCount = new ConcurrentHashMap<>();
    // 存储分类中出现的总词数
    private Map<String, Integer> categoryTotalWords = new ConcurrentHashMap<>();
    // 存储该分类中出现的词
    private Map<String, Map<String, Integer>> wordFreq = new ConcurrentHashMap<>();
    // 计算数据库中存储的所有账单总数
    int totalDoc = 0;

    @PostConstruct
    @Override
    public void load() {
        // 1.从数据库中加载所有表
        List<BayesCategoryStat> bayesCategoryStats = bayesCategoryStatMapper.selectAll();
        List<BayesWordFreq> bayesWordFreqs = bayesWordFreq.selectAll();
        // 2.遍历集合并存储
        for (BayesCategoryStat data : bayesCategoryStats) {
            categoryDocCount.put(data.getCategoryName(), data.getDocCount());
            categoryTotalWords.put(data.getCategoryName(), data.getTotalWords());
            totalDoc += data.getDocCount();
        }

        for (BayesWordFreq freq : bayesWordFreqs) {
            wordFreq.computeIfAbsent(freq.getCategoryName(), k -> new ConcurrentHashMap<>()).put(freq.getWord(), freq.getFreqCount());
        }

        log.info("数据库中的数据已加载到内存中，分类：{}", wordFreq);

    }

    @Transactional(rollbackFor =  Exception.class)
    @Override
    public synchronized void train(String category, String text) {
        // 1.判断文本
        if(text == null || text.isBlank() || totalDoc == 0) return;
        // 2.判断集合中是否有这个分类，如果没有则创建
        Map<String, Integer> categoryText = wordFreq.computeIfAbsent(category, k -> new ConcurrentHashMap<>());
        String[] word = text.trim().split("[\\s,，、]+");
        int addCount = 0;
        Set<String> uniqueWords = new HashSet<>();
        // 3.遍历出现的文本，将文本中的数据添加
        for (String s : word) {
            if(s.isBlank()) continue;
            categoryText.merge(s, 1, Integer::sum);
            addCount++;
            uniqueWords.add(s);
            // 4.更新数据库
        }

        if(uniqueWords.isEmpty())return;

        bayesWordFreq.insertOrIncrement(category, uniqueWords);

        // 5.更新内存中的数据
        categoryDocCount.merge(category, 1, Integer::sum);
        categoryTotalWords.merge(category, addCount, Integer::sum);
        totalDoc ++;

        bayesCategoryStatMapper.insertOrUpdate(category, addCount);

        log.info("数据已经反哺，反哺的分类为：{}，更新的词语为：{}", category, text);
    }

    @Override
    public Optional<Map.Entry<String, Double>> predict(String text) {
        if(text == null || text.isBlank() || totalDoc == 0)
            return Optional.empty();
        String[] words = text.trim().split("[\\s,，、]+");
        if(words.length == 0)
            return Optional.empty();

        // 获取总词汇
        Set<String> categoryWords = new HashSet<>();
        for(Map<String, Integer> map : wordFreq.values())
        {
            categoryWords.addAll(map.keySet());
        }
        int T = categoryWords.size();
        double maxProd = -1;
        String bestCategory = null;

        // 计算每个分类的概率
        for (String category : categoryDocCount.keySet()) {
            // 计算该分类的概率
            double categoryProd = (double) categoryDocCount.get(category) / totalDoc;
            // 定义一个变量，让每个词组之间连×
            double likelihood = 1.0;
            // 拿到该分类总出现的所有词
            Map<String, Integer> categoryWord = wordFreq.getOrDefault(category, Collections.emptyMap());
            // 拿到该分类下出现过的词的总数
            Integer categoryWordNumber = categoryTotalWords.getOrDefault(category, 0);
            for (String word : words) {
                if(word.isBlank()) continue;
                // 判断这个词是否存在过
                Integer categoryWordTimes = categoryWord.getOrDefault(word, 0);
                double wordProd = (double) (categoryWordTimes + 1) / (categoryWordNumber + T);
                likelihood *= wordProd;
            }
            double finalScore = likelihood * categoryProd;

            log.debug("分类 [{}] 的得分: {}", category, finalScore);

            if(finalScore > maxProd) {
                maxProd = finalScore;
                bestCategory = category;
            }

        }

        return Optional.of(new AbstractMap.SimpleEntry<String, Double>(bestCategory, maxProd));
    }
}
