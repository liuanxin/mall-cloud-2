package com.github.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.message.dao.MqReceiveDao;
import com.github.message.model.MqReceiveEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MqReceiveService {

    private final MqReceiveDao mqReceiveDao;

    @Transactional
    public int add(MqReceiveEntity record) {
        return mqReceiveDao.insert(record);
    }

    @Transactional
    public int addBatch(List<MqReceiveEntity> list) {
        if (list == null || list.size() == 0) {
            return 0;
        }
        return mqReceiveDao.batchInsert(list);
    }

    @Transactional
    public int replaceBatch(List<MqReceiveEntity> list) {
        if (list == null || list.size() == 0) {
            return 0;
        }
        return mqReceiveDao.batchReplace(list);
    }

    @Transactional
    public int addOrUpdate(MqReceiveEntity record) {
        return mqReceiveDao.insertOrUpdate(record);
    }

    @Transactional
    public int updateById(MqReceiveEntity record) {
        return record == null ? 0 : mqReceiveDao.updateById(record);
    }

    @Transactional
    public int deleteById(Long id) {
        return (id == null || id <= 0) ? 0 : mqReceiveDao.deleteById(id);
    }

    public MqReceiveEntity queryById(Long id) {
        return (id == null || id <= 0) ? null : mqReceiveDao.selectById(id);
    }

    public List<MqReceiveEntity> queryByIds(List<Long> ids) {
        if (ids == null || ids.size() == 0) {
            return Collections.emptyList();
        }
        return mqReceiveDao.selectBatchIds(ids);
    }

    public MqReceiveEntity queryByMsgAndAppCode(String msgId, String appCode) {
        LambdaQueryWrapper<MqReceiveEntity> mqSendQuery = Wrappers.lambdaQuery(MqReceiveEntity.class);
        mqSendQuery.select(MqReceiveEntity::getId, MqReceiveEntity::getRetryCount)
                .eq(MqReceiveEntity::getMsgId, msgId).eq(MqReceiveEntity::getAppCode, appCode).last("LIMIT 1");
        return mqReceiveDao.selectOne(mqSendQuery);
    }
}
