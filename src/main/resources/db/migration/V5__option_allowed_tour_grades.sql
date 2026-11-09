-- Additional design. Preserve existing rows; do not invent cross-grade permissions.
ALTER TABLE `여행옵션` ADD COLUMN `허용투어등급` VARCHAR(255);
-- Defaults remain valid for their own tour grade in application code.
-- Populate cross-grade permissions only after product policy review.
