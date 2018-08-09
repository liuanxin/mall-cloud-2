package com.github.message.config;

import com.github.queue.constant.QueueConst;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.Queue;

@Configuration
public class QueueConfig {

    @Bean(QueueConst.SIMPLE_MQ_NAME)
    public Queue simpleQueue() {
        return new ActiveMQQueue(QueueConst.SIMPLE_MQ_NAME);
    }
}
