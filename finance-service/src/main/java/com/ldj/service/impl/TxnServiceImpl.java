package com.ldj.service.impl;

import com.ldj.mapper.BayesCategoryStatMapper;
import com.ldj.mapper.TxnMapper;
import com.ldj.pojo.Txn;
import com.ldj.service.BayesService;
import com.ldj.service.TxnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TxnServiceImpl implements TxnService {
    private final MatchMethod matchMethod;
    private final BayesService bayesService;
    private final TxnMapper txnMapper;
    private final BayesCategoryStatMapper bayesCategoryStatMapper;

    @Override
    public Txn updateCategoryById(Integer id, Integer categoryId) {
        String text;
        Txn txn = txnMapper.selectById(id);
        String categoryName = bayesCategoryStatMapper.selectCategoryNameById(categoryId);
        if(txn == null)
        {
            throw new RuntimeException("账单不存在");
        }
        txn.setCategoryId(categoryId);
        txn.setClassifySource("MANUAL");
        txn.setStatus("CONFIRMED");
        txnMapper.updateByTxn(txn);

        // 开始反哺用户规则
        if(categoryName != null)
        {
            text = (txn.getCounterpartyEnc() == null ? "" : txn.getCounterpartyEnc()) + (txn.getSummary() == null ? "" : txn.getSummary());
            matchMethod.trainUserRuleWeight(id, text, categoryId);
            log.info("用户规则修改成功");

            bayesService.train(categoryName, text);
            log.info("贝叶斯模型已学习 [{}] -> {}", text, categoryName);
        }
        txn.setRuleUpdate(true);
        return txn;
    }
}
