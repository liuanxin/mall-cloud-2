package com.github.message.listener;

import com.github.message.constant.MqConst;
import com.github.message.handle.MqReceiverHandler;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ExampleListener {

    private final MqReceiverHandler handler;

    @RabbitListener(queues = MqConst.EXAMPLE_QUEUE)
    public void onReceive(Message message, Channel channel) {
        handler.doConsume(message, channel, this::business);
    }

    public void business(String json) {
         /*
         XXX req = JsonUtil.convert(json, XXX.class);
         if (ObjectUtil.isNotNull(req)) {
             xxxService.doSomething(json);
         }
         */
    }
}
