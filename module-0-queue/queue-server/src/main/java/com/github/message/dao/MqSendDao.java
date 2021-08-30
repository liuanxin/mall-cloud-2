package com.github.message.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.message.model.MqSendEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** mq 生产记录 --> t_mq_send */
@Mapper
public interface MqSendDao extends BaseMapper<MqSendEntity> {

    int insertOrUpdate(MqSendEntity record);

    int batchInsert(@Param("list") List<MqSendEntity> list);

    int batchReplace(@Param("list") List<MqSendEntity> list);
}
