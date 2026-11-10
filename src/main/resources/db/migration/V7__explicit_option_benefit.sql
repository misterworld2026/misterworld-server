-- Reviewed user decision: no separate champagne EXTRA for PREMIUM.
-- Preserve existing options; legacy EXTRA rows need explicit benefit classification.
ALTER TABLE `여행옵션` ADD COLUMN `부가혜택코드` VARCHAR(40);
ALTER TABLE `여행옵션` ADD CONSTRAINT chk_option_benefit CHECK (`부가혜택코드` IS NULL OR `부가혜택코드` IN ('CHAMPAGNE','COFFEE','OTHER'));
