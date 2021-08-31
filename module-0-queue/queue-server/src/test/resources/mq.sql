
CREATE TABLE IF NOT EXISTS `t_mq_receive` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `queue` varchar(64) NOT NULL DEFAULT '' COMMENT '队列',
  `msg_id` varchar(32) NOT NULL DEFAULT '' COMMENT '消息 id',
  `app_code` varchar(16) NOT NULL DEFAULT '' COMMENT '应用代码(ods-in-bound, ods-out-bound, ods-after-sale 等)',
  `business_type` varchar(32) NOT NULL DEFAULT '' COMMENT '业务场景',
  `status` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '状态(0.初始, 1.失败, 2.成功)',
  `retry_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '重试次数',
  `msg_json` longtext COMMENT '消息内容(json 格式)',
  `remark` varchar(512) NOT NULL DEFAULT '' COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `udk_msg_app` (`msg_id`,`app_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mq 消费记录';


CREATE TABLE IF NOT EXISTS `t_mq_send` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `exchange` varchar(64) NOT NULL DEFAULT '' COMMENT '交换器',
  `routing_key` varchar(64) NOT NULL DEFAULT '' COMMENT '路由键',
  `msg_id` varchar(32) NOT NULL DEFAULT '' COMMENT '消息 id',
  `app_code` varchar(16) NOT NULL DEFAULT '' COMMENT '应用代码(ods-in-bound, ods-out-bound, ods-after-sale 等)',
  `business_type` varchar(32) NOT NULL DEFAULT '' COMMENT '业务场景',
  `status` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '状态(0.初始, 1.失败, 2.成功)',
  `fail_type` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '错误类型(0.无错, 1.连接失败, 2.到交换机失败, 3. 到队列失败)',
  `retry_count` int(10) unsigned NOT NULL DEFAULT '0' COMMENT '重试次数',
  `msg_json` longtext COMMENT '消息内容(json 格式)',
  `remark` varchar(512) NOT NULL DEFAULT '' COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `udk_msg_app` (`msg_id`,`app_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='mq 生产记录';
