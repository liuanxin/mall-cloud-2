package com.github.message.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** mq 消费记录 --> t_mq_receive */
@Data
@TableName("t_mq_receive")
public class MqReceiveEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 队列 --> queue */
    private String queue;

    /** 消息 id --> msg_id */
    private String msgId;

    /** 应用代码 --> app_code */
    private String appCode;

    /** 业务场景 --> business_type */
    private String businessType;

    /** 状态(0.初始, 1.失败, 2.成功) --> status */
    private Integer status;

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
