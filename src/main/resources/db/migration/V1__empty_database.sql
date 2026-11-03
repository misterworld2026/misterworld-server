-- Empty database ONLY. Existing DDL/data must be reviewed before migration.
CREATE TABLE `회원` (
    `회원_ID` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `이메일` VARCHAR(255) NOT NULL,
    `비밀번호` VARCHAR(255) NOT NULL,
    `이름` VARCHAR(255) NOT NULL,
    `주소` VARCHAR(255) NOT NULL,
    `연락처` VARCHAR(255) NOT NULL,
    `Field` INT NOT NULL,
    `회원구분` VARCHAR(40) NOT NULL,
    CHECK (`Field` >= 0),
    CHECK (`회원구분` IN ('MEMBER', 'STAFF')),
    UNIQUE (`이메일`)
);

CREATE TABLE `여행상품` (
    `여행상품_ID` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `상품명` VARCHAR(255) NOT NULL,
    `테마` VARCHAR(255) NOT NULL,
    `상품설명` VARCHAR(2000),
    `클래식_가격` INT,
    `그랜드_가격` INT NOT NULL,
    `프리미엄_가격` INT NOT NULL,
    CHECK (`클래식_가격` >= 0),
    CHECK (`그랜드_가격` >= 0),
    CHECK (`프리미엄_가격` >= 0)
);

CREATE TABLE `여행옵션` (
    `여행옵션_ID` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `여행상품_ID` BIGINT NOT NULL,
    `옵션종류` VARCHAR(255) NOT NULL,
    `옵션명` VARCHAR(255) NOT NULL,
    `옵션등급` VARCHAR(255) NOT NULL,
    `추가금액` INT NOT NULL,
    `옵션설명` VARCHAR(2000),
    `기본등급` VARCHAR(255),
    FOREIGN KEY (`여행상품_ID`) REFERENCES `여행상품` (`여행상품_ID`),
    CHECK (`추가금액` >= 0)
);

CREATE TABLE `여행일정` (
    `여행일정_ID` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `여행상품_ID` BIGINT NOT NULL,
    `출발일` DATE NOT NULL,
    `종료일` DATE NOT NULL,
    `여행상태` VARCHAR(40) NOT NULL,
    `현재신청인원` INT NOT NULL,
    FOREIGN KEY (`여행상품_ID`) REFERENCES `여행상품` (`여행상품_ID`),
    CHECK (`여행상태` IN ('RECRUITING_SCHEDULE', 'CONFIRMED_SCHEDULE', 'CLOSED_SCHEDULE', 'IN_PROGRESS_SCHEDULE', 'COMPLETED_SCHEDULE', 'CANCELLED_SCHEDULE')),
    CHECK (`현재신청인원` >= 0),
    CHECK (`종료일` >= `출발일`)
);

CREATE TABLE `여행신청` (
    `여행신청_ID` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `회원_ID` BIGINT NOT NULL,
    `여행일정_ID` BIGINT NOT NULL,
    `투어등급` VARCHAR(255) NOT NULL,
    `호텔옵션` VARCHAR(255) NOT NULL,
    `교통옵션` VARCHAR(255) NOT NULL,
    `식사옵션` VARCHAR(255) NOT NULL,
    `추가옵션` VARCHAR(2000),
    `신청인원` INT NOT NULL,
    `기본가격` INT NOT NULL,
    `옵션추가금액` INT NOT NULL,
    `할인율` INT NOT NULL,
    `할인금액` INT NOT NULL,
    `최종가격` INT NOT NULL,
    `신청상태` VARCHAR(40) NOT NULL,
    `신청일시` DATETIME(6) NOT NULL,
    FOREIGN KEY (`회원_ID`) REFERENCES `회원` (`회원_ID`),
    FOREIGN KEY (`여행일정_ID`) REFERENCES `여행일정` (`여행일정_ID`),
    CHECK (`신청인원` >= 0),
    CHECK (`기본가격` >= 0),
    CHECK (`옵션추가금액` >= 0),
    CHECK (`할인율` >= 0),
    CHECK (`할인금액` >= 0),
    CHECK (`최종가격` >= 0),
    CHECK (`신청상태` IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    CHECK (`신청인원` > 0)
);

CREATE TABLE `여행객` (
    `여행객_ID` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `여행신청_ID` BIGINT NOT NULL,
    `이름` VARCHAR(255) NOT NULL,
    `연락처` VARCHAR(255) NOT NULL,
    `생년월일` DATE NOT NULL,
    `참가자구분` VARCHAR(40) NOT NULL,
    FOREIGN KEY (`여행신청_ID`) REFERENCES `여행신청` (`여행신청_ID`),
    CHECK (`참가자구분` IN ('APPLICANT', 'COMPANION'))
);

CREATE TABLE `물품재고` (
    `물품재고_ID` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `여행상품_ID` BIGINT NOT NULL,
    `물품명` VARCHAR(255) NOT NULL,
    `재고수량` INT NOT NULL,
    `재고상태` VARCHAR(40) NOT NULL,
    `수정일시` DATETIME(6) NOT NULL,
    FOREIGN KEY (`여행상품_ID`) REFERENCES `여행상품` (`여행상품_ID`),
    CHECK (`재고수량` >= 0),
    CHECK (`재고상태` IN ('NORMAL', 'LOW_STOCK', 'OUT_OF_STOCK'))
);

CREATE TABLE `문자알림` (
    `문자알림_ID` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `여행신청_ID` BIGINT NOT NULL,
    `알림종류` VARCHAR(40) NOT NULL,
    `수신번호` VARCHAR(255) NOT NULL,
    `메시지내용` VARCHAR(2000) NOT NULL,
    `발송상태` VARCHAR(40) NOT NULL,
    `발송일시` DATETIME(6),
    FOREIGN KEY (`여행신청_ID`) REFERENCES `여행신청` (`여행신청_ID`),
    CHECK (`알림종류` IN ('DEPARTURE_CONFIRMED', 'SCHEDULE_CANCELLED')),
    CHECK (`발송상태` IN ('PENDING', 'SENT', 'FAILED')),
    UNIQUE (`여행신청_ID`, `알림종류`)
);
