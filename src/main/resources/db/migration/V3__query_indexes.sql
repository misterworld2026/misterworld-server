CREATE INDEX idx_application_member ON `여행신청` (`회원_ID`, `신청일시`);
CREATE INDEX idx_application_schedule ON `여행신청` (`여행일정_ID`, `신청상태`);
CREATE INDEX idx_schedule_product_dates ON `여행일정` (`여행상품_ID`, `출발일`);
CREATE INDEX idx_schedule_status_dates ON `여행일정` (`여행상태`, `출발일`, `종료일`);
CREATE INDEX idx_product_theme ON `여행상품` (`테마`);
CREATE INDEX idx_option_product ON `여행옵션` (`여행상품_ID`);
CREATE INDEX idx_inventory_product ON `물품재고` (`여행상품_ID`);
CREATE INDEX idx_participant_application ON `여행객` (`여행신청_ID`);
CREATE INDEX idx_notification_status ON `문자알림` (`발송상태`);
