package com.github.global.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

/** 当前类上的方法, 在其他类调用时, 都会异步运行 */
@Async
@Configuration
public class AsyncService {

    /** 异步发送短信 */
    public void sendSms(String phone, String sms) {
        // todo ...
    }
}
