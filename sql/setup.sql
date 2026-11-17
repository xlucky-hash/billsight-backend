-- ============================================================
-- 多源账单管理与个人财务分析系统 —— 数据库初始化脚本
-- 适用：MySQL 8.0+   字符集：utf8mb4
-- 用法：mysql -u root -p < sql/setup.sql
-- 说明：脚本会重建 match 库下的 6 张表，请勿在生产库直接执行。
--       种子数据仅包含系统内置规则与贝叶斯初始词频，不含任何用户账单。
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `match` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */;
USE `match`;
DROP TABLE IF EXISTS `bayes_category_stat`;
CREATE TABLE `bayes_category_stat` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称（如：餐饮、交通）',
  `doc_count` int(11) NOT NULL DEFAULT '0' COMMENT '该分类下的账单总条数',
  `total_words` int(11) NOT NULL DEFAULT '0' COMMENT '该分类下的词总数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_name` (`category_name`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='贝叶斯分类统计表';
DROP TABLE IF EXISTS `bayes_word_freq`;
CREATE TABLE `bayes_word_freq` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  `word` varchar(100) NOT NULL COMMENT '特征词',
  `freq_count` int(11) NOT NULL DEFAULT '0' COMMENT '词频（出现次数）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cat_word` (`category_name`,`word`)
) ENGINE=InnoDB AUTO_INCREMENT=169 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='贝叶斯词频统计表';
DROP TABLE IF EXISTS `raw_record`;
CREATE TABLE `raw_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `batch_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '导入批次ID',
  `record_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '记录类型: OCR_TEXT/CSV_ROW/API_JSON',
  `raw_content` text COLLATE utf8mb4_unicode_ci COMMENT '原始文本或原始行数据',
  `raw_json` json DEFAULT NULL COMMENT 'OCR返回的文字块、坐标、置信度等结构化数据',
  `parse_status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '解析状态: SUCCESS/FAILED',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='原始记录表(永久保存)';
DROP TABLE IF EXISTS `sys_rule`;
CREATE TABLE `sys_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `keyword` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商户/摘要关键词',
  `category_id` bigint(20) NOT NULL COMMENT '分类ID',
  `priority` int(11) NOT NULL DEFAULT '0' COMMENT '优先级（越大越优先）',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '软删状态：1启用，0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_enabled_priority` (`enabled`,`priority`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通用规则表';
DROP TABLE IF EXISTS `txn`;
CREATE TABLE `txn` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `batch_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源导入批次ID',
  `account_id` bigint(20) DEFAULT NULL COMMENT '所属账户ID',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '状态: PENDING_REVIEW/CONFIRMED/IGNORED',
  `txn_time` datetime DEFAULT NULL COMMENT '交易时间',
  `amount` decimal(18,2) NOT NULL COMMENT '金额(明文,支持聚合)',
  `direction` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收支方向: INCOME/EXPENSE',
  `counterparty_enc` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '加密对方/商户',
  `summary` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '摘要',
  `txn_type_raw` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原始交易类型文本',
  `category_id` bigint(20) DEFAULT NULL COMMENT '最终分类ID',
  `system_category_id` bigint(20) DEFAULT NULL COMMENT '系统建议分类ID(校对页展示)',
  `classify_source` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类来源: RULE/NB/LLM/MANUAL',
  `confidence` decimal(5,4) DEFAULT NULL COMMENT '置信度0~1,低置信标黄',
  `corrected` tinyint(4) NOT NULL DEFAULT '0' COMMENT '用户是否改过: 0否 1是',
  `remark` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status_time` (`user_id`,`status`,`txn_time`),
  KEY `idx_user_category_time` (`user_id`,`category_id`,`txn_time`),
  KEY `idx_batch_id` (`batch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易主表';
DROP TABLE IF EXISTS `user_rule`;
CREATE TABLE `user_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `feature` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关键词/商户特征',
  `category_id` bigint(20) NOT NULL COMMENT '命中分类ID',
  `hit_count` int(11) NOT NULL DEFAULT '0' COMMENT '累计命中次数',
  `weight` decimal(5,4) NOT NULL DEFAULT '1.0000' COMMENT '规则权重',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '软删状态：1启用，0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_feature` (`user_id`,`feature`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户级规则表';

-- ---------------- 种子数据（系统规则 / 贝叶斯初始词频） ----------------

INSERT INTO `sys_rule` (`id`, `keyword`, `category_id`, `priority`, `enabled`, `created_at`) VALUES (1,'肯德基',1,10,1,'2026-11-11 10:37:46'),(2,'汉堡',1,8,1,'2026-11-11 10:37:46'),(3,'瑞幸',1,10,1,'2026-11-11 10:37:46'),(4,'沙县小吃',1,5,1,'2026-11-11 10:37:46'),(5,'地铁',2,10,1,'2026-11-11 10:37:46'),(6,'公交',2,7,1,'2026-11-11 10:37:46'),(7,'高铁',2,10,1,'2026-11-11 10:37:46'),(8,'12306',2,10,1,'2026-11-11 10:37:46'),(9,'淘宝',3,15,1,'2026-11-11 10:37:46'),(10,'京东',3,10,1,'2026-11-11 10:37:46'),(11,'拼多多',3,10,1,'2026-11-11 10:37:46'),(12,'无效规则',4,10,0,'2026-11-11 10:37:46');
INSERT INTO `bayes_category_stat` (`id`, `category_name`, `doc_count`, `total_words`, `created_at`, `updated_at`) VALUES (1,'餐饮',47,122,'2026-11-11 18:43:03','2026-11-16 16:03:11'),(2,'交通',34,85,'2026-11-11 18:43:03','2026-11-16 16:02:36'),(3,'购物',25,71,'2026-11-11 19:15:18','2026-11-12 14:29:55'),(4,'娱乐',20,56,'2026-11-11 19:15:18','2026-11-12 14:29:57'),(5,'生活缴费',16,46,'2026-11-11 19:16:01','2026-11-13 08:48:47');
INSERT INTO `bayes_word_freq` (`id`, `category_name`, `word`, `freq_count`, `created_at`, `updated_at`) VALUES (1,'餐饮','星巴克',9,'2026-11-11 18:43:03','2026-11-11 19:16:22'),(2,'餐饮','咖啡',6,'2026-11-11 18:43:03','2026-11-11 19:16:22'),(3,'餐饮','拿铁',4,'2026-11-11 18:43:03','2026-11-11 19:16:22'),(4,'交通','滴滴',9,'2026-11-11 18:43:03','2026-11-11 19:16:22'),(5,'交通','打车',7,'2026-11-11 18:43:03','2026-11-11 19:16:22'),(12,'餐饮','汉堡',6,'2026-11-11 18:57:41','2026-11-11 19:16:22'),(13,'餐饮','薯条',1,'2026-11-11 18:57:41','2026-11-11 18:57:41'),(14,'餐饮','炸鸡',4,'2026-11-11 18:57:41','2026-11-11 19:16:22'),(15,'交通','高铁',7,'2026-11-11 18:57:41','2026-11-11 19:16:22'),(16,'交通','火车',3,'2026-11-11 18:57:41','2026-11-11 19:16:22'),(17,'交通','公交',6,'2026-11-11 18:57:41','2026-11-11 19:16:22'),(18,'餐饮','麦当劳',9,'2026-11-11 18:57:41','2026-11-11 19:16:22'),(20,'交通','飞机',1,'2026-11-11 19:10:58','2026-11-11 19:10:58'),(21,'交通','巴士',1,'2026-11-11 19:14:24','2026-11-11 19:14:24'),(22,'餐饮','可乐',1,'2026-11-11 19:15:18','2026-11-11 19:15:18'),(23,'餐饮','肯德基',6,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(24,'餐饮','海底捞',5,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(25,'餐饮','火锅',3,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(26,'交通','快车',3,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(27,'交通','地铁',7,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(28,'交通','12306',5,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(29,'交通','加油',5,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(30,'交通','中石化',3,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(31,'购物','淘宝',8,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(32,'购物','网购',5,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(33,'购物','衣服',4,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(34,'购物','京东',7,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(35,'购物','家电',4,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(36,'购物','手机',3,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(37,'购物','拼多多',5,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(38,'购物','百货',3,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(39,'娱乐','电影',7,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(40,'娱乐','猫眼',5,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(41,'娱乐','爆米花',3,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(42,'娱乐','游戏',6,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(43,'娱乐','充值',5,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(44,'娱乐','KTV',5,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(45,'娱乐','唱歌',3,'2026-11-11 19:15:18','2026-11-11 19:16:22'),(56,'餐饮','瑞幸',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(57,'餐饮','外卖',4,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(58,'餐饮','美团',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(59,'餐饮','饿了么',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(60,'餐饮','沙县小吃',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(61,'餐饮','奶茶',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(62,'餐饮','喜茶',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(63,'餐饮','烧烤',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(64,'餐饮','夜宵',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(65,'餐饮','食堂',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(66,'餐饮','早餐',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(67,'交通','专车',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(68,'交通','出行',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(69,'交通','机票',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(70,'交通','航班',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(71,'交通','中石油',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(72,'交通','停车费',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(73,'交通','高速',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(74,'交通','过路费',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(75,'交通','共享单车',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(76,'交通','哈啰',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(77,'购物','天猫',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(78,'购物','鞋',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(79,'购物','超市',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(80,'购物','便利店',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(81,'购物','数码',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(82,'购物','化妆品',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(83,'购物','护肤',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(84,'购物','山姆',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(85,'购物','沃尔玛',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(86,'购物','京东到家',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(87,'购物','唯品会',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(88,'购物','退货',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(89,'娱乐','淘票票',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(90,'娱乐','王者荣耀',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(91,'娱乐','Steam',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(92,'娱乐','网吧',2,'2026-11-11 19:16:22','2026-11-13 15:44:23'),(93,'娱乐','剧本杀',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(94,'娱乐','密室逃脱',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(95,'娱乐','演唱会',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(96,'娱乐','门票',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(97,'娱乐','旅游',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(98,'娱乐','酒店',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(99,'娱乐','民宿',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(100,'娱乐','景点',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(101,'娱乐','迪士尼',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(102,'生活缴费','电费',4,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(103,'生活缴费','水费',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(104,'生活缴费','燃气',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(105,'生活缴费','物业',4,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(106,'生活缴费','话费',4,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(107,'生活缴费','宽带',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(108,'生活缴费','中国移动',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(109,'生活缴费','中国电信',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(110,'生活缴费','联通',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(111,'生活缴费','房租',4,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(112,'生活缴费','租金',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(113,'生活缴费','水电',3,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(114,'生活缴费','维修',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(115,'生活缴费','保洁',2,'2026-11-11 19:16:22','2026-11-13 08:48:47'),(116,'生活缴费','罚款',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(117,'生活缴费','快递',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(118,'生活缴费','顺丰',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(119,'生活缴费','菜鸟驿站',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(120,'生活缴费','挂号',1,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(121,'生活缴费','买药',2,'2026-11-11 19:16:22','2026-11-11 19:16:22'),(156,'餐饮','海南鸡饭',2,'2026-11-11 19:19:44','2026-11-11 19:21:28'),(158,'娱乐','台球',2,'2026-11-11 19:29:41','2026-11-11 19:29:49'),(160,'购物','抖音',2,'2026-11-11 19:32:29','2026-11-11 19:32:38'),(168,'餐饮','麦当劳(科技园店)',1,'2026-11-16 16:03:11','2026-11-16 16:03:11');
