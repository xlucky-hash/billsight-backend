package com.ldj.mapper;

import com.ldj.pojo.RawRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

import java.util.List;

@Mapper
public interface RawRecordMapper {

    public void insert(List<RawRecord> rawRecords);

    @Insert("insert into raw_record(user_id, batch_id, record_type, raw_content, raw_json, parse_status, created_at) values(#{userId}, #{batchId}, #{recordType}, #{rawContent}, #{rawJson}, #{parseStatus}, #{createdAt})")
    public void insertOne(RawRecord record);
}
