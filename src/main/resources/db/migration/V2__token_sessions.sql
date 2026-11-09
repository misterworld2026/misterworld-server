-- Additional design: revocation and refresh rotation; no raw refresh tokens stored.
CREATE TABLE `인증세션` (
 `id` VARCHAR(36) PRIMARY KEY,
 `memberId` BIGINT NOT NULL,
 `refreshHash` VARCHAR(64) NOT NULL,
 `expiresAt` TIMESTAMP(6) NOT NULL,
 `revoked` BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY (`memberId`) REFERENCES `회원` (`회원_ID`)
);
CREATE INDEX idx_session_member ON `인증세션` (`memberId`);
