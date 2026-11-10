-- Mock payment only. Existing records remain unpaid; departure status is unchanged.
ALTER TABLE `여행신청` ADD COLUMN `결제상태` VARCHAR(20) NOT NULL DEFAULT 'UNPAID';
ALTER TABLE `여행신청` ADD COLUMN `결제금액` INT;
ALTER TABLE `여행신청` ADD COLUMN `결제완료일시` DATETIME(6);
ALTER TABLE `여행신청` ADD CONSTRAINT ck_application_payment CHECK (
 (`결제상태` = 'UNPAID' AND `결제금액` IS NULL AND `결제완료일시` IS NULL)
 OR (`결제상태` = 'PAID' AND `결제금액` >= 0 AND `결제금액` IS NOT NULL AND `결제완료일시` IS NOT NULL)
);
