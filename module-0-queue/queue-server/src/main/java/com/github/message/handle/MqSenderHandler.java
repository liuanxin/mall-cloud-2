package com.github.message.handle;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
import com.github.common.util.ApplicationContexts;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.message.constant.MqConst;
import com.github.message.model.MqData;
import com.github.message.model.MqInfo;
import com.github.message.model.MqSendEntity;
import com.github.message.model.SelfCorrelationData;
import com.github.message.service.MqSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

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
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"NullableProblems", "ConstantConditions"})
@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class MqSenderHandler implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    @Value("${mq.providerRetryCount:3}")
    private int providerRetryCount;

    private final RabbitTemplate rabbitTemplate;
    private final MqSendService mqSendService;

    /**
     * 消息发送, 发送时只能确定「可以到达 exchange」和「未到达 queue」, 因此先将记录设置为成功,
     * 在「连接失败」、「到达 exchange 后失败重试达到上限」、「未到达 queue」时再将记录设置为失败
     */
    public void doProvide(MqInfo mqInfo, String json) {
        provide(new SelfCorrelationData(String.valueOf(IdWorker.getId()), LogUtil.getTraceId(), mqInfo, json));
    }

    private void provide(SelfCorrelationData correlationData) {
        String msgId = correlationData.getId();
        String traceId = correlationData.getTraceId();
        MqInfo mqInfo = correlationData.getMqInfo();
        String json = correlationData.getJson();

        String exchangeName = mqInfo.getExchangeName();
        String routingKey = mqInfo.getRoutingKey();
        String desc = mqInfo.providerDesc();

        MqData mqData = new MqData(msgId, traceId, DateUtil.now(), mqInfo, json);

        MqSendEntity model = mqSendService.queryByMsgAndAppCode(msgId, MqConst.APP_CODE);
        if (U.isNull(model)) {
            model = new MqSendEntity();
            model.setExchange(exchangeName);
            model.setRoutingKey(routingKey);
            model.setMsgId(msgId);
            model.setAppCode(MqConst.APP_CODE);
            model.setBusinessType(mqInfo.name().toLowerCase());
            // 先写成功, 后面异常时写失败
            model.setStatus(2);
            model.setFailType(0);
            model.setRetryCount(0);
            model.setMsgJson(json);
            model.setRemark(String.format("消息(%s)开始发送", desc));
            mqSendService.add(model);
        } else {
            // 先写成功, 后面异常时写失败
            model.setStatus(2);
            model.setFailType(0);
            model.setRetryCount(model.getRetryCount() + 1);
            model.setRemark(String.format("消息(%s)重试", desc));
            mqSendService.updateById(model);
        }

        String data = JsonUtil.toJson(mqData);
        // 默认是持久化的 setDeliveryMode(MessageDeliveryMode.PERSISTENT)
        Message msg = MessageBuilder.withBody(data.getBytes(StandardCharsets.UTF_8))
                .setMessageId(msgId).setCorrelationId(traceId).build();
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, msg, correlationData);
        } catch (Exception e) {
            MqSendEntity errorModel = new MqSendEntity();
            errorModel.setId(model.getId());
            errorModel.setStatus(1);
            errorModel.setFailType(1);
            errorModel.setRemark(String.format("连接 mq 失败(%s)", desc));
            mqSendService.updateById(errorModel);
        }
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            if (log.isDebugEnabled()) {
                log.debug("消息({})到 exchange 成功", JsonUtil.toJson(correlationData));
            }
        } else {
            if (log.isErrorEnabled()) {
                log.error("消息({})到 exchange 失败, 原因({})", JsonUtil.toJson(correlationData), cause);
            }
            if (correlationData instanceof SelfCorrelationData) {
                SelfCorrelationData data = (SelfCorrelationData) correlationData;

                // 从记录中获取重试次数
                String messageId = correlationData.getId();
                long msgId = U.toLong(messageId);
                if (msgId > 0) {
                    MqSendEntity mqSend = mqSendService.queryById(msgId);
                    if (U.isNotNull(mqSend) && U.greater0(mqSend.getRetryCount())) {
                        if (mqSend.getRetryCount() < providerRetryCount) {
                            // 重试
                            ApplicationContexts.getBean(MqSenderHandler.class).provide(data);
                        } else {
                            MqSendEntity model = new MqSendEntity();
                            model.setId(msgId);
                            model.setStatus(1);
                            model.setFailType(2);
                            model.setRemark(String.format("%s发送失败且重试(%s)达到上限", data.getMqInfo().providerDesc(), providerRetryCount));
                            mqSendService.addOrUpdate(model);
                        }
                    }
                }
            }
        }
    }

    /*
    发布消息, rabbitmq 的投递路径是:
      producer -> exchange -- (routing_key) --> queue -> consumer

    ConfirmCallback#confirm : 消息跟 exchange 交互时调用, 成功到达则 ack 为 true, 此时还无法确定消息有没有到达 queue
    ReturnCallback#returnedMessage : 消息 routing 不到 queue 时调用(需要设置 spring.rabbitmq.template.mandatory = true 默认会将消息丢弃)
    */

    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        if (log.isErrorEnabled()) {
            log.error("消息({})无法到队列: 响应码({}) 响应文本({}) 交换机({}) 路由键({})",
                    JsonUtil.toJson(message), replyCode, replyText, exchange, routingKey);
        }
        long msgId = U.toLong(message.getMessageProperties().getMessageId());
        if (msgId > 0) {
            MqSendEntity model = new MqSendEntity();
            model.setId(msgId);
            model.setStatus(0);
            model.setFailType(3);
            model.setRemark(replyText);
            mqSendService.addOrUpdate(model);
        }
    }
}
