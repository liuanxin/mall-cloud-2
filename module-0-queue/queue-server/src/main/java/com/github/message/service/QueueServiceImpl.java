package com.github.message.service;

import com.github.queue.constant.QueueConst;
import com.github.queue.service.QueueService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.Queue;

/**
 * <p>类上的注解相当于下面的配置</p>
 *
 * &lt;dubbo:service exported="false" unexported="false"
 *     interface="com.github.message.service.MessageService"
 *     listener="" version="1.0" filter="" timeout="5000"
 *     id="com.github.message.service.MessageService" /&gt;
 */
@RestController
public class QueueServiceImpl implements QueueService {

    private final JmsTemplate jmsTemplate;
    private final Queue simpleQueue;

    public QueueServiceImpl(JmsTemplate jmsTemplate,
                            @Qualifier(QueueConst.SIMPLE_MQ_NAME) Queue simpleQueue) {
        this.jmsTemplate = jmsTemplate;
        this.simpleQueue = simpleQueue;
    }

    @Override
    public void submitSimple(String simpleInfo) {
        jmsTemplate.convertAndSend(simpleQueue, simpleInfo);
    }
}
