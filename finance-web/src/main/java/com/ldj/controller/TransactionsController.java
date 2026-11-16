package com.ldj.controller;

import com.ldj.pojo.Result;
import com.ldj.pojo.Txn;
import com.ldj.service.TxnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
public class TransactionsController {
    private final TxnService txnService;

    @PatchMapping("/{id}/category")
    public Result updateCategory(@PathVariable("id") Integer id, Integer NewCategoryId)
    {
        Txn txn = txnService.updateCategoryById(id, NewCategoryId);
        return Result.success(txn);
    }

}
