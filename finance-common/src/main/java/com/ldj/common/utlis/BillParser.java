package com.ldj.common.utlis;

import com.ldj.pojo.Txn;

import java.io.InputStream;
import java.util.List;

public interface BillParser {
    public boolean support(String fileName);

    public List<Txn> parse(InputStream inputStream);
}
