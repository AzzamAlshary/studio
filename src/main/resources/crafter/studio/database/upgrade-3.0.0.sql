SET GLOBAL innodb_large_prefix = TRUE ;

SET GLOBAL innodb_file_format = 'BARRACUDA' ;

SET GLOBAL innodb_file_format_max = 'BARRACUDA' ;

SET GLOBAL innodb_file_per_table = TRUE ;


use crafter ;
-- Rename tables script
ALTER TABLE `cstudio_activity` RENAME `audit` ;
ALTER TABLE `cstudio_dependency` RENAME `dependency` ;
ALTER TABLE `cstudio_objectstate` RENAME `item_state` ;
ALTER TABLE `cstudio_pagenavigationordersequence` RENAME `navigation_order_sequence` ;
ALTER TABLE `cstudio_copytoenvironment` RENAME `publish_request` ;
ALTER TABLE `cstudio_site` RENAME `site` ;
ALTER TABLE `cstudio_objectmetadata` RENAME `item_metadata` ;
ALTER TABLE `cstudio_user` RENAME `user` ;
ALTER TABLE `cstudio_group` RENAME `group` ;
ALTER TABLE `cstudio_usergroup` RENAME `group_user` ;

ALTER TABLE `dependency` MODIFY COLUMN `type` VARCHAR(50) NOT NULL ;

CREATE TABLE _meta (`version` VARCHAR(10) NOT NULL , PRIMARY KEY (`version`)) ;

CREATE TABLE IF NOT EXISTS gitlog
(
  `id`          BIGINT(20)    NOT NULL AUTO_INCREMENT,
  `site_id`     VARCHAR(50)   NOT NULL,
  `commit_id`   VARCHAR(50)   NOT NULL,
  `processed`   INT           NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE `uq_siteid_commitid` (`site_id`, `commit_id`),
  INDEX `gitlog_site_idx` (`site_id` ASC)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8
  ROW_FORMAT = DYNAMIC ;

ALTER TABLE `site` ADD COLUMN `last_verified_gitlog_commit_id` VARCHAR(50) NULL ;

INSERT INTO _meta (version) VALUES ('3.0.3-SNAPSHOT.1') ;
