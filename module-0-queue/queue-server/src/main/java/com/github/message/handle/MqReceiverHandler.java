package com.github.message.handle;

import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.global.service.RedissonService;
import com.github.message.constant.MqConst;
import com.github.message.model.MqData;
import com.github.message.model.MqInfo;
import com.github.message.model.MqReceiveEntity;
import com.github.message.service.MqReceiveService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * <pre>
 * mq 发布订阅(无需再定义 Exchange、Queue、Binding 进 spring 上下文)
 *
 * 1. 在 MqConst 中新增 4 个值: desc(队列描述）、exchangeName(交换机名)、routingKey(路由键)、queueName(队列名)
 * 2. 在 MqInfo 中新增 1 个枚举, 将上面的 4 个值对应起来
 * 3. 发布 mq 消息
 *     private final MqSenderHandler handler;
 *
 *     public void xxx() {
 *         // 发布到 mq 的实体
 *         XXX<YYY> req = ...;
 *         handler.doProvide(MqInfo.xxx, JsonUtil.toJson(req));
 *     }
 * 4. 消费 mq 消息
 *     private final XxxService xxxService;
 *     private final MqReceiverHandler handler;
 *
 *     &#064;RabbitListener(queues = MqConst.xxx)
 *     public void onReceive(Message message, Channel channel) {
 *         handler.doConsume(message, channel, this::business);
 *     }
 *     public void business(String json) {
 *         // 从 mq 接收到的实体
 *         XXX<YYY> req = JsonUtil.convertType(json, new TypeReference<XXX<YYY>>() {});
 *         if (ObjectUtil.isNotNull(req)) {
 *             xxxService.xxx(req);
 *         }
 *     }
 * </pre>
 */
@RequiredArgsConstructor
@Slf4j
@Configuration
@ConditionalOnClass(RabbitListener.class)
public class MqReceiverHandler {

    @Value("${mq.consumerRetryCount:5}")
    private int consumerRetryCount;

    private final RedissonService redisService;
    private final MqReceiveService mqReceiveService;

    /**
     * 消息处理, 需要设置 spring.rabbitmq.listener.simple.acknowledge-mode = manual 才可以手动处理 ack
     *
     * 消费成功时: 发送 ack 并写记录(状态为成功)
     * 消费失败时:
     *   未达到上限则 nack(重回队列) 并写记录(状态为失败)
     *   已达到上限则 ack 并写记录(状态为失败)
     */
    public void doConsume(Message message, Channel channel, Consumer<String> consumer) {
        MqData data = JsonUtil.toObjectNil(new String(message.getBody()), MqData.class);
        // 发布如果是用的 MqSenderHandler 这里是一定会有值的
        if (U.isNull(data)) {
            return;
        }

        String desc = ("(" + data.getMqInfo().getDesc() + ")");
        long start = System.currentTimeMillis();
        try {
            LogUtil.bindBasicInfo(message.getMessageProperties().getCorrelationId());
            if (log.isInfoEnabled()) {
                log.info("开始消费 mq{}, 消息发送时间({})", desc, DateUtil.formatDateTimeMs(data.getSendTime()));
            }
            doDataConsume(data, message, channel, consumer);
        } finally {
            if (log.isInfoEnabled()) {
                log.info("消费 mq{} 结束, 耗时: ({})", desc, DateUtil.toHuman(System.currentTimeMillis() - start));
            }
            LogUtil.unbind();
        }
    }

    private void doDataConsume(MqData data, Message message, Channel channel, Consumer<String> consumer) {
        String msgId = message.getMessageProperties().getMessageId();
        if (redisService.tryLock(msgId)) {
            try {
                String json = data.getData();
                if (U.isEmpty(json)) {
                    return;
                }

                MqInfo mqInfo = data.getMqInfo();
                String desc = mqInfo.consumerDesc();
                if (log.isDebugEnabled()) {
                    log.debug("{}接收到消息({})", desc, json);
                }

                MqReceiveEntity model = mqReceiveService.queryByMsgAndAppCode(msgId, MqConst.APP_CODE);
                if (U.isNull(model)) {
                    model = new MqReceiveEntity();
                    model.setQueue(mqInfo.getQueueName());
                    model.setMsgId(data.getMsgId());
                    model.setAppCode(MqConst.APP_CODE);
                    model.setBusinessType(mqInfo.name().toLowerCase());
                    model.setRetryCount(0);
                    model.setMsgJson(json);

                    // model.setStatus(CommonConst.ZERO);
                    // model.setRemark(String.format("开始消费(%s)", desc));
                    // mqReceiveService.add(model);
                } else {
                    model.setRetryCount(model.getRetryCount() + 1);
                }

                // 成功了就只写一次消费成功, 失败了也只写一次, 上面不写初始, 少操作一次 db
                long deliveryTag = message.getMessageProperties().getDeliveryTag();
                try {
                    consumer.accept(json);
                    if (log.isDebugEnabled()) {
                        log.debug("{}消费成功", desc);
                    }
                    ack(channel, deliveryTag, String.format("%s消费成功, 发送 ack 时异常", desc));

                    model.setStatus(2);
                    model.setRemark(String.format("消费(%s)成功", desc));
                    mqReceiveService.addOrUpdate(model);
                } catch (Exception e) {
                    String failMsg = e.getMessage();
                    if (log.isErrorEnabled()) {
                        log.error(String.format("%s消费失败", desc), e);
                    }
                    model.setStatus(1);
                    // 如果重试次数达到设定的值则发送 ack, 否则发送 nack
                    if (model.getRetryCount() > consumerRetryCount) {
                        ack(channel, deliveryTag, String.format("%s消费失败且重试(%s)达到上限, 发送 ack 时异常", desc, consumerRetryCount));
                        model.setRemark(String.format("消费(%s)失败(%s)且重试(%s)达到上限", desc, failMsg, consumerRetryCount));
                    } else {
                        nack(channel, deliveryTag, String.format("%s消费失败, 发送 nack 时异常", desc));
                        model.setRemark(String.format("消费(%s)失败(%s)", desc, failMsg));
                    }
                    mqReceiveService.addOrUpdate(model);
                }
            } finally {
                redisService.unlock(msgId);
            }
        } else {
            log.info("消息 id({})正在被处理", msgId);
        }
    }

    private void ack(Channel channel, long deliveryTag, String errorDesc) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(errorDesc, e);
            }
        }
    }
    private void nack(Channel channel, long deliveryTag, String errorDesc) {
        try {
            channel.basicNack(deliveryTag, false, true);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(errorDesc, e);
            }
        }
    }
}
