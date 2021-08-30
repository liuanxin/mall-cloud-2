package com.github.message.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** mq 生产记录 --> t_mq_send */
@Data
@TableName("t_mq_send")
public class MqSendEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 交换器 --> exchange */
    private String exchange;

    /** 路由键 --> routing_key */
    private String routingKey;

    /** 消息 id --> msg_id */
    private String msgId;

    /** 应用代码 --> app_code */
    private String appCode;

    /** 业务场景 --> business_type */
    private String businessType;

    /** 状态(0.初始, 1.失败, 2.成功) --> status */
    private Integer status;

    /** 错误类型(0.无错, 1.连接失败, 2.到交换机失败, 3. 到队列失败) --> fail_type */
    private Integer failType;

    /** 重试次数 --> retry_count */
    private Integer retryCount;

    /** 消息内容(json 格式) --> msg_json */
    private String msgJson;

    /** 备注 --> remark */
    private String remark;

    /** 创建时间 --> create_time */
    private Date createTime;

    /** 更新时间 --> update_time */
    private Date updateTime;
}
