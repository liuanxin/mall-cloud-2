package com.github.message.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.message.model.MqReceiveEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** mq 消费记录 --> t_mq_receive */
@Mapper
public interface MqReceiveDao extends BaseMapper<MqReceiveEntity> {

    int insertOrUpdate(MqReceiveEntity record);

    int batchInsert(@Param("list") List<MqReceiveEntity> list);

    int batchReplace(@Param("list") List<MqReceiveEntity> list);
}
