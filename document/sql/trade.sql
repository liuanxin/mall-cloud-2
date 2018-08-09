
DROP TABLE IF EXISTS `t_trade_order`;
CREATE TABLE IF NOT EXISTS `t_trade_order` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) NOT NULL comment '订单号',
  `user_id` bigint(20) unsigned NOT NULL comment '买家 id',
  `business_id` bigint(20) unsigned comment '卖家 id',
  `pay_amount` bigint(20) unsigned not null comment '支付金额(单位: 分)',
  `order_state` int(10) unsigned not null comment '订单状态(1.已创建, 2.已支付, 10.已取消, 11.已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment '订单表';
