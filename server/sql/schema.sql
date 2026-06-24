/* ============================================================
   校园展览馆校友资料系统 · 建库建表（schema only）
   MySQL 8.0 / utf8mb4 / InnoDB
   不含任何数据行
   ============================================================ */

CREATE DATABASE IF NOT EXISTS `campus_exhibition`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
USE `campus_exhibition`;

/* ---------- 1. 学院字典 ---------- */
CREATE TABLE `sys_college` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(100) NOT NULL COMMENT '学院名称',
  `code`        VARCHAR(50)  NOT NULL COMMENT '学院编码',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB COMMENT='学院字典';

/* ---------- 2. 专业字典 ---------- */
CREATE TABLE `sys_major` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `college_id`  BIGINT       NOT NULL COMMENT '所属学院',
  `name`        VARCHAR(100) NOT NULL COMMENT '专业名称',
  `code`        VARCHAR(50)  DEFAULT NULL COMMENT '专业编码',
  `sort`        INT          NOT NULL DEFAULT 0,
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_college` (`college_id`)
) ENGINE=InnoDB COMMENT='专业字典';

/* ---------- 3. 系统用户（管理员账号） ---------- */
CREATE TABLE `sys_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt密码',
  `real_name`   VARCHAR(50)  NOT NULL COMMENT '真实姓名',
  `role`        VARCHAR(20)  NOT NULL COMMENT '角色:college/academic/admin',
  `college_id`  BIGINT       DEFAULT NULL COMMENT '所属学院(学院管理员)',
  `phone`       VARCHAR(20)  DEFAULT NULL,
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `last_login`  DATETIME     DEFAULT NULL,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_college` (`college_id`)
) ENGINE=InnoDB COMMENT='系统用户';

/* ---------- 4. 校友/学生主表 ---------- */
CREATE TABLE `alumni` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `student_no`   VARCHAR(50)  NOT NULL COMMENT '学号',
  `name`         VARCHAR(50)  NOT NULL COMMENT '姓名',
  `gender`       TINYINT      DEFAULT NULL COMMENT '1男 2女',
  `college_id`   BIGINT       NOT NULL COMMENT '学院',
  `major_id`     BIGINT       DEFAULT NULL COMMENT '专业',
  `enroll_year`  INT          DEFAULT NULL COMMENT '入学年份',
  `grad_year`    INT          DEFAULT NULL COMMENT '毕业年份',
  `identity`     TINYINT      NOT NULL DEFAULT 1 COMMENT '1在校生 2校友',
  `avatar`       VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `summary`      VARCHAR(500) DEFAULT NULL COMMENT '简介/一句话标签',
  `face_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '人脸录入:0未录 1已录',
  `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0停用',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_college` (`college_id`),
  KEY `idx_grad_year` (`grad_year`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB COMMENT='校友/学生主表';

/* ---------- 5. 资料分类 ---------- */
CREATE TABLE `archive_category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父级,0为根',
  `name`        VARCHAR(50)  NOT NULL COMMENT '分类名:荣誉/作品/成绩等',
  `icon`        VARCHAR(255) DEFAULT NULL,
  `sort`        INT          NOT NULL DEFAULT 0,
  `status`      TINYINT      NOT NULL DEFAULT 1,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB COMMENT='资料分类';

/* ---------- 6. 资料档案（核心业务表） ---------- */
CREATE TABLE `archive` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `alumni_id`     BIGINT       NOT NULL COMMENT '归属校友',
  `category_id`   BIGINT       NOT NULL COMMENT '分类',
  `title`         VARCHAR(200) NOT NULL COMMENT '标题',
  `content`       TEXT         DEFAULT NULL COMMENT '正文描述',
  `event_date`    DATE         DEFAULT NULL COMMENT '事件时间(用于成长轨迹排序)',
  `college_id`    BIGINT       NOT NULL COMMENT '冗余学院,便于数据权限',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'draft'
                  COMMENT 'draft/pending_college/pending_academic/approved/rejected/published/unpublished',
  `is_top`        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `is_recommend`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否推荐(进默认轮播池)',
  `display_sort`  INT          NOT NULL DEFAULT 0 COMMENT '展示排序',
  `submit_user`   BIGINT       DEFAULT NULL COMMENT '提交人',
  `submit_time`   DATETIME     DEFAULT NULL,
  `publish_time`  DATETIME     DEFAULT NULL COMMENT '上架时间',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alumni` (`alumni_id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_college` (`college_id`),
  KEY `idx_event_date` (`event_date`)
) ENGINE=InnoDB COMMENT='资料档案';

/* ---------- 7. 媒体资源 ---------- */
CREATE TABLE `archive_media` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `archive_id`  BIGINT       NOT NULL COMMENT '所属档案',
  `type`        TINYINT      NOT NULL COMMENT '1图片 2视频 3文档',
  `url`         VARCHAR(500) NOT NULL COMMENT '资源地址',
  `thumbnail`   VARCHAR(500) DEFAULT NULL COMMENT '缩略图(视频/图片)',
  `file_name`   VARCHAR(255) DEFAULT NULL,
  `file_size`   BIGINT       DEFAULT NULL COMMENT '字节',
  `duration`    INT          DEFAULT NULL COMMENT '视频时长(秒)',
  `sort`        INT          NOT NULL DEFAULT 0,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_archive` (`archive_id`)
) ENGINE=InnoDB COMMENT='档案媒体资源';

/* ---------- 8. 审核记录 ---------- */
CREATE TABLE `audit_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `archive_id`   BIGINT       NOT NULL COMMENT '档案',
  `node`         VARCHAR(20)  NOT NULL COMMENT '环节:college/academic',
  `action`       VARCHAR(20)  NOT NULL COMMENT 'approve/reject',
  `opinion`      VARCHAR(500) DEFAULT NULL COMMENT '审核意见(驳回必填)',
  `auditor_id`   BIGINT       NOT NULL COMMENT '审核人',
  `auditor_name` VARCHAR(50)  DEFAULT NULL,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_archive` (`archive_id`)
) ENGINE=InnoDB COMMENT='审核记录';

/* ---------- 9. 人脸特征 ---------- */
CREATE TABLE `face_feature` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `alumni_id`   BIGINT       NOT NULL COMMENT '校友',
  `feature`     BLOB         NOT NULL COMMENT 'Arcface特征向量(加密存储)',
  `model_ver`   VARCHAR(20)  DEFAULT NULL COMMENT '模型版本',
  `quality`     FLOAT        DEFAULT NULL COMMENT '采集质量分',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1有效 0失效',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alumni` (`alumni_id`)
) ENGINE=InnoDB COMMENT='人脸特征库';

/* ---------- 10. 人脸识别日志 ---------- */
CREATE TABLE `face_recog_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `alumni_id`   BIGINT       DEFAULT NULL COMMENT '命中校友,null为未命中',
  `score`       FLOAT        DEFAULT NULL COMMENT '相似度',
  `hit`         TINYINT      NOT NULL DEFAULT 0 COMMENT '1命中 0未命中',
  `device`      VARCHAR(50)  DEFAULT NULL COMMENT '一体机标识',
  `cost_ms`     INT          DEFAULT NULL COMMENT '耗时毫秒',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_alumni` (`alumni_id`),
  KEY `idx_create` (`create_time`)
) ENGINE=InnoDB COMMENT='人脸识别日志';

/* ---------- 11. 轮播配置 ---------- */
CREATE TABLE `screen_carousel` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `name`          VARCHAR(100) NOT NULL COMMENT '轮播方案名',
  `interval_sec`  INT          NOT NULL DEFAULT 8 COMMENT '单页停留秒数',
  `effect`        VARCHAR(20)  NOT NULL DEFAULT 'fade' COMMENT '动效:fade/slide/zoom',
  `order_type`    VARCHAR(20)  NOT NULL DEFAULT 'sort' COMMENT '顺序:sort/random/time',
  `is_default`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认方案',
  `status`        TINYINT      NOT NULL DEFAULT 1,
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='大屏轮播配置';

/* ---------- 12. 轮播项（轮播池内容） ---------- */
CREATE TABLE `screen_carousel_item` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT,
  `carousel_id`  BIGINT   NOT NULL COMMENT '轮播方案',
  `archive_id`   BIGINT   DEFAULT NULL COMMENT '关联档案',
  `alumni_id`    BIGINT   DEFAULT NULL COMMENT '关联校友(整人轮播)',
  `sort`         INT      NOT NULL DEFAULT 0,
  `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_carousel` (`carousel_id`)
) ENGINE=InnoDB COMMENT='轮播项';

/* ---------- 13. 操作日志 ---------- */
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

/* ---------- 14. 登录日志 ---------- */
CREATE TABLE `sys_login_log` (
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
