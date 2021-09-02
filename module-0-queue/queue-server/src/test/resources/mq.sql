
CREATE TABLE IF NOT EXISTS `t_mq_receive` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `queue` varchar(64) NOT NULL DEFAULT '' COMMENT '队列名',
  `idempotent_key` varchar(64) NOT NULL DEFAULT '' COMMENT '幂等键(比如「单号 + 状态」、「单号 + 更新时间」等)',
  `msg_id` varchar(32) NOT NULL DEFAULT '' COMMENT '消息 id',
  `app_code` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '应用代码(比如 0.未知, 1.用户, 2.商品, 3.订单)',
  `msg_type` varchar(32) NOT NULL DEFAULT '' COMMENT '消息场景类型',
  `status` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '消费状态(0.初始, 1.失败, 2.成功)',
  `retry_count` int unsigned NOT NULL DEFAULT '0' COMMENT '重试次数',
  `msg_json` longtext COMMENT '消息内容(json 格式)',
  `remark` longtext COMMENT '备注(消费失败时附上原因)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `udk_idempotent_key` (`idempotent_key`),
  INDEX `idk_msg_type` (`msg_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接收 mq 记录';

/* 要做消息的消费幂等, 需要按业务逻辑来 */

CREATE TABLE IF NOT EXISTS `t_mq_send` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `exchange` varchar(64) NOT NULL DEFAULT '' COMMENT '交换器',
  `routing_key` varchar(64) NOT NULL DEFAULT '' COMMENT '路由键',
  `msg_id` varchar(32) NOT NULL DEFAULT '' COMMENT '消息 id',
  `app_code` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '应用代码(比如 0.未知, 1.用户, 2.商品, 3.订单)',
  `msg_type` varchar(32) NOT NULL DEFAULT '' COMMENT '消息场景类型',
  `status` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '发送状态(0.初始, 1.失败, 2.成功)',
  `fail_type` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '失败类型(0.无错, 1.连接失败, 2.到交换机失败, 3. 到队列失败)',
  `retry_count` int unsigned NOT NULL DEFAULT '0' COMMENT '重试次数',
  `msg_json` longtext COMMENT '消息内容(json 格式)',
  `remark` varchar(512) NOT NULL DEFAULT '' COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idk_msg_type` (`msg_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发送 mq 记录';
