package com.github.message.constant;

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
public class MqConst {

    // 一个队列有 交换机名(exchange)、路由(routing_key)、队列名(queue) 三个值

    // 1. 初始 --> 绑定(binding) queue 到 exchange, 基于 routing_key
    //   交换机分为三种:
    //     + Fanout(不指定 routingKey, 所有跟它绑定的 queue 都会接收到)
    //     + Direct(全匹配 routingKey)
    //     + Topic(模糊匹配, # 匹配一个或多个, * 匹配一个)
    //
    //   队列可以定义死信队列, 消费时如果出现以下三种情况, 该消息会被丢进死信(未配置消息将会被丢弃)
    //     + 消息被 channel.basicNack 或 channel.basicReject 且 requeue 的值是 false
    //     + 消息在队列的存活时间超出了设置的 ttl 时间
    //     + 消息队列的数量达到了上限
    //   设置重要的消息「死信 --> 死信的死信(延迟半小时) --> 死信」成一个环, 再「重要队列 --> 死信」
    //   死信的死信因为是一个延迟队列(就是想它到期了再回去死信), 因此不需要消费, 只消费死信队列即可

    // 2. 发送 ==> 向 exchange 的 routing_key 发送消息
    //   rabbitTemplate.convertAndSend(exchange, routingKey, Message(json), new SelfCorrelationData(mqInfo, json));

    // 3. 消费 ==> 基于 queue
    //   @RabbitListener(queues = xxx)


    public static final String APP_CODE = "in_bound";

    public static final String EXCHANGE = "Company.Team.Project.Exchange";


    public static final String DEAD_DESC = "死信队列";
    public static final String DEAD_ROUTING_KEY = "Company.Team.Project.DeadRoutingKey";
    public static final String DEAD_QUEUE = "Company.Team.Project.DeadQueue";

    public static final String DEAD_DEAD_DESC = "死信队列";
    public static final String DEAD_DEAD_ROUTING_KEY = "Company.Team.Project.DeadRoutingKey";
    public static final String DEAD_DEAD_QUEUE = "Company.Team.Project.DeadQueue";

    public static final String EXAMPLE_DESC = "示例推送";
    public static final String EXAMPLE_ROUTING_KEY = "Company.Team.Project.RoutingKey";
    public static final String EXAMPLE_QUEUE = "Company.Team.Project.Queue";
}
