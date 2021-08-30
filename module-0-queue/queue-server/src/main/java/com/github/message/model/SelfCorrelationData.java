package com.github.message.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelfCorrelationData extends CorrelationData {

    private String traceId;
    private MqInfo mqInfo;
    private String json;

    public SelfCorrelationData(String msgId, String traceId, MqInfo mqInfo, String json) {
        super(msgId);

        this.traceId = traceId;
        this.mqInfo = mqInfo;
        this.json = json;
    }
}
