package com.ldj.mapper;

import com.ldj.pojo.Txn;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TxnMapper {
    void insert(List<Txn> txn);

    @Insert("insert into txn(user_id,counterparty_enc, amount, txn_time, category_id, classify_source, confidence, batch_id, direction, status) values" +
            "(#{userId}, #{counterpartyEnc}, #{amount}, #{txnTime}, #{categoryId}, #{classifySource}, #{confidence}, #{batchId}, #{direction}, #{status})")
    void insertOne(Txn txn);

    @Select("select * from txn where id = #{id}")
    Txn selectById(Integer id);

    @Update("update txn set category_id = #{categoryId}, status = #{status}, classify_source = #{classifySource} where id = #{id}")
    void updateByTxn(Txn txn);

}
