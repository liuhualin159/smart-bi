-- ----------------------------
-- Agent BI V2 链接访问次数限制、推送脱敏支撑
-- T047: 链接访问控制、推送脱敏
-- ----------------------------

-- bi_share_link 增加最大访问次数（null 或 0 表示不限制）
ALTER TABLE bi_share_link ADD COLUMN max_access_count bigint(20) DEFAULT NULL COMMENT '最大访问次数，null/0=不限制';
