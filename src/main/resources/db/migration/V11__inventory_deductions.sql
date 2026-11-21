CREATE TABLE `재고차감내역` (
    `재고차감내역_ID` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `여행일정_ID` BIGINT NOT NULL,
    `물품재고_ID` BIGINT NOT NULL,
    `차감수량` INT NOT NULL,
    `복구일시` DATETIME(6),
    CONSTRAINT `uk_inventory_deduction_schedule_item` UNIQUE (`여행일정_ID`, `물품재고_ID`),
    CONSTRAINT `fk_inventory_deduction_schedule` FOREIGN KEY (`여행일정_ID`) REFERENCES `여행일정` (`여행일정_ID`),
    CONSTRAINT `fk_inventory_deduction_item` FOREIGN KEY (`물품재고_ID`) REFERENCES `물품재고` (`물품재고_ID`),
    CONSTRAINT `ck_inventory_deduction_positive` CHECK (`차감수량` > 0)
);
