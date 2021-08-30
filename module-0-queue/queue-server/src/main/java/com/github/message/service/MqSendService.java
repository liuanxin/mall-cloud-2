package com.github.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.message.dao.MqSendDao;
import com.github.message.model.MqSendEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MqSendService {

    private final MqSendDao mqSendDao;

    @Transactional
    public int add(MqSendEntity record) {
        return mqSendDao.insert(record);
    }

    @Transactional
    public int addBatch(List<MqSendEntity> list) {
        if (list == null || list.size() == 0) {
            return 0;
        }
        return mqSendDao.batchInsert(list);
    }

    @Transactional
    public int replaceBatch(List<MqSendEntity> list) {
        if (list == null || list.size() == 0) {
            return 0;
        }
        return mqSendDao.batchReplace(list);
    }

    @Transactional
    public int addOrUpdate(MqSendEntity record) {
        return mqSendDao.insertOrUpdate(record);
    }

    @Transactional
    public int updateById(MqSendEntity record) {
        return record == null ? 0 : mqSendDao.updateById(record);
    }

    @Transactional
    public int deleteById(Long id) {
        return (id == null || id <= 0) ? 0 : mqSendDao.deleteById(id);
    }

    public MqSendEntity queryById(Long id) {
        return (id == null || id <= 0) ? null : mqSendDao.selectById(id);
    }

    public List<MqSendEntity> queryByIds(List<Long> ids) {
        if (ids == null || ids.size() == 0) {
            return Collections.emptyList();
        }
        return mqSendDao.selectBatchIds(ids);
    }

    public MqSendEntity queryByMsgAndAppCode(String msgId, String appCode) {
        LambdaQueryWrapper<MqSendEntity> mqSendQuery = Wrappers.lambdaQuery(MqSendEntity.class);
        mqSendQuery.select(MqSendEntity::getId, MqSendEntity::getRetryCount)
                .eq(MqSendEntity::getMsgId, msgId).eq(MqSendEntity::getAppCode, appCode).last("LIMIT 1");
        return mqSendDao.selectOne(mqSendQuery);
    }
}
