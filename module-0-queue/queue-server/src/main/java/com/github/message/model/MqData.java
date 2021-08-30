package com.github.message.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqData implements Serializable {
    private static final long serialVersionUID = 0L;

    private String msgId;
    private String traceId;
    private Date sendTime;
    private MqInfo mqInfo;
    private String data;
}
