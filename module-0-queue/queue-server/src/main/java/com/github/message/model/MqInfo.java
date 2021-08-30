package com.github.message.model;

import com.github.common.util.A;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

import static com.github.message.constant.MqConst.*;


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
@Getter
@AllArgsConstructor
public enum MqInfo {

    /** 死信 --> 如果处理失败将会到死信的死信 */
    DEAD(DEAD_DESC, EXCHANGE, DEAD_ROUTING_KEY, DEAD_QUEUE, A.maps(
            "x-dead-letter-exchange", EXCHANGE,
            "x-dead-letter-routing-key", DEAD_ROUTING_KEY
    )),

    /** 死信的死信(延迟 10 秒), 延迟后将会回到死信 */
    DEAD_DEAD(DEAD_DEAD_DESC, EXCHANGE, DEAD_DEAD_ROUTING_KEY, DEAD_DEAD_QUEUE, A.maps(
            "x-dead-letter-exchange", EXCHANGE,
            "x-dead-letter-routing-key", DEAD_ROUTING_KEY,
            "x-message-ttl", 10000
    )),

    EXAMPLE(EXAMPLE_DESC, EXCHANGE, EXAMPLE_ROUTING_KEY, EXAMPLE_QUEUE, Collections.emptyMap());

    private final String desc;
    private final String exchangeName;
    private final String routingKey;
    private final String queueName;
    private final Map<String, Object> args;

    public String providerDesc() {
        return String.format("%s(%s -- %s --> %s)", desc, exchangeName, routingKey, queueName);
    }
    public String consumerDesc() {
        return String.format("%s(%s <-- %s -- %s)", desc, queueName, routingKey, exchangeName);
    }
}
