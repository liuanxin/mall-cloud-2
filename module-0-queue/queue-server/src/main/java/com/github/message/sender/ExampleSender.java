package com.github.message.sender;

import com.github.message.handle.MqSenderHandler;
import com.github.message.model.MqInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ExampleSender {

    private final MqSenderHandler handler;

    public void send(String json) {
        handler.doProvide(MqInfo.EXAMPLE, json);
    }
}
