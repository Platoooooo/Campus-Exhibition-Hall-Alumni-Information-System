/* ============================================================
   迁移脚本：修复操作日志表 & 新建登录日志表
   执行前提：已 USE campus_exhibition
   ============================================================ */

USE `campus_exhibition`;

/* ---- 1. 重建 sys_oper_log（列定义对齐 Java Entity） ---- */
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `description` VARCHAR(100) DEFAULT NULL COMMENT '操作描述',
  `method`      VARCHAR(10)  DEFAULT NULL COMMENT 'GET/POST/PUT/DELETE',
  `uri`         VARCHAR(200) DEFAULT NULL COMMENT '请求URI',
  `user_id`     BIGINT       DEFAULT NULL COMMENT '操作人ID',
  `username`    VARCHAR(50)  DEFAULT NULL COMMENT '操作人用户名',
  `ip`          VARCHAR(50)  DEFAULT NULL,
  `params`      TEXT         DEFAULT NULL COMMENT '请求参数(已过滤敏感字段)',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0成功 1异常',
  `cost_ms`     BIGINT       DEFAULT NULL COMMENT '耗时(ms)',
  `error_msg`   VARCHAR(500) DEFAULT NULL COMMENT '异常信息',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_create` (`create_time`)
) ENGINE=InnoDB COMMENT='操作日志';

/* ---- 2. 新建 sys_login_log ---- */
CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       DEFAULT NULL,
  `username`    VARCHAR(50)  DEFAULT NULL,
  `ip`          VARCHAR(50)  DEFAULT NULL,
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0失败 1成功',
  `fail_reason` VARCHAR(200) DEFAULT NULL COMMENT '失败原因',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_create` (`create_time`)
) ENGINE=InnoDB COMMENT='登录日志';
