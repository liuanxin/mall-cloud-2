
DROP TABLE IF EXISTS `t_common_config`;
CREATE TABLE IF NOT EXISTS `t_common_config` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `key` varchar(32) NOT NULL COMMENT '键',
  `value` varchar(256) NOT NULL COMMENT '值',
  `comment` varchar(512) NOT NULL COMMENT '说明',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `con_key` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置表';
