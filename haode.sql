-- MySQL dump 10.13  Distrib 9.3.0, for macos14.7 (arm64)
--
-- Host: 127.0.0.1    Database: haode
-- ------------------------------------------------------
-- Server version	9.3.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `haode`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `haode` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `haode`;

--
-- Table structure for table `after_sales`
--

DROP TABLE IF EXISTS `after_sales`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `after_sales` (
  `id` binary(16) NOT NULL,
  `order_id` binary(16) NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_after_sales_order` (`order_id`),
  KEY `idx_after_sales_user_created` (`user_id`,`created_at`),
  CONSTRAINT `fk_after_sales_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `after_sales`
--

LOCK TABLES `after_sales` WRITE;
/*!40000 ALTER TABLE `after_sales` DISABLE KEYS */;
/*!40000 ALTER TABLE `after_sales` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `announcement_reads`
--

DROP TABLE IF EXISTS `announcement_reads`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement_reads` (
  `announcement_id` binary(16) NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `read_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`announcement_id`,`user_id`),
  KEY `idx_announcement_reads_user` (`user_id`),
  CONSTRAINT `fk_announcement_reads_announcement` FOREIGN KEY (`announcement_id`) REFERENCES `announcements` (`id`),
  CONSTRAINT `fk_announcement_reads_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcement_reads`
--

LOCK TABLES `announcement_reads` WRITE;
/*!40000 ALTER TABLE `announcement_reads` DISABLE KEYS */;
INSERT INTO `announcement_reads` VALUES (_binary '3333333333333333','7d65f42d-3988-4c56-a783-58febdecfdbc','2026-10-08 01:38:00.384943');
/*!40000 ALTER TABLE `announcement_reads` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `announcements`
--

DROP TABLE IF EXISTS `announcements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcements` (
  `id` binary(16) NOT NULL,
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(4000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `published` tinyint(1) NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_announcements_published_created` (`published`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcements`
--

LOCK TABLES `announcements` WRITE;
/*!40000 ALTER TABLE `announcements` DISABLE KEYS */;
INSERT INTO `announcements` VALUES (_binary '3333333333333333','‰ºòÊÉ†Âà∏Ê¥ªÂä®','Âç≥Êó•Ëµ∑ÂºÄÊîæÈÇÄËØ∑ÊúâÁ§ºÔºö\n1. Âú®„ÄåÊàëÁöÑ‰ºòÊÉ†Âà∏„ÄçÂ°´ÂÜôÂ•ΩÂèãÈÇÄËØ∑Á†ÅÔºåÂèåÊñπÂêÑÂæó‰∏ÄÂº†ÈÇÄËØ∑Á§ºÂà∏Ôºà‰ºòÊÉ†Âà∏1Ôºâ„ÄÇÊØè‰∏™Ë¥¶Âè∑Âè™ËÉΩÁªëÂÆö‰∏ÄÊ¨°‰∏äÁ∫ß„ÄÇ\n2. ÊØèÊàêÂäüÈÇÄËØ∑‰∏Ä‰ΩçÂ•ΩÂèãÔºåÈÇÄËØ∑‰∫∫ÈÉΩ‰ºöÂÜçÂæó‰∏ÄÂº†‰ºòÊÉ†Âà∏1„ÄÇ\n3. Ë¢´ÈÇÄËØ∑‰∫∫Á¥ØËÆ°ÂÆû‰ªòËææÂà∞ÂêéÂè∞ËÆæÂÆöÈáëÈ¢ùÂêéÔºåÈÇÄËØ∑‰∫∫ÂÜçÂæó‰∏ÄÂº†Ê∂àË¥πÂ•ñÂä±Âà∏Ôºà‰ºòÊÉ†Âà∏2Ôºâ„ÄÇ\n4. ÈÇÄËØ∑Âè™ËÆ°Áõ¥Â±û‰∏ÄÂ±ÇÔºåÂ•ΩÂèãÂÜçÈÇÄËØ∑ÁöÑ‰∫∫‰∏çÁÆóÂú®‰Ω†Âêç‰∏ã„ÄÇ\n5. ‰∏ãÂçïÊó∂ÊØèÁ¨îÁªìÁÆóÊúÄÂ§ö‰ΩøÁî®‰∏ÄÂº†‰ºòÊÉ†Âà∏ÔºõÊª°0ÂáèX‰∏∫Êó†Èó®ÊßõÂà∏„ÄÇ\n\nÂÖ∑‰ΩìÊª°ÂáèÈáëÈ¢ù‰ª•‰ºòÊÉ†Âà∏È°µÂíåÂêéÂè∞ÂΩìÂâçËÆæÁΩÆ‰∏∫ÂáÜ„ÄÇ',1,'2026-10-08 09:36:45.774630','2026-10-08 09:36:45.774630');
/*!40000 ALTER TABLE `announcements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `checkouts`
--

DROP TABLE IF EXISTS `checkouts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `checkouts` (
  `id` binary(16) NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  `goods_amount` decimal(12,2) NOT NULL,
  `discount_amount` decimal(12,2) NOT NULL,
  `coupon_id` binary(16) DEFAULT NULL,
  `address_id` binary(16) DEFAULT NULL,
  `receiver_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receiver_phone` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receiver_province` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receiver_city` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receiver_district` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `receiver_detail` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_checkouts_user_created` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `checkouts`
--

LOCK TABLES `checkouts` WRITE;
/*!40000 ALTER TABLE `checkouts` DISABLE KEYS */;
INSERT INTO `checkouts` VALUES (_binary 'cD3E\Ùçç\r\Íê\ÀJ$','7c78feb2-1b99-435f-9bb8-0936807b26a3',399.00,'CANCELLED','2026-09-21 00:13:33.969265','2026-09-21 01:19:42.230217',399.00,0.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(_binary 'y¢;ŒÄC\‰µ˙Å…≥ñâ:','7c78feb2-1b99-435f-9bb8-0936807b26a3',399.00,'CANCELLED','2026-09-23 06:42:24.791494','2026-09-23 06:47:54.035454',399.00,0.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(_binary '£°1˚c©I)á^G\˜kkÖ\–','7d65f42d-3988-4c56-a783-58febdecfdbc',399.00,'CANCELLED','2026-10-07 23:20:45.784369','2026-10-07 23:20:50.434703',399.00,0.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(_binary '\Ózj\œ\Ó\ÿOYù˛k\∆√ë|¨','7d65f42d-3988-4c56-a783-58febdecfdbc',399.00,'CANCELLED','2026-10-08 01:40:32.735143','2026-10-08 01:40:37.112534',399.00,0.00,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `checkouts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `coupon_policies`
--

DROP TABLE IF EXISTS `coupon_policies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_policies` (
  `id` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL,
  `invite_template_id` binary(16) DEFAULT NULL,
  `purchase_template_id` binary(16) DEFAULT NULL,
  `purchase_threshold` decimal(12,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_policy_invite_template` (`invite_template_id`),
  KEY `fk_policy_purchase_template` (`purchase_template_id`),
  CONSTRAINT `fk_policy_invite_template` FOREIGN KEY (`invite_template_id`) REFERENCES `coupon_templates` (`id`),
  CONSTRAINT `fk_policy_purchase_template` FOREIGN KEY (`purchase_template_id`) REFERENCES `coupon_templates` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupon_policies`
--

LOCK TABLES `coupon_policies` WRITE;
/*!40000 ALTER TABLE `coupon_policies` DISABLE KEYS */;
INSERT INTO `coupon_policies` VALUES ('default',1,_binary '',_binary '\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"',199.87);
/*!40000 ALTER TABLE `coupon_policies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `coupon_templates`
--

DROP TABLE IF EXISTS `coupon_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupon_templates` (
  `id` binary(16) NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `threshold_amount` decimal(12,2) NOT NULL,
  `discount_amount` decimal(12,2) NOT NULL,
  `active` tinyint(1) NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  `per_order_limit` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupon_templates`
--

LOCK TABLES `coupon_templates` WRITE;
/*!40000 ALTER TABLE `coupon_templates` DISABLE KEYS */;
INSERT INTO `coupon_templates` VALUES (_binary '','ÈÇÄËØ∑Á§ºÂà∏',0.00,10.00,1,'2026-10-08 09:36:45.708012','2026-10-08 01:37:23.359126',20),(_binary '\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"\"','Ê∂àË¥πÂ•ñÂä±Âà∏',0.00,20.00,1,'2026-10-08 09:36:45.708012','2026-10-08 09:36:45.708012',1),(_binary '\√{¢¨\ZUGü©52\"Ø\‹','Êãâ‰∫∫Á§ºÂç∑',0.00,5.00,1,'2026-10-08 01:36:54.195563','2026-10-08 01:36:54.195614',40);
/*!40000 ALTER TABLE `coupon_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','orders and payments','SQL','V1__orders_and_payments.sql',-1167254799,'root','2026-09-21 08:00:39',120,1),(2,'2','products users and order snapshots','SQL','V2__products_users_and_order_snapshots.sql',-263469388,'root','2026-09-21 08:00:39',119,1),(3,'3','initial products','SQL','V3__initial_products.sql',730339178,'root','2026-09-21 08:00:39',129,1),(4,'4','align session token type','SQL','V4__align_session_token_type.sql',-63775917,'root','2026-09-21 10:34:52',116,1),(5,'5','add user phone','SQL','V5__add_user_phone.sql',2050968959,'root','2026-09-21 11:08:02',220,1),(6,'6','add product image','SQL','V6__add_product_image.sql',-79492151,'root','2026-09-22 09:25:51',362,1),(7,'7','add user avatar','SQL','V7__add_user_avatar.sql',-404102919,'root','2026-09-22 13:01:27',730,1),(8,'8','add reviews and after sales','SQL','V8__add_reviews_and_after_sales.sql',1777434709,'root','2026-09-23 14:27:55',1837,1),(9,'9','align review rating type','SQL','V9__align_review_rating_type.sql',350282491,'root','2026-09-23 14:40:50',907,1),(10,'10','add checkout aggregation','SQL','V10__add_checkout_aggregation.sql',-1058116520,'root','2026-09-23 16:20:25',211,1),(11,'11','add media and review moderation','SQL','V11__add_media_and_review_moderation.sql',-506926396,'root','2026-09-23 17:20:31',165,1),(12,'12','add media owner','SQL','V12__add_media_owner.sql',1109772321,'root','2026-09-23 17:20:31',19,1),(13,'13','product zone and homepage','SQL','V13__product_zone_and_homepage.sql',-1624990177,'root','2026-10-08 05:48:08',411,1),(14,'14','product types','SQL','V14__product_types.sql',595079507,'root','2026-10-08 08:10:24',278,1),(15,'15','support center','SQL','V15__support_center.sql',-1209122083,'root','2026-10-08 08:41:35',257,1),(16,'16','coupons and announcements','SQL','V16__coupons_and_announcements.sql',108491074,'root','2026-10-08 09:36:45',393,1),(17,'17','coupon per order limit','SQL','V17__coupon_per_order_limit.sql',1220358323,'root','2026-10-08 09:36:45',45,1),(18,'18','addresses and checkout receiver','SQL','V18__addresses_and_checkout_receiver.sql',-1054400602,'root','2026-10-08 09:54:55',114,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `invite_purchase_rewards`
--

DROP TABLE IF EXISTS `invite_purchase_rewards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invite_purchase_rewards` (
  `invitee_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `inviter_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `coupon_id` binary(16) DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`invitee_id`),
  KEY `idx_invite_rewards_inviter` (`inviter_id`),
  CONSTRAINT `fk_invite_rewards_invitee` FOREIGN KEY (`invitee_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_invite_rewards_inviter` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `invite_purchase_rewards`
--

LOCK TABLES `invite_purchase_rewards` WRITE;
/*!40000 ALTER TABLE `invite_purchase_rewards` DISABLE KEYS */;
/*!40000 ALTER TABLE `invite_purchase_rewards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `media_assets`
--

DROP TABLE IF EXISTS `media_assets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_assets` (
  `id` binary(16) NOT NULL,
  `storage_path` varchar(1024) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `owner_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_media_assets_owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `media_assets`
--

LOCK TABLES `media_assets` WRITE;
/*!40000 ALTER TABLE `media_assets` DISABLE KEYS */;
INSERT INTO `media_assets` VALUES (_binary '\ÏawõG\‚£hüç\‘T´','ad04ebb9-8db2-4901-9457-4a3a14794d9f.jpg','image/jpeg','ÂÖ∞Ëä±Â∫è.jpg','2026-10-08 00:12:20.733811',NULL),(_binary '\–+÷°´O∂ïÅ|ôcû','577e3f92-ddaa-4bcf-b56e-89aef4426e2c.jpg','image/jpeg','ÊöñÂíñ.jpg','2026-10-08 00:15:03.250994',NULL),(_binary '\Z5p6\»A\ıß®\Úpt£ñ','d936606a-1afa-4364-9571-982ee437921e.jpg','image/jpeg','ec95b4f2958ce6a603357b2b7a309d2e.jpg','2026-10-07 23:17:35.327822',NULL),(_binary 'Kxñ\‘\0\0Hëâ™V˝Zn','2bd05260-1974-463e-a75c-fd1c8fd99726.jpg','image/jpeg','cba6ebb9f7beef05d40ebf747a50b94c.jpg','2026-10-07 23:17:54.602448',NULL),(_binary 'vk“ÇŒñI’Æw ]r≥gô','5d676319-ebdb-476f-beb6-0fdc43a3e64a.jpg','image/jpeg','Á±≥ÁôΩ.jpg','2026-10-08 00:19:35.398593',NULL),(_binary 'ìÅC\ÿ\ÎJàπk\«ˇ§\ÏI','475e28f4-9e70-43b2-9196-0e502c865819.jpg','image/jpeg','Ë±ÜÊ≤ôÁ¥´.jpg','2026-10-08 00:10:27.978660',NULL),(_binary 'ìØZßµ∂F˚ºΩç\ı\Ã\◊•','48e02fb1-af01-4b0a-930d-3125a888d4a0.jpg','image/jpeg','Á≤â.jpg','2026-10-08 00:15:32.158287',NULL),(_binary '˚\€\’\Â/A#∏àÆ¶éh\¬','89e2fdfe-3e5b-4e37-9597-2e0f27e802e3.jpg','image/jpeg','1ef6746b939887062f642ff9d7795d46.jpg','2026-10-07 23:17:46.696627',NULL);
/*!40000 ALTER TABLE `media_assets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` binary(16) NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  `product_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_type_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `product_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_type_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `product_subtitle` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `checkout_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_orders_user_created` (`user_id`,`created_at`),
  KEY `idx_orders_product` (`product_id`),
  KEY `idx_orders_checkout` (`checkout_id`),
  CONSTRAINT `fk_orders_checkout` FOREIGN KEY (`checkout_id`) REFERENCES `checkouts` (`id`),
  CONSTRAINT `fk_orders_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (_binary 'cD3E\Ùçç\r\Íê\ÀJ$','7c78feb2-1b99-435f-9bb8-0936807b26a3',399.00,'CANCELLED','2026-09-21 00:13:33.969265','2026-09-21 01:19:42.230217','bamboo-spring',NULL,'Êò•Êó•Á´πËØ≠Âõõ‰ª∂Â•ó',NULL,'Â§©‰∏ùÁ´πÁ∫§Áª¥ ¬∑ 1.5Á±≥Â∫ä',1,_binary 'cD3E\Ùçç\r\Íê\ÀJ$'),(_binary 'y¢;ŒÄC\‰µ˙Å…≥ñâ:','7c78feb2-1b99-435f-9bb8-0936807b26a3',399.00,'CANCELLED','2026-09-23 06:42:24.791494','2026-09-23 06:47:54.035454','bamboo-spring',NULL,'Êò•Êó•Á´πËØ≠Âõõ‰ª∂Â•ó',NULL,'Â§©‰∏ùÁ´πÁ∫§Áª¥ ¬∑ 1.5Á±≥Â∫ä',1,_binary 'y¢;ŒÄC\‰µ˙Å…≥ñâ:'),(_binary 'πâùí\⁄6Kπ\Û´u\Àk[l','7d65f42d-3988-4c56-a783-58febdecfdbc',399.00,'CANCELLED','2026-10-08 01:40:32.741699','2026-10-08 01:40:37.125101','bamboo-spring','d9708ec9-5bab-4cf2-842c-ba31060b5050','Êò•Êó•Á´πËØ≠Âõõ‰ª∂Â•ó','ÊöñÂíñ','Â§©‰∏ùÁ´πÁ∫§Áª¥ ¬∑ 1.5Á±≥Â∫ä',1,_binary '\Ózj\œ\Ó\ÿOYù˛k\∆√ë|¨'),(_binary '¬Ç^nçOøùô¯∑\ƒ˙','7d65f42d-3988-4c56-a783-58febdecfdbc',399.00,'CANCELLED','2026-10-08 00:45:07.757118','2026-10-08 00:50:38.228936','bamboo-spring','d9708ec9-5bab-4cf2-842c-ba31060b5050','Êò•Êó•Á´πËØ≠Âõõ‰ª∂Â•ó','ÊöñÂíñ','Â§©‰∏ùÁ´πÁ∫§Áª¥ ¬∑ 1.5Á±≥Â∫ä',1,NULL),(_binary '˙\Œn\ 2<HH°c\”cP±\ˆÜ','7d65f42d-3988-4c56-a783-58febdecfdbc',399.00,'CANCELLED','2026-10-07 23:20:45.797138','2026-10-07 23:20:50.443985','bamboo-spring',NULL,'Êò•Êó•Á´πËØ≠Âõõ‰ª∂Â•ó',NULL,'Â§©‰∏ùÁ´πÁ∫§Áª¥ ¬∑ 1.5Á±≥Â∫ä',1,_binary '£°1˚c©I)á^G\˜kkÖ\–');
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payments`
--

DROP TABLE IF EXISTS `payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payments` (
  `id` binary(16) NOT NULL,
  `order_id` binary(16) DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `transaction_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  `checkout_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payments_transaction` (`transaction_id`),
  UNIQUE KEY `uk_payments_checkout` (`checkout_id`),
  KEY `fk_payments_order` (`order_id`),
  CONSTRAINT `fk_payments_checkout` FOREIGN KEY (`checkout_id`) REFERENCES `checkouts` (`id`),
  CONSTRAINT `fk_payments_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payments`
--

LOCK TABLES `payments` WRITE;
/*!40000 ALTER TABLE `payments` DISABLE KEYS */;
INSERT INTO `payments` VALUES (_binary '\Z\’$∞±H\Ï∏ƒïLC\‰\’\Ú',_binary 'y¢;ŒÄC\‰µ˙Å…≥ñâ:',399.00,'CLOSED',NULL,'2026-09-23 06:42:24.802170','2026-09-23 06:47:54.076823',_binary 'y¢;ŒÄC\‰µ˙Å…≥ñâ:'),(_binary '\Ì\‰ûEJMµ\«}∞x÷ú',NULL,399.00,'CLOSED',NULL,'2026-10-08 01:40:32.744226','2026-10-08 01:40:37.122172',_binary '\Ózj\œ\Ó\ÿOYù˛k\∆√ë|¨'),(_binary '5ß‘π!Bhù‹êr\≈æs',_binary 'cD3E\Ùçç\r\Íê\ÀJ$',399.00,'CLOSED',NULL,'2026-09-21 00:13:33.970821','2026-09-21 01:19:42.578194',_binary 'cD3E\Ùçç\r\Íê\ÀJ$'),(_binary '=L\‚\Z\ÊCˇπ\Ê∫ µ\‰r',NULL,399.00,'CLOSED',NULL,'2026-10-07 23:20:45.798414','2026-10-07 23:20:50.440921',_binary '£°1˚c©I)á^G\˜kkÖ\–'),(_binary 'i¥6\ÃEÅû\ÃÉ˚\Ù<',_binary '¬Ç^nçOøùô¯∑\ƒ˙',399.00,'CLOSED',NULL,'2026-10-08 00:45:07.757995','2026-10-08 00:50:38.244900',NULL);
/*!40000 ALTER TABLE `payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `product_types`
--

DROP TABLE IF EXISTS `product_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_types` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `intro` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_url` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_asset_id` binary(16) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_product_types_product` (`product_id`,`sort_order`,`id`),
  KEY `fk_product_types_image_asset` (`image_asset_id`),
  CONSTRAINT `fk_product_types_image_asset` FOREIGN KEY (`image_asset_id`) REFERENCES `media_assets` (`id`),
  CONSTRAINT `fk_product_types_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_types`
--

LOCK TABLES `product_types` WRITE;
/*!40000 ALTER TABLE `product_types` DISABLE KEYS */;
INSERT INTO `product_types` VALUES ('33d7f76f-f582-403c-8ed9-f3d2da2ef9ab-default','33d7f76f-f582-403c-8ed9-f3d2da2ef9ab','ÈªòËÆ§Ê¨æ','Â•Ω','',NULL,0,1),('8a909ad4-8ed0-493d-b613-d74db6029f13','bamboo-spring','ÂÖ∞Ëä±Â∫è','ÊµãËØï',NULL,_binary '\ÏawõG\‚£hüç\‘T´',2,1),('cotton-cloud-default','cotton-cloud','ÈªòËÆ§Ê¨æ','Êñ∞ÁñÜÈïøÁªíÊ£â ¬∑ ‰∫≤ËÇ§Ê¨æ',NULL,_binary '˚\€\’\Â/A#∏àÆ¶éh\¬',0,1),('d9708ec9-5bab-4cf2-842c-ba31060b5050','bamboo-spring','ÊöñÂíñ','',NULL,_binary '\–+÷°´O∂ïÅ|ôcû',1,1),('linen-moon-default','linen-moon','ÈªòËÆ§Ê¨æ','Ê∞¥Ê¥ó‰∫öÈ∫ª ¬∑ Á∫ØËâ≤Ê¨æ',NULL,_binary 'Kxñ\‘\0\0Hëâ™V˝Zn',0,1);
/*!40000 ALTER TABLE `product_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subtitle` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `original_price` decimal(12,2) DEFAULT NULL,
  `color` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `accent` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pattern` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tag` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `zone` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Â∫äÂìÅÂå∫',
  `homepage` tinyint(1) NOT NULL DEFAULT '0',
  `sort_order` int NOT NULL DEFAULT '0',
  `active` tinyint(1) NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `image_url` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_asset_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_products_active_id` (`active`,`id`),
  KEY `fk_products_image_asset` (`image_asset_id`),
  CONSTRAINT `fk_products_image_asset` FOREIGN KEY (`image_asset_id`) REFERENCES `media_assets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES ('33d7f76f-f582-403c-8ed9-f3d2da2ef9ab','ÈôàÊµ©ÁÑ∂','Â•Ω',100.00,200.00,'','','Â•Ω','','Â∫äÂìÅÂå∫',0,0,0,'2026-10-01 14:39:55.833581','2026-10-08 06:50:22.864751','',NULL),('bamboo-spring','Êò•Êó•Á´πËØ≠Âõõ‰ª∂Â•ó','Â§©‰∏ùÁ´πÁ∫§Áª¥ ¬∑ 1.5Á±≥Â∫ä',399.00,498.62,'#d9e5d8','#6d896e','Á´π','ËΩªÊüîÈÄèÊ∞î','Â∫äÂìÅÂå∫',1,0,1,'2026-09-21 08:00:39.182808','2026-10-08 07:55:11.475420','/api/media/1a357036-18c8-41f5-a708-a8f27074a396',_binary '\Z5p6\»A\ıß®\Úpt£ñ'),('cotton-cloud','‰∫ëÊúµÂÖ®Ê£âË¢´Â•ó','Êñ∞ÁñÜÈïøÁªíÊ£â ¬∑ ‰∫≤ËÇ§Ê¨æ',329.00,399.00,'#dce8ed','#668b9a','‰∫ë','ÊüîËΩØ‰∫≤ËÇ§','Â∫äÂìÅÂå∫',1,0,1,'2026-09-21 08:00:39.182808','2026-10-08 07:17:46.705200',NULL,_binary '˚\€\’\Â/A#∏àÆ¶éh\¬'),('linen-moon','ÊúàÁôΩ‰∫öÈ∫ªÂ∫äÁ¨†','Ê∞¥Ê¥ó‰∫öÈ∫ª ¬∑ Á∫ØËâ≤Ê¨æ',269.00,329.00,'#e5ddd2','#9b8871','Êúà','Ëá™ÁÑ∂ËÇåÁêÜ','Â∫äÂìÅÂå∫',1,0,1,'2026-09-21 08:00:39.182808','2026-10-08 07:17:54.609186',NULL,_binary 'Kxñ\‘\0\0Hëâ™V˝Zn');
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review_images`
--

DROP TABLE IF EXISTS `review_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_images` (
  `id` binary(16) NOT NULL,
  `review_id` binary(16) NOT NULL,
  `asset_id` binary(16) NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_images_asset` (`review_id`,`asset_id`),
  KEY `idx_review_images_review_sort` (`review_id`,`sort_order`),
  KEY `fk_review_images_asset` (`asset_id`),
  CONSTRAINT `fk_review_images_asset` FOREIGN KEY (`asset_id`) REFERENCES `media_assets` (`id`),
  CONSTRAINT `fk_review_images_review` FOREIGN KEY (`review_id`) REFERENCES `reviews` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review_images`
--

LOCK TABLES `review_images` WRITE;
/*!40000 ALTER TABLE `review_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `review_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `id` binary(16) NOT NULL,
  `order_id` binary(16) NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rating` int NOT NULL,
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING_REVIEW',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reviews_order` (`order_id`),
  KEY `idx_reviews_user_created` (`user_id`,`created_at`),
  KEY `idx_reviews_status_created` (`status`,`created_at`),
  CONSTRAINT `fk_reviews_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reviews`
--

LOCK TABLES `reviews` WRITE;
/*!40000 ALTER TABLE `reviews` DISABLE KEYS */;
/*!40000 ALTER TABLE `reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sessions`
--

DROP TABLE IF EXISTS `sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sessions` (
  `token_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` timestamp(6) NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`token_hash`),
  KEY `idx_sessions_user` (`user_id`),
  KEY `idx_sessions_expiry` (`expires_at`),
  CONSTRAINT `fk_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sessions`
--

LOCK TABLES `sessions` WRITE;
/*!40000 ALTER TABLE `sessions` DISABLE KEYS */;
INSERT INTO `sessions` VALUES ('1e81dd5b34cdb866ac157de5cef0c522d0dc883ea77aa24a4dc03240438185c5','7c78feb2-1b99-435f-9bb8-0936807b26a3','2026-09-28 00:25:38.967733','2026-09-21 00:25:38.967744'),('4cc9ef6242c8c0459d30280601b049a5ca522ae5384e548e898c4c1a206b0d2a','7d65f42d-3988-4c56-a783-58febdecfdbc','2026-10-08 06:35:22.452107','2026-10-01 06:35:22.452128'),('57d53424943ec5d546d06b40d5332af4b597ba6e0c7902093b2259bd076a7c0c','7d65f42d-3988-4c56-a783-58febdecfdbc','2026-10-08 06:34:27.808130','2026-10-01 06:34:27.808165'),('8cd7d89dc47b51df37af29ffa55d14987052aadb5608750be17cff3175bca02e','7c78feb2-1b99-435f-9bb8-0936807b26a3','2026-09-28 00:12:57.931470','2026-09-21 00:12:57.931497'),('8e631b4a13c5df71d73ef16fe73d21d8c2bbc266b4514fc57df7a8af9d6e3931','7c78feb2-1b99-435f-9bb8-0936807b26a3','2026-09-28 00:25:15.374685','2026-09-21 00:25:15.374691'),('a7885658995e4a86828ea594cafeebba78f1102a710005186ec4bc1aaf84ef1b','7c78feb2-1b99-435f-9bb8-0936807b26a3','2026-09-28 00:25:12.353818','2026-09-21 00:25:12.353827'),('d3ff63295e2ca94416e7d804a6c3f897fff31255827ab9d522f770a9fe8eb907','7c78feb2-1b99-435f-9bb8-0936807b26a3','2026-09-28 01:42:30.528451','2026-09-21 01:42:30.528717');
/*!40000 ALTER TABLE `sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `support_messages`
--

DROP TABLE IF EXISTS `support_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `support_messages` (
  `id` binary(16) NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sender` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(2000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `read_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_support_user_created` (`user_id`,`created_at`),
  KEY `idx_support_user_unread` (`user_id`,`sender`,`read_at`),
  CONSTRAINT `fk_support_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `support_messages`
--

LOCK TABLES `support_messages` WRITE;
/*!40000 ALTER TABLE `support_messages` DISABLE KEYS */;
INSERT INTO `support_messages` VALUES (_binary '\Ôã\r\»Aûäzíó\\}Ç:','7d65f42d-3988-4c56-a783-58febdecfdbc','USER','hi','2026-10-08 00:42:14.855323','2026-10-08 00:42:17.298841'),(_binary 'i˘\’og_Kÿºz\ÓÉ\ÎU\≈\‘','7d65f42d-3988-4c56-a783-58febdecfdbc','ADMIN','ÂíãÁöÑ‰∫Ü','2026-10-08 03:25:13.174893','2026-10-08 03:25:14.657092'),(_binary 'Æ\œ\Ÿ¡∂KKÆ\Ó+\ÍjX.\«','7d65f42d-3988-4c56-a783-58febdecfdbc','ADMIN','ÊÄé‰πà‰∫ÜÔºü','2026-10-08 00:42:19.397658','2026-10-08 00:42:19.457020'),(_binary '∑¯GÑ˝˚Iøñ¸î/±\\áΩ','7d65f42d-3988-4c56-a783-58febdecfdbc','USER','Â§ßÂÇªÂ≠ê','2026-10-08 03:24:59.920168','2026-10-08 03:25:06.335093');
/*!40000 ALTER TABLE `support_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `support_settings`
--

DROP TABLE IF EXISTS `support_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `support_settings` (
  `id` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `wechat_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `support_settings`
--

LOCK TABLES `support_settings` WRITE;
/*!40000 ALTER TABLE `support_settings` DISABLE KEYS */;
INSERT INTO `support_settings` VALUES ('default','hongridajiafang');
/*!40000 ALTER TABLE `support_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_addresses`
--

DROP TABLE IF EXISTS `user_addresses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_addresses` (
  `id` binary(16) NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `receiver_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `city` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `district` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `detail` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_default` tinyint(1) NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_addresses_user_default` (`user_id`,`is_default`),
  CONSTRAINT `fk_addresses_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_addresses`
--

LOCK TABLES `user_addresses` WRITE;
/*!40000 ALTER TABLE `user_addresses` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_addresses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_coupons`
--

DROP TABLE IF EXISTS `user_coupons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_coupons` (
  `id` binary(16) NOT NULL,
  `user_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_id` binary(16) DEFAULT NULL,
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `threshold_amount` decimal(12,2) NOT NULL,
  `discount_amount` decimal(12,2) NOT NULL,
  `source` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checkout_id` binary(16) DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  `used_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_coupons_user_status` (`user_id`,`status`,`created_at`),
  KEY `idx_user_coupons_checkout` (`checkout_id`),
  KEY `fk_user_coupons_template` (`template_id`),
  CONSTRAINT `fk_user_coupons_template` FOREIGN KEY (`template_id`) REFERENCES `coupon_templates` (`id`),
  CONSTRAINT `fk_user_coupons_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_coupons`
--

LOCK TABLES `user_coupons` WRITE;
/*!40000 ALTER TABLE `user_coupons` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_coupons` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `wechat_openid` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `phone` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `avatar_url` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `invite_code` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `inviter_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_wechat_openid` (`wechat_openid`),
  UNIQUE KEY `uk_users_phone` (`phone`),
  UNIQUE KEY `uk_users_invite_code` (`invite_code`),
  KEY `idx_users_inviter` (`inviter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES ('7c78feb2-1b99-435f-9bb8-0936807b26a3','oZZxO3c6PwUnYkDbRJhvJs7pam5Y','2026-09-21 00:12:57.912945','2026-10-08 09:36:45.532707','legacy-e2ea05901818e4faf0841e850','http://tmp/RvWpXFMowNDF8bf778e3a314cf4ef3a271172ff01ddc.jpeg','7C78FEB2',NULL),('7d65f42d-3988-4c56-a783-58febdecfdbc','dev-local-openid','2026-10-01 06:34:27.769343','2026-10-08 02:07:18.809008','13800000000','http://tmp/TvVUBItTFD0z1e6490e038954543aaebccc8e95d8d2a.jpeg','7D65F42D',NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'haode'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-10 16:31:15
